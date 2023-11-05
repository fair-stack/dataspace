package cn.cnic.dataspace.api.service.file;

import cn.cnic.dataspace.api.asynchronous.ZipAsync;
import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.elfinder.core.Target;
import cn.cnic.dataspace.api.elfinder.core.Volume;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorageService;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.filehandle.Control;
import cn.cnic.dataspace.api.filehandle.ControlImpl;
import cn.cnic.dataspace.api.model.space.SpaceSimple;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.model.file.*;
import cn.cnic.dataspace.api.util.*;
import cn.hutool.extra.cglib.CglibUtil;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;
import static cn.cnic.dataspace.api.util.CommonUtils.messageInternational;

@Slf4j
@Service
@EnableAsync
public class FileHandService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private SpaceUrl spaceUrl;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private ElfinderStorageService elfinderStorageService;

    @Autowired
    private SpaceControlConfig spaceControlConfig;

    @Autowired
    private ZipAsync zipAsync;

    @Lazy
    @Autowired
    private CacheLoading cacheLoading;

    // File control layer
    private final Control control = new ControlImpl();

    private final Cache<String, String> tokenCache = CaffeineUtil.getTokenCache();

    // Member permission verification
    private ResponseResult<Object> spaceAuth(String spaceId, Token token) {
        // Verify permissions
        try {
            spaceControlConfig.spatialVerification(spaceId, token.getEmailAccounts(), Constants.SpaceRole.LEVEL_OTHER);
        } catch (CommonException e) {
            return ResultUtil.error(e.getCode(), e.getMsg());
        }
        return ResultUtil.success();
    }

    // File upload verification
    public ResponseResult<Object> uploadCheck(String token, FileCheck fileCheck) {
        if (null == fileCheck || fileCheck == null) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        List<String> validation = CommonUtils.validation(fileCheck);
        if (!validation.isEmpty()) {
            return ResultUtil.error(validation.toString());
        }
        Map<String, Object> resultMap = new HashMap<>(2);
        Token usersToken = jwtTokenUtils.getToken(token);
        String name = fileCheck.getName();
        String spaceId = fileCheck.getSpaceId();
        boolean last = true;
        if (CommonUtils.isFtpChar(name)) {
            resultMap.put("code", 1);
            resultMap.put("message", messageInternational("COMMAND_FILE_CHAR") + CommonUtils.takeOutChar(name));
            last = false;
        }
        if (last) {
            // Verify permissions
            ResponseResult<Object> objectResponseResult = spaceAuth(spaceId, usersToken);
            if (objectResponseResult.getCode() != 0) {
                resultMap.put("code", 2);
                resultMap.put("message", objectResponseResult.getMessage());
                last = false;
            }
        }
        // Verify if the file exists
        if (last) {
            String realPath = getRealPath(fileCheck.getHash(), spaceId);
            if (new File(realPath, name).exists()) {
                resultMap.put("code", 3);
                resultMap.put("message", messageInternational("COMMAND_TARGET_FILE"));
                last = false;
            }
        }
        // Compare file size
        if (last) {
            String total = fileCheck.getTotal();
            // Long aLong = Long.valueOf(total);
            // Verify space capacity
            if (SpaceSizeControl.validation(spaceId)) {
                resultMap.put("code", 5);
                resultMap.put("message", messageInternational("FILE_SIZE_FULL"));
                last = false;
            }
            // if(aLong <= 0){
            // resultMap.put("code", 4);
            // resultMap.put("message", messageInternational("FILE_SIZE_ERROR"));
            // last = false;
            // }else {
            // 
            // //                else if(SpaceSizeControl.validation(spaceId,aLong)) {
            // //                    resultMap.put("code", 6);
            // //                    resultMap.put("message", messageInternational("FILE_SIZE_FULL_ONE"));
            // //                    last = false;
            // //                }
            // }
        }
        // Verify duplicate uploads
        // if(last) {
        // String code = usersToken.getUserId() + "_" + spaceId + "_" + fileCheck.getHash() + "_" + fileCheck.getName();
        // log.info("code : {} "+code);
        // String file = fileUploadCheck.getIfPresent(code);
        // if (null != file) {
        // Log. info ("Enter duplicate upload verification");
        // Long data = fileUploadSchedule.getIfPresent(code);
        // Log. info ("Get the first progress {}"+data);
        // try {
        // Thread.sleep(500);
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
        // Long dataTo = fileUploadSchedule.getIfPresent(code);
        // Log. info ("Get second progress {}"+data);
        // if (null != dataTo) {
        // Log. info ("Enter progress verification");
        // if (null == data || data != dataTo) {
        // resultMap.put("code", 3);
        // resultMap.put("message", messageInternational("COMMAND_TARGET_FILE"));
        // last = false;
        // }
        // }
        // } else {
        // fileUploadCheck.put(code, "true");
        // }
        // }
        if (last) {
            resultMap.put("code", 0);
            resultMap.put("message", "success");
        }
        return ResultUtil.success(resultMap);
    }

    /**
     * File sharding upload
     */
    public ResponseResult<Object> fileUpload(String token, FileRequest fileRequest, MultipartFile file) throws NoSuchAlgorithmException {
        if (null == fileRequest || file == null) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        List<String> validation = CommonUtils.validation(fileRequest);
        if (!validation.isEmpty()) {
            return ResultUtil.error(validation.toString());
        }
        Token usersToken = jwtTokenUtils.getToken(token);
        String spaceId = fileRequest.getSpaceId();
        control.validateFileMakePermissions(usersToken.getEmailAccounts(), spaceId);
        // File path releaseStored
        String releaseStored = spaceUrl.getReleaseStored();
        String fileMd5 = fileRequest.getFileMd5();
        String chunkIndex = fileRequest.getChunkIndex();
        String name = fileRequest.getName();
        // Verify if the shard has been uploaded
        if (isUpload(fileMd5, Integer.valueOf(chunkIndex), spaceId, usersToken.getUserId(), fileRequest.getHash(), name)) {
            return ResultUtil.success();
        }
        String userId = usersToken.getUserId();
        String filePath = releaseStored + "/" + Constants.Release.FILE_SHARD + "/" + CommonUtils.getDateString(new Date()) + "/" + userId + "/" + spaceId + "/" + MD5.getMD5(fileRequest.getHash()) + "/" + fileMd5 + "/";
        FileUtils.createFolder(filePath);
        File localFile = new File(filePath, name + "-" + chunkIndex);
        boolean result;
        try {
            result = fileUpload(file.getInputStream(), localFile);
        } catch (IOException ioException) {
            ioException.printStackTrace();
            localFile.delete();
            return ResultUtil.error("文件分片上传失败! " + name);
        }
        if (!result) {
            localFile.delete();
            return ResultUtil.error("文件分片上传失败! " + name);
        }
        // Information storage
        UploadFile uploadFile = new UploadFile();
        uploadFile.setFileName(name);
        uploadFile.setFilePath(localFile.getPath());
        uploadFile.setTargetPath(fileRequest.getHash());
        uploadFile.setFileMd5(fileMd5);
        uploadFile.setFileSize(file.getSize());
        uploadFile.setFileStatus(result ? 1 : 0);
        uploadFile.setFileTotal(Long.valueOf(fileRequest.getTotal()));
        uploadFile.setCheckIndex(Integer.valueOf(chunkIndex));
        uploadFile.setCheckCount(Integer.valueOf(fileRequest.getChunkCount()));
        uploadFile.setCreateTime(new Date());
        uploadFile.setSpaceId(fileRequest.getSpaceId());
        uploadFile.setUserId(usersToken.getUserId());
        mongoTemplate.insert(uploadFile);
        return ResultUtil.success();
    }

    private boolean isUpload(String fileMd5, Integer chunkIndex, String spaceId, String userId, String hash, String fileName) {
        Criteria criteria = Criteria.where("fileMd5").is(fileMd5).and("spaceId").is(spaceId).and("userId").is(userId).and("targetPath").is(hash).and("checkIndex").is(chunkIndex).and("fileName").is(fileName);
        Query query = new Query().addCriteria(criteria);
        UploadFile uploadFiles = mongoTemplate.findOne(query, UploadFile.class);
        if (null != uploadFiles) {
            if (uploadFiles.getFileStatus() == 1) {
                return true;
            } else {
                mongoTemplate.remove(uploadFiles);
                // Delete files
                String filePath = uploadFiles.getFilePath();
                File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        return false;
    }

    private boolean fileUpload(InputStream inputStream, File localFile) {
        boolean result = true;
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(localFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        } catch (SocketException ex) {
            log.info("文件(" + localFile.getName() + ")上传失败：{} " + ex.getMessage());
            result = false;
        } catch (IOException ex) {
            log.info("文件(" + localFile.getName() + ")上传失败：{} " + ex.getMessage());
            result = false;
        } finally {
            try {
                if (null != inputStream) {
                    inputStream.close();
                }
                if (null != out) {
                    out.close();
                }
            } catch (IOException e) {
            }
        }
        return result;
    }

    public ResponseResult<Object> uploadMerge(String token, String md5, String spaceId, String hash) {
        if (StringUtils.isEmpty(md5)) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        Token usersToken = jwtTokenUtils.getToken(token);
        // Verify permissions
        ResponseResult<Object> objectResponseResult = spaceAuth(spaceId, usersToken);
        if (objectResponseResult.getCode() != 0) {
            return objectResponseResult;
        }
        Criteria criteria = Criteria.where("fileMd5").is(md5).and("spaceId").is(spaceId).and("userId").is(usersToken.getUserId()).and("targetPath").is(hash);
        Query query = new Query().addCriteria(criteria);
        query.with(Sort.by(Sort.Direction.ASC, "checkIndex"));
        List<UploadFile> uploadFiles = mongoTemplate.find(query, UploadFile.class);
        if (!uploadFiles.isEmpty()) {
            UploadFile uploadFile = uploadFiles.get(0);
            int checkCount = uploadFile.getCheckCount();
            if (checkCount != uploadFiles.size()) {
                return ResultUtil.errorInternational("FILE_INDEX_ERROR");
            }
            String realPath = getRealPath(hash, spaceId);
            if (StringUtils.isEmpty(realPath)) {
                return ResultUtil.errorInternational("");
            }
            // merge
            control.uploadMerge(uploadFiles, realPath);
            mongoTemplate.remove(query, UploadFile.class);
        }
        return ResultUtil.success();
    }

    private String getRealPath(String hash, String spaceId) {
        ElfinderStorage elfinderStorage = null;
        try {
            elfinderStorage = elfinderStorageService.getElfinderStorageSimple(spaceId);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return elfinderStorage.fromHash(hash).toString();
    }

    public ResponseResult<Object> uploadVer(String token, String fileMd5, String spaceId, String hash) {
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        // Verify if the shard has been uploaded
        Criteria criteria = Criteria.where("fileMd5").is(fileMd5).and("spaceId").is(spaceId).and("userId").is(userIdFromToken).and("targetPath").is(hash);
        Query query = new Query().addCriteria(criteria);
        List<UploadFile> uploadFiles = mongoTemplate.find(query, UploadFile.class);
        List<Integer> result = new ArrayList<>(uploadFiles.size());
        for (UploadFile uploadFile : uploadFiles) {
            result.add(uploadFile.getCheckIndex());
        }
        return ResultUtil.success(result);
    }

    /**
     * Compressing shp files
     */
    public void shpFileDown(String token, String hash, String name, String spaceId, HttpServletRequest request, HttpServletResponse response) throws UnsupportedEncodingException {
        // Permission parameter validation
        Map<String, Object> paramMap = new HashMap<>(3);
        paramMap.put("hash", hash);
        paramMap.put("fileName", name);
        paramMap.put("spaceId", spaceId);
        List<String> strings = CommonUtils.validationMap(paramMap);
        if (!strings.isEmpty()) {
            throw new CommonException(strings.toString());
        }
        Token usersToken = jwtTokenUtils.getToken(token);
        // Verify permissions
        ResponseResult<Object> objectResponseResult = spaceAuth(spaceId, usersToken);
        if (objectResponseResult.getCode() != 0) {
            throw new CommonException(objectResponseResult.getCode(), objectResponseResult.getMessage().toString());
        }
        String realPath = getRealPath(hash, spaceId);
        if (null == realPath) {
            throw new CommonException("输入的文件hash无效");
        }
        File file = new File(realPath);
        List<String> filePaths = new ArrayList<>();
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File shpFile : files) {
                    if (shpFile.getName().contains(name)) {
                        log.info("{}  shp 文件路径：" + shpFile.getPath());
                        filePaths.add(shpFile.getPath());
                    }
                }
            }
        }
        if (filePaths.isEmpty()) {
            throw new CommonException("未找到相对应的文件");
        }
        // Browser handling garbled code issues
        String userAgent = request.getHeader("User-Agent");
        // Filename. getBytes ("UTF-8") handling garbled code in Safari
        String fileName = name + ".zip";
        byte[] bytes = userAgent.contains("MSIE") ? fileName.getBytes() : fileName.getBytes("UTF-8");
        // Most browsers support ISO encoding
        fileName = new String(bytes, "ISO-8859-1");
        // Dealing with Space Truncation in Firefox with Double Quotations Outside File Names
        response.setHeader("Content-disposition", String.format("attachment; filename=\"%s\"", fileName));
        response.setContentType("application/x-msdownload");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        FileUtils.doZIP(filePaths, response);
        return;
    }

    /**
     * Shp file download
     */
    public void shpDown(String token, String hash, String spaceId, HttpServletResponse response) {
        Token usersToken = jwtTokenUtils.getToken(token);
        // Verify permissions
        ResponseResult<Object> objectResponseResult = spaceAuth(spaceId, usersToken);
        if (objectResponseResult.getCode() != 0) {
            throw new CommonException(objectResponseResult.getCode(), objectResponseResult.getMessage().toString());
        }
        boolean result = entranceDown(hash, spaceId, "", response);
        if (!result) {
            throw new CommonException("文件获取失败!");
        }
        return;
    }

    /**
     * Create a folder
     */
    public ResponseResult<Object> createFolder(String token, String spaceId, String hash, String path) throws IOException {
        // Parameters - Permission Verification
        Map<String, Object> paramMap = new HashMap<>(3);
        paramMap.put("hash", hash);
        paramMap.put("spaceId", spaceId);
        paramMap.put("path", path);
        List<String> strings = CommonUtils.validationMap(paramMap);
        if (!strings.isEmpty()) {
            return ResultUtil.error(strings.toString());
        }
        Token usersToken = jwtTokenUtils.getToken(token);
        // Verify permissions
        ResponseResult<Object> objectResponseResult = spaceAuth(spaceId, usersToken);
        if (objectResponseResult.getCode() != 0) {
            return objectResponseResult;
        }
        control.validateFileMakePermissions(usersToken.getEmailAccounts(), spaceId);
        String[] split = path.split("/");
        for (String s : split) {
            if (CommonUtils.isFtpChar(s)) {
                return ResultUtil.error(messageInternational("COMMAND_FOLDER_CHAR") + CommonUtils.takeOutChar(s));
            }
        }
        ElfinderStorage elfinderStorage = null;
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            elfinderStorage = elfinderStorageService.getElfinderStorage(request, spaceId);
        } catch (Exception e) {
            e.printStackTrace();
            return ResultUtil.errorInternational("GENERAL_FILEPATH_ERROR");
        }
        String realPath = elfinderStorage.fromHash(hash).toString();
        Volume volume = elfinderStorage.getVolumes().get(0);
        String[] dir = path.split(CommonUtils.FILE_SPLIT);
        String rootPath = "";
        synchronized (this) {
            for (String s : dir) {
                if (s.equals("")) {
                    continue;
                }
                rootPath = rootPath + "/" + s;
                File file = new File(realPath + rootPath);
                if (!file.exists()) {
                    control.createFolder(file.toPath(), true, true, SpaceSvnLog.ELFINDER);
                }
            }
        }
        Target target = volume.fromPath(realPath + path);
        String pathHash = "";
        try {
            pathHash = elfinderStorage.getHash(target);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return ResultUtil.success(pathHash);
    }

    /**
     * Space file download
     */
    public void download(String hash, String spaceId, HttpServletRequest request, HttpServletResponse response) {
        String token = CommonUtils.getUser(request, Constants.TOKEN);
        if (null == token) {
            throw new CommonException(401, "请登录");
        }
        if (!jwtTokenUtils.validateToken(token)) {
            Cookie cookie = CommonUtils.getCookie(Constants.TOKEN, "", 0, 1);
            Cookie cookie2 = CommonUtils.getCookie(Constants.WAY, "", 0, 1);
            response.addCookie(cookie);
            response.addCookie(cookie2);
            throw new CommonException(401, "请登录");
        } else {
            String ifPresent = tokenCache.getIfPresent(token);
            if (ifPresent == null) {
                throw new CommonException(401, "请登录");
            }
        }
        Token usersToken = jwtTokenUtils.getToken(token);
        // Verify permissions
        ResponseResult<Object> objectResponseResult = spaceAuth(spaceId, usersToken);
        if (objectResponseResult.getCode() != 0) {
            throw new CommonException(objectResponseResult.getCode(), objectResponseResult.getMessage().toString());
        }
        String realPath = getRealPath(hash, spaceId);
        if (null == realPath) {
            throw new CommonException("输入的文件hash无效");
        }
        control.validateFileOtherPermissions(usersToken.getEmailAccounts(), spaceId, realPath, "down");
        boolean result = entranceDown(hash, spaceId, "down", response);
        if (!result) {
            throw new CommonException("文件获取下载失败!");
        }
        return;
    }

    /**
     * Unified download file entry
     */
    public boolean entranceDown(String hash, String spaceId, String type, HttpServletResponse response) {
        Map<String, Object> paramMap = new HashMap<>(3);
        paramMap.put("hash", hash);
        paramMap.put("spaceId", spaceId);
        List<String> strings = CommonUtils.validationMap(paramMap);
        if (!strings.isEmpty()) {
            throw new CommonException(strings.toString());
        }
        String realPath = getRealPath(hash, spaceId);
        if (null == realPath) {
            throw new CommonException("输入的文件hash无效");
        }
        File file = new File(realPath);
        if (!file.exists()) {
            throw new CommonException("文件名或文件路径已被修改！");
        }
        boolean result = false;
        if (file.isDirectory()) {
            result = FileUtils.downloadFolder(realPath, response);
        } else {
            result = FileUtils.downloadFile(realPath, response);
        }
        if (result && type.equals("down")) {
            spaceControlConfig.spaceStat(spaceId, "download", 1l);
        }
        if (result && type.equals("down")) {
            long fileSize = control.getFileSize(realPath, spaceId);
            spaceControlConfig.spaceStat(spaceId, "downSize", fileSize);
            spaceControlConfig.dataOut("web", fileSize, spaceId);
        }
        return result;
    }

    /**
     * File compression
     */
    public ResponseResult<Object> zipReduce(String token, String hash, String spaceId, HttpServletResponse response) {
        Map<String, Object> paramMap = new HashMap<>(2);
        paramMap.put("hash", hash);
        paramMap.put("spaceId", spaceId);
        List<String> strings = CommonUtils.validationMap(paramMap);
        if (!strings.isEmpty()) {
            return ResultUtil.error(strings.toString());
        }
        Token usersToken = jwtTokenUtils.getToken(token);
        // Verify permissions
        ResponseResult<Object> objectResponseResult = spaceAuth(spaceId, usersToken);
        if (objectResponseResult.getCode() != 0) {
            return ResultUtil.error(objectResponseResult.getCode(), objectResponseResult.getMessage().toString());
        }
        control.validateFileMakePermissions(usersToken.getEmailAccounts(), spaceId);
        String realPath = getRealPath(hash, spaceId);
        if (null == realPath) {
            return ResultUtil.errorInternational("FILE_HASH");
        }
        File file = new File(realPath);
        if (!file.exists()) {
            return ResultUtil.errorInternational("FILE_PATH");
        }
        if (!file.isDirectory()) {
            return ResultUtil.errorInternational("FILE_ZIP_ERROR_ONE");
        }
        String targetPath = realPath.substring(0, realPath.lastIndexOf("/"));
        if (new File(targetPath, file.getName() + ".zip").exists()) {
            return ResultUtil.error(messageInternational("FILE_ZIP_CON_SOU") + file.getName() + ".zip" + messageInternational("FILE_ZIP_CON_TAR"));
        }
        // Verify space capacity
        if (SpaceSizeControl.validation(spaceId)) {
            return ResultUtil.errorInternational("FILE_SIZE_FULL");
        } else {
            long invoke = control.getFileSize(realPath, spaceId);
            // Long invoke = FileSizeComputer.FORK_JOIN_POOL.invoke(new FileSizeComputer(file));
            if (SpaceSizeControl.validation(spaceId, invoke)) {
                return ResultUtil.errorInternational("FILE_SIZE_FULL_ONE");
            }
        }
        // Asynchronous compression
        String uuid = CommonUtils.generateUUID();
        String tempPath = spaceUrl.getReleaseStored() + "/" + Constants.Release.FILE_ZIP + "/" + usersToken.getUserId() + "/" + uuid + "ZIP";
        zipAsync.compress(realPath, tempPath, targetPath, spaceId, usersToken, spaceControlConfig);
        return ResultUtil.success();
    }

    public ResponseResult<Object> zipUnpack(String token, String hash, String spaceId, HttpServletResponse response) {
        Map<String, Object> paramMap = new HashMap<>(2);
        paramMap.put("hash", hash);
        paramMap.put("spaceId", spaceId);
        List<String> strings = CommonUtils.validationMap(paramMap);
        if (!strings.isEmpty()) {
            return ResultUtil.error(strings.toString());
        }
        Token usersToken = jwtTokenUtils.getToken(token);
        // Verify permissions
        ResponseResult<Object> objectResponseResult = spaceAuth(spaceId, usersToken);
        if (objectResponseResult.getCode() != 0) {
            return ResultUtil.error(objectResponseResult.getCode(), objectResponseResult.getMessage().toString());
        }
        control.validateFileMakePermissions(usersToken.getEmailAccounts(), spaceId);
        String realPath = getRealPath(hash, spaceId);
        if (null == realPath) {
            return ResultUtil.errorInternational("FILE_HASH");
        }
        File file = new File(realPath);
        if (!file.exists()) {
            return ResultUtil.errorInternational("FILE_PATH");
        }
        String name = file.getName();
        if (!name.contains(".zip")) {
            return ResultUtil.errorInternational("FILE_ZIP_ERROR_TWO");
        }
        // Verify space capacity
        if (SpaceSizeControl.validation(spaceId)) {
            return ResultUtil.errorInternational("FILE_SIZE_FULL");
        } else {
            if (SpaceSizeControl.validation(spaceId, file.length())) {
                return ResultUtil.errorInternational("FILE_SIZE_FULL_ONE");
            }
        }
        String target = realPath.substring(0, realPath.lastIndexOf("/"));
        String targetName = name.substring(0, name.lastIndexOf("."));
        File targetFile = new File(target, targetName);
        if (targetFile.exists()) {
            return ResultUtil.error(messageInternational("FILE_ZIP_EXIT") + targetName + messageInternational("FILE_ZIP_CON_TAR"));
        }
        zipAsync.decompression(realPath, target + "/" + targetName, spaceId, usersToken, spaceControlConfig);
        return ResultUtil.success();
    }

    /**
     * File Query
     */
    public ResponseResult<Object> cmd(String token, Integer page, Integer size, String direction, String sort, String target, String spaceId) {
        if (StringUtils.isEmpty(spaceId)) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        if (!token.equals("ds.publicFile")) {
            Token usersToken = jwtTokenUtils.getToken(token);
            // Verify permissions
            ResponseResult<Object> objectResponseResult = spaceAuth(spaceId, usersToken);
            if (objectResponseResult.getCode() != 0) {
                return ResultUtil.error(objectResponseResult.getCode(), objectResponseResult.getMessage().toString());
            }
        }
        SpaceSimple spaceSimple = cacheLoading.getSpaceSimple(spaceId);
        if (null == spaceSimple) {
            return ResultUtil.errorInternational("RELEASE_SPACE_DELETE");
        }
        String spaceName = spaceSimple.getSpaceName();
        String path = pathToPes(spaceSimple.getFilePath().replaceAll("//", "/"));
        FileResult fileResult = new FileResult();
        Criteria criteria = new Criteria();
        if (StringUtils.isEmpty(target) || StringUtils.isEmpty(target.trim()) || target.equals("A_")) {
            criteria.and("fId").is("0");
            fileResult.setHash("A_");
            fileResult.setDirs(1);
            fileResult.setMime("directory");
            fileResult.setName(spaceName);
        } else {
            FileEntity fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("hash").is(target)), FileEntity.class, spaceId);
            if (fileMapping == null) {
                return ResultUtil.errorInternational("SPACE_FILE_DATA_ADD");
            }
            criteria.and("fId").is(fileMapping.getId());
            CglibUtil.copy(fileMapping, fileResult);
            fileResult.setPhash(fileMapping.getFHash());
            fileResult.setDirs(fileMapping.getType());
            fileResult.setMime((fileMapping.getType() == 1 ? "directory" : "file"));
        }
        List<FileResult> fileResultList = new ArrayList<>();
        fileResultList.add(fileResult);
        boolean fIsnull = true;
        boolean fNotFolder = false;
        long count = mongoTemplate.count(new Query().addCriteria(criteria), spaceId);
        if (count > 0) {
            fIsnull = false;
            Query query = new Query().addCriteria(criteria);
            query.with(PageRequest.of(page - 1, size));
            Sort.Direction dir = Sort.Direction.ASC;
            if (direction.equals("desc")) {
                dir = Sort.Direction.DESC;
            }
            query.with(Sort.by(dir, "type", sort));
            List<FileEntity> fileMappingList = mongoTemplate.find(query, FileEntity.class, spaceId);
            for (FileEntity fileMapping : fileMappingList) {
                FileResult res = new FileResult();
                CglibUtil.copy(fileMapping, res);
                res.setPhash(fileMapping.getFHash());
                res.setDirs(fileMapping.getType());
                res.setMime((fileMapping.getType() == 1 ? "directory" : "file"));
                res.setPath((fileMapping.getPath().replaceAll(path, spaceName)));
                if (fileMapping.getType() == 1) {
                    fNotFolder = true;
                    Query qu = new Query().addCriteria(Criteria.where("fId").is(fileMapping.getId()));
                    boolean isNull = !mongoTemplate.exists(qu, spaceId);
                    res.setIsNull(isNull);
                    if (!isNull) {
                        qu.addCriteria(Criteria.where("type").is(1));
                        boolean notFolder = mongoTemplate.exists(qu, spaceId);
                        res.setNotFolder(notFolder);
                    } else {
                        res.setNotFolder(false);
                    }
                }
                fileResultList.add(res);
            }
        }
        FileResult fileResult1 = fileResultList.get(0);
        fileResult1.setIsNull(fIsnull);
        fileResult1.setNotFolder(fNotFolder);
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("total", count);
        resultMap.put("files", fileResultList);
        return ResultUtil.success(resultMap);
    }

    /**
     * File Query
     */
    public ResponseResult<Object> cmdTo(String token, String target, String spaceId) {
        if (StringUtils.isEmpty(spaceId)) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        SpaceSimple spaceSimple = cacheLoading.getSpaceSimple(spaceId);
        if (null == spaceSimple) {
            return ResultUtil.errorInternational("RELEASE_SPACE_DELETE");
        }
        String spaceName = spaceSimple.getSpaceName();
        String path = pathToPes(spaceSimple.getFilePath().replaceAll("//", "/"));
        FileResult fileResult = new FileResult();
        Criteria criteria = new Criteria();
        if (StringUtils.isEmpty(target) || StringUtils.isEmpty(target.trim()) || target.equals("A_")) {
            criteria.and("fId").is("0");
            fileResult.setHash("A_");
            fileResult.setDirs(1);
            fileResult.setMime("directory");
            fileResult.setName(spaceName);
        } else {
            FileEntity fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("hash").is(target)), FileEntity.class, spaceId);
            if (fileMapping == null) {
                return ResultUtil.errorInternational("SPACE_FILE_DATA_ADD");
            }
            criteria.and("fId").is(fileMapping.getId());
            CglibUtil.copy(fileMapping, fileResult);
            fileResult.setPhash(fileMapping.getFHash());
            fileResult.setDirs(fileMapping.getType());
            fileResult.setMime((fileMapping.getType() == 1 ? "directory" : "file"));
        }
        List<FileResult> fileResultList = new ArrayList<>();
        fileResultList.add(fileResult);
        boolean fIsnull = true;
        boolean fNotFolder = false;
        long count = mongoTemplate.count(new Query().addCriteria(criteria), spaceId);
        if (count > 0) {
            fIsnull = false;
            Query query = new Query().addCriteria(criteria);
            query.with(Sort.by(Sort.Direction.DESC, "type"));
            List<FileEntity> fileMappingList = mongoTemplate.find(query, FileEntity.class, spaceId);
            for (FileEntity fileMapping : fileMappingList) {
                FileResult res = new FileResult();
                CglibUtil.copy(fileMapping, res);
                res.setPhash(fileMapping.getFHash());
                res.setDirs(fileMapping.getType());
                res.setMime((fileMapping.getType() == 1 ? "directory" : "file"));
                res.setPath((fileMapping.getPath().replaceAll(path, spaceName)));
                if (fileMapping.getType() == 1) {
                    fNotFolder = true;
                    Query qu = new Query().addCriteria(Criteria.where("fId").is(fileMapping.getId()));
                    boolean isNull = !mongoTemplate.exists(qu, spaceId);
                    res.setIsNull(isNull);
                    if (!isNull) {
                        qu.addCriteria(Criteria.where("type").is(1));
                        boolean notFolder = mongoTemplate.exists(qu, spaceId);
                        res.setNotFolder(notFolder);
                    } else {
                        res.setNotFolder(false);
                    }
                }
                fileResultList.add(res);
            }
        }
        FileResult fileResult1 = fileResultList.get(0);
        fileResult1.setIsNull(fIsnull);
        fileResult1.setNotFolder(fNotFolder);
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("total", count);
        resultMap.put("files", fileResultList);
        return ResultUtil.success(resultMap);
    }

    private String pathToPes(String str) {
        int i = str.lastIndexOf("/");
        if (i == str.length() - 1) {
            return pathToPes(str.substring(0, str.length() - 1));
        } else {
            return str;
        }
    }

    public ResponseResult<Object> folderSize(String token, String hash, String spaceId) {
        Token usersToken = jwtTokenUtils.getToken(token);
        // Verify permissions
        ResponseResult<Object> objectResponseResult = spaceAuth(spaceId, usersToken);
        if (objectResponseResult.getCode() != 0) {
            return objectResponseResult;
        }
        return ResultUtil.success(folderRecursion(hash, spaceId));
    }

    private long folderRecursion(String fHash, String spaceId) {
        long total = 0;
        Criteria criteria = Criteria.where("fHash").is(fHash);
        List<FileEntity> fileMappings = mongoTemplate.find(new Query().addCriteria(criteria), FileEntity.class, spaceId);
        if (fileMappings.isEmpty()) {
            return 0;
        }
        for (FileEntity fileMapping : fileMappings) {
            int type = fileMapping.getType();
            if (type == 1) {
                total += folderRecursion(fileMapping.getHash(), spaceId);
            } else {
                total += fileMapping.getSize();
            }
        }
        return total;
    }

    /**
     * Space file retrieval
     */
    public ResponseResult<Object> search(String token, String spaceId, String q, String target, Integer page, Integer size, String direction, String sort) {
        Token usersToken = jwtTokenUtils.getToken(token);
        // Verify permissions
        ResponseResult<Object> objectResponseResult = spaceAuth(spaceId, usersToken);
        if (objectResponseResult.getCode() != 0) {
            return objectResponseResult;
        }
        SpaceSimple spaceSimple = cacheLoading.getSpaceSimple(spaceId);
        if (null == spaceSimple) {
            return ResultUtil.errorInternational("RELEASE_SPACE_DELETE");
        }
        String spaceName = spaceSimple.getSpaceName();
        String path = pathToPes(spaceSimple.getFilePath().replaceAll("//", "/"));
        Pattern name = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(q.trim()) + ".*$", Pattern.CASE_INSENSITIVE);
        Criteria criteria1 = Criteria.where("name").regex(name);
        Criteria criteria2 = Criteria.where("data.value").regex(name);
        Criteria criteria = new Criteria().orOperator(criteria1, criteria2);
        String realPath = getRealPath(target, spaceId);
        Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(realPath) + ".*$", Pattern.CASE_INSENSITIVE);
        criteria.and("path").regex(pattern);
        Query query = new Query().addCriteria(criteria);
        long count = mongoTemplate.count(query, FileMapping.class, spaceId);
        List<FileResult> fileResultList = new ArrayList<>();
        if (count > 0) {
            query.with(PageRequest.of(page - 1, size));
            Sort.Direction dir = Sort.Direction.ASC;
            if (direction.equals("desc")) {
                dir = Sort.Direction.DESC;
            }
            query.with(Sort.by(dir, "type", sort));
            List<FileEntity> fileMappings = mongoTemplate.find(query, FileEntity.class, spaceId);
            for (FileEntity fileMapping : fileMappings) {
                FileResult res = new FileResult();
                CglibUtil.copy(fileMapping, res);
                res.setPhash(fileMapping.getFHash());
                res.setDirs(fileMapping.getType());
                res.setMime((fileMapping.getType() == 1 ? "directory" : "file"));
                res.setPath((fileMapping.getPath().replaceAll(path, spaceName)));
                fileResultList.add(res);
            }
        }
        Map<String, Object> resultMap = new HashMap<>(2);
        resultMap.put("total", count);
        resultMap.put("data", fileResultList);
        return ResultUtil.success(resultMap);
    }

    /**
     * Get the selected folder hierarchy directory
     */
    public ResponseResult<Object> hierarchy(String token, String spaceId, String source, String target) {
        Token usersToken = jwtTokenUtils.getToken(token);
        // Verify permissions
        ResponseResult<Object> objectResponseResult = spaceAuth(spaceId, usersToken);
        if (objectResponseResult.getCode() != 0) {
            return objectResponseResult;
        }
        SpaceSimple spaceSimple = cacheLoading.getSpaceSimple(spaceId);
        if (null == spaceSimple) {
            return ResultUtil.errorInternational("RELEASE_SPACE_DELETE");
        }
        String spaceName = spaceSimple.getSpaceName();
        FileMapping sourceFileMapping = null;
        if (source.equals("A_")) {
            sourceFileMapping = new FileMapping();
            sourceFileMapping.setId("A_");
            sourceFileMapping.setType(1);
            sourceFileMapping.setHash("A_");
            sourceFileMapping.setName(spaceName);
            sourceFileMapping.setPath(pathToPes(spaceSimple.getFilePath().replaceAll("//", "/")));
        } else {
            sourceFileMapping = control.getFileMappingAsHash(source, spaceId);
        }
        FileMapping targetFileMapping = control.getFileMappingAsHash(target, spaceId);
        // The level that needs to be pulled (remove the last layer)
        String sourcePath = sourceFileMapping.getPath();
        String targetPath = targetFileMapping.getPath();
        String res = targetPath.replaceAll(sourcePath, "");
        String[] split = res.split("/");
        List<String> pathList = new ArrayList<>(split.length);
        for (String s : split) {
            if (StringUtils.isEmpty(s)) {
                continue;
            }
            sourcePath = sourcePath + "/" + s;
            if (sourcePath.equals(targetPath)) {
                continue;
            }
            pathList.add(sourcePath);
        }
        List<FileMapping> fileMappingList = new ArrayList<>(pathList.size() + 2);
        for (String path : pathList) {
            FileMapping fileMapping = control.getFileMapping(path, spaceId);
            if (null != fileMapping) {
                fileMappingList.add(fileMapping);
            }
        }
        fileMappingList.add(targetFileMapping);
        List<FileEntity> fileEntityList = new ArrayList<>();
        for (FileMapping fileMapping : fileMappingList) {
            List<FileEntity> entityList = recursionFolder(fileMapping.getFId(), spaceId, fileMapping.getName());
            if (!entityList.isEmpty()) {
                fileEntityList.addAll(entityList);
            }
            entityList.clear();
        }
        fileMappingList.add(sourceFileMapping);
        // Modify Return Entity
        List<SearchEntity> searchEntityList = new ArrayList<>();
        for (FileMapping fileMapping : fileMappingList) {
            if (fileMapping.getId().equals(targetFileMapping.getId())) {
                continue;
            }
            SearchEntity searchEntity = new SearchEntity();
            CglibUtil.copy(fileMapping, searchEntity);
            searchEntity.setPhash(fileMapping.getFHash());
            searchEntity.setMime((fileMapping.getType() == 1 ? "directory" : "file"));
            searchEntity.setIsNull(false);
            searchEntity.setNotFolder(true);
            searchEntityList.add(searchEntity);
        }
        FileEntity fileEntity = new FileEntity();
        CglibUtil.copy(targetFileMapping, fileEntity);
        fileEntityList.add(fileEntity);
        Iterator<FileEntity> iterator = fileEntityList.iterator();
        while (iterator.hasNext()) {
            FileEntity fileMapping = iterator.next();
            SearchEntity searchEntity = new SearchEntity();
            CglibUtil.copy(fileMapping, searchEntity);
            searchEntity.setPhash(fileMapping.getFHash());
            searchEntity.setMime((fileMapping.getType() == 1 ? "directory" : "file"));
            if (fileMapping.getType() == 1) {
                setFolderState(fileMapping.getId(), spaceId, searchEntity);
            }
            searchEntityList.add(searchEntity);
        }
        fileEntityList.clear();
        return ResultUtil.success(searchEntityList);
    }

    private List<FileEntity> recursionFolder(String fId, String spaceId, String fileName) {
        Query query = new Query().addCriteria(Criteria.where("fId").is(fId).and("type").is(1).and("name").ne(fileName));
        query.with(PageRequest.of(0, 100));
        return mongoTemplate.find(query, FileEntity.class, spaceId);
    }

    private void setFolderState(String id, String spaceId, SearchEntity searchEntity) {
        Query qu = new Query().addCriteria(Criteria.where("fId").is(id));
        boolean isNull = !mongoTemplate.exists(qu, spaceId);
        searchEntity.setIsNull(isNull);
        if (!isNull) {
            qu.addCriteria(Criteria.where("type").is(1));
            boolean notFolder = mongoTemplate.exists(qu, spaceId);
            searchEntity.setNotFolder(notFolder);
        } else {
            searchEntity.setNotFolder(false);
        }
    }

    /**
     * Space file deletion
     */
    public ResponseResult<Object> delete(String token, FileDelete fileDelete) {
        if (null == fileDelete || StringUtils.isEmpty(fileDelete.getSpaceId()) || null == fileDelete.getHashList() || fileDelete.getHashList().isEmpty()) {
            return ResultUtil.errorInternational("GENERAL_PARAMETER_ERROR");
        }
        Token usersToken = jwtTokenUtils.getToken(token);
        // Verify permissions
        ResponseResult<Object> objectResponseResult = spaceAuth(fileDelete.getSpaceId(), usersToken);
        if (objectResponseResult.getCode() != 0) {
            return objectResponseResult;
        }
        List<String> hashList = fileDelete.getHashList();
        for (String hash : hashList) {
            String realPath = getRealPath(hash, fileDelete.getSpaceId());
            if (StringUtils.isNotEmpty(realPath)) {
                try {
                    control.delete(new File(realPath).toPath(), SpaceSvnLog.ELFINDER);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    return ResultUtil.errorInternational("SYSTEM_ERROR");
                }
            }
        }
        return ResultUtil.success();
    }
}
