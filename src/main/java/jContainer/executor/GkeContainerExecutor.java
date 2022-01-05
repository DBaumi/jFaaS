package jContainer.executor;

import jContainer.helper.ExecutionType;
import jContainer.helper.FunctionDefinition;
import jContainer.management.DockerImageManager;
import jContainer.management.TerraformGkeManager;

import java.io.IOException;

/**
 * WIP: On hold because the invoker will not be implemented for jFaaS.
 */
public class GkeContainerExecutor {

    private DockerImageManager imageManager;
    private TerraformGkeManager terraformGkeManager;

    public GkeContainerExecutor(FunctionDefinition functionDefinition, ExecutionType executionType) {
        /*this.imageManager = new DockerImageManager(functionDefinition, executionType);
        this.terraformGkeManager = new TerraformGkeManager(functionDefinition);*/
    }

    public void executeFunctionWithJarInGKE() throws IOException {

    }

    public void executeFunctionWithDockerhubInGKE() throws IOException {

    }

}
