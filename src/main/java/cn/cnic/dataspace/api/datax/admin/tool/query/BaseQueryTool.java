package cn.cnic.dataspace.api.datax.admin.tool.query;

import cn.cnic.dataspace.api.datax.admin.core.util.LocalCacheUtil;
import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.tool.database.ColumnInfo;
import cn.cnic.dataspace.api.datax.admin.tool.database.DasColumn;
import cn.cnic.dataspace.api.datax.admin.tool.database.TableInfo;
import cn.cnic.dataspace.api.datax.admin.tool.meta.DatabaseInterface;
import cn.cnic.dataspace.api.datax.admin.tool.meta.DatabaseMetaFactory;
import cn.cnic.dataspace.api.datax.admin.tool.sql.DataBaseType;
import cn.cnic.dataspace.api.datax.admin.util.*;
import cn.cnic.dataspace.api.datax.core.util.Constants;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract Query Tool
 */
public abstract class BaseQueryTool implements QueryToolInterface {

    protected static final Logger logger = LoggerFactory.getLogger(BaseQueryTool.class);

    /**
     * Used to obtain query statements
     */
    protected DatabaseInterface sqlBuilder;

    protected DataSource datasource;

    protected Connection connection;

    /**
     * Current database name
     */
    protected String currentSchema;

    protected String currentDatabase;

    /**
     * Construction method
     */
    BaseQueryTool(JobDatasource jobDatasource) throws SQLException {
        if (LocalCacheUtil.get(jobDatasource.getCacheKey()) == null) {
            getDataSource(jobDatasource);
        } else {
            this.connection = (Connection) LocalCacheUtil.get(jobDatasource.getCacheKey());
            if (!this.connection.isValid(500)) {
                LocalCacheUtil.remove(jobDatasource.getCacheKey());
                getDataSource(jobDatasource);
            }
        }
        sqlBuilder = DatabaseMetaFactory.getByDbType(jobDatasource.getDatasource());
        currentSchema = getSchema(jobDatasource.getJdbcUsername());
        currentDatabase = jobDatasource.getDatasource();
        LocalCacheUtil.set(jobDatasource.getCacheKey(), this.connection, 4 * 60 * 60 * 1000);
    }

    private void getDataSource(JobDatasource jobDatasource) throws SQLException {
        String userName = AESUtil.decrypt(jobDatasource.getJdbcUsername());
        // The default here is to use the hikari data source
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setUsername(userName);
        dataSource.setPassword(AESUtil.decrypt(jobDatasource.getJdbcPassword()));
        dataSource.setJdbcUrl(jobDatasource.getJdbcUrl());
        if (StringUtils.isEmpty(jobDatasource.getJdbcDriverClass())) {
            String driverClassName = null;
            switch(jobDatasource.getDatasource()) {
                case JdbcConstants.MYSQL:
                    driverClassName = DataBaseType.MySql.getDriverClassName();
                    break;
                case JdbcConstants.SQL_SERVER:
                    driverClassName = DataBaseType.SQLServer.getDriverClassName();
                    break;
                case JdbcConstants.DB2:
                    driverClassName = DataBaseType.DB2.getDriverClassName();
                    break;
                case JdbcConstants.POSTGRESQL:
                    driverClassName = DataBaseType.PostgreSQL.getDriverClassName();
                    break;
                case JdbcConstants.ORACLE:
                    driverClassName = DataBaseType.Oracle.getDriverClassName();
                    break;
                case JdbcConstants.RDBMS:
                    driverClassName = DataBaseType.HIVE.getDriverClassName();
                    break;
            }
            dataSource.setDriverClassName(driverClassName);
        } else {
            dataSource.setDriverClassName(jobDatasource.getJdbcDriverClass());
        }
        dataSource.setMaximumPoolSize(1);
        dataSource.setMinimumIdle(0);
        dataSource.setConnectionTimeout(30000);
        this.datasource = dataSource;
        this.connection = this.datasource.getConnection();
    }

    // Obtain schema based on connection
    private String getSchema(String jdbcUsername) {
        String res = null;
        try {
            res = connection.getCatalog();
        } catch (SQLException e) {
            try {
                res = connection.getSchema();
            } catch (SQLException e1) {
                logger.error("[SQLException getSchema Exception] --> " + "the exception message is:" + e1.getMessage());
            }
            logger.error("[getSchema Exception] --> " + "the exception message is:" + e.getMessage());
        }
        // If res is null, treat the username as a schema
        if (StrUtil.isBlank(res) && StringUtils.isNotBlank(jdbcUsername)) {
            res = AESUtil.decrypt(jdbcUsername).toUpperCase(Locale.ROOT);
        }
        return res;
    }

    @Override
    public TableInfo buildTableInfo(String tableName) {
        // Obtain Table Information
        List<Map<String, Object>> tableInfos = this.getTableInfo(tableName);
        Pattern pattern = Pattern.compile("[\u4E00-\u9FA5|\\！|\\，|\\。|\\（|\\）|\\《|\\》|\\“|\\”|\\？|\\：|\\；|\\【|\\】]");
        Matcher matcher = pattern.matcher(tableName);
        if (CollectionUtils.isEmpty(tableInfos)) {
            if (matcher.find()) {
                throw new CommonException(CommonUtils.messageInternational("query_table_info_failed"));
            }
        }
        TableInfo tableInfo = new TableInfo();
        // Table name, comment
        List tValues = new ArrayList(tableInfos.get(0).values());
        tableInfo.setName(StrUtil.toString(tValues.get(0)));
        tableInfo.setComment(StrUtil.toString(tValues.get(1)));
        // Get all fields
        List<ColumnInfo> fullColumn = getColumns(tableName);
        tableInfo.setColumns(fullColumn);
        // Get primary key columns
        List<String> primaryKeys = getPrimaryKeys(tableName);
        logger.info("主键列为：{}", primaryKeys);
        // Set ifPrimaryKey flag
        fullColumn.forEach(e -> {
            if (primaryKeys.contains(e.getName())) {
                e.setIfPrimaryKey(true);
            } else {
                e.setIfPrimaryKey(false);
            }
        });
        return tableInfo;
    }

    // No matter how you search, the returned result should only be the table name and table annotation. Traverse the map to obtain the value value
    @Override
    public List<Map<String, Object>> getTableInfo(String tableName) {
        String sqlQueryTableNameComment = sqlBuilder.getSQLQueryTableNameComment();
        logger.info(sqlQueryTableNameComment);
        List<Map<String, Object>> res = null;
        try {
            res = JdbcUtils.executeQuery(connection, sqlQueryTableNameComment, ImmutableList.of(currentSchema, tableName));
        } catch (SQLException e) {
            logger.error("[getTableInfo Exception] --> " + "the exception message is:" + e.getMessage());
        }
        return res;
    }

    @Override
    public List<Map<String, Object>> getTables() {
        String sqlQueryTables = sqlBuilder.getSQLQueryTables();
        logger.info(sqlQueryTables);
        List<Map<String, Object>> res = null;
        try {
            res = JdbcUtils.executeQuery(connection, sqlQueryTables, ImmutableList.of(currentSchema));
        } catch (SQLException e) {
            logger.error("[getTables Exception] --> " + "the exception message is:" + e.getMessage());
        }
        return res;
    }

    @Override
    public List<ColumnInfo> getColumns(String tableName) {
        List<ColumnInfo> fullColumn = Lists.newArrayList();
        // Get all fields of the specified table
        try {
            // Obtain SQL statements for querying all fields in the specified table
            String querySql = sqlBuilder.getSQLQueryFields(tableName);
            logger.info("querySql: {}", querySql);
            // Get all fields
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(querySql);
            ResultSetMetaData metaData = resultSet.getMetaData();
            List<DasColumn> dasColumns = buildDasColumn(tableName, metaData);
            statement.close();
            // Building a fullColumn
            fullColumn = buildFullColumn(dasColumns);
        } catch (SQLException e) {
            logger.error("[getColumns Exception] --> " + "the exception message is:" + e.getMessage());
        }
        return fullColumn;
    }

    @Override
    public long getDataCount(String tableName) {
        String sqlQueryCount = sqlBuilder.getSQLQueryCount(tableName);
        logger.info("querySql: {}", sqlQueryCount);
        long count = 0L;
        try {
            // Get all fields
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sqlQueryCount);
            while (resultSet.next()) {
                count = resultSet.getLong(1);
            }
            statement.close();
        } catch (SQLException e) {
            logger.error("[getDataCount Exception] --> " + "the exception message is:" + e.getMessage());
        }
        return count;
    }

    @Override
    public List<Map<String, Object>> getDatas(String tableName) {
        List<Map<String, Object>> lineDatas = Lists.newArrayList();
        // Obtain the top 100 data in the table
        try {
            // Obtain SQL statements for querying all fields in the specified table
            String querySql = sqlBuilder.getDatas(tableName);
            logger.info("querySql: {}", querySql);
            // Get all fields
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(querySql);
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (resultSet.next()) {
                Map<String, Object> lineData = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    lineData.put(metaData.getColumnLabel(i), resultSet.getObject(i));
                }
                lineDatas.add(lineData);
            }
            statement.close();
        } catch (SQLException e) {
            logger.error("[getDatas Exception] --> " + "the exception message is:" + e.getMessage());
        }
        return lineDatas;
    }

    private List<ColumnInfo> buildFullColumn(List<DasColumn> dasColumns) {
        List<ColumnInfo> res = Lists.newArrayList();
        dasColumns.forEach(e -> {
            ColumnInfo columnInfo = new ColumnInfo();
            columnInfo.setName(e.getColumnName());
            columnInfo.setComment(e.getColumnComment());
            columnInfo.setType(e.getColumnTypeName());
            columnInfo.setIfPrimaryKey(e.isIsprimaryKey());
            columnInfo.setIsnull(e.getIsNull());
            res.add(columnInfo);
        });
        return res;
    }

    // Building DasColumn Objects
    private List<DasColumn> buildDasColumn(String tableName, ResultSetMetaData metaData) {
        List<DasColumn> res = Lists.newArrayList();
        try {
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                DasColumn dasColumn = new DasColumn();
                dasColumn.setColumnClassName(metaData.getColumnClassName(i));
                dasColumn.setColumnTypeName(metaData.getColumnTypeName(i));
                dasColumn.setColumnName(metaData.getColumnName(i));
                dasColumn.setIsNull(metaData.isNullable(i));
                res.add(dasColumn);
            }
            Statement statement = connection.createStatement();
            if (currentDatabase.equals(JdbcConstants.MYSQL) || currentDatabase.equals(JdbcConstants.ORACLE)) {
                DatabaseMetaData databaseMetaData = connection.getMetaData();
                ResultSet resultSet = databaseMetaData.getPrimaryKeys(null, null, tableName);
                while (resultSet.next()) {
                    String name = resultSet.getString("COLUMN_NAME");
                    res.forEach(e -> {
                        if (e.getColumnName().equals(name)) {
                            e.setIsprimaryKey(true);
                        } else {
                            e.setIsprimaryKey(false);
                        }
                    });
                }
                res.forEach(e -> {
                    String sqlQueryComment = sqlBuilder.getSQLQueryComment(currentSchema, tableName, e.getColumnName());
                    // Query Field Comment
                    try {
                        ResultSet resultSetComment = statement.executeQuery(sqlQueryComment);
                        while (resultSetComment.next()) {
                            e.setColumnComment(resultSetComment.getString(1));
                        }
                        JdbcUtils.close(resultSetComment);
                    } catch (SQLException e1) {
                        logger.error("[buildDasColumn executeQuery Exception] --> " + "the exception message is:" + e1.getMessage());
                    }
                });
            }
            JdbcUtils.close(statement);
        } catch (SQLException e) {
            logger.error("[buildDasColumn Exception] --> " + "the exception message is:" + e.getMessage());
        }
        return res;
    }

    // Obtain the primary key of the specified table, which may be multiple, so use list
    @Override
    public List<String> getPrimaryKeys(String tableName) {
        List<String> res = Lists.newArrayList();
        String sqlQueryPrimaryKey = sqlBuilder.getSQLQueryPrimaryKey();
        try {
            List<Map<String, Object>> pkColumns = JdbcUtils.executeQuery(connection, sqlQueryPrimaryKey, ImmutableList.of(currentSchema, tableName));
            // Just return the primary key name
            pkColumns.forEach(e -> res.add((String) new ArrayList<>(e.values()).get(0)));
        } catch (SQLException e) {
            logger.error("[getPrimaryKeys Exception] --> " + "the exception message is:" + e.getMessage());
        }
        return res;
    }

    @Override
    public List<String> getColumnNames(String tableName, String datasource) {
        List<String> res = Lists.newArrayList();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            // Obtain SQL statements for querying all fields in the specified table
            String querySql = sqlBuilder.getSQLQueryFields(tableName);
            logger.info("querySql: {}", querySql);
            // Get all fields
            stmt = connection.createStatement();
            rs = stmt.executeQuery(querySql);
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnName(i);
                if (JdbcConstants.HIVE.equals(datasource)) {
                    if (columnName.contains(Constants.SPLIT_POINT)) {
                        res.add(i - 1 + Constants.SPLIT_SCOLON + columnName.substring(columnName.indexOf(Constants.SPLIT_POINT) + 1) + Constants.SPLIT_SCOLON + metaData.getColumnTypeName(i));
                    } else {
                        res.add(i - 1 + Constants.SPLIT_SCOLON + columnName + Constants.SPLIT_SCOLON + metaData.getColumnTypeName(i));
                    }
                } else {
                    res.add(columnName);
                }
            }
        } catch (SQLException e) {
            logger.error("[getColumnNames Exception] --> " + "the exception message is:" + e.getMessage());
        } finally {
            JdbcUtils.close(rs);
            JdbcUtils.close(stmt);
        }
        return res;
    }

    @Override
    public List<String> getTableNames(String tableSchema) {
        List<String> tables = new ArrayList<String>();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.createStatement();
            // Obtain SQL
            String sql = getSQLQueryTables(tableSchema);
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String tableName = rs.getString(1);
                tables.add(tableName);
            }
            tables.sort(Comparator.naturalOrder());
        } catch (SQLException e) {
            logger.error("[getTableNames Exception] --> " + "the exception message is:" + e.getMessage());
        } finally {
            JdbcUtils.close(rs);
            JdbcUtils.close(stmt);
        }
        return tables;
    }

    @Override
    public List<String> getTableNames() {
        List<String> tables = new ArrayList<String>();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.createStatement();
            // Obtain SQL
            String sql = getSQLQueryTables();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String tableName = rs.getString(1);
                tables.add(tableName);
            }
        } catch (SQLException e) {
            logger.error("[getTableNames Exception] --> " + "the exception message is:" + e.getMessage());
        } finally {
            JdbcUtils.close(rs);
            JdbcUtils.close(stmt);
        }
        return tables;
    }

    public Boolean dataSourceTest() {
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            if (metaData.getDatabaseProductName().length() > 0) {
                return true;
            }
        } catch (SQLException e) {
            logger.error("[dataSourceTest Exception] --> " + "the exception message is:" + e.getMessage());
        }
        return false;
    }

    protected String getSQLQueryTables(String tableSchema) {
        return sqlBuilder.getSQLQueryTables(tableSchema);
    }

    /**
     * Do not rewrite if no other parameters are required
     */
    protected String getSQLQueryTables() {
        return sqlBuilder.getSQLQueryTables();
    }

    @Override
    public List<String> getColumnsByQuerySql(String querySql) throws SQLException {
        List<String> res = Lists.newArrayList();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            querySql = querySql.replace(";", "");
            // Assemble SQL statements by adding where 1=0 after them
            String sql = querySql.concat(" where 1=0");
            // Determine if there is already a where, and if so, add and 1=0
            // Starting from the last), find where, or find the entire statement
            if (querySql.contains(")")) {
                if (querySql.substring(querySql.indexOf(")")).contains("where")) {
                    sql = querySql.concat(" and 1=0");
                }
            } else {
                if (querySql.contains("where")) {
                    sql = querySql.concat(" and 1=0");
                }
            }
            // Get all fields
            stmt = connection.createStatement();
            rs = stmt.executeQuery(sql);
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                res.add(metaData.getColumnName(i));
            }
        } finally {
            JdbcUtils.close(rs);
            JdbcUtils.close(stmt);
        }
        return res;
    }

    @Override
    public long getMaxIdVal(String tableName, String primaryKey) {
        Statement stmt = null;
        ResultSet rs = null;
        long maxVal = 0;
        try {
            stmt = connection.createStatement();
            // Obtain SQL
            String sql = getSQLMaxID(tableName, primaryKey);
            rs = stmt.executeQuery(sql);
            rs.next();
            maxVal = rs.getLong(1);
        } catch (SQLException e) {
            logger.error("[getMaxIdVal Exception] --> " + "the exception message is:" + e.getMessage());
        } finally {
            JdbcUtils.close(rs);
            JdbcUtils.close(stmt);
        }
        return maxVal;
    }

    private String getSQLMaxID(String tableName, String primaryKey) {
        return sqlBuilder.getMaxId(tableName, primaryKey);
    }

    public void executeCreateTableSql(String querySql) {
        if (StringUtils.isBlank(querySql)) {
            return;
        }
        Statement stmt = null;
        try {
            stmt = connection.createStatement();
            stmt.executeUpdate(querySql);
        } catch (SQLException e) {
            logger.error("[executeCreateTableSql Exception] --> " + "the exception message is:" + e.getMessage());
        } finally {
            JdbcUtils.close(stmt);
        }
    }

    public List<String> getTableSchema() {
        List<String> schemas = new ArrayList<>();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = connection.createStatement();
            // Obtain SQL
            String sql = getSQLQueryTableSchema();
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String tableName = rs.getString(1);
                schemas.add(tableName);
            }
        } catch (SQLException e) {
            logger.error("[getTableNames Exception] --> " + "the exception message is:" + e.getMessage());
        } finally {
            JdbcUtils.close(rs);
            JdbcUtils.close(stmt);
        }
        return schemas;
    }

    protected String getSQLQueryTableSchema() {
        return sqlBuilder.getSQLQueryTableSchema();
    }
}
