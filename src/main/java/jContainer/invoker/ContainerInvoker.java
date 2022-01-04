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
     * @param executionType as enum
     */
    public ContainerInvoker(final ExecutionType executionType) {
        this.executionType = executionType;
        this.cleaner = new LocalFileCleaner(executionType);
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
        final String functionAsInputForInvoker = Utils.extractInputFromFunction(function);
        JsonObject result = new JsonObject();
        final Stopwatch timer = new Stopwatch(false);

        if (Utils.isDockerhubRepoLink(functionAsInputForInvoker)) {
            ContainerInvoker.logger.info("Invocation with public dockerhub image was chosen!");
            result = this.invokeWithDockerhubImageOnContainer(functionAsInputForInvoker, functionInputs);
        } else if (functionAsInputForInvoker.contains(":")) {
            ContainerInvoker.logger.info("Invocation with JAR file with JDK{} was chosen!", Utils.extractJDKVersionFromFunction(functionAsInputForInvoker));
            result = this.invokeWithJarOnContainer(functionAsInputForInvoker, functionInputs);
        } else if (!functionAsInputForInvoker.contains(":")) {
            ContainerInvoker.logger.info("Invocation with JAR file with JDK8 is assumed!");
            result = this.invokeWithJarOnContainer(functionAsInputForInvoker, functionInputs);
        } else {
            ContainerInvoker.logger.error("Dockerhub link to public repository or JAR name was provided wrong. Please check for the correct format: dockerhub link = {}, JAR = {}", "username/repository:imagetag", "functionName:JDK (8, 11, 19) or functionName");
        }

        return new PairResult<>(new Gson().fromJson(result, JsonObject.class).toString(), (long) timer.getElapsedTime());
    }

    /**
     * Invoke a function on a container system with an existing JAR file of the function in jars/ directory.
     *
     * @param function       as a String for the function to invoke
     * @param functionInputs as a Map<String, Object> to represent JSON input
     * @return functionOutputs as a Map<String, Object> to represent JSON output
     * @throws IOException when input cannot be found
     */
    abstract public JsonObject invokeWithJarOnContainer(String function, Map<String, Object> functionInputs) throws IOException;

    /**
     * Invoke a function on a container system with a dockerhub repository link to an image with the function already in it.
     *
     * @param dockerhubImageLink as a String for the function to invoke
     * @param functionInputs     as a Map<String, Object> to represent JSON input
     * @return functionOutputs      as a Map<String, Object> to represent JSON output
     * @throws IOException when input cannot be found
     */
    abstract public JsonObject invokeWithDockerhubImageOnContainer(String dockerhubImageLink, Map<String, Object> functionInputs) throws IOException;
    
    public ExecutionType getExecutionType() {
        return this.executionType;
    }

    public void setExecutionType(final ExecutionType executionType) {
        this.executionType = executionType;
    }

}
