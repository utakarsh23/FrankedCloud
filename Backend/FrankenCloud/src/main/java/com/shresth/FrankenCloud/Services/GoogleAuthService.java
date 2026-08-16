package com.shresth.FrankenCloud.Services;

import com.shresth.FrankenCloud.DTO.GoogleTokenResponse;
import com.shresth.FrankenCloud.Entity.DriveAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GoogleAuthService {

    @Autowired
    private final EncryptionService encryptionService;

    @Value("${google.drive.client-id}")
    private String clientId;

    @Value("${google.drive.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    
    // In-memory token cache: accountId -> CachedToken
    private final Map<String, CachedToken> tokenCache = new ConcurrentHashMap<>();

    public GoogleAuthService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    private record CachedToken(String accessToken, Instant expiresAt) {}

    public String getValidAccessToken(DriveAccount driveAccount) {
        String accountId = driveAccount.getAccountId();

        CachedToken cached = tokenCache.get(accountId);
        if (cached != null && Instant.now().isBefore(cached.expiresAt().minusSeconds(300))) {
            return cached.accessToken();
        }


        String refreshToken = encryptionService.decryptInput(driveAccount.getRefreshToken());

        String newAccessToken = refreshAccessToken(refreshToken);
        tokenCache.put(accountId, new CachedToken(newAccessToken, Instant.now().plusSeconds(3600)));

        return newAccessToken;
    }

    private String refreshAccessToken(String refreshToken) {
        String tokenEndpoint = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", refreshToken);
        params.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(
                    tokenEndpoint, request, GoogleTokenResponse.class);

            if (response.getBody() != null && response.getBody().getAccessToken() != null) {
                return response.getBody().getAccessToken();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh Google Drive access token for account", e);
        }

        throw new RuntimeException("Empty token response from Google OAuth server");
    }
}