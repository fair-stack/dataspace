package cn.cnic.dataspace.api.datax.admin.tool.query;

import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.util.JdbcConstants;
import cn.cnic.dataspace.api.datax.admin.util.RdbmsException;
import java.sql.SQLException;

/**
 * Tool class, obtaining singleton entities
 */
public class QueryToolFactory {

    public static BaseQueryTool getByDbType(JobDatasource jobDatasource) {
        // Get dbType
        String datasource = jobDatasource.getDatasource();
        if (JdbcConstants.MYSQL.equals(datasource)) {
            return getMySQLQueryToolInstance(jobDatasource);
        } else if (JdbcConstants.ORACLE.equals(datasource)) {
            return getOracleQueryToolInstance(jobDatasource);
        } else if (JdbcConstants.POSTGRESQL.equals(datasource)) {
            return getPostgresqlQueryToolInstance(jobDatasource);
        } else if (JdbcConstants.SQL_SERVER.equals(datasource)) {
            return getSqlserverQueryToolInstance(jobDatasource);
        } else if (JdbcConstants.HIVE.equals(datasource)) {
            return getHiveQueryToolInstance(jobDatasource);
        } else if (JdbcConstants.MARIADB.equals(datasource)) {
            return getDB2QueryToolInstance(jobDatasource);
        } else if (JdbcConstants.RDBMS.equals(datasource)) {
            return getRDBMSQueryToolInstance(jobDatasource);
        }
        throw new UnsupportedOperationException("找不到该类型: ".concat(datasource));
    }

    private static BaseQueryTool getMySQLQueryToolInstance(JobDatasource jdbcDatasource) {
        try {
            return new MySQLQueryTool(jdbcDatasource);
        } catch (Exception e) {
            throw RdbmsException.asConnException(JdbcConstants.MYSQL, e, jdbcDatasource.getJdbcUsername(), jdbcDatasource.getDatasourceName());
        }
    }

    private static BaseQueryTool getOracleQueryToolInstance(JobDatasource jdbcDatasource) {
        try {
            return new OracleQueryTool(jdbcDatasource);
        } catch (Exception e) {
            throw RdbmsException.asConnException(JdbcConstants.ORACLE, e, jdbcDatasource.getJdbcUsername(), jdbcDatasource.getDatasourceName());
        }
    }

    private static BaseQueryTool getPostgresqlQueryToolInstance(JobDatasource jdbcDatasource) {
        try {
            return new PostgresqlQueryTool(jdbcDatasource);
        } catch (Exception e) {
            throw RdbmsException.asConnException(JdbcConstants.POSTGRESQL, e, jdbcDatasource.getJdbcUsername(), jdbcDatasource.getDatasourceName());
        }
    }

    private static BaseQueryTool getSqlserverQueryToolInstance(JobDatasource jdbcDatasource) {
        try {
            return new SqlServerQueryTool(jdbcDatasource);
        } catch (Exception e) {
            throw RdbmsException.asConnException(JdbcConstants.SQL_SERVER, e, jdbcDatasource.getJdbcUsername(), jdbcDatasource.getDatasourceName());
        }
    }

    private static BaseQueryTool getHiveQueryToolInstance(JobDatasource jdbcDatasource) {
        try {
            return new HiveQueryTool(jdbcDatasource);
        } catch (Exception e) {
            throw RdbmsException.asConnException(JdbcConstants.HIVE, e, jdbcDatasource.getJdbcUsername(), jdbcDatasource.getDatasourceName());
        }
    }

    private static BaseQueryTool getRDBMSQueryToolInstance(JobDatasource jdbcDatasource) {
        try {
            return new RDBMSQueryTool(jdbcDatasource);
        } catch (Exception e) {
            throw RdbmsException.asConnException(JdbcConstants.RDBMS, e, jdbcDatasource.getJdbcUsername(), jdbcDatasource.getDatasourceName());
        }
    }

    private static BaseQueryTool getDB2QueryToolInstance(JobDatasource jdbcDatasource) {
        try {
            return new DB2SQLQueryTool(jdbcDatasource);
        } catch (Exception e) {
            throw RdbmsException.asConnException(JdbcConstants.DB2, e, jdbcDatasource.getJdbcUsername(), jdbcDatasource.getDatasourceName());
        }
    }
}
