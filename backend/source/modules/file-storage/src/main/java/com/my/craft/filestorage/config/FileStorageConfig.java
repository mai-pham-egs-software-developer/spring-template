package com.my.craft.filestorage.config;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.my.craft.filestorage.storage.S3StorageProvider;
import com.my.craft.filestorage.storage.StorageProvider;

/**
 * Wires the S3-compatible client/presigner from {@code file-storage.s3.*} and exposes the default
 * {@link StorageProvider} bean. Leaving {@code s3.endpoint} unset targets real AWS S3; pointing it
 * at a MinIO (or other S3-compatible) host switches the backend without any code change -- see
 * docs/file-storage.md.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageConfig {

    @Bean
    public S3Client s3Client(FileStorageProperties properties) {
        FileStorageProperties.S3 s3 = properties.getS3();
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(credentialsProvider(s3))
                .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(s3.isPathStyleAccessEnabled()).build());
        if (s3.getEndpoint() != null) {
            builder.endpointOverride(s3.getEndpoint());
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(FileStorageProperties properties) {
        FileStorageProperties.S3 s3 = properties.getS3();
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(s3.getRegion()))
                .credentialsProvider(credentialsProvider(s3))
                .serviceConfiguration(
                        S3Configuration.builder().pathStyleAccessEnabled(s3.isPathStyleAccessEnabled()).build());
        if (s3.getEndpoint() != null) {
            builder.endpointOverride(s3.getEndpoint());
        }
        return builder.build();
    }

    @Bean
    public StorageProvider storageProvider(S3Client s3Client, S3Presigner s3Presigner, FileStorageProperties properties) {
        return new S3StorageProvider(s3Client, s3Presigner, properties.getBucket());
    }

    private static AwsCredentialsProvider credentialsProvider(FileStorageProperties.S3 s3) {
        if (s3.getAccessKey() != null && s3.getSecretKey() != null) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey()));
        }
        return DefaultCredentialsProvider.create();
    }
}
