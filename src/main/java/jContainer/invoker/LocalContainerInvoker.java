package jContainer.invoker;

import com.google.gson.JsonObject;
import jContainer.executor.LocalDockerContainerExecutor;
import jContainer.helper.ExecutionType;
import jContainer.helper.FunctionDefinition;
import jContainer.helper.Stopwatch;
import jContainer.helper.Utils;

import java.io.IOException;
import java.util.Map;

public class LocalContainerInvoker extends ContainerInvoker {

    public LocalContainerInvoker() {
        super(ExecutionType.LOCAL_DOCKER);
    }

    /**
     * Invoke a function on the local docker engine.
     *
     * @param function       as a String for the function to invoke
     * @param functionInputs as a Map<String, Object> to represent JSON input
     * @return functionOutputs as a Map<String, Object> to represent JSON output
     * @throws IOException
     */
    @Override
    public JsonObject invokeWithJarOnContainer(String function, final Map<String, Object> functionInputs) throws IOException {
        final Stopwatch invocationTime = new Stopwatch(false);
        function = function.toLowerCase();

        final FunctionDefinition functionDefinition = new FunctionDefinition(Utils.extractFunctionNameFromFunction(function), functionInputs, Utils.extractJDKVersionFromFunction(function));
        final LocalDockerContainerExecutor localDockerContainerExecutor = new LocalDockerContainerExecutor(functionDefinition);
        localDockerContainerExecutor.executeFunctionWithJarInLocalContainer();

        functionDefinition.setFunctionOutputs(localDockerContainerExecutor.resultFromLocalContainerExecution());

        localDockerContainerExecutor.removeLocalDockerResources();
        ContainerInvoker.logger.info("Execution on local docker container took: {}ms", localDockerContainerExecutor.getExecutionTime());
        ContainerInvoker.logger.info("Invocation of function '{}' as a local docker container took {}ms", functionDefinition.getFunctionName(), invocationTime.getElapsedTime());

        this.cleaner.cleanDirectories();

        return functionDefinition.getFunctionOutputs();
    }

    /**
     * Invoke a function on a container system with a dockerhub repository link to an image with the function already in it on the local docker engine.
     *
     * @param dockerhubImageLink as a String for the function to invoke
     * @param functionInputs     as a Map<String, Object> to represent JSON input
     * @return functionOutputs      as a Map<String, Object> to represent JSON output
     * @throws IOException
     */
    @Override
    public JsonObject invokeWithDockerhubImageOnContainer(String dockerhubImageLink, final Map<String, Object> functionInputs) throws IOException {
        final Stopwatch invocationTime = new Stopwatch(false);
        dockerhubImageLink = dockerhubImageLink.toLowerCase();

        final FunctionDefinition functionDefinition = new FunctionDefinition(Utils.getFunctionNameFromDockerhubLink(dockerhubImageLink), functionInputs);
        final LocalDockerContainerExecutor localDockerContainerExecutor = new LocalDockerContainerExecutor(functionDefinition);
        localDockerContainerExecutor.getImageManager().setImageName(dockerhubImageLink);
        localDockerContainerExecutor.executeFunctionWithDockerhubInLocalContainer();

        functionDefinition.setFunctionOutputs(localDockerContainerExecutor.resultFromLocalContainerExecution());

        localDockerContainerExecutor.removeLocalDockerResources();
        ContainerInvoker.logger.info("Execution on local docker container took: {}ms", localDockerContainerExecutor.getExecutionTime());
        ContainerInvoker.logger.info("Invocation of function '{}' as a local docker container took {}ms", functionDefinition.getFunctionName(), invocationTime.getElapsedTime());

        this.cleaner.cleanDirectories();

        return functionDefinition.getFunctionOutputs();
    }
}
