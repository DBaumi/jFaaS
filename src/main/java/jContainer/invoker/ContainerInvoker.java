package jContainer.invoker;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jContainer.helper.ExecutionType;
import jContainer.helper.LocalFileCleaner;
import jContainer.helper.Stopwatch;
import jContainer.helper.Utils;
import jFaaS.utils.PairResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 *  Abstract invoker class which will be the interface for the jContainer integration in jFaaS.
 */
public abstract class ContainerInvoker {
    /* Execution type */
    public ExecutionType executionType;

    /* File cleaner for created local files */
    public LocalFileCleaner cleaner;

    /* Logger */
    final static Logger logger = LoggerFactory.getLogger(ContainerInvoker.class);

    /**
     * Constructor for the container invoker.
     *
     * @param executionType
     */
    public ContainerInvoker(final ExecutionType executionType) {
        this.executionType = executionType;
    }

    /**
     * Starts the proper invocation function depending on its input.
     *
     * @param function       String containing the execution type and the dockerhub link or the function JAR name
     * @param functionInputs JsonObject for the inputs of the execution
     * @return JsonObject of the execution result
     * @throws IOException when input file cannot be found
     */
    public PairResult<String, Long> invokeFunction(final String function, final Map<String, Object> functionInputs) throws IOException {
        final String functionResourceLink = Utils.extractResourceLink(function);
        JsonObject result = new JsonObject();
        final Stopwatch timer = new Stopwatch(false);

        if (this instanceof EcsContainerInvoker && Utils.isAwsEcrRepoLink(functionResourceLink)) {
            ContainerInvoker.logger.info("Invocation with public AWS ECR image was chosen!");
            result = this.invokeWithAwsEcrImage(functionResourceLink, functionInputs);
        } else if (this instanceof LocalContainerInvoker && Utils.isDockerhubRepoLink(functionResourceLink)) {
            ContainerInvoker.logger.info("Invocation with private dockerhub image was chosen!");
            result = this.invokeWithDockerhubImage(functionResourceLink, functionInputs);
        } else if (functionResourceLink.contains(":")) {
            ContainerInvoker.logger.info("Invocation with JAR file was chosen!");
            result = this.invokeWithJar(functionResourceLink, functionInputs);
        } else {
            if (this instanceof EcsContainerInvoker) {
                ContainerInvoker.logger.error("Dockerhub link to private repository was provided wrong. Please check for the correct format: {}", "aws_account_id.dkr.ecr.region.amazonaws.com/my-repository:tag");
                result.addProperty("error", "AWS ECR link was provided in wrong format!");
            } else if (this instanceof LocalContainerInvoker) {
                ContainerInvoker.logger.error("Dockerhub link to private repository was provided wrong. Please check for the correct format: {}", "username/repository:imagetag");
                result.addProperty("error", "Dockerhub link was provided in wrong format!");
            } else {
                ContainerInvoker.logger.error("Invalid Link given");
                result.addProperty("error", "Link is invalid (maybe unimplemented provider?)");
            }
        }

        return new PairResult<>(new Gson().fromJson(result, JsonObject.class).toString(), (long) timer.getElapsedTime());
    }

    /**
     * Invoke a function on a container system with an existing JAR file of the function in jars/ directory.
     *
     * @param jarName       as a String for the function to invoke
     * @param functionInputs as a Map<String, Object> to represent JSON input
     * @return functionOutputs as a Map<String, Object> to represent JSON output
     * @throws IOException
     */
    abstract public JsonObject invokeWithJar(String jarName, Map<String, Object> functionInputs) throws IOException;

    /**
     * Invoke a function on a container system with a dockerhub repository link to an image with the function already in it.
     *
     * @param dockerhubImageLink as a String for the function to invoke
     * @param functionInputs     as a Map<String, Object> to represent JSON input
     * @return functionOutputs      as a Map<String, Object> to represent JSON output
     * @throws IOException
     */
    abstract public JsonObject invokeWithDockerhubImage(String dockerhubImageLink, Map<String, Object> functionInputs) throws IOException;

    /**
     * Invoke a function on a container system with an AWS ECR repository link to an image with the function already in it.
     *
     * @param awsEcrImageLink as a String for the function to invoke
     * @param functionInputs  as a Map<String, Object> to represent JSON input
     * @return functionOutputs      as a Map<String, Object> to represent JSON output
     * @throws IOException
     */
    abstract public JsonObject invokeWithAwsEcrImage(final String awsEcrImageLink, final Map<String, Object> functionInputs) throws IOException;


    public ExecutionType getExecutionType() {
        return this.executionType;
    }

    public void setExecutionType(final ExecutionType executionType) {
        this.executionType = executionType;
    }

}
