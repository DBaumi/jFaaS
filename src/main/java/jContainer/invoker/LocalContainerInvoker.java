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

    public LocalContainerInvoker(){
        super(ExecutionType.LOCAL_DOCKER);
    }

    /**
     * Invoke a function on the local docker engine.
     * @param jarName as a String for the function to invoke
     * @param functionInputs as a Map<String, Object> to represent JSON input
     * @return functionOutputs as a Map<String, Object> to represent JSON output
     * @throws IOException
     */
    @Override
    public JsonObject invokeWithJar(String jarName, Map<String, Object> functionInputs) throws IOException {
        Stopwatch invocationTime = new Stopwatch(false);
        jarName = jarName.toLowerCase();

        FunctionDefinition functionDefinition = new FunctionDefinition(Utils.extractFunctionNameFromFunction(jarName), functionInputs, Utils.extractJDKVersionFromFunction(jarName));
        LocalDockerContainerExecutor localDockerContainerExecutor = new LocalDockerContainerExecutor(functionDefinition);
        localDockerContainerExecutor.executeFunctionWithJarInLocalContainer();

        functionDefinition.setFunctionOutputs(localDockerContainerExecutor.resultFromLocalContainerExecution());

        localDockerContainerExecutor.removeLocalDockerResources();
        logger.info("Execution on local docker container took: {}ms", localDockerContainerExecutor.getExecutionTime());
        logger.info("Invocation of function '{}' as a local docker container took {}ms", functionDefinition.getFunctionName(), invocationTime.getElapsedTime());

        this.cleaner.cleanDirectories();

        return functionDefinition.getFunctionOutputs();
    }

    /**
     * Invoke a function on a container system with a dockerhub repository link to an image with the function already in it on the local docker engine.
     *
     * @param dockerhubImageLink    as a String for the function to invoke
     * @param functionInputs        as a Map<String, Object> to represent JSON input
     * @return functionOutputs      as a Map<String, Object> to represent JSON output
     * @throws IOException
     */
    @Override
    public JsonObject invokeWithDockerhubImage(String dockerhubImageLink, Map<String, Object> functionInputs) throws IOException {
        Stopwatch invocationTime = new Stopwatch(false);
        dockerhubImageLink = dockerhubImageLink.toLowerCase();

        FunctionDefinition functionDefinition = new FunctionDefinition(Utils.getFunctionNameFromDockerhubLink(dockerhubImageLink), functionInputs);
        LocalDockerContainerExecutor localDockerContainerExecutor = new LocalDockerContainerExecutor(functionDefinition);
        localDockerContainerExecutor.getImageManager().setImageName(dockerhubImageLink);
        localDockerContainerExecutor.executeFunctionWithDockerhubInLocalContainer();

        functionDefinition.setFunctionOutputs(localDockerContainerExecutor.resultFromLocalContainerExecution());

        localDockerContainerExecutor.removeLocalDockerResources();
        logger.info("Execution on local docker container took: {}ms", localDockerContainerExecutor.getExecutionTime());
        logger.info("Invocation of function '{}' as a local docker container took {}ms", functionDefinition.getFunctionName(), invocationTime.getElapsedTime());

        this.cleaner.cleanDirectories();

        return functionDefinition.getFunctionOutputs();
    }

    /**
     *
     *
     * @param awsEcrImageLink as a String for the function to invoke
     * @param functionInputs  as a Map<String, Object> to represent JSON input
     * @return
     * @throws IOException
     */
    @Override
    public JsonObject invokeWithAwsEcrImage(String awsEcrImageLink, final Map<String, Object> functionInputs) throws IOException {
        logger.warn("Local invocation with ECR image is not yet supported!");
        throw new UnsupportedOperationException("Local execution with an ECR image is not yet supported!");
    }
}
