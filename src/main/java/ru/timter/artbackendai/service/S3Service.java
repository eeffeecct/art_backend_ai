package ru.timter.artbackendai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
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
    private final S3Presigner s3Presigner;

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

    public String generatePresignedUrl(String key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(builder -> builder.bucket(bucket).key(key).build())
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public String generatePresignedUrlForDataset(String key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(60)) // 1 hour for UI viewing
                .getObjectRequest(builder -> builder.bucket("dataset").key(key).build())
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public String getPublicUrl(String key) {
        return String.format("%s/%s/%s", publicUrl, bucket, key);
    }
}
