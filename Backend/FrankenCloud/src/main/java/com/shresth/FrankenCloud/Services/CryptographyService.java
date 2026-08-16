package com.shresth.FrankenCloud.Services;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptographyService {

    private static final int SALT_LENGTH_BYTES = 16;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    //userId, fileId, fileName, userEmail and salt.
    // idea here is that a fileId will be generated first in the db and will be used to generate the encryption key which is hash of all the metadata and salt.
    public String generateEncryptionKey(ObjectId userId, ObjectId fileId, String fileName, String userMail) {
        try {
            String payload = String.join(":",
                    userId.toHexString(),
                    fileId.toHexString(),
                    fileName,
                    userMail.toLowerCase().trim(),
                    generateSalt()
            );
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(keyBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error initializing SHA-256 digest", e);
        }
    }

    public String generateIV() {
        byte[] iv = new byte[16]; // 16 bytes = 128 bits
        secureRandom.nextBytes(iv);
        return Base64.getEncoder().encodeToString(iv);
    }

}
