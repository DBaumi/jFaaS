package jContainer.invoker;

import com.google.gson.JsonObject;
import jContainer.helper.ExecutionType;

import java.io.IOException;
import java.util.Map;

/**
 * WIP: Implementation is on hold because the integration of Google GKE will not be beneficial for the integration into jFaaS.
 */
public class GkeContainerInvoker extends ContainerInvoker {

    public GkeContainerInvoker() {
        super(ExecutionType.GKE);
    }

    /**
     * Invoke a function on Google GKE service.
     * @param jarName as a String for the function to invoke
     * @param functionInputs as a Map<String, Object> to represent JSON input
     * @return functionOutputs as a Map<String, Object> to represent JSON output
     * @throws IOException
     */
    @Override
    public JsonObject invokeWithJar(String jarName, Map<String, Object> functionInputs) throws IOException {
        logger.warn("Google GKE execution with JAR file is not yet supported!");
        throw new UnsupportedOperationException("Google GKE execution with a JAR file is not yet supported!");
    }

    /**
     * Invoke a function on a container system with a dockerhub repository link to an image with the function already in it on Google GKE service.
     *
     * @param dockerhubImageLink       as a String for the function to invoke
     * @param functionInputs as a Map<String, Object> to represent JSON input
     * @return functionOutputs as a Map<String, Object> to represent JSON output
     * @throws IOException
     */
    @Override
    public JsonObject invokeWithDockerhubImage(String dockerhubImageLink, Map<String, Object> functionInputs) throws IOException {
        logger.warn("Google GKE execution with dockerhub image is not yet supported!");
        throw new UnsupportedOperationException("Local execution with a dockerhub image is not yet supported!");
    }

    /**
     * Invoke a function on a container system with an AWS ECR repository link to an image with the function already in it.
     *
     * @param awsEcrImageLink as a String for the function to invoke
     * @param functionInputs  as a Map<String, Object> to represent JSON input
     * @return functionOutputs      as a Map<String, Object> to represent JSON output
     * @throws IOException
     */
    @Override
    public JsonObject invokeWithAwsEcrImage(String awsEcrImageLink, Map<String, Object> functionInputs) throws IOException {
        logger.warn("Google GKE execution with an ECR image is not yet supported!");
        throw new UnsupportedOperationException("Google GKE execution with an ECR image is not yet supported!");
    }
}
