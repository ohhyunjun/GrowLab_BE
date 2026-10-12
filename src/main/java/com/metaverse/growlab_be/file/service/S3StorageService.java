package com.metaverse.growlab_be.file.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    @Value("${app.storage.type:local}")
    private String storageType;

    @Value("${aws.s3.bucket:}")
    private String bucket;

    @Value("${aws.s3.region:ap-northeast-2}")
    private String region;

    @Value("${aws.s3.public-base-url:}")
    private String publicBaseUrl;

    public boolean isEnabled() {
        return "s3".equalsIgnoreCase(storageType);
    }

    public UploadResult upload(MultipartFile file, String folder) {
        if (!isEnabled()) {
            throw new IllegalStateException("S3 storage is not enabled.");
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("AWS_S3_BUCKET is required when APP_STORAGE_TYPE=s3.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 비어 있습니다.");
        }

        String originalFileName = file.getOriginalFilename();
        String extension = getExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + extension;
        String key = normalizeFolder(folder) + "/" + storedFileName;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(resolveContentType(file))
                .contentLength(file.getSize())
                .build();

        try (S3Client s3 = S3Client.builder().region(Region.of(region)).build()) {
            s3.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new RuntimeException("S3 업로드 중 파일을 읽지 못했습니다.", e);
        }

        return new UploadResult(originalFileName, storedFileName, key, publicUrl(key));
    }

    private String publicUrl(String key) {
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            return publicBaseUrl.replaceAll("/+$", "") + "/" + key;
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }

    private String normalizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "uploads";
        }
        return folder.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }
        String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        return extension.matches("\\.[a-z0-9]{1,10}") ? extension : ".jpg";
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return (contentType == null || contentType.isBlank()) ? "application/octet-stream" : contentType;
    }

    public record UploadResult(String originalFileName, String storedFileName, String key, String url) {
    }
}
