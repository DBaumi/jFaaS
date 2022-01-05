package jContainer.management;

import jContainer.helper.CredentialsProperties;
import jContainer.helper.FunctionDefinition;

import java.io.IOException;

public abstract class AbstractTerraformManager extends AbstractManager {

    protected final DockerContainerTerraformManager dockerContainerTerraformManager;
    protected String dockerImageName = this.getCreatedImageNameDockerHub();

    public AbstractTerraformManager(final FunctionDefinition functionDefinition) {
        super(functionDefinition);
        this.dockerContainerTerraformManager = new DockerContainerTerraformManager(functionDefinition);
    }

    /**
     * defines the structure for a terraform file for aws ecs
     */
    protected abstract void createTerraformScript();

    /**
     * executes the created terraform script
     */
    public void runTerraformScript() throws IOException {
        this.createTerraformScript();

//        start local terraform container
        this.dockerContainerTerraformManager.startTerraformContainer();
        this.execTerraformCommand("terraform init", "terraform refresh", "terraform apply -auto-approve");
    }

    /**
     * destroys the created aws resources
     */
    public void removeTerraformCreatedResources() {
        this.execTerraformCommand("terraform plan -destroy -out=tfplan", "terraform apply \"tfplan\"");
    }

    /**
     * removes all locally created docker resources.
     *
     * @return true if deletion was successful, false otherwise
     */
    public Boolean removeLocalCreatedTerraformResources() {
        return this.dockerContainerTerraformManager.removeTerraformDockerResources();
    }

    /**
     * gets the outputs out of terraform script
     */
    public void getTerraformOutput() {
        this.execTerraformCommand(
                "terraform output -json",
                "terraform output -json > " + this.getFunctionDefinition().getFunctionName() + "_output.json"
        );
        // TODO: copy output to local machine?
    }

    /**
     * send and execute commands in terraform-docker-container
     *
     * @param commands
     */
    protected void execTerraformCommand(final String... commands) {
        for (final String command : commands) {
            this.dockerContainerTerraformManager.sendCommandToContainer(command);
        }
    }

    protected String getUniqueSuffix() {
        return "_" + this.getFunctionDefinition().getFunctionName().toLowerCase() + "_" + CredentialsProperties.localUser.toLowerCase();
    }

    public String getDockerImageName() {
        return this.dockerImageName;
    }

    public void setDockerImageName(final String dockerImageName) {
        this.dockerImageName = dockerImageName;
    }

    public DockerContainerTerraformManager getDockerContainerTerraformManager() {
        return this.dockerContainerTerraformManager;
    }
}
