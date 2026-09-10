package com.my.craft.filestorage.config;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds the top-level {@code file-storage:} YAML key. See docs/file-storage.md.
 */
@ConfigurationProperties(prefix = "file-storage")
public class FileStorageProperties {

    /** Bucket that holds every object this module writes/presigns/deletes. */
    private String bucket;

    /** Prefix prepended to every generated object key, e.g. {@code uploads}. */
    private String keyPrefix = "uploads";

    private final Upload upload = new Upload();
    private final Download download = new Download();
    private final Temp temp = new Temp();
    private final S3 s3 = new S3();

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public Upload getUpload() {
        return upload;
    }

    public Download getDownload() {
        return download;
    }

    public Temp getTemp() {
        return temp;
    }

    public S3 getS3() {
        return s3;
    }

    public static class Upload {

        /** How long a presigned upload URL stays valid after it's issued. */
        private Duration expiration = Duration.ofMinutes(15);

        public Duration getExpiration() {
            return expiration;
        }

        public void setExpiration(Duration expiration) {
            this.expiration = expiration;
        }
    }

    public static class Download {

        /** How long a presigned download URL stays valid after it's issued. */
        private Duration expiration = Duration.ofMinutes(10);

        public Duration getExpiration() {
            return expiration;
        }

        public void setExpiration(Duration expiration) {
            this.expiration = expiration;
        }
    }

    public static class Temp {

        /** Age after which an unconfirmed temp upload is eligible for cleanup. */
        private Duration ttl = Duration.ofHours(24);

        /** Cron expression the cleanup schedule runs on. Default: top of every hour. */
        private String cleanupCron = "0 0 * * * *";

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }

        public String getCleanupCron() {
            return cleanupCron;
        }

        public void setCleanupCron(String cleanupCron) {
            this.cleanupCron = cleanupCron;
        }
    }

    /**
     * S3-compatible client settings. Leave {@code endpoint} unset to talk to real AWS S3; point it
     * at a MinIO (or other S3-compatible) host to switch provider without touching any code --
     * see docs/file-storage.md.
     */
    public static class S3 {

        private String region = "us-east-1";
        private URI endpoint;
        private boolean pathStyleAccessEnabled = false;
        private String accessKey;
        private String secretKey;

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public URI getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(URI endpoint) {
            this.endpoint = endpoint;
        }

        public boolean isPathStyleAccessEnabled() {
            return pathStyleAccessEnabled;
        }

        public void setPathStyleAccessEnabled(boolean pathStyleAccessEnabled) {
            this.pathStyleAccessEnabled = pathStyleAccessEnabled;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }
    }
}
