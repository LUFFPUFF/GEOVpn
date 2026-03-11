package com.vpn.gateway.util;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.util.Base64;

public class GenerationSecretKey {

    public static void main(String[] args) {
        String base64Key = Base64.getEncoder().encodeToString(
                Keys.secretKeyFor(SignatureAlgorithm.HS512).getEncoded()
        );

        System.out.print(base64Key);
    }
}
