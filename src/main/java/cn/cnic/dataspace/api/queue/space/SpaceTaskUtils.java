package cn.cnic.dataspace.api.queue.space;

import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.harvest.TaskFileImp;
import cn.cnic.dataspace.api.model.network.FileDeData;
import cn.cnic.dataspace.api.queue.FTPUtils;
import cn.cnic.dataspace.api.queue.ProgressBarThread;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.FileUtils;
import cn.cnic.dataspace.api.websocket.WebSocketProcess;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Data
public class SpaceTaskUtils {

    private long total;

    private long totalProgress = 0L;

    private long totalData = 0L;

    private long sort;

    private String user;

    private String rootId;

    private long totalSize;

    // Failed File Progress
    private long progress;

    // List - List of files with data size
    private List<TaskFileImp> taskFileImpList;

    private List<TaskFileImp> errorFileImpList;

    private WebSocketProcess webSocketTask;

    public SpaceTaskUtils(List<TaskFileImp> taskFileImpList, WebSocketProcess webSocketTask) {
        this.taskFileImpList = taskFileImpList;
        this.webSocketTask = webSocketTask;
        this.errorFileImpList = new ArrayList<>(16);
    }

    public void destruction() {
        taskFileImpList.clear();
        errorFileImpList.clear();
        total = 0;
        sort = 0;
        progress = 0;
        totalProgress = 0;
        totalData = 0;
        user = null;
        rootId = null;
        totalSize = 0;
        webSocketTask = null;
    }

    public FTPClient login(String host, int port, String username, String password) throws IOException {
        return FTPUtils.login(host, port, username, password);
    }

    /**
     * Determine whether the given path is a file or folder
     */
    public boolean isDirectory(FTPClient ftpClient, String path) throws IOException {
        return ftpClient.changeWorkingDirectory(new String(path.getBytes(), FTPUtils.CHARSET));
    }

    /**
     * Determine if the local path exists, create a path if it does not exist
     */
    public void makeDirs(String path) {
        File localFile = new File(path);
        if (!localFile.exists()) {
            localFile.mkdirs();
        }
    }

    public boolean makeFile(String filePath) {
        File localFile = new File(filePath);
        return localFile.exists();
    }

    /**
     * FTP Download Single File
     */
    public boolean downloadFile(FTPClient ftpClient, String fileName, String downDir, long fileSize, String taskId) {
        boolean result = true;
        File file = new File(downDir, fileName);
        OutputStream os = null;
        InputStream inputStream = null;
        ProgressBarThread pbt = null;
        try {
            ftpClient.setControlEncoding("GBK");
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            // If the file name contains Chinese, the file on the FTP cannot be found when retrieving the file, resulting in an empty file saved locally. Therefore, it is also necessary to convert it
            inputStream = ftpClient.retrieveFileStream(new String(fileName.getBytes(), FTPUtils.CHARSET));
            if (null == inputStream) {
                return false;
            }
            // Create a progress bar
            pbt = new ProgressBarThread(fileSize, taskId, user, rootId, webSocketTask);
            // Start the thread and refresh the progress bar
            new Thread(pbt).start();
            os = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int i = 0;
            while ((i = inputStream.read(buffer, 0, 1024)) > -1) {
                os.write(buffer, 0, i);
                this.progress += i;
                pbt.updateProgress(i);
            }
            os.flush();
            totalProgress++;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommonException(e.getMessage());
        } finally {
            try {
                pbt.finish();
                this.totalData += this.progress;
                if (null != inputStream) {
                    inputStream.close();
                }
                if (null != os) {
                    os.close();
                }
                ftpClient.completePendingCommand();
            } catch (Exception e) {
            }
        }
        return result;
    }

    public boolean getPathSize() {
        return taskFileImpList.isEmpty();
    }

    /**
     * Add a file task set based on the FTP directory
     */
    public void getFilePathList(FTPClient ftpClient, List<String> pathList) throws IOException {
        // Switch working directory
        for (String path : pathList) {
            // Obtain a list of files in the directory
            int i = path.lastIndexOf("/");
            if (i > 0) {
                String rootPath = path.substring(0, i);
                String file = path.substring(i + 1);
                ftpClient.changeWorkingDirectory(new String(rootPath.getBytes(), FTPUtils.CHARSET));
                FTPFile[] ftpFiles = ftpClient.listFiles();
                if (ftpFiles.length == 0) {
                    FTPFile ftpFile = new FTPFile();
                    ftpFile.setName(path);
                    ftpFile.setSize(0);
                    insertFileTask(ftpFile, FileUtils.getPartPath(path), true);
                    return;
                }
                for (FTPFile ftpFile : ftpFiles) {
                    if (ftpFile.getName().equals(file)) {
                        if (ftpFile.isFile()) {
                            sort++;
                            insertFileTask(ftpFile, rootPath, false);
                        } else {
                            getFilePathList(ftpClient, path);
                        }
                        break;
                    }
                }
            } else {
                getFilePathList(ftpClient, path);
            }
        }
        return;
    }

    /**
     * Recursively obtain all files in the ftp directory
     */
    private void getFilePathList(FTPClient ftpClient, String path) throws IOException {
        // Switch working directory
        ftpClient.changeWorkingDirectory(new String(path.getBytes(), FTPUtils.CHARSET));
        FTPFile[] ftpFiles = ftpClient.listFiles();
        // Circular download of files in the FTP directory
        if (ftpFiles.length == 0) {
            FTPFile ftpFile = new FTPFile();
            ftpFile.setName(FileUtils.getFileName(path));
            ftpFile.setSize(0);
            insertFileTask(ftpFile, FileUtils.getPartPath(path), true);
            return;
        }
        for (FTPFile ftpFile : ftpFiles) {
            if (ftpFile.isFile()) {
                sort++;
                insertFileTask(ftpFile, path, false);
            } else {
                getFilePathList(ftpClient, path + "/" + ftpFile.getName());
            }
        }
        return;
    }

    /**
     * Save Task Set - FTP
     */
    private void insertFileTask(FTPFile ftpFile, String path, boolean isFolder) {
        String taskId = CommonUtils.generateUUID();
        TaskFileImp taskFileImp = new TaskFileImp();
        taskFileImp.setCreateTime(new Date());
        taskFileImp.setFileName(ftpFile.getName());
        taskFileImp.setTaskId(taskId);
        taskFileImp.setPath(path);
        taskFileImp.setRootId(rootId);
        taskFileImp.setSort(sort);
        if (isFolder) {
            taskFileImp.setSize(0);
            taskFileImp.setType(1);
            taskFileImpList.add(taskFileImp);
            return;
        }
        total = total + ftpFile.getSize();
        taskFileImp.setSize(ftpFile.getSize());
        taskFileImpList.add(taskFileImp);
        // if(ftpFile.getSize()>0){
        // total = total + ftpFile.getSize();
        // taskFileImp.setSize(ftpFile.getSize());
        // taskFileImpList.add(taskFileImp);
        // }else {
        // taskFileImp.setSize(0);
        // taskFileImp.setState(3);
        // TaskFileImp. setError ("This file is empty and currently does not support transfer!");
        // errorFileImpList.add(taskFileImp);
        // }
    }

    private void insertFileTask(File ftpFile, String path, String spaceId, boolean isFolder) {
        String replace;
        if (path.lastIndexOf("/") != path.length() - 1) {
            replace = path.substring(0, path.lastIndexOf("/") + 1);
        } else {
            String sub = path.substring(0, path.length() - 1);
            replace = sub.substring(0, sub.lastIndexOf("/") + 1);
        }
        replace = replace.replaceAll(spaceId, "~");
        replace = replace.substring(replace.indexOf("~") + 1);
        TaskFileImp taskFileImp = new TaskFileImp();
        taskFileImp.setCreateTime(new Date());
        taskFileImp.setFileName(ftpFile.getName());
        taskFileImp.setTaskId(CommonUtils.generateUUID());
        taskFileImp.setPath(replace);
        taskFileImp.setRootId(rootId);
        taskFileImp.setSort(sort);
        if (isFolder) {
            taskFileImp.setSize(0);
            taskFileImp.setType(1);
            taskFileImpList.add(taskFileImp);
            return;
        }
        if (!ftpFile.exists()) {
            taskFileImp.setSize(0);
            taskFileImp.setState(3);
            taskFileImp.setError("文件未找到!!");
            errorFileImpList.add(taskFileImp);
        } else {
            total = total + ftpFile.length();
            taskFileImp.setSize(ftpFile.length());
            taskFileImpList.add(taskFileImp);
        }
    }

    private void insertFileTask(FileDeData.FileDe ftpFile) {
        TaskFileImp taskFileImp = new TaskFileImp();
        taskFileImp.setCreateTime(new Date());
        taskFileImp.setFileName(ftpFile.getFilename());
        taskFileImp.setTaskId(CommonUtils.generateUUID());
        // int index = ftpFile.getPath().lastIndexOf("/");
        // String path;
        // if(index > 0){
        // path = ftpFile.getPath().substring(0,index);
        // }else {
        // path = "/";
        // }
        taskFileImp.setPath(FileUtils.getPartPath(ftpFile.getPath()));
        taskFileImp.setRootId(rootId);
        taskFileImp.setSort(sort);
        // Download link
        String dLink = ftpFile.getDlink() + "&access_token=";
        taskFileImp.setLink(dLink);
        total = total + ftpFile.getSize();
        taskFileImp.setSize(ftpFile.getSize());
        taskFileImpList.add(taskFileImp);
        // if(ftpFile.getSize()>0){
        // total = total + ftpFile.getSize();
        // taskFileImp.setSize(ftpFile.getSize());
        // taskFileImpList.add(taskFileImp);
        // }else {
        // taskFileImp.setSize(0);
        // taskFileImp.setState(3);
        // TaskFileImp. setError ("This file is empty and currently does not support transfer!");
        // errorFileImpList.add(taskFileImp);
        // }
    }

    private void insertFolderTask(String path) {
        TaskFileImp taskFileImp = new TaskFileImp();
        taskFileImp.setCreateTime(new Date());
        taskFileImp.setFileName(FileUtils.getFileName(path));
        taskFileImp.setTaskId(CommonUtils.generateUUID());
        taskFileImp.setPath(FileUtils.getPartPath(path));
        taskFileImp.setRootId(rootId);
        taskFileImp.setSort(sort);
        taskFileImp.setType(1);
        taskFileImp.setSize(0);
        taskFileImpList.add(taskFileImp);
    }

    /*Space Import*/
    /**
     * Join File Transfer Subtask Collection
     */
    public void loadingTaskList(List<String> pathList, String spaceId) {
        for (String path : pathList) {
            File file = new File(path);
            if (file.exists()) {
                if (file.isFile()) {
                    sort++;
                    insertFileTask(file, path, spaceId, false);
                } else {
                    File[] files = file.listFiles();
                    if (files.length == 0) {
                        // Empty folders support import
                        sort++;
                        insertFileTask(file, path, spaceId, true);
                    } else {
                        for (File listFile : files) {
                            List<String> levelPath = new ArrayList<>(1);
                            levelPath.add(path + "/" + listFile.getName());
                            this.loadingTaskList(levelPath, spaceId);
                        }
                    }
                }
            } else {
                sort++;
                insertFileTask(file, path, spaceId, false);
            }
        }
    }

    /**
     * Copying Files
     */
    public boolean copyFile(File resource, File target, long fileSize, String taskId) {
        boolean result = false;
        // File input stream and buffering
        FileInputStream inputStream = null;
        BufferedInputStream bufferedInputStream = null;
        // File output stream and buffering
        FileOutputStream outputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        // Create a progress bar
        ProgressBarThread pbt = new ProgressBarThread(fileSize, taskId, user, rootId, webSocketTask);
        // Start the thread and refresh the progress bar
        new Thread(pbt).start();
        try {
            inputStream = new FileInputStream(resource);
            bufferedInputStream = new BufferedInputStream(inputStream);
            outputStream = new FileOutputStream(target);
            bufferedOutputStream = new BufferedOutputStream(outputStream);
            // Buffering an array of large files can make 1024 * 2 larger, but the larger the file, the faster it is
            byte[] bytes = new byte[1024 * 2];
            int len = 0;
            while ((len = inputStream.read(bytes)) != -1) {
                bufferedOutputStream.write(bytes, 0, len);
                this.progress += len;
                pbt.updateProgress(len);
            }
            // Flush output buffer stream
            bufferedOutputStream.flush();
            result = true;
            totalProgress++;
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommonException(e.getMessage());
        } finally {
            this.totalData += this.progress;
            pbt.finish();
            if (bufferedInputStream != null) {
                try {
                    bufferedInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /*Network disk import*/
    public void netFileTaskCov(List<FileDeData.FileDe> fileDeList, List<String> nullDirList) {
        Iterator<FileDeData.FileDe> iterator = fileDeList.iterator();
        while (iterator.hasNext()) {
            sort++;
            FileDeData.FileDe fileDe = iterator.next();
            insertFileTask(fileDe);
        }
        if (!nullDirList.isEmpty()) {
            for (String path : nullDirList) {
                insertFolderTask(path);
            }
        }
    }

    public boolean netDownloadFile(String remoteFileUrl, File localFile, long fileSize, String taskId) {
        CloseableHttpResponse response = null;
        InputStream input = null;
        FileOutputStream output = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        // Create a progress bar
        ProgressBarThread pbt = new ProgressBarThread(fileSize, taskId, user, rootId, webSocketTask);
        // Start the thread and refresh the progress bar
        new Thread(pbt).start();
        try {
            HttpGet httpget = new HttpGet(remoteFileUrl);
            httpget.setHeader("User-Agent", "pan.baidu.com");
            response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return false;
            }
            input = entity.getContent();
            output = new FileOutputStream(localFile);
            // Create handling tools
            byte[] datas = new byte[1024];
            int len = 0;
            while ((len = input.read(datas)) != -1) {
                // Loop reading data
                output.write(datas, 0, len);
                this.progress += len;
                pbt.updateProgress(len);
            }
            output.flush();
            totalProgress++;
            if (this.progress < fileSize) {
                localFile.delete();
                return false;
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            throw new CommonException(e.getMessage());
        } finally {
            this.totalData += this.progress;
            pbt.finish();
            // Turn off low-level flow.
            if (input != null) {
                try {
                    input.close();
                } catch (Exception e) {
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (Exception e) {
                }
            }
            if (response != null) {
                try {
                    response.close();
                } catch (Exception e) {
                }
            }
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
     * Message notification
     */
    public void sendMessage(String email, String message) {
        try {
            webSocketTask.sendMessage(email, message);
        } catch (Exception e) {
            log.error("消息发送失败: email " + email + " errormessage: {}" + e.getMessage());
        }
    }
}
