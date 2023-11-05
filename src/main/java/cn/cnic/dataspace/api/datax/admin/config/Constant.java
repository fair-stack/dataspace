package cn.cnic.dataspace.api.datax.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10001)
public class Constant {

    public static final String SERVICE_RESTART = "服务重启";

    public static String DEFAULT_JDBC_URL;

    public static String DEFAULT_JDBC_USERNAME;

    public static String DEFAULT_JDBC_PASSWORD;

    public static String DEFAULT_JDBC_DATABASE;

    public static String SQL_ROOT_PATH;

    @Value("${spring.datasource.custom_url}")
    public void setDefaultJdbcUrl(String defaultJdbcUrl) {
        Constant.DEFAULT_JDBC_URL = defaultJdbcUrl;
    }

    @Value("${spring.datasource.username}")
    public void setDefaultJdbcUsername(String jdbcUsername) {
        Constant.DEFAULT_JDBC_USERNAME = jdbcUsername;
    }

    @Value("${spring.datasource.password}")
    public void setDefaultJdbcPassword(String jdbcPassword) {
        Constant.DEFAULT_JDBC_PASSWORD = jdbcPassword;
    }

    @Value("${spring.datasource.default_db}")
    public void setDefaultJdbcDatabase(String jdbcDatabase) {
        Constant.DEFAULT_JDBC_DATABASE = jdbcDatabase;
    }

    @Value("${spring.datasource.sql_root_path}")
    public void setSqlRootPath(String sqlRootPath) {
        Constant.SQL_ROOT_PATH = sqlRootPath;
    }
}
