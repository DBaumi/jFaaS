package jContainer.executor;

import com.google.gson.JsonObject;
import jContainer.helper.ExecutionType;
import jContainer.helper.FunctionDefinition;
import jContainer.helper.Stopwatch;
import jContainer.management.CloudWatchLogsManager;
import jContainer.management.DockerImageManager;
import jContainer.management.TerraformAwsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class AwsContainerExecutor {
    /* cloud watch logs manager to retrieve the output and the execution time from event log */
    private CloudWatchLogsManager cloudWatchLogsManager;

    /* image manager to create, build, push and pull docker images */
    private DockerImageManager imageManager;

    /* creates local-terraform container to deploy ECS resources */
    private TerraformAwsManager terraformAwsManager;

    /* Logger */
    final static Logger logger = LoggerFactory.getLogger(AwsContainerExecutor.class);

    /**
     * Constructor for AWS execution.
     *
     * @param functionDefinition function
     * @param executionType      provider
     */
    public AwsContainerExecutor(final FunctionDefinition functionDefinition, final ExecutionType executionType) {
        this.imageManager = new DockerImageManager(functionDefinition, executionType);
        this.cloudWatchLogsManager = new CloudWatchLogsManager(functionDefinition);
        this.terraformAwsManager = new TerraformAwsManager(functionDefinition);
    }

    /**
     * Executes the function provided as a JAR on AWS ECS service.
     *
     * @param functionNameWithEnding
     * @throws IOException
     */
    public void executeFunctionWithJarInECS(final String functionNameWithEnding) throws IOException {
        this.imageManager.createDockerImage(functionNameWithEnding);
        this.imageManager.pushDockerImageToHub();
        this.terraformAwsManager.runTerraformScript();
    }

    /**
     * Executes the function provided as a docker image on AWS ECS service.
     *
     * @param dockerhubImageLink
     * @throws IOException
     */
    public void executeFunctionWithDockerhubInECS(final String dockerhubImageLink) throws IOException {
        this.imageManager.setImageName(dockerhubImageLink);
        this.terraformAwsManager.setDockerImageName(this.imageManager.getImageName());
        this.terraformAwsManager.runTerraformScript();
    }

    /**
     * Returns the execution time from CloudWatchLogEvent.
     *
     * @return execution time
     */
    public Long containerExecutionTime() {
        return this.cloudWatchLogsManager.executionTimeFromLogEvent();
    }

    /**
     * Returns the result of the execution.
     *
     * @param functionName
     * @return result from cloudwatch logs as JsonObject
     */
    public JsonObject containerExecutionResult(final String functionName) {
        return this.cloudWatchLogsManager.resultFromLogStreamPrefix(functionName);
    }

    /**
     * Kill all created resources with ECS execution (local docker & ecs services).
     */
    public void cleanUpAllResources(final Boolean deleteLocalImage) {
        final Stopwatch cleanUpTime = new Stopwatch(false);

        if (deleteLocalImage) {
            this.imageManager.removeDockerImage();
        }

        this.terraformAwsManager.removeTerraformCreatedResources();
        this.terraformAwsManager.removeLocalCreatedTerraformResources();

        AwsContainerExecutor.logger.info("Clean up of all terraform cloud and local resources took {}ms", cleanUpTime.getElapsedTime());
    }

    public CloudWatchLogsManager getCloudWatchLogsManager() {
        return this.cloudWatchLogsManager;
    }

    public void setCloudWatchLogsManager(final CloudWatchLogsManager cloudWatchLogsManager) {
        this.cloudWatchLogsManager = cloudWatchLogsManager;
    }

    public DockerImageManager getImageManager() {
        return this.imageManager;
    }

    public void setImageManager(final DockerImageManager imageManager) {
        this.imageManager = imageManager;
    }

    public TerraformAwsManager getTerraformAwsManager() {
        return this.terraformAwsManager;
    }

    public void setTerraformAwsManager(final TerraformAwsManager terraformAwsManager) {
        this.terraformAwsManager = terraformAwsManager;
    }
}
