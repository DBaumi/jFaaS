package jContainer.executor;

import com.github.dockerjava.api.model.Container;
import com.google.gson.JsonObject;
import jContainer.helper.*;
import jContainer.management.DockerImageManager;
import jContainer.management.DockerManager;
import jContainer.management.TerminalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 */
public class LocalDockerContainerExecutor extends DockerManager {

    /* Name for the docker container */
    private String localFunctionContainerName;

    /* Name of the image for the container */
    private final DockerImageManager imageManager;

    /* Output of the execution on the container */
    private String output;

    /* Execution time of local container */
    private Double executionTime;

    /* Logger */
    final static Logger logger = LoggerFactory.getLogger(LocalDockerContainerExecutor.class);

    /**
     * initialize docker-client
     *
     * @param functionDefinition
     */
    public LocalDockerContainerExecutor(final FunctionDefinition functionDefinition) {
        super(functionDefinition);
        this.localFunctionContainerName = "local-function_" + functionDefinition.getFunctionName();
        this.imageManager = new DockerImageManager(functionDefinition, ExecutionType.LOCAL_DOCKER);
    }

    /**
     * Starts the execution as a local container. It first creates an image and then
     * executes the function in a container.
     *
     * @throws IOException
     */
    public void executeFunctionWithJarInLocalContainer() throws IOException {
        this.imageManager.setWorkingDirectory(Constants.Paths.localFunctionDocker + this.getFunctionDefinition().getFunctionName() + '/');
        this.imageManager.createDockerImage(this.getFunctionDefinition().getJarFileName());
        this.startContainer();
    }

    /**
     * Starts the execution as a local container from a dockerhub link to a public repository image. If first creates an image and then
     */
    public void executeFunctionWithDockerhubInLocalContainer() throws IOException {
        this.imageManager.setWorkingDirectory(Constants.Paths.localFunctionDocker + this.getFunctionDefinition().getFunctionName() + '/');
        Boolean ready = this.prepareDockerImageForLocalExecution();

        if(ready) {
            this.startContainer();
        } else {
            logger.error("Image could not be pulled, check provided dockerhub image link: '{}'", this.imageManager.getImageName());
        }
    }

    /**
     * Starts a docker container with the already created image for the execution of the function.
     */
    private void startContainer() {
        final List<Container> containers = DockerManager.dockerClient.listContainersCmd().exec();

        if (containers.stream().noneMatch(c -> c.getImage().equals(this.localFunctionContainerName))) {
            final Stopwatch runtime = new Stopwatch(false);

            // execute function in container
            this.containerCommandExecutor("docker run -d -i --name " + this.localFunctionContainerName + " " + this.imageManager.getImageName());
            this.executionTime = runtime.getElapsedTime();
            Utils.sleep(5000);

            // get output
            this.getContainerOutput();
        }
    }

    /**
     * Activates container logs and sets them to the output field.
     */
    private void getContainerOutput() {
        List<String> containerOutput = null;

        while (containerOutput == null) {
            containerOutput = this.containerCommandExecutor("docker logs " + this.localFunctionContainerName);
        }

        this.output = containerOutput.get(0);
    }

    /**
     * Checks the output of the execution on the container for white spaces and returns it as a JsonObject.
     *
     * @return output JsonObject
     */
    public JsonObject resultFromLocalContainerExecution() {
        this.output = Utils.checkForLinebreak(this.output);
        return Utils.generateJsonOutput(this.output);
    }

    /**
     * Stops the running container.
     */
    public void stopFunctionContainer() {
        this.containerCommandExecutor("docker stop " + this.localFunctionContainerName);
    }

    /**
     * Prepares directory for the local image/container.
     */
    private Boolean prepareDockerImageForLocalExecution(){
        Boolean ready;
        final File localDockerDirectory = new File(this.imageManager.getWorkingDirectory());
        localDockerDirectory.mkdirs();

        this.imageManager.pullDockerImageFromHub();
        Utils.sleep(Constants.utils.sleepTimer);

        ready = this.imageManager.checkForSuccessfulImagePull();

        return ready;
    }

    /**
     * Removes all locally created container resources and log the time.
     */
    public void removeLocalDockerResources() {
        final Stopwatch stopwatch = new Stopwatch(false);
        if (this.removeLocalContainer()) {
            logger.info("Successfully deleted container '{}'!", this.localFunctionContainerName);
        } else {
            logger.error("Failed to delete container '{}'!", this.localFunctionContainerName);
        }
        if (this.removeLocalImage()) {
            logger.info("Successfully deleted image '{}'!", this.imageManager.getImageName());
        } else {
            logger.error("Failed to delete image '{}'!", this.imageManager.getImageName());
        }
        logger.info("Time to remove resources was: {}ms", stopwatch.getElapsedTime());
    }

    /**
     * Removes the local container for execution of the function.
     */
    private Boolean removeLocalContainer() {
        final List<String> terminalOutput = this.containerCommandExecutor("docker rm -f " + this.localFunctionContainerName);

        if (terminalOutput.size() > 0) {
            for (final String line : terminalOutput) {
                if (line.contains(this.localFunctionContainerName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Removes the local image of the function.
     */
    private Boolean removeLocalImage() {
        final List<String> terminalOutput = this.containerCommandExecutor("docker rmi " + this.imageManager.getImageName());

        if (terminalOutput.size() > 0) {
            for (final String line : terminalOutput) {
                if (line.contains("Deleted")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper function to execute commands inside the container always in the same directory.
     *
     * @param command
     */
    private List<String> containerCommandExecutor(final String command) {
        return TerminalManager.executeCommand(
                Constants.Paths.localFunctionDocker,
                false,
                command
        );
    }

    public String getLocalFunctionContainerName() {
        return this.localFunctionContainerName;
    }

    public void setLocalFunctionContainerName(final String localFunctionContainerName) {
        this.localFunctionContainerName = localFunctionContainerName;
    }

    public String getOutput() {
        return this.output;
    }

    public void setOutput(final String output) {
        this.output = output;
    }

    public Double getExecutionTime() {
        return this.executionTime;
    }

    public DockerImageManager getImageManager() {
        return this.imageManager;
    }
}
