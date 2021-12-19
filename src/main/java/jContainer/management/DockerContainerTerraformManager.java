package jContainer.management;

import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import jContainer.helper.Constants;
import jContainer.helper.FunctionDefinition;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DockerContainerTerraformManager extends DockerManager {

    private final String localTerraformDockerImageName = "local-terraform";

    public DockerContainerTerraformManager(final FunctionDefinition functionDefinition) {
        super(functionDefinition);
    }

    /**
     * creates predefined local terraform image
     *
     * @throws IOException
     */
    private void createLocalTerraformImage() throws IOException {
        final String destinationFolder = Constants.Paths.localTerraformDocker;
        final String scriptFolder = Constants.Paths.scriptFolder + this.getFunctionDefinition().getFunctionName() + '/';
        final File destinationPath = new File(destinationFolder);

        if (!destinationPath.exists()) {
            destinationPath.mkdirs();
        }
        FileUtils.copyDirectory(new File(scriptFolder), new File(destinationFolder + this.getFunctionDefinition().getFunctionName()));//, asFilter);

        final StringBuilder content = new StringBuilder();
        content.append("FROM ubuntu:21.10\n");
        content.append("WORKDIR /\n");
        content.append(
                "RUN apt update -y\n" +
                        "RUN apt upgrade -y \n" +
                        "RUN apt install sudo -y \n" +
                        "RUN sudo apt install -y \\\n" +
                        "\t\twget \\\n" +
                        "\t\tunzip\n" +
                        "RUN wget https://releases.hashicorp.com/terraform/0.14.5/terraform_0.14.5_linux_amd64.zip\n" +
                        "RUN unzip terraform_0.14.5_linux_amd64.zip\n" +
                        "RUN sudo mv terraform /usr/local/bin/\n");
        content.append("ADD ./" + this.getFunctionDefinition().getFunctionName() + " ./\n");

        this.createFile(destinationFolder + "Dockerfile", content.toString());

        final List<Image> images = DockerManager.dockerClient.listImagesCmd().exec();
        final List<String> repoTags = images.stream()
                .map(i -> Arrays.stream(i.getRepoTags()).collect(Collectors.toList()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        if (repoTags.stream().noneMatch(t -> t.contains(this.localTerraformDockerImageName))) {
            TerminalManager.executeCommand(Constants.Paths.localTerraformDocker, false, "docker build . -t " + this.localTerraformDockerImageName);
        }
    }

    /**
     * creates and starts the local terraform-docker-container
     *
     * @throws IOException
     */
    public void startTerraformContainer() throws IOException {
        this.createLocalTerraformImage();
        final List<Container> containers = DockerManager.dockerClient.listContainersCmd().exec();
        if (containers.stream().noneMatch(c -> c.getImage().equals(this.localTerraformDockerImageName))) {
            TerminalManager.executeCommand(Constants.Paths.localTerraformDocker, false, "docker run -d -i --name " + this.getContainerName() + " " + this.localTerraformDockerImageName);
        }

    }

    /**
     * @return
     */
    public Boolean removeTerraformDockerResources() {
        final List<String> containerDeletion = TerminalManager.executeCommand(
                Constants.Paths.localTerraformDocker,
                false,
                "docker stop " + this.getContainerName(), "docker rm -f " + this.getContainerName()
        );

        final List<String> imageDeletion = TerminalManager.executeCommand(
                Constants.Paths.localTerraformDocker,
                false, "docker rmi " + this.localTerraformDockerImageName
        );

        return (containerDeletion.size() > 0) && (imageDeletion.size() > 0);
    }

    /**
     * execute a command in terraform-docker-container
     *
     * @param command
     */
    public void sendCommandToContainer(final String command) {
        TerminalManager.executeCommand(Constants.Paths.localTerraformDocker, false, "docker exec " + this.getContainerName() + " " + command);
    }

    public String getLocalTerraformDockerImageName() {
        return this.localTerraformDockerImageName;
    }

    private String getContainerName() {
        return this.localTerraformDockerImageName + "_" + this.getFunctionDefinition().getFunctionName();
    }

}
