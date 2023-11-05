package cn.cnic.dataspace.api.datax.admin.tool.meta;

import cn.cnic.dataspace.api.datax.admin.util.JdbcConstants;

/**
 * Meta Information Factory
 */
public class DatabaseMetaFactory {

    // Return the corresponding interface based on the database type
    public static DatabaseInterface getByDbType(String dbType) {
        if (JdbcConstants.MYSQL.equals(dbType)) {
            return MySQLDatabaseMeta.getInstance();
        } else if (JdbcConstants.ORACLE.equals(dbType)) {
            return OracleDatabaseMeta.getInstance();
        } else if (JdbcConstants.POSTGRESQL.equals(dbType)) {
            return PostgresqlDatabaseMeta.getInstance();
        } else if (JdbcConstants.SQL_SERVER.equals(dbType)) {
            return SqlServerDatabaseMeta.getInstance();
        } else if (JdbcConstants.HIVE.equals(dbType)) {
            return HiveDatabaseMeta.getInstance();
        } else if (JdbcConstants.DB2.equals(dbType)) {
            return DB2DatabaseMeta.getInstance();
        } else {
            throw new UnsupportedOperationException("暂不支持的类型：".concat(dbType));
        }
    }
}
