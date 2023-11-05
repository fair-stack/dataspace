package cn.cnic.dataspace.api.quartz;

import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.backup.FtpHost;
import cn.cnic.dataspace.api.queue.FTPUtils;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.Constants;
import lombok.Data;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import java.io.*;
import java.util.*;

@Data
public class BackupUtils {

    private long total_fileNum;

    private long total_fileSize;

    // File Progress
    private long fileNum;

    // Total failed file progress
    private long progress;

    private FtpHost ftpHost;

    public BackupUtils(FtpHost ftpHost) {
        this.ftpHost = ftpHost;
    }

    public BackupUtils() {
    }

    public void destruction() {
        ftpHost = null;
    }

    public FTPClient login() throws IOException {
        if (null == ftpHost) {
            return null;
        }
        return FTPUtils.login(ftpHost.getHost(), Integer.valueOf(ftpHost.getPort()), ftpHost.getUsername(), ftpHost.getPassword());
    }

    public Date getNextDate(Date startDate, String cycle) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        if (cycle.equals(Constants.Backup.DAY)) {
            calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + 1);
        } else if (cycle.equals(Constants.Backup.WEEK)) {
            calendar.set(Calendar.WEEK_OF_YEAR, calendar.get(Calendar.WEEK_OF_YEAR) + 1);
        } else if (cycle.equals(Constants.Backup.MONTH)) {
            calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
        }
        return calendar.getTime();
    }

    public void strategy(File file, FTPClient ftpClient, String spaceId) throws IOException {
        // Create directory
        String date = CommonUtils.getDateString(new Date());
        String code = date + " " + spaceId;
        ftpClient.makeDirectory(new String(code.getBytes(), FTPUtils.CHARSET));
        ftpClient.changeWorkingDirectory(new String(code.getBytes(), FTPUtils.CHARSET));
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File file1 : files) {
                upload(file1, ftpClient);
                ftpClient.changeToParentDirectory();
            }
        } else {
            upload(file, ftpClient);
        }
    }

    /**
     * @ param file Uploaded file or folder
     */
    private void upload(File file, FTPClient ftpClient) throws IOException {
        if (file.isDirectory()) {
            ftpClient.makeDirectory(new String(file.getName().getBytes(), FTPUtils.CHARSET));
            ftpClient.changeWorkingDirectory(new String(file.getName().getBytes(), FTPUtils.CHARSET));
            String[] files = file.list();
            for (int i = 0; i < files.length; i++) {
                File file1 = new File(file.getPath() + "/" + files[i]);
                if (file1.isDirectory()) {
                    upload(file1, ftpClient);
                    ftpClient.changeToParentDirectory();
                } else {
                    this.fileNum++;
                    File file2 = new File(file.getPath() + "/" + files[i]);
                    this.progress += file2.length();
                    uploadFile(file2, ftpClient);
                }
            }
        } else {
            this.fileNum++;
            this.progress += file.length();
            uploadFile(file, ftpClient);
        }
    }

    private void uploadFile(File file, FTPClient ftpClient) throws IOException {
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            ftpClient.storeFile(new String(file.getName().getBytes(), FTPUtils.CHARSET), input);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommonException("上传失败：{} " + e.getMessage());
        } finally {
            if (null != input) {
                input.close();
            }
        }
    }

    // recursion
    public void getFileSize(File f) {
        // Get folder size
        File[] fList = f.listFiles();
        for (int i = 0; i < fList.length; i++) {
            if (fList[i].isDirectory()) {
                getFileSize(fList[i]);
            } else {
                this.total_fileSize += fList[i].length();
                this.total_fileNum++;
            }
        }
    }

    public boolean deleteFile(FTPClient ftpClient, String strategy, String spaceId, String rootPath) throws IOException {
        if (strategy.equals(Constants.Backup.ALL)) {
            return true;
        }
        List<String> nameList = new ArrayList<>(10);
        FTPFile[] ftpFiles = ftpClient.listFiles();
        for (FTPFile ftpFile : ftpFiles) {
            if (ftpFile.isDirectory()) {
                String name = ftpFile.getName();
                if (name.contains(" ")) {
                    String[] split = name.split(" ");
                    if (split[1].equals(spaceId)) {
                        nameList.add(split[0]);
                    }
                }
            }
        }
        List<String> endList = null;
        if (strategy.equals(Constants.Backup.THREE)) {
            if (nameList.size() > 2) {
                Collections.sort(nameList);
                endList = nameList.subList(0, nameList.size() - 2);
            }
        } else if (strategy.equals(Constants.Backup.NEW)) {
            endList = nameList;
        }
        if (null != endList) {
            // delete
            for (String date : endList) {
                String deletePath = rootPath + "/" + date + " " + spaceId;
                boolean b = ftpClient.removeDirectory(new String(deletePath.getBytes(), FTPUtils.CHARSET));
                System.out.println(b);
            }
        }
        return true;
    }
}
