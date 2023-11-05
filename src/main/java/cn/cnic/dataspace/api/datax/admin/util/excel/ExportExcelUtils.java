package cn.cnic.dataspace.api.datax.admin.util.excel;

import cn.cnic.dataspace.api.datax.admin.tool.sql.CommonDBUtils;
import cn.cnic.dataspace.api.datax.admin.tool.sql.JdbcConnectionFactory;
import cn.cnic.dataspace.api.datax.admin.tool.sql.SqlUtils;
import cn.cnic.dataspace.api.datax.admin.util.JdbcConstants;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import groovy.lang.Tuple2;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ExportExcelUtils {

    private static final int SIZE = 10000;

    // first row is header
    private static final int EXCEL_MAX_ROW = 1048575 - 1;

    /**
     * Add All to Memory Export
     */
    public static void exportAll(String targetPath, OutputStream outputStream, String dbName, String tableName) {
        List<List<String>> lines = new ArrayList<>();
        Connection connection = null;
        ResultSet resultSet = null;
        try {
            connection = new JdbcConnectionFactory(dbName).getConnection();
            String selectSqlWithAll = SqlUtils.generateSelectSqlWithAll(dbName, tableName);
            resultSet = CommonDBUtils.query(connection, selectSqlWithAll, 100);
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String> header = new ArrayList<>();
            for (int i = 0; i < columnCount; i++) {
                String columnLabel = metaData.getColumnLabel(i + 1);
                if (!columnLabel.equals((JdbcConstants.PRIMARY_KEY))) {
                    header.add(columnLabel);
                }
            }
            lines.add(header);
            while (resultSet.next()) {
                List<String> line = new ArrayList<>();
                for (String colLabel : header) {
                    line.add(resultSet.getString(colLabel));
                }
                lines.add(line);
                if (lines.size() > EXCEL_MAX_ROW) {
                    log.error("get data row greater than " + EXCEL_MAX_ROW);
                    break;
                }
            }
            // max row 1048575
            if (lines.size() > EXCEL_MAX_ROW) {
                lines = lines.subList(0, EXCEL_MAX_ROW);
            }
            if (StringUtils.isNotEmpty(targetPath)) {
                EasyExcel.write(targetPath).sheet("Sheet1").doWrite(lines);
            } else {
                if (outputStream == null) {
                    throw new RuntimeException("outputStream is null");
                }
                EasyExcel.write(outputStream).sheet("Sheet1").doWrite(lines);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            CommonDBUtils.closeDBResources(connection);
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Batch export data to Excel
     */
    public static void export(String targetPath, OutputStream outputStream, String dbName, String tableName) {
        List<List<String>> lines = new ArrayList<>();
        Connection connection = null;
        ExcelWriter excelWriter = null;
        ResultSet resultSet = null;
        try {
            if (StringUtils.isNotEmpty(targetPath)) {
                excelWriter = EasyExcel.write(targetPath).build();
            } else {
                if (outputStream == null) {
                    throw new RuntimeException("outputStream is null");
                }
                excelWriter = EasyExcel.write(outputStream).build();
            }
            WriteSheet sheet1 = EasyExcel.writerSheet("Sheet1").build();
            connection = new JdbcConnectionFactory(dbName).getConnection();
            // split
            long dataCount = selectDataCount(connection, dbName, tableName);
            if (dataCount > EXCEL_MAX_ROW) {
                // excel max row 1048575
                log.error("get data row greater than " + EXCEL_MAX_ROW);
                dataCount = EXCEL_MAX_ROW;
            }
            List<Tuple2<Long, Long>> split = split(dataCount, SIZE);
            String selectSqlNoResult = SqlUtils.generateSelectSqlNoResult(dbName, tableName);
            resultSet = CommonDBUtils.query(connection, selectSqlNoResult, 100);
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            List<String> header = new ArrayList<>();
            for (int i = 0; i < columnCount; i++) {
                String columnLabel = metaData.getColumnLabel(i + 1);
                if (!columnLabel.equals((JdbcConstants.PRIMARY_KEY))) {
                    header.add(columnLabel);
                }
            }
            lines.add(header);
            resultSet.close();
            // Write once first, only write to the header if there is no data
            excelWriter.write(lines, sheet1);
            // Paging Query Data
            for (Tuple2<Long, Long> longLongTuple2 : split) {
                lines.clear();
                String selectSqlWithPaging = SqlUtils.generateSelectSqlWithPaging(dbName, tableName, longLongTuple2.getV1(), longLongTuple2.getV2());
                resultSet = CommonDBUtils.query(connection, selectSqlWithPaging, 100);
                while (resultSet.next()) {
                    List<String> line = new ArrayList<>();
                    for (String colLabel : header) {
                        line.add(resultSet.getString(colLabel));
                    }
                    lines.add(line);
                }
                resultSet.close();
                // Write data to Excel
                excelWriter.write(lines, sheet1);
            }
        } catch (SQLException e) {
            log.error("执行查询sql失败");
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            excelWriter.finish();
            CommonDBUtils.closeDBResources(connection);
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    private static long selectDataCount(Connection connection, String dbName, String tableName) {
        String countSql = SqlUtils.generateSelectCountSql(dbName, tableName);
        ResultSet resultSet = null;
        long count = 0;
        try {
            resultSet = CommonDBUtils.query(connection, countSql, 1);
            while (resultSet.next()) {
                count = resultSet.getLong(1);
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        } finally {
            CommonDBUtils.closeDBResources(resultSet, null);
        }
        return count;
    }

    // split  max count 1048575
    private static List<Tuple2<Long, Long>> split(long count, int pageSize) {
        List<Tuple2<Long, Long>> splits = new ArrayList<>();
        if (count == 0 || pageSize == 0) {
            return splits;
        }
        long pageCount = count / pageSize + 1;
        for (int i = 1; i <= pageCount; i++) {
            long start = pageSize * (i - 1);
            long limit = SIZE;
            if ((start + pageSize) > EXCEL_MAX_ROW) {
                limit = count - start;
            }
            Tuple2<Long, Long> split = Tuple2.tuple(start, limit);
            splits.add(split);
        }
        return splits;
    }
}
