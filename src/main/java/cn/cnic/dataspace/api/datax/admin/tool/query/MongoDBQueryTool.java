package cn.cnic.dataspace.api.datax.admin.tool.query;

import cn.cnic.dataspace.api.datax.admin.core.util.LocalCacheUtil;
import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.tool.database.ColumnInfo;
import cn.cnic.dataspace.api.datax.admin.tool.database.TableInfo;
import cn.cnic.dataspace.api.datax.admin.util.AESUtil;
import cn.cnic.dataspace.api.datax.admin.util.DataXException;
import cn.cnic.dataspace.api.datax.admin.util.MongoDBReaderErrorCode;
import cn.cnic.dataspace.api.datax.core.util.Constants;
import com.alibaba.fastjson.JSON;
import com.mongodb.*;
import com.mongodb.MongoClient;
import com.mongodb.client.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class MongoDBQueryTool {

    private static MongoClient connection = null;

    private static MongoDatabase collections;

    public MongoDBQueryTool(JobDatasource jobDatasource) {
        if (LocalCacheUtil.get(jobDatasource.getCacheKey()) == null) {
            getDataSource(jobDatasource);
        } else {
            connection = (MongoClient) LocalCacheUtil.get(jobDatasource.getCacheKey());
            if (connection == null) {
                LocalCacheUtil.remove(jobDatasource.getCacheKey());
                getDataSource(jobDatasource);
            }
        }
        LocalCacheUtil.set(jobDatasource.getCacheKey(), connection, 4 * 60 * 60 * 1000);
    }

    private void getDataSource(JobDatasource jobDatasource) {
        if (jobDatasource.getJdbcUrl().startsWith("mongodb://")) {
            // mongodb://[username]:[password]
            try {
                connection = new MongoClient(new MongoClientURI(jobDatasource.getJdbcUrl()));
                // There are no exceptions when creating a connection, only when using it
                getCollectionNames(jobDatasource.getDatabaseName());
            } catch (NumberFormatException e) {
                throw DataXException.asDataXException(MongoDBReaderErrorCode.ILLEGAL_VALUE, "不合法参数");
            } catch (Exception e) {
                // This directly returns e.getMessage
                throw DataXException.asDataXException(MongoDBReaderErrorCode.UNEXCEPT_EXCEPTION, e.getMessage());
            }
        } else {
            // [host]:[port]
            if (!isHostPortPattern(jobDatasource.getJdbcUrl())) {
                throw DataXException.asDataXException(MongoDBReaderErrorCode.ILLEGAL_VALUE, "不合法参数");
            }
            try {
                MongoCredential credential = MongoCredential.createCredential(AESUtil.decrypt(jobDatasource.getJdbcUsername()), jobDatasource.getDatabaseName(), AESUtil.decrypt(jobDatasource.getJdbcPassword()).toCharArray());
                connection = new MongoClient(parseServerAddress(jobDatasource.getJdbcUrl()), Arrays.asList(credential));
                // There are no exceptions when creating a connection, only when using it
                getCollectionNames(jobDatasource.getDatabaseName());
            } catch (UnknownHostException e) {
                throw DataXException.asDataXException(MongoDBReaderErrorCode.ILLEGAL_ADDRESS, "不合法的地址");
            } catch (NumberFormatException e) {
                throw DataXException.asDataXException(MongoDBReaderErrorCode.ILLEGAL_VALUE, "不合法参数");
            } catch (Exception e) {
                // This directly returns e.getMessage
                throw DataXException.asDataXException(MongoDBReaderErrorCode.UNEXCEPT_EXCEPTION, e.getMessage());
            }
        }
        collections = connection.getDatabase(jobDatasource.getDatabaseName());
    }

    // Close Connection
    public static void sourceClose() {
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * Obtain a list of DB names
     */
    public List<String> getDBNames() {
        MongoIterable<String> dbs = connection.listDatabaseNames();
        List<String> dbNames = new ArrayList<>();
        dbs.forEach((Block<? super String>) dbNames::add);
        return dbNames;
    }

    /**
     * Query based on query
     */
    public String filterQuery(String collectionName, Map<String, Object> queryMap) {
        Document queryFilter = Document.parse(JSON.toJSONString(queryMap));
        Document filter = new Document();
        filter = new Document("$and", Arrays.asList(filter, queryFilter));
        MongoCollection<Document> spaceSvn = collections.getCollection(collectionName);
        Document first = spaceSvn.find(filter).first();
        if (first == null) {
            return "";
        }
        return first.toJson();
    }

    /**
     * Test if the connection was successful
     */
    public boolean dataSourceTest(String dbName) {
        collections = connection.getDatabase(dbName);
        return collections.listCollectionNames().iterator().hasNext();
    }

    /**
     * Obtain a list of Collection names
     */
    public List<String> getCollectionNames(String dbName) {
        collections = connection.getDatabase(dbName);
        List<String> collectionNames = new ArrayList<>();
        collections.listCollectionNames().forEach((Block<? super String>) collectionNames::add);
        return collectionNames;
    }

    /**
     * Query columns through CollectionName
     */
    public List<String> getColumns(String collectionName) {
        MongoCollection<Document> collection = collections.getCollection(collectionName);
        Document document = collection.find(new BasicDBObject()).first();
        List<String> list = new ArrayList<>();
        if (null == document || document.size() <= 0) {
            return list;
        }
        document.forEach((k, v) -> {
            if (null != v) {
                String type = v.getClass().getSimpleName();
                list.add(k + ":" + type);
            }
            /*if ("Document".equals(type)) {
        ((Document) v).forEach((k1, v1) -> {
          String simpleName = v1.getClass().getSimpleName();
        });
      } */
        });
        return list;
    }

    /**
     * Building tableInfo
     */
    public TableInfo buildTableInfo(String collectionName) {
        List<String> columns = getColumns(collectionName);
        TableInfo tableInfo = new TableInfo();
        tableInfo.setName(collectionName);
        List<ColumnInfo> cols = columns.stream().map(var -> {
            ColumnInfo col = new ColumnInfo();
            col.setName(var.split(Constants.SPLIT_SCOLON)[0]);
            col.setType(var.split(Constants.SPLIT_SCOLON)[1]);
            return col;
        }).collect(Collectors.toList());
        tableInfo.setColumns(cols);
        return tableInfo;
    }

    /**
     * Query columns through CollectionName
     */
    public long getDataCount(String collectionName) {
        MongoCollection<Document> collection = collections.getCollection(collectionName);
        long l = collection.countDocuments();
        return l;
    }

    public List<Map<String, Object>> getDatas(String collectionName) {
        List<Map<String, Object>> datas = new ArrayList<>();
        try {
            FindIterable<Document> iterable = collections.getCollection(collectionName).find(new BasicDBObject()).limit(10);
            iterable.forEach(new Consumer<Document>() {

                @Override
                public void accept(Document document) {
                    datas.add(JSON.parseObject(document.toJson()));
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return datas;
    }

    /**
     * Determine if the address type meets the requirements
     */
    private static boolean isHostPortPattern(String addressStr) {
        for (String address : Arrays.asList(addressStr.split(","))) {
            String regex = "(\\S+):([0-9]+)";
            if (!((String) address).matches(regex)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Convert to Mongo Address Protocol
     */
    private static List<ServerAddress> parseServerAddress(String rawAddress) throws UnknownHostException {
        List<ServerAddress> addressList = new ArrayList<>();
        for (String address : Arrays.asList(rawAddress.split(","))) {
            String[] tempAddress = address.split(":");
            try {
                ServerAddress sa = new ServerAddress(tempAddress[0], Integer.valueOf(tempAddress[1]));
                addressList.add(sa);
            } catch (Exception e) {
                throw new UnknownHostException();
            }
        }
        return addressList;
    }
}
