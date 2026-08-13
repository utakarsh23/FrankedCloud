package com.shresth.FrankenCloud.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EncryptionService {

    private final TextEncryptor textEncryptor;

    public String encryptInput(String input) {
        return textEncryptor.encrypt(input);
    }

    public String decryptInput(String input) {
        return textEncryptor.decrypt(input);
    }
}