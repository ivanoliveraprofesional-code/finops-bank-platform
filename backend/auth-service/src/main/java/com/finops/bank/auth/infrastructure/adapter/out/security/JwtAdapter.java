package com.finops.bank.auth.infrastructure.adapter.out.security;

import com.finops.bank.auth.application.port.out.TokenGeneratorPort;
import com.finops.bank.auth.domain.exception.JwtGenerationException;
import com.finops.bank.auth.domain.model.User;
import com.finops.bank.auth.infrastructure.config.RsaKeyConfig;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtAdapter implements TokenGeneratorPort {

    private final RsaKeyConfig rsaKeyConfig;

    @Override
    public String generateToken(User user) {
        try {
            JWSSigner signer = new RSASSASigner(rsaKeyConfig.getRsaJwk());

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getUsername())
                    .issuer("finops-bank-auth")
                    .claim("uid", user.getId())
                    .claim("roles", user.getRoles())
                    .expirationTime(new Date(new Date().getTime() + 900 * 1000))
                    .issueTime(new Date())
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(rsaKeyConfig.getRsaJwk().getKeyID())
                            .type(JOSEObjectType.JWT)
                            .build(),
                    claimsSet);

            signedJWT.sign(signer);

            return signedJWT.serialize();

        } catch (JOSEException e) {
            throw new JwtGenerationException("Error signing JWT", e);
        }
    }
}