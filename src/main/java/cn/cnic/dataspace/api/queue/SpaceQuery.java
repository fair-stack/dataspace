package cn.cnic.dataspace.api.queue;

import cn.cnic.dataspace.api.model.harvest.MiningTask;
import cn.cnic.dataspace.api.queue.backup.FileSendTreadPool;
import cn.cnic.dataspace.api.queue.space.SpaceTaskUtils;
import cn.cnic.dataspace.api.queue.space.SpaceTreadPool;
import lombok.Data;
import org.springframework.data.mongodb.core.MongoTemplate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class SpaceQuery {

    private static SpaceQuery spaceQuery = null;

    private static ConcurrentHashMap<String, LinkedBlockingQueue<SpaceTask>> userCacheMap = new ConcurrentHashMap<String, LinkedBlockingQueue<SpaceTask>>();

    private static ConcurrentHashMap<String, LinkedBlockingQueue<FileSend>> backupCacheMap = new ConcurrentHashMap<String, LinkedBlockingQueue<FileSend>>();

    private SpaceQuery() {
    }

    public synchronized static SpaceQuery getInstance() {
        if (spaceQuery == null) {
            spaceQuery = new SpaceQuery();
        }
        return spaceQuery;
    }

    public static int getTaskCount() {
        return userCacheMap.size();
    }

    public void addCache(String userId, MiningTask user, MongoTemplate mongoTemplate, SpaceTaskUtils spaceTaskUtils) {
        LinkedBlockingQueue<SpaceTask> linkList = userCacheMap.get(userId);
        if (linkList == null) {
            linkList = new LinkedBlockingQueue<SpaceTask>();
            linkList.offer(new SpaceTask(user, spaceTaskUtils));
            userCacheMap.put(userId, linkList);
            SpaceTreadPool.getInstance(mongoTemplate).execute(userId);
        } else {
            linkList.offer(new SpaceTask(user, spaceTaskUtils));
        }
    }

    public void addFileSendCache(String userId, Map<String, Object> paramMap, String type, MongoTemplate mongoTemplate) {
        LinkedBlockingQueue<FileSend> linkList = backupCacheMap.get(userId);
        if (linkList == null) {
            linkList = new LinkedBlockingQueue<FileSend>();
            linkList.offer(new FileSend(paramMap, type));
            backupCacheMap.put(userId, linkList);
            FileSendTreadPool.getInstance(mongoTemplate).execute(userId);
        } else {
            linkList.offer(new FileSend(paramMap, type));
        }
    }

    public SpaceTask getNextElement(String userId) {
        LinkedBlockingQueue<SpaceTask> linkList = userCacheMap.get(userId);
        SpaceTask user = null;
        if (linkList != null) {
            user = linkList.poll();
        }
        if (user == null) {
            userCacheMap.remove(userId);
        }
        return user;
    }

    public FileSend getFileSendElement(String userId) {
        LinkedBlockingQueue<FileSend> linkList = backupCacheMap.get(userId);
        FileSend user = null;
        if (linkList != null) {
            user = linkList.poll();
        }
        if (user == null) {
            backupCacheMap.remove(userId);
        }
        return user;
    }

    @Data
    public class SpaceTask {

        private MiningTask miningTask;

        private SpaceTaskUtils spaceTaskUtils;

        public SpaceTask(MiningTask miningTask, SpaceTaskUtils spaceTaskUtils) {
            this.miningTask = miningTask;
            this.spaceTaskUtils = spaceTaskUtils;
        }
    }

    @Data
    public class FileSend {

        private Map<String, Object> paramMap;

        private String type;

        public FileSend(Map<String, Object> paramMap, String type) {
            this.paramMap = paramMap;
            this.type = type;
        }
    }
}
