package com.shresth.FrankenCloud.Config;

import org.springframework.beans.factory.annotation.Value; // Fixed import!
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class EncryptionConfig {

    @Value("${app.encryption.password}")
    private String password;

    @Value("${app.encryption.salt}")
    private String salt;

    @Bean
    public TextEncryptor textEncryptor() {
        // Encryptors.delux() uses AES-256 GCM mode under the hood
        return Encryptors.delux(password, salt);
    }
}