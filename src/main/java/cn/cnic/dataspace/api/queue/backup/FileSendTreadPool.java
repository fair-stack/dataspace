package cn.cnic.dataspace.api.queue.backup;

import cn.cnic.dataspace.api.config.space.FileOperationFactory;
import cn.cnic.dataspace.api.datax.admin.tool.sql.CommonDBUtils;
import cn.cnic.dataspace.api.datax.admin.tool.sql.JdbcConnectionFactory;
import cn.cnic.dataspace.api.datax.admin.tool.sql.SqlUtils;
import cn.cnic.dataspace.api.datax.admin.util.JdbcConstants;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.manage.ReleaseAccount;
import cn.cnic.dataspace.api.model.release.ResourceV2;
import cn.cnic.dataspace.api.queue.FTPUtils;
import cn.cnic.dataspace.api.queue.SpaceQuery;
import cn.cnic.dataspace.api.util.CaffeineUtil;
import cn.cnic.dataspace.api.util.Constants;
import cn.cnic.dataspace.api.util.HttpClient;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.CollectionUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class FileSendTreadPool {

    private static FileSendTreadPool fileSendTreadPool = null;

    private ExecutorService exeCuter;

    private MongoTemplate mongoTemplate;

    private final Cache<String, String> insertDBData = CaffeineUtil.getScienceData();

    private final Cache<String, Map<String, String>> publicOrgUrl = CaffeineUtil.getPublicOrgUrl();

    private final Cache<String, Boolean> publicFileStop = CaffeineUtil.getPublicFileStop();

    private final static int MAX = 10;

    // private long uploadTotal = 0L;
    private FileSendTreadPool(MongoTemplate mongoTemplate) {
        exeCuter = Executors.newFixedThreadPool(MAX);
        this.mongoTemplate = mongoTemplate;
    }

    public synchronized static FileSendTreadPool getInstance(MongoTemplate mon) {
        if (fileSendTreadPool == null) {
            fileSendTreadPool = new FileSendTreadPool(mon);
        }
        return fileSendTreadPool;
    }

    public void execute(final String userId) {
        exeCuter.execute(new Runnable() {

            @SneakyThrows
            @Override
            public void run() {
                SpaceQuery.FileSend fileSend = SpaceQuery.getInstance().getFileSendElement(userId);
                while (fileSend != null) {
                    // Core processes
                    String resourceId = (String) fileSend.getParamMap().get("resourceId");
                    String version = (String) fileSend.getParamMap().get("version");
                    log.info("线程：" + resourceId + version + " 开始执行 --------");
                    switch(fileSend.getType()) {
                        case "sendFile":
                            try {
                                dealWithTask(fileSend);
                            } catch (Exception e) {
                                e.printStackTrace();
                                log.info("数据传输过程中出现错误: {} " + e.getMessage());
                            }
                            break;
                        case "sendApi":
                            try {
                                sendInstDBFile(fileSend.getParamMap());
                            } catch (Exception e) {
                                e.printStackTrace();
                                log.info("数据传输完成、通知互操作接口过程中出现错误: {} " + e.getMessage());
                            }
                            break;
                    }
                    log.info("线程：" + resourceId + version + " 结束执行 --------");
                    publicFileStop.invalidate(resourceId + version);
                    fileSend = SpaceQuery.getInstance().getFileSendElement(userId);
                }
            }
        });
    }

    @SneakyThrows
    private void dealWithTask(SpaceQuery.FileSend fileSend) throws IOException {
        publicFile(fileSend.getParamMap());
        return;
    }

    private void publicFile(Map paramMap) {
        String resourceId = (String) paramMap.get("resourceId");
        String version = (String) paramMap.get("version");
        if (CollectionUtils.isEmpty(paramMap)) {
            return;
        }
        log.info("1.{} 调用异步文件发布 --- ");
        // interrupt
        interruptValidation(resourceId + version);
        int dataType = (int) paramMap.get("dataType");
        List<String> pathList = (List) paramMap.get("pathList");
        if (dataType == 1 || dataType == 2) {
            List<String> list = (List) paramMap.get("csvList");
            // export table to excel
            try {
                tableToCsv(paramMap.get("spaceDbName").toString(), list);
            } catch (Exception e) {
                updateStateError(paramMap, e.getMessage());
                return;
            }
            if (null == pathList) {
                pathList = list;
            } else {
                pathList.addAll(list);
            }
        }
        // interrupt
        interruptValidation(resourceId + version);
        if (null == pathList) {
            updateStateError(paramMap, "所选文件路径未找到！");
            return;
        }
        String targetPath = paramMap.get("targetPath").toString();
        File targetFile = new File(targetPath);
        if (targetFile.exists()) {
            // If the file exists, it will be directly deleted - subsequent logic will change
            targetFile.delete();
        }
        log.info("2.{} 参数条件校验完成，准备进行文件发布 --- ");
        // Send physical files
        String type = (String) paramMap.get("type");
        if (type.equals(Constants.SCI)) {
            // Determine the method of sending physical files
            return;
            // sendFileToSCIDB(paramMap);
        } else if (type.equals(Constants.INSERT)) {
            // Send to insertDB
            sendFileToInsertDB(paramMap, pathList);
        }
        return;
    }

    private void tableToCsv(String dbName, List<String> csvList) {
        // Export structured data to Excel
        Connection connection = null;
        ResultSet resultSet = null;
        try {
            connection = new JdbcConnectionFactory(dbName).getConnection();
            for (String path : csvList) {
                List<List<String>> lines = new ArrayList<>();
                String tableName = new File(path).getName().split("\\.")[0];
                String selectSqlWithAll = SqlUtils.generateSelectSqlWithAll(dbName, tableName);
                resultSet = CommonDBUtils.query(connection, selectSqlWithAll, 100);
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                List<String> header = new ArrayList<>();
                for (int i = 0; i < columnCount; i++) {
                    String columnLabel = metaData.getColumnLabel(i + 1);
                    if (!columnLabel.equals((JdbcConstants.PRIMARY_KEY))) {
                        header.add(columnLabel);
                    }
                }
                lines.add(header);
                while (resultSet.next()) {
                    List<String> line = new ArrayList<>();
                    for (int i = 0; i < columnCount; i++) {
                        if (!metaData.getColumnLabel(i + 1).equals(JdbcConstants.PRIMARY_KEY)) {
                            line.add(resultSet.getString(i + 1));
                        }
                    }
                    lines.add(line);
                }
                resultSet.close();
                EasyExcel.write(new File(path)).sheet("Sheet1").doWrite(lines);
            }
        } catch (SQLException e) {
            log.error("执行查询sql失败");
            log.error(e.getMessage(), e);
            throw new CommonException("表格文件生成-执行查询sql失败");
        } finally {
            CommonDBUtils.closeDBResources(connection);
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return;
    }

    private void sendFileToInsertDB(Map paramMap, List<String> pathList) {
        String username = (String) paramMap.get("username");
        String password = (String) paramMap.get("password");
        String spaceId = (String) paramMap.get("spaceId");
        String resourceId = (String) paramMap.get("resourceId");
        String version = (String) paramMap.get("version");
        String ftpUrl = paramMap.get("ftpUrl").toString();
        // interrupt
        interruptValidation(resourceId + version);
        // File push call
        long uploadTotal = 0l;
        try {
            Map<String, Object> resultMap = this.sendFtpFile(ftpUrl, username, password, pathList, resourceId + version);
            boolean result = resultMap.get("code").equals("true") ? true : false;
            if (!result) {
                log.info("3 {} 文件推送中有部分文件推送失败 {} " + resultMap.get("mes"));
                updateStateError(paramMap, resultMap.get("mes").toString());
                return;
            }
            uploadTotal = (long) resultMap.get("total");
        } catch (Exception e) {
            e.printStackTrace();
            log.info("3 {} 文件推送时发生异常 {} " + e.getMessage());
            updateStateError(paramMap, "文件推送失败 {} " + e.getMessage());
            return;
        }
        // interrupt
        interruptValidation(resourceId + version);
        log.info("4 {} 文件推送成功 -- 调用文件通知接口");
        // interrupt
        interruptValidation(resourceId + version);
        paramMap.put("total", uploadTotal);
        sendInstDBFile(paramMap);
        return;
    }

    /**
     * File notification interface
     */
    private void sendInstDBFile(Map<String, Object> paramMap) {
        String username = (String) paramMap.get("username");
        String password = (String) paramMap.get("password");
        String resourceId = (String) paramMap.get("resourceId");
        String version = (String) paramMap.get("version");
        String orgId = (String) paramMap.get("orgId");
        String traceId = paramMap.get("traceId").toString();
        String ftpUrl = paramMap.get("ftpUrl").toString();
        String id = paramMap.get("id").toString();
        Update update = new Update();
        if (paramMap.get("total") != null) {
            update.set("dataSize", (paramMap.get("total")));
        }
        update.set("fileSend", true);
        update.set("ftpUsername", username);
        update.set("ftpPassword", password);
        update.set("ftpHost", ftpUrl);
        try {
            HttpClient httpClient = new HttpClient();
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("resourceId", traceId));
            Map<String, Object> tokenMap = getToken(httpClient, paramMap.get("tokenUrl").toString(), orgId);
            if (!(Boolean) tokenMap.get("code")) {
                log.info("5 {} 互操作 获取token接口 返回的错误 {} 失败!");
                update.set("auditSend", false);
                update.set("fileSuccessOf", 2);
                update.set("auditError", tokenMap.get("message"));
            } else {
                // Notify file upload end insertDB
                String resultData = requestFileNotice(httpClient, orgId, tokenMap.get("token").toString(), params);
                // Notify file upload end insertDB
                ;
                // Parse return value
                HashMap dataMap = JSONObject.parseObject(resultData, HashMap.class);
                if (CollectionUtils.isEmpty(dataMap)) {
                    // Null verification
                    log.info("5 {} 文件上传通知接口-返回的信息为空 {} 失败!");
                    update.set("auditSend", false);
                    update.set("fileSuccessOf", 2);
                    update.set("auditError", "文件上传通知接口-返回的信息为空");
                } else if (!dataMap.containsKey("result")) {
                    log.info("5 {} 文件上传通知接口-格式无法解析!  {} " + resultData);
                    update.set("auditSend", false);
                    update.set("fileSuccessOf", 2);
                    update.set("auditError", "文件上传通知接口-格式无法解析 {} " + resultData);
                } else if (!(boolean) dataMap.get("result")) {
                    // Status code verification
                    log.info("5 {} 文件上传通知接口- 返回状态码为不通过 （发生错误） {} " + resultData);
                    update.set("auditSend", false);
                    update.set("fileSuccessOf", 2);
                    update.set("auditError", "文件上传通知接口- 返回状态码为不通过 {} " + resultData);
                } else {
                    // success
                    update.set("auditSend", true);
                    update.set("fileSuccessOf", 1);
                    log.info("推送成功-> insertDB {} ---");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            update.set("auditSend", false);
            update.set("fileSuccessOf", 2);
            update.set("auditError", "文件上传通知接口-系统抛出异常 {} " + e.getMessage());
        }
        // interrupt
        interruptValidation(resourceId + version);
        Query query = new Query().addCriteria(Criteria.where("_id").is(id));
        mongoTemplate.upsert(query, update, ResourceV2.class);
        log.info("推送任务结束 - > {} --");
        return;
    }

    private String requestFileNotice(HttpClient httpClient, String orgId, String token, List<NameValuePair> params) {
        String ifPresent = insertDBData.getIfPresent(Constants.UPLOAD_COMPLETED + orgId);
        Map fileUrl = JSONObject.parseObject(ifPresent, Map.class);
        Map<String, String> header = new HashMap<>(8);
        header.put("token", token);
        header.put("version", fileUrl.get("version").toString());
        // Notify file upload end insertDB
        return httpClient.doPostHeader(params, fileUrl.get("url").toString(), header);
    }

    // FTP transfer to database server
    private Map<String, Object> sendFtpFile(String hostname, String username, String password, List<String> pathList, String jobId) {
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("code", "false");
        String host;
        int port;
        hostname = hostname.substring(hostname.lastIndexOf("/") + 1);
        if (hostname.contains(":")) {
            host = hostname.substring(0, hostname.indexOf(":"));
            port = Integer.parseInt(hostname.substring(hostname.indexOf(":") + 1));
        } else {
            host = hostname;
            port = 21;
        }
        // Connect to FTP server
        FTPClient ftp = null;
        try {
            ftp = FTPUtils.login(host, port, username, password);
            ftp.setControlEncoding("GBK");
            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                ftp.disconnect();
                log.info("3.{}  无法连接到ftp服务器：" + host + " ------");
                resultMap.put("mes", "无法连接到ftp服务器");
                return resultMap;
            }
        } catch (IOException e) {
            closedFtp(ftp);
            e.printStackTrace();
            log.info("3.{}  无法连接到ftp服务器：" + host + " ------");
            resultMap.put("mes", "无法连接到ftp服务器");
            return resultMap;
        }
        // Push files
        List<String> errorList = new ArrayList<>();
        DataSizeStorage dataSizeStorage = new DataSizeStorage(0);
        for (String path : pathList) {
            File file = new File(path);
            if (!file.exists()) {
                closedFtp(ftp);
                log.info("3.{}  文件未找到（文件可能被移动或者修改） 文件路径" + path + " ------");
                resultMap.put("mes", "文件未找到（文件可能被移动或者修改）" + path);
                return resultMap;
            }
            try {
                List<String> upload = upload(file, ftp, jobId, dataSizeStorage);
                errorList.addAll(upload);
                ftp.changeToParentDirectory();
            } catch (Exception exception) {
                exception.printStackTrace();
                closedFtp(ftp);
                log.info("3.{}  文件上传失败  ------");
                resultMap.put("mes", "文件上传失败 {} " + exception.getMessage());
                return resultMap;
            }
        }
        closedFtp(ftp);
        resultMap.put("total", dataSizeStorage.getTotal());
        dataSizeStorage.destroy();
        if (errorList.size() > 0) {
            resultMap.put("mes", "任务推送错误-错误列表: " + JSONObject.toJSONString(errorList));
            errorList.clear();
            return resultMap;
        }
        resultMap.put("code", "true");
        return resultMap;
    }

    /**
     * @ param file Uploaded file or folder
     */
    private List<String> upload(File file, FTPClient ftpClient, String jobId, DataSizeStorage dataSizeStorage) throws IOException {
        // interrupt
        interruptValidation(jobId);
        List<String> errorList = new ArrayList<>();
        if (file.isDirectory()) {
            ftpClient.makeDirectory(new String(file.getName().getBytes(), FTPUtils.CHARSET));
            ftpClient.changeWorkingDirectory(new String(file.getName().getBytes(), FTPUtils.CHARSET));
            String[] files = file.list();
            for (int i = 0; i < files.length; i++) {
                // interrupt
                interruptValidation(jobId);
                File fileLevel = new File(file.getPath() + "/" + files[i]);
                List<String> upload = upload(fileLevel, ftpClient, jobId, dataSizeStorage);
                if (fileLevel.isDirectory()) {
                    ftpClient.changeToParentDirectory();
                }
                errorList.addAll(upload);
            }
        } else {
            dataSizeStorage.addSize(file.length());
            String res = sendFile(null, file, ftpClient);
            if (!res.equals("")) {
                errorList.add(res);
            }
        }
        // Duplicate verification file
        // FTPFile existF = FTPUtils.getExist(ftpClient, file.getName());
        // if(file.isDirectory()){
        // if(null == existF){
        // ftpClient.makeDirectory(new String(file.getName().getBytes(), FTPUtils.CHARSET));
        // }
        // ftpClient.changeWorkingDirectory(new String(file.getName().getBytes(), FTPUtils.CHARSET));
        // String[] files = file.list();
        // for (int i = 0; i < files.length; i++) {
        // InterruptValidation (jobId)// interrupt
        // File fileLevel = new File(file.getPath()+"/"+files[i] );
        // List<String> upload = upload(fileLevel, ftpClient, jobId);
        // if(fileLevel.isDirectory()){
        // ftpClient.changeToParentDirectory();
        // }
        // errorList.addAll(upload);
        // }
        // }else{
        // String res = sendFile(existF, file, ftpClient);
        // if(!res.equals("")){
        // errorList.add(res);
        // }
        // }
        return errorList;
    }

    private String sendFile(FTPFile ftpFile, File file, FTPClient ftpClient) {
        String res = "";
        boolean result = uploadFile(file, ftpClient);
        if (!result) {
            res = "错误-文件上传-文件地址：" + file.getPath();
        }
        // if(null == ftpFile) {
        // boolean result = uploadFile(file, ftpClient);
        // if(!result){
        // Res="Error - File Upload - File Address:"+file. getPath();
        // }
        // uploadTotal+=file.length();
        // }Else if (ftpFile. getSize()<file. length()) {//File continuation
        // boolean result = passOnFile(file.getName(), file, ftpFile.getSize(), ftpClient);
        // if(!result) {
        // Res="Error - File Renewal - File Address:"+file. getPath();
        // }
        // uploadTotal += (file.length() - ftpFile.getSize());
        // }Else if (ftpFile. getSize()>file. length()) {//The target side file is too large
        // Log. info ("Data Publishing - File Push: Target Side File Too Large"+file. getPath());
        // }
        return res;
    }

    /**
     * New file upload
     */
    private boolean uploadFile(File file, FTPClient ftpClient) {
        FileInputStream input = null;
        try {
            input = new FileInputStream(file);
            ftpClient.storeFile(new String(file.getName().getBytes(), FTPUtils.CHARSET), input);
            return true;
        } catch (Exception e) {
            log.info("----- 数据发布文件推送：文件上传失败-------: {} " + e.getMessage());
            return false;
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }

    /**
     * Breakpoint continuation
     */
    private boolean passOnFile(String remoteFile, File localFile, long remoteSize, FTPClient ftpClient) {
        RandomAccessFile raf = null;
        OutputStream out = null;
        try {
            raf = new RandomAccessFile(localFile, "r");
            out = ftpClient.appendFileStream(new String(remoteFile.getBytes(), StandardCharsets.ISO_8859_1));
            /*Additional continuation*/
            // Breakpoint continuation
            ftpClient.setRestartOffset(remoteSize);
            raf.seek(remoteSize);
            byte[] bytes = new byte[1024];
            int c;
            while ((c = raf.read(bytes)) != -1) {
                out.write(bytes, 0, c);
            }
            out.flush();
            return ftpClient.completePendingCommand();
        } catch (Throwable throwable) {
            // If the upload process is interrupted, the connection is still unable to delete the file, and it is easy to occupy resources.
            throwable.printStackTrace();
            log.info("----- 数据发布文件推送：断点续传失败! ------");
            return false;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException ioException) {
            }
        }
    }

    private void closedFtp(FTPClient ftpClient) {
        try {
            if (null != ftpClient) {
                ftpClient.logout();
                if (ftpClient.isConnected()) {
                    ftpClient.disconnect();
                }
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    /**
     * Obtain insertDB token (login)
     */
    private Map<String, Object> getToken(HttpClient httpClient, String url, String orgId) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("code", false);
        // token
        String token = insertDBData.getIfPresent(Constants.INS_TOKEN + orgId);
        if (StringUtils.isEmpty(token)) {
            // Login user to obtain token
            Query queryOne = new Query().addCriteria(Criteria.where("orgId").is(orgId));
            ReleaseAccount one = mongoTemplate.findOne(queryOne, ReleaseAccount.class);
            if (one == null) {
                resultMap.put("message", "所选发布机构下未添加发布账号,请联系管理员");
                return resultMap;
            }
            String result = "";
            try {
                Map<String, String> ifPresent = publicOrgUrl.getIfPresent(orgId);
                if (ifPresent == null) {
                    resultMap.put("message", "未找到所选发布机构,请联系管理员");
                    return resultMap;
                }
                String host = ifPresent.get("host");
                result = httpClient.doGetHeader(host + url, one.getAuthCode());
            } catch (Exception e) {
                e.printStackTrace();
                resultMap.put("message", "总中心机构获取接口查询-连接失败,请稍后重试!");
                return resultMap;
            }
            HashMap hashMap;
            try {
                hashMap = JSONObject.parseObject(result, HashMap.class);
            } catch (Exception e) {
                e.printStackTrace();
                resultMap.put("message", "获取token授权信息时发生异常-连接失败 {} " + result);
                return resultMap;
            }
            if (CollectionUtils.isEmpty(hashMap)) {
                resultMap.put("message", "获取token授权接口-返回信息无法解析! {} " + result);
                return resultMap;
            }
            if (!hashMap.containsKey("ticket")) {
                resultMap.put("message", "获取token授权接口-返回信息无ticket字段! {} " + result);
                return resultMap;
            }
            if (hashMap.containsKey("serviceList")) {
                List<Map<String, Object>> urlList = (List) hashMap.get("serviceList");
                for (Map<String, Object> objectMap : urlList) {
                    String name = objectMap.get("name").toString();
                    insertDBData.put(name + orgId, JSONObject.toJSONString(objectMap));
                }
            }
            Map data = (Map) hashMap.get("ticket");
            insertDBData.put(Constants.INS_TOKEN + orgId, data.get("token").toString());
            token = data.get("token").toString();
        }
        resultMap.put("code", true);
        resultMap.put("token", token);
        return resultMap;
    }

    /**
     * Status modification
     */
    private void updateStateError(Map<String, Object> paramMap, String error) {
        String username = (String) paramMap.get("username");
        String password = (String) paramMap.get("password");
        String ftpUrl = paramMap.get("ftpUrl").toString();
        String id = paramMap.get("id").toString();
        Update update = new Update();
        update.set("fileSuccessOf", 2);
        update.set("fileSend", false);
        update.set("ftpUsername", username);
        update.set("ftpPassword", password);
        update.set("ftpHost", ftpUrl);
        update.set("auditSend", false);
        update.set("auditError", error);
        Query query = new Query().addCriteria(Criteria.where("_id").is(id));
        mongoTemplate.upsert(query, update, ResourceV2.class);
    }

    /**
     * Data transmission verification (restriction)
     */
    private void interruptValidation(String jobId) {
        Boolean ifPresent = publicFileStop.getIfPresent(jobId);
        if (null != ifPresent) {
            if (ifPresent) {
                log.info("线程：" + jobId + " {} 数据已被撤销，传输中断停止");
                throw new RuntimeException("数据已被撤销，传输中断停止");
            }
        }
    }
}
