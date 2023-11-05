package cn.cnic.dataspace.api.elfinder.util;

import org.apache.commons.lang3.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

/**
 * ImageUtil for elfinder plugin
 *
 * @author wangCc
 * @date 2021-6-17 14:17:39
 */
public class ImageUtil {

    public static String getImageStr(InputStream in, String fileType) throws IOException {
        String fileContentBase64 = null;
        String base64Str = "data:" + fileType + ";base64,";
        String content;
        byte[] data;
        try {
            data = new byte[in.available()];
            in.read(data);
            in.close();
            if (data.length == 0) {
                return null;
            }
            Base64.Encoder encoder = Base64.getEncoder();
            content = encoder.encodeToString(data);
            content = content.replaceAll("\n", "").replaceAll("\r", "");
            if (StringUtils.isBlank(content)) {
                return null;
            }
            fileContentBase64 = base64Str + content;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return fileContentBase64;
    }

    public static byte[] decodeImageStr(String content) {
        return Base64.getDecoder().decode(content);
    }
}
