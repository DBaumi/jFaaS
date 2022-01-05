package jContainer.management;

import jContainer.helper.CredentialsProperties;
import jContainer.helper.FunctionDefinition;

import java.io.IOException;


public class TerraformGkeManager extends AbstractTerraformManager {


    public TerraformGkeManager(final FunctionDefinition functionDefinition) {
        super(functionDefinition);
    }

    /**
     * defines the structure for a terraform file for gke
     * but terraform apply -auto-approve takes too much time ca.20min therefore it throws an error which results in
     * not being able to make a cleanup with terraform destroy, cleanup has to be done manually
     */
    @Override
    protected void createTerraformScript() {

        final String containerName = "jcontainer" + this.getUniqueSuffix().toLowerCase();

        // following strings should be in the credentials-file
        final String pathToGkeCred = "./google_cred.json";
        final String containerImage = "albertneuner/jcontainer:fibonacci";
        final String region = "us-central1";
        final String machineType = "g1-small";
        final String gkeProjectId = "projectid";
        // Google gke serviceaccount has to have the role or permission "owner"
        final String serviceAccount = "serviceaccount@yourserviceaccount.iam.gserviceaccount.com";
        final StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(
                "locals {\n" +
                        "\tregion = \"" + region + "\"\n" +
                        "\tproject = \"" + gkeProjectId + "\"\n" +
                        "\tname = \"" + containerName + "\"\n" +
                        "\tmachine_type = \"" + machineType + "\"\n" +
                        "\tinitial_node_count = 1\n" +
                        "\tdocker_image = \"" + containerImage + "\"\n" +
                        "\tservice_ac_email = \"" + serviceAccount + "\"\n" +
                        "}\n"
        );
        stringBuilder.append(
                "provider \"google\" {\n" +
                        "\tcredentials = file(\"" + pathToGkeCred + "\")\n" +
                        "\tproject = local.project\n" +
                        "\tregion = local.region\n" +
                        "}\n"
        );
        stringBuilder.append(
                "resource \"google_container_cluster\" \"default\" {\n" +
                        "\tname = local.name\n" +
                        "\tproject = local.project\n" +
                        "\tdescription = \"${local.name}-cluster\"\n" +
                        "\tlocation = local.region\n" +
                        "\tremove_default_node_pool = true\n" +
                        "\tinitial_node_count = local.initial_node_count\n" +
                        "\tmaster_auth {\n" +
                        "\t\tclient_certificate_config {\n" +
                        "\t\t\tissue_client_certificate = false\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "}\n"
        );

        stringBuilder.append(
                "resource \"google_container_node_pool\" \"default\" {\n" +
                        "\tname = \"${local.name}-node-pool\"\n" +
                        "\tproject = local.project\n" +
                        "\tlocation = local.region\n" +
                        "\tcluster = google_container_cluster.default.name\n" +
                        "\tnode_count = 1\n" +
                        "\tnode_config {\n" +
                        "\t\tservice_account = local.service_ac_email\n" +
                        "\t\tpreemptible  = true\n" +
                        "\t\tmachine_type = local.machine_type\n" +
                        "\t\tmetadata = {\n" +
                        "\t\t\tdisable-legacy-endpoints = \"true\"\n" +
                        "\t\t}\n" +
                        "\t\toauth_scopes = [\n" +
                        "\t\t\t\"https://www.googleapis.com/auth/logging.write\",\n" +
                        "\t\t\t\"https://www.googleapis.com/auth/monitoring\",\n" +
                        "\t\t]\n" +
                        "\t}\n" +
                        "}\n"
        );

        stringBuilder.append(
                "data \"google_client_config\" \"default\" {}\n"
        );
        stringBuilder.append(
                "provider \"kubernetes\" {\n" +
                        "\thost  = \"https://${google_container_cluster.default.endpoint}\"\n" +
                        "\ttoken = data.google_client_config.default.access_token\n" +
                        "\tcluster_ca_certificate = base64decode(\n" +
                        "\tgoogle_container_cluster.default.master_auth[0].cluster_ca_certificate,\n" +
                        "\t)\n" +
                        "}\n"

        );
        stringBuilder.append(
                "resource \"kubernetes_deployment\" \"fib_test_1450_11122021\" {\n" +
                        "\tmetadata {\n" +
                        "\t\tname = local.name\n" +
                        "\t\tnamespace = \"default\"\n" +
                        "\t\tlabels = {\n" +
                        "\t\t\tapp = local.name\n" +
                        "\t\t}\n" +
                        "\t\tannotations = {\n" +
                        "\t\t\t\"deployment.kubernetes.io/revision\" = \"1\"\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "\tspec {\n" +
                        "\t\treplicas = 3\n" +
                        "\t\tselector {\n" +
                        "\t\t\tmatch_labels = {\n" +
                        "\t\t\t\tapp = local.name\n" +
                        "\t\t\t}\n" +
                        "\t\t}\n" +
                        "\t\ttemplate {\n" +
                        "\t\t\tmetadata {\n" +
                        "\t\t\t\tlabels = {\n" +
                        "\t\t\t\t\tapp = local.name\n" +
                        "\t\t\t\t}\n" +
                        "\t\t\t}\n" +
                        "\t\t\tspec {\n" +
                        "\t\t\t\tcontainer {\n" +
                        "\t\t\t\t\tname = local.name\n" +
                        "\t\t\t\t\timage = local.docker_image\n" +
                        "\t\t\t\t\ttermination_message_path = \"/dev/termination-log\"\n" +
                        "\t\t\t\t\ttermination_message_policy = \"File\"\n" +
                        "\t\t\t\t\timage_pull_policy = \"IfNotPresent\"\n" +
                        "\t\t\t\t}\n" +
                        "\t\t\t\trestart_policy = \"Always\"\n" +
                        "\t\t\t\ttermination_grace_period_seconds = 30\n" +
                        "\t\t\t\tdns_policy = \"ClusterFirst\"\n" +
                        "\t\t\t}\n" +
                        "\t\t}\n" +
                        "\t\tstrategy {\n" +
                        "\t\t\ttype = \"RollingUpdate\"\n" +
                        "\t\t\trolling_update {\n" +
                        "\t\t\t\tmax_unavailable = \"25%\"\n" +
                        "\t\t\t\tmax_surge = \"25%\"\n" +
                        "\t\t\t}\n" +
                        "\t\t}\n" +
                        "\t\trevision_history_limit = 10\n" +
                        "\t\tprogress_deadline_seconds = 600\n" +
                        "\t}\n" +
                        "}\n"
        );
        this.createFile(this.getWorkingDirectory() + this.getFunctionDefinition().getFunctionName() + ".tf", stringBuilder.toString());
    }

}
