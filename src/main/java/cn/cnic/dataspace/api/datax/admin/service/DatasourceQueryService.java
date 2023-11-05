package cn.cnic.dataspace.api.datax.admin.service;

import cn.cnic.dataspace.api.datax.admin.tool.database.TableInfo;
import com.baomidou.mybatisplus.extension.api.R;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Database Query Service
 */
public interface DatasourceQueryService {

    /**
     * Get db list
     */
    List<String> getDBs(Long id) throws IOException;

    /**
     * Find available tables based on the data source table ID
     */
    List<String> getTables(Long id, String tableSchema) throws IOException;

    /**
     * Query all table names and table data volume
     */
    List<Map<String, Object>> getTableNameAndDataCount(Long datasourceId, String tableSchema) throws IOException;

    /**
     * Get CollectionNames
     */
    List<String> getCollectionNames(long id, String dbName) throws IOException;

    /**
     * Query all fields of the table based on the data source ID and table name
     */
    List<String> getColumns(Long id, String tableName) throws IOException;

    /**
     * Obtain fields based on SQL statements
     */
    List<String> getColumnsByQuerySql(Long datasourceId, String querySql) throws SQLException;

    /**
     * Obtain PG table schema
     */
    List<String> getTableSchema(Long id);

    List<Map<String, Object>> getDatas(Long datasourceId, String tableName) throws IOException;

    R<TableInfo> getTableInfo(Long dataSourceId, String tableName) throws IOException;
}
