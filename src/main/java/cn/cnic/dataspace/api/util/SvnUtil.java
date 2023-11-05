package cn.cnic.dataspace.api.util;

import cn.cnic.dataspace.api.config.space.FileOperationFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static cn.cnic.dataspace.api.util.CommonUtils.messageInternational;

/**
 * SvnUtil
 *
 * @author wangCc
 * @date 2021-04-12 10:17
 */
@Slf4j
public final class SvnUtil {

    private static final String ACCOUNT = "svnUser";

    private static final String PASSWORD = "harryssecret";

    private static final String SVN_PREFIX = "svn://";

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);

    public static final String EXAMPLE = FileOperationFactory.getSpaceUrl().getRootDir() + "/6062cf4d0662c663bcb7fe4b/1387680764514119680";

    /**
     * authentication manager
     */
    private static ISVNAuthenticationManager authenticationManager() {
        return SVNWCUtil.createDefaultAuthenticationManager(SvnUtil.ACCOUNT, SvnUtil.PASSWORD.toCharArray());
    }

    /**
     * svnClientManager
     */
    private static SVNClientManager svnClientManager() {
        DAVRepositoryFactory.setup();
        return SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), ACCOUNT, PASSWORD);
    }

    /**
     * create a svn repository
     *
     * @param svnHost     svn host
     * @param spaceId     spaceId
     * @param svnRepoPath svn repo path for snvRepository  :/data/svn/spaceId
     * @param spacePath   path of space
     */
    public static long createRepo(String svnHost, String spaceId, String svnRepoPath, String spacePath) throws SVNException {
        ISVNAuthenticationManager authenticationManager = authenticationManager();
        SVNRepository repository = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), authenticationManager).createRepository(SVNRepositoryFactory.createLocalRepository(new File(svnRepoPath), true, false), false);
        repository.setAuthenticationManager(authenticationManager);
        long latestRevision = repository.getLatestRevision();
        updateSvnConf(svnRepoPath);
        asyncCheckout(SVN_PREFIX + svnHost + "/" + spaceId, spacePath, String.valueOf(latestRevision));
        return latestRevision;
    }

    /**
     * update svn repository conf about pwd and authorization
     */
    @SneakyThrows
    private static void updateSvnConf(String svnRepoPath) {
        // /data/svn/1380818097539178496/conf
        deleteAndCopyFile(svnRepoPath + "/conf/authz", new ClassPathResource("/static/authz").getInputStream());
        deleteAndCopyFile(svnRepoPath + "/conf/passwd", new ClassPathResource("/static/passwd").getInputStream());
        deleteAndCopyFile(svnRepoPath + "/conf/svnserve.conf", new ClassPathResource("/static/svnserve.conf").getInputStream());
        log.info("updateSvnConf svn repository conf has replaced completed ...");
    }

    /**
     * delete and copy file for new svn repository
     */
    @SneakyThrows
    private static void deleteAndCopyFile(String destination, InputStream inputStream) {
        File file = new File(destination);
        if (file.exists()) {
            file.delete();
        }
        int index;
        byte[] bytes = new byte[1024];
        FileOutputStream downloadFile = new FileOutputStream(destination);
        while ((index = inputStream.read(bytes)) != -1) {
            downloadFile.write(bytes, 0, index);
            downloadFile.flush();
        }
        inputStream.close();
        downloadFile.close();
    }

    /**
     * init space dir for new svn repository
     */
    private static void asyncCheckout(String svnPath, String spacePath, String version) {
        EXECUTOR.submit(() -> {
            try {
                Thread.sleep(3000);
                log.info("start to check out from svn repository ....");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            log.info("----svn path " + svnPath);
            log.info("----spacePath path " + spacePath);
            checkout(svnPath, spacePath, String.valueOf(version));
        });
    }

    /**
     * synchronizing directory by add or update operation
     *
     * @param spacePath /data/disk/userId/spaceId/
     */
    public static void addFiles(String spacePath) {
        try {
            svnClientManager().getWCClient().doAdd(new File(spacePath), true, true, false, SVNDepth.INFINITY, false, false, true);
        } catch (SVNException e) {
            if (e.getMessage().startsWith("svn: E155004")) {
                try {
                    svnClientManager().getWCClient().doUnlock(new File[] { new File(spacePath) }, true);
                    update(spacePath);
                } catch (SVNException ex) {
                    throw new RuntimeException(messageInternational("INTERNAL_ERROR"));
                }
            }
        }
    }

    /**
     * undo add file
     *
     * @param filePath specific file path
     */
    public static void undoAdd(String filePath) {
        try {
            svnClientManager().getWCClient().doRevert(new File[] { new File(filePath) }, SVNDepth.INFINITY, new ArrayList<>());
        } catch (SVNException e) {
            e.printStackTrace();
        }
    }

    /**
     * svn directory push
     *
     * @param wcPath svn repository path for data space
     */
    public static Long svnPush(String wcPath, String description) {
        long newRevision;
        try {
            newRevision = svnClientManager().getCommitClient().doCommit(new File[] { new File(wcPath) }, false, description, null, null, false, false, SVNDepth.INFINITY).getNewRevision();
        } catch (SVNException e) {
            e.printStackTrace();
            if (e.getMessage().startsWith("svn: E155015")) {
                // resolveConflict(wcPath);
                undoAdd(e.getMessage().split("'")[1]);
                newRevision = svnPush(wcPath, description);
                return newRevision;
            } else if (e.getMessage().startsWith("svn: E155010")) {
                undoAdd(e.getMessage().split("'")[1]);
                newRevision = svnPush(wcPath, description);
                return newRevision;
            }
            throw new RuntimeException(messageInternational("INTERNAL_ERROR"));
        }
        return newRevision;
    }

    public static long svnPush(String description, List<File> files) {
        File[] fileArray = new File[files.size()];
        for (int i = 0; i < files.size(); i++) {
            fileArray[i] = files.get(i);
        }
        try {
            return svnClientManager().getCommitClient().doCommit(fileArray, false, description, null, null, false, false, SVNDepth.INFINITY).getNewRevision();
        } catch (SVNException e) {
            e.printStackTrace();
            throw new RuntimeException(messageInternational("INTERNAL_ERROR"));
        }
    }

    /**
     * prePublish file changelist
     */
    public static List<Map<String, Object>> changeList(String filePath) {
        List<Map<String, Object>> list = new ArrayList<>();
        File file = new File(filePath);
        try {
            svnClientManager().getDiffClient().doDiffStatus(file, SVNRevision.WORKING, file, SVNRevision.HEAD, SVNDepth.INFINITY, false, diffStatus -> {
                Map<String, Object> map = new HashMap<>(8);
                /*System.out.println("diffStatus.getModificationType().toString() = " + diffStatus.getModificationType().toString());
                        System.out.println("diffStatus.getFile().getName() = " + diffStatus.getFile().getName());
                        System.out.println("diffStatus.getPath() = " + diffStatus.getPath());
                        System.out.println("diffStatus.getKind().toString() = " + diffStatus.getKind().toString());*/
                map.put("method", diffStatus.getModificationType().toString());
                map.put("file", diffStatus.getFile().getName());
                map.put("path", "/" + diffStatus.getPath().replace(diffStatus.getFile().getName(), ""));
                map.put("type", diffStatus.getKind().toString());
                list.add(map);
            });
        } catch (SVNException e) {
            e.printStackTrace();
            throw new RuntimeException(messageInternational("INTERNAL_ERROR"));
        }
        return list;
    }

    /**
     * influenced file svn log
     */
    public static List<Map<String, Object>> svnInfluencedLog(String filePath, String revertVersion) {
        File file = new File(filePath);
        final List<Map<String, Object>> list = new ArrayList<>();
        try {
            svnClientManager().getDiffClient().doDiffStatus(file, SVNRevision.WORKING, file, SVNRevision.parse(revertVersion), SVNDepth.INFINITY, false, diffStatus -> {
                Map<String, Object> map = new HashMap<>(8);
                map.put("method", diffStatus.getModificationType().toString());
                map.put("change", "/" + diffStatus.getPath());
                map.put("file", diffStatus.getFile().getName());
                map.put("path", "/" + diffStatus.getPath().replace(diffStatus.getFile().getName(), ""));
                list.add(map);
            });
        } catch (SVNException e) {
            e.printStackTrace();
            throw new RuntimeException(messageInternational("INTERNAL_ERROR"));
        }
        return list;
    }

    /**
     * delete specific file
     *
     * @param filePath file detail path: /data/disk/userId/spaceId/xx
     */
    public static boolean delete(String filePath) {
        boolean flag;
        try {
            svnClientManager().getWCClient().doDelete(new File(filePath), true, true, false);
            flag = true;
        } catch (SVNException e) {
            flag = new File(filePath).delete();
        }
        return flag;
    }

    /**
     * clean up svn repository
     *
     * @param workspace dataSpace file path
     */
    private static void cleanUp(String workspace) {
        try {
            svnClientManager().getWCClient().doCleanup(new File(workspace));
        } catch (SVNException e) {
            e.printStackTrace();
            throw new RuntimeException(messageInternational("INTERNAL_ERROR"));
        }
    }

    /**
     * resolve conflict svn repository
     *
     * @param workspace dataSpace file path
     */
    private static void resolveConflict(String workspace) {
        try {
            svnClientManager().getWCClient().doResolve(new File(workspace), SVNDepth.fromRecurse(true), SVNConflictChoice.MINE_CONFLICT);
        } catch (SVNException e) {
            e.printStackTrace();
            throw new RuntimeException(messageInternational("INTERNAL_ERROR"));
        }
    }

    /**
     * synchronize directory update before commit - sync without new version
     */
    public static long update(String filePath) {
        try {
            return svnClientManager().getUpdateClient().doUpdate(new File(filePath.substring(0, EXAMPLE.length())), SVNRevision.HEAD, SVNDepth.INFINITY, false, false);
        } catch (SVNException e) {
            e.printStackTrace();
            throw new RuntimeException(messageInternational("INTERNAL_ERROR"));
        }
    }

    /**
     * reName method
     */
    public static void reName(String originalFile, String wcPath) {
        SVNClientManager svnClientManager = svnClientManager();
        try {
            svnClientManager.getWCClient().doDelete(new File(originalFile), true, false, false);
            svnClientManager.getWCClient().doAdd(new File[] { new File(wcPath.substring(0, EXAMPLE.length())) }, true, false, false, SVNDepth.INFINITY, false, false, true);
        } catch (SVNException e) {
            e.printStackTrace();
            throw new RuntimeException(messageInternational("INTERNAL_ERROR"));
        }
    }

    /**
     * move method
     */
    public static void move(List<String> originalFiles, String wcPath, boolean deleteFiles) {
        SVNClientManager svnClientManager = svnClientManager();
        try {
            for (String originalFile : originalFiles) {
                svnClientManager.getWCClient().doDelete(new File(originalFile), true, deleteFiles, false);
            }
            svnClientManager.getWCClient().doAdd(new File[] { new File(wcPath.substring(0, EXAMPLE.length())) }, true, false, false, SVNDepth.INFINITY, false, false, true);
        } catch (SVNException e) {
            e.printStackTrace();
            throw new RuntimeException(messageInternational("INTERNAL_ERROR"));
        }
    }

    /**
     * check out files by specific version
     *
     * @param svnPath
     * @param spacePath check out target directory
     */
    private static void checkout(String svnPath, String spacePath, String version) {
        SVNUpdateClient updateClient = svnClientManager().getUpdateClient();
        updateClient.setIgnoreExternals(false);
        File wcDir = new File(spacePath);
        long workingVersion;
        try {
            workingVersion = updateClient.doCheckout(SVNURL.parseURIEncoded(svnPath), wcDir, SVNRevision.HEAD, SVNRevision.parse(version), SVNDepth.INFINITY, false);
        } catch (SVNException e) {
            e.printStackTrace();
            throw new RuntimeException(messageInternational("INTERNAL_ERROR"));
        }
        log.info("check out version " + workingVersion + " to dictionary " + wcDir + " ....");
    }

    /**
     * rollback to a specific reversion
     */
    public static void rollback(String spacePath, String version) {
        SVNUpdateClient updateClient = svnClientManager().getUpdateClient();
        updateClient.setIgnoreExternals(false);
        try {
            updateClient.doUpdate(new File(spacePath), SVNRevision.parse(version), SVNDepth.fromRecurse(true), false, false);
        } catch (SVNException e) {
            e.printStackTrace();
            throw new RuntimeException(messageInternational("INTERNAL_ERROR"));
        }
    }

    /**
     * get un-versioned files
     */
    @SneakyThrows
    public static void unVersionedFiles(String spacePath) {
        SVNStatusClient statusClient = svnClientManager().getStatusClient();
        List<String> list = new ArrayList<>();
        statusClient.doStatus(new File(spacePath), SVNRevision.WORKING, SVNDepth.INFINITY, false, true, false, false, svnStatus -> System.out.println("svnStatus = " + svnStatus.getRepositoryRelativePath()), list);
        for (String s : list) {
            System.out.println("s = " + s);
        }
    }

    /**
     * file revert last version
     */
    public static void revert(String... files) {
        File[] filesList = new File[files.length];
        for (int i = 0; i < files.length; i++) {
            filesList[i] = new File(files[i]);
        }
        try {
            svnClientManager().getWCClient().doRevert(filesList, SVNDepth.INFINITY, new ArrayList<>());
        } catch (SVNException e) {
            if (e.getMessage().startsWith("svn: E155004")) {
                cleanUp(files[0].substring(0, EXAMPLE.length()));
            }
        }
    }

    /**
     * svn difference with two version
     */
    public static String differVersion(String filePath, String compareVersion, String targetVersion) {
        File file = new File(filePath);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            svnClientManager().getDiffClient().doDiff(file, SVNRevision.parse(compareVersion), file, StringUtils.isBlank(targetVersion) ? SVNRevision.WORKING : SVNRevision.parse(targetVersion), SVNDepth.INFINITY, false, out, new ArrayList<>());
        } catch (SVNException e) {
            e.printStackTrace();
            throw new RuntimeException(messageInternational("INTERNAL_ERROR"));
        }
        String result = null;
        try {
            result = out.toString().contains("@") ? out.toString("UTF-8").split("@@")[2] : "暂无变化";
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * file svn log
     */
    public static List<Map<String, Object>> svnLog(String filePath) {
        SVNClientManager svnClientManager = svnClientManager();
        File file = new File(filePath);
        final List<Map<String, Object>> list = new ArrayList<>();
        try {
            svnClientManager.getLogClient().doLog(new File[] { file }, SVNRevision.parse("0"), svnClientManager.getStatusClient().doStatus(file, false).getRevision(), true, false, 100, x -> {
                Map<String, Object> content = new HashMap<>();
                content.put("dateTime", CommonUtils.getDateTimeString(x.getDate()));
                content.put("revision", x.getRevision());
                list.add(content);
            });
        } catch (SVNException e) {
            e.printStackTrace();
            throw new RuntimeException(messageInternational("INTERNAL_ERROR"));
        }
        return list;
    }

    @SneakyThrows
    public static SVNRepository getSVNRepository(String svnHost, String spaceId) {
        try {
            SVNClientManager svnClientManager = SVNClientManager.newInstance(SVNWCUtil.createDefaultOptions(true), SvnUtil.ACCOUNT, SvnUtil.PASSWORD);
            SVNRepository svnRepository = svnClientManager.createRepository(SVNURL.parseURIEncoded(SVN_PREFIX + svnHost + "/" + spaceId), true);
            return svnRepository;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
