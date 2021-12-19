package jContainer.management;

import jContainer.helper.Constants;
import jContainer.helper.CredentialsProperties;
import jContainer.helper.FunctionDefinition;

import java.io.IOException;

public class TerraformAwsManager extends AbstractManager {

    private final DockerContainerTerraformManager dockerContainerTerraformManager;
    private String dockerImageName = this.getCreatedImageNameDockerHub();

    public TerraformAwsManager(final FunctionDefinition functionDefinition) {
        super(functionDefinition);
        this.dockerContainerTerraformManager = new DockerContainerTerraformManager(functionDefinition);
    }

    /**
     * defines the structure for a terraform file for aws ecs
     */
    private void createAWSTerraformScript() {

        final String logGroupName = Constants.CloudWatch.log_group_name + this.getUniqueSuffix();
        final String clusterName = "terraform_cluster" + this.getUniqueSuffix();
        final String dockerImageName = this.dockerImageName;
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(
                "locals {\n" +
                        "\tl_region = \"" + CredentialsProperties.awsRegion + "\"\n" +
                        "\tl_access_key = \"" + CredentialsProperties.awsAccessKey + "\"\n" +
                        "\tl_secret_key = \"" + CredentialsProperties.awsSecretKey + "\"\n" +
                        "\tl_token = \"" + CredentialsProperties.awsSessionToken + "\"\n" +
                        "\tl_log_group_name = \"" + logGroupName + "\"\n" +
                        "\tl_retention_in_days = \"" + Constants.CloudWatch.retention_time_days + "\"\n" +
                        "\tl_cluster_name = \"ecs-terraform-cluster" + this.getUniqueSuffix() + "\"\n" +
                        "\tl_kms_key_id = \"" + CredentialsProperties.awsEncryptionKey + "\"\n" +
                        "\tl_family = \"" + this.getFunctionDefinition().getFunctionName() + "_definition\"\n" +
                        "\tl_function_name = \"" + this.getFunctionDefinition().getFunctionName() + "\"\n" +
                        "\tl_docker_image = \"" + dockerImageName + "\"\n" +
                        "\tl_log_group_prefix = \"" + Constants.CloudWatch.log_group_prefix + "\"\n" +
                        "\tl_execution_role_arn = \"" + CredentialsProperties.awsExecutionRole + "\"\n" +
                        "\tl_task_role_arn = \"" + CredentialsProperties.awsExecutionRole + "\"\n" +
                        "\tl_service_name = \"terraform_service" + this.getUniqueSuffix() + "\"\n" +
                        "\tl_subnet = \"" + CredentialsProperties.awsSubnet + "\"\n" +
                        "\tl_vpc_security_group = \"" + CredentialsProperties.awsVpcSecurityGroup + "\"\n" +
                        "}\n"
        );
        // aws credentials
        stringBuilder.append(
                "provider \"aws\" {\n" +
                        "\tregion = local.l_region\n" +
                        "\taccess_key = local.l_access_key\n" +
                        "\tsecret_key = local.l_secret_key\n" +
                        "\ttoken = local.l_token\n" +
                        "}\n"
        );
        // define cloudwatch logs
        stringBuilder.append(
                "resource \"aws_cloudwatch_log_group\" \"terraform_ecs_log\" {\n" +
                        "\tname = local.l_log_group_name\n" +
                        "\tretention_in_days = local.l_retention_in_days\n" +
                        "}\n"
        );

        // output
        stringBuilder.append(
                "output \"log_group\" {\n" +
                        "\tvalue = aws_cloudwatch_log_group.terraform_ecs_log.arn\n" +
                        "}\n"
        );

        stringBuilder.append(
                "resource \"aws_ecs_cluster\" \"" + clusterName + "\" {\n" +
                        "\tname = local.l_cluster_name\n" +
                        "\tconfiguration {\n" +
                        "\t\texecute_command_configuration {\n" +
                        "\t\t\t\tkms_key_id = local.l_kms_key_id\n" +
                        "\t\t\t\tlogging = \"OVERRIDE\"\n" +
                        "\t\t\t\tlog_configuration {\n" +
                        "\t\t\t\tcloud_watch_encryption_enabled = true\n" +
                        "\t\t\t\tcloud_watch_log_group_name = aws_cloudwatch_log_group.terraform_ecs_log.name\n" +
                        "\t\t\t}\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "}\n"
        );

        stringBuilder.append(
                "output \"cluster_arn\" {\n" +
                        "\tvalue = aws_ecs_cluster." + clusterName + ".arn\n" +
                        "}\n"
        );

        // taskdefinition
        stringBuilder.append(
                "resource \"aws_ecs_task_definition\" \"terraform_task" + this.getUniqueSuffix() + "\" {\n" +
                        "\tfamily = local.l_family\n" +
                        "\tcontainer_definitions = jsonencode([\n" +
                        "\t\t{\n" +
                        "\t\t\tname = local.l_function_name\n" +
                        "\t\t\timage = local.l_docker_image\n" +
                        "\t\t\tcpu = 10\n" +
                        "\t\t\tmemory = 512\n" +
                        "\t\t\tessential = true\n" +
                        "\t\t\tlogConfiguration: {\n" +
                        "\t\t\t\tlogDriver: \"awslogs\",\n" +
                        "\t\t\t\toptions: {\n" +
                        "\t\t\t\t\tawslogs-group : local.l_log_group_name,\n" +
                        "\t\t\t\t\tawslogs-region : local.l_region,\n" +
                        "\t\t\t\t\tawslogs-stream-prefix : local.l_log_group_prefix\n" +
                        "\t\t\t\t}\n" +
                        "\t\t\t}\n" +
                        "\t\t}\n" +
                        "\t])\n" +
                        "\tcpu = 512\n" +
                        "\tmemory = 4096\n" +
                        "\trequires_compatibilities = [\"FARGATE\"]\n" +
                        "\tnetwork_mode = \"awsvpc\"\n" +
                        "\texecution_role_arn = local.l_execution_role_arn\n" +
                        "\ttask_role_arn = local.l_task_role_arn\n" +
                        "}\n"
        );
        stringBuilder.append(
                "output \"aws_ecs_task_arn\" {\n" +
                        "\tvalue = aws_ecs_task_definition.terraform_task" + this.getUniqueSuffix() + ".task_role_arn\n" +
                        "}\n"
        );
        // servicesettings
        stringBuilder.append(
                "resource \"aws_ecs_service\" \"terraform_service" + this.getUniqueSuffix() + "\" {\n" +
                        "\tname = local.l_service_name\n" +
                        "\tcluster = aws_ecs_cluster." + clusterName + ".id\n" +
                        "\ttask_definition = aws_ecs_task_definition.terraform_task" + this.getUniqueSuffix() + ".arn\n" +
                        "\tdesired_count = 1\n" +
                        "\tlaunch_type = \"FARGATE\"\n" +
                        "\tnetwork_configuration {\n" +
                        "\t\tsubnets = [local.l_subnet]\n" +
                        "\t\tsecurity_groups  = [local.l_vpc_security_group]\n" +
                        "\t\tassign_public_ip = true\n" +
                        "\t}\n" +
                        "}\n"
        );

        this.createFile(this.getWorkingDirectory() + this.getFunctionDefinition().getFunctionName() + ".tf", stringBuilder.toString());
    }

    /**
     * executes the created terraform script
     */
    public void runTerraformScript() throws IOException {
        this.createAWSTerraformScript();

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
    }

    /**
     * send and execute commands in terraform-docker-container
     *
     * @param commands
     */
    private void execTerraformCommand(final String... commands) {
        for (final String command : commands) {
            this.dockerContainerTerraformManager.sendCommandToContainer(command);
        }
    }

    private String getUniqueSuffix() {
        return "_" + this.getFunctionDefinition().getFunctionName() + "_" + CredentialsProperties.localUser;
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
