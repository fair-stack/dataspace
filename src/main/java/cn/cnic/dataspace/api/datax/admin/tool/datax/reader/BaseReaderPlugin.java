package cn.cnic.dataspace.api.datax.admin.tool.datax.reader;

import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.tool.datax.BaseDataxPlugin;
import cn.cnic.dataspace.api.datax.admin.tool.pojo.DataxHivePojo;
import cn.cnic.dataspace.api.datax.admin.tool.pojo.DataxMongoDBPojo;
import cn.cnic.dataspace.api.datax.admin.tool.pojo.DataxRdbmsPojo;
import cn.cnic.dataspace.api.datax.admin.util.AESUtil;
import cn.cnic.dataspace.api.util.JwtTokenUtils;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import java.util.Map;

/**
 * Reader
 *
 * @author
 * @ClassName BaseReaderPlugin
 * @Version 1.0
 * @since 2019/8/2 16:27
 */
public abstract class BaseReaderPlugin extends BaseDataxPlugin {

    @Override
    public Map<String, Object> build(DataxRdbmsPojo plugin) {
        // structure
        Map<String, Object> readerObj = Maps.newLinkedHashMap();
        readerObj.put("name", getName());
        Map<String, Object> parameterObj = Maps.newLinkedHashMap();
        Map<String, Object> connectionObj = Maps.newLinkedHashMap();
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
        // Determine if it is querySql
        if (StrUtil.isNotBlank(plugin.getQuerySql())) {
            connectionObj.put("querySql", ImmutableList.of(plugin.getQuerySql()));
        } else {
            parameterObj.put("column", plugin.getRdbmsColumns());
            // Determine if there is a where
            if (StringUtils.isNotBlank(plugin.getWhereParam())) {
                parameterObj.put("where", plugin.getWhereParam());
            }
            connectionObj.put("table", plugin.getTables());
        }
        parameterObj.put("splitPk", plugin.getSplitPk());
        connectionObj.put("jdbcUrl", ImmutableList.of(jobDatasource.getJdbcUrl()));
        parameterObj.put("connection", ImmutableList.of(connectionObj));
        readerObj.put("parameter", parameterObj);
        return readerObj;
    }

    @Override
    public Map<String, Object> buildHive(DataxHivePojo dataxHivePojo) {
        return null;
    }

    @Override
    public Map<String, Object> buildMongoDB(DataxMongoDBPojo dataxMongoDBPojo) {
        return null;
    }
}
