package cn.cnic.dataspace.api.datax.admin.tool.sql;

import cn.cnic.dataspace.api.datax.admin.dto.QueryDataMappingDataVO;
import cn.cnic.dataspace.api.datax.admin.dto.QueryDataMappingGroupVO;
import cn.cnic.dataspace.api.datax.admin.tool.database.ColumnInfo;
import cn.cnic.dataspace.api.datax.admin.tool.database.TableInfo;
import cn.cnic.dataspace.api.datax.admin.util.JdbcConstants;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.util.CommonUtils;
import com.beust.jcommander.internal.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.jdbc.SQL;
import org.springframework.jdbc.object.SqlUpdate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// import static cn.cnic.dataspace.api.datax.admin.util.JdbcConstants.EMPTY_VAL_KEY;
@Component
@Slf4j
public class SqlUtils {

    private static final Map<String, String> COLUMN_MAP = new HashMap<>();

    private static final Map<String, String> COLUMN_MAP2 = new HashMap<>();

    static {
        COLUMN_MAP.put("数字", "double");
        COLUMN_MAP.put("文本", "text");
        COLUMN_MAP.put("日期", "datetime");
        // date
        COLUMN_MAP.put("date", "datetime");
        COLUMN_MAP.put("datetime", "datetime");
        COLUMN_MAP.put("timestamp", "datetime");
        COLUMN_MAP.put("time", "datetime");
        // text
        COLUMN_MAP.put("longtext", "text");
        COLUMN_MAP.put("varchar", "text");
        COLUMN_MAP.put("char", "text");
        COLUMN_MAP.put("tinytext", "text");
        COLUMN_MAP.put("text", "text");
        COLUMN_MAP.put("mediumtext", "text");
        // double
        COLUMN_MAP.put("int", "double");
        COLUMN_MAP.put("bigint", "double");
        COLUMN_MAP.put("tinyint", "double");
        COLUMN_MAP.put("mediumint", "double");
        COLUMN_MAP.put("smallint", "double");
        COLUMN_MAP.put("decimal", "double");
        COLUMN_MAP.put("float", "double");
        COLUMN_MAP.put("double", "double");
        // date
        COLUMN_MAP2.put("date", "日期");
        COLUMN_MAP2.put("datetime", "日期");
        COLUMN_MAP2.put("timestamp", "日期");
        COLUMN_MAP2.put("time", "日期");
        // text2
        COLUMN_MAP2.put("longtext", "文本");
        COLUMN_MAP2.put("varchar", "文本");
        COLUMN_MAP2.put("char", "文本");
        COLUMN_MAP2.put("tinytext", "文本");
        COLUMN_MAP2.put("text", "文本");
        COLUMN_MAP2.put("mediumtext", "文本");
        // double2
        COLUMN_MAP2.put("int", "数字");
        COLUMN_MAP2.put("bigint", "数字");
        COLUMN_MAP2.put("tinyint", "数字");
        COLUMN_MAP2.put("mediumint", "数字");
        COLUMN_MAP2.put("smallint", "数字");
        COLUMN_MAP2.put("decimal", "数字");
        COLUMN_MAP2.put("float", "数字");
        COLUMN_MAP2.put("double", "数字");
    }

    /**
     * Convert type to text numeric date
     */
    public static String getViewType(String type) {
        if (StringUtils.isEmpty(type)) {
            return "文本";
        }
        String retS = COLUMN_MAP2.get(type.toLowerCase(Locale.ROOT));
        return StringUtils.isEmpty(retS) ? "文本" : retS;
    }

    public static String getInsertDBColType(String type) {
        if (StringUtils.isEmpty(type)) {
            return "longtext";
        }
        String retS = COLUMN_MAP.get(type.toLowerCase(Locale.ROOT));
        return StringUtils.isEmpty(retS) ? "longtext" : retS;
    }

    /**
     * Uuid (32-bit)
     */
    public static String getUUID32() {
        return UUID.randomUUID().toString().replace("-", "").toLowerCase(Locale.ENGLISH);
    }

    /**
     * When judging a string, it contains Chinese characters
     */
    public static boolean isCN(String str) {
        Pattern pattern = Pattern.compile("[\u4E00-\u9FA5|\\！|\\，|\\。|\\（|\\）|\\《|\\》|\\“|\\”|\\？|\\：|\\；|\\【|\\】]");
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Generate and modify table name SQL
     */
    public static String generateAlterTableNameSql(String dbName, String tableName, String newTableName) {
        if (StringUtils.isEmpty(dbName)) {
            return "alter table `" + tableName + "` rename to `" + newTableName + "`";
        } else {
            return "alter table `" + dbName + "`.`" + tableName + "` rename to `" + dbName + "`.`" + newTableName + "`";
        }
    }

    /**
     * Modify field SQL
     */
    public static String generateAlterColSql(String dbName, String tableName, String oldColName, String newColName, String type) {
        if (StringUtils.isEmpty(dbName)) {
            return "alter table `" + tableName + "` change column `" + oldColName + "` `" + newColName + "` " + getInsertDBColType(type);
        } else {
            return "alter table `" + dbName + "`.`" + tableName + "` change column `" + oldColName + "` `" + newColName + "` " + getInsertDBColType(type);
        }
    }

    /**
     * Generate database creation SQL
     */
    public static String generateCreateDBSql(String dbName) {
        if (StringUtils.isEmpty(dbName)) {
            throw new RuntimeException(CommonUtils.messageInternational("db_not_empty"));
        }
        return "create database `" + dbName + "` DEFAULT CHARSET=utf8 COLLATE utf8_general_ci";
    }

    /**
     * Query statement
     */
    public static String generateSelectSql(String dbName, String tableName, Integer start, Integer size) {
        if (StringUtils.isEmpty(tableName)) {
            throw new RuntimeException(CommonUtils.messageInternational("param_error"));
        }
        if (StringUtils.isEmpty(dbName)) {
            return "select * from `" + tableName + "` limit " + start + "," + size;
        } else {
            return "select * from `" + dbName + "`.`" + tableName + "` limit " + start + "," + size;
        }
    }

    /**
     * Query statement
     */
    public static String generateSelectBySortAndFilterAndSql(String dbName, String tableName, Integer start, Integer size, Map<String, QueryDataMappingDataVO.QuerySortFilter> querySortFilterMap) {
        if (StringUtils.isEmpty(tableName)) {
            throw new RuntimeException(CommonUtils.messageInternational("param_error"));
        }
        SQL sql = new SQL();
        sql.SELECT("*");
        if (StringUtils.isEmpty(dbName)) {
            sql.FROM(String.format("`%s`", tableName));
        } else {
            sql.FROM(String.format("`%s`.`%s`", dbName, tableName));
        }
        Set<Map.Entry<String, QueryDataMappingDataVO.QuerySortFilter>> entries = querySortFilterMap.entrySet();
        for (Map.Entry<String, QueryDataMappingDataVO.QuerySortFilter> entry : entries) {
            String col = entry.getKey();
            QueryDataMappingDataVO.QuerySortFilter querySortFilter = entry.getValue();
            sql.AND().WHERE(getWhere(col, querySortFilter));
            if (StringUtils.isNotEmpty(querySortFilter.getSort())) {
                String sortStr = String.format("`%s` %s", col, querySortFilter.getSort());
                sql.ORDER_BY(sortStr);
            }
        }
        sql.LIMIT(size).OFFSET(start);
        return sql.toString();
    }

    /**
     * Query statement
     */
    public static String generateSelectCountByFilterAndSql(String dbName, String tableName, Map<String, QueryDataMappingDataVO.QuerySortFilter> querySortFilterMap) {
        if (StringUtils.isEmpty(tableName)) {
            throw new RuntimeException(CommonUtils.messageInternational("param_error"));
        }
        SQL sql = new SQL();
        sql.SELECT("count(1)");
        if (StringUtils.isEmpty(dbName)) {
            sql.FROM(String.format("`%s`", tableName));
        } else {
            sql.FROM(String.format("`%s`.`%s`", dbName, tableName));
        }
        Set<Map.Entry<String, QueryDataMappingDataVO.QuerySortFilter>> entries = querySortFilterMap.entrySet();
        for (Map.Entry<String, QueryDataMappingDataVO.QuerySortFilter> entry : entries) {
            String col = entry.getKey();
            QueryDataMappingDataVO.QuerySortFilter querySortFilter = entry.getValue();
            sql.AND().WHERE(getWhere(col, querySortFilter));
        }
        return sql.toString();
    }

    /**
     * @param filter
     * @return
     */
    private static String getWhere(String col, QueryDataMappingDataVO.QuerySortFilter filter) {
        String where = "";
        if (filter == null) {
            return "1=1";
        }
        if (StringUtils.isEmpty(col)) {
            return "1=1";
        }
        if (filter.getFilterType() == null) {
            // Default not to add conditions
            return "1=1";
        } else if (filter.getFilterType() == 0) {
            // Query according to options
            List<QueryDataMappingDataVO.Select> selectList = filter.getFilter().getSelect();
            if (CollectionUtils.isEmpty(selectList)) {
                // If the option is empty, default to querying all
                return "1=1";
            } else {
                // Splicing Option Conditions
                boolean isContainNull = false;
                List<String> collect = new ArrayList<>();
                for (QueryDataMappingDataVO.Select select : selectList) {
                    if (select.isNull()) {
                        // Option value represents null value
                        isContainNull = true;
                    } else {
                        collect.add("'" + select.getValue() + "'");
                    }
                }
                if (!CollectionUtils.isEmpty(collect)) {
                    String join = String.join(",", collect);
                    where = "`" + col + "` in (" + join + ")";
                    if (isContainNull) {
                        // To query for null values
                        where += " or `" + col + "` is null";
                    }
                } else {
                    where += "`" + col + "` is null";
                }
            }
        } else if (filter.getFilterType() == 1) {
            String oper = filter.getFilter().getCondition().getOper();
            String oper_en = JdbcConstants.OPER_MAP.get(oper);
            String value = filter.getFilter().getCondition().getValue();
            String value2 = filter.getFilter().getCondition().getValue2();
            if (StringUtils.isEmpty(oper_en)) {
                return "1=1";
            }
            switch(oper_en) {
                case "like":
                    where = "`" + col + "` like '%" + value + "%'";
                    break;
                case "not like":
                    where = "`" + col + "` not like '%" + value + "%'";
                    where += " or `" + col + "` is null";
                    break;
                case "interval":
                    if (StringUtils.isEmpty(value) || StringUtils.isEmpty(value2)) {
                        return "1=1";
                    }
                    where = "`" + col + "` >= '" + value + "' and `" + col + "` <= '" + value2 + "'";
                    break;
                case "not interval":
                    if (StringUtils.isEmpty(value) || StringUtils.isEmpty(value2)) {
                        return "1=1";
                    }
                    where = "`" + col + "` < '" + value + "' or `" + col + "` > '" + value2 + "'";
                    where += " or `" + col + "` is null";
                    break;
                case "is null":
                    where = "`" + col + "` is null";
                    break;
                case "is not null":
                    where = "`" + col + "` is not null";
                    break;
                case "start":
                    where = "`" + col + "` like '" + value + "%'";
                    break;
                case "end":
                    where = "`" + col + "` like '%" + value + "'";
                    break;
                case "!=":
                    where = "`" + col + "` != '" + value + "'";
                    where += " or `" + col + "` is null";
                    break;
                default:
                    where = "`" + col + "`" + oper_en + "'" + value + "'";
            }
        } else {
            where = "1=1";
        }
        return where;
    }

    /**
     * Query statement
     */
    public static String generateSelectBySortAndFilterOrSql(String dbName, String tableName, Integer start, Integer size, Map<String, QueryDataMappingDataVO.QuerySortFilter> querySortFilterMap) {
        if (StringUtils.isEmpty(tableName)) {
            throw new RuntimeException(CommonUtils.messageInternational("param_error"));
        }
        SQL sql = new SQL();
        sql.SELECT("*");
        if (StringUtils.isEmpty(dbName)) {
            sql.FROM(String.format("`%s`", tableName));
        } else {
            sql.FROM(String.format("`%s`.`%s`", dbName, tableName));
        }
        Set<Map.Entry<String, QueryDataMappingDataVO.QuerySortFilter>> entries = querySortFilterMap.entrySet();
        for (Map.Entry<String, QueryDataMappingDataVO.QuerySortFilter> entry : entries) {
            String col = entry.getKey();
            QueryDataMappingDataVO.QuerySortFilter querySortFilter = entry.getValue();
            sql.OR().WHERE(getWhere(col, querySortFilter));
            if (StringUtils.isNotEmpty(querySortFilter.getSort())) {
                String sortStr = String.format("`%s` %s", col, querySortFilter.getSort());
                sql.ORDER_BY(sortStr);
            }
        }
        sql.LIMIT(size).OFFSET(start);
        return sql.toString();
    }

    /**
     * Query statement returns the number of queries
     */
    public static String generateSelectCountByFilterOrSql(String dbName, String tableName, Map<String, QueryDataMappingDataVO.QuerySortFilter> querySortFilterMap) {
        if (StringUtils.isEmpty(tableName)) {
            throw new RuntimeException(CommonUtils.messageInternational("param_error"));
        }
        SQL sql = new SQL();
        sql.SELECT("count(1)");
        if (StringUtils.isEmpty(dbName)) {
            sql.FROM(String.format("`%s`", tableName));
        } else {
            sql.FROM(String.format("`%s`.`%s`", dbName, tableName));
        }
        Set<Map.Entry<String, QueryDataMappingDataVO.QuerySortFilter>> entries = querySortFilterMap.entrySet();
        for (Map.Entry<String, QueryDataMappingDataVO.QuerySortFilter> entry : entries) {
            String col = entry.getKey();
            QueryDataMappingDataVO.QuerySortFilter querySortFilter = entry.getValue();
            sql.OR().WHERE(getWhere(col, querySortFilter));
        }
        return sql.toString();
    }

    /**
     * Query statement
     */
    public static String generateSelectSqlNoResult(String dbName, String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            throw new RuntimeException(CommonUtils.messageInternational("param_error"));
        }
        if (StringUtils.isEmpty(dbName)) {
            return String.format("select * from `%s` where 0 = 1", tableName);
        } else {
            return String.format("select * from `%s`.`%s` where 0 = 1", dbName, tableName);
        }
    }

    /**
     * Query statement
     */
    public static String generateSelectSqlWithAll(String dbName, String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            throw new RuntimeException(CommonUtils.messageInternational("param_error"));
        }
        if (StringUtils.isEmpty(dbName)) {
            return "select * from `" + tableName + "`";
        } else {
            return "select * from `" + dbName + "`.`" + tableName + "`";
        }
    }

    /**
     * Query statement padding
     */
    public static String generateSelectSqlWithPaging(String dbName, String tableName, long offset, long size) {
        if (StringUtils.isEmpty(tableName)) {
            throw new RuntimeException(CommonUtils.messageInternational("param_error"));
        }
        if (StringUtils.isEmpty(dbName)) {
            return String.format("select * from `%s` limit %s offset %s", tableName, size, offset);
            // return "select * from `" + tableName + "` offsite " + offset + " limit " + size;
        } else {
            return String.format("select * from `%s`.`%s` limit %s offset %s", dbName, tableName, size, offset);
            // return "select * from `" + dbName + "`.`" + tableName + "` offsite " + offset + " limit " + size;
        }
    }

    /**
     * Total number of queries statement
     */
    public static String generateSelectCountSql(String dbName, String tableName) {
        if (StringUtils.isEmpty(tableName)) {
            throw new RuntimeException(CommonUtils.messageInternational("param_error"));
        }
        if (StringUtils.isEmpty(dbName)) {
            return "select count(1) from `" + tableName + "`";
        } else {
            return "select count(1) from `" + dbName + "`.`" + tableName + "`";
        }
    }

    /**
     * create like
     *
     * @param dbName
     * @param sourceTableName
     * @param targetTableName
     * @return
     */
    public static String generateCreateLikeSql(String dbName, String sourceTableName, String targetTableName) {
        if (StringUtils.isAnyEmpty(sourceTableName, targetTableName)) {
            throw new RuntimeException(CommonUtils.messageInternational("param_error"));
        }
        if (StringUtils.isEmpty(dbName)) {
            return String.format("create table `%s` like `%s`", targetTableName, sourceTableName);
        } else {
            return String.format("create table `%s`.`%s` like `%s`.`%s`", dbName, targetTableName, dbName, sourceTableName);
        }
    }

    /**
     * insert select
     *
     * @param dbName
     * @param sourceTableName
     * @param targetTableName
     * @return
     */
    public static String generateInsertFromSelectSql(String dbName, String sourceTableName, String targetTableName) {
        if (StringUtils.isAnyEmpty(sourceTableName, targetTableName)) {
            throw new RuntimeException(CommonUtils.messageInternational("param_error"));
        }
        if (StringUtils.isEmpty(dbName)) {
            return String.format("insert into `%s` select * from `%s`", targetTableName, sourceTableName);
        } else {
            return String.format("insert into `%s`.`%s` select * from `%s`.`%s`", dbName, targetTableName, dbName, sourceTableName);
        }
    }

    /**
     * Generate Delete Table SQL
     */
    public static String generateDropTableSql(String dbName, String tableName) {
        if (StringUtils.isEmpty(dbName)) {
            return String.format("drop table if exists `%s`", tableName);
        } else {
            return String.format("drop table if exists `%s`.`%s`", dbName, tableName);
        }
    }

    /**
     * MySQL Table Creation Statement
     */
    public static String generateCreateTableSqlWithDefaultId(String dbName, TableInfo tableInfo) {
        List<ColumnInfo> columns = tableInfo.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo col = columns.get(i);
            if (StringUtils.isBlank(col.getType()) || "null".equals(col.getType())) {
                // Set the default type to longtext
                col.setType("longtext");
            }
        }
        // ColumnInfo primaryCol = new ColumnInfo();
        // primaryCol.setName("ds_id");
        // primaryCol.setComment("primary key");
        // primaryCol.setType("int");
        // primaryCol.setIfPrimaryKey(true);
        List<ColumnInfo> newCols = Lists.newArrayList();
        newCols.addAll(columns);
        String str = "CREATE TABLE ";
        if (StringUtils.isNotBlank(tableInfo.getName())) {
            str += "`" + dbName + "`" + ".`" + tableInfo.getName() + "`( ";
        } else {
            throw new RuntimeException(CommonUtils.messageInternational("tablename_not_empty"));
        }
        String tableComment = "comment";
        if (!StringUtils.isEmpty(tableInfo.getComment())) {
            tableComment = tableInfo.getComment();
        }
        if (!CollectionUtils.isEmpty(newCols)) {
            str += JdbcConstants.PRIMARY_KEY + " int  primary key auto_increment,";
            for (ColumnInfo column : newCols) {
                str += "`" + column.getName() + "` \t " + generateColumnSting(column.getType(), column.getComment(), "");
            }
            // Remove the last comma
            str = str.substring(0, str.length() - 1);
        }
        str += ") ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_general_ci COMMENT = '" + tableComment + "';";
        return str;
    }

    /**
     * MySQL Table Creation Statement
     */
    public static String generateCreateTableSql(String dbName, TableInfo tableInfo) {
        List<ColumnInfo> columns = tableInfo.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            ColumnInfo col = columns.get(i);
            if (StringUtils.isBlank(col.getType()) || "null".equals(col.getType())) {
                // Set the default type to longtext
                col.setType("longtext");
            }
        }
        String str = "CREATE TABLE ";
        if (StringUtils.isNotBlank(tableInfo.getName())) {
            str += "`" + dbName + "`" + ".`" + tableInfo.getName() + "`( ";
        } else {
            throw new RuntimeException(CommonUtils.messageInternational("tablename_not_empty"));
        }
        String tableComment = "comment";
        if (!StringUtils.isEmpty(tableInfo.getComment())) {
            tableComment = tableInfo.getComment();
        }
        if (!CollectionUtils.isEmpty(columns)) {
            for (ColumnInfo column : columns) {
                str += "`" + column.getName() + "` \t " + generateColumnSting(column.getType(), column.getComment(), "");
            }
            // Remove the last comma
            str = str.substring(0, str.length() - 1);
        }
        str += ") ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE utf8_general_ci COMMENT = '" + tableComment + "';";
        return str;
    }

    /**
     * Generate insert statement
     */
    public static String generateInsertSql(String tableName, List<String> columnNames) {
        SQL sql = new SQL();
        // set a table name
        sql.INSERT_INTO("`" + tableName + "`");
        // set field name
        for (String key : columnNames) {
            sql.VALUES("`" + key + "`", "?");
        }
        return sql.toString();
    }

    /**
     * Generate insert statement
     */
    public static String generateInsertSql(String dbName, String tableName, List<String> columnNames) {
        SQL sql = new SQL();
        // set a table name
        sql.INSERT_INTO("`" + dbName + "`.`" + tableName + "`");
        // set field name
        for (String key : columnNames) {
            sql.VALUES("`" + key + "`", "?");
        }
        return sql.toString();
    }

    /**
     * Generate update statement
     */
    public static String generateUpdateSql(String tableName, List<String> columnNames) {
        SQL sql = new SQL();
        // set a table name
        sql.UPDATE("`" + tableName + "`");
        // set field name
        for (String key : columnNames) {
            sql.SET("`" + key + "`=?");
            // sql.VALUES("`" + key + "`", "?");
        }
        String sqlStr = sql.toString();
        return sqlStr + " WHERE " + JdbcConstants.PRIMARY_KEY + " = ?";
    }

    private static String generateColumnSting(String type, String comment, String default_) {
        String ret = "";
        ret += getInsertDBColType(type);
        ret += " comment '" + comment + "'";
        ret += " default null,";
        return ret;
    }

    /**
     * Delete Column SQL
     */
    public static String generateDropColSql(String dbName, String tableName, String col) {
        if (StringUtils.isEmpty(dbName)) {
            return String.format("alter table `%s` drop column `%s`", tableName, col);
        } else {
            return String.format("alter table `%s`.`%s` drop column `%s`", dbName, tableName, col);
        }
    }

    /**
     * Add column SQL after
     */
    public static String generateAddAfterColSql(String dbName, String tableName, String preColName, String newColName, String newColType) {
        if (StringUtils.isEmpty(dbName)) {
            return String.format("alter table `%s` add column `%s` %s after `%s`", tableName, newColName, getInsertDBColType(newColType), preColName);
        } else {
            return String.format("alter table `%s`.`%s` add column `%s` %s after `%s`", dbName, tableName, newColName, getInsertDBColType(newColType), preColName);
        }
    }

    /**
     * Add column SQL
     */
    public static String generateAddColSql(String dbName, String tableName, String newColName, String newColType) {
        if (StringUtils.isEmpty(dbName)) {
            return String.format("alter table `%s` add column `%s` %s ", tableName, newColName, getInsertDBColType(newColType));
        } else {
            return String.format("alter table `%s`.`%s` add column `%s` %s ", dbName, tableName, newColName, getInsertDBColType(newColType));
        }
    }

    /**
     * @param dbName
     * @param tableName
     * @param fromColName
     * @param newColName
     * @return
     */
    public static String generateSetValueSql(String dbName, String tableName, String fromColName, String newColName) {
        if (StringUtils.isEmpty(dbName)) {
            return String.format("update `%s` set `%s` = `%s`", tableName, newColName, fromColName);
        } else {
            return String.format("update `%s`.`%s` set `%s` = `%s`", dbName, tableName, newColName, fromColName);
        }
    }

    /**
     * @param dbName
     * @param tableName
     * @param colName
     * @return
     */
    public static String generateSetCol2NullSql(String dbName, String tableName, String colName) {
        if (StringUtils.isEmpty(dbName)) {
            return String.format("update `%s` set `%s` = null", tableName, colName);
        } else {
            return String.format("update `%s`.`%s` set `%s` = null", dbName, tableName, colName);
        }
    }

    /**
     * Delete a row of SQL
     */
    public static String generateDeleteByPrimarySql(String dbName, String tableName, String primaryKey) {
        SQL sql = new SQL();
        if (StringUtils.isNotEmpty(dbName)) {
            sql.DELETE_FROM(String.format("`%s`.`%s`", dbName, tableName));
        } else {
            sql.DELETE_FROM(tableName);
        }
        sql.WHERE(JdbcConstants.PRIMARY_KEY + "=" + primaryKey);
        return sql.toString();
    }

    /**
     * merge sql
     *
     * @param dbName
     * @param tableName
     * @param colNames
     * @param newCol
     * @param split
     * @return
     */
    public static String generateMergeSql(String dbName, String tableName, List<String> colNames, String newCol, String split) {
        String updateSql = "update ";
        if (StringUtils.isNotEmpty(dbName)) {
            updateSql += String.format("`%s`.`%s` set `%s` = ", dbName, tableName, newCol);
        } else {
            updateSql += String.format("`%s` set `%s` = ", tableName, newCol);
        }
        updateSql += "concat(";
        List<String> concatCols = Lists.newArrayList();
        List<String> concatCols2 = Lists.newArrayList();
        for (String colName : colNames) {
            concatCols.add(String.format("ifnull(`%s`, '')", colName));
        }
        for (int i = 0; i < concatCols.size(); i++) {
            concatCols2.add(concatCols.get(i));
            if (i != (concatCols.size() - 1)) {
                concatCols2.add("'" + split + "'");
            }
        }
        String concatStr = String.join(",", concatCols2);
        updateSql += concatStr;
        updateSql += ")";
        return updateSql;
    }

    /**
     * split
     *
     * @param dbName
     * @param tableName
     * @param splitCol
     * @param left
     * @param right
     * @param split
     * @return
     */
    public static String generateSplitColSql(String dbName, String tableName, String splitCol, String left, String right, String split) {
        String updateSql = "update ";
        if (StringUtils.isNotEmpty(dbName)) {
            updateSql += String.format("`%s`.`%s` set `%s` = substring(`%s`, 1, case when (LOCATE('%s', `%s` ) - 1) = -1 then (length(`%s`)) else (LOCATE('%s',`%s` ) - 1) end),", dbName, tableName, left, splitCol, split, splitCol, splitCol, split, splitCol);
        } else {
            updateSql += String.format("`%s` set `%s` = substring(`%s`, 1, case when (LOCATE('%s', `%s` ) - 1) = -1 then (length(`%s`)) else (LOCATE('%s',`%s` ) - 1) end),", tableName, left, splitCol, split, splitCol, splitCol, split, splitCol);
        }
        updateSql += String.format("`%s` = substring(`%s`, LOCATE('%s',`%s`) + 1, length(`%s`)) ", right, splitCol, split, splitCol, splitCol);
        return updateSql;
    }

    /**
     * upper
     *
     * @param dbName
     * @param tableName
     * @param colName
     * @return
     */
    public static String generateToUpperSql(String dbName, String tableName, String colName) {
        String updateSql = "update ";
        if (StringUtils.isNotEmpty(dbName)) {
            updateSql += String.format("`%s`.`%s` set `%s` = upper(`%s`)", dbName, tableName, colName, colName);
        } else {
            updateSql += String.format("`%s` set `%s` = upper(`%s`)", tableName, colName, colName);
        }
        return updateSql;
    }

    /**
     * lower
     *
     * @param dbName
     * @param tableName
     * @param colName
     * @return
     */
    public static String generateToLowerSql(String dbName, String tableName, String colName) {
        String updateSql = "update ";
        if (StringUtils.isNotEmpty(dbName)) {
            updateSql += String.format("`%s`.`%s` set `%s` = lower(`%s`)", dbName, tableName, colName, colName);
        } else {
            updateSql += String.format("`%s` set `%s` = lower(`%s`)", tableName, colName, colName);
        }
        return updateSql;
    }

    /**
     * @param dbName
     * @param tableName
     * @param colName
     * @param prex
     * @return
     */
    public static String generateAddPrexSql(String dbName, String tableName, String colName, String prex) {
        String updateSql = "update ";
        if (StringUtils.isNotEmpty(dbName)) {
            updateSql += String.format("`%s`.`%s` set `%s` = concat('%s',`%s`)", dbName, tableName, colName, prex, colName);
        } else {
            updateSql += String.format("`%s` set `%s` = concat('%s',`%s`)", tableName, colName, prex, colName);
        }
        return updateSql;
    }

    /**
     * @param dbName
     * @param tableName
     * @param colName
     * @param suffix
     * @return
     */
    public static String generateAddSuffixSql(String dbName, String tableName, String colName, String suffix) {
        String updateSql = "update ";
        if (StringUtils.isNotEmpty(dbName)) {
            updateSql += String.format("`%s`.`%s` set `%s` = concat(`%s`,'%s')", dbName, tableName, colName, colName, suffix);
        } else {
            updateSql += String.format("`%s` set `%s` = concat(`%s`,'%s')", tableName, colName, colName, suffix);
        }
        return updateSql;
    }

    /**
     * replace
     *
     * @param dbName
     * @param tableName
     * @param colName
     * @param search
     * @param replace
     * @return
     */
    public static String generateReplaceSql(String dbName, String tableName, String colName, String search, String replace) {
        String updateSql = "update ";
        if (StringUtils.isNotEmpty(dbName)) {
            updateSql += String.format("`%s`.`%s` set `%s` = replace(`%s`,'%s', '%s')", dbName, tableName, colName, colName, search, replace);
        } else {
            updateSql += String.format("`%s` set `%s` = replace(`%s`,'%s', '%s')", tableName, colName, colName, search, replace);
        }
        return updateSql;
    }

    /**
     * Generate grouped SQL
     */
    public static String generateGroupBySql(String dbName, String tableName, QueryDataMappingGroupVO queryDataMappingGroupVO) {
        SQL sql = new SQL();
        sql.SELECT("`" + queryDataMappingGroupVO.getCurrentColName() + "`");
        if (StringUtils.isEmpty(dbName)) {
            sql.FROM(String.format("`%s`", tableName));
        } else {
            sql.FROM(String.format("`%s`.`%s`", dbName, tableName));
        }
        Map<String, QueryDataMappingDataVO.QuerySortFilter> querySortFilter = queryDataMappingGroupVO.getQuerySortFilter();
        if (querySortFilter != null) {
            Set<Map.Entry<String, QueryDataMappingDataVO.QuerySortFilter>> entries = querySortFilter.entrySet();
            for (Map.Entry<String, QueryDataMappingDataVO.QuerySortFilter> entry : entries) {
                String col = entry.getKey();
                sql.AND().WHERE(getWhere(col, entry.getValue()));
            }
        }
        sql.GROUP_BY("`" + queryDataMappingGroupVO.getCurrentColName() + "`");
        return sql.toString();
    }
}
