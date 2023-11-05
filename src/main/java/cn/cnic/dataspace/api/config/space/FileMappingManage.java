package cn.cnic.dataspace.api.config.space;

import cn.cnic.dataspace.api.cacheLoading.CacheLoading;
import cn.cnic.dataspace.api.elfinder.core.Target;
import cn.cnic.dataspace.api.elfinder.core.Volume;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorage;
import cn.cnic.dataspace.api.elfinder.service.ElfinderStorageService;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.file.FileMapping;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.SpaceUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import static cn.cnic.dataspace.api.model.space.SpaceSvnLog.*;

/**
 * @ author chl
 */
@Component
@Slf4j
public class FileMappingManage {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ElfinderStorageService elfinderStorageService;

    @Autowired
    private SpaceUrl spaceUrl;

    @Autowired
    private CacheLoading cacheLoading;

    public Operator getOperator(String userEmail) {
        return cacheLoading.getOperator(userEmail);
    }

    public boolean isFolder(String path, String spaceId) {
        FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("path").is(path)), FileMapping.class, spaceId);
        if (null == fileMapping) {
            return false;
        }
        return fileMapping.getType() == 1;
    }

    public List<FileMapping> getFileMappingList(String path, String spaceId) {
        String id = "0";
        if (!path.equals("/")) {
            FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("path").is(path)), FileMapping.class, spaceId);
            if (null == fileMapping) {
                return new ArrayList<>(0);
            }
            id = fileMapping.getId();
        }
        List<FileMapping> fileMappings = mongoTemplate.find(new Query().addCriteria(Criteria.where("fId").is(id)), FileMapping.class, spaceId);
        return fileMappings;
    }

    public FileMapping getFileMapping(String path, String spaceId) {
        FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("path").is(path)), FileMapping.class, spaceId);
        return fileMapping;
    }

    public FileMapping getFileMappingAsHash(String hash, String spaceId) {
        FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("hash").is(hash)), FileMapping.class, spaceId);
        return fileMapping;
    }

    /**
     * Obtain File Author
     */
    public String getFileAuthor(String path, String spaceId) {
        FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("path").is(path)), FileMapping.class, spaceId);
        if (null == fileMapping) {
            throw new CommonException("文件未找到!");
        }
        return fileMapping.getAuthor().getEmail();
    }

    /**
     * Obtain File Author
     */
    public String getFileAuthorAsHash(String hash, String spaceId) {
        FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("hash").is(hash)), FileMapping.class, spaceId);
        if (null == fileMapping) {
            throw new CommonException("文件未找到!");
        }
        return fileMapping.getAuthor().getEmail();
    }

    /**
     * Get File/Folder Size
     */
    public long getSizeBytes(String path, String spaceId) {
        FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("path").is(path)), FileMapping.class, spaceId);
        if (null == fileMapping) {
            return 0;
        }
        if (fileMapping.getType() == 1) {
            return folderRecursion(fileMapping.getId(), spaceId);
        } else {
            return fileMapping.getSize();
        }
    }

    private long folderRecursion(String fId, String spaceId) {
        long total = 0;
        Criteria criteria = Criteria.where("fId").is(fId);
        List<FileMapping> fileMappings = mongoTemplate.find(new Query().addCriteria(criteria), FileMapping.class, spaceId);
        if (fileMappings.isEmpty()) {
            return 0;
        }
        for (FileMapping fileMapping : fileMappings) {
            int type = fileMapping.getType();
            if (type == 1) {
                total += folderRecursion(fileMapping.getId(), spaceId);
            } else {
                total += fileMapping.getSize();
            }
        }
        return total;
    }

    /**
     * Parsing File Hash
     */
    private String getFileHash(String path, ElfinderStorage elfinderStorage) {
        // File parsing processing
        if (null == elfinderStorage) {
            return null;
        }
        Volume volume = elfinderStorage.getVolumes().get(0);
        Target target = volume.fromPath(path);
        try {
            return elfinderStorage.getHash(target);
        } catch (IOException ioException) {
            ioException.printStackTrace();
            return null;
        }
    }

    /**
     * Obtain log space display path
     */
    private boolean getSpacePath(Path path) {
        String rootDir = spaceUrl.getRootDir();
        String replace = path.toString().replaceAll(rootDir, "");
        String[] pathSplit = replace.split(CommonUtils.FILE_SPLIT);
        return pathSplit.length == 3 ? true : false;
    }

    /**
     * File Mapping Management
     */
    public void transit(String type, String spaceId, Path path, Path tarPath, boolean isFolder, String source, Operator operator) {
        switch(type) {
            case FILE_COPY:
                copy(path, tarPath, spaceId, isFolder, operator);
                break;
            case FILE_MOVE:
                move(path, tarPath, spaceId, isFolder);
                break;
            case FILE_RENAME:
                rename(path, tarPath, spaceId, isFolder);
                break;
            default:
                break;
        }
    }

    public void transit(String type, String spaceId, Path path, boolean exists, boolean isFolder, String source, long size, Operator operator) {
        switch(type) {
            case FILE_CREATE:
                create(path, spaceId, isFolder, size, operator, exists);
                break;
            case FILE_UPLOAD:
                create(path, spaceId, isFolder, size, operator, exists);
                break;
            case FILE_DELETE:
                delete(path, spaceId, isFolder);
                break;
            case FILE_MODIFY:
                updateFile(path, spaceId, size);
                break;
            default:
                break;
        }
    }

    /**
     * Space Import - Network Disk Import - Share Link Import
     */
    public void mappingFileOrFolder(List<String> pathList, String spaceId, String email) {
        ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorageSimple(spaceId);
        for (String s : pathList) {
            File file = new File(s);
            createMapping(file.toPath(), spaceId, file.isDirectory(), file.length(), getOperator(email), elfinderStorage);
        }
    }

    /**
     * Create File Folder
     */
    private void create(Path path, String spaceId, boolean isFolder, long size, Operator operator, boolean exists) {
        ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorageSimple(spaceId);
        if (exists) {
            FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("path").is(path.toString())), FileMapping.class, spaceId);
            if (fileMapping == null) {
                createMapping(path, spaceId, isFolder, size, operator, elfinderStorage);
            } else {
                // modify
                Update update = new Update();
                update.set("size", size);
                update.set("updateTime", new Date());
                mongoTemplate.findAndModify(new Query().addCriteria(Criteria.where("_id").is(fileMapping.getId())), update, new FindAndModifyOptions().returnNew(true).upsert(true), FileMapping.class, spaceId);
            }
            return;
        }
        createMapping(path, spaceId, isFolder, size, operator, elfinderStorage);
    }

    private void createMapping(Path path, String spaceId, boolean isFolder, long size, Operator operator, ElfinderStorage elfinderStorage) {
        String fId = "";
        String fHash = "";
        String filePath = path.toString();
        boolean judge = mongoTemplate.exists(new Query().addCriteria(Criteria.where("path").is(filePath)), FileMapping.class, spaceId);
        if (judge) {
            return;
        }
        if (getSpacePath(path)) {
            fId = "0";
            fHash = "A_";
        } else {
            String parentPath = path.getParent().toString();
            FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("path").is(parentPath)), FileMapping.class, spaceId);
            if (fileMapping == null) {
                throw new RuntimeException("父级文件夹未找到！");
            }
            fId = fileMapping.getId();
            fHash = fileMapping.getHash();
        }
        String fileName = path.getFileName().toString();
        FileMapping fileMappingOne = getCreateMapping(filePath, fileName, isFolder, size, operator, fId, fHash, elfinderStorage);
        // FileMapping fileMappingOne = new FileMapping();
        // fileMappingOne.setId(CommonUtils.generateSnowflake());
        // fileMappingOne.setFId(fId);
        // fileMappingOne.setFHash(fHash);
        // fileMappingOne.setHash(getFileHash(filePath,elfinderStorage));
        // fileMappingOne.setName(fileName);
        // fileMappingOne.setPath(filePath);
        // if(!isFolder){
        // fileMappingOne.setSuffix(fileName.substring(fileName.lastIndexOf(".")+1));
        // }
        // fileMappingOne.setSize(size);
        // fileMappingOne.setType((isFolder ? 1 : 0));
        // fileMappingOne.setAuthor(operator);
        // fileMappingOne.setCreateTime(new Date());
        // fileMappingOne.setUpdateTime(new Date());
        mongoTemplate.insert(fileMappingOne, spaceId);
    }

    /**
     * File Movement
     */
    private void move(Path sourcePath, Path targetPath, String spaceId, boolean isFolder) {
        String source = sourcePath.toString();
        String target = targetPath.toString();
        String targetId = "";
        String targetHash = "";
        if (getSpacePath(targetPath)) {
            targetId = "0";
            targetHash = "A_";
        } else {
            String parentPath = targetPath.getParent().toString();
            FileMapping tarParent = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("path").is(parentPath)), FileMapping.class, spaceId);
            if (tarParent == null) {
                throw new RuntimeException("父级文件夹未找到！");
            }
            targetId = tarParent.getId();
            targetHash = tarParent.getHash();
        }
        FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("path").is(source)), FileMapping.class, spaceId);
        if (fileMapping == null) {
            throw new RuntimeException("移动的原文件夹未找到！");
        }
        ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorageSimple(spaceId);
        String fileHash = getFileHash(target, elfinderStorage);
        Update update = new Update();
        update.set("fId", targetId);
        update.set("path", target);
        update.set("fHash", targetHash);
        update.set("hash", fileHash);
        update.set("updateTime", new Date());
        mongoTemplate.findAndModify(new Query().addCriteria(Criteria.where("_id").is(fileMapping.getId())), update, new FindAndModifyOptions().returnNew(true).upsert(true), FileMapping.class, spaceId);
        if (isFolder) {
            moveFolder(fileMapping.getId(), fileHash, spaceId, source, target, elfinderStorage);
        }
    }

    /**
     * Circular Move Files
     */
    private void moveFolder(String fId, String hash, String spaceId, String source, String target, ElfinderStorage elfinderStorage) {
        List<FileMapping> fileMappings = mongoTemplate.find(new Query().addCriteria(Criteria.where("fId").is(fId)), FileMapping.class, spaceId);
        Iterator<FileMapping> iterator = fileMappings.iterator();
        while (iterator.hasNext()) {
            FileMapping next = iterator.next();
            String newPath = target + next.getPath().replaceAll(CommonUtils.escapeExprSpecialWord(source), "");
            String fileHash = getFileHash(newPath, elfinderStorage);
            Update update = new Update();
            update.set("fHash", hash);
            update.set("path", newPath);
            update.set("hash", getFileHash(newPath, elfinderStorage));
            update.set("updateTime", new Date());
            mongoTemplate.findAndModify(new Query().addCriteria(Criteria.where("_id").is(next.getId())), update, new FindAndModifyOptions().returnNew(true).upsert(true), FileMapping.class, spaceId);
            if (next.getType() == 1) {
                moveFolder(next.getId(), fileHash, spaceId, source + "/" + next.getName(), target + "/" + next.getName(), elfinderStorage);
            }
        }
    }

    /**
     * File copying
     */
    private void copy(Path sourcePath, Path targetPath, String spaceId, boolean isFolder, Operator operator) {
        String source = sourcePath.toString();
        String target = targetPath.toString();
        String targetId = "";
        String targetHash = "";
        boolean exists = mongoTemplate.exists(new Query().addCriteria(Criteria.where("path").is(target)), FileMapping.class, spaceId);
        if (exists) {
            return;
        }
        if (getSpacePath(targetPath)) {
            targetId = "0";
            targetHash = "A_";
        } else {
            String parentPath = targetPath.getParent().toString();
            FileMapping tarParent = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("path").is(parentPath)), FileMapping.class, spaceId);
            if (tarParent == null) {
                throw new RuntimeException("父级文件夹未找到！");
            }
            targetId = tarParent.getId();
            targetHash = tarParent.getHash();
        }
        FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("path").is(source)), FileMapping.class, spaceId);
        if (fileMapping == null) {
            throw new RuntimeException("复制的原文件夹未找到！");
        }
        String id = fileMapping.getId();
        ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorageSimple(spaceId);
        fileMapping.setId(CommonUtils.generateSnowflake());
        fileMapping.setFId(targetId);
        fileMapping.setFHash(targetHash);
        fileMapping.setPath(target);
        fileMapping.setHash(getFileHash(target, elfinderStorage));
        fileMapping.setAuthor(operator);
        fileMapping.setCreateTime(new Date());
        fileMapping.setUpdateTime(new Date());
        if (isFolder) {
            List<FileMapping> fileMappingList = copyFolder(id, fileMapping.getId(), fileMapping.getHash(), spaceId, source, target, elfinderStorage, operator);
            if (fileMappingList.isEmpty()) {
                return;
            }
            fileMappingList.add(fileMapping);
            mongoTemplate.insert(fileMappingList, spaceId);
        } else {
            mongoTemplate.insert(fileMapping, spaceId);
        }
    }

    /**
     * Circular Copy Folder
     */
    private List<FileMapping> copyFolder(String sourceId, String targetId, String hash, String spaceId, String source, String target, ElfinderStorage elfinderStorage, Operator operator) {
        List<FileMapping> fileMappingList = new ArrayList<>(10);
        List<FileMapping> fileMappings = mongoTemplate.find(new Query().addCriteria(Criteria.where("fId").is(sourceId)), FileMapping.class, spaceId);
        Iterator<FileMapping> iterator = fileMappings.iterator();
        while (iterator.hasNext()) {
            FileMapping next = iterator.next();
            String id = next.getId();
            String fileId = CommonUtils.generateSnowflake();
            String s = next.getPath().replaceAll(CommonUtils.escapeExprSpecialWord(source), "");
            String newPath = target + s;
            String fileHash = getFileHash(newPath, elfinderStorage);
            next.setId(fileId);
            next.setFId(targetId);
            next.setHash(fileHash);
            next.setFHash(hash);
            next.setPath(newPath);
            next.setAuthor(operator);
            next.setCreateTime(new Date());
            next.setUpdateTime(new Date());
            fileMappingList.add(next);
            if (next.getType() == 1) {
                List<FileMapping> copyList = copyFolder(id, fileId, fileHash, spaceId, source + "/" + next.getName(), target + "/" + next.getName(), elfinderStorage, operator);
                if (!copyList.isEmpty()) {
                    fileMappingList.addAll(copyList);
                }
            }
        }
        return fileMappingList;
    }

    /**
     * Rename - File Mapping
     */
    private void rename(Path sourcePath, Path targetPath, String spaceId, boolean isFolder) {
        ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorageSimple(spaceId);
        String source = sourcePath.toString();
        String target = targetPath.toString();
        String fileName = targetPath.getFileName().toString();
        String fileHash = getFileHash(target, elfinderStorage);
        Criteria criteria = new Criteria();
        if (isFolder) {
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(source) + ".*$", Pattern.CASE_INSENSITIVE);
            criteria.and("path").regex(pattern);
            List<FileMapping> fileMappingList = mongoTemplate.find(new Query().addCriteria(criteria), FileMapping.class, spaceId);
            if (fileMappingList.isEmpty()) {
                return;
            }
            Iterator<FileMapping> iterator = fileMappingList.iterator();
            while (iterator.hasNext()) {
                FileMapping next = iterator.next();
                String path = next.getPath();
                Update update = new Update();
                if (path.equals(source)) {
                    // Directly modify the modified folder
                    update.set("path", target);
                    update.set("name", fileName);
                    update.set("hash", fileHash);
                } else {
                    // The files under the modified folder need to be processed in the path
                    if (path.split(CommonUtils.FILE_SPLIT).length == source.split(CommonUtils.FILE_SPLIT).length) {
                        continue;
                    }
                    String s = path.replaceAll(CommonUtils.escapeExprSpecialWord(source), "");
                    String newPath = target + s;
                    String newHash = getFileHash(newPath, elfinderStorage);
                    String parent = new File(newPath).getParent();
                    update.set("fHash", getFileHash(parent, elfinderStorage));
                    update.set("path", newPath);
                    update.set("hash", newHash);
                }
                mongoTemplate.findAndModify(new Query().addCriteria(Criteria.where("path").is(path)), update, new FindAndModifyOptions().returnNew(true).upsert(true), FileMapping.class, spaceId);
            }
        } else {
            criteria.and("path").is(source);
            FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(criteria), FileMapping.class, spaceId);
            if (null == fileMapping) {
                return;
            }
            Update update = new Update();
            update.set("path", target);
            update.set("name", fileName);
            if (!isFolder) {
                update.set("suffix", fileName.substring(fileName.lastIndexOf(".") + 1));
            }
            update.set("hash", fileHash);
            mongoTemplate.findAndModify(new Query().addCriteria(Criteria.where("_id").is(fileMapping.getId())), update, new FindAndModifyOptions().returnNew(true).upsert(true), FileMapping.class, spaceId);
        }
    }

    // private void renameFolder(String spaceId,String fId,String source,String target,String fHash,ElfinderStorage elfinderStorage){
    // 
    // List<FileMapping> fileMappingList = mongoTemplate.find(new Query()
    // .addCriteria(Criteria.where("fId").is(fId)), FileMapping.class,spaceId);
    // if(fileMappingList.isEmpty()){
    // return;
    // }
    // for (FileMapping mapping : fileMappingList) {
    // String path = mapping.getPath();
    // String s = path.replaceAll(source, "");
    // String newPath = target+s;
    // String newHash = getFileHash(newPath, elfinderStorage);
    // Update update = new Update();
    // update.set("fHash",fHash);
    // update.set("path",newPath);
    // update.set("hash",newHash);
    // mongoTemplate.findAndModify(new Query().addCriteria(Criteria.where("_id").is(mapping.getId())), update,
    // new FindAndModifyOptions().returnNew(true).upsert(true),FileMapping.class,spaceId);
    // if(mapping.getType() == 1){
    // renameFolder(spaceId,mapping.getId(),source,target,newHash,elfinderStorage);
    // }
    // }
    // }
    /**
     * Delete - File Mapping
     */
    private void delete(Path path, String spaceId, boolean isFolder) {
        String rootPath = path.toString();
        Criteria criteria = Criteria.where("path").is(rootPath);
        FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(criteria), FileMapping.class, spaceId);
        String id = fileMapping.getId();
        if (isFolder) {
            deleteFolder(id, spaceId);
        }
        mongoTemplate.remove(new Query().addCriteria(criteria), spaceId);
    }

    private void deleteFolder(String id, String spaceId) {
        Criteria criteria = Criteria.where("fId").is(id);
        List<FileMapping> fileMappingList = mongoTemplate.find(new Query().addCriteria(criteria), FileMapping.class, spaceId);
        for (FileMapping fileMapping : fileMappingList) {
            int type = fileMapping.getType();
            if (type == 1) {
                deleteFolder(fileMapping.getId(), spaceId);
            }
            mongoTemplate.remove(fileMapping, spaceId);
        }
    }

    /**
     * Modify File Content - File Mapping
     */
    private void updateFile(Path path, String spaceId, long size) {
        String filePath = path.toString();
        FileMapping fileMapping = mongoTemplate.findOne(new Query().addCriteria(Criteria.where("path").is(filePath)), FileMapping.class, spaceId);
        if (fileMapping == null) {
            throw new RuntimeException("文件夹未找到！");
        }
        if (fileMapping.getType() != 0) {
            return;
        }
        Update update = new Update();
        update.set("size", size);
        update.set("updateTime", new Date());
        mongoTemplate.findAndModify(new Query().addCriteria(Criteria.where("path").is(filePath)), update, new FindAndModifyOptions().returnNew(true).upsert(true), FileMapping.class, spaceId);
    }

    /**
     * System version 1.2.1 obtains a list of all file mappings in the space
     */
    public List<FileMapping> getSpaceFileMappingList(String spaceId, File[] files, Operator operator) {
        ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorageSimple(spaceId);
        return copyFolder("0", "A_", files, spaceId, operator, elfinderStorage);
    }

    /**
     * Synchronize file/folder database mapping - do not carry parent folder information
     */
    public void saveSpaceFileMapping(String spaceId, File file, Operator operator) {
        Path path = file.toPath();
        create(path, spaceId, file.isDirectory(), file.length(), operator, false);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                saveSpaceFileMapping(spaceId, f, operator);
            }
        }
    }

    /**
     * Synchronize file/folder database mapping - carry parent folder information
     */
    public void saveSpaceFileMapping(String spaceId, File file, Operator operator, String fId, String fHash) {
        ElfinderStorage elfinderStorage = elfinderStorageService.getElfinderStorageSimple(spaceId);
        FileMapping rootMapping = getCreateMapping(file.getPath(), file.getName(), file.isDirectory(), file.length(), operator, fId, fHash, elfinderStorage);
        mongoTemplate.insert(rootMapping, spaceId);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            List<FileMapping> fileMappings = copyFolder(rootMapping.getId(), rootMapping.getHash(), files, operator, elfinderStorage);
            mongoTemplate.insert(fileMappings, spaceId);
        }
    }

    /**
     * System Start Sync File - Find Log Complete File Information
     */
    private List<FileMapping> copyFolder(String fId, String fHash, File[] files, String spaceId, Operator operator, ElfinderStorage elfinderStorage) {
        List<FileMapping> fileMappingList = new ArrayList<>();
        for (File file : files) {
            String name = file.getName();
            Criteria orOperator = new Criteria().orOperator(Criteria.where("fileAction").is("create"), Criteria.where("fileAction").is("upload"));
            Criteria criteria = Criteria.where("action").is("file").and("spaceId").is(spaceId).andOperator(orOperator);
            Pattern pattern = Pattern.compile("^.*" + CommonUtils.escapeExprSpecialWord(name) + ".*$", Pattern.CASE_INSENSITIVE);
            criteria.and("description").regex(pattern);
            List<SpaceSvnLog> spaceSvnLogs = mongoTemplate.find(new Query().addCriteria(criteria), SpaceSvnLog.class);
            if (!spaceSvnLogs.isEmpty()) {
                operator = spaceSvnLogs.get(0).getOperator();
            }
            FileMapping createMapping = getCreateMapping(file.getPath(), file.getName(), file.isDirectory(), file.length(), operator, fId, fHash, elfinderStorage);
            if (!spaceSvnLogs.isEmpty()) {
                createMapping.setCreateTime(spaceSvnLogs.get(0).getCreateTime());
                createMapping.setUpdateTime(createMapping.getCreateTime());
                spaceSvnLogs.clear();
            }
            fileMappingList.add(createMapping);
            if (createMapping.getType() == 1) {
                List<FileMapping> copyList = copyFolder(createMapping.getId(), createMapping.getHash(), file.listFiles(), spaceId, operator, elfinderStorage);
                if (!copyList.isEmpty()) {
                    fileMappingList.addAll(copyList);
                }
            }
        }
        return fileMappingList;
    }

    /**
     * Circular Folder - Create
     */
    private List<FileMapping> copyFolder(String fId, String fHash, File[] files, Operator operator, ElfinderStorage elfinderStorage) {
        List<FileMapping> fileMappingList = new ArrayList<>();
        for (File file : files) {
            FileMapping createMapping = getCreateMapping(file.getPath(), file.getName(), file.isDirectory(), file.length(), operator, fId, fHash, elfinderStorage);
            fileMappingList.add(createMapping);
            if (createMapping.getType() == 1) {
                List<FileMapping> copyList = copyFolder(createMapping.getId(), createMapping.getHash(), file.listFiles(), operator, elfinderStorage);
                if (!copyList.isEmpty()) {
                    fileMappingList.addAll(copyList);
                }
            }
        }
        return fileMappingList;
    }

    private FileMapping getCreateMapping(String path, String fileName, boolean isFolder, long size, Operator operator, String fId, String fHash, ElfinderStorage elfinderStorage) {
        FileMapping fileMappingOne = new FileMapping();
        fileMappingOne.setId(CommonUtils.generateSnowflake());
        fileMappingOne.setFId(fId);
        fileMappingOne.setFHash(fHash);
        fileMappingOne.setHash(getFileHash(path, elfinderStorage));
        fileMappingOne.setName(fileName);
        fileMappingOne.setPath(path);
        if (!isFolder) {
            fileMappingOne.setSuffix(fileName.substring(fileName.lastIndexOf(".") + 1));
        }
        fileMappingOne.setSize(size);
        fileMappingOne.setType((isFolder ? 1 : 0));
        fileMappingOne.setAuthor(operator);
        fileMappingOne.setCreateTime(new Date());
        fileMappingOne.setUpdateTime(new Date());
        return fileMappingOne;
    }
}
