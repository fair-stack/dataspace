package cn.cnic.dataspace.api.datax.admin.tool.query;

import cn.cnic.dataspace.api.datax.admin.tool.database.ColumnInfo;
import cn.cnic.dataspace.api.datax.admin.tool.database.TableInfo;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Basic query interface
 */
public interface QueryToolInterface {

    /**
     * Building a tableInfo object
     */
    TableInfo buildTableInfo(String tableName);

    /**
     * Query primary key
     */
    List<String> getPrimaryKeys(String tableName);

    /**
     * Obtain specified table information
     */
    List<Map<String, Object>> getTableInfo(String tableName);

    /**
     * Obtain all tables under the current schema
     */
    List<Map<String, Object>> getTables();

    /**
     * Obtain all fields based on table name
     */
    List<ColumnInfo> getColumns(String tableName);

    /**
     * Obtaining data volume
     */
    long getDataCount(String tableName);

    /**
     * Obtain the top 100 pieces of data
     */
    List<Map<String, Object>> getDatas(String tableName);

    /**
     * Obtain all field names based on table names (excluding table names)
     */
    List<String> getColumnNames(String tableName, String datasource);

    /**
     * Get all available table names
     */
    List<String> getTableNames(String schema);

    /**
     * Get all available table names
     */
    List<String> getTableNames();

    /**
     * Obtain columns by querying SQL
     */
    List<String> getColumnsByQuerySql(String querySql) throws SQLException;

    /**
     * Get the current table maxId
     */
    long getMaxIdVal(String tableName, String primaryKey);
}
