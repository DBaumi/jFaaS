package jContainer.management;

import com.github.dockerjava.api.model.Image;
import com.google.gson.Gson;
import jContainer.executor.AwsContainerExecutor;
import jContainer.helper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class DockerImageManager extends DockerManager {

    private final String jdkVersion;
    private String destinationFolder;
    private String imageName;
    private ExecutionType type;
    private final static Logger logger = LoggerFactory.getLogger(DockerImageManager.class);

    public DockerImageManager(final FunctionDefinition functionDefinition, final ExecutionType type) {
        super(functionDefinition);
        this.type = type;
        this.jdkVersion = functionDefinition.getJavaVersion();
    }

    /**
     * create needed content for the dockerfile
     *
     * @param fileNameWithEnding of the function needed to run in a container
     * @throws FileNotFoundException
     */
    private void createDockerfileForFunction(final String fileNameWithEnding) throws IOException {

        final File filePath = new File(this.destinationFolder);

        if (!filePath.exists()) {
            filePath.mkdirs();
        }

        final Path providedPathForJar = Paths.get(CredentialsProperties.pathToJar + fileNameWithEnding);

        final File jarFile = Files.exists(providedPathForJar)
                ? new File(providedPathForJar.toString())
                : new File(Constants.Paths.fallbackJarFolder + fileNameWithEnding);

        logger.info("Looking for JAR in '{}'", jarFile);

        if(jarFile.exists()) {
            final InputStream inputStreamForJar = new FileInputStream(jarFile);

            if (inputStreamForJar.available() != 0) {
                Files.copy(
                        inputStreamForJar,
                        Paths.get(this.destinationFolder + fileNameWithEnding),
                        StandardCopyOption.REPLACE_EXISTING
                );

                final File copiedJarFile = new File(this.destinationFolder + fileNameWithEnding);

                if (!copiedJarFile.exists()) {
                    throw new FileNotFoundException("Copied file '" + fileNameWithEnding + "' does not show up in the destination folder '" + this.destinationFolder + "'!");
                }
            } else {
                throw new FileNotFoundException("JAR file could not be read properly, please check file '" + fileNameWithEnding + "' and maybe build it again!");
            }

            inputStreamForJar.close();

            /* get credentials file from root folder */
            this.copyCredentials();

            final String dockerImage = this.selectDockerImageFromJavaVersion();
            final String payload = new Gson().toJson(this.getFunctionDefinition().getFunctionInputs());
            final StringBuilder content = new StringBuilder();

            content.append("FROM " + dockerImage + "\n");
            content.append("WORKDIR /\n");
//        content.append("ADD " + fileNameWithEnding + " ./" + fileNameWithEnding + "\n");
//        content.append("ADD " + Constants.Paths.credentialsFile + " ./" + Constants.Paths.credentialsFile + "\n");
            content.append("ADD . ./\n");
            content.append("CMD java -jar " + fileNameWithEnding + " " + payload + "\n");

            this.createFile(this.destinationFolder + "Dockerfile", content.toString());

        } else {
            LocalFileCleaner cleaner = new LocalFileCleaner();
            cleaner.cleanDirectories();

            throw new FileNotFoundException("File '" + jarFile + "' does not exist!");
        }
    }

    /**
     * Returns docker hub image depending on the java version in the function.
     *
     * @return docker hub image as String
     */
    private String selectDockerImageFromJavaVersion() {
        switch (this.jdkVersion) {
            case "19":
                return Constants.DockerImageTags.openJDK_19_slim;
            case "11":
                return Constants.DockerImageTags.eclipseTemurinJDK_11;
            default:    // jdk8 as default
                return Constants.DockerImageTags.alpineJavaJDK_8;
        }
    }

    private void copyGoogleCredentials() throws IOException {
        final File credentialsFile = new File(Constants.Paths.googleCredentials);
        final String destinationFolder = Constants.Paths.scriptFolder + this.getFunctionDefinition().getFunctionName() + '/';
        final File filePath = new File(destinationFolder);

        if (!credentialsFile.exists()) {
            throw new FileNotFoundException("'" + Constants.Paths.googleCredentials + "' not found");
        }
        Files.copy(Paths.get(Constants.Paths.googleCredentials), Paths.get(destinationFolder + Constants.Paths.googleCredentials), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Copies credentials.properties file into the script folder for the later execution of AWS services, credentials are needed.
     *
     * @throws IOException
     */
    private void copyCredentials() throws IOException {
        final InputStream inputStreamForCredential = new FileInputStream(Constants.Paths.credentialsFile);

        if (inputStreamForCredential.available() != 0) {
            Files.copy(
                    inputStreamForCredential,
                    Paths.get(this.destinationFolder + Constants.Paths.credentialsFile),
                    StandardCopyOption.REPLACE_EXISTING
            );

            final File copiedCredentialFile = new File(this.destinationFolder + Constants.Paths.credentialsFile);

            if (!copiedCredentialFile.exists()) {
                throw new FileNotFoundException("Copying the file 'credentials.property' to the destination folder did not work!");
            }
        }

        inputStreamForCredential.close();
    }

    /**
     * creating docker image for the function
     *
     * @param fileNameWithEnding name of the function needed to run in container
     * @throws FileNotFoundException
     */
    public void createDockerImage(final String fileNameWithEnding) throws IOException {
        this.destinationFolder = (this.type == ExecutionType.LOCAL_DOCKER)
                ? (Constants.Paths.localFunctionDocker + this.getFunctionDefinition().getFunctionName() + '/')
                : (Constants.Paths.scriptFolder + this.getFunctionDefinition().getFunctionName() + '/'); // directory to start docker build


        switch (this.type) {
            case LOCAL_DOCKER:
                this.imageName = "local-function:" + this.getFunctionDefinition().getFunctionName();
                break;
            case ECS:
                if (AwsContainerExecutor.ecrManager == null) {
                    System.err.println("ECR Manager not set");
                    return;
                }
                this.imageName = AwsContainerExecutor.ecrManager.getRepoLink() + ":" + this.getFunctionDefinition().getFunctionName();
//                this.imageName = "ecr_image_" + this.getFunctionDefinition().getFunctionName();
                break;
            default:
                this.imageName = this.getCreatedImageNameDockerHub();
        }

        this.createDockerfileForFunction(fileNameWithEnding);

        if (this.type.equals(ExecutionType.GKE)) {
            this.copyGoogleCredentials();
        }

        final List<Image> images = DockerManager.dockerClient.listImagesCmd().exec();
        if (images.toString().contains(this.getImageName())) {
            this.removeDockerImage();
        }

        this.runCommandInWorkingDirectory("docker build ./ -t " + this.getImageName());
    }

    /**
     * Checks if the image was pulled successfully.
     *
     * @return true when image is showing in list, false otherwise
     */
    public Boolean checkForSuccessfulImagePull() {
        final List<String> imageList = TerminalManager.executeCommand(
                Constants.Paths.localFunctionDocker,
                false,
                "docker image ls"
        );

        return imageList.contains(Utils.getImageNameWithoutTagFromDockerhubLink(this.getImageName()))
                && imageList.contains(this.getFunctionDefinition().getFunctionName());
    }

    /**
     * push the created image onto dockerhub repository
     */
    public void pushDockerImageToHub() {
        this.runCommandInWorkingDirectory(
                "docker login -u " + CredentialsProperties.dockerUser + " -p " + CredentialsProperties.dockerAccessToken,
                "docker push " + this.getCreatedImageNameDockerHub()
        );
    }

    /**
     * push the created image onto AWS ECR repository
     */
    public void pushDockerImageToAwsEcr() {
        if (AwsContainerExecutor.ecrManager == null) {
            System.err.println("ECR Manager not set");
            return;
        }
        AwsContainerExecutor.ecrManager.pushImageToRepo(this.getImageName());
    }

    /**
     * pull image from provided dockerhub repository.
     */
    public void pullDockerImageFromHub() {
        this.runCommandInWorkingDirectory(
                "docker login -u " + CredentialsProperties.dockerUser + " -p " + CredentialsProperties.dockerAccessToken,
                "docker image pull " + this.getImageName()
        );
    }

    /**
     * pull image from provided AWS ECR repository.
     */
    public void pullDockerImageFromAwsEcr() {
        if (AwsContainerExecutor.ecrManager == null) {
            System.err.println("ECR Manager not set");
            return;
        }
        AwsContainerExecutor.ecrManager.pullImageFromRepo(this.getImageName());
    }

    /**
     * deleting the created docker image
     */
    public void removeDockerImage() {
        this.runCommandInWorkingDirectory(
                "docker rmi " + this.getImageName()
        );
    }

    /**
     * Returns the public dockerhub repository for the pushed function.
     * Format: 'username/repositoryname:tag'
     *
     * @return string
     */
    @Override
    protected String getCreatedImageNameDockerHub() {
        return Constants.Docker.hub_user_and_repo_name + this.getFunctionDefinition().getFunctionName();
    }

    public String getDestinationFolder() {
        return this.destinationFolder;
    }

    public void setDestinationFolder(final String destinationFolder) {
        this.destinationFolder = destinationFolder;
    }

    public String getImageName() {
        return this.imageName;
    }

    public void setImageName(final String imageName) {
        this.imageName = imageName;
    }

    public ExecutionType getType() {
        return this.type;
    }

    public void setType(final ExecutionType type) {
        this.type = type;
    }

}
