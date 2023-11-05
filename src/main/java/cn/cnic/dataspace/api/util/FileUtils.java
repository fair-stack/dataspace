package cn.cnic.dataspace.api.util;

import cn.hutool.core.io.IoUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.Zip64Mode;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
public class FileUtils {

    public static String formFileSize(long fileS) {
        // Convert File Size
        if (fileS == 0) {
            return "0B";
        }
        DecimalFormat df = new DecimalFormat("#.00");
        String fileSizeString = "";
        if (fileS < 1024) {
            fileSizeString = df.format((double) fileS) + "B";
        } else if (fileS < 1048576) {
            fileSizeString = df.format((double) fileS / 1024) + "KB";
        } else if (fileS < 1073741824) {
            fileSizeString = df.format((double) fileS / 1048576) + "MB";
        } else if (fileS < 1099511627776L) {
            fileSizeString = df.format((double) fileS / 1073741824) + "GB";
        } else {
            fileSizeString = df.format((double) fileS / 1099511627776L) + "TB";
        }
        return fileSizeString;
    }

    public static long longFormString(String string) {
        // Convert File Size
        if (string.contains("0B")) {
            return Long.valueOf(string.substring(0, string.length() - 1));
        }
        Long aLong = Long.valueOf(string.substring(0, string.length() - 2));
        if (string.contains("KB")) {
            return aLong * 1024;
        } else if (string.contains("MB")) {
            return aLong * 1048576;
        } else if (string.contains("GB")) {
            return aLong * 1073741824;
        } else if (string.contains("TB")) {
            return aLong * 1099511627776L;
        }
        return 0L;
    }

    public String formFileSizeToGB(long fileS) {
        // Convert File Size
        if (fileS == 0) {
            return "0";
        }
        DecimalFormat df = new DecimalFormat("#,##0.00000#");
        String fileSizeString = df.format((double) fileS / 1073741824);
        int i = fileSizeString.indexOf(".");
        String substring = fileSizeString.substring(i + 1);
        int length = substring.length();
        int len = 0;
        for (int b = 0; b < length; b++) {
            String substring1 = substring.substring(b, b + 1);
            Integer integer = Integer.valueOf(substring1);
            if (integer > 0) {
                len = b;
            }
        }
        if (len == 0) {
            String substring1 = fileSizeString.substring(0, i);
            Integer integer = Integer.valueOf(substring1);
            if (integer > 0) {
                return String.valueOf(integer);
            } else {
                return "0";
            }
        } else {
            String res = substring.substring(0, len + 1);
            String substring1 = fileSizeString.substring(0, i + 1);
            return substring1 + res;
        }
    }

    /**Method name: getFilesCount*/
    public static String getFilesCount(File file) {
        // Determine if the incoming file is empty
        if (!file.exists()) {
            return "0:0";
        }
        int count = 0;
        long data = 0;
        // Put all directories and files into an array
        File[] files = file.listFiles();
        // Traverse every element of an array
        for (File f : files) {
            // Determine if the element is a folder, and call this method repeatedly if it is a folder (recursion)
            if (f.isDirectory()) {
                String filesCount = getFilesCount(f);
                String[] split = filesCount.split(":");
                count += Integer.valueOf(split[0]);
                data += Long.valueOf(split[1]);
            } else {
                count++;
                data += f.length();
            }
        }
        return count + ":" + data;
    }

    public static String getFileLen(File file) {
        // Determine if the incoming file is empty
        if (!file.exists()) {
            return "0:0";
        }
        int count = 0;
        long data = 0;
        if (file.isDirectory()) {
            // Put all directories and files into an array
            File[] files = file.listFiles();
            // Traverse every element of an array
            for (File f : files) {
                String filesCount = getFileLen(f);
                String[] split = filesCount.split(":");
                count += Integer.valueOf(split[0]);
                data += Long.valueOf(split[1]);
            }
        } else {
            count++;
            data += file.length();
        }
        return count + ":" + data;
    }

    public long getSpaceSize(String path) {
        try {
            Runtime rt = Runtime.getRuntime();
            // Df - h View Hard Disk Space
            Process p = rt.exec("df -h " + path);
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String str = null;
                String[] strArray = null;
                int line = 0;
                while ((str = in.readLine()) != null) {
                    line++;
                    if (line != 2) {
                        continue;
                    }
                    int m = 0;
                    strArray = str.split(" ");
                    for (String para : strArray) {
                        if (para.trim().length() == 0) {
                            continue;
                        }
                        ++m;
                        if (m == 2) {
                            return getSize(para);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                in.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0L;
    }

    /**
     * Conversion
     */
    private long getSize(String size) {
        log.info("size: " + size);
        if (size.endsWith("K")) {
            Long k = new Double(size.replaceAll("K", "")).longValue();
            return k * 1024;
        } else if (size.endsWith("M")) {
            Long k = new Double(size.replaceAll("M", "")).longValue();
            return k * 1048576;
        } else if (size.endsWith("G")) {
            Long k = new Double(size.replaceAll("G", "")).longValue();
            return k * 1073741824L;
        } else if (size.endsWith("T")) {
            Long k = new Double(size.replaceAll("T", "")).longValue();
            return k * 1099511627776L;
        } else if (size.endsWith("P")) {
            Long k = new Double(size.replaceAll("P", "")).longValue();
            return k * 1125899906842624L;
        } else {
            return 0L;
        }
    }

    public static String getFileMD5(File file) {
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte[] buffer = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }

    /**
     * Java file operation to obtain file extension
     */
    public static String getFileName(String path) {
        if ((path != null) && (path.length() > 0)) {
            if (!path.contains("/")) {
                return path;
            }
            int dot = path.lastIndexOf('/');
            if (dot < 0) {
                return null;
            }
            if (dot == (path.length() - 1)) {
                path = path.substring(0, path.length() - 1);
                return path.substring(path.lastIndexOf('/') + 1);
            }
            return path.substring(dot + 1);
        }
        return null;
    }

    public static String getPartPath(String path) {
        if ((path != null) && (path.length() > 0)) {
            if (!path.contains("/")) {
                return "/";
            }
            int dot = path.lastIndexOf('/');
            if (dot < 0 || dot == 0) {
                return "/";
            }
            if (dot == (path.length() - 1)) {
                path = path.substring(0, path.length() - 1);
                return getPartPath(path);
            }
            return path.substring(0, dot);
        }
        return null;
    }

    /**
     * Java file operation to obtain file names without extensions
     */
    public static String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename.toLowerCase();
    }

    public static void createFolder(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * Compress files
     */
    public static void doZIP(List<String> annexPaths, HttpServletResponse response) {
        InputStream input = null;
        // Define compressed output stream
        ZipOutputStream zipOut = null;
        try {
            zipOut = new ZipOutputStream(response.getOutputStream());
            for (String filePath : annexPaths) {
                File file = new File(filePath);
                input = new FileInputStream(file);
                // Child entries in the compressed package
                int i = filePath.lastIndexOf("/") + 1;
                String name = filePath.substring(i);
                ZipEntry zipEntry = new ZipEntry(name);
                zipOut.putNextEntry(zipEntry);
                int len = 0;
                byte[] buffer = new byte[1024];
                while ((len = input.read(buffer)) != -1) {
                    zipOut.write(buffer, 0, len);
                }
                input.close();
            }
            zipOut.closeEntry();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            try {
                if (null != input) {
                    input.close();
                }
                if (null != zipOut) {
                    zipOut.close();
                }
            } catch (IOException ioException) {
            }
        }
    }

    /**
     * Physical file download
     */
    public static boolean downloadFile(String path, HttpServletResponse response) {
        // File input stream
        FileInputStream fis = null;
        // Output stream
        OutputStream os = null;
        BufferedInputStream bis = null;
        try {
            // The path is concatenated based on the log path and file name
            File file = new File(path);
            if (!file.exists()) {
                log.info("文件不存在!");
                return false;
            }
            // Control file name encoding
            String filename = new String((file.getName()).getBytes("UTF-8"), "ISO8859-1");
            // Set force download not to open
            response.setContentType("application/force-download");
            // Set File Name
            response.addHeader("Content-Disposition", "attachment;fileName=" + filename);
            response.addHeader("Content-Length", "" + file.length());
            // Start downloading
            fis = new FileInputStream(file);
            os = response.getOutputStream();
            bis = new BufferedInputStream(fis);
            byte[] buffer = new byte[2048];
            // output
            int i = bis.read(buffer);
            while (i != -1) {
                os.write(buffer);
                i = bis.read(buffer);
            }
            os.flush();
        } catch (Exception e) {
            e.printStackTrace();
            log.info("文件下载失败");
            return false;
        } finally {
            try {
                fis.close();
                bis.close();
                os.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * Folder Download
     */
    // public static boolean downloadFolder(String path, HttpServletResponse response) {
    // OutputStream os=null// Output stream
    // ZipOutputStream zipOut = null;
    // try {
    // //The path is concatenated based on the log path and file name
    // File file = new File(path);
    // if(!file.exists() || !file.isDirectory()){
    // Log. info ("File does not exist or is not a folder!");
    // return false;
    // }
    // //Browser handling garbled code issues
    // String fileName = file.getName()+".zip";
    // String filename=new String ((fileName). getBytes ("UTF-8"), "ISO8859-1")// Control file name encoding
    // //Response. setContentType ("application/force download")// Set force download not to open
    // response.setContentType("application/octet-stream");
    // response.setCharacterEncoding("UTF-8");
    // response.setHeader("Content-Disposition", "attachment;filename=" + filename);
    // 
    // //Start downloading
    // os = response.getOutputStream();
    // zipOut = new ZipOutputStream(os);
    // ZipStream zipStream = new ZipStream();
    // zipStream.toZip(file,zipOut,"");
    // zipOut.flush();
    // zipOut.closeEntry();
    // } catch (Exception e) {
    // e.printStackTrace();
    // Log. info ("Folder download failed");
    // return false;
    // }finally {
    // try {
    // if(os != null){ os.close();}
    // if(zipOut != null){ zipOut.close();}
    // }catch (Exception e){
    // e.printStackTrace();
    // }
    // }
    // return true;
    // }
    public static boolean downloadFolder(String path, HttpServletResponse response) {
        ZipArchiveOutputStream zous = null;
        try {
            // The path is concatenated based on the log path and file name
            File file = new File(path);
            if (!file.exists() || !file.isDirectory()) {
                log.info("文件不存在或者不是文件夹!");
                return false;
            }
            // Set Response
            response.reset();
            response.setContentType("application/octet-stream");
            response.setHeader("Accept-Ranges", "bytes");
            String fileName = file.getName() + ".zip";
            // Control file name encoding
            String filename = new String((fileName).getBytes("UTF-8"), "ISO8859-1");
            response.setContentType("application/octet-stream");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + filename);
            zous = new ZipArchiveOutputStream(response.getOutputStream());
            zous.setUseZip64(Zip64Mode.AsNeeded);
            downloadFileToServer(file, "", zous);
            // zous.closeArchiveEntry();
        } catch (Exception e) {
            e.printStackTrace();
            log.info("文件夹下载失败");
            return false;
        } finally {
            try {
                if (zous != null) {
                    zous.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private static void downloadFileToServer(File file, String path, ZipArchiveOutputStream zous) throws Exception {
        if (file.isDirectory()) {
            // Recursive download of files to compressed stream
            for (File listFile : file.listFiles()) {
                String levelPath = "";
                if (listFile.isDirectory()) {
                    levelPath = path + listFile.getName() + "/";
                } else {
                    levelPath = path;
                }
                downloadFileToServer(listFile, levelPath, zous);
            }
        } else {
            ArchiveEntry entry = new ZipArchiveEntry(path + file.getName());
            zous.putArchiveEntry(entry);
            try {
                InputStream inputStream = new FileInputStream(file);
                try {
                    int len;
                    byte[] bytes = new byte[1024];
                    while ((len = inputStream.read(bytes)) != -1) {
                        zous.write(bytes, 0, len);
                    }
                } finally {
                    IoUtil.close(inputStream);
                }
                zous.closeArchiveEntry();
                zous.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Verify if it is a folder
     */
    public static boolean isFolder(Path path) {
        return Files.isDirectory(path);
    }

    // Does the file exist
    public static boolean exists(Path path) {
        return Files.exists(path);
    }

    // Last modification time of the file
    public static long getLastModifiedTime(Path path) throws IOException {
        return Files.getLastModifiedTime(path).to(TimeUnit.SECONDS);
    }
}
