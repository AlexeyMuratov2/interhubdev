package com.example.interhubdev.document.internal.storedFile;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for MinIO client.
 */
@Configuration
@Slf4j
class MinioConfig {
    
    @Value("${app.storage.endpoint}")
    private String endpoint;
    
    @Value("${app.storage.access-key}")
    private String accessKey;
    
    @Value("${app.storage.secret-key}")
    private String secretKey;
    
    @Bean
    MinioClient minioClient() {
        log.info("Initializing MinIO client with endpoint: {}", endpoint);
        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build();
    }
}
