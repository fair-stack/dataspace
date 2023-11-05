package cn.cnic.dataspace.api.datax.admin.tool.sql;

import cn.cnic.dataspace.api.datax.admin.config.Constant;
import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.util.JdbcConstants;
import org.apache.commons.lang3.StringUtils;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Date: 3:12 PM on March 16, 2015
 */
public class JdbcConnectionFactory {

    private DataBaseType dataBaseType;

    private String jdbcUrl;

    private String userName;

    private String password;

    public JdbcConnectionFactory() {
        this.dataBaseType = DataBaseType.MySql;
        this.jdbcUrl = Constant.DEFAULT_JDBC_URL + "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowMultiQueries=true";
        this.userName = Constant.DEFAULT_JDBC_USERNAME;
        this.password = Constant.DEFAULT_JDBC_PASSWORD;
    }

    public JdbcConnectionFactory(String dbName) {
        this.dataBaseType = DataBaseType.MySql;
        if (StringUtils.isNotBlank(dbName)) {
            this.jdbcUrl = Constant.DEFAULT_JDBC_URL + "/" + dbName + "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowMultiQueries=true";
        } else {
            this.jdbcUrl = Constant.DEFAULT_JDBC_URL + "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowMultiQueries=true";
        }
        this.userName = Constant.DEFAULT_JDBC_USERNAME;
        this.password = Constant.DEFAULT_JDBC_PASSWORD;
    }

    public JobDatasource getDataSource() {
        JobDatasource jobDatasource = new JobDatasource();
        jobDatasource.setJdbcDriverClass(JdbcConstants.MYSQL_DRIVER);
        jobDatasource.setDatasource(JdbcConstants.MYSQL);
        jobDatasource.setJdbcUrl(jdbcUrl);
        jobDatasource.setJdbcUsername(userName);
        jobDatasource.setJdbcPassword(password);
        return jobDatasource;
    }

    public Connection getConnection() throws SQLException {
        Connection connection = CommonDBUtils.getConnection(dataBaseType, jdbcUrl, userName, password, 1);
        if (connection != null) {
            connection.setAutoCommit(true);
        }
        return connection;
    }

    public String getConnectionInfo() {
        return "jdbcUrl:" + jdbcUrl;
    }
}
