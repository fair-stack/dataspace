package cn.cnic.dataspace.api.util;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.mina.util.Base64;
import sun.font.FontDesignMetrics;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

public class QRCodeUtil {

    /**
     * Create QR code
     */
    public static BufferedImage createImage(String charSet, String content, int qrWidth, int qrHeight) {
        Hashtable hints = new Hashtable();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, charSet);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix bitMatrix = null;
        try {
            // Modify the bottom height of the QR code
            // Modify the bottom height of the QR code
            // Modify the bottom height of the QR code
            // Modify the bottom height of the QR code
            // Modify the bottom height of the QR code
            // Modify the bottom height of the QR code
            bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, qrWidth, qrHeight, hints);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return image;
    }

    /**
     * Set the logo for the generated QR code
     */
    public static void insertLogoImage(BufferedImage source, Image logo, int logoWidth, int logoHeight) {
        Graphics2D graph = source.createGraphics();
        int qrWidth = source.getWidth();
        int qrHeight = source.getHeight();
        int x = (qrWidth - logoWidth) / 2;
        int y = (qrHeight - logoHeight) / 2;
        graph.drawImage(logo, x, y, logoWidth, logoHeight, null);
        Shape shape = new RoundRectangle2D.Float(x, y, logoWidth, logoHeight, 6, 6);
        graph.setStroke(new BasicStroke(3f));
        graph.draw(shape);
        graph.dispose();
    }

    /**
     * Reduce logo image
     */
    public static Image compressLogo(String logoPath, int logoWidth, int logoHeight) {
        File file = new File(logoPath);
        if (!file.exists()) {
            System.err.println("" + logoPath + "   该文件不存在！");
            return null;
        }
        Image original = null;
        try {
            original = ImageIO.read(new File(logoPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        int width = original.getWidth(null);
        int height = original.getHeight(null);
        if (width > logoWidth) {
            width = logoWidth;
        }
        if (height > logoHeight) {
            height = logoHeight;
        }
        Image image = original.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage tag = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = tag.getGraphics();
        // Draw a reduced image
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return image;
    }

    /**
     * Add explanatory text at the bottom
     */
    public static BufferedImage addBottomFont(BufferedImage source, String text, int step) {
        int qrWidth = source.getWidth();
        System.out.println("二维码的宽度" + qrWidth);
        int qrHeight = source.getHeight();
        System.out.println("二维码的高度" + qrHeight);
        BufferedImage textImage = textToImage(text, qrWidth, 20, 16);
        Graphics2D graph = source.createGraphics();
        // Enable text anti aliasing
        graph.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int width = textImage.getWidth(null);
        int height = textImage.getHeight(null);
        Image src = textImage;
        graph.drawImage(src, 0, qrHeight - (20 * step) - 10, width, height, null);
        graph.dispose();
        return source;
    }

    /**
     * Add civilization description to QR code
     */
    public static BufferedImage textToImage(String str, int width, int height, int fontSize) {
        BufferedImage textImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = (Graphics2D) textImage.getGraphics();
        // Enable text anti aliasing
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setBackground(Color.WHITE);
        g2.clearRect(0, 0, width, height);
        g2.setPaint(Color.BLACK);
        FontRenderContext context = g2.getFontRenderContext();
        Font font = new Font("微软雅黑", Font.PLAIN, fontSize);
        g2.setFont(font);
        LineMetrics lineMetrics = font.getLineMetrics(str, context);
        FontMetrics fontMetrics = FontDesignMetrics.getMetrics(font);
        float offset = (width - fontMetrics.stringWidth(str)) / 2;
        float y = (height + lineMetrics.getAscent() - lineMetrics.getDescent() - lineMetrics.getLeading()) / 2;
        g2.drawString(str, (int) offset, (int) y);
        return textImage;
    }

    /**
     * Add explanatory text at the top
     */
    public static void addUpFont(BufferedImage source, String text) {
        int qrWidth = source.getWidth();
        int qrHeight = source.getHeight();
        BufferedImage textImage = textToImage(text, qrWidth, 20, 20);
        Graphics2D graph = source.createGraphics();
        // Enable text anti aliasing
        graph.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int width = textImage.getWidth(null);
        int height = textImage.getHeight(null);
        Image src = textImage;
        graph.drawImage(src, 0, 4, width, height, null);
        graph.dispose();
    }

    /**
     * Generate QR code image
     */
    public static void encode(String charSet, String content, int qrWidth, int qrHeight, String formatName, String imgPath) {
        BufferedImage image = QRCodeUtil.createImage(charSet, content, qrWidth, qrHeight);
        try {
            ImageIO.write(image, formatName, new File(imgPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate QR code image stream
     */
    public static BufferedImage encode(String charSet, String content, int qrWidth, int qrHeight) {
        BufferedImage image = QRCodeUtil.createImage(charSet, content, qrWidth, qrHeight);
        return image;
    }

    public static void encode(BufferedImage image, String formatName, String imgPath) {
        try {
            ImageIO.write(image, formatName, new File(imgPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void mkdirs(String destPath) {
        File file = new File(destPath);
        // When the folder does not exist, mkdirs will automatically create multi-level directories, which is different from mkdir. (mkdir will throw an exception if the parent directory does not exist)
        if (!file.exists() && !file.isDirectory()) {
            file.mkdirs();
        }
    }

    /**Parsing QR codes*/
    public static String decode(File file, String cherSet) throws Exception {
        BufferedImage image;
        image = ImageIO.read(file);
        if (image == null) {
            return null;
        }
        BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result result;
        Hashtable hints = new Hashtable();
        hints.put(DecodeHintType.CHARACTER_SET, cherSet);
        result = new MultiFormatReader().decode(bitmap, hints);
        String resultStr = result.getText();
        return resultStr;
    }

    // BufferedImage to base64
    public static String GetBase64FromImage(BufferedImage img) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            // Format images
            ImageIO.write(img, "jpg", stream);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        byte[] bytes = Base64.encodeBase64(stream.toByteArray());
        String base64 = new String(bytes);
        return "data:image/jpeg;base64," + base64;
    }
}
