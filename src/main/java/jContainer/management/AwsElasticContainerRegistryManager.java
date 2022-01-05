package jContainer.management;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.ecr.AmazonECR;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.*;
import jContainer.executor.AwsContainerExecutor;
import jContainer.helper.Constants;
import jContainer.helper.CredentialsProperties;
import jContainer.helper.FunctionDefinition;

import java.util.List;

public class AwsElasticContainerRegistryManager extends AbstractManager {

    private final Repository repository;
    private final String repoLink;
    private final AmazonECR amazonECR;
    private final String repoName = "jcontainer_ecr";

    public AwsElasticContainerRegistryManager(final FunctionDefinition functionDefinition) {
        super(functionDefinition);
        this.amazonECR = this.setClient();

        if (CredentialsProperties.repoLink == null || CredentialsProperties.repoLink.isEmpty()) {
            this.repository = this.createEcrRepo();
            this.repoLink = this.repository.getRepositoryUri();
        } else {
            this.repository = null;
            this.repoLink = CredentialsProperties.repoLink;
        }
    }

    /**
     * creates an AmazonECR client
     *
     * @return
     */
    private AmazonECR setClient() {
        return AmazonECRClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(CredentialsProperties.credentials))
                .withRegion(CredentialsProperties.awsRegion)
                .build();
    }

    /**
     * creates an AWS ECR repository
     *
     * @return ECR Repository URI
     */
    public Repository createEcrRepo() {

        final ImageScanningConfiguration configuration = new ImageScanningConfiguration();
        configuration.setScanOnPush(false);

        final DescribeRepositoriesRequest describeRepositoriesRequest = new DescribeRepositoriesRequest().withRepositoryNames(this.repoName);
        DescribeRepositoriesResult describeRepositoryResult = null;

        try {
            describeRepositoryResult = this.amazonECR.describeRepositories(describeRepositoriesRequest);
        } catch (final Exception e) {
            System.out.println(this.repoName + " does not exist and has to be created");
        }

        if (describeRepositoryResult == null || describeRepositoryResult.getRepositories().isEmpty()) {
            final CreateRepositoryRequest repositoryRequest = new CreateRepositoryRequest()
                    .withRepositoryName(this.repoName)
                    .withImageTagMutability(ImageTagMutability.MUTABLE)
                    .withImageScanningConfiguration(configuration);

            final CreateRepositoryResult repositoryResult = this.amazonECR.createRepository(repositoryRequest);

            return repositoryResult.getRepository();
        }
        return describeRepositoryResult.getRepositories().get(0);
    }

    private DescribeImagesResult getImagesWithName(final String imageName) {

        final DescribeImagesRequest describeImagesRequest = new DescribeImagesRequest()
                .withRepositoryName(this.repoName)
                .withImageIds(new ImageIdentifier().withImageTag(this.getFunctionDefinition().getFunctionName().toLowerCase()));
        try {
            return this.amazonECR.describeImages(describeImagesRequest);
        } catch (final Exception e) {
            System.out.println(imageName + " does not exist");
        }
        return null;
    }

    /**
     * pushes image onto AWS ECR Repository
     *
     * @param imageName AWS ECR URI:Imagetag
     */
    public void pushImageToRepo(final String imageName) {

        final String ecrID = this.repoLink.split("/")[0];
        final String commandLinux = Constants.aws_cmd + " ecr get-login-password --region " + CredentialsProperties.awsRegion + " | docker login --username AWS --password-stdin " + ecrID;

        final DescribeImagesResult describeImagesResult = this.getImagesWithName(imageName);
        if (describeImagesResult != null && describeImagesResult.getImageDetails().size() > 0) {
            AwsContainerExecutor.logger.info("Image " + imageName + " already exists and will be replaced");
//            this.removeImageFromRepo(this.getFunctionDefinition().getFunctionName().toLowerCase());
        }

        //possible error handling here
        final List<String> response = this.runCommandInWorkingDirectory(
                commandLinux,
//                "docker tag " + imageName + " " + imageNameOnECR, // only if image name and tag are different from uri
                "docker push " + imageName
        );
    }

    /**
     * pulls image from AWS ECR Repository
     *
     * @param imageName AWS ECR URI:Imagetag
     */
    public void pullImageFromRepo(final String imageName) {

        //check if image exists
        final DescribeImagesResult describeImagesResult = this.getImagesWithName(imageName);
        if (describeImagesResult == null || describeImagesResult.getImageDetails().size() == 0) {
            AwsContainerExecutor.logger.info("Image " + imageName + " does not exist on ECR Repository (" + this.repoLink + ")");
            System.exit(0);
        }

        this.runCommandInWorkingDirectory(
                "docker pull " + imageName
        );
    }

    /**
     * removing Image from AWS ECR Repository
     *
     * @param imageName AWS ECR URI:Imagetag
     */
    public void removeImageFromRepo(final String imageName) {
        final BatchDeleteImageRequest batchDeleteImageRequest = new BatchDeleteImageRequest()
                .withRepositoryName(this.repoName)
                .withImageIds(new ImageIdentifier().withImageTag(imageName));
        final BatchDeleteImageResult result = this.amazonECR.batchDeleteImage(batchDeleteImageRequest);
    }

    /**
     * removing AWS ECR Repository
     */
    public void removeRepo() {
        final DeleteRepositoryRequest deleteRepositoryRequest = new DeleteRepositoryRequest()
                .withRepositoryName(this.repoName)
                .withForce(true);
        final DeleteRepositoryResult result = this.amazonECR.deleteRepository(deleteRepositoryRequest);
    }

    public Repository getRepository() {
        return this.repository;
    }

    public String getRepoLink() {
        return this.repoLink;
    }
}
