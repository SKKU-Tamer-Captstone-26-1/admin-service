package com.ontheblock.admin.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ImpersonatedCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Generates V4 signed upload URLs for product (beverage) images.
 * Mirrors authorization-service ProfileStorageService and shares the
 * map-service product media bucket / object convention
 * (bucket on-the-block-product-media, object path whiskies/{productId}/{uuid}.jpg).
 */
@Slf4j
@Service
public class ProductMediaStorageService {

    private static final String OBJECT_PREFIX = "whiskies/";
    // The proto request has no content_type field, so the upload is fixed to JPEG
    // (consistent with the authorization-service profile upload contract).
    private static final String CONTENT_TYPE = "image/jpeg";
    private static final String EXTENSION = "jpg";
    private static final long SIGNED_URL_EXPIRY_MINUTES = 15;

    @Value("${app.product-media-bucket:on-the-block-product-media}")
    private String bucket;

    // Cloud Run: 비워두면 attached 서비스 계정으로 직접 서명.
    // 로컬: SIGNING_SERVICE_ACCOUNT 환경변수에 서비스 계정 이메일을 설정하면 impersonation으로 서명.
    @Value("${app.signing-service-account:}")
    private String signingServiceAccount;

    /**
     * @param productId product UUID; blank when uploading for a new (not-yet-created) product,
     *                  in which case a fresh UUID is allocated for the object path.
     */
    public UploadUrlResult generateUploadUrl(String productId) {
        String effectiveProductId;
        if (productId == null || productId.isBlank()) {
            effectiveProductId = UUID.randomUUID().toString();
        } else {
            // Validate/canonicalize to prevent object-name path injection.
            effectiveProductId = UUID.fromString(productId).toString();
        }
        String objectName = OBJECT_PREFIX + effectiveProductId + "/" + UUID.randomUUID() + "." + EXTENSION;

        BlobInfo blobInfo = BlobInfo.newBuilder(bucket, objectName)
                .setContentType(CONTENT_TYPE)
                .build();

        Storage storage = buildStorage();
        URL signedUrl = storage.signUrl(
                blobInfo,
                SIGNED_URL_EXPIRY_MINUTES,
                TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                Storage.SignUrlOption.withContentType(),
                Storage.SignUrlOption.withV4Signature()
        );

        String objectUrl = "https://storage.googleapis.com/" + bucket + "/" + objectName;
        log.debug("Generated product image upload URL for product={}", effectiveProductId);
        return new UploadUrlResult(signedUrl.toString(), objectUrl);
    }

    private Storage buildStorage() {
        if (signingServiceAccount == null || signingServiceAccount.isBlank()) {
            return StorageOptions.getDefaultInstance().getService();
        }
        try {
            GoogleCredentials source = GoogleCredentials.getApplicationDefault()
                    .createScoped("https://www.googleapis.com/auth/cloud-platform");
            ImpersonatedCredentials impersonated = ImpersonatedCredentials.create(
                    source,
                    signingServiceAccount,
                    null,
                    List.of("https://www.googleapis.com/auth/cloud-platform"),
                    300
            );
            log.info("Using impersonated credentials for signing: {}", signingServiceAccount);
            return StorageOptions.newBuilder().setCredentials(impersonated).build().getService();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create impersonated credentials for " + signingServiceAccount, e);
        }
    }

    public record UploadUrlResult(String uploadUrl, String objectUrl) {}
}
