package com.vpn.bot.util;

import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;

@Service
public class ImageDecoder {
    public InputStream decodeBase64(String base64DataUrl) {
        try {
            // Убираем префикс "data:image/png;base64,", если он есть
            String base64Image = base64DataUrl.contains(",") ? base64DataUrl.split(",")[1] : base64DataUrl;
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            return new ByteArrayInputStream(imageBytes);
        } catch (Exception e) {
            return null;
        }
    }
}