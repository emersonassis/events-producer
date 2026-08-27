package br.com.outbox.sender.message.sqs;

/**
 * Factory estática que constrói o SqsClient com região e credenciais de ambiente.
 */
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

public class SqsClientBuilder {

    public static SqsClient buildSqsClient(String region) throws SdkClientException {
        if (region.isBlank())
            region = "us-east-1";

        return SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }
}
