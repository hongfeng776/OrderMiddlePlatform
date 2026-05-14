package com.orderplatform.order.service;

import com.orderplatform.order.config.MinIOConfig;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinIOService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinIOConfig minIOConfig;

    public void initBucket() {
        try {
            boolean found = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minIOConfig.getBucketName()).build()
            );
            if (!found) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(minIOConfig.getBucketName()).build()
                );
                log.info("创建MinIO bucket成功: {}", minIOConfig.getBucketName());
            }
        } catch (Exception e) {
            log.error("初始化MinIO bucket失败: {}", e.getMessage(), e);
            throw new RuntimeException("MinIO初始化失败: " + e.getMessage(), e);
        }
    }

    public String uploadFile(String fileName, byte[] data, String contentType) {
        initBucket();
        String objectName = generateObjectName(fileName);
        String md5Hash = calculateMD5(data);
        log.info("开始上传文件: {}, 大小: {}KB, MD5: {}", 
                objectName, data.length / 1024, md5Hash);

        int retryCount = 0;
        int maxRetries = 3;
        while (retryCount < maxRetries) {
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-MD5", md5Hash);
                headers.put("x-amz-meta-original-filename", fileName);
                headers.put("x-amz-meta-upload-time", LocalDateTime.now().toString());

                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minIOConfig.getBucketName())
                                .object(objectName)
                                .stream(inputStream, data.length, -1)
                                .contentType(contentType)
                                .userMetadata(headers)
                                .build()
                );

                verifyUpload(objectName, data.length, md5Hash);
                log.info("文件上传成功: {}", objectName);
                return objectName;

            } catch (Exception e) {
                retryCount++;
                log.error("文件上传失败，第 {} 次重试，objectName: {}, 错误: {}", 
                        retryCount, objectName, e.getMessage(), e);
                if (retryCount >= maxRetries) {
                    throw new RuntimeException("文件上传失败，已重试 " + maxRetries + " 次: " + e.getMessage(), e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new RuntimeException("文件上传失败");
    }

    private void verifyUpload(String objectName, long expectedSize, String expectedMd5) throws Exception {
        StatObjectResponse stat = minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(minIOConfig.getBucketName())
                        .object(objectName)
                        .build()
        );

        if (stat.size() != expectedSize) {
            throw new RuntimeException(String.format(
                    "文件大小不匹配，期望: %d，实际: %d", expectedSize, stat.size()));
        }
        log.debug("文件大小校验通过: {} bytes", stat.size());
    }

    public String getPresignedDownloadUrl(String objectName) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minIOConfig.getBucketName())
                            .object(objectName)
                            .expiry(minIOConfig.getDownloadExpiry(), TimeUnit.SECONDS)
                            .build()
            );
            log.info("生成预签名URL成功: {}", url);
            return url;
        } catch (Exception e) {
            log.error("生成预签名下载链接失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成下载链接失败: " + e.getMessage(), e);
        }
    }

    public InputStream downloadFile(String objectName) {
        try {
            log.info("开始下载文件: {}", objectName);
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minIOConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("从MinIO下载文件失败: {}, 错误: {}", objectName, e.getMessage(), e);
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    public byte[] downloadFileAsBytes(String objectName) {
        try (InputStream is = downloadFile(objectName)) {
            return is.readAllBytes();
        } catch (Exception e) {
            log.error("下载文件为字节数组失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件下载失败: " + e.getMessage(), e);
        }
    }

    private String generateObjectName(String originalFileName) {
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String timestamp = now.format(DateTimeFormatter.ofPattern("HHmmssSSS"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = "";
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFileName.substring(lastDotIndex);
        }
        return String.format("exports/%s/%s_%s_%s%s", 
                datePath, timestamp, uuid, 
                originalFileName.replace(extension, ""), extension);
    }

    private String calculateMD5(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            log.warn("计算MD5失败: {}", e.getMessage());
            return "";
        }
    }

    public String getDownloadUrl(String objectName) {
        try {
            boolean exist = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(minIOConfig.getBucketName()).build()
            );
            if (!exist) {
                throw new RuntimeException("Bucket不存在");
            }

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minIOConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
            log.info("文件存在: {}，大小: {} bytes", objectName, stat.size());
            return getPresignedDownloadUrl(objectName);
        } catch (Exception e) {
            log.error("获取下载URL失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取下载链接失败: " + e.getMessage(), e);
        }
    }
}
