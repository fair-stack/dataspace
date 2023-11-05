package cn.cnic.dataspace.api.service.impl;

import cn.cnic.dataspace.api.service.FileService;
import cn.cnic.dataspace.api.util.*;
import com.github.benmanes.caffeine.cache.Cache;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

/**
 * FileServiceImpl
 *
 * @author wangCc
 * @date 2021-03-18 19:07
 */
@Service
public class FileServiceImpl implements FileService {

    private final Cache<String, String> privateLink = CaffeineUtil.getPrivateLink();

    @Override
    public void download(String code, HttpServletRequest request, HttpServletResponse response) {
        OutputStream os = null;
        // Create input stream read file
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        try {
            if (StringUtils.isEmpty(code.trim())) {
                response.sendError(500, CommonUtils.messageInternational("FILE_PARSE"));
                return;
            }
            code = code.replaceAll("\\+", " ");
            String path = privateLink.getIfPresent(code);
            if (path == null) {
                String decrypt = SMS4.Decrypt(code);
                if (StringUtils.isNotEmpty(decrypt) && !decrypt.trim().equals("")) {
                    if (decrypt.contains("/")) {
                        path = decrypt;
                    } else {
                        response.sendError(500, CommonUtils.messageInternational("FILE_PARSE"));
                        return;
                    }
                } else {
                    response.sendError(500, CommonUtils.messageInternational("FILE_PARSE"));
                    return;
                }
            }
            File file = new File(path);
            if (!file.exists()) {
                return;
            }
            // Control file name encoding
            String filename = new String((file.getName()).getBytes("UTF-8"), "ISO8859-1");
            // Set force download not to open
            response.setContentType("application/force-download");
            // Set File Name
            response.addHeader("Content-Disposition", "attachment;fileName=" + filename);
            response.addHeader("Content-Length", "" + file.length());
            // Create output stream
            os = response.getOutputStream();
            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            // Method of writing files
            int byteRead = 0;
            byte[] buffer = new byte[1024];
            while ((byteRead = bis.read(buffer, 0, 1024)) != -1) {
                os.write(buffer, 0, byteRead);
            }
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                response.sendError(500, "Download failed!");
            } catch (Exception r) {
                r.printStackTrace();
            }
            return;
        } finally {
            try {
                // Close input output stream
                if (bis != null) {
                    bis.close();
                }
                if (fis != null) {
                    fis.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
