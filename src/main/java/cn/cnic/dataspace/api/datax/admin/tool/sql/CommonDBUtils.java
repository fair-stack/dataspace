package cn.cnic.dataspace.api.datax.admin.tool.sql;

import cn.cnic.dataspace.api.datax.admin.util.RetryUtil;
import cn.cnic.dataspace.api.datax.core.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Slf4j
public class CommonDBUtils {

    /**
     * Get direct JDBC connection
     * <p/>
     * if connecting failed, try to connect for MAX_TRY_TIMES times
     * <p/>
     * NOTE: In DataX, we don't need connection pool in fact
     */
    public static Connection getConnection(final DataBaseType dataBaseType, final String jdbcUrl, final String username, final String password, final int retryTimes) {
        return getConnection(dataBaseType, jdbcUrl, username, password, String.valueOf(Constants.SOCKET_TIMEOUT_INSECOND * 1000), retryTimes);
    }

    /**
     * @ param dataBaseType
     */
    public static Connection getConnection(final DataBaseType dataBaseType, final String jdbcUrl, final String username, final String password, final String socketTimeout, final int retryTimes) {
        try {
            return RetryUtil.executeWithRetry(new Callable<Connection>() {

                @Override
                public Connection call() throws Exception {
                    return CommonDBUtils.connect(dataBaseType, jdbcUrl, username, password, socketTimeout);
                }
            }, retryTimes, 1000L, true);
        } catch (Exception e) {
            throw new RuntimeException(String.format("数据库连接失败. 因为根据您配置的连接信息:%s获取数据库连接失败. 请检查您的配置并作出修改.", jdbcUrl), e);
        }
    }

    private static synchronized Connection connect(DataBaseType dataBaseType, String url, String user, String pass, String socketTimeout) {
        // Handling of OB10
        if (url.startsWith(Constants.OB10_SPLIT_STRING) && dataBaseType == DataBaseType.MySql) {
            String[] ss = url.split(Constants.OB10_SPLIT_STRING_PATTERN);
            if (ss.length != 3) {
                throw new RuntimeException("JDBC OB10格式错误，请联系askdatax");
            }
            log.info("this is ob1_0 jdbc url.");
            user = ss[1].trim() + ":" + user;
            url = ss[2];
            log.info("this is ob1_0 jdbc url. user=" + user + " :url=" + url);
        }
        Properties prop = new Properties();
        prop.put("user", user);
        prop.put("password", pass);
        if (dataBaseType == DataBaseType.Oracle) {
            // oracle.net.READ_TIMEOUT for jdbc versions < 10.1.0.5 oracle.jdbc.ReadTimeout for jdbc versions >=10.1.0.5
            // unit ms
            prop.put("oracle.jdbc.ReadTimeout", socketTimeout);
        }
        return connect(dataBaseType, url, prop);
    }

    private static synchronized Connection connect(DataBaseType dataBaseType, String url, Properties prop) {
        try {
            Class.forName(dataBaseType.getDriverClassName());
            DriverManager.setLoginTimeout(Constants.TIMEOUT_SECONDS);
            return DriverManager.getConnection(url, prop);
        } catch (Exception e) {
            e.printStackTrace();
            String errMsg = e.getMessage();
            /*if (errMsg.startsWith("Access denied for user")) {
                throw new RuntimeException("username or password not correct");
            } else if (errMsg.startsWith("Unknown database")) {
                throw new RuntimeException("database not exist");
            } else if (errMsg.startsWith("Communications link failure")) {
                throw new RuntimeException("connect fail, please check host or port");
            }*/
            throw new RuntimeException(errMsg);
        }
    }

    /**
     * a wrapped method to execute select-like sql statement .
     *
     * @param conn Database connection .
     * @param sql  sql statement to be executed
     * @return a {@link ResultSet}
     * @throws SQLException if occurs SQLException.
     */
    public static ResultSet query(Connection conn, String sql, int fetchSize) throws SQLException {
        // Default query timeout of 3600 seconds
        return query(conn, sql, fetchSize, Constants.SOCKET_TIMEOUT_INSECOND);
    }

    /**
     * a wrapped method to execute select-like sql statement .
     *
     * @param conn         Database connection .
     * @param sql          sql statement to be executed
     * @param fetchSize
     * @param queryTimeout unit:second
     * @return
     * @throws SQLException
     */
    public static ResultSet query(Connection conn, String sql, int fetchSize, int queryTimeout) throws SQLException {
        // make sure autocommit is off
        // conn.setAutoCommit(false);
        Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        stmt.setFetchSize(fetchSize);
        stmt.setQueryTimeout(queryTimeout);
        return query(stmt, sql);
    }

    /**
     * a wrapped method to execute select-like sql statement .
     *
     * @param stmt {@link Statement}
     * @param sql  sql statement to be executed
     * @return a {@link ResultSet}
     * @throws SQLException if occurs SQLException.
     */
    public static ResultSet query(Statement stmt, String sql) throws SQLException {
        return stmt.executeQuery(sql);
    }

    public static List<String> getTables(DataBaseType dataBaseType, String dbName, Connection conn) {
        ResultSet rs = null;
        String sql = null;
        List<String> tables = new ArrayList<>();
        if (dataBaseType == DataBaseType.MySql) {
            sql = "select table_name from information_schema.tables where table_schema='" + dbName + "'";
        } else if (dataBaseType == DataBaseType.PostgreSQL) {
            sql = "select tablename from pg_tables  where schemaname = 'public'";
        } else if (dataBaseType == DataBaseType.Oracle) {
            sql = "select t.table_name from user_tables t";
        } else if (dataBaseType == DataBaseType.SQLServer) {
            sql = "select name from sys.tables";
        } else {
            String msg = String.format("this method not implement DataBaseType {%s}", dataBaseType.getTypeName());
            throw new RuntimeException(msg);
        }
        try {
            rs = CommonDBUtils.query(conn, sql, 50, 2);
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CommonDBUtils.closeDBResources(rs, conn);
        }
        return new ArrayList<>(tables);
    }

    public static void executeSql(Connection connection, String sql) throws SQLException {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute(sql);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void executeSql(Connection connection, String sql, int queryTimeOut) throws SQLException {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.setQueryTimeout(queryTimeOut);
            statement.execute(sql);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void executeSqlWithoutResultSet(Connection connection, String sql) {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute(sql);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @return Left:ColumnName Middle:ColumnType Right:ColumnTypeName
     */
    public static Triple<List<String>, List<Integer>, List<String>> getColumnMetaData(Connection conn, String tableName, String column) {
        Statement statement = null;
        ResultSet rs = null;
        Triple<List<String>, List<Integer>, List<String>> columnMetaData = new ImmutableTriple<List<String>, List<Integer>, List<String>>(new ArrayList<String>(), new ArrayList<Integer>(), new ArrayList<String>());
        try {
            statement = conn.createStatement();
            String queryColumnSql = "select " + column + " from " + tableName + " where 1=2";
            rs = statement.executeQuery(queryColumnSql);
            ResultSetMetaData rsMetaData = rs.getMetaData();
            for (int i = 0, len = rsMetaData.getColumnCount(); i < len; i++) {
                columnMetaData.getLeft().add(rsMetaData.getColumnName(i + 1));
                columnMetaData.getMiddle().add(rsMetaData.getColumnType(i + 1));
                columnMetaData.getRight().add(rsMetaData.getColumnTypeName(i + 1));
            }
            return columnMetaData;
        } catch (SQLException e) {
            throw new RuntimeException(String.format("获取表:%s 的字段的元信息时失败. 请联系 DBA 核查该库、表信息.", tableName), e);
        } finally {
            CommonDBUtils.closeDBResources(rs, statement, null);
        }
    }

    public static void closeDBResources(Connection conn) {
        CommonDBUtils.closeDBResources(null, null, conn);
    }

    public static void closeDBResources(ResultSet rs, Connection conn) {
        CommonDBUtils.closeDBResources(rs, null, conn);
    }

    public static void closeDBResources(ResultSet rs, Statement stmt, Connection conn) {
        if (null != rs) {
            try {
                rs.close();
            } catch (SQLException unused) {
            }
        }
        if (null != stmt) {
            try {
                stmt.close();
            } catch (SQLException unused) {
            }
        }
        if (null != conn) {
            try {
                conn.close();
            } catch (SQLException unused) {
            }
        }
    }

    /**
     * Query table data
     */
    public static List<Map<String, Object>> getDatas(DataBaseType dataBaseType, Connection conn, String tableName, String page, String limit) throws SQLException {
        String sql = null;
        if (DataBaseType.MySql == dataBaseType) {
            sql = String.format("select * from %s limit %s,%s", tableName, page, limit);
        } else if (DataBaseType.PostgreSQL == dataBaseType) {
            sql = String.format("select * from %s limit %s offset %s", tableName, limit, "" + (Integer.valueOf(page) * Integer.valueOf(limit)));
        } else {
            throw new RuntimeException("not implement");
        }
        ResultSet resultSet = CommonDBUtils.query(conn, sql, 20);
        List<Map<String, Object>> retData = new ArrayList<>();
        int columnCount = resultSet.getMetaData().getColumnCount();
        while (resultSet.next()) {
            Map<String, Object> data = new HashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                String colName = resultSet.getMetaData().getColumnName(i);
                String colValue = resultSet.getString(i);
                data.put(colName, colValue);
            }
            retData.add(data);
        }
        return retData;
    }
}
