package cn.cnic.dataspace.api.bigdataprocessing;

import cn.cnic.dataspace.api.config.space.FileMappingManage;
import cn.cnic.dataspace.api.model.file.FileMapping;
import cn.cnic.dataspace.api.model.space.Space;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.model.space.child.AuthorizationPerson;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.statistics.SpaceDataStatistic;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.SpaceUrl;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Space log data processing rules
 */
@Slf4j
@Component
public class SpatialLogRule {

    @Resource
    private MongoTemplate mongoTemplate;

    @Autowired
    private SpaceUrl spaceUrl;

    @Autowired
    private FileMappingManage fileMappingManage;

    // implement
    public void execute() {
        // 1. Obtain all spaces
        List<Space> spaceList = mongoTemplate.find(new Query().addCriteria(Criteria.where("state").ne("0")), Space.class);
        // The number of logs with a cycle space judgment exceeding one month
        Date pastMonth = CommonUtils.getPastMonth(1);
        String spaceLogPath = spaceUrl.getSpaceLogPath();
        for (Space space : spaceList) {
            String spaceId = space.getSpaceId();
            Criteria lte = Criteria.where("spaceId").is(spaceId).and("createTime").lte(pastMonth);
            List<SpaceSvnLog> spaceSvnLogs = mongoTemplate.find(new Query().addCriteria(lte), SpaceSvnLog.class);
            if (!spaceSvnLogs.isEmpty()) {
                log.info("空间：" + space.getSpaceName() + " 日志文件 共 " + spaceSvnLogs.size() + " 条 >>>>>>>>>>");
                File file = new File(spaceLogPath, spaceId);
                if (!file.exists()) {
                    file.mkdirs();
                }
                String fileName = CommonUtils.getDateString(pastMonth) + "_log.json";
                // write file
                String json = JSONObject.toJSONString(spaceSvnLogs);
                File jsonFile = new File(file.getPath(), fileName);
                try {
                    FileUtils.write(jsonFile, json, "utf-8", false);
                    // Remove CSV file
                    File file1 = new File(file.getPath(), spaceId + ".csv");
                    if (file1.exists()) {
                        file1.delete();
                    }
                    log.info("日志导出文件处理成功!");
                    mongoTemplate.remove(new Query().addCriteria(lte), SpaceSvnLog.class);
                } catch (IOException ioException) {
                    log.info("日志导出文件处理失败! {} " + ioException.getMessage());
                    // ioException.printStackTrace();
                }
            }
        }
    }

    /**
     * Spatial entity file data synchronization
     */
    public void spaceFileUpload(long logCount, Space space, SpaceDataStatistic spaceDataStatistic) {
        String spaceId = space.getSpaceId();
        if (logCount > 0) {
            log.info("开始校验 >>>>>>> 空间：" + space.getSpaceName() + "（" + spaceId + "）");
            String filePath = space.getFilePath();
            Operator operator = new Operator();
            Set<AuthorizationPerson> authorizationList = space.getAuthorizationList();
            for (AuthorizationPerson person : authorizationList) {
                if (person.getRole().equals("拥有者")) {
                    operator.setEmail(person.getEmail());
                    operator.setPersonId(person.getUserId());
                    operator.setPersonName(person.getUserName());
                    break;
                }
            }
            long data = 0l;
            int count = 0;
            File rootFile = new File(filePath);
            if (rootFile.exists()) {
                File[] files = rootFile.listFiles();
                if (files.length > 0) {
                    // Obtain files under the space root
                    Map<String, List<FileMapping>> fileLevelMapping = getFileLevelMapping("0", spaceId);
                    for (File file : files) {
                        // Remove redundant database mappings
                        deleteFileMapping(fileLevelMapping, file, spaceId);
                        // Recursive verification
                        String res = updateSpaceFileMapping(file, spaceId, operator, "0", "A_");
                        String[] split = res.split(":");
                        count += Integer.valueOf(split[0]);
                        data += Long.valueOf(split[1]);
                    }
                    // Delete file mappings that are not present in the file system
                    for (String fileName : fileLevelMapping.keySet()) {
                        List<FileMapping> fileMappings1 = fileLevelMapping.get(fileName);
                        for (FileMapping mapping : fileMappings1) {
                            mappingDel(mapping, spaceId);
                        }
                    }
                    fileLevelMapping.clear();
                }
            }
            // Space quantity+file quantity modification
            boolean judge = false;
            if (spaceDataStatistic.getFileNum() != count) {
                log.info("空间：" + space.getSpaceName() + " 文件数量有变动 由 " + spaceDataStatistic.getFileNum() + " 变为 " + count);
                spaceDataStatistic.setFileNum(count);
                judge = true;
            }
            if (data != spaceDataStatistic.getDataSize()) {
                log.info("空间：" + space.getSpaceName() + " 实体文件大小有变动 由 " + spaceDataStatistic.getDataSize() + " 变为 " + data);
                spaceDataStatistic.setDataSize(data);
                judge = true;
            }
            if (judge) {
                mongoTemplate.save(spaceDataStatistic);
            }
            log.info("校验结束 >>>> 空间：" + space.getSpaceName() + "（" + spaceId + "）");
        }
    }

    private String updateSpaceFileMapping(File rootFile, String spaceId, Operator operator, String fId, String fHash) {
        long data = 0l;
        int count = 0;
        String path = rootFile.getPath();
        Query query = new Query().addCriteria(Criteria.where("path").is(path));
        FileMapping fileMapping = mongoTemplate.findOne(query, FileMapping.class, spaceId);
        if (null == fileMapping) {
            // Add Mapping
            fileMappingManage.saveSpaceFileMapping(spaceId, rootFile, operator, fId, fHash);
            String res = cn.cnic.dataspace.api.util.FileUtils.getFileLen(rootFile);
            String[] split = res.split(":");
            count += Integer.valueOf(split[0]);
            data += Long.valueOf(split[1]);
        } else {
            if (rootFile.isDirectory()) {
                // Get File Subordinate Mapping
                Map<String, List<FileMapping>> fileLevelMapping = getFileLevelMapping(fileMapping.getId(), spaceId);
                for (File file : rootFile.listFiles()) {
                    // Remove redundant database mappings
                    deleteFileMapping(fileLevelMapping, file, spaceId);
                    // Recursive verification
                    String res = updateSpaceFileMapping(file, spaceId, fileMapping.getAuthor(), fileMapping.getId(), fileMapping.getHash());
                    String[] split = res.split(":");
                    count += Integer.valueOf(split[0]);
                    data += Long.valueOf(split[1]);
                }
                // Delete file mappings that are not present in the file system
                for (String fileName : fileLevelMapping.keySet()) {
                    List<FileMapping> fileMappings1 = fileLevelMapping.get(fileName);
                    for (FileMapping mapping : fileMappings1) {
                        mappingDel(mapping, spaceId);
                    }
                }
                fileLevelMapping.clear();
            } else {
                // Comparison information modification
                long length = rootFile.length();
                data += length;
                count++;
                if (fileMapping.getSize() != length || fileMapping.getType() != 0) {
                    Update update = new Update();
                    update.set("size", length);
                    update.set("type", 0);
                    mongoTemplate.upsert(new Query().addCriteria(Criteria.where("_id").is(fileMapping.getId())), update, spaceId);
                }
            }
        }
        return count + ":" + data;
    }

    /**
     * Get File Subordinate Mapping
     */
    private Map<String, List<FileMapping>> getFileLevelMapping(String fId, String spaceId) {
        // Obtain the mapping under the corresponding folder of the database - format assembly
        List<FileMapping> fileMappings = mongoTemplate.find(new Query().addCriteria(Criteria.where("fId").is(fId)), FileMapping.class, spaceId);
        Map<String, List<FileMapping>> fileMap = new HashMap<>(fileMappings.size());
        Iterator<FileMapping> iterator = fileMappings.iterator();
        while (iterator.hasNext()) {
            FileMapping next = iterator.next();
            if (fileMap.containsKey(next.getName())) {
                List<FileMapping> fileMappings1 = fileMap.get(next.getName());
                fileMappings1.add(next);
                fileMap.put(next.getName(), fileMappings1);
            } else {
                List<FileMapping> fMList = new ArrayList<>(1);
                fMList.add(next);
                fileMap.put(next.getName(), fMList);
            }
        }
        fileMappings.clear();
        return fileMap;
    }

    /**
     * Remove redundant database mappings
     */
    private void deleteFileMapping(Map<String, List<FileMapping>> fileMap, File file, String spaceId) {
        // Compare database mappings with redundant deletions
        String name = file.getName();
        if (fileMap.containsKey(name)) {
            if (fileMap.get(name).size() > 1) {
                List<FileMapping> fileMappings1 = fileMap.get(name);
                Iterator<FileMapping> iterator1 = fileMappings1.iterator();
                while (iterator1.hasNext()) {
                    // Folder (Path+Type) File (Size+Type+Path)
                    FileMapping next = iterator1.next();
                    int type = next.getType();
                    String path1 = next.getPath();
                    if (file.isDirectory()) {
                        if (type != 1 || !file.getPath().equals(path1)) {
                            mappingDel(next, spaceId);
                            iterator1.remove();
                        }
                    } else {
                        long size = next.getSize();
                        if (type != 0 || size != file.length() || !file.getPath().equals(path1)) {
                            mappingDel(next, spaceId);
                            iterator1.remove();
                        }
                    }
                }
                // If there are still duplicates - keep only one mapping
                if (fileMappings1.size() > 1) {
                    fileMappings1 = fileMappings1.subList(1, fileMappings1.size());
                    for (FileMapping mapping : fileMappings1) {
                        mappingDel(mapping, spaceId);
                    }
                }
                fileMappings1.clear();
            }
            // Remove the processed file data from the mapping map
            fileMap.remove(name);
        }
    }

    private void mappingDel(FileMapping mapping, String spaceId) {
        Query query = new Query().addCriteria(Criteria.where("_id").is(mapping.getId()));
        if (mapping.getType() == 1) {
            deleteFolder(mapping.getId(), spaceId);
        }
        mongoTemplate.remove(query, spaceId);
    }

    private void deleteFolder(String id, String spaceId) {
        Criteria criteria = Criteria.where("fId").is(id);
        List<FileMapping> fileMappingList = mongoTemplate.find(new Query().addCriteria(criteria), FileMapping.class, spaceId);
        for (FileMapping fileMapping : fileMappingList) {
            int type = fileMapping.getType();
            if (type == 1) {
                deleteFolder(fileMapping.getId(), spaceId);
            }
            mongoTemplate.remove(new Query().addCriteria(Criteria.where("_id").is(fileMapping.getId())), spaceId);
        }
    }
}
