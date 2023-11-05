package cn.cnic.dataspace.api.util;

import cn.cnic.dataspace.api.config.space.FileOperationFactory;
import cn.cnic.dataspace.api.model.space.Space;
import java.io.File;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

/**
 * find file and folder with forkJoin
 *
 * @author wangCc
 * @date 2021-11-16 10:46:40
 */
public final class FileFinder extends RecursiveTask<Map<String, Object>> {

    public final static ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool();

    public final static Map<String, Space> SPACE_NAME_MAP = new LinkedHashMapCache(1024);

    private final File[] filePaths;

    private final String content;

    public FileFinder(String content, File... filePaths) {
        this.content = content;
        this.filePaths = filePaths;
    }

    @Override
    public Map<String, Object> compute() {
        Map<String, Object> map = new HashMap<>(16);
        Set<Map<String, Object>> fileList = new HashSet<>();
        Set<Map<String, Object>> folderList = new HashSet<>();
        for (File filePath : filePaths) {
            File[] children = filePath.listFiles();
            if (children != null) {
                List<ForkJoinTask<Map<String, Object>>> tasks = new ArrayList<>();
                for (final File child : children) {
                    if ((!child.getName().contains(".svn")) && child.toString().split("/").length > 3) {
                        // String spaceId = child.toString().split("/")[4];
                        String spaceId = getSpaceId(child.getPath());
                        if (child.getName().contains(content)) {
                            Map<String, Object> fileMap = new HashMap<>(16);
                            fileMap.put("name", child.getName());
                            fileMap.put("spaceId", spaceId);
                            fileMap.put("spaceName", getSpace(spaceId).getSpaceName());
                            fileMap.put("public", getSpace(spaceId).getIsPublic());
                            fileMap.put("homeUrl", getSpace(spaceId).getHomeUrl());
                            if (!child.isDirectory()) {
                                fileList.add(fileMap);
                            } else {
                                folderList.add(fileMap);
                                tasks.add(new FileFinder(child.toString()));
                            }
                        }
                    }
                }
                for (final ForkJoinTask<Map<String, Object>> task : invokeAll(tasks)) {
                    task.join();
                }
            }
        }
        map.put("file", fileList);
        map.put("folder", folderList);
        return map;
    }

    /**
     * get spaceName by spaceId
     */
    private Space getSpace(String spaceId) {
        if (SPACE_NAME_MAP.containsKey(spaceId)) {
            return SPACE_NAME_MAP.get(spaceId);
        } else {
            Space space = FileOperationFactory.getSpaceRepository().findById(spaceId).get();
            SPACE_NAME_MAP.put(spaceId, space);
            return space;
        }
    }

    private String getSpaceId(String path) {
        String rootDir = FileOperationFactory.getSpaceUrl().getRootDir();
        String replace = path.replaceAll(rootDir, "");
        String[] pathSplit = replace.split(CommonUtils.FILE_SPLIT);
        return pathSplit.length >= 3 ? pathSplit[2] : "";
    }

    /**
     * linked hash map for space name cache with LRU
     */
    private static class LinkedHashMapCache extends LinkedHashMap<String, Space> {

        private final int capacity;

        public LinkedHashMapCache(int capacity) {
            this.capacity = capacity;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Space> eldest) {
            return size() > this.capacity;
        }
    }
}
