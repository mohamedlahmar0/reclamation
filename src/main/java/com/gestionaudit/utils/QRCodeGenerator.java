package com.gestionaudit.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class QRCodeGenerator {

    private static Map<EncodeHintType, Object> hints() {
        Map<EncodeHintType, Object> h = new HashMap<>();
        h.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        h.put(EncodeHintType.MARGIN, 1);
        h.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        return h;
    }

    private static BitMatrix encode(String text, int width, int height) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        return qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints());
    }

    public static Image generateQRCodeImage(String text, int width, int height) throws WriterException {
        BitMatrix bitMatrix = encode(text, width, height);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    /** PNG bytes for PDF / email embeds (no JavaFX dependency on caller). */
    public static byte[] generateQRCodePngBytes(String text, int width, int height) throws WriterException, IOException {
        BitMatrix bitMatrix = encode(text, width, height);
        BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "PNG", baos);
        return baos.toByteArray();
    }
}
