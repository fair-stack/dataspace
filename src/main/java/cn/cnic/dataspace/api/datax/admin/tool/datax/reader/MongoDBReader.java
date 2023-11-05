package cn.cnic.dataspace.api.datax.admin.tool.datax.reader;

import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.tool.pojo.DataxMongoDBPojo;
import cn.cnic.dataspace.api.datax.admin.util.AESUtil;
import cn.cnic.dataspace.api.datax.core.util.Constants;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import java.util.Map;

public class MongoDBReader extends BaseReaderPlugin implements DataxReaderInterface {

    @Override
    public String getName() {
        return "mongodbreader";
    }

    @Override
    public Map<String, Object> sample() {
        return null;
    }

    public Map<String, Object> buildMongoDB(DataxMongoDBPojo plugin) {
        // structure
        JobDatasource dataSource = plugin.getJdbcDatasource();
        Map<String, Object> readerObj = Maps.newLinkedHashMap();
        readerObj.put("name", getName());
        Map<String, Object> parameterObj = Maps.newLinkedHashMap();
        String[] addressList = null;
        String str = dataSource.getJdbcUrl().replace(Constants.MONGO_URL_PREFIX, Constants.STRING_BLANK);
        if (str.contains(Constants.SPLIT_AT) && str.contains(Constants.SPLIT_DIVIDE)) {
            // mongodb://[username:password@]host1[:port1][,...hostN[:portN]]][/[database][?options]]
            addressList = str.substring(str.indexOf(Constants.SPLIT_AT) + 1, str.indexOf(Constants.SPLIT_DIVIDE)).split(Constants.SPLIT_COMMA);
        } else if (str.contains(Constants.SPLIT_DIVIDE)) {
            // No account password
            addressList = str.substring(0, str.indexOf(Constants.SPLIT_DIVIDE)).split(Constants.SPLIT_COMMA);
        } else {
            // [host]:[port]
            addressList = new String[] { str };
        }
        String userName = AESUtil.decrypt(dataSource.getJdbcUsername());
        // Determine whether the account secret is ciphertext
        if (userName == null) {
            userName = dataSource.getJdbcUsername();
        }
        String pwd = AESUtil.decrypt(dataSource.getJdbcPassword());
        if (pwd == null) {
            pwd = dataSource.getJdbcPassword();
        }
        parameterObj.put("address", addressList);
        parameterObj.put("userName", userName == null ? Constants.STRING_BLANK : userName);
        parameterObj.put("userPassword", pwd == null ? Constants.STRING_BLANK : pwd);
        parameterObj.put("dbName", dataSource.getDatabaseName());
        parameterObj.put("collectionName", plugin.getReaderTable());
        parameterObj.put("column", plugin.getColumns());
        if (StringUtils.isNotBlank(plugin.getQuery())) {
            parameterObj.put("query", plugin.getQuery());
        }
        readerObj.put("parameter", parameterObj);
        return readerObj;
    }
}
