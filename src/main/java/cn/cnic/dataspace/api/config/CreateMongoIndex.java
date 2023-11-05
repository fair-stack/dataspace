package cn.cnic.dataspace.api.config;

import cn.cnic.dataspace.api.model.space.Space;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Create an index for the mongo database
 */
@Repository
@Slf4j
public class CreateMongoIndex {

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * Create a single or federated index
     */
    public boolean createInboxIndex(String collectionName, String... index_key) {
        boolean success = true;
        try {
            Index index = new Index();
            for (int i = 0; i < index_key.length; i++) {
                index.on(index_key[i], Sort.Direction.ASC);
            }
            mongoTemplate.indexOps(collectionName).ensureIndex(index);
        } catch (Exception ex) {
            success = false;
        }
        return success;
    }

    /**
     * Get existing index collection
     */
    public List<IndexInfo> getInboxIndex(String collectionName) {
        List<IndexInfo> indexInfoList = mongoTemplate.indexOps(collectionName).getIndexInfo();
        return indexInfoList;
    }

    /**
     * Delete Index
     */
    public boolean deleteInboxIndex(String indexName, String collectionName) {
        boolean success = true;
        try {
            mongoTemplate.indexOps(collectionName).dropIndex(indexName);
        } catch (Exception ex) {
            success = false;
        }
        return success;
    }

    /**
     * Obtain the name of the dataset in mongo
     */
    public Set<String> getNames() {
        Set<String> res = mongoTemplate.getCollectionNames();
        return res;
    }

    private void createIndex(String collection, String[] index) {
        List<IndexInfo> indexInfoList = getInboxIndex(collection);
        List<String> list = new ArrayList<>(indexInfoList.size());
        for (IndexInfo indexInfo : indexInfoList) {
            String name = indexInfo.getName();
            list.add(name.substring(0, name.indexOf("_")));
        }
        for (String in : index) {
            if (!list.contains(in)) {
                createInboxIndex(collection, in);
                log.info("mongodb  create " + collection + " index {}  创建完成!  ------");
            }
        }
    }

    public void createIndex() {
        // release
        String resourceColl = "resource_v2";
        String[] resourceIndex = { "resourceId", "version", "traceId", "type" };
        createIndex(resourceColl, resourceIndex);
        // space
        String spaceColl = "space";
        String[] spaceIndex = { "homeUrl", "spaceShort", "userId", "state" };
        createIndex(spaceColl, spaceIndex);
        // Space Log
        String spaceLogColl = "space";
        String[] spaceLogIndex = { "spaceId", "action", "createTime", "description" };
        createIndex(spaceLogColl, spaceLogIndex);
    }

    public void createSpaceFileMappingIndex() {
        String[] mappingIndex = { "fId", "hash", "fHash", "type", "path", "createTime" };
        List<Space> spaceList = mongoTemplate.find(new Query().addCriteria(Criteria.where("state").is("1")), Space.class);
        for (Space space : spaceList) {
            String spaceId = space.getSpaceId();
            createIndex(spaceId, mappingIndex);
        }
    }

    public void createSpaceFileMappingIndex(String spaceId) {
        String[] mappingIndex = { "fId", "hash", "fHash", "type", "path", "createTime" };
        createIndex(spaceId, mappingIndex);
    }
}
