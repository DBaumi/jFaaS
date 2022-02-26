package jContainer.invoker;

import com.google.gson.JsonObject;
import jContainer.executor.AwsContainerExecutor;
import jContainer.helper.ExecutionType;
import jContainer.helper.FunctionDefinition;
import jContainer.helper.Stopwatch;
import jContainer.helper.Utils;

import java.io.IOException;
import java.util.Map;

public class EcsContainerInvoker extends ContainerInvoker {

    public EcsContainerInvoker() {
        super(ExecutionType.ECS);
    }

    /**
     * Invoke a function on AWS ECS service.
     *
     * @param jarName       as a String for the function to invoke
     * @param functionInputs as a Map<String, Object> to represent JSON input
     * @return functionOutputs as a Map<String, Object> to represent JSON output
     * @throws IOException
     */
    @Override
    public JsonObject invokeWithJar(String jarName, final Map<String, Object> functionInputs) throws IOException {
        final Stopwatch invocationTime = new Stopwatch(false);
        jarName = jarName.toLowerCase();

        final FunctionDefinition functionDefinition = new FunctionDefinition(Utils.extractFunctionNameFromResourceLink(jarName), functionInputs, Utils.extractJDKVersionFromResourceLink(jarName));
        final AwsContainerExecutor awsContainerExecutor = new AwsContainerExecutor(functionDefinition, this.executionType);
        awsContainerExecutor.executeFunctionWithJarInECS(functionDefinition.getJarFileName());

        functionDefinition.setFunctionOutputs(awsContainerExecutor.containerExecutionResult(functionDefinition.getFunctionName()));

        awsContainerExecutor.cleanUpAllResources(true, true);

        ContainerInvoker.logger.info("Execution on ECS took: {}ms", awsContainerExecutor.containerExecutionTime());
        ContainerInvoker.logger.info("Invocation of function '{}' on AWS with ECS container system took {}ms", functionDefinition.getFunctionName(), invocationTime.getElapsedTime());

        return functionDefinition.getFunctionOutputs();
    }

    /**
     * Invoke a function on a container system with a dockerhub repository link to an image with the function already in it on AWS ECS service.
     *
     * @param dockerhubImageLink as a String for the function to invoke
     * @param functionInputs     as a Map<String, Object> to represent JSON input
     * @return functionOutputs      as a Map<String, Object> to represent JSON output
     * @throws IOException
     */
    @Override
    public JsonObject invokeWithDockerhubImage(String dockerhubImageLink, final Map<String, Object> functionInputs) throws IOException {
        final Stopwatch invocationTime = new Stopwatch(false);
        dockerhubImageLink = dockerhubImageLink.toLowerCase();

        final FunctionDefinition functionDefinition = new FunctionDefinition(Utils.getFunctionNameFromDockerhubLink(dockerhubImageLink), functionInputs);
        final AwsContainerExecutor awsContainerExecutor = new AwsContainerExecutor(functionDefinition, this.executionType);
        awsContainerExecutor.executeFunctionWithECRImageInECS(dockerhubImageLink);

        functionDefinition.setFunctionOutputs(awsContainerExecutor.containerExecutionResult(functionDefinition.getFunctionName()));

        awsContainerExecutor.cleanUpAllResources(false, false);

        ContainerInvoker.logger.info("Execution on ECS took: {}ms", awsContainerExecutor.containerExecutionTime());
        ContainerInvoker.logger.info("Invocation of function '{}' on AWS with ECS container system took {}ms", functionDefinition.getFunctionName(), invocationTime.getElapsedTime());

        return functionDefinition.getFunctionOutputs();
    }

    /**
     * Invoke a function on a container system with a dockerhub repository link to an image with the function already in it on AWS ECS service.
     *
     * @param awsEcrImageLink as a String for the function to invoke
     * @param functionInputs  as a Map<String, Object> to represent JSON input
     * @return functionOutputs      as a Map<String, Object> to represent JSON output
     * @throws IOException
     */
    @Override
    public JsonObject invokeWithAwsEcrImage(String awsEcrImageLink, final Map<String, Object> functionInputs) throws IOException {
        final Stopwatch invocationTime = new Stopwatch(false);
        awsEcrImageLink = awsEcrImageLink.toLowerCase();

        final FunctionDefinition functionDefinition = new FunctionDefinition(Utils.getFunctionNameFromAwsEcrLink(awsEcrImageLink), functionInputs);
        final AwsContainerExecutor awsContainerExecutor = new AwsContainerExecutor(functionDefinition, this.executionType);
        awsContainerExecutor.executeFunctionWithECRImageInECS(awsEcrImageLink);

        functionDefinition.setFunctionOutputs(awsContainerExecutor.containerExecutionResult(functionDefinition.getFunctionName()));

        awsContainerExecutor.cleanUpAllResources(false, false);

        ContainerInvoker.logger.info("Execution on ECS took: {}ms", awsContainerExecutor.containerExecutionTime());
        ContainerInvoker.logger.info("Invocation of function '{}' on AWS with ECS container system took {}ms", functionDefinition.getFunctionName(), invocationTime.getElapsedTime());

        return functionDefinition.getFunctionOutputs();
    }
}
