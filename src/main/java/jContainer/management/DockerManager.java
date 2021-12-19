package jContainer.management;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import jContainer.helper.CredentialsProperties;
import jContainer.helper.FunctionDefinition;

import java.time.Duration;

public abstract class DockerManager extends AbstractManager {

    protected static DockerClient dockerClient;

    /**
     * initialize docker-client
     *
     * @param functionDefinition
     */
    public DockerManager(final FunctionDefinition functionDefinition) {
        super(functionDefinition);
    }

    static {
        final DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withRegistryUsername(CredentialsProperties.dockerUser)
                .withRegistryPassword(CredentialsProperties.dockerAccessToken)
                .build();

        final DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();
        DockerManager.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }
}
