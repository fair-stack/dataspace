package cn.cnic.dataspace.api.datax.admin.util.excel;

import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.tool.sql.JdbcConnectionFactory;
import cn.cnic.dataspace.api.datax.admin.tool.sql.SqlUtils;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSON;
import com.beust.jcommander.internal.Lists;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Directly receiving data using map
 */
@Slf4j
public class NoModelDataListener2Increment extends AnalysisEventListener<Map<Integer, String>> {

    /**
     * Store the database every 5 items, and in actual use, up to 100 items can be stored. Then, clear the list to facilitate memory reclamation
     */
    private static final int BATCH_COUNT = 100;

    private List<Object[]> cacheDataList = Lists.newArrayList(BATCH_COUNT);

    private List<String> columns;

    private String dbName;

    private String tableName;

    private JdbcTemplate jdbcTemplate;

    private HikariDataSource dataSource;

    public NoModelDataListener2Increment(String dbName, String tableName, List<String> columns) {
        this.dbName = dbName;
        this.tableName = tableName;
        this.dataSource = getDataSource();
        this.columns = columns;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> data, AnalysisContext context) {
        log.info("解析到一条头数据:{}", JSON.toJSONString(data));
    }

    @Override
    public void invoke(Map<Integer, String> data, AnalysisContext context) {
        String[] dataArr = getDataArr(data);
        // Intercept the head length data, otherwise the length of the fields inserted into the database is inconsistent
        // Data longer than the first column will be lost
        String[] splitArr = Arrays.copyOf(dataArr, columns.size());
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
        String insertSql = SqlUtils.generateInsertSql(tableName, columns);
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
}
