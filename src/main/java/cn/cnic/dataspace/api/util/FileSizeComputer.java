package cn.cnic.dataspace.api.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

/**
 * computing file or folder size with forkJoin
 *
 * @author wangCc
 * @date 2021-4-27 17:15:17
 */
public final class FileSizeComputer extends RecursiveTask<Long> {

    public final static ForkJoinPool FORK_JOIN_POOL = new ForkJoinPool();

    private final File filePaths;

    public FileSizeComputer(File filePath) {
        this.filePaths = filePath;
    }

    @Override
    public Long compute() {
        long size = 0;
        final File file = filePaths;
        File[] children = file.listFiles();
        if (children != null) {
            List<ForkJoinTask<Long>> tasks = new ArrayList<>();
            for (final File child : children) {
                if ((!child.getName().contains(".svn"))) {
                    if (!child.isDirectory()) {
                        size += child.length();
                    } else {
                        tasks.add(new FileSizeComputer(child));
                    }
                }
            }
            for (final ForkJoinTask<Long> task : invokeAll(tasks)) {
                size += task.join();
            }
        } else {
            size += file.length();
        }
        return size;
    }
}
