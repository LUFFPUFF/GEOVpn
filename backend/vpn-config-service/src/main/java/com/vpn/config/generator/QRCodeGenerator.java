package com.vpn.config.generator;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.vpn.common.exception.ValidationException;
import com.vpn.config.config.VpnConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Генератор QR кодов для VLESS конфигураций
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QRCodeGenerator {

    private final VpnConfigProperties configProperties;

    public String generateQRCodeBase64(String vlessLink) {

        if (!configProperties.getQrCode().isEnabled()) {
            log.debug("QR code generation is disabled");
            return null;
        }

        if (vlessLink == null || vlessLink.isEmpty()) {
            throw new ValidationException("VLESS link cannot be empty");
        }

        try {
            int size = configProperties.getQrCode().getSize();
            String format = configProperties.getQrCode().getFormat();

            BufferedImage qrImage = generateQRCodeImage(vlessLink, size, size);
            String base64 = imageToBase64(qrImage, format);

            log.debug("Generated QR code: size={}x{}, format={}, length={}",
                    size, size, format, base64.length());

            return base64;

        } catch (Exception e) {
            log.error("Failed to generate QR code", e);
            throw new ValidationException("Failed to generate QR code: " + e.getMessage());
        }
    }

    /**
     * Генерирует QR код как BufferedImage
     */
    public BufferedImage generateQRCodeImage(String content, int width, int height)
            throws WriterException {

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height,
                hints
        );

        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * Получить data URL для встраивания в HTML
     */
    public String generateQRCodeDataUrl(String vlessLink) {
        String base64 = generateQRCodeBase64(vlessLink);
        if (base64 == null) {
            return null;
        }
        return String.format("data:image/%s;base64,%s",
                configProperties.getQrCode().getFormat().toLowerCase(),
                base64);
    }

    /**
     * Сохранить QR код в файл
     */
    public void saveQRCodeToFile(String vlessLink, String filePath) throws IOException, WriterException {
        int size = configProperties.getQrCode().getSize();
        BufferedImage qrImage = generateQRCodeImage(vlessLink, size, size);

        java.io.File outputFile = new java.io.File(filePath);
        ImageIO.write(qrImage, configProperties.getQrCode().getFormat(), outputFile);

        log.info("QR code saved to: {}", filePath);
    }


    private String imageToBase64(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, format, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }


}
