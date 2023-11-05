package cn.cnic.dataspace.api.datax.admin.tool.datax.writer;

import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.tool.datax.BaseDataxPlugin;
import cn.cnic.dataspace.api.datax.admin.tool.pojo.DataxHivePojo;
import cn.cnic.dataspace.api.datax.admin.tool.pojo.DataxMongoDBPojo;
import cn.cnic.dataspace.api.datax.admin.tool.pojo.DataxRdbmsPojo;
import cn.cnic.dataspace.api.datax.admin.util.AESUtil;
import cn.cnic.dataspace.api.datax.core.util.Constants;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * datax writer base
 *
 * @author
 * @ClassName BaseWriterPlugin
 * @Version 1.0
 * @since 2019/8/2 16:28
 */
public abstract class BaseWriterPlugin extends BaseDataxPlugin {

    @Override
    public Map<String, Object> build(DataxRdbmsPojo plugin) {
        Map<String, Object> writerObj = Maps.newLinkedHashMap();
        writerObj.put("name", getName());
        Map<String, Object> parameterObj = Maps.newLinkedHashMap();
        // parameterObj.put("writeMode", "insert");
        JobDatasource jobDatasource = plugin.getJobDatasource();
        String userName = AESUtil.decrypt(jobDatasource.getJdbcUsername());
        // Determine whether the account secret is ciphertext
        if (userName == null) {
            userName = jobDatasource.getJdbcUsername();
        }
        String pwd = AESUtil.decrypt(jobDatasource.getJdbcPassword());
        if (pwd == null) {
            pwd = jobDatasource.getJdbcPassword();
        }
        parameterObj.put("username", userName);
        parameterObj.put("password", pwd);
        parameterObj.put("column", plugin.getRdbmsColumns());
        parameterObj.put("preSql", splitSql(plugin.getPreSql()));
        parameterObj.put("postSql", splitSql(plugin.getPostSql()));
        Map<String, Object> connectionObj = Maps.newLinkedHashMap();
        connectionObj.put("table", plugin.getTables());
        connectionObj.put("jdbcUrl", jobDatasource.getJdbcUrl());
        parameterObj.put("connection", ImmutableList.of(connectionObj));
        writerObj.put("parameter", parameterObj);
        return writerObj;
    }

    private String[] splitSql(String sql) {
        String[] sqlArr = null;
        if (StringUtils.isNotBlank(sql)) {
            Pattern p = Pattern.compile("\r\n|\r|\n|\n\r");
            Matcher m = p.matcher(sql);
            String sqlStr = m.replaceAll(Constants.STRING_BLANK);
            sqlArr = sqlStr.split(Constants.SPLIT_COLON);
        }
        return sqlArr;
    }

    @Override
    public Map<String, Object> buildHive(DataxHivePojo dataxHivePojo) {
        return null;
    }

    @Override
    public Map<String, Object> buildMongoDB(DataxMongoDBPojo plugin) {
        return null;
    }
}
