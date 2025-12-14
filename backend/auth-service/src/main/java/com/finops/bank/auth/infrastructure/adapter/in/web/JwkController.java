package com.finops.bank.auth.infrastructure.adapter.in.web;

import com.finops.bank.auth.infrastructure.config.RsaKeyConfig;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwkController {

    private final RsaKeyConfig rsaKeyConfig;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> keys() {
        return new JWKSet(rsaKeyConfig.getRsaJwk().toPublicJWK()).toJSONObject();
    }
    
    @GetMapping("/auth/public-key-raw")
    public String getRawPublicKey() {
        try {
            return Base64.getEncoder().encodeToString(
                rsaKeyConfig.getRsaJwk().toRSAPublicKey().getEncoded()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }	
}