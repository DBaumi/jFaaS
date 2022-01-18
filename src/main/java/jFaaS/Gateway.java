package jFaaS;

import com.amazonaws.regions.Regions;
import jContainer.helper.Constants;
import jContainer.helper.CredentialsProperties;
import jContainer.invoker.*;
import jContainer.management.TerminalManager;
import jFaaS.invokers.*;
import jFaaS.utils.PairResult;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Gateway implements FaaSInvoker {

    private final static Logger LOGGER = Logger.getLogger(Gateway.class.getName());
    private FaaSInvoker lambdaInvoker;
    private String awsAccessKey;
    private String awsSecretKey;
    private String awsSessionToken;
    private Regions currentRegion;
    private FaaSInvoker openWhiskInvoker;
    private String openWhiskKey;
    private FaaSInvoker googleFunctionInvoker;
    private String googleServiceAccountKey;
    private String googleToken;
    private FaaSInvoker azureInvoker;
    private String azureKey;
    private final FaaSInvoker httpGETInvoker;
    private VMInvoker vmInvoker;
    private ContainerInvoker localContainerInvoker;
    private ContainerInvoker ecsContainerInvoker;

    /**
     * Gateway.
     *
     * @param credentialsFile contains credentials for FaaS providers
     */
    public Gateway(final String credentialsFile) {
        final Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(credentialsFile));
            if (properties.containsKey("aws_access_key") && properties.containsKey("aws_secret_key")) {
                this.awsAccessKey = properties.getProperty("aws_access_key");
                this.awsSecretKey = properties.getProperty("aws_secret_key");
                this.awsSessionToken = !jContainer.helper.Utils.isNullOrEmpty(properties.getProperty("aws_session_token")) ? properties.getProperty("aws_session_token") : null;

            }
            if (properties.containsKey("ibm_api_key")) {
                this.openWhiskKey = properties.getProperty("ibm_api_key");
            }

            if (properties.containsKey("google_sa_key")) {
                this.googleServiceAccountKey = properties.getProperty("google_sa_key");
            }
            if (properties.containsKey("google_token")) {
                this.googleServiceAccountKey = properties.getProperty("google_token");
            }

            if (properties.containsKey("azure_key")) {
                this.azureKey = properties.getProperty("azure_key");
            }

        } catch (final IOException e) {
            Gateway.LOGGER.log(Level.WARNING, "Could not load credentials file.");
        }
        this.httpGETInvoker = new HTTPGETInvoker();

    }


    /**
     * Gateway.
     */
    public Gateway() {
        this.httpGETInvoker = new HTTPGETInvoker();
    }

    /**
     * Detect aws lambda region
     *
     * @param function arn
     * @return region
     */
    private static Regions detectRegion(final String function) {
        String regionName;
        final int searchIndex = function.indexOf("lambda:");
        if (searchIndex != -1) {
            regionName = function.substring(searchIndex + "lambda:".length());
            regionName = regionName.split(":")[0];
            try {
                return Regions.fromName(regionName);
            } catch (final Exception e) {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Invoke a cloud function.
     *
     * @param function       identifier of the function
     * @param functionInputs input parameters
     *
     * @return json result
     *
     * @throws IOException on failure
     */
    @Override
    public PairResult<String, Long> invokeFunction(final String function, final Map<String, Object> functionInputs) throws Exception {
        if (function.contains("container")) {
            if (function.contains("local")) {
                this.localContainerInvoker = new LocalContainerInvoker();
                return this.localContainerInvoker.invokeFunction(function, functionInputs);
            } else if (function.contains("ecs")) {

                final List<String> response = TerminalManager.executeCommand(null, true,
                        Constants.aws_cmd + " configure set aws_access_key_id " + CredentialsProperties.awsAccessKey,
                        Constants.aws_cmd + " configure set aws_secret_access_key " + CredentialsProperties.awsSecretKey,
                        CredentialsProperties.basicSessionCredentials == null ? "" : Constants.aws_cmd + " configure set aws_session_token " + CredentialsProperties.awsSessionToken,
                        Constants.aws_cmd + " configure set region " + CredentialsProperties.awsRegion
                );

                response.forEach(s -> {
                    final String errAWSnotFound = "not found";
                    final String errAWSnichtGefunden = "nicht gefunden";
                    final String err = "Error";
                    if (s.contains(errAWSnotFound) || s.contains(errAWSnichtGefunden) || s.contains(err)) {
                        throw new RuntimeException("AWS CLI is not installed or aws.exe not added to PATH!");
                    }
                });

                this.ecsContainerInvoker = new EcsContainerInvoker();
                return this.ecsContainerInvoker.invokeFunction(function, functionInputs);
            } else if (function.contains("gke")) {
                // WIP: open for future work
            }
        }

        if (function.contains("arn:") && this.awsSecretKey != null && this.awsAccessKey != null) {
            final Regions tmpRegion = Gateway.detectRegion(function);
/*            if(lambdaInvoker == null || tmpRegion != currentRegion){
                currentRegion = tmpRegion;
                lambdaInvoker = new LambdaInvoker(awsAccessKey, awsSecretKey, awsSessionToken, currentRegion);
            }
*/
            this.lambdaInvoker = new LambdaInvoker(this.awsAccessKey, this.awsSecretKey, this.awsSessionToken, tmpRegion);
            return this.lambdaInvoker.invokeFunction(function, functionInputs);

        } else if (function.contains("functions.appdomain.cloud") || function.contains("functions.cloud.ibm")) {
            if (this.openWhiskKey != null) {
                if (this.openWhiskInvoker == null) {
                    this.openWhiskInvoker = new OpenWhiskInvoker(this.openWhiskKey);
                }
            } else {
                if (this.openWhiskInvoker == null) {
                    this.openWhiskInvoker = new OpenWhiskInvoker("");
                }
            }
            return this.openWhiskInvoker.invokeFunction(function.endsWith(".json") ? function : function + ".json", functionInputs);
        } else if (function.contains("cloudfunctions.net")) {
            if (this.googleServiceAccountKey != null) {
                if (this.googleFunctionInvoker == null) {
                    this.googleFunctionInvoker = new GoogleFunctionInvoker(this.googleServiceAccountKey, "serviceAccount");
                }
            } else if (this.googleToken != null) {
                if (this.googleFunctionInvoker == null) {
                    this.googleFunctionInvoker = new GoogleFunctionInvoker(this.googleToken, "token");
                }
            } else {
                if (this.googleFunctionInvoker == null) {
                    this.googleFunctionInvoker = new GoogleFunctionInvoker();
                }
            }
            return this.googleFunctionInvoker.invokeFunction(function, functionInputs);

        } else if (function.contains("azurewebsites.net")) {
            if (this.azureKey != null) {
                if (this.azureInvoker == null) {
                    this.azureInvoker = new AzureInvoker(this.azureKey);
                }

            } else {

                if (this.azureInvoker == null) {

                    this.azureInvoker = new AzureInvoker();
                }
            }

        } else if (function.contains("fc.aliyuncs.com")) {
            // TODO check for alibaba authentication. Currently no authentication is assumed
            return this.httpGETInvoker.invokeFunction(function, functionInputs);
        } else if (function.contains(":VM:")) {
            if (this.vmInvoker == null) {
                this.vmInvoker = new VMInvoker();
            }
            return this.vmInvoker.invokeFunction(function, functionInputs);
        }

        return null;
    }

    /**
     * Returns the assigned memory of a function.
     *
     * @param function to return the memory from
     * @return the amount of memory in MB or -1 if the provider is unsupported
     */
    public Integer getAssignedMemory(final String function) {
        if (function.contains("arn:") && this.awsSecretKey != null && this.awsAccessKey != null) {
            final Regions tmpRegion = Gateway.detectRegion(function);
            if (this.lambdaInvoker == null || tmpRegion != this.currentRegion) {
                this.currentRegion = tmpRegion;
                this.lambdaInvoker = new LambdaInvoker(this.awsAccessKey, this.awsSecretKey, this.awsSessionToken, this.currentRegion);
            }
            return ((LambdaInvoker) this.lambdaInvoker).getAssignedMemory(function);
        }
        // TODO implement for different providers
        Gateway.LOGGER.log(Level.WARNING, "Getting the assigned memory is currently not supported for your provider.");
        return -1;
    }
}
