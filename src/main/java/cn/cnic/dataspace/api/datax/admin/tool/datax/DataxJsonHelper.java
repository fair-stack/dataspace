package cn.cnic.dataspace.api.datax.admin.tool.datax;

import cn.cnic.dataspace.api.datax.admin.dto.*;
import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.tool.datax.reader.*;
import cn.cnic.dataspace.api.datax.admin.tool.datax.writer.*;
import cn.cnic.dataspace.api.datax.admin.tool.pojo.DataxHivePojo;
import cn.cnic.dataspace.api.datax.admin.tool.pojo.DataxMongoDBPojo;
import cn.cnic.dataspace.api.datax.admin.tool.pojo.DataxRdbmsPojo;
import cn.cnic.dataspace.api.datax.admin.util.JdbcConstants;
import cn.cnic.dataspace.api.datax.core.util.Constants;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static cn.cnic.dataspace.api.datax.admin.util.JdbcConstants.*;

/**
 * Build a tool class for com.wugui.datax json
 */
@Data
public class DataxJsonHelper implements DataxJsonInterface {

    /**
     * According to the datax example, the read table supports multiple tables (not to be considered for now, we will implement it later, let's save it with a list here)
     */
    private List<String> readerTables;

    /**
     * Read Field List
     */
    private List<String> readerColumns;

    /**
     * Reader jdbc data source
     */
    private JobDatasource readerDatasource;

    /**
     * Writer jdbc data source
     */
    private JobDatasource writerDatasource;

    /**
     * Table written
     */
    private List<String> writerTables;

    /**
     * List of fields written
     */
    private List<String> writerColumns;

    private Map<String, Object> buildReader;

    private Map<String, Object> buildWriter;

    private BaseDataxPlugin readerPlugin;

    private BaseDataxPlugin writerPlugin;

    private HiveReaderDto hiveReaderDto;

    private HiveWriterDto hiveWriterDto;

    private RdbmsReaderDto rdbmsReaderDto;

    private RdbmsWriterDto rdbmsWriterDto;

    private MongoDBReaderDto mongoDBReaderDto;

    private MongoDBWriterDto mongoDBWriterDto;

    // Used to save additional parameters
    private Map<String, Object> extraParams = Maps.newHashMap();

    public void initReader(DataXJsonBuildDto dataxJsonDto, JobDatasource readerDatasource) {
        this.readerDatasource = readerDatasource;
        this.readerTables = dataxJsonDto.getReaderTables();
        this.readerColumns = dataxJsonDto.getReaderColumns();
        this.hiveReaderDto = dataxJsonDto.getHiveReader();
        this.rdbmsReaderDto = dataxJsonDto.getRdbmsReader();
        this.mongoDBReaderDto = dataxJsonDto.getMongoDBReader();
        // Reader plugin
        String datasource = readerDatasource.getDatasource();
        // format
        this.readerColumns = convertKeywordsColumns(datasource, this.readerColumns);
        if (MYSQL.equals(datasource)) {
            readerPlugin = new MysqlReader();
            buildReader = buildReader();
        } else if (ORACLE.equals(datasource)) {
            readerPlugin = new OracleReader();
            buildReader = buildReader();
        } else if (SQL_SERVER.equals(datasource)) {
            readerPlugin = new SqlServerReader();
            buildReader = buildReader();
        } else if (POSTGRESQL.equals(datasource)) {
            readerPlugin = new PostgresqlReader();
            buildReader = buildReader();
        } else if (HIVE.equals(datasource)) {
            readerPlugin = new HiveReader();
            buildReader = buildHiveReader();
        } else if (MONGODB.equals(datasource)) {
            readerPlugin = new MongoDBReader();
            buildReader = buildMongoDBReader();
        } else if (DB2.equals(datasource)) {
            readerPlugin = new DB2Reader();
            buildReader = buildReader();
        }
    }

    public void initWriter(DataXJsonBuildDto dataxJsonDto, JobDatasource readerDatasource) {
        this.writerDatasource = readerDatasource;
        this.writerTables = dataxJsonDto.getWriterTables();
        this.writerColumns = dataxJsonDto.getWriterColumns();
        this.hiveWriterDto = dataxJsonDto.getHiveWriter();
        this.rdbmsWriterDto = dataxJsonDto.getRdbmsWriter();
        this.mongoDBWriterDto = dataxJsonDto.getMongoDBWriter();
        // writer
        String datasource = readerDatasource.getDatasource();
        this.writerColumns = convertKeywordsColumns(datasource, this.writerColumns);
        if (MYSQL.equals(datasource)) {
            writerPlugin = new MysqlWriter();
            buildWriter = this.buildWriter();
        } else if (ORACLE.equals(datasource)) {
            writerPlugin = new OraclelWriter();
            buildWriter = this.buildWriter();
        } else if (JdbcConstants.SQL_SERVER.equals(datasource)) {
            writerPlugin = new SqlServerlWriter();
            buildWriter = this.buildWriter();
        } else if (POSTGRESQL.equals(datasource)) {
            writerPlugin = new PostgresqllWriter();
            buildWriter = this.buildWriter();
        } else if (JdbcConstants.HIVE.equals(datasource)) {
            writerPlugin = new HiveWriter();
            buildWriter = this.buildHiveWriter();
        } else if (JdbcConstants.MONGODB.equals(datasource)) {
            writerPlugin = new MongoDBWriter();
            buildWriter = this.buildMongoDBWriter();
        } else if (DB2.equals(datasource)) {
            writerPlugin = new DB2Writer();
            buildWriter = this.buildWriter();
        }
    }

    private List<String> convertKeywordsColumns(String datasource, List<String> columns) {
        if (columns == null) {
            return null;
        }
        List<String> toColumns = new ArrayList<>();
        columns.forEach(s -> {
            toColumns.add(doConvertKeywordsColumn(datasource, s));
        });
        return toColumns;
    }

    private String doConvertKeywordsColumn(String dbType, String column) {
        if (column == null) {
            return null;
        }
        column = column.trim();
        column = column.replace("[", "");
        column = column.replace("]", "");
        column = column.replace("`", "");
        column = column.replace("\"", "");
        column = column.replace("'", "");
        switch(dbType) {
            case MYSQL:
                return String.format("`%s`", column);
            case SQL_SERVER:
                return String.format("[%s]", column);
            case POSTGRESQL:
            case ORACLE:
                return String.format("\"%s\"", column);
            default:
                return column;
        }
    }

    @Override
    public Map<String, Object> buildJob() {
        Map<String, Object> res = Maps.newLinkedHashMap();
        Map<String, Object> jobMap = Maps.newLinkedHashMap();
        jobMap.put("setting", buildSetting());
        jobMap.put("content", ImmutableList.of(buildContent()));
        res.put("job", jobMap);
        return res;
    }

    @Override
    public Map<String, Object> buildSetting() {
        Map<String, Object> res = Maps.newLinkedHashMap();
        Map<String, Object> speedMap = Maps.newLinkedHashMap();
        Map<String, Object> errorLimitMap = Maps.newLinkedHashMap();
        speedMap.putAll(ImmutableMap.of("channel", 3, "byte", 1048576));
        errorLimitMap.putAll(ImmutableMap.of("record", 0, "percentage", 0.02));
        res.put("speed", speedMap);
        res.put("errorLimit", errorLimitMap);
        return res;
    }

    @Override
    public Map<String, Object> buildContent() {
        Map<String, Object> res = Maps.newLinkedHashMap();
        res.put("reader", this.buildReader);
        res.put("writer", this.buildWriter);
        return res;
    }

    @Override
    public Map<String, Object> buildReader() {
        DataxRdbmsPojo dataxPluginPojo = new DataxRdbmsPojo();
        dataxPluginPojo.setJobDatasource(readerDatasource);
        dataxPluginPojo.setTables(readerTables);
        dataxPluginPojo.setRdbmsColumns(readerColumns);
        dataxPluginPojo.setSplitPk(rdbmsReaderDto.getReaderSplitPk());
        if (StringUtils.isNotBlank(rdbmsReaderDto.getQuerySql())) {
            dataxPluginPojo.setQuerySql(rdbmsReaderDto.getQuerySql());
        }
        // where
        if (StringUtils.isNotBlank(rdbmsReaderDto.getWhereParams())) {
            dataxPluginPojo.setWhereParam(rdbmsReaderDto.getWhereParams());
        }
        return readerPlugin.build(dataxPluginPojo);
    }

    @Override
    public Map<String, Object> buildHiveReader() {
        DataxHivePojo dataxHivePojo = new DataxHivePojo();
        dataxHivePojo.setJdbcDatasource(readerDatasource);
        List<Map<String, Object>> columns = Lists.newArrayList();
        readerColumns.forEach(c -> {
            Map<String, Object> column = Maps.newLinkedHashMap();
            column.put("index", c.split(Constants.SPLIT_SCOLON)[0]);
            column.put("type", c.split(Constants.SPLIT_SCOLON)[2]);
            columns.add(column);
        });
        dataxHivePojo.setColumns(columns);
        dataxHivePojo.setReaderDefaultFS(hiveReaderDto.getReaderDefaultFS());
        dataxHivePojo.setReaderFieldDelimiter(hiveReaderDto.getReaderFieldDelimiter());
        dataxHivePojo.setReaderFileType(hiveReaderDto.getReaderFileType());
        dataxHivePojo.setReaderPath(hiveReaderDto.getReaderPath());
        dataxHivePojo.setSkipHeader(hiveReaderDto.getReaderSkipHeader());
        return readerPlugin.buildHive(dataxHivePojo);
    }

    @Override
    public Map<String, Object> buildMongoDBReader() {
        DataxMongoDBPojo dataxMongoDBPojo = new DataxMongoDBPojo();
        dataxMongoDBPojo.setJdbcDatasource(readerDatasource);
        List<Map<String, Object>> columns = Lists.newArrayList();
        buildColumns(readerColumns, columns);
        dataxMongoDBPojo.setColumns(columns);
        dataxMongoDBPojo.setAddress(readerDatasource.getJdbcUrl());
        dataxMongoDBPojo.setDbName(readerDatasource.getDatabaseName());
        dataxMongoDBPojo.setReaderTable(readerTables.get(0));
        dataxMongoDBPojo.setQuery(mongoDBReaderDto.getQueryJson());
        return readerPlugin.buildMongoDB(dataxMongoDBPojo);
    }

    @Override
    public Map<String, Object> buildWriter() {
        DataxRdbmsPojo dataxPluginPojo = new DataxRdbmsPojo();
        dataxPluginPojo.setJobDatasource(writerDatasource);
        dataxPluginPojo.setTables(writerTables);
        dataxPluginPojo.setRdbmsColumns(writerColumns);
        dataxPluginPojo.setPreSql(rdbmsWriterDto.getPreSql());
        dataxPluginPojo.setPostSql(rdbmsWriterDto.getPostSql());
        return writerPlugin.build(dataxPluginPojo);
    }

    @Override
    public Map<String, Object> buildHiveWriter() {
        DataxHivePojo dataxHivePojo = new DataxHivePojo();
        dataxHivePojo.setJdbcDatasource(writerDatasource);
        List<Map<String, Object>> columns = Lists.newArrayList();
        writerColumns.forEach(c -> {
            Map<String, Object> column = Maps.newLinkedHashMap();
            column.put("name", c.split(Constants.SPLIT_SCOLON)[1]);
            column.put("type", c.split(Constants.SPLIT_SCOLON)[2]);
            columns.add(column);
        });
        dataxHivePojo.setColumns(columns);
        dataxHivePojo.setWriterDefaultFS(hiveWriterDto.getWriterDefaultFS());
        dataxHivePojo.setWriteFieldDelimiter(hiveWriterDto.getWriteFieldDelimiter());
        dataxHivePojo.setWriterFileType(hiveWriterDto.getWriterFileType());
        dataxHivePojo.setWriterPath(hiveWriterDto.getWriterPath());
        dataxHivePojo.setWriteMode(hiveWriterDto.getWriteMode());
        dataxHivePojo.setWriterFileName(hiveWriterDto.getWriterFileName());
        return writerPlugin.buildHive(dataxHivePojo);
    }

    @Override
    public Map<String, Object> buildMongoDBWriter() {
        DataxMongoDBPojo dataxMongoDBPojo = new DataxMongoDBPojo();
        dataxMongoDBPojo.setJdbcDatasource(writerDatasource);
        List<Map<String, Object>> columns = Lists.newArrayList();
        buildColumns(writerColumns, columns);
        dataxMongoDBPojo.setColumns(columns);
        dataxMongoDBPojo.setAddress(writerDatasource.getJdbcUrl());
        dataxMongoDBPojo.setDbName(writerDatasource.getDatabaseName());
        dataxMongoDBPojo.setWriterTable(readerTables.get(0));
        dataxMongoDBPojo.setUpsertInfo(mongoDBWriterDto.getUpsertInfo());
        return writerPlugin.buildMongoDB(dataxMongoDBPojo);
    }

    private void buildColumns(List<String> columns, List<Map<String, Object>> returnColumns) {
        columns.forEach(c -> {
            Map<String, Object> column = Maps.newLinkedHashMap();
            column.put("name", c.split(Constants.SPLIT_SCOLON)[0]);
            column.put("type", c.split(Constants.SPLIT_SCOLON)[1]);
            returnColumns.add(column);
        });
    }
}
