package cn.cnic.dataspace.api.filehandle;

import cn.cnic.dataspace.api.config.space.FileMappingManage;
import cn.cnic.dataspace.api.config.space.FileOperationFactory;
import cn.cnic.dataspace.api.config.space.SpaceControlConfig;
import cn.cnic.dataspace.api.elfinder.core.Volume;
import cn.cnic.dataspace.api.elfinder.service.VolumeHandler;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.ftp.minimalftp.FTPConnection;
import cn.cnic.dataspace.api.ftp.minimalftp.Utils;
import cn.cnic.dataspace.api.ftp.minimalftp.api.ResponseException;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.model.space.child.Operator;
import cn.cnic.dataspace.api.model.file.FileMapping;
import cn.cnic.dataspace.api.model.file.UploadFile;
import cn.cnic.dataspace.api.util.*;
import io.milton.http.Auth;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import static cn.cnic.dataspace.api.model.space.SpaceSvnLog.*;
import static cn.cnic.dataspace.api.util.CommonUtils.*;

@Slf4j
public class ControlImpl implements Control<File> {

    private final FTPConnection con;

    private final File rootDir;

    public ControlImpl() {
        this.con = null;
        this.rootDir = null;
    }

    public ControlImpl(FTPConnection con) {
        this.con = con;
        this.rootDir = this.getRoot();
    }

    public ControlImpl(File rootDir, FTPConnection con) {
        this.con = con;
        this.rootDir = rootDir;
        if (!rootDir.exists())
            rootDir.mkdirs();
    }

    private SpaceUrl spaceUrl() {
        return FileOperationFactory.getSpaceUrl();
    }

    private SpaceControlConfig spaceStatisticConfig() {
        return FileOperationFactory.getSpaceStatisticConfig();
    }

    private FileMappingManage fileMappingManage() {
        return FileOperationFactory.getFileMappingManage();
    }

    /**
     * Obtain the current user information for the operation
     */
    private Operator operator() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        Object userAuth = null;
        if (!Objects.isNull(requestAttributes)) {
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            if (request != null) {
                userAuth = request.getHeader("authorization");
            }
        }
        // get wevDav user
        String user = null;
        if (userAuth != null) {
            user = FileOperationFactory.getJwtTokenUtils().getEmail(userAuth.toString());
            if (user == null) {
                Auth auth = new Auth("" + userAuth);
                user = auth.getUser();
            }
        }
        Object authorization = Objects.isNull(con) ? Objects.isNull(user) ? "admin@dataspace.cn" : user : con.getUsername();
        return authorization != null ? fileMappingManage().getOperator(authorization.toString()) : new Operator();
    }

    /**
     * Obtain log space display path
     */
    private String getSpacePath(Path path) {
        String rootDir = spaceUrl().getRootDir();
        String replace = path.toString().replaceAll(rootDir, "");
        String[] pathSplit = replace.split(CommonUtils.FILE_SPLIT);
        String code = pathSplit.length >= 2 ? pathSplit[1] : "";
        return replace.replaceAll("/" + code + "/", "");
    }

    /**
     * Obtain spatial identity - IP conversion
     */
    @Override
    public String spaceId(Path path) {
        String rootDir = spaceUrl().getRootDir();
        String replace = path.toString().replaceAll(rootDir, "");
        String[] pathSplit = replace.split(CommonUtils.FILE_SPLIT);
        String code = pathSplit.length >= 2 ? pathSplit[1] : "";
        if (!code.equals("")) {
            code = spaceStatisticConfig().getSpaceId(code);
        }
        if (null == code || code.equals("")) {
            throw new RuntimeException(messageInternational("FILE_HASH"));
        }
        return code;
    }

    public String spaceId(String path) {
        String rootDir = spaceUrl().getRootDir();
        String replace = path.replaceAll(rootDir, "");
        String[] pathSplit = replace.split(CommonUtils.FILE_SPLIT);
        String code = pathSplit.length >= 2 ? pathSplit[1] : "";
        if (!code.equals("")) {
            code = spaceStatisticConfig().getSpaceId(code);
        }
        if (null == code || code.equals("")) {
            throw new RuntimeException(messageInternational("FILE_HASH"));
        }
        return code;
    }

    // FTP database interaction operation
    public boolean isFolder(String path, String spaceId) {
        return fileMappingManage().isFolder(path, spaceId);
    }

    public List<FileMapping> getFileMappingList(String path, String spaceId) {
        return fileMappingManage().getFileMappingList(path, spaceId);
    }

    public FileMapping getFileMapping(String path, String spaceId) {
        return fileMappingManage().getFileMapping(path, spaceId);
    }

    public FileMapping getFileMappingAsHash(String hash, String spaceId) {
        return fileMappingManage().getFileMappingAsHash(hash, spaceId);
    }

    public void validateFileMakePermissions(String email, String spaceId) {
        spaceStatisticConfig().validateSpacePermissions(email, spaceId, SpaceRoleEnum.F_MAKE.getRole());
    }

    public void validateFileOtherPermissions(String email, String spaceId, String path, String type) {
        String fileAuthor = fileMappingManage().getFileAuthor(path, spaceId);
        String role = "";
        switch(type) {
            case "edit":
                role = fileAuthor.equals(email) ? SpaceRoleEnum.F_EDIT_AM.getRole() : SpaceRoleEnum.F_EDIT_OT.getRole();
                break;
            case "delete":
                role = fileAuthor.equals(email) ? SpaceRoleEnum.F_DEL_AM.getRole() : SpaceRoleEnum.F_DEL_OT.getRole();
                break;
            case "down":
                role = fileAuthor.equals(email) ? SpaceRoleEnum.F_DOWN_AM.getRole() : SpaceRoleEnum.F_DOWN_OT.getRole();
                break;
            default:
                throw new CommonException("未找到类型");
        }
        spaceStatisticConfig().validateSpacePermissions(email, spaceId, role);
    }

    public void validateWebDavFileDownPermissions(String path) {
        this.validateFileOtherPermissions(operator().getEmail(), spaceId(path), path, "down");
    }

    public void validateWebDavFileCreatePermission(String path) {
        validateFileMakePermissions(operator().getEmail(), spaceId(path));
        // this.validateFileOtherPermissions(operator().getEmail(),spaceId(path),path,"down");
    }

    public void validateWebDavFileEditPermission(String path) {
        // validateFileMakePermissions(operator().getEmail(), spaceId(path));
        this.validateFileOtherPermissions(operator().getEmail(), spaceId(path), path, "edit");
    }

    public long getFileSize(String path, String spaceId) {
        return fileMappingManage().getSizeBytes(path, spaceId);
    }

    /**
     * User Action Logging
     */
    private void logIntegrating(String spaceId, String content, String fileAction, String method) {
        Operator operator = operator();
        FileOperationFactory.getSvnSpaceLogRepository().save(SpaceSvnLog.builder().spaceSvnId(generateSnowflake()).spaceId(spaceId).operatorId(operator.getPersonId()).operator(operator).createTime(new Date()).method(method).description(content).version(ACTION_VALUE).action(ACTION_FILE).fileAction(fileAction).build());
    }

    /**
     * Log information acquisition
     */
    private void logMessage(String type, Path path, Path tarPath, boolean isFolder, String method) {
        StringBuffer message = new StringBuffer();
        switch(type) {
            case FILE_CREATE:
                message.append(isFolder ? "创建文件夹: " : "创建文件: ").append(getSpacePath(path));
                break;
            case FILE_COPY:
                message.append(isFolder ? "复制文件夹: " : "复制文件: ").append(getSpacePath(path) + " 至 " + getSpacePath(tarPath));
                break;
            case FILE_MOVE:
                message.append(isFolder ? "移动文件夹: " : "移动文件: ").append(getSpacePath(path) + " 至 " + getSpacePath(tarPath));
                break;
            case FILE_RENAME:
                message.append(isFolder ? "重命名文件夹: " : "重命名文件: ").append(getSpacePath(path) + " 重命名为 " + getSpacePath(tarPath));
                break;
            case FILE_UPLOAD:
                message.append(isFolder ? "上传文件夹: " : "上传文件: ").append(getSpacePath(path));
                break;
            case FILE_DELETE:
                message.append(isFolder ? "删除文件夹: " : "删除文件: ").append(getSpacePath(path));
                break;
            case FILE_MODIFY:
                message.append("修改文件内容: ").append(getSpacePath(path));
                break;
            case FILE_PASS_ON:
                message.append("文件续传: ").append(getSpacePath(path));
                break;
            default:
                break;
        }
        logIntegrating(spaceId(path), message.toString(), type, method);
    }

    /**
     * WebDav+web inflow records
     */
    private void dataInStatistic(String spaceId, String type, long data) {
        spaceStatisticConfig().dataFlow(type, data, spaceId, true);
    }

    /**
     * Create files, folders - web pages (copy) Cut
     */
    @Override
    public void webCreateAndCopy(VolumeHandler src, VolumeHandler dst, String destination) throws IOException {
        Path srcPath = src.getVolume().fromTarget(src.getTarget());
        Path dstPath = dst.getVolume().fromTarget(dst.getTarget());
        String spaceId = spaceId(dstPath);
        if (SpaceSizeControl.validation(spaceId)) {
            // Space capacity limitations
            throw new RuntimeException(messageInternational("FILE_SIZE_FULL"));
        }
        validateFileMakePermissions(operator().getEmail(), spaceId);
        if (srcPath.toString().equals(dstPath.toString())) {
            throw new RuntimeException(messageInternational("FILE_COPY_ERROR"));
        }
        boolean isFolder = src.isFolder();
        if (isFolder) {
            String srcString = srcPath.toString();
            String dstString = dstPath.toString();
            if (dstString.contains(srcString)) {
                throw new RuntimeException(messageInternational("FILE_COPY_ERROR_ONE"));
            }
            createAndCopyFolder(src, dst);
        } else {
            createAndCopyFile(src, dst);
        }
        fileMappingManage().transit(FILE_COPY, spaceId(dstPath), srcPath, dstPath, isFolder, ELFINDER, operator());
        // Copy folder logging
        logMessage(FILE_COPY, srcPath, dstPath, isFolder, ELFINDER);
    }

    /**
     * Web - file and folder creation
     */
    @Override
    public void createFile(Path path) throws IOException {
        String spaceId = spaceId(path);
        if (SpaceSizeControl.validation(spaceId)) {
            // Space capacity limitations
            throw new RuntimeException(messageInternational("FILE_SIZE_FULL"));
        }
        validateFileMakePermissions(operator().getEmail(), spaceId);
        boolean directory = path.toFile().isDirectory();
        Files.createFile(path);
        fileMappingManage().transit(FILE_CREATE, spaceId, path, false, directory, ELFINDER, 0, operator());
        logMessage(FILE_CREATE, path, null, directory, ELFINDER);
    }

    /**
     * Copy webdav file upload
     */
    @Override
    public void createAndCopyFile(File file, InputStream in) throws IOException {
        String spaceId = spaceId(file.toPath());
        if (SpaceSizeControl.validation(spaceId)) {
            throw new RuntimeException(messageInternational("FILE_SIZE_FULL"));
        }
        validateFileMakePermissions(operator().getEmail(), spaceId);
        try (FileOutputStream out = new FileOutputStream(file)) {
            IOUtils.copy(in, out);
        }
        fileMappingManage().transit(FILE_UPLOAD, spaceId, file.toPath(), true, false, WEBDAV, file.length(), operator());
        dataInStatistic(spaceId, "webDav", file.length());
        if (file.length() > 0) {
            logMessage(FILE_UPLOAD, file.toPath(), null, false, WEBDAV);
        }
    }

    /**
     * Copy webdav file upload
     */
    @Override
    public void createAndCopyFileNoAuth(File file, InputStream in) throws IOException {
        String spaceId = spaceId(file.toPath());
        if (SpaceSizeControl.validation(spaceId)) {
            throw new RuntimeException(messageInternational("FILE_SIZE_FULL"));
        }
        // validateFileMakePermissions(operator().getEmail(),spaceId);
        try (FileOutputStream out = new FileOutputStream(file)) {
            IOUtils.copy(in, out);
        }
        fileMappingManage().transit(FILE_UPLOAD, spaceId, file.toPath(), true, false, WEBDAV, file.length(), operator());
        dataInStatistic(spaceId, "webDav", file.length());
        if (file.length() > 0) {
            logMessage(FILE_UPLOAD, file.toPath(), null, false, WEBDAV);
        }
    }

    /**
     * Receives a stream asynchronously, sending a response after it's done
     *
     * @param out The stream
     */
    @Override
    public void receiveStream(OutputStream out, File file, String spaceId, boolean exists, long souData, int ty) {
        new Thread(() -> {
            try {
                con.receiveData(out);
                try {
                    long data = file.length();
                    fileMappingManage().transit(FILE_UPLOAD, spaceId(file.toPath()), file.toPath(), exists, false, FTP, data, operator());
                    spaceStatisticConfig().dataFlow("ftp", (data - souData), spaceId, true);
                    if (ty == 1) {
                        logMessage(FILE_PASS_ON, file.toPath(), null, false, FTP);
                    } else {
                        logMessage(FILE_UPLOAD, file.toPath(), null, false, FTP);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("ftp：上传记录信息错误 {} " + e.getMessage());
                }
                con.sendResponse(226, "File received!");
            } catch (ResponseException ex) {
                con.sendResponse(ex.getCode(), ex.getMessage());
            } catch (Exception ex) {
                con.sendResponse(451, ex.getMessage());
            }
        }).start();
    }

    /**
     * move from origin to destination
     */
    @Override
    public void move(Path origin, Path destination, String move) throws IOException {
        if (destination.toFile().exists()) {
            throw new RuntimeException(messageInternational("FILE_EXIST"));
        }
        String spaceId = spaceId(destination);
        validateFileMakePermissions(operator().getEmail(), spaceId);
        if (origin.toString().equals(destination.toString())) {
            throw new RuntimeException(messageInternational("FILE_COPY_ERROR"));
        }
        boolean originDirectory = origin.toFile().isDirectory();
        if (originDirectory) {
            String srcString = origin.toString();
            String dstString = destination.toString();
            if (dstString.contains(srcString)) {
                throw new RuntimeException(messageInternational("FILE_COPY_ERROR_ONE"));
            }
        }
        Files.move(origin, destination, StandardCopyOption.REPLACE_EXISTING);
        boolean directory = destination.toFile().isDirectory();
        fileMappingManage().transit(FILE_MOVE, spaceId, origin, destination, directory, move, operator());
        logMessage(FILE_MOVE, origin, destination, directory, move);
    }

    /**
     * Create a folder for a unified entrance
     */
    @Override
    public void createFolder(Path path, boolean upload, boolean record, String move) throws IOException {
        String spaceId = spaceId(path);
        validateFileMakePermissions(operator().getEmail(), spaceId);
        Files.createDirectories(path);
        if (null == move) {
            move = ELFINDER;
        }
        if ((!upload)) {
            fileMappingManage().transit(FILE_CREATE, spaceId, path, false, true, move, 0, operator());
            logMessage(FILE_CREATE, path, null, true, move);
        } else {
            if (record) {
                fileMappingManage().transit(FILE_UPLOAD, spaceId, path, false, true, move, 0, operator());
                logMessage(FILE_UPLOAD, path, null, true, move);
            }
        }
    }

    @Override
    public void delete(Path path, String move) throws IOException {
        String spaceId = spaceId(path);
        validateFileOtherPermissions(operator().getEmail(), spaceId, path.toString(), "delete");
        File file = path.toFile();
        if (!file.exists()) {
            return;
        }
        boolean directory = file.isDirectory();
        // long sizeBytes = getSizeBytes(file.toPath(), true);
        long sizeBytes = fileMappingManage().getSizeBytes(file.getPath(), spaceId);
        if (directory) {
            Files.walk(file.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } else {
            if (!file.delete()) {
                throw new IOException("Couldn't delete the file");
            }
        }
        fileMappingManage().transit(FILE_DELETE, spaceId, path, false, directory, move, 0, operator());
        logMessage(FILE_DELETE, path, null, directory, move);
        SpaceSizeControl.updateActual(spaceId, -sizeBytes);
        spaceStatisticConfig().deleteData(spaceId, sizeBytes);
    }

    /**
     * Unified Entry File Renaming
     */
    @Override
    public void rename(Path origin, Path destination, String move) {
        String spaceId = spaceId(destination);
        validateFileOtherPermissions(operator().getEmail(), spaceId, origin.toString(), "edit");
        if (destination.toFile().exists()) {
            throw new RuntimeException(messageInternational("COMMAND_TARGET_FILE"));
        }
        boolean directory = origin.toFile().isDirectory();
        try {
            Files.move(origin, destination);
        } catch (IOException e) {
            throw new RuntimeException(messageInternational("FOLDER_NAME_EMPTY"));
        }
        fileMappingManage().transit(FILE_RENAME, spaceId, origin, destination, directory, move, operator());
        logMessage(FILE_RENAME, origin, destination, directory, move);
    }

    /**
     * Write content into file page file modification
     */
    @Override
    public void write(String filePath, String data, OutputStream output, String encoding) throws IOException {
        File file = new File(filePath);
        String spaceId = spaceId(file.toPath());
        if (SpaceSizeControl.validation(spaceId)) {
            // Space capacity limitations
            throw new RuntimeException(messageInternational("FILE_SIZE_FULL"));
        }
        validateFileOtherPermissions(operator().getEmail(), spaceId, filePath, "edit");
        long start = new File(filePath).length();
        IOUtils.write(data, output, encoding);
        long end = file.length();
        fileMappingManage().transit(FILE_MODIFY, spaceId, file.toPath(), false, false, ELFINDER, end, operator());
        spaceStatisticConfig().dataFlow("web-update", (end - start), spaceId, true);
        logMessage(FILE_MODIFY, file.toPath(), null, false, ELFINDER);
    }

    /**
     * Web file sharding and merging
     */
    @Override
    public void uploadMerge(List<UploadFile> uploadFiles, String realPath) {
        String spaceId = spaceId(realPath);
        if (SpaceSizeControl.validation(spaceId)) {
            // Space capacity limitations
            throw new RuntimeException(messageInternational("FILE_SIZE_FULL"));
        }
        validateFileMakePermissions(operator().getEmail(), spaceId);
        UploadFile uploadFile = uploadFiles.get(0);
        // merge
        FileChannel out = null;
        Path path = null;
        try {
            String fileName = uploadFile.getFileName();
            String filePath = uploadFile.getFilePath().substring(0, uploadFile.getFilePath().lastIndexOf("-"));
            out = new FileOutputStream(filePath).getChannel();
            for (UploadFile file : uploadFiles) {
                File patch = new File(file.getFilePath());
                FileChannel in = new FileInputStream(patch).getChannel();
                in.transferTo(0, in.size(), out);
                in.close();
                patch.delete();
            }
            File file = new File(filePath);
            path = new File(realPath, fileName).toPath();
            Files.move(file.toPath(), path);
            String substring = filePath.substring(0, filePath.lastIndexOf("/"));
            new File(substring).delete();
        } catch (Exception e) {
            e.printStackTrace();
            throw new CommonException("文件上传合并失败，请稍后重试");
        } finally {
            if (null != out) {
                try {
                    out.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
        // Statistics - Log+Data Size
        fileMappingManage().transit(FILE_UPLOAD, spaceId(path), path, false, false, ELFINDER, uploadFile.getFileTotal(), operator());
        spaceStatisticConfig().dataFlow("web", uploadFile.getFileTotal(), spaceId(path), true);
        logMessage(FILE_UPLOAD, path, null, false, ELFINDER);
    }

    @Override
    public void stateUpload(Path fromTarget, long data) {
        dataInStatistic(spaceId(fromTarget), "web", data);
    }

    @Override
    public void stateDown(String spaceId, long data, String type) {
        spaceStatisticConfig().dataOut(type, data, spaceId);
    }

    /**
     * Gets children paths from the give directory (excluding hidden files).
     *
     * @param dir path to the directory.
     * @return the children paths from the give directory.
     */
    @Override
    public List<Path> listChildrenNotHidden(Path dir) throws IOException {
        DirectoryStream.Filter<Path> filter = path -> !Files.isHidden(path);
        if (FileUtils.isFolder(dir)) {
            List<Path> list = new ArrayList<>();
            try (DirectoryStream<Path> directoryStream = (filter != null ? Files.newDirectoryStream(dir, filter) : Files.newDirectoryStream(dir))) {
                for (Path p : directoryStream) {
                    list.add(p);
                }
            }
            return Collections.unmodifiableList(list);
        }
        return Collections.emptyList();
    }

    /**
     * Searches a given path to get the given target.
     *
     * @param path   the path to be search.
     * @param target the target.
     * @return a list of the found paths that contains the target string.
     */
    @Override
    public List<Path> search(Path path, String target) throws IOException {
        log.info("---- web 文件检索 search ----");
        return search(path, target, FileTreeSearch.MatchMode.ANYWHERE, true);
    }

    /**
     * Searches a given path to get the given target.
     *
     * @param path       the path to be search.
     * @param target     the target.
     * @param mode       the match mode constraint (EXACT, ANYWHERE).
     * @param ignoreCase the flag that indicates if is to make a ignore case search.
     * @return a list of the found paths that contains the target string.
     */
    public List<Path> search(Path path, String target, FileTreeSearch.MatchMode mode, boolean ignoreCase) throws IOException {
        if (FileUtils.isFolder(path)) {
            FileTreeSearch fileTreeSearch = new FileTreeSearch(target, mode, ignoreCase);
            Files.walkFileTree(path, fileTreeSearch);
            List<Path> paths = fileTreeSearch.getFoundPaths();
            return Collections.unmodifiableList(paths);
        }
        throw new IllegalArgumentException("The provided path is not a directory");
    }

    /**
     * File Tree Search Visitor Inner Class Implementation.
     */
    private static class FileTreeSearch extends SimpleFileVisitor<Path> {

        enum MatchMode {

            EXACT, ANYWHERE
        }

        private final String query;

        private final FileTreeSearch.MatchMode mode;

        private final boolean ignoreCase;

        private final List<Path> foundPaths;

        FileTreeSearch(String query, FileTreeSearch.MatchMode mode, boolean ignoreCase) {
            this.query = query;
            this.mode = mode;
            this.ignoreCase = ignoreCase;
            this.foundPaths = new ArrayList<>();
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
            Objects.requireNonNull(dir);
            if (exc == null) {
                search(dir);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            super.visitFile(file, attrs);
            search(file);
            return FileVisitResult.CONTINUE;
        }

        private void search(Path path) {
            if (path != null && path.getFileName() != null) {
                String fileName = path.getFileName().toString();
                boolean found;
                switch(mode) {
                    case EXACT:
                        if (ignoreCase) {
                            found = fileName.equalsIgnoreCase(query);
                        } else {
                            found = fileName.equals(query);
                        }
                        break;
                    case ANYWHERE:
                        if (ignoreCase) {
                            found = fileName.toLowerCase().contains(query.toLowerCase());
                        } else {
                            found = fileName.contains(query);
                        }
                        break;
                    default:
                        // NOP - This Should Never Happen
                        throw new AssertionError();
                }
                if (found) {
                    foundPaths.add(path);
                }
            }
        }

        List<Path> getFoundPaths() {
            return Collections.unmodifiableList(foundPaths);
        }
    }

    private void createAndCopyFile(VolumeHandler src, VolumeHandler dst) throws IOException {
        Volume volume = dst.getVolume();
        Path path = volume.fromTarget(dst.getTarget());
        if (SpaceSizeControl.validation(spaceId(path))) {
            // Space capacity limitations
            throw new RuntimeException(messageInternational("FILE_SIZE_FULL"));
        }
        // create a file
        Files.createFile(path);
        InputStream is = null;
        OutputStream os = null;
        try {
            is = src.openInputStream();
            os = dst.openOutputStream();
            IOUtils.copy(is, os);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            if (null != is) {
                is.close();
            }
            if (null != os) {
                os.close();
            }
        }
        String spaceId = spaceId(path);
        dataInStatistic(spaceId, "web-copy", path.toFile().length());
    }

    private void createAndCopyFolder(VolumeHandler src, VolumeHandler dst) throws IOException {
        Volume volume = dst.getVolume();
        Path path = volume.fromTarget(dst.getTarget());
        Files.createDirectories(path);
        for (VolumeHandler c : src.listChildren()) {
            if (c.isFolder()) {
                createAndCopyFolder(c, new VolumeHandler(dst, c.getName()));
            } else {
                createAndCopyFile(c, new VolumeHandler(dst, c.getName()));
            }
        }
    }

    @Override
    public long getSizeBytes(Path path, boolean recursive) throws IOException {
        if (FileUtils.isFolder(path) && recursive) {
            FileTreeSize fileTreeSize = new FileTreeSize();
            Files.walkFileTree(path, fileTreeSize);
            return fileTreeSize.getTotalSize();
        }
        return Files.size(path);
    }

    /**
     * File Tree Size Visitor Inner Class Implementation.
     */
    private class FileTreeSize extends SimpleFileVisitor<Path> {

        private long totalSize;

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
            totalSize += attrs.size();
            return FileVisitResult.CONTINUE;
        }

        long getTotalSize() {
            return totalSize;
        }
    }

    /*  ftp */
    @Override
    public File getRoot() {
        return rootDir;
    }

    @Override
    public String getPath(File file) {
        return rootDir.toURI().relativize(file.toURI()).getPath();
    }

    @Override
    public boolean exists(File file) {
        return file.exists();
    }

    @Override
    public boolean isDirectory(File file) {
        return file.isDirectory();
    }

    @Override
    public int getPermissions(File file) {
        int perms = 0;
        perms = Utils.setPermission(perms, Utils.CAT_OWNER + Utils.TYPE_READ, file.canRead());
        perms = Utils.setPermission(perms, Utils.CAT_OWNER + Utils.TYPE_WRITE, file.canWrite());
        perms = Utils.setPermission(perms, Utils.CAT_OWNER + Utils.TYPE_EXECUTE, file.canExecute());
        return perms;
    }

    @Override
    public long getSize(File file) {
        return file.length();
    }

    @Override
    public long getLastModified(File file) {
        return file.lastModified();
    }

    @Override
    public int getHardLinks(File file) {
        return file.isDirectory() ? 3 : 1;
    }

    @Override
    public String getName(File file) {
        return file.getName();
    }

    @Override
    public String getOwner(File file) {
        return "-";
    }

    @Override
    public String getGroup(File file) {
        return "-";
    }

    @Override
    public File getParent(File file) throws IOException {
        if (file.equals(rootDir)) {
            throw new FileNotFoundException("No permission to access this file");
        }
        return file.getParentFile();
    }

    @Override
    public File[] listFiles(File dir) throws IOException {
        if (!dir.isDirectory())
            throw new IOException("Not a directory");
        return dir.listFiles();
    }

    @Override
    public File findFile(String path) throws IOException {
        File file = new File(rootDir, path);
        if (!isInside(rootDir, file)) {
            throw new FileNotFoundException("No permission to access this file");
        }
        return file;
    }

    @Override
    public File findFile(File cwd, String path) throws IOException {
        File file = new File(cwd, path);
        if (!isInside(rootDir, file)) {
            throw new FileNotFoundException("No permission to access this file");
        }
        return file;
    }

    @Override
    public InputStream readFile(File file, long start) throws IOException {
        // Not really needed, but helps a bit in performance
        if (start <= 0) {
            return new FileInputStream(file);
        }
        // Use RandomAccessFile to seek a file
        final RandomAccessFile raf = new RandomAccessFile(file, "r");
        raf.seek(start);
        // Create a stream using the RandomAccessFile
        return new FileInputStream(raf.getFD()) {

            @Override
            public void close() throws IOException {
                super.close();
                raf.close();
            }
        };
    }

    @Override
    public OutputStream writeFile(File file, long start) throws IOException {
        // Not really needed, but helps a bit in performance
        if (start <= 0) {
            return new FileOutputStream(file, false);
        } else if (start == file.length()) {
            return new FileOutputStream(file, true);
        }
        // Use RandomAccessFile to seek a file
        final RandomAccessFile raf = new RandomAccessFile(file, "rw");
        raf.seek(start);
        // Create a stream using the RandomAccessFile
        return new FileOutputStream(raf.getFD()) {

            @Override
            public void close() throws IOException {
                super.close();
                raf.close();
            }
        };
    }

    @Override
    public void chmod(File file, int perms) throws IOException {
        boolean read = Utils.hasPermission(perms, Utils.CAT_OWNER + Utils.TYPE_READ);
        boolean write = Utils.hasPermission(perms, Utils.CAT_OWNER + Utils.TYPE_WRITE);
        boolean execute = Utils.hasPermission(perms, Utils.CAT_OWNER + Utils.TYPE_EXECUTE);
        if (!file.setReadable(read, true))
            throw new IOException("Couldn't update the readable permission");
        if (!file.setWritable(write, true))
            throw new IOException("Couldn't update the writable permission");
        if (!file.setExecutable(execute, true))
            throw new IOException("Couldn't update the executable permission");
    }

    @Override
    public void touch(File file, long time) throws IOException {
        if (!file.setLastModified(time))
            throw new IOException("Couldn't touch the file");
    }

    private boolean isInside(File dir, File file) {
        if (file.equals(dir))
            return true;
        try {
            return file.getCanonicalPath().startsWith(dir.getCanonicalPath() + File.separator);
        } catch (IOException ex) {
            return false;
        }
    }
}
