package com.mosiacstore.mosiac.infrastructure.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

@Component
@RequiredArgsConstructor
@Slf4j
public class QRCodeGenerator {

    /**
     * Generates a QR code as a BufferedImage
     *
     * @param data The data to encode in the QR code
     * @param width The width of the QR code
     * @param height The height of the QR code
     * @return BufferedImage of the generated QR code
     * @throws WriterException If there is an error generating the QR code
     */
    public BufferedImage generateQRCodeImage(String data, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * Converts a BufferedImage to a byte array
     *
     * @param image The BufferedImage to convert
     * @return byte array representation of the image
     * @throws IOException If there is an error converting the image
     */
    public byte[] toByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }
}