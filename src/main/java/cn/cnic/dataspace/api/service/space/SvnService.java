package cn.cnic.dataspace.api.service.space;

import cn.cnic.dataspace.api.config.space.MsgUtil;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorageService;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.SpaceLock;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.space.child.Person;
import cn.cnic.dataspace.api.repository.SpaceRepository;
import cn.cnic.dataspace.api.repository.SvnSpaceLogRepository;
import cn.cnic.dataspace.api.repository.UserRepository;
import cn.cnic.dataspace.api.util.JwtTokenUtils;
import cn.cnic.dataspace.api.util.ResponseResult;
import cn.cnic.dataspace.api.util.ResultUtil;
import cn.cnic.dataspace.api.util.SvnUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import static cn.cnic.dataspace.api.model.space.SpaceSvnLog.ACTION_VERSION;
import static cn.cnic.dataspace.api.util.CommonUtils.*;

/**
 * SvnService
 *
 * @author wangCc
 * @date 2021-08-26 19:13
 */
@Slf4j
@Service
public class SvnService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SpaceRepository spaceRepository;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private SvnSpaceLogRepository svnSpaceLogRepository;

    @Autowired
    private ElfinderStorageService elfinderStorageService;

    @Autowired
    private SpaceService spaceService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MsgUtil msgUtil;

    @Autowired
    private MessageService messageService;

    /**
     * check space operation lock for svn
     */
    public boolean checkSpaceLock(String spaceId) {
        return mongoTemplate.find(new Query().addCriteria(Criteria.where("spaceId").is(spaceId)), SpaceLock.class).size() > 0;
    }

    /**
     * set space operation lock for svn
     */
    private void addSpaceLock(String email, String spaceId) {
        mongoTemplate.upsert(new Query().addCriteria(Criteria.where("spaceId").is(spaceId)), new Update().set("lock", 1).set("email", email), "spaceLock");
    }

    /**
     * release space lock for svn
     */
    private void releaseSpaceLock(String email, String spaceId) {
        mongoTemplate.findAndRemove(new Query().addCriteria(Criteria.where("spaceId").is(spaceId).and("email").is(email)), SpaceLock.class);
    }

    /**
     * add and push push directly
     */
    public ResponseResult<Object> addPush(String token, String spaceId, String description) {
        if (checkSpaceLock(spaceId)) {
            return ResultUtil.errorInternational("SVN_VERSION");
        }
        // prevent concurrency
        addSpaceLock(jwtTokenUtils.getEmail(token), spaceId);
        prePublish(token, spaceId);
        final Space space = spaceRepository.findById(spaceId).get();
        final String filePath = space.getFilePath();
        Long update;
        try {
            final List<Map<String, Object>> changeList = SvnUtil.changeList(filePath);
            if (changeList.size() > 0) {
                List<String> filePathList = new ArrayList<>();
                // get all of change file List
                for (Map<String, Object> stringObjectMap : changeList) {
                    String path = filePath + stringObjectMap.get("path").toString() + stringObjectMap.get("file").toString();
                    filePathList.add(path);
                }
                /*For (String filePathReal: filePathList){*/
            }
            update = push(token, spaceId, "update").getData();
        } finally {
            releaseSpaceLock(jwtTokenUtils.getEmail(token), spaceId);
        }
        // record log
        if (update > 0) {
            final String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
            svnSpaceLogRepository.save(SpaceSvnLog.builder().spaceSvnId(generateSnowflake()).spaceId(spaceId).description(description).version(update).createTime(new Date()).operatorId(userIdFromToken).operator(new Operator(userRepository.findById(userIdFromToken).get())).action(ACTION_VERSION).build());
        }
        // prevent concurrency
        releaseSpaceLock(jwtTokenUtils.getEmail(token), spaceId);
        msgUtil.sendMsg(jwtTokenUtils.getEmail(token), messageInternational("SVN_CREATE_SVN") + (update == -1 ? messageInternational("SVN_UNSUCCESSFULLY") : messageInternational("SVN_SUCCESSFULLY")));
        return ResultUtil.success(update);
    }

    /**
     * svn repository push for data space
     */
    public ResponseResult<Long> push(String token, String spaceId, String description) {
        if (spaceService.authenticCheck(token, spaceId)) {
            return ResultUtil.errorInternational("PERMISSION_DENIED");
        }
        Long version = -1L;
        try {
            version = SvnUtil.svnPush(spaceRepository.findById(spaceId).get().getFilePath(), description);
            Map<String, Object> map = new HashMap<>(16);
            String content = messageInternational("SVN_VERSION_CREATE") + version;
            map.put("title", messageInternational("SVN_GENERATE_VERSION"));
            map.put("content", content);
            msgUtil.sendMsg(jwtTokenUtils.getEmail(token), msgUtil.mapToString(map));
            messageService.sendToApplicant(messageInternational("SVN_COMPLETED"), content, new Person(userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get()), 1);
        } finally {
            releaseSpaceLock(jwtTokenUtils.getEmail(token), spaceId);
        }
        return ResultUtil.success(version);
    }

    /**
     * files undo add with revert
     */
    public ResponseResult<Object> undoAdd(String token, String spaceId, String filePath) {
        if (spaceService.authenticCheck(token, spaceId)) {
            return ResultUtil.errorInternational("PERMISSION_DENIED");
        }
        SvnUtil.undoAdd(filePath);
        return ResultUtil.success();
    }

    /**
     * space file version control
     */
    public ResponseResult<Object> rollback(String token, String spaceId, String version) {
        if (spaceService.authenticCheck(token, spaceId)) {
            return ResultUtil.errorInternational("PERMISSION_DENIED");
        }
        String spacePath = spaceRepository.findById(spaceId).get().getFilePath();
        // SvnUtil.unVersionedFiles(spacePath);
        List<Map<String, Object>> changeList = SvnUtil.changeList(spacePath);
        List<String> deleteFileList = new ArrayList<>();
        if (changeList.size() > 0) {
            List<Map<String, Object>> objects = changeList.stream().filter(o -> StringUtils.equals(o.get("type").toString(), "file")).collect(Collectors.toList());
            String[] filesList = new String[objects.size()];
            for (int i = 0; i < objects.size(); i++) {
                Map<String, Object> stringObjectMap = objects.get(i);
                filesList[i] = spacePath + stringObjectMap.get("path") + stringObjectMap.get("file");
                if (StringUtils.equals("added", stringObjectMap.get("method").toString())) {
                    deleteFileList.add(filesList[i]);
                }
            }
            // revert files
            SvnUtil.revert(filesList);
        }
        // space version rollback
        SvnUtil.rollback(spacePath, version);
        if (deleteFileList.size() > 0) {
            for (String filePath : deleteFileList) {
                File file = new File(filePath);
                if (file.exists()) {
                    boolean delete = file.delete();
                    log.info("SvnService.rollback delete " + delete + " " + file.getAbsolutePath());
                }
            }
        }
        String userIdFromToken = jwtTokenUtils.getUserIdFromToken(token);
        svnSpaceLogRepository.save(SpaceSvnLog.builder().spaceSvnId(generateSnowflake()).spaceId(spaceId).version(-2).operatorId(userIdFromToken).operator(new Operator(userRepository.findById(userIdFromToken).get())).createTime(new Date()).description(messageInternational("SVN_VERSION_ROLLBACK") + " " + version).action(ACTION_VERSION).build());
        Map<String, Object> map = new HashMap<>();
        String content = messageInternational("SVN_ROLLBACK") + version;
        map.put("title", messageInternational("SVN_GENERATE_VERSION"));
        map.put("content", content);
        msgUtil.sendMsg(jwtTokenUtils.getEmail(token), msgUtil.mapToString(map));
        messageService.sendToApplicant(messageInternational("SVN_RECOVERY"), content, new Person(userRepository.findById(jwtTokenUtils.getUserIdFromToken(token)).get()), 1);
        return ResultUtil.success(messageInternational("SVN_RECOVERY") + " " + messageInternational("SVN_SUCCESSFULLY"));
    }

    /**
     * files revert
     */
    public ResponseResult<Object> revert(String token, String spaceId, String... filesPath) {
        if (spaceService.authenticCheck(token, spaceId)) {
            return ResultUtil.errorInternational("PERMISSION_DENIED");
        }
        SvnUtil.revert(filesPath);
        return ResultUtil.successInternational("SVN_ROLLBACK_SUCCESSFULLY");
    }

    /**
     * version difference with concurrent working version
     */
    public ResponseResult<Object> differ(HttpServletRequest request, String hash, String version) {
        return ResultUtil.success(compare(request, hash, version, null));
    }

    /**
     * version difference with difference working version
     */
    public ResponseResult<Object> compare(HttpServletRequest request, String hash, String compareVersion, String targetVersion) {
        String token = jwtTokenUtils.getToken(request);
        String spaceId = request.getParameter("spaceId");
        if (spaceService.authenticCheck(token, spaceId)) {
            return ResultUtil.errorInternational("PERMISSION_DENIED");
        }
        return ResultUtil.success(SvnUtil.differVersion(elfinderStorageService.getElfinderStorage(request, spaceId).fromHash(hash).toString(), compareVersion, targetVersion));
    }

    /**
     * svn change log record of specific file
     */
    public ResponseResult<Object> svnChangeLog(HttpServletRequest request, String hash) {
        String token = jwtTokenUtils.getToken(request);
        if (spaceService.authenticCheck(token, request.getParameter("spaceId"))) {
            return ResultUtil.errorInternational("PERMISSION_DENIED");
        }
        return ResultUtil.success(SvnUtil.svnLog(elfinderStorageService.getElfinderStorage(request, request.getParameter("spaceId")).fromHash(hash).toString()));
    }

    /**
     * * influenced file svn log
     */
    public ResponseResult<Object> svnDifferLog(HttpServletRequest request, String spaceId, String revertVersion) {
        if (spaceService.authenticCheck(jwtTokenUtils.getToken(request), spaceId)) {
            return ResultUtil.errorInternational("PERMISSION_DENIED");
        }
        return ResultUtil.success(SvnUtil.svnInfluencedLog(spaceRepository.findById(spaceId).get().getFilePath(), revertVersion));
    }

    /**
     * space publish file changelist
     */
    public ResponseResult<Object> prePublish(String token, String spaceId) {
        if (spaceService.authenticCheck(token, spaceId)) {
            return ResultUtil.errorInternational("PERMISSION_DENIED");
        }
        Optional<Space> spaceOptional = spaceRepository.findById(spaceId);
        if (spaceOptional.isPresent()) {
            Space space = spaceOptional.get();
            String filePath = space.getFilePath();
            SvnUtil.addFiles(filePath);
            return ResultUtil.success();
        } else {
            return ResultUtil.errorInternational("RESOURCE_DOES_NOT_EXIST");
        }
    }

    /**
     * svn space update
     */
    public ResponseResult<Object> svnUpdate(String spaceId) {
        SvnUtil.update(spaceRepository.findById(spaceId).get().getFilePath());
        return ResultUtil.success();
    }
}
