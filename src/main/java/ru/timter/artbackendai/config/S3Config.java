package ru.timter.artbackendai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${s3.endpoint}")
    private String endpoint;

    @Value("${s3.public-endpoint}")
    private String publicEndpoint;

    @Value("${s3.region}")
    private String region;

    @Value("${s3.access-key}")
    private String accessKey;

    @Value("${s3.secret-key}")
    private String secretKey;

    private StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    }

    @Bean
    public S3Client s3Client() {
        // Internal endpoint — used for server-side uploads (backend -> MinIO).
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(credentials())
                .forcePathStyle(true) // Required for MinIO
                .build();
    }

    /**
     * Presigner that signs URLs with the INTERNAL endpoint (e.g. http://minio:9000).
     * Consumed by the Python worker, which lives on the same Docker network.
     */
    @Bean
    public S3Presigner s3PresignerInternal() {
        return buildPresigner(endpoint);
    }

    /**
     * Presigner that signs URLs with the PUBLIC endpoint (e.g. http://localhost:9000
     * or http://your-server:9000). Consumed by the user's browser.
     */
    @Bean
    public S3Presigner s3PresignerPublic() {
        return buildPresigner(publicEndpoint);
    }

    private S3Presigner buildPresigner(String uri) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(uri))
                .region(Region.of(region))
                .credentialsProvider(credentials())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}
