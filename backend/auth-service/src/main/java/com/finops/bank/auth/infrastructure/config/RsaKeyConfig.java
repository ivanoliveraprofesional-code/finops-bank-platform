package com.finops.bank.auth.infrastructure.config;

import com.finops.bank.auth.domain.exception.KeyInitializationException;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Configuration
public class RsaKeyConfig {

    @Value("${jwt.private-key:#{null}}") 
    private String privateKeyPem;

    @Value("${jwt.public-key:#{null}}")
    private String publicKeyPem;

    @Getter
    private RSAKey rsaJwk;

    @PostConstruct
    public void init() {
        if (privateKeyPem != null && !privateKeyPem.isBlank()) {
            log.info("Loading RSA Keys from configuration...");
            loadKeysFromPem();
        } else {
            log.warn("No keys found in config. GENERATING EPHEMERAL KEYS for DEV mode.");
            generateEphemeralKeys();
        }
    }

    private void loadKeysFromPem() {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            String privateClean = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
                
            String publicClean = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

            PKCS8EncodedKeySpec keySpecPKCS8 = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateClean));
            RSAPrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpecPKCS8);

            X509EncodedKeySpec keySpecX509 = new X509EncodedKeySpec(Base64.getDecoder().decode(publicClean));
            RSAPublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpecX509);

            this.rsaJwk = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();

        } catch (NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            throw new KeyInitializationException("Failed to load RSA keys from PEM format", e);
        }
    }

    private void generateEphemeralKeys() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();
            
            this.rsaJwk = new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();

        } catch (NoSuchAlgorithmException e) {
            throw new KeyInitializationException("Failed to generate ephemeral RSA keys", e);
        }
    }
}