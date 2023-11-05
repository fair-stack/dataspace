package cn.cnic.dataspace.api.util;

import cn.cnic.dataspace.api.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.io.*;
import java.util.zip.ZipOutputStream;

/**
 * Zip stream processing file
 */
@Slf4j
public class ZipStream {

    // File compression
    public void zipFile(File inputFile, ZipOutputStream zipoutputStream) {
        try {
            if (inputFile.exists()) {
                // Determine if the file exists
                if (inputFile.isFile()) {
                    // Determine whether it belongs to a file or a folder
                    // Create input stream read file
                    FileInputStream fis = null;
                    BufferedInputStream bis = null;
                    try {
                        fis = new FileInputStream(inputFile);
                        bis = new BufferedInputStream(fis);
                        // Write the file into the zip file and package it immediately
                        // Get File Name
                        ZipEntry ze = new ZipEntry(inputFile.getName());
                        zipoutputStream.putNextEntry(ze);
                        // Method of writing files
                        int byteRead = 0;
                        byte[] buffer = new byte[1024];
                        while ((byteRead = bis.read(buffer, 0, 1024)) != -1) {
                            zipoutputStream.write(buffer, 0, byteRead);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        // Close input output stream
                        if (bis != null) {
                            bis.close();
                        }
                        if (fis != null) {
                            fis.close();
                        }
                    }
                } else {
                    // If it is a folder, use an exhaustive method to obtain the file and write it to the zip file
                    try {
                        File[] files = inputFile.listFiles();
                        for (int i = 0; i < files.length; i++) {
                            zipFile(files[i], zipoutputStream);
                            zipoutputStream.flush();
                            zipoutputStream.closeEntry();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Zip the file
     */
    public void fileToZip(File srcFile, File zipFile) throws RuntimeException {
        if (zipFile == null) {
            return;
        }
        if (!zipFile.getName().endsWith(".zip")) {
            return;
        }
        ZipOutputStream zos = null;
        try {
            FileOutputStream out = new FileOutputStream(zipFile);
            zos = new ZipOutputStream(out);
            // this.zipFile(srcFile, zos);
            this.toZip(srcFile, zos, "");
            zos.flush();
            zos.closeEntry();
        } catch (Exception e) {
            log.error("ZipUtil toZip exception, ", e);
            throw new RuntimeException("zipFile error from ZipUtils", e);
        } finally {
            if (zos != null) {
                try {
                    zos.close();
                } catch (IOException e) {
                    log.error("ZipUtil toZip close exception, ", e);
                }
            }
        }
    }

    // public  boolean zipArchive(List<String> pathList, File zipFile) {
    // boolean flag = false;
    // if(zipFile == null){
    // return flag;
    // }
    // if(!zipFile.getName().endsWith(".zip")){
    // return flag;
    // }
    // ZipArchiveOutputStream zaos = null;
    // try {
    // zaos = new ZipArchiveOutputStream(zipFile);
    // zaos.setEncoding("UTF-8");
    // zaos.setUseZip64(Zip64Mode.AsNeeded);
    // zaos.setUseLanguageEncodingFlag(true);
    // for (String path : pathList) {
    // File file = new File(path);
    // toZip(file,zaos,"");
    // }
    // zaos.finish();
    // flag=true;
    // } catch (Exception e) {
    // flag = false;
    // log.error("ZipUtil toZip exception, ", e);
    // //   throw new RuntimeException("zipFile error from ZipUtils", e);
    // } finally {
    // try {
    // if (zaos != null) {
    // zaos.close();
    // }
    // } catch (IOException e) {
    // log.error("ZipUtil toZip close exception, ", e);
    // flag = false;
    // }
    // }
    // return flag;
    // }
    public void toZip(File file, ZipOutputStream zaos, String path) throws IOException {
        // Encapsulate each file with ZipArchiveEntry
        // Write ZipArchiveOutputStream to a compressed file again
        if (file.exists()) {
            if (file.isFile()) {
                if (file.exists()) {
                    ZipEntry zipArchiveEntry = new ZipEntry(path + file.getName());
                    zaos.putNextEntry(zipArchiveEntry);
                    InputStream is = null;
                    try {
                        is = new FileInputStream(file);
                        byte[] buffer = new byte[1024 * 2];
                        int len = -1;
                        while ((len = is.read(buffer)) != -1) {
                            // Write the bytes of the buffer to ZipArchiveEntry
                            zaos.write(buffer, 0, len);
                        }
                    } catch (Exception e) {
                        throw e;
                    } finally {
                        if (is != null)
                            is.close();
                    }
                }
            } else {
                File[] files = file.listFiles();
                String name = file.getName();
                if (files.length == 0) {
                    addFileToZip(file, zaos, path + name + "/");
                    zaos.flush();
                    zaos.closeEntry();
                } else {
                    for (File file1 : files) {
                        toZip(file1, zaos, path + name + "/");
                        zaos.flush();
                        zaos.closeEntry();
                    }
                }
            }
        }
    }

    private void addFileToZip(File file, ZipOutputStream out, String path) {
        try {
            ZipEntry entry = new ZipEntry(path);
            entry.setTime(file.lastModified());
            out.putNextEntry(entry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Extract the file using org. apache. tools. zip. ZipFile, which matches the
     */
    public void readByApacheZipFile(String archive, String targetPath) {
        BufferedInputStream bi = null;
        ZipFile zf = null;
        BufferedOutputStream bos = null;
        try {
            // Support for Chinese
            zf = new ZipFile(archive);
            // String path = archive.substring(0,archive.lastIndexOf("/"));
            Enumeration e = zf.entries();
            while (e.hasMoreElements()) {
                org.apache.tools.zip.ZipEntry ze2 = (org.apache.tools.zip.ZipEntry) e.nextElement();
                String entryName = ze2.getName();
                String filePath = targetPath + "/" + entryName;
                if (ze2.isDirectory()) {
                    File decompressDirFile = new File(filePath);
                    if (!decompressDirFile.exists()) {
                        decompressDirFile.mkdirs();
                    }
                } else {
                    String fileDir = filePath.substring(0, filePath.lastIndexOf("/"));
                    File fileDirFile = new File(fileDir);
                    if (!fileDirFile.exists()) {
                        fileDirFile.mkdirs();
                    }
                    bos = new BufferedOutputStream(new FileOutputStream(filePath));
                    bi = new BufferedInputStream(zf.getInputStream(ze2));
                    byte[] readContent = new byte[1024];
                    int readCount = bi.read(readContent);
                    while (readCount != -1) {
                        bos.write(readContent, 0, readCount);
                        readCount = bi.read(readContent);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommonException(e.getMessage());
        } finally {
            try {
                if (bi != null) {
                    bi.close();
                }
                if (zf != null) {
                    zf.close();
                }
                if (bos != null) {
                    bos.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public static String unzip(String filePath, String zipDir) {
        File file = new File(zipDir);
        if (!file.exists()) {
            file.mkdirs();
        }
        String name = "";
        BufferedOutputStream dest = null;
        BufferedInputStream is = null;
        try {
            ZipEntry entry;
            ZipFile zipfile = new ZipFile(filePath);
            Enumeration dir = zipfile.entries();
            while (dir.hasMoreElements()) {
                entry = (ZipEntry) dir.nextElement();
                if (entry.isDirectory()) {
                    name = entry.getName();
                    name = name.substring(0, name.length() - 1);
                    File fileObject = new File(zipDir, name);
                    if (!fileObject.exists()) {
                        fileObject.mkdirs();
                    }
                }
            }
            Enumeration e = zipfile.entries();
            while (e.hasMoreElements()) {
                entry = (ZipEntry) e.nextElement();
                if (entry.isDirectory()) {
                    continue;
                } else {
                    is = new BufferedInputStream(zipfile.getInputStream(entry));
                    int count;
                    byte[] dataByte = new byte[1024];
                    FileOutputStream fos = new FileOutputStream(zipDir + "/" + entry.getName());
                    dest = new BufferedOutputStream(fos, 1024);
                    while ((count = is.read(dataByte, 0, 1024)) != -1) {
                        dest.write(dataByte, 0, count);
                    }
                    dest.flush();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommonException("组件解压失败! {} " + e.getMessage());
        } finally {
            try {
                if (dest != null) {
                    dest.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioException) {
            }
        }
        return name;
    }

    /**
     * Copying Files
     */
    public static boolean copyFile(File resource, File target) throws Exception {
        boolean result = false;
        long start = System.currentTimeMillis();
        // File input stream and buffering
        FileInputStream inputStream = null;
        BufferedInputStream bufferedInputStream = null;
        // File output stream and buffering
        FileOutputStream outputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
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
            }
            // Flush output buffer stream
            bufferedOutputStream.flush();
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            if (bufferedOutputStream != null) {
                bufferedOutputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("耗时：" + (end - start) / 1000 + " s");
        return result;
    }
}
