package cn.cnic.dataspace.api.datax.core.util;

import cn.cnic.dataspace.api.datax.core.biz.model.HandleProcessCallbackParam;
import cn.cnic.dataspace.api.datax.core.log.JobLogger;
import cn.cnic.dataspace.api.datax.core.thread.ProcessCallbackThread;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 1. Embedded compilers such as' Python Interpreter 'cannot reference extension packages, so it is recommended to use the Java call console process method' Runtime. getRuntime(). exec() 'to run scripts (shell or Python);
 */
public class ScriptUtil {

    /**
     * make script file
     *
     * @param scriptFileName
     * @param content
     * @throws IOException
     */
    public static void markScriptFile(String scriptFileName, String content) throws IOException {
        // make file,   filePath/gluesource/666-123456789.py
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(scriptFileName);
            fileOutputStream.write(content.getBytes("UTF-8"));
            fileOutputStream.close();
        } catch (Exception e) {
            throw e;
        } finally {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }
    }

    /**
     * Script execution, real-time output of log files
     */
    public static int execToFile(String command, String scriptFile, String logFile, long logId, long logDateTime, String... params) {
        FileOutputStream fileOutputStream = null;
        Thread inputThread = null;
        Thread errThread = null;
        try {
            // file
            fileOutputStream = new FileOutputStream(logFile, true);
            // command
            List<String> cmdarray = new ArrayList<>();
            cmdarray.add(command);
            cmdarray.add(scriptFile);
            if (params != null && params.length > 0) {
                for (String param : params) {
                    cmdarray.add(param);
                }
            }
            String[] cmdarrayFinal = cmdarray.toArray(new String[cmdarray.size()]);
            // process-exec
            final Process process = Runtime.getRuntime().exec(cmdarrayFinal);
            String prcsId = ProcessUtil.getProcessId(process);
            JobLogger.log("------------------Process id: " + prcsId);
            // update task process id
            HandleProcessCallbackParam prcs = new HandleProcessCallbackParam(logId, logDateTime, prcsId);
            ProcessCallbackThread.pushCallBack(prcs);
            // log-thread
            final FileOutputStream finalFileOutputStream = fileOutputStream;
            inputThread = new Thread(() -> {
                try {
                    copy(process.getInputStream(), finalFileOutputStream, new byte[1024]);
                } catch (IOException e) {
                    JobLogger.log(e);
                }
            });
            errThread = new Thread(() -> {
                try {
                    copy(process.getErrorStream(), finalFileOutputStream, new byte[1024]);
                } catch (IOException e) {
                    JobLogger.log(e);
                }
            });
            inputThread.start();
            errThread.start();
            // process-wait
            // exit code: 0=success, 1=error
            int exitValue = process.waitFor();
            // log-thread join
            inputThread.join();
            errThread.join();
            return exitValue;
        } catch (Exception e) {
            JobLogger.log(e);
            return -1;
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    JobLogger.log(e);
                }
            }
            if (inputThread != null && inputThread.isAlive()) {
                inputThread.interrupt();
            }
            if (errThread != null && errThread.isAlive()) {
                errThread.interrupt();
            }
        }
    }

    /**
     * Data Flow Copy (Input automatically closes, Output does not process)
     */
    private static long copy(InputStream inputStream, OutputStream outputStream, byte[] buffer) throws IOException {
        try {
            long total = 0;
            for (; ; ) {
                int res = inputStream.read(buffer);
                if (res == -1) {
                    break;
                }
                if (res > 0) {
                    total += res;
                    if (outputStream != null) {
                        outputStream.write(buffer, 0, res);
                    }
                }
            }
            outputStream.flush();
            // out = null;
            inputStream.close();
            inputStream = null;
            return total;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }
    /**
     * Script execution, real-time output of log files
     */
    /*Public static int execToFileB (String command, String scriptFile, String logFile, String... params) throws IOException{*/
}
