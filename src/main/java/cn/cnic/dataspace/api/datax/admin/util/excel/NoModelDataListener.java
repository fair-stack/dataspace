package cn.cnic.dataspace.api.datax.admin.util.excel;

import cn.cnic.dataspace.api.datax.admin.entity.ImportExcelTask;
import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.mapper.ImportExcelTaskMapper;
import cn.cnic.dataspace.api.datax.admin.tool.database.ColumnInfo;
import cn.cnic.dataspace.api.datax.admin.tool.database.TableInfo;
import cn.cnic.dataspace.api.datax.admin.tool.sql.CommonDBUtils;
import cn.cnic.dataspace.api.datax.admin.tool.sql.JdbcConnectionFactory;
import cn.cnic.dataspace.api.datax.admin.tool.sql.SqlUtils;
import cn.cnic.dataspace.api.datax.admin.util.JdbcConstants;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSON;
import com.beust.jcommander.internal.Lists;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Directly receiving data using map
 */
@Slf4j
public class NoModelDataListener extends AnalysisEventListener<Map<Integer, String>> {

    /**
     * Store the database every 5 items, and in actual use, up to 100 items can be stored. Then, clear the list to facilitate memory reclamation
     */
    private static final int BATCH_COUNT = 100;

    private List<Object[]> cacheDataList = Lists.newArrayList(BATCH_COUNT);

    private List<String> head = Lists.newArrayList();

    private String dbName;

    private String tableName;

    private String isHeader;

    private JdbcTemplate jdbcTemplate;

    private HikariDataSource dataSource;

    private boolean isSetHeader = false;

    private boolean isCreateTable = false;

    public NoModelDataListener(String dbName, String tableName, String isHeader) {
        this.dbName = dbName;
        this.tableName = tableName;
        this.isHeader = isHeader;
        this.dataSource = getDataSource();
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> data, AnalysisContext context) {
        log.info("解析到一条头数据:{}", JSON.toJSONString(data));
        String[] header = getDataArr(data);
        head.clear();
        head.addAll(Lists.newArrayList(header));
        // Create a table based on the actual column in Excel
        createTable();
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        // Log. info ("Parse to a piece of data: {}", JSON. toJSONString (data));
        // Using the first column to create a table that is longer than the first column will result in data loss
        if ("0".equals(isHeader)) {
            if (!isSetHeader) {
                // Create a table using the longest column in the first column
                Integer maxCol = data.keySet().stream().max(Integer::compare).get();
                List<String> columnByIntNum = getColumnByIntNum(maxCol + 1);
                head.clear();
                head.addAll(columnByIntNum);
                isSetHeader = true;
                // Create Excel according to the length of the first column in Excel
                createTable();
            }
        }
        String[] dataArr = getDataArr(data);
        // Intercept the head length data, otherwise the length of the fields inserted into the database is inconsistent
        // Data longer than the first column will be lost
        String[] splitArr = Arrays.copyOf(dataArr, head.size());
        cacheDataList.add(splitArr);
        if (cacheDataList.size() >= BATCH_COUNT) {
            saveData();
            cacheDataList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
        log.info("所有数据解析完成！");
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (Exception ignore) {
                log.error("datasource close failed");
                log.error(ignore.getMessage(), ignore);
            }
        }
    }

    /**
     * get sql dataSource
     *
     * @return
     */
    private HikariDataSource getDataSource() {
        JobDatasource jobDatasource = new JdbcConnectionFactory(dbName).getDataSource();
        // The default here is to use the hikari data source
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setUsername(jobDatasource.getJdbcUsername());
        dataSource.setPassword(jobDatasource.getJdbcPassword());
        dataSource.setJdbcUrl(jobDatasource.getJdbcUrl());
        dataSource.setDriverClassName(jobDatasource.getJdbcDriverClass());
        dataSource.setMaximumPoolSize(1);
        dataSource.setMinimumIdle(0);
        dataSource.setConnectionTimeout(30000);
        return dataSource;
    }

    /**
     * Plus storage database
     */
    private void saveData() {
        log.info("{}条数据，开始存储数据库！", cacheDataList.size());
        if (!isCreateTable) {
            throw new RuntimeException("没有建表,可能因为第一行数据为空或者存在空数据");
        }
        String insertSql = SqlUtils.generateInsertSql(tableName, head);
        try {
            jdbcTemplate.batchUpdate(insertSql, cacheDataList);
            log.info("存储数据库成功！");
        } catch (DataAccessException e) {
            log.error("存储数据库失败，执行insert sql失败");
            log.error(e.getMessage(), e);
            if (dataSource != null) {
                try {
                    dataSource.close();
                } catch (Exception ignore) {
                    log.error("存储数据库失败,执行insert sql失败," + ignore.getMessage(), ignore);
                }
            }
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Create a table based on the head
     */
    private void createTable() {
        TableInfo tableInfo = new TableInfo();
        tableInfo.setName(tableName);
        List<String> newHead = new ArrayList<>();
        for (int i = 0; i < head.size(); i++) {
            String col = head.get(i);
            if (StringUtils.isEmpty(col)) {
                String s = getRandomStringByLength(4);
                head.set(i, JdbcConstants.COL_PRE + s);
            } else {
                if (JdbcConstants.PRIMARY_KEY.equals(col)) {
                    String s = getRandomStringByLength(4);
                    head.set(i, JdbcConstants.COL_PRE + s);
                } else {
                    if (newHead.contains(col)) {
                        String s = getRandomStringByLength(4);
                        head.set(i, JdbcConstants.COL_PRE + s);
                    } else {
                        newHead.add(col);
                    }
                }
            }
        }
        List<ColumnInfo> columnInfos = head.stream().map(var -> {
            ColumnInfo col = new ColumnInfo();
            col.setName(var);
            return col;
        }).collect(Collectors.toList());
        tableInfo.setColumns(columnInfos);
        String createTableSql = SqlUtils.generateCreateTableSqlWithDefaultId(dbName, tableInfo);
        Connection connection = null;
        try {
            connection = new JdbcConnectionFactory(dbName).getConnection();
            CommonDBUtils.executeSql(connection, createTableSql);
            isCreateTable = true;
        } catch (SQLException e) {
            log.error("建表失败");
            log.error(e.getMessage(), e);
            if (dataSource != null) {
                try {
                    dataSource.close();
                } catch (Exception ignore) {
                    log.error(ignore.getMessage(), ignore);
                }
            }
            throw new RuntimeException("建表失败," + e.getMessage(), e);
        } finally {
            CommonDBUtils.closeDBResources(connection);
        }
    }

    /**
     * Take out the values stored in the map and convert them into arrays
     */
    private String[] getDataArr(Map<Integer, String> data) {
        Integer maxCol = data.keySet().stream().max(Integer::compare).get();
        String[] dataArr = new String[maxCol + 1];
        Set<Map.Entry<Integer, String>> entries = data.entrySet();
        for (Map.Entry<Integer, String> entry : entries) {
            Integer key = entry.getKey();
            String value = entry.getValue();
            dataArr[key] = value;
        }
        return dataArr;
    }

    /**
     * Obtain random English lowercase strings
     */
    public static String getRandomStringByLength(int length) {
        return RandomStringUtils.randomAlphabetic(length).toLowerCase(Locale.ROOT);
    }

    public static List<String> getColumnByIntNum(int num) {
        List<String> cols = Lists.newArrayList();
        for (int i = 1; i <= num; i++) {
            cols.add(getStringByNumber(i));
        }
        return cols;
    }

    private static String getStringByNumber(int num) {
        StringBuilder sb = new StringBuilder();
        int m = num;
        int n = 0;
        while (m > 0) {
            n = (m - 1) % 26;
            m = (m - 1) / 26;
            sb.append(getCharByNum(n));
        }
        String s = sb.reverse().toString();
        return s;
    }

    private static char getCharByNum(int num) {
        return (char) (65 + num);
    }
}
