package cn.cnic.dataspace.api.util;

import lombok.extern.slf4j.Slf4j;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;

@Slf4j
public class CsvUtils {

    /**
     * Generate as CVS file
     */
    public static File createCSVFile(List<String> exportData, String head, String outPutPath, String fileName) {
        File csvFile = null;
        BufferedWriter csvFileOutputStream = null;
        try {
            File file = new File(outPutPath);
            if (!file.exists()) {
                file.mkdir();
            }
            // Define file name format and create
            // File.createTempFile("",".csv");
            csvFile = new File(outPutPath, fileName);
            // UTF-8 enables correct reading of delimiters', '
            csvFileOutputStream = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvFile), "GBK"), 1024);
            // Write file header
            csvFileOutputStream.write(head);
            csvFileOutputStream.newLine();
            // Write file content
            Iterator<String> iterator = exportData.iterator();
            while (iterator.hasNext()) {
                String next = iterator.next();
                csvFileOutputStream.write(next);
                if (iterator.hasNext()) {
                    csvFileOutputStream.newLine();
                }
            }
            csvFileOutputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                csvFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return csvFile;
    }

    /**
     * Download files
     */
    public static void exportFile(HttpServletResponse response, File csvFile, String fileName) throws IOException {
        // response.setHeader("Content-type", "application-download");
        FileInputStream in = null;
        OutputStream out = response.getOutputStream();
        byte[] buffer = new byte[1024];
        int len = 0;
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
        response.setCharacterEncoding("UTF-8");
        try {
            in = new FileInputStream(csvFile);
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } catch (FileNotFoundException e) {
            log.error("获取文件错误!");
        } finally {
            if (in != null) {
                try {
                    in.close();
                    out.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Download CSV file
     */
    public static void download(String path, HttpServletResponse response) {
        try {
            // Path refers to the path of the file to be downloaded.
            File file = new File(path);
            // Obtain the file name.
            String filename = file.getName();
            // Download files in a stream format.
            InputStream fis = new BufferedInputStream(new FileInputStream(path));
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();
            // Clear response
            response.reset();
            // Set the response header and encode the file name with UTF-8, otherwise the file name will be garbled and incorrect when downloading
            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
            response.addHeader("Content-Length", "" + file.length());
            OutputStream toClient = new BufferedOutputStream(response.getOutputStream());
            response.setContentType("application/vnd.ms-excel;charset=gb2312");
            toClient.write(buffer);
            toClient.flush();
            toClient.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
