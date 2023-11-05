package cn.cnic.dataspace.api.datax.admin.tool.sql;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * refer:http://blog.csdn.net/ring0hx/article/details/6152528
 * <p/>
 */
public enum DataBaseType {

    MySql("mysql", "com.mysql.jdbc.Driver"),
    Oracle("oracle", "oracle.jdbc.OracleDriver"),
    SQLServer("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver"),
    PostgreSQL("postgresql", "org.postgresql.Driver"),
    DB2("db2", "com.ibm.db2.jcc.DB2Driver"),
    DRDS("drds", "com.mysql.jdbc.Driver"),
    Tddl("mysql", "com.mysql.jdbc.Driver"),
    RDBMS("rdbms", "cn.cnic.dataspace.api.datax.admin.tool.query.sql.DataBaseType"),
    HIVE("hive", "org.apache.hive.jdbc.HiveDriver"),
    Presto("presto", "com.facebook.presto.jdbc.PrestoDriver"),
    DM("dm", "dm.jdbc.driver.DmDriver");

    private String typeName;

    private String driverClassName;

    DataBaseType(String typeName, String driverClassName) {
        this.typeName = typeName;
        this.driverClassName = driverClassName;
    }

    public static DataBaseType retDataBaseType(String jdbcUrl) {
        String dataBaseTypeStr = jdbcUrl.split(":")[1];
        DataBaseType dataBaseType = null;
        if ("mysql".equals(dataBaseTypeStr)) {
            dataBaseType = DataBaseType.MySql;
        } else if ("postgresql".equals(dataBaseTypeStr)) {
            dataBaseType = DataBaseType.PostgreSQL;
        } else if ("oracle".equals(dataBaseTypeStr)) {
            dataBaseType = DataBaseType.Oracle;
        } else if ("sqlserver".equals(dataBaseTypeStr)) {
            dataBaseType = DataBaseType.SQLServer;
        }
        return dataBaseType;
    }

    public String getDriverClassName() {
        return this.driverClassName;
    }

    public String appendJDBCSuffixForReader(String jdbc) {
        String result = jdbc;
        String suffix = null;
        switch(this) {
            case MySql:
            case DRDS:
                suffix = "yearIsDateType=false&zeroDateTimeBehavior=convertToNull&tinyInt1isBit=false&rewriteBatchedStatements=true";
                if (jdbc.contains("?")) {
                    result = jdbc + "&" + suffix;
                } else {
                    result = jdbc + "?" + suffix;
                }
                break;
            case Oracle:
                break;
            case SQLServer:
                break;
            case DB2:
                break;
            case PostgreSQL:
                break;
            case RDBMS:
                break;
            default:
                throw new RuntimeException("unsupported database type.");
        }
        return result;
    }

    public String appendJDBCSuffixForWriter(String jdbc) {
        String result = jdbc;
        String suffix = null;
        switch(this) {
            case MySql:
                suffix = "yearIsDateType=false&zeroDateTimeBehavior=convertToNull&rewriteBatchedStatements=true&tinyInt1isBit=false";
                if (jdbc.contains("?")) {
                    result = jdbc + "&" + suffix;
                } else {
                    result = jdbc + "?" + suffix;
                }
                break;
            case DRDS:
                suffix = "yearIsDateType=false&zeroDateTimeBehavior=convertToNull";
                if (jdbc.contains("?")) {
                    result = jdbc + "&" + suffix;
                } else {
                    result = jdbc + "?" + suffix;
                }
                break;
            case Oracle:
                break;
            case SQLServer:
                break;
            case DB2:
                break;
            case PostgreSQL:
                break;
            case RDBMS:
                break;
            default:
                throw new RuntimeException("unsupported database type.");
        }
        return result;
    }

    public String quoteColumnName(String columnName) {
        String result = columnName;
        switch(this) {
            case MySql:
                result = "`" + columnName.replace("`", "``") + "`";
                break;
            case Oracle:
                break;
            case SQLServer:
                result = "[" + columnName + "]";
                break;
            case DB2:
            case PostgreSQL:
                break;
            default:
                throw new RuntimeException("unsupported database type");
        }
        return result;
    }

    public String quoteTableName(String tableName) {
        String result = tableName;
        switch(this) {
            case MySql:
                result = "`" + tableName.replace("`", "``") + "`";
                break;
            case Oracle:
                break;
            case SQLServer:
                break;
            case DB2:
                break;
            case PostgreSQL:
                break;
            default:
                throw new RuntimeException("unsupported database type");
        }
        return result;
    }

    private static Pattern mysqlPattern = Pattern.compile("jdbc:mysql://(.+):\\d+/.+");

    private static Pattern oraclePattern = Pattern.compile("jdbc:oracle:thin:@(.+):\\d+:.+");

    /**
     * Note: Currently, only IP information is recognized from MySQL/Oracle. If not recognized, null is returned
     */
    public static String parseIpFromJdbcUrl(String jdbcUrl) {
        Matcher mysql = mysqlPattern.matcher(jdbcUrl);
        if (mysql.matches()) {
            return mysql.group(1);
        }
        Matcher oracle = oraclePattern.matcher(jdbcUrl);
        if (oracle.matches()) {
            return oracle.group(1);
        }
        return null;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
}
