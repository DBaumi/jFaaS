package jFaaS;

import com.amazonaws.regions.Regions;
import com.google.gson.JsonObject;
import jContainer.invoker.ContainerInvoker;
import jFaaS.invokers.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Gateway implements FaaSInvoker {

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

    private ContainerInvoker dockerInvoker;

    private final static Logger LOGGER = Logger.getLogger(Gateway.class.getName());

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
                if (properties.containsKey("aws_session_token")) {
                    this.awsSessionToken = properties.getProperty("aws_session_token");
                }
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
     * Invoke a cloud function.
     *
     * @param function       identifier of the function
     * @param functionInputs input parameters
     * @return json result
     * @throws IOException on failure
     */
    @Override
    public JsonObject invokeFunction(final String function, final Map<String, Object> functionInputs) throws IOException {
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


            return this.azureInvoker.invokeFunction(function, functionInputs);


        } else if (function.contains("fc.aliyuncs.com")) {
            // TODO check for alibaba authentication. Currently no authentication is assumed
            return this.httpGETInvoker.invokeFunction(function, functionInputs);
        } else if (function.contains(":VM:")) {
            if (this.vmInvoker == null) {
                this.vmInvoker = new VMInvoker();
            }
            return this.vmInvoker.invokeFunction(function, functionInputs);
        } else if (function.contains(":container:")) {
            if (this.containerInvoker == null) {
                this.containerInvoker = new ContainerInvoker();
            }
            return this.containerInvoker.invokeFunction(function, functionInputs);
        }
        return null;
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
}
