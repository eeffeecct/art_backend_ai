package ru.timter.artbackendai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    // Two presigners: internal host for the worker, public host for the browser.
    private final S3Presigner s3PresignerInternal;
    private final S3Presigner s3PresignerPublic;

    @Value("${s3.bucket}")
    private String bucket;

    @Value("${s3.public-url}")
    private String publicUrl;

    public String uploadFile(String key, InputStream inputStream, long contentLength, String contentType) throws IOException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));

        return key;
    }

    /**
     * URL for the Python worker (signed with the internal MinIO endpoint, e.g. http://minio:9000).
     */
    public String generateWorkerUrl(String key) {
        return presign(s3PresignerInternal, bucket, key, Duration.ofMinutes(60));
    }

    /**
     * URL for the browser to display the uploaded image (public endpoint).
     */
    public String generatePresignedUrl(String key) {
        return presign(s3PresignerPublic, bucket, key, Duration.ofMinutes(60));
    }

    /**
     * URL for the browser to display a matched dataset image (public endpoint).
     */
    public String generatePresignedUrlForDataset(String key) {
        return presign(s3PresignerPublic, "dataset", key, Duration.ofMinutes(60));
    }

    private String presign(S3Presigner presigner, String bucketName, String key, Duration ttl) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(builder -> builder.bucket(bucketName).key(key).build())
                .build();
        return presigner.presignGetObject(presignRequest).url().toString();
    }

    public String getPublicUrl(String key) {
        return String.format("%s/%s/%s", publicUrl, bucket, key);
    }
}
