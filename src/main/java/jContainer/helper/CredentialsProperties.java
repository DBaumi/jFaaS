package jContainer.helper;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import org.apache.commons.lang.NullArgumentException;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;

public class CredentialsProperties {
    private static final java.util.Properties credentialProperties;

    // AWS
    public static final String awsAccessKey;
    public static final String awsSecretKey;
    public static final String awsSessionToken;
    public static final String awsSubnet;
    public static final String awsVpcSecurityGroup;
    public static final String awsExecutionRole;
    public static final String awsEncryptionKey;
    public static final String awsRegion;
    public static final String repoLink;
    public static final BasicSessionCredentials basicSessionCredentials;
    public static final BasicAWSCredentials credentials;
    public static final AWSLogsClient awsLogsClient;

    // Docker
    public static final String dockerRepo;
    public static final String dockerUser;
    public static final String dockerAccessToken;

    // Misc
    public static final String localUser;
    public static final String pathToJar;
    public static final String privateKey;

    static {
        credentialProperties = CredentialsProperties.setProperties();

        //optional
        privateKey = CredentialsProperties.credentialProperties.getProperty("private_key");
        repoLink = CredentialsProperties.credentialProperties.getProperty("ecr_repo_link");
        awsRegion = Utils.isNullOrEmpty(CredentialsProperties.credentialProperties.getProperty("aws_region")) ? Constants.region : CredentialsProperties.credentialProperties.getProperty("aws_region");
        awsSessionToken = CredentialsProperties.credentialProperties.getProperty("aws_session_token");
        pathToJar = CredentialsProperties.credentialProperties.getProperty("absolute_path_to_JAR");

        // obligatory aws
        awsAccessKey = CredentialsProperties.getMandatoryProperty("aws_access_key");
        awsSecretKey = CredentialsProperties.getMandatoryProperty("aws_secret_key");
        awsSubnet = CredentialsProperties.getMandatoryProperty("aws_subnet");
        awsVpcSecurityGroup = CredentialsProperties.getMandatoryProperty("aws_vpc_security_group");
        awsExecutionRole = CredentialsProperties.getMandatoryProperty("aws_execution_role_arn");
        awsEncryptionKey = CredentialsProperties.getMandatoryProperty("aws_encryption_key_arn");

        // obligatory docker
        // TODO not needed anymore
        dockerRepo = CredentialsProperties.getMandatoryProperty("docker_repository");
        dockerUser = CredentialsProperties.getMandatoryProperty("docker_user");
        dockerAccessToken = CredentialsProperties.getMandatoryProperty("docker_access_token");

        String hostOrUsername = CredentialsProperties.credentialProperties.getProperty("local_user");
        hostOrUsername = Utils.checkForSpecialCharacter(hostOrUsername);
        if (Utils.isNullOrEmpty(hostOrUsername)) {
            try {
                hostOrUsername = InetAddress.getLocalHost().getHostName();
            } catch (final Exception e) {
                throw new NullArgumentException("Cannot read local host name. Please specify local_user in credentials file instead");
            }
        }

        localUser = hostOrUsername;

        if (!Utils.isNullOrEmpty(CredentialsProperties.awsSessionToken)) {
            credentials = null;
            basicSessionCredentials = new BasicSessionCredentials(CredentialsProperties.awsAccessKey, CredentialsProperties.awsSecretKey, CredentialsProperties.awsSessionToken);

            awsLogsClient = (AWSLogsClient) AWSLogsClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(CredentialsProperties.basicSessionCredentials))
                    .withRegion(Regions.EU_CENTRAL_1)
                    .build();
        } else {
            basicSessionCredentials = null;
            credentials = new BasicAWSCredentials(CredentialsProperties.awsAccessKey, CredentialsProperties.awsSecretKey);

            awsLogsClient = (AWSLogsClient) AWSLogsClientBuilder.standard()
                    .withCredentials(new AWSStaticCredentialsProvider(CredentialsProperties.credentials))
                    .withRegion(Regions.EU_CENTRAL_1)
                    .build();
        }
    }

    /**
     * throw error if property is empty
     *
     * @param key
     * @return property if not empty
     */
    private static String getMandatoryProperty(final String key) {
        final String value = CredentialsProperties.credentialProperties.getProperty(key);
        if (value.isEmpty()) {
            throw new NullArgumentException("Key '" + key + "' is not set in file 'credentials.properties'");
        }
        return value;
    }

    /**
     * getting credentials out of credentials.properties
     */
    private static java.util.Properties setProperties() {
        final java.util.Properties properties = new java.util.Properties();
        BufferedInputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream("./" + Constants.Paths.credentialsFile));
            properties.load(inputStream);
            inputStream.close();

        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return properties;
    }
}