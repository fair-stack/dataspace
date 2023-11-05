package cn.cnic.dataspace.api.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * @Description md5
 * @Author chl
 */
public class MD5 {

    public static String getMD5(String str) throws NoSuchAlgorithmException {
        byte[] buf = str.getBytes();
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(buf);
        byte[] tmp = md5.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : tmp) {
            if (b >= 0 && b < 16)
                sb.append("0");
            sb.append(Integer.toHexString(b & 0xff));
        }
        return sb.toString();
    }
}
