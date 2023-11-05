package cn.cnic.dataspace.api.queue;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class FTPUtils {

    public final static String LOCAL_CHARSET = "UTF-8";

    public final static String CHARSET = "iso-8859-1";

    /**
     * Login to FTP
     */
    public static FTPClient login(String host, int port, String username, String password) throws IOException {
        FTPClient ftpClient = new FTPClient();
        ftpClient.setDataTimeout(60000);
        ftpClient.setDefaultTimeout(10000);
        ftpClient.setConnectTimeout(60000);
        // Connect to FTP server
        ftpClient.connect(host, port);
        // Passive mode
        ftpClient.enterLocalPassiveMode();
        // Set encoding
        ftpClient.setControlEncoding("UTF-8");
        ftpClient.setRemoteVerificationEnabled(false);
        ftpClient.login(username, password);
        ftpClient.setBufferSize(8096);
        ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
        return ftpClient;
    }

    /**
     * Delete
     */
    public static boolean delete(FTPClient ftpClient, String fileName) {
        try {
            boolean flag = ftpClient.deleteFile(new String(fileName.getBytes(LOCAL_CHARSET), CHARSET));
            return flag;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
    }

    public static void close(FTPClient ftpClient) {
        if (ftpClient != null) {
            try {
                ftpClient.disconnect();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Determine if a file named fileName exists in the directory where ftpClient is currently located (0 does not exist, 1 exists, 2 has different sizes)
     */
    public static int isExist(FTPClient ftpClient, final String fileName, long size) {
        try {
            FTPFile[] ftpFiles = ftpClient.listFiles(null, new FTPFileFilter() {

                @Override
                public boolean accept(FTPFile file) {
                    if (file.getName().equals(fileName)) {
                        return true;
                    }
                    return false;
                }
            });
            if (ftpFiles != null && ftpFiles.length > 0) {
                for (FTPFile ftpFile : ftpFiles) {
                    if (ftpFile.getSize() != size) {
                        return 2;
                    }
                }
                return 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Get duplicate files
     */
    public static FTPFile getExist(FTPClient ftpClient, final String fileName) throws IOException {
        FTPFile[] files = ftpClient.listFiles(null, new FTPFileFilter() {

            @Override
            public boolean accept(FTPFile file) {
                if (file.getName().equals(fileName)) {
                    return true;
                }
                return false;
            }
        });
        if (null != files && files.length > 0) {
            return files[0];
        }
        return null;
    }
}
