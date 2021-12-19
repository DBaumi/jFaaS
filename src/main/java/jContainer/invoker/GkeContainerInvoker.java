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
     *
     * @param function       as a String for the function to invoke
     * @param functionInputs as a Map<String, Object> to represent JSON input
     * @return functionOutputs as a Map<String, Object> to represent JSON output
     * @throws IOException
     */
    @Override
    public JsonObject invokeWithJarOnContainer(final String function, final Map<String, Object> functionInputs) throws IOException {
        return new JsonObject();
    }

    /**
     * Invoke a function on a container system with a dockerhub repository link to an image with the function already in it on Google GKE service.
     *
     * @param dockerhubImageLink as a String for the function to invoke
     * @param functionInputs     as a Map<String, Object> to represent JSON input
     * @return functionOutputs as a Map<String, Object> to represent JSON output
     * @throws IOException
     */
    @Override
    public JsonObject invokeWithDockerhubImageOnContainer(final String dockerhubImageLink, final Map<String, Object> functionInputs) throws IOException {
        return new JsonObject();
    }
}
