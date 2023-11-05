package cn.cnic.dataspace.api.datax.admin.service.impl;

import cn.cnic.dataspace.api.config.space.FileMappingManage;
import cn.cnic.dataspace.api.datax.admin.core.scheduler.JobScheduler;
import cn.cnic.dataspace.api.datax.admin.core.thread.JobTriggerPoolHelper;
import cn.cnic.dataspace.api.datax.admin.core.trigger.TriggerTypeEnum;
import cn.cnic.dataspace.api.datax.admin.dto.*;
import cn.cnic.dataspace.api.datax.admin.dto.mapper.DataMappingStructMapper;
import cn.cnic.dataspace.api.datax.admin.entity.*;
import cn.cnic.dataspace.api.datax.admin.enums.DataMappingType;
import cn.cnic.dataspace.api.datax.admin.enums.SourceType;
import cn.cnic.dataspace.api.datax.admin.mapper.*;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingLockService;
import cn.cnic.dataspace.api.datax.admin.service.DataMappingService;
import cn.cnic.dataspace.api.datax.admin.service.DataxJsonService;
import cn.cnic.dataspace.api.datax.admin.service.JobService;
import cn.cnic.dataspace.api.datax.admin.tool.database.ColumnInfo;
import cn.cnic.dataspace.api.datax.admin.tool.database.TableInfo;
import cn.cnic.dataspace.api.datax.admin.tool.datax.dto.BuildDto;
import cn.cnic.dataspace.api.datax.admin.tool.datax.dto.BuildMongoDBDto;
import cn.cnic.dataspace.api.datax.admin.tool.datax.dto.BuildRDBMSDto;
import cn.cnic.dataspace.api.datax.admin.tool.sql.CommonDBUtils;
import cn.cnic.dataspace.api.datax.admin.tool.sql.JdbcConnectionFactory;
import cn.cnic.dataspace.api.datax.admin.tool.sql.SqlUtils;
import cn.cnic.dataspace.api.datax.admin.util.JdbcConstants;
import cn.cnic.dataspace.api.datax.admin.util.excel.*;
import cn.cnic.dataspace.api.datax.core.biz.ExecutorBiz;
import cn.cnic.dataspace.api.datax.core.biz.model.ReturnT;
import cn.cnic.dataspace.api.elfinder.core.Target;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorageService;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.user.ConsumerDO;
import cn.cnic.dataspace.api.repository.SpaceRepository;
import cn.cnic.dataspace.api.repository.SvnSpaceLogRepository;
import cn.cnic.dataspace.api.repository.UserRepository;
import cn.cnic.dataspace.api.service.space.SpaceService;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.SpaceSizeControl;
import cn.cnic.dataspace.api.util.Token;
import cn.cnic.dataspace.api.websocket.WebSocketProcess;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.enums.ApiErrorCode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.io.IOUtils;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import static cn.cnic.dataspace.api.model.space.SpaceSvnLog.*;
import static cn.cnic.dataspace.api.util.CommonUtils.messageInternational;

@Service
public class DataMappingServiceImpl extends ServiceImpl<DataMappingMapper, DataMapping> implements DataMappingService {

    @Resource
    private DataMappingMapper dataMappingMapper;

    @Resource
    private DataMappingLockService dataMappingLockService;

    @Resource
    private ImportExcelTaskMapper importExcelTaskMapper;

    @Resource
    private DataMappingStructMapper dataMappingStructMapper;

    @Resource
    private SpaceRepository spaceRepository;

    @Resource
    private SvnSpaceLogRepository svnSpaceLogRepository;

    @Resource
    private DataMappingMetaMapper dataMappingMetaMapper;

    @Resource
    private UserRepository userRepository;

    @Resource
    private ElfinderStorageService elfinderStorageService;

    @Resource
    private DataxJsonService dataxJsonService;

    @Resource
    private JobService jobService;

    @Resource
    private JobLogMapper jobLogMapper;

    @Resource
    private JobDatasourceMapper jobDatasourceMapper;

    @Resource
    private ExportExcelTaskMapper exportExcelTaskMapper;

    @Value("${file.import_excel_path}")
    private String IMPORT_EXCEL_PATH;

    @Resource
    private WebSocketProcess webSocketProcess;

    @Resource
    private FileMappingManage fileMappingManage;

    private static final String[][] ESCAPES = { { "+", "_P" }, { "-", "_M" }, { "/", "_S" }, { ".", "_D" }, { "=", "_E" } };

    private int poolSize = Runtime.getRuntime().availableProcessors() * 2;

    private static final int LIMIT = 1000;

    private ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("import-excel-pool-%d").build();

    // Import Excel locally and import Excel from space using the same thread pool to export Excel to space
    ExecutorService executorService = new ThreadPoolExecutor(poolSize, poolSize * 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(LIMIT), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());

    /**
     * Send notification to the front-end to refresh the structured data list
     */
    private void socketMessage2AllClient(String spaceId) {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("mark", "table_refresh");
        messageMap.put("spaceId", spaceId);
        try {
            webSocketProcess.sendMessage2AllSpaceClient(spaceId, JSONObject.toJSONString(messageMap));
            log.error("---- socket send ----");
        } catch (Exception e) {
            log.error("空间结构化数据列表刷新消息通知发送失败: {} " + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> delete(String spaceId, String userId, String dataMappingId) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (!dataMappingLockService.tryLockDataMapping(Long.valueOf(dataMappingId), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        // Delete table
        String deleteTableName = SqlUtils.getUUID32();
        // Swap names for name and deletename
        DataMapping update = new DataMapping();
        update.setId(dataMapping.getId());
        update.setName(deleteTableName);
        update.setDeleteName(dataMapping.getName());
        // Updating status here does not take effect
        update.setStatus(0);
        dataMappingMapper.updateById(update);
        // delete
        dataMappingMapper.deleteById(dataMappingId);
        // Modify Table Name
        String alterTableNameSql = SqlUtils.generateAlterTableNameSql(space.getDbName(), dataMapping.getName(), deleteTableName);
        Connection connection = null;
        try {
            // Here is a message to determine if it is repeated
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            CommonDBUtils.executeSql(connection, alterTableNameSql);
        } catch (SQLException e) {
            log.error("修改表名称失败");
            log.error(e.getMessage(), e);
            // TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            // return R.failed(CommonUtils.messageInternational("delete_datamapping_failed"));
            // Table import failed and can be successfully deleted without creating a table
            return R.ok(true);
        } finally {
            dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, false);
            CommonDBUtils.closeDBResources(connection);
        }
        // If there are scheduled tasks, close the scheduled tasks
        stopImportFromDataSource(spaceId, userId, dataMapping.getId());
        kill(dataMapping.getId());
        addLog(spaceId, userId, "删除结构化数据 " + dataMapping.getName());
        // Notify all frontends to refresh the structured data list page
        socketMessage2AllClient(spaceId);
        return R.ok(true);
    }

    /**
     * kill running job
     *
     * @param dataMappingId
     */
    private void kill(Long dataMappingId) {
        JobInfo jobInfo = jobService.getJobByDataMappingId(dataMappingId);
        if (jobInfo == null) {
            return;
        }
        List<JobLog> jobLogs = jobLogMapper.getSuccessTriggerListByJobId(jobInfo.getId());
        if (CollectionUtils.isEmpty(jobLogs)) {
            return;
        }
        // request of kill
        try {
            ExecutorBiz executorBiz = JobScheduler.getExecutorBiz(jobLogs.get(0).getExecutorAddress());
            ReturnT<String> kill = executorBiz.kill(jobInfo.getId());
            if (kill.getCode() != ReturnT.SUCCESS_CODE) {
                log.error(kill.getMsg());
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> copy(String spaceId, String currentUserId, Long dataMappingId, String newName) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        if (StringUtils.isEmpty(newName)) {
            newName = dataMapping.getName() + "_";
        }
        if (!dataMappingLockService.tryLockDataMapping(Long.valueOf(dataMappingId), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        // insert
        DataMapping newDataMapping = new DataMapping();
        newDataMapping.setName(newName);
        newDataMapping.setIsPublic(1);
        newDataMapping.setStatus(1);
        newDataMapping.setType(DataMappingType.FILE);
        newDataMapping.setSpaceId(spaceId);
        newDataMapping.setSource("从(" + dataMapping.getName() + ")复制");
        newDataMapping.setSourceType(SourceType.COPY);
        newDataMapping.setUpdateBy(currentUserId);
        try {
            dataMappingMapper.insert(newDataMapping);
        } catch (DuplicateKeyException e) {
            dataMappingLockService.releaseLock(dataMappingId, spaceId, false);
            return R.failed(CommonUtils.messageInternational("structured_data_name_duplicate"));
        }
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            String createLikeSql = SqlUtils.generateCreateLikeSql(space.getDbName(), dataMapping.getName(), newName);
            CommonDBUtils.executeSql(connection, createLikeSql);
        } catch (Exception e) {
            log.error("执行create like sql失败");
            log.error(e.getMessage(), e);
            CommonDBUtils.closeDBResources(connection);
            dataMappingLockService.releaseLock(dataMappingId, spaceId, false);
            // rollback
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return R.failed(CommonUtils.messageInternational("copy_failed"));
        }
        try {
            String insertFromSelectSql = SqlUtils.generateInsertFromSelectSql(space.getDbName(), dataMapping.getName(), newName);
            CommonDBUtils.executeSql(connection, insertFromSelectSql);
        } catch (SQLException e) {
            log.error("执行insert select sql失败");
            log.error(e.getMessage(), e);
            // rollback
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return R.failed(CommonUtils.messageInternational("copy_failed"));
        } finally {
            dataMappingLockService.releaseLock(dataMappingId, spaceId, false);
            CommonDBUtils.closeDBResources(connection);
        }
        addLog(spaceId, currentUserId, "从(" + dataMapping.getName() + ")复制结构化数据 (" + newName + ")");
        // Notify all frontends to refresh the structured data list page
        socketMessage2AllClient(spaceId);
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> rename(String spaceId, String userId, String dataMappingId, String newName) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), spaceId, false)) {
            return R.failed(CommonUtils.messageInternational("data_lock"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        // update
        DataMapping update = new DataMapping();
        update.setId(dataMapping.getId());
        update.setName(newName);
        update.setUpdateBy(userId);
        try {
            dataMappingMapper.updateById(update);
        } catch (DuplicateKeyException e) {
            dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, false);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return R.failed(CommonUtils.messageInternational("structured_data_name_duplicate"));
        }
        // Modify tableName
        String alterTableNameSql = SqlUtils.generateAlterTableNameSql(space.getDbName(), dataMapping.getName(), newName);
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            // Set the timeout for executing SQL statements to 10 seconds
            CommonDBUtils.executeSql(connection, alterTableNameSql, 10);
        } catch (Exception e) {
            log.error("修改表名称失败");
            log.error(e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return R.failed(CommonUtils.messageInternational("alter_tablename_failed"));
        } finally {
            dataMappingLockService.releaseLock(dataMapping.getId(), spaceId, false);
            CommonDBUtils.closeDBResources(connection);
        }
        addLog(spaceId, userId, "结构化数据文件 " + dataMapping.getName() + " 重命名为 " + newName);
        // update jobInfo
        if (dataMapping.getSourceType() == SourceType.DATASOURCE) {
            JobInfo jobByDataMappingId = jobService.getJobByDataMappingId(Long.valueOf(dataMappingId));
            if (jobByDataMappingId != null) {
                ImportFromDataSourceVo importFromDataSourceVo = jobByDataMappingId.getImportFromDataSourceVo();
                importFromDataSourceVo.setWriterTableName(newName);
                ReturnT<String> returnT = jobService.update(jobByDataMappingId);
                log.error(returnT.getMsg());
            }
        }
        // Notify all frontends to refresh the structured data list page
        socketMessage2AllClient(spaceId);
        return R.ok(true);
    }

    @Override
    public R<Boolean> updatePublic(String spaceId, String userId, String dataMappingId, String isPublic) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        DataMapping update = new DataMapping();
        update.setId(dataMapping.getId());
        update.setIsPublic(Integer.valueOf(isPublic));
        dataMappingMapper.updateById(update);
        String msg = "仅创建人可见";
        if ("1".equals(isPublic)) {
            msg = "公开";
        }
        addLog(spaceId, userId, "更新结构化数据 " + dataMapping.getName() + " 为 " + msg);
        // Notify all frontends to refresh the structured data list page
        socketMessage2AllClient(spaceId);
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> importExcel(String spaceId, String userId, MultipartFile file, String isHeader, String sheetName, Integer sheetNum) {
        Space space = spaceRepository.findById(spaceId).get();
        if (StringUtils.isEmpty(sheetName)) {
            return R.failed(CommonUtils.messageInternational("param_error"));
        }
        if (!"1".equals(isHeader) && !"0".equals(isHeader)) {
            return R.failed(CommonUtils.messageInternational("param_error"));
        }
        if (!file.getOriginalFilename().endsWith(".xls") && !file.getOriginalFilename().endsWith(".xlsx")) {
            return R.failed(CommonUtils.messageInternational("not_support_other_type"));
        }
        if (file.getSize() <= 0) {
            return R.failed(CommonUtils.messageInternational("excel_file_size0"));
        }
        // ConsumerDO consumerDO = userRepository.findById(userId).get();
        // File tempFile = new File("/Users/apple/tmp/" + SqlUtils.getUUID32() + File.separator + file.getOriginalFilename());
        File tempFile = new File(IMPORT_EXCEL_PATH + SqlUtils.getUUID32() + File.separator + file.getOriginalFilename());
        tempFile.getParentFile().mkdirs();
        try {
            file.transferTo(tempFile);
        } catch (IOException e) {
            log.error("文件存储失败");
            log.error(e.getMessage(), e);
            return R.failed(CommonUtils.messageInternational("import_excel_failed"));
        }
        if (sheetName.contains(",")) {
            // Import multiple sheets
            Map<String, ImportExcelTask> taskMap = new HashMap<>();
            Map<String, String> tableNameMap = new HashMap<>();
            String[] sheetNames = sheetName.split(",");
            for (String sheet : sheetNames) {
                // First, perform the operations that can be rolled back here
                String tableName = sheet;
                try {
                    ImportExcelTask importExcelTask = insertDB(tableName, spaceId, userId, tempFile, sheet, SourceType.OFFLINE);
                    taskMap.put(sheet, importExcelTask);
                    tableNameMap.put(sheet, tableName);
                } catch (DuplicateKeyException e) {
                    // Add a random number directly after sheetName with duplicate names
                    try {
                        tableName = sheet + "_" + NoModelDataListener.getRandomStringByLength(4);
                        ImportExcelTask importExcelTask = insertDB(tableName, spaceId, userId, tempFile, sheet, SourceType.OFFLINE);
                        taskMap.put(sheet, importExcelTask);
                        tableNameMap.put(sheet, tableName);
                    } catch (DuplicateKeyException e1) {
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                        return R.failed(CommonUtils.messageInternational("structured_data_name_duplicate"));
                    }
                }
            }
            for (String name : sheetNames) {
                // Subsequent operations that cannot be rolled back
                // parse excel
                ReadExcelThread readExcelThread = new ReadExcelThread(tempFile.getPath(), isHeader, name, sheetNum, space.getDbName(), tableNameMap.get(name), taskMap.get(name), importExcelTaskMapper, dataMappingLockService);
                executorService.execute(readExcelThread);
                addLog(spaceId, userId, "从成员本地文件导入(" + tempFile.getName() + ",sheetName " + name + ")");
            }
        } else {
            String tableName = sheetName;
            try {
                ImportExcelTask importExcelTask = insertDB(tableName, spaceId, userId, tempFile, sheetName, SourceType.OFFLINE);
                ReadExcelThread readExcelThread = new ReadExcelThread(tempFile.getPath(), isHeader, sheetName, sheetNum, space.getDbName(), tableName, importExcelTask, importExcelTaskMapper, dataMappingLockService);
                executorService.execute(readExcelThread);
            } catch (DuplicateKeyException e) {
                try {
                    tableName = sheetName + "_" + NoModelDataListener.getRandomStringByLength(4);
                    ImportExcelTask importExcelTask = insertDB(tableName, spaceId, userId, tempFile, sheetName, SourceType.OFFLINE);
                    ReadExcelThread readExcelThread = new ReadExcelThread(tempFile.getPath(), isHeader, sheetName, sheetNum, space.getDbName(), tableName, importExcelTask, importExcelTaskMapper, dataMappingLockService);
                    executorService.execute(readExcelThread);
                } catch (DuplicateKeyException e1) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return R.failed(CommonUtils.messageInternational("structured_data_name_duplicate"));
                }
            }
            addLog(spaceId, userId, "从成员本地文件导入(" + tempFile.getName() + ",sheetName " + sheetName + ")");
        }
        // Notify all frontends to refresh the structured data list page
        socketMessage2AllClient(spaceId);
        return R.ok(true);
    }

    @Override
    public void exportExcel(HttpServletResponse response, String spaceId, String userId, Long dataMappingId) throws IOException {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            // return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
            Map<String, Object> result = new HashMap<>();
            result.put("code", -1);
            result.put("msg", CommonUtils.messageInternational("structured_data_not_found"));
            setResponse(response, result);
            return;
        }
        // if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), false)) {
        // Map<String, Object> result = new HashMap<>();
        // result.put("code", -1);
        // result.put("msg", CommonUtils.messageInternational("data_lock"));
        // setResponse(response, result);
        // return;
        // }
        Space space = spaceRepository.findById(spaceId).get();
        // Please note that some students have reported that using Swagger can cause various problems. Please use the browser directly or use Postman
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        // response.addHeader("Content-Length", "" + file.length());
        // Here, URLEncoder.encode can prevent Chinese garbled code
        String fileName = URLEncoder.encode(dataMapping.getName(), "UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");
        try {
            ExportExcelUtils.export(null, response.getOutputStream(), space.getDbName(), dataMapping.getName());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setHeader("Content-Disposition", "");
            Map<String, Object> result = new HashMap<>();
            result.put("code", -1);
            result.put("msg", CommonUtils.messageInternational("query_data_failed"));
            setResponse(response, result);
            return;
        }
        addLog(spaceId, userId, "导出Excel " + dataMapping.getName() + ".xlsx");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public // Rollback of tasks exceeding the capacity of the thread pool
    R<Boolean> exportExcelToSpace(HttpServletRequest request, String spaceId, Token currentUser, Long dataMappingId, String hash) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (SpaceSizeControl.validation(spaceId)) {
            // Space capacity limitations
            throw new CommonException(messageInternational("FILE_SIZE_FULL"));
        }
        // if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId(), false)) {
        // return R.failed(CommonUtils.messageInternational("data_lock"));
        // }
        Space space = spaceRepository.findById(spaceId).get();
        // Server Path
        String spacePath = "";
        // Display Path
        String viewPath = "";
        try {
            ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorage(request, spaceId);
            Target target = elfinderStorage.fromHash(hash);
            spacePath = target.toString();
            viewPath = "/" + target.getVolume().getPath(target);
        } catch (Exception e) {
            log.error("elfinder解析hash失败");
            log.error(e.getMessage(), e);
            // dataMappingLockService.releaseLock(dataMappingId, false);
            return R.failed(messageInternational("FILE_SPACE_ERROR"));
        }
        if (!new File(spacePath).exists()) {
            // dataMappingLockService.releaseLock(dataMappingId, false);
            return R.failed(CommonUtils.messageInternational("space_dir_not_found"));
        }
        if (new File(spacePath).isFile()) {
            // dataMappingLockService.releaseLock(dataMappingId);
            return R.failed(CommonUtils.messageInternational("please_select_dir"));
        }
        String targetFilePath = spacePath + File.separator + dataMapping.getName() + ".xlsx";
        if (new File(targetFilePath).exists()) {
            // dataMappingLockService.releaseLock(dataMappingId);
            return R.failed(CommonUtils.messageInternational("FILE_EXIST"));
        }
        ExportExcelTask exportExcelTask = new ExportExcelTask();
        exportExcelTask.setDataMappingId(dataMappingId);
        exportExcelTask.setTaskDesc("导出Excel " + dataMapping.getName() + ".xlsx 到空间 " + viewPath);
        exportExcelTask.setSpaceId(spaceId);
        exportExcelTask.setStatus(1);
        exportExcelTask.setTargetPath(targetFilePath);
        exportExcelTaskMapper.insert(exportExcelTask);
        Operator operator = new Operator(currentUser);
        // Create spatial file metadata
        fileMappingManage.transit(FILE_CREATE, spaceId, Paths.get(targetFilePath), false, false, TABLE, 0, operator);
        ExportExcelThread exportExcelThread = new ExportExcelThread(targetFilePath, space.getDbName(), dataMapping.getName(), exportExcelTaskMapper, exportExcelTask, operator);
        try {
            executorService.execute(exportExcelThread);
        } catch (RejectedExecutionException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return R.failed(CommonUtils.messageInternational(""));
        } finally {
            // dataMappingLockService.releaseLock(dataMappingId);
        }
        addLog(spaceId, currentUser.getUserId(), "导出Excel " + dataMapping.getName() + ".xlsx 到空间 " + viewPath);
        return R.ok(true);
    }

    private R<String> getFilePathFromHash(HttpServletRequest request, String spaceId, String hash, String isHeader, String sheetName, int fromType) {
        if (StringUtils.isEmpty(sheetName)) {
            return R.failed(CommonUtils.messageInternational("param_error"));
        }
        if (!"1".equals(isHeader) && !"0".equals(isHeader)) {
            return R.failed(CommonUtils.messageInternational("param_error"));
        }
        // Server Local File Directory
        String spacePath = "";
        try {
            if (SourceType.codeOf(fromType) == SourceType.OFFLINE) {
                for (String[] pair : ESCAPES) {
                    hash = hash.replace(pair[1], pair[0]);
                }
                spacePath = new String(Base64.decodeBase64(hash));
            } else {
                ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorage(request, spaceId);
                Target target = elfinderStorage.fromHash(hash);
                spacePath = target.toString();
            }
        } catch (Exception e) {
            log.error("elfinder解析hash失败");
            log.error(e.getMessage(), e);
            return R.failed(messageInternational("FILE_SPACE_ERROR"));
        }
        if (StringUtils.isEmpty(spacePath)) {
            return R.failed(messageInternational("FILE_SPACE_ERROR"));
        }
        File targetFile = new File(spacePath);
        if (!targetFile.exists()) {
            return R.failed(CommonUtils.messageInternational("file_not_exist"));
        }
        if (!targetFile.isFile()) {
            return R.failed(CommonUtils.messageInternational("must_file"));
        }
        if (!targetFile.getName().endsWith(".xlsx")) {
            return R.failed(CommonUtils.messageInternational("not_support_other_type"));
        }
        if (targetFile.length() <= 0) {
            return R.failed(CommonUtils.messageInternational("excel_file_size0"));
        }
        return R.ok(spacePath);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> importExcelFromSpaceFile(HttpServletRequest request, String spaceId, String userId, String hash, String isHeader, String sheetName, Integer sheetNum, int fromType) {
        R<String> stringR = getFilePathFromHash(request, spaceId, hash, isHeader, sheetName, fromType);
        String spacePath = stringR.getData();
        if (!stringR.ok()) {
            return R.failed(stringR.getMsg());
        }
        File targetFile = new File(spacePath);
        Space space = spaceRepository.findById(spaceId).get();
        if (sheetName.contains(",")) {
            // Import multiple sheets
            Map<String, ImportExcelTask> taskMap = new HashMap<>();
            Map<String, String> tableNameMap = new HashMap<>();
            String[] sheetNames = sheetName.split(",");
            for (String sheet : sheetNames) {
                // First, perform the operations that can be rolled back here
                String tableName = sheet;
                try {
                    ImportExcelTask importExcelTask = insertDB(tableName, spaceId, userId, targetFile, sheet, SourceType.codeOf(fromType));
                    taskMap.put(sheet, importExcelTask);
                    tableNameMap.put(sheet, tableName);
                } catch (DuplicateKeyException e) {
                    try {
                        tableName = sheet + "_" + NoModelDataListener.getRandomStringByLength(4);
                        ImportExcelTask importExcelTask = insertDB(tableName, spaceId, userId, targetFile, sheet, SourceType.codeOf(fromType));
                        taskMap.put(sheet, importExcelTask);
                        tableNameMap.put(sheet, tableName);
                    } catch (DuplicateKeyException e1) {
                        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                        return R.failed(CommonUtils.messageInternational("structured_data_name_duplicate"));
                    }
                }
            }
            for (String name : sheetNames) {
                // Subsequent operations that cannot be rolled back
                // parse excel
                ReadExcelThread readExcelThread = new ReadExcelThread(targetFile.getPath(), isHeader, name, sheetNum, space.getDbName(), tableNameMap.get(name), taskMap.get(name), importExcelTaskMapper, dataMappingLockService);
                executorService.execute(readExcelThread);
                if (SourceType.codeOf(fromType) == SourceType.SPACE) {
                    addLog(spaceId, userId, String.format("从空间文件(%s,sheetName %s)导入(%s)", targetFile.getName(), name, tableNameMap.get(name)));
                } else {
                    addLog(spaceId, userId, String.format("从本地文件(%s,sheetName %s)导入(%s)", targetFile.getName(), name, tableNameMap.get(name)));
                }
                // AddLog (spaceId, userId, "Import from space file ("+targetFile. getName()+", sheetName"+name+")");
            }
        } else {
            String tableName = sheetName;
            try {
                ImportExcelTask importExcelTask = insertDB(tableName, spaceId, userId, targetFile, sheetName, SourceType.codeOf(fromType));
                ReadExcelThread readExcelThread = new ReadExcelThread(targetFile.getPath(), isHeader, sheetName, sheetNum, space.getDbName(), tableName, importExcelTask, importExcelTaskMapper, dataMappingLockService);
                executorService.execute(readExcelThread);
            } catch (DuplicateKeyException e) {
                try {
                    tableName = sheetName + "_" + NoModelDataListener.getRandomStringByLength(4);
                    ImportExcelTask importExcelTask = insertDB(tableName, spaceId, userId, targetFile, sheetName, SourceType.codeOf(fromType));
                    ReadExcelThread readExcelThread = new ReadExcelThread(targetFile.getPath(), isHeader, sheetName, sheetNum, space.getDbName(), tableName, importExcelTask, importExcelTaskMapper, dataMappingLockService);
                    executorService.execute(readExcelThread);
                } catch (DuplicateKeyException e1) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                    return R.failed(CommonUtils.messageInternational("structured_data_name_duplicate"));
                }
            }
            if (SourceType.codeOf(fromType) == SourceType.SPACE) {
                addLog(spaceId, userId, String.format("从空间文件(%s,sheetName %s)导入(%s)", targetFile.getName(), sheetName, tableName));
            } else {
                addLog(spaceId, userId, String.format("从本地文件(%s,sheetName %s)导入(%s)", targetFile.getName(), sheetName, tableName));
            }
            // AddLog (spaceId, userId, "Import from space file ("+targetFile. getName()+", sheetName"+sheetName+")");
        }
        // Notify all frontends to refresh the structured data list page
        socketMessage2AllClient(spaceId);
        return R.ok(true);
    }

    @Override
    public R<Boolean> incrementImportExcelFromFile(HttpServletRequest request, String spaceId, String userId, String hash, String isHeader, String sheetName, int fromType, Long dataMappingId) {
        R<String> filePathFromHash = getFilePathFromHash(request, spaceId, hash, isHeader, sheetName, fromType);
        String spacePath = filePathFromHash.getData();
        if (!filePathFromHash.ok()) {
            return R.failed(filePathFromHash.getMsg());
        }
        File targetFile = new File(spacePath);
        Space space = spaceRepository.findById(spaceId).get();
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        List<String> columns = Lists.newArrayList();
        JdbcConnectionFactory jdbcConnectionFactory = new JdbcConnectionFactory();
        Connection connection = null;
        String sql = String.format("select * from `%s`.`%s` where 0=1", space.getDbName(), dataMapping.getName());
        try {
            connection = jdbcConnectionFactory.getConnection();
            ResultSet query = CommonDBUtils.query(connection, sql, 1);
            ResultSetMetaData metaData = query.getMetaData();
            for (int i = 0; i < metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnName(i + 1);
                if (!JdbcConstants.PRIMARY_KEY.equals(columnName)) {
                    columns.add(columnName);
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return R.failed(CommonUtils.messageInternational("query_data_failed"));
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        if (CollectionUtils.isEmpty(columns)) {
            return R.failed(CommonUtils.messageInternational("query_data_failed"));
        }
        if (sheetName.contains(",")) {
            // Import multiple sheets
            String[] sheetNames = sheetName.split(",");
            for (String sheet : sheetNames) {
                // First, perform the operations that can be rolled back here
                ImportExcelTask importExcelTask = createImportTask(dataMappingId, spaceId, targetFile, sheet, SourceType.codeOf(fromType));
                ReadExcelThread2Increment readExcelThread = new ReadExcelThread2Increment(targetFile.getPath(), isHeader, sheet, space.getDbName(), dataMapping.getName(), columns, importExcelTask, importExcelTaskMapper, dataMappingLockService);
                executorService.execute(readExcelThread);
                if (SourceType.codeOf(fromType) == SourceType.SPACE) {
                    addLog(spaceId, userId, String.format("从空间文件(%s,sheetName %s)导入(%s)", targetFile.getName(), sheet, dataMapping.getName()));
                } else {
                    addLog(spaceId, userId, String.format("从本地文件(%s,sheetName %s)导入(%s)", targetFile.getName(), sheet, dataMapping.getName()));
                }
            }
        } else {
            ImportExcelTask importExcelTask = createImportTask(dataMappingId, spaceId, targetFile, sheetName, SourceType.codeOf(fromType));
            ReadExcelThread2Increment readExcelThread = new ReadExcelThread2Increment(targetFile.getPath(), isHeader, sheetName, space.getDbName(), dataMapping.getName(), columns, importExcelTask, importExcelTaskMapper, dataMappingLockService);
            executorService.execute(readExcelThread);
            if (SourceType.codeOf(fromType) == SourceType.SPACE) {
                addLog(spaceId, userId, String.format("从空间文件(%s,sheetName %s)导入(%s)", targetFile.getName(), sheetName, dataMapping.getName()));
            } else {
                addLog(spaceId, userId, String.format("从本地文件(%s,sheetName %s)导入(%s)", targetFile.getName(), sheetName, dataMapping.getName()));
            }
        }
        // Notify all frontends to refresh the structured data list page
        socketMessage2AllClient(spaceId);
        return R.ok(true);
    }

    @Override
    public R<Map<String, Object>> getData(String spaceId, String userId, String dataMappingId, Integer isReturnId, Integer current, Integer size) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        // if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId())) {
        // Map<String, Object> ret = new HashMap<>();
        // ret.put("total", 0);
        // ret.put("current", 1);
        // ret.put("colInfos", Lists.newArrayList());
        // ret.put("size", 10);
        // ret.put("records", Lists.newArrayList());
        // return R.ok(ret);
        // }
        Space space = spaceRepository.findById(spaceId).get();
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            String count = "0";
            String countSql = SqlUtils.generateSelectCountSql(space.getDbName(), dataMapping.getName());
            String sql = SqlUtils.generateSelectSql(space.getDbName(), dataMapping.getName(), (current - 1) * size, size);
            ResultSet query = CommonDBUtils.query(connection, countSql, 1);
            while (query.next()) {
                count = query.getString(1);
            }
            query.close();
            ResultSet query2 = CommonDBUtils.query(connection, sql, 100);
            ResultSetMetaData metaData = query2.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<Map<String, String>> colInfos = new ArrayList<>();
            for (int i = 0; i < columnCount; i++) {
                Map<String, String> colInfo = new HashMap<>();
                String columnLabel = metaData.getColumnLabel(i + 1);
                String columnTypeName = metaData.getColumnTypeName(i + 1);
                if (isReturnId == 1) {
                    colInfo.put("key", columnLabel);
                    colInfo.put("type", SqlUtils.getViewType(columnTypeName));
                    colInfos.add(colInfo);
                } else {
                    if (!JdbcConstants.PRIMARY_KEY.equals(columnLabel)) {
                        colInfo.put("key", columnLabel);
                        colInfo.put("type", SqlUtils.getViewType(columnTypeName));
                        colInfos.add(colInfo);
                    }
                }
            }
            List<List<Object>> lines = new ArrayList<>();
            while (query2.next()) {
                List<Object> line = new ArrayList<>();
                for (int i = 0; i < columnCount; i++) {
                    Object object = query2.getObject(i + 1);
                    if (isReturnId == 1) {
                        line.add(object);
                    } else {
                        String columnLabel = metaData.getColumnLabel(i + 1);
                        if (!JdbcConstants.PRIMARY_KEY.equals(columnLabel)) {
                            line.add(object);
                        }
                    }
                }
                lines.add(line);
            }
            query2.close();
            Map<String, Object> ret = new HashMap<>();
            ret.put("total", count);
            ret.put("current", current);
            ret.put("colInfos", colInfos);
            ret.put("size", size);
            ret.put("records", lines);
            return R.ok(ret);
        } catch (SQLException e) {
            log.error(CommonUtils.messageInternational("query_data_failed"));
            log.error(e.getMessage(), e);
            Map<String, Object> ret = new HashMap<>();
            ret.put("total", 0);
            ret.put("current", current);
            ret.put("colInfos", Lists.newArrayList());
            ret.put("size", size);
            ret.put("records", Lists.newArrayList());
            return R.ok(ret);
        } finally {
            CommonDBUtils.closeDBResources(connection);
            // dataMappingLockService.releaseLock(dataMapping.getId());
        }
    }

    @Override
    public R<Map<String, Object>> getDataBySortAndFilter(String spaceId, String userId, QueryDataMappingDataVO queryDataMappingDataVO) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(queryDataMappingDataVO.getDataMappingId(), spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        // if (!dataMappingLockService.tryLockDataMapping(dataMapping.getId())) {
        // Map<String, Object> ret = new HashMap<>();
        // ret.put("total", 0);
        // ret.put("current", 1);
        // ret.put("colInfos", Lists.newArrayList());
        // ret.put("size", 10);
        // ret.put("records", Lists.newArrayList());
        // return R.ok(ret);
        // }
        Map<String, QueryDataMappingDataVO.QuerySortFilter> querySortFilter = queryDataMappingDataVO.getQuerySortFilter();
        if (CollectionUtils.isEmpty(querySortFilter.keySet())) {
            return getData(spaceId, userId, queryDataMappingDataVO.getDataMappingId().toString(), 1, queryDataMappingDataVO.getCurrent(), queryDataMappingDataVO.getSize());
        }
        // if (CollectionUtils.isEmpty(queryDataMappingDataVO.getFilters())
        // && CollectionUtils.isEmpty(queryDataMappingDataVO.getSorts())) {
        // return getData(spaceId, userId, queryDataMappingDataVO.getDataMappingId().toString(), queryDataMappingDataVO.getCurrent(), queryDataMappingDataVO.getSize());
        // }
        Space space = spaceRepository.findById(spaceId).get();
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            String count = "0";
            String countSql = SqlUtils.generateSelectCountByFilterAndSql(space.getDbName(), dataMapping.getName(), querySortFilter);
            ResultSet query = CommonDBUtils.query(connection, countSql, 1);
            while (query.next()) {
                count = query.getString(1);
            }
            query.close();
            String sql = SqlUtils.generateSelectBySortAndFilterAndSql(space.getDbName(), dataMapping.getName(), (queryDataMappingDataVO.getCurrent() - 1) * queryDataMappingDataVO.getSize(), queryDataMappingDataVO.getSize(), querySortFilter);
            ResultSet query2 = CommonDBUtils.query(connection, sql, 100);
            ResultSetMetaData metaData = query2.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<Map<String, String>> colInfos = new ArrayList<>();
            for (int i = 0; i < columnCount; i++) {
                Map<String, String> colInfo = new HashMap<>();
                String columnLabel = metaData.getColumnLabel(i + 1);
                String columnTypeName = metaData.getColumnTypeName(i + 1);
                colInfo.put("key", columnLabel);
                colInfo.put("type", SqlUtils.getViewType(columnTypeName));
                colInfos.add(colInfo);
            }
            List<List<Object>> lines = new ArrayList<>();
            while (query2.next()) {
                List<Object> line = new ArrayList<>();
                for (int i = 0; i < columnCount; i++) {
                    Object object = query2.getObject(i + 1);
                    line.add(object);
                }
                lines.add(line);
            }
            query2.close();
            Map<String, Object> ret = new HashMap<>();
            ret.put("total", count);
            ret.put("current", queryDataMappingDataVO.getCurrent());
            ret.put("colInfos", colInfos);
            ret.put("size", queryDataMappingDataVO.getSize());
            ret.put("records", lines);
            return R.ok(ret);
        } catch (SQLException e) {
            log.error(CommonUtils.messageInternational("query_data_failed"));
            log.error(e.getMessage(), e);
            Map<String, Object> ret = new HashMap<>();
            ret.put("total", 0);
            ret.put("current", 1);
            ret.put("colInfos", Lists.newArrayList());
            ret.put("size", 10);
            ret.put("records", Lists.newArrayList());
            return R.ok(ret);
        } finally {
            CommonDBUtils.closeDBResources(connection);
            // dataMappingLockService.releaseLock(dataMapping.getId());
        }
    }

    @Override
    public R<Map<String, Object>> getSchema(String spaceId, String userId, String dataMappingId) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        Space space = spaceRepository.findById(dataMapping.getSpaceId()).get();
        String selectSqlNoResult = SqlUtils.generateSelectSqlNoResult(space.getDbName(), dataMapping.getName());
        Connection connection = null;
        ResultSet resultSet = null;
        List<Map<String, String>> colInfos = new ArrayList<>();
        Map<String, Object> ret = new HashMap<>();
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            resultSet = CommonDBUtils.query(connection, selectSqlNoResult, 1);
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 0; i < columnCount; i++) {
                String columnLabel = metaData.getColumnLabel(i + 1);
                String columnTypeName = metaData.getColumnTypeName(i + 1);
                String type = SqlUtils.getViewType(columnTypeName);
                Map<String, String> colInfo = new HashMap<>();
                colInfo.put("key", columnLabel);
                colInfo.put("type", type);
                colInfos.add(colInfo);
            }
            ret.put("col", colInfos);
            return R.ok(ret);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return R.failed(CommonUtils.messageInternational("query_data_failed"));
        } finally {
            CommonDBUtils.closeDBResources(resultSet, connection);
        }
    }

    @Override
    public IPage<DataMappingDto> selectByPage(String spaceId, String userId, String name, Integer current, Integer size) {
        Space space = spaceRepository.findById(spaceId).get();
        Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
        String role = "";
        for (AuthorizationPerson authorizationPerson : authorizationList) {
            if (userId.equals(authorizationPerson.getUserId())) {
                role = authorizationPerson.getRole();
            }
        }
        if (SpaceService.SPACE_SENIOR.equals(role)) {
            // Determine if it is an administrator. Administrator returns all
            Page<DataMapping> page = new Page<>(current, size);
            IPage<DataMapping> dataMappingPage = dataMappingMapper.selectPagingBySpaceIdAndName(page, spaceId, name);
            IPage<DataMappingDto> dto = new Page<>(dataMappingPage.getCurrent(), dataMappingPage.getSize());
            dto.setPages(dataMappingPage.getPages());
            dto.setRecords(dataMappingStructMapper.sourceToTarget(dataMappingPage.getRecords()));
            return dto;
        }
        Page<DataMapping> page = new Page<>(current, size);
        IPage<DataMapping> dataMappingPage = dataMappingMapper.getPagingBySpaceIdAndUserId(page, spaceId, userId, name);
        IPage<DataMappingDto> dto = new Page<>(dataMappingPage.getCurrent(), dataMappingPage.getSize());
        dto.setPages(dataMappingPage.getPages());
        dto.setRecords(dataMappingStructMapper.sourceToTarget(dataMappingPage.getRecords()));
        return dto;
    }

    @Override
    public List<DataMappingDto> selectAll(String spaceId, String userId, String name) {
        Space space = spaceRepository.findById(spaceId).get();
        Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
        String role = "";
        for (AuthorizationPerson authorizationPerson : authorizationList) {
            if (userId.equals(authorizationPerson.getUserId())) {
                role = authorizationPerson.getRole();
            }
        }
        if (SpaceService.SPACE_SENIOR.equals(role)) {
            // Determine if it is an administrator. Administrator returns all
            List<DataMapping> dataMappingList = dataMappingMapper.selectListBySpaceIdAndName(spaceId, name);
            List<DataMappingDto> dataMappingDtos = dataMappingStructMapper.sourceToTarget(dataMappingList);
            return dataMappingDtos;
        }
        List<DataMapping> dataMappingList = dataMappingMapper.getListBySpaceIdAndUserId(spaceId, userId, name);
        List<DataMappingDto> dataMappingDtos = dataMappingStructMapper.sourceToTarget(dataMappingList);
        for (DataMappingDto dataMappingDto : dataMappingDtos) {
            // If the data comes from a data source, whether to enable scheduled tasks
            if (dataMappingDto.getSourceType() == SourceType.DATASOURCE) {
                JobInfo jobInfo = jobService.getJobByDataMappingId(dataMappingDto.getId());
                if (jobInfo != null) {
                    dataMappingDto.setTriggerStatus(jobInfo.getTriggerStatus());
                }
            }
        }
        return dataMappingDtos;
    }

    @Override
    public List<DataMappingDto> selectAllWithDataCount(String spaceId, String userId, String name) {
        Space space = spaceRepository.findById(spaceId).get();
        Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
        String role = "";
        for (AuthorizationPerson authorizationPerson : authorizationList) {
            if (userId.equals(authorizationPerson.getUserId())) {
                role = authorizationPerson.getRole();
            }
        }
        if (SpaceService.SPACE_SENIOR.equals(role)) {
            // Determine if it is an administrator. Administrator returns all
            List<DataMapping> dataMappingList = dataMappingMapper.selectListBySpaceIdAndName(spaceId, name);
            List<DataMappingDto> dataMappingDtos = dataMappingStructMapper.sourceToTarget(dataMappingList);
            Connection connection = null;
            try {
                connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
                for (DataMappingDto dataMappingDto : dataMappingDtos) {
                    selectDataCount(connection, space.getDbName(), dataMappingDto);
                }
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            } finally {
                CommonDBUtils.closeDBResources(connection);
            }
            return dataMappingDtos;
        }
        List<DataMapping> dataMappingList = dataMappingMapper.getListBySpaceIdAndUserId(spaceId, userId, name);
        List<DataMappingDto> dataMappingDtos = dataMappingStructMapper.sourceToTarget(dataMappingList);
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            for (DataMappingDto dataMappingDto : dataMappingDtos) {
                selectDataCount(connection, space.getDbName(), dataMappingDto);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        return dataMappingDtos;
    }

    private void selectDataCount(Connection connection, String dbName, DataMappingDto dataMappingDto) {
        String countSql = SqlUtils.generateSelectCountSql(dbName, dataMappingDto.getName());
        ResultSet resultSet = null;
        try {
            resultSet = CommonDBUtils.query(connection, countSql, 1);
            while (resultSet.next()) {
                long count = resultSet.getLong(1);
                dataMappingDto.setDataCount(count);
            }
        } catch (SQLException e) {
            dataMappingDto.setDataCount(0L);
            log.error(e.getMessage(), e);
        } finally {
            CommonDBUtils.closeDBResources(resultSet, null);
        }
    }

    @Override
    public R<DataMappingDto> getBasicInfo(String spaceId, Long dataMappingId) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        // If the id is empty here, an exception will be thrown
        Optional<ConsumerDO> createUser = userRepository.findById(dataMapping.getCreateBy());
        Optional<ConsumerDO> updateUser = userRepository.findById(dataMapping.getUpdateBy());
        if (createUser.isPresent()) {
            dataMapping.setCreateByUserName(createUser.get().getName());
        }
        if (updateUser.isPresent()) {
            dataMapping.setUpdateByUserName(updateUser.get().getName());
        }
        DataMappingDto dataMappingDto = dataMappingStructMapper.sourceToTarget(dataMapping);
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            selectDataCount(connection, space.getDbName(), dataMappingDto);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        // If the data comes from a data source, whether to enable scheduled tasks
        if (dataMapping.getSourceType() == SourceType.DATASOURCE) {
            JobInfo jobInfo = jobService.getJobByDataMappingId(dataMappingId);
            dataMappingDto.setTriggerStatus(jobInfo.getTriggerStatus());
        }
        return R.ok(dataMappingDto);
    }

    @Override
    public R<List<String>> getSheetNames(HttpServletRequest request, String spaceId, String hash) {
        // Server Local File Directory
        String spacePath = "";
        try {
            ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorage(request, spaceId);
            Target target = elfinderStorage.fromHash(hash);
            spacePath = target.toString();
        } catch (Exception e) {
            log.error("elfinder解析hash失败");
            log.error(e.getMessage(), e);
            return R.failed(messageInternational("FILE_SPACE_ERROR"));
        }
        if (StringUtils.isEmpty(spacePath)) {
            return R.failed(messageInternational("FILE_SPACE_ERROR"));
        }
        File targetFile = new File(spacePath);
        if (!targetFile.exists()) {
            return R.failed(CommonUtils.messageInternational("file_not_exist"));
        }
        if (!targetFile.isFile()) {
            return R.failed(CommonUtils.messageInternational("must_file"));
        }
        if (!targetFile.getName().endsWith(".xlsx")) {
            return R.failed(CommonUtils.messageInternational("not_support_other_type"));
        }
        if (targetFile.length() <= 0) {
            return R.failed(CommonUtils.messageInternational("excel_file_size0"));
        }
        R<List<String>> listR = readExcel(spacePath);
        return listR;
    }

    @Override
    public R<Map<String, Object>> getSheetNames(HttpServletRequest request, String spaceId, MultipartFile file) {
        Map<String, Object> ret = new HashMap<>();
        if (!file.getOriginalFilename().endsWith(".xlsx")) {
            return R.failed(CommonUtils.messageInternational("not_support_other_type"));
        }
        if (file.getSize() <= 0) {
            return R.failed(CommonUtils.messageInternational("excel_file_size0"));
        }
        File tempFile = new File(IMPORT_EXCEL_PATH + SqlUtils.getUUID32() + File.separator + file.getOriginalFilename());
        tempFile.getParentFile().mkdirs();
        try {
            file.transferTo(tempFile);
        } catch (IOException e) {
            log.error("文件存储失败");
            log.error(e.getMessage(), e);
            return R.failed(CommonUtils.messageInternational("import_excel_failed"));
        }
        try {
            String hash = new String(Base64.encodeBase64(tempFile.getPath().getBytes()));
            for (String[] pair : ESCAPES) {
                hash = hash.replace(pair[0], pair[1]);
            }
            ret.put("hash", hash);
        } catch (Exception e) {
            log.error("elfinder解析hash失败");
            log.error(e.getMessage(), e);
            return R.failed(messageInternational("FILE_SPACE_ERROR"));
        }
        R<List<String>> listR = readExcel(tempFile.getPath());
        if (listR.getCode() != ApiErrorCode.SUCCESS.getCode()) {
            return R.failed(listR.getMsg());
        }
        ret.put("sheetNames", listR.getData());
        return R.ok(ret);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> importFromDataSource(String spaceId, String currentUserId, ImportFromDataSourceVo importFromDataSourceVo) {
        importFromDataSourceVo.setUpdateDate(new Date().getTime());
        if (StringUtils.isEmpty(importFromDataSourceVo.getWriterTableName())) {
            return R.failed(CommonUtils.messageInternational("param_error"));
        }
        if (StringUtils.isEmpty(importFromDataSourceVo.getReaderTableName())) {
            return R.failed(CommonUtils.messageInternational("param_error"));
        }
        if (CollectionUtils.isEmpty(importFromDataSourceVo.getReaderColumns())) {
            return R.failed(CommonUtils.messageInternational("param_error"));
        }
        if (CollectionUtils.isEmpty(importFromDataSourceVo.getWriterColumns())) {
            return R.failed(CommonUtils.messageInternational("param_error"));
        }
        JobDatasource jobDatasource = jobDatasourceMapper.selectById(importFromDataSourceVo.getDatasourceId());
        if (jobDatasource == null) {
            return R.failed(CommonUtils.messageInternational("datasource_not_found"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        // Create Table
        TableInfo newTableInfo = new TableInfo();
        newTableInfo.setName(importFromDataSourceVo.getWriterTableName());
        newTableInfo.setColumns(importFromDataSourceVo.getWriterColumns());
        DataMapping newDataMapping = new DataMapping();
        newDataMapping.setSource(String.format("从数据源(%s)表(%s)导入", jobDatasource.getDatasourceName(), importFromDataSourceVo.getReaderTableName()));
        newDataMapping.setName(importFromDataSourceVo.getWriterTableName());
        newDataMapping.setStatus(1);
        newDataMapping.setSpaceId(spaceId);
        newDataMapping.setType(DataMappingType.FILE);
        newDataMapping.setIsPublic(1);
        newDataMapping.setUpdateBy(currentUserId);
        newDataMapping.setSourceType(SourceType.DATASOURCE);
        try {
            dataMappingMapper.insert(newDataMapping);
        } catch (DuplicateKeyException e) {
            // Automatically add random suffixes for duplicate names
            String newDataMappingName = importFromDataSourceVo.getWriterTableName() + "_" + NoModelDataListener.getRandomStringByLength(4);
            newDataMapping.setName(newDataMappingName);
            importFromDataSourceVo.setWriterTableName(newDataMappingName);
            newTableInfo.setName(newDataMappingName);
            try {
                dataMappingMapper.insert(newDataMapping);
            } catch (DuplicateKeyException e1) {
                return R.failed(CommonUtils.messageInternational("structured_data_name_duplicate"));
            }
        }
        // Increase the usage of data sources
        jobDatasourceMapper.incrementCitationNum(jobDatasource.getId());
        String createTableSqlWithDefaultId = SqlUtils.generateCreateTableSqlWithDefaultId(space.getDbName(), newTableInfo);
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            CommonDBUtils.executeSql(connection, createTableSqlWithDefaultId);
        } catch (SQLException e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            log.error(CommonUtils.messageInternational("create_table_failed"));
            log.error(e.getMessage(), e);
            return R.failed(CommonUtils.messageInternational("create_table_failed"));
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        importFromDataSourceVo.setDataMappingId(newDataMapping.getId());
        importFromDataSourceVo.setDbName(space.getDbName());
        String dataxJson = "{}";
        if (JdbcConstants.MONGODB.equals(jobDatasource.getDatasource())) {
            BuildDto buildDto = new BuildMongoDBDto(importFromDataSourceVo, jobDatasourceMapper);
            DataXJsonBuildDto dataXJsonBuildDto = buildDto.getDataXJsonBuildDto();
            dataxJson = dataxJsonService.buildJobJson(dataXJsonBuildDto);
        } else {
            BuildDto buildDto = new BuildRDBMSDto(importFromDataSourceVo, jobDatasourceMapper);
            DataXJsonBuildDto dataXJsonBuildDto = buildDto.getDataXJsonBuildDto();
            dataxJson = dataxJsonService.buildJobJson(dataXJsonBuildDto);
        }
        JobInfo jobInfo = new JobInfo();
        jobInfo.setSpaceId(spaceId);
        jobInfo.setJobGroup(1);
        jobInfo.setJobDesc(String.format("从数据源(%s)表(%s)导入(%s)", jobDatasource.getDatasourceName(), importFromDataSourceVo.getReaderTableName(), importFromDataSourceVo.getWriterTableName()));
        jobInfo.setUserId(currentUserId);
        jobInfo.setExecutorRouteStrategy("RANDOM");
        jobInfo.setExecutorHandler("executorJobHandler");
        jobInfo.setExecutorBlockStrategy("SERIAL_EXECUTION");
        jobInfo.setExecutorTimeout(0);
        jobInfo.setExecutorFailRetryCount(0);
        jobInfo.setGlueType("JAVA_BEAN");
        jobInfo.setJobJson(dataxJson);
        jobInfo.setDataMappingId(newDataMapping.getId());
        jobInfo.setJobCron(importFromDataSourceVo.getCron());
        jobInfo.setImportFromDataSourceVo(importFromDataSourceVo);
        ReturnT<String> add = jobService.add(jobInfo);
        if (add.getCode() == 500) {
            // Delete Table Operation
            log.error(add.getMsg());
            deleteTable(space.getDbName(), importFromDataSourceVo.getWriterTableName());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return R.failed(add.getMsg());
        }
        String jobInfoId = add.getContent();
        addLog(spaceId, currentUserId, String.format(String.format("从数据源(%s)表(%s)导入(%s)", jobDatasource.getDatasourceName(), importFromDataSourceVo.getReaderTableName(), importFromDataSourceVo.getWriterTableName())));
        if (importFromDataSourceVo.getImportType() == 0) {
            // Immediately execute once
            try {
                // ExecutorParam=1 Execute a run
                JobTriggerPoolHelper.trigger(Integer.parseInt(jobInfoId), TriggerTypeEnum.MANUAL, -1, null, "");
            } catch (Exception e) {
                // Temporary execution failure does not affect previous operations
                log.error("执行trigger失败");
                log.error(e.getMessage(), e);
                return R.failed(e.getMessage());
                // TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                // Delete Table Operation
                // deleteTable(space.getDbName(), importFromDataSourceVo.getWriterTableName());
                // return R.failed(CommonUtils.messageInternational(""));
            }
        } else if (importFromDataSourceVo.getImportType() == 1) {
            // Start scheduled task
            ReturnT<String> start = jobService.start(Integer.parseInt(jobInfoId));
            if (start.getCode() == 500) {
                // Tentative start of scheduled task failure does not affect previous operations
                log.error(start.getMsg());
                return R.failed(start.getMsg());
                // TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                // Delete Table Operation
                // deleteTable(space.getDbName(), importFromDataSourceVo.getWriterTableName());
                // return R.failed(start.getMsg());
            }
        } else {
            // Not to be executed temporarily
        }
        // Notify all frontends to refresh the structured data list page
        socketMessage2AllClient(spaceId);
        return R.ok(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public R<Boolean> updateImportFromDataSource(String spaceId, String currentUserId, ImportFromDataSourceVo importFromDataSourceVo) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(importFromDataSourceVo.getDataMappingId(), spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (dataMapping.getSourceType() != SourceType.DATASOURCE) {
            return R.failed(CommonUtils.messageInternational("data_mapping_not_from_datasource"));
        }
        JobInfo jobInfo = jobService.getJobByDataMappingId(importFromDataSourceVo.getDataMappingId());
        if (jobInfo == null) {
            return R.failed(CommonUtils.messageInternational("job_info_not_found"));
        }
        Space space = spaceRepository.findById(spaceId).get();
        // old
        ImportFromDataSourceVo oldImportInfo = jobInfo.getImportFromDataSourceVo();
        // There may be concurrency issues with TODO. If someone modifies the update time before obtaining dataMapping, there may be issues
        if (oldImportInfo.getUpdateDate() != null && !oldImportInfo.getUpdateDate().toString().equals(importFromDataSourceVo.getUpdateDate().toString())) {
            return R.failed(CommonUtils.messageInternational("update_datasource_import_failed"));
        }
        JobDatasource jobDatasource = jobDatasourceMapper.selectById(importFromDataSourceVo.getDatasourceId());
        if (jobDatasource == null) {
            return R.failed(CommonUtils.messageInternational("datasource_not_found"));
        }
        importFromDataSourceVo.setDataMappingId(dataMapping.getId());
        importFromDataSourceVo.setDbName(space.getDbName());
        String dataxJson = "{}";
        if (JdbcConstants.MONGODB.equals(jobDatasource.getDatasource())) {
            BuildDto buildDto = new BuildMongoDBDto(importFromDataSourceVo, jobDatasourceMapper);
            DataXJsonBuildDto dataXJsonBuildDto = buildDto.getDataXJsonBuildDto();
            dataxJson = dataxJsonService.buildJobJson(dataXJsonBuildDto);
        } else {
            BuildDto buildDto = new BuildRDBMSDto(importFromDataSourceVo, jobDatasourceMapper);
            DataXJsonBuildDto dataXJsonBuildDto = buildDto.getDataXJsonBuildDto();
            dataxJson = dataxJsonService.buildJobJson(dataXJsonBuildDto);
        }
        jobInfo.setJobJson(dataxJson);
        jobInfo.setJobCron(importFromDataSourceVo.getCron());
        // Compare and identify the columns that need to be added
        List<ColumnInfo> oldWriterColumns = oldImportInfo.getWriterColumns();
        Set<String> oldWriterSet = new HashSet<>();
        oldWriterColumns.forEach(var -> oldWriterSet.add(var.getName()));
        List<ColumnInfo> writerColumns = importFromDataSourceVo.getWriterColumns();
        // Fields to be added
        List<ColumnInfo> addColumns = new ArrayList<>();
        for (ColumnInfo writerColumn : writerColumns) {
            if (!oldWriterSet.contains(writerColumn.getName())) {
                addColumns.add(writerColumn);
            }
        }
        // Use oldImportInfo to update and prevent updating values that should not be updated. The front-end should limit values that cannot be modified
        oldImportInfo.setUpdateDate(new Date().getTime());
        oldImportInfo.setCron(importFromDataSourceVo.getCron());
        oldImportInfo.setDatasourceId(importFromDataSourceVo.getDatasourceId());
        oldImportInfo.setReaderTableName(importFromDataSourceVo.getReaderTableName());
        oldImportInfo.setReaderColumns(importFromDataSourceVo.getReaderColumns());
        oldImportInfo.setWriterColumns(importFromDataSourceVo.getWriterColumns());
        oldImportInfo.setImportType(importFromDataSourceVo.getImportType());
        oldImportInfo.setWhereType(importFromDataSourceVo.getWhereType());
        oldImportInfo.setWhere(importFromDataSourceVo.getWhere());
        oldImportInfo.setFilterConditions(importFromDataSourceVo.getFilterConditions());
        // update db
        ReturnT<String> add = jobService.update(jobInfo);
        if (add.getCode() == 500) {
            return R.failed(add.getMsg());
        }
        // add col
        List<String> successCreatedCols = new ArrayList<>();
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(space.getDbName()).getConnection();
            for (ColumnInfo addColumn : addColumns) {
                String addColSql = SqlUtils.generateAddColSql(space.getDbName(), dataMapping.getName(), addColumn.getName(), addColumn.getType());
                CommonDBUtils.executeSql(connection, addColSql);
                successCreatedCols.add(addColumn.getName());
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            // Rollback to delete columns that have been successfully created
            dropCol(connection, space.getDbName(), dataMapping.getName(), successCreatedCols);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return R.failed(CommonUtils.messageInternational("add_col_failed"));
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        if (importFromDataSourceVo.getImportType() == 0) {
            // Immediately execute once
            // Immediately execute once
            R<Boolean> booleanR = triggerImportFromDataSource(spaceId, currentUserId, dataMapping.getId());
            if (booleanR.getCode() != ApiErrorCode.SUCCESS.getCode()) {
                log.error(booleanR.getMsg());
                return R.failed(booleanR.getMsg());
            }
        } else if (importFromDataSourceVo.getImportType() == 1) {
            // Start scheduled task
            R<Boolean> booleanR = startImportFromDataSource(spaceId, currentUserId, dataMapping.getId());
            if (booleanR.getCode() != ApiErrorCode.SUCCESS.getCode()) {
                log.error(booleanR.getMsg());
                return R.failed(booleanR.getMsg());
            }
        } else if (importFromDataSourceVo.getImportType() == 2) {
            // Stop timing
            ReturnT<String> stop = jobService.stop(jobInfo.getId());
            if (stop.getCode() != ReturnT.SUCCESS_CODE) {
                log.error(stop.getMsg());
                return R.failed(stop.getMsg());
            }
        }
        return R.ok(true);
    }

    /**
     * Batch Delete Columns
     */
    private void dropCol(Connection connection, String dbName, String tableName, List<String> cols) {
        cols.forEach(var -> {
            String dropColSql = SqlUtils.generateDropColSql(dbName, tableName, var);
            try {
                CommonDBUtils.executeSql(connection, dropColSql);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });
    }

    @Override
    public R<ImportFromDataSourceVo> getImportFromDataSource(String spaceId, String currentUserId, Long dataMappingId) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (dataMapping.getSourceType() != SourceType.DATASOURCE) {
            return R.failed(CommonUtils.messageInternational("data_mapping_not_from_datasource"));
        }
        JobInfo jobInfo = jobService.getJobByDataMappingId(dataMappingId);
        if (jobInfo == null) {
            return R.failed(CommonUtils.messageInternational("job_info_not_found"));
        }
        ImportFromDataSourceVo importFromDataSourceVo = jobInfo.getImportFromDataSourceVo();
        if (importFromDataSourceVo.getUpdateDate() == null) {
            importFromDataSourceVo.setUpdateDate(new Date().getTime());
        }
        return R.ok(importFromDataSourceVo);
    }

    @Override
    public R<Boolean> stopImportFromDataSource(String spaceId, String currentUserId, Long dataMappingId) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (dataMapping.getSourceType() != SourceType.DATASOURCE) {
            return R.failed(CommonUtils.messageInternational("data_mapping_not_from_datasource"));
        }
        JobInfo jobInfo = jobService.getJobByDataMappingId(dataMappingId);
        if (jobInfo == null) {
            return R.failed(CommonUtils.messageInternational("job_info_not_found"));
        }
        ReturnT<String> stop = jobService.stop(jobInfo.getId());
        if (stop.getCode() == 500) {
            return R.failed(stop.getMsg());
        }
        return R.ok(true);
    }

    @Override
    public R<Boolean> startImportFromDataSource(String spaceId, String currentUserId, Long dataMappingId) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (dataMapping.getSourceType() != SourceType.DATASOURCE) {
            return R.failed(CommonUtils.messageInternational("data_mapping_not_from_datasource"));
        }
        JobInfo jobInfo = jobService.getJobByDataMappingId(dataMappingId);
        if (jobInfo == null) {
            return R.failed(CommonUtils.messageInternational("job_info_not_found"));
        }
        ReturnT<String> start = jobService.start(jobInfo.getId());
        if (start.getCode() == 500) {
            return R.failed(start.getMsg());
        }
        return R.ok(true);
    }

    @Override
    public R<Boolean> triggerImportFromDataSource(String spaceId, String currentUserId, Long dataMappingId) {
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        if (dataMapping.getSourceType() != SourceType.DATASOURCE) {
            return R.failed(CommonUtils.messageInternational("data_mapping_not_from_datasource"));
        }
        // if (!dataMappingLockService.tryLockDataMapping(dataMappingId, false)) {
        // return R.failed(CommonUtils.messageInternational("data_lock"));
        // }
        try {
            JobInfo jobInfo = jobService.getJobByDataMappingId(dataMappingId);
            if (jobInfo == null) {
                return R.failed(CommonUtils.messageInternational("job_info_not_found"));
            }
            JobTriggerPoolHelper.trigger(jobInfo.getId(), TriggerTypeEnum.MANUAL, -1, null, "1");
            return R.ok(true);
        } finally {
            // dataMappingLockService.releaseLock(dataMappingId, false);
        }
    }

    @Override
    public R<Map<String, Object>> getStaInfo(String spaceId) {
        Space space = spaceRepository.findById(spaceId).get();
        QueryWrapper<DataMapping> dataMappingQueryWrapper = new QueryWrapper<>();
        dataMappingQueryWrapper.eq("space_id", spaceId);
        List<DataMapping> dataMappingList = dataMappingMapper.selectList(dataMappingQueryWrapper);
        Map<String, Object> ret = new HashMap<>();
        ret.put("tableCount", dataMappingList.size());
        ret.put("dataCount", getCount(space, dataMappingList));
        return R.ok(ret);
    }

    @Override
    public R<DataMappingMeta> getColumnMeta(String currentUserId, String spaceId, String dataMappingId) {
        Optional<Space> byId = spaceRepository.findById(spaceId);
        Optional<ConsumerDO> byId1 = userRepository.findById(currentUserId);
        DataMapping dataMapping = dataMappingMapper.getByPrimaryKeyAndSpaceId(dataMappingId, spaceId);
        if (dataMapping == null) {
            return R.failed(CommonUtils.messageInternational("structured_data_not_found"));
        }
        QueryWrapper<DataMappingMeta> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", currentUserId);
        queryWrapper.eq("dataMappingId", dataMappingId);
        DataMappingMeta dataMappingMeta = dataMappingMetaMapper.selectOne(queryWrapper);
        if (dataMapping == null) {
            dataMappingMeta = new DataMappingMeta();
            dataMappingMeta.setDataMappingId(dataMappingId);
            dataMappingMeta.setUserId(currentUserId);
            dataMappingMeta.setShowColumns(Arrays.asList("all"));
        }
        return R.ok(dataMappingMeta);
    }

    @Override
    public void setColumnMeta(String currentUserId, String spaceId, DataMappingMeta dataMappingMeta) {
        if (dataMappingMeta.getDataMappingId().isEmpty()) {
            return;
        }
        if (CollectionUtils.isEmpty(dataMappingMeta.getShowColumns())) {
            return;
        }
        dataMappingMeta.setUserId(currentUserId);
        dataMappingMetaMapper.insert(dataMappingMeta);
    }

    @Override
    public void updateColumnMeta(String currentUserId, String spaceId, DataMappingMeta dataMappingMeta) {
        if (dataMappingMeta.getDataMappingId().isEmpty()) {
            return;
        }
        if (CollectionUtils.isEmpty(dataMappingMeta.getShowColumns())) {
            return;
        }
        DataMappingMeta selectById = dataMappingMetaMapper.selectById(dataMappingMeta.getId());
        if (selectById == null) {
            return;
        }
        dataMappingMeta.setUserId(currentUserId);
        dataMappingMetaMapper.updateById(dataMappingMeta);
    }

    private Long getCount(Space space, List<DataMapping> dataMappingList) {
        Connection connection = null;
        Long dataCount = 0L;
        Set<String> names = dataMappingList.stream().map(var -> var.getName()).collect(Collectors.toSet());
        try {
            connection = new JdbcConnectionFactory().getConnection();
            String sql = String.format("select TABLE_ROWS as a, TABLE_NAME as b from information_schema.TABLES where TABLE_SCHEMA = '%s'", space.getDbName());
            ResultSet resultSet = CommonDBUtils.query(connection, sql, 1);
            while (resultSet.next()) {
                if (names.contains(resultSet.getString("b"))) {
                    dataCount += resultSet.getLong("a");
                }
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
        return dataCount;
    }

    /**
     * Reading Excel sheet
     */
    private R<List<String>> readExcel(String fileName) {
        OPCPackage pkg = null;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(fileName);
            pkg = OPCPackage.open(fileInputStream);
            XSSFReader r = new XSSFReader(pkg);
            Iterator sheets = r.getSheetsData();
            List<String> sheetNames = new ArrayList<>();
            if (sheets instanceof XSSFReader.SheetIterator) {
                XSSFReader.SheetIterator sheetiterator = (XSSFReader.SheetIterator) sheets;
                while (sheetiterator.hasNext()) {
                    InputStream dummy = null;
                    try {
                        dummy = sheetiterator.next();
                        String sheetName = sheetiterator.getSheetName();
                        sheetNames.add(sheetName);
                    } finally {
                        if (dummy != null) {
                            try {
                                dummy.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            return R.ok(sheetNames);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return R.failed("excel_parse_failed");
        } finally {
            if (pkg != null) {
                try {
                    pkg.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            IOUtils.closeQuietly(fileInputStream);
        }
    }

    /**
     * Add Action Log
     */
    private void addLog(String spaceId, String userId, String desc) {
        svnSpaceLogRepository.save(SpaceSvnLog.builder().spaceId(spaceId).version(SpaceSvnLog.ACTION_VALUE).description(desc).action(SpaceSvnLog.ACTION_TABLE).operatorId(userId).operator(new Operator(userRepository.findById(userId).get())).createTime(new Date()).build());
    }

    /**
     * Create Excel Import Task
     */
    private ImportExcelTask createImportTask(Long dataMappingId, String spaceId, File excelFile, String sheetName, SourceType sourceType) {
        ImportExcelTask importExcelTask = new ImportExcelTask();
        if (sourceType == SourceType.OFFLINE) {
            importExcelTask.setTaskDesc("从成员本地文件导入(" + excelFile.getName() + ",sheetName " + sheetName + ")");
        } else if (sourceType == SourceType.SPACE) {
            importExcelTask.setTaskDesc("从空间文件导入(" + excelFile.getName() + ",sheetName " + sheetName + ")");
        }
        importExcelTask.setDataMappingId(dataMappingId);
        importExcelTask.setStatus(1);
        importExcelTask.setFileLocation(excelFile.getAbsolutePath());
        importExcelTask.setSpaceId(spaceId);
        importExcelTaskMapper.insert(importExcelTask);
        return importExcelTask;
    }

    /**
     * insert db
     *
     * @param tableName
     * @param spaceId
     * @param excelFile
     * @param sheetName
     * @return
     */
    private ImportExcelTask insertDB(String tableName, String spaceId, String userId, File excelFile, String sheetName, SourceType sourceType) {
        DataMapping newDataMapping = new DataMapping();
        newDataMapping.setName(tableName);
        newDataMapping.setIsPublic(1);
        newDataMapping.setStatus(1);
        newDataMapping.setType(DataMappingType.FILE);
        newDataMapping.setSpaceId(spaceId);
        newDataMapping.setUpdateBy(userId);
        newDataMapping.setSourceType(sourceType);
        ImportExcelTask importExcelTask = new ImportExcelTask();
        if (sourceType == SourceType.OFFLINE) {
            newDataMapping.setSource("从成员本地文件导入(" + excelFile.getName() + ",sheetName " + sheetName + ")");
            importExcelTask.setTaskDesc("从成员本地文件导入(" + excelFile.getName() + ",sheetName " + sheetName + ")");
        } else if (sourceType == SourceType.SPACE) {
            newDataMapping.setSource("从空间文件导入(" + excelFile.getName() + ",sheetName " + sheetName + ")");
            importExcelTask.setTaskDesc("从空间文件导入(" + excelFile.getName() + ",sheetName " + sheetName + ")");
        }
        dataMappingMapper.insert(newDataMapping);
        importExcelTask.setDataMappingId(newDataMapping.getId());
        importExcelTask.setStatus(1);
        importExcelTask.setFileLocation(excelFile.getAbsolutePath());
        importExcelTask.setSpaceId(spaceId);
        importExcelTaskMapper.insert(importExcelTask);
        return importExcelTask;
    }

    /**
     * drop table no exception
     *
     * @param dbName
     * @param tableName
     */
    private void deleteTable(String dbName, String tableName) {
        String createTableSqlWithDefaultId = SqlUtils.generateDropTableSql(dbName, tableName);
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(dbName).getConnection();
            CommonDBUtils.executeSql(connection, createTableSqlWithDefaultId);
        } catch (SQLException e) {
            log.error(CommonUtils.messageInternational("create_table_failed"));
            log.error(e.getMessage(), e);
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
    }

    /**
     * setResultToResponse
     *
     * @param response
     * @param result
     * @throws IOException
     */
    private void setResponse(HttpServletResponse response, Map<String, Object> result) throws IOException {
        // Reset response
        if (!response.isCommitted()) {
            response.reset();
        }
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        // GetOutputStream() cannot be used again with getWriter() before it
        response.getOutputStream().write(JSON.toJSONString(result).getBytes(StandardCharsets.UTF_8));
        // response.getWriter().println();
    }
}
