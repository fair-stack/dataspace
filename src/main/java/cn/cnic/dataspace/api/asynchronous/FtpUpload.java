package cn.cnic.dataspace.api.asynchronous;

import cn.cnic.dataspace.api.queue.FTPUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import java.io.IOException;
import java.io.InputStream;

public class FtpUpload {

    /**
     * Description: Upload files to FTP server
     */
    public static // FTP server hostname
    // FTP server hostname
    // FTP server hostname
    // FTP server port
    // FTP login account
    // FTP login password
    // String path,//FTP server save directory
    // The file name uploaded to the FTP server
    boolean // Input stream
    uploadFile(String url, int port, String username, String password, String filename, InputStream input) {
        boolean success = false;
        FTPClient ftp = null;
        try {
            int reply;
            ftp = FTPUtils.login(url, port, username, password);
            ftp.setControlEncoding("GBK");
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                ftp.disconnect();
                return success;
            }
            // ftp.makeDirectory(path);
            // ftp.changeWorkingDirectory(path);
            ftp.storeFile(filename, input);
            ftp.logout();
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ioe) {
                }
            }
        }
        return success;
    }
}
