package cn.cnic.dataspace.api.datax.admin.tool.meta;

public interface DatabaseInterface {

    /**
     * Returns the minimal SQL to launch in order to determine the layout of the resultset for a given com.com.wugui.datax.admin.tool.database table
     *
     * @param tableName The name of the table to determine the layout for
     * @return The SQL to launch.
     */
    String getSQLQueryFields(String tableName);

    String getSQLQueryCount(String tableName);

    /**
     * Get Primary Key Field
     */
    String getSQLQueryPrimaryKey();

    String getSQLQueryTableNameComment();

    String getSQLQueryTablesNameComments();

    /**
     * Obtain SQL for all table names
     */
    String getSQLQueryTables(String... tableSchema);

    /**
     * Obtain SQL for all table names
     */
    String getSQLQueryTables();

    /**
     * Obtain Table schema
     */
    String getSQLQueryTableSchema(String... args);

    /**
     * Obtain SQL for all fields
     */
    String getSQLQueryColumns(String... args);

    /**
     * Obtain SQL statements for table and field annotations
     */
    String getSQLQueryComment(String schemaName, String tableName, String columnName);

    /**
     * Get the current table maxId
     */
    String getMaxId(String tableName, String primaryKey);

    String getDatas(String tableName);
}
