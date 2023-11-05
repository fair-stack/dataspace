package cn.cnic.dataspace.api.datax.admin.controller;

import cn.cnic.dataspace.api.datax.admin.aop.HasSpacePermission;
import cn.cnic.dataspace.api.datax.admin.service.DatasourceQueryService;
import cn.cnic.dataspace.api.datax.admin.tool.database.TableInfo;
import com.baomidou.mybatisplus.extension.api.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Query database table names and controllers for fields
 */
@RestController
@RequestMapping("/metadata")
@Api(tags = "jdbc数据库查询控制器")
@HasSpacePermission
public class MetadataController extends BaseController {

    @Autowired
    private DatasourceQueryService datasourceQueryService;

    /**
     * Obtain the mongo database name based on the data source ID
     */
    @GetMapping("/getDBs")
    @ApiOperation("根据数据源id获取mongo库名")
    public R<List<String>> getDBs(Long datasourceId) throws IOException {
        return success(datasourceQueryService.getDBs(datasourceId));
    }

    /**
     * Obtain CollectionNames based on the data source ID and dbname
     */
    @PostMapping("/collectionNames")
    @ApiOperation("根据数据源id,dbname获取CollectionNames")
    public R<List<String>> getCollectionNames(Long datasourceId, String dbName) throws IOException {
        return success(datasourceQueryService.getCollectionNames(datasourceId, dbName));
    }

    /**
     * Obtain PG table schema
     */
    @PostMapping("/getDBSchema")
    @ApiOperation("根据数据源id获取 db schema")
    public R<List<String>> getTableSchema(Long datasourceId) {
        return success(datasourceQueryService.getTableSchema(datasourceId));
    }

    /**
     * Obtain available table names based on the data source ID
     */
    @GetMapping("/getTables")
    @ApiOperation("根据数据源id获取可用表名")
    public R<List<String>> getTableNames(Long datasourceId, String tableSchema) throws IOException {
        return success(datasourceQueryService.getTables(datasourceId, tableSchema));
    }

    /**
     * Obtain available table names based on the data source ID
     */
    @GetMapping("/getTableNameAndDataCount")
    @ApiOperation("根据数据源id获取可用表名")
    public R getTableNameAndDataCount(Long datasourceId, String tableSchema) throws IOException {
        List<Map<String, Object>> ret = datasourceQueryService.getTableNameAndDataCount(datasourceId, tableSchema);
        return success(ret);
    }

    /**
     * Obtain all fields based on the data source ID and table name
     */
    @PostMapping("/getColumns")
    @ApiOperation("根据数据源id和表名获取所有字段")
    public R<List<String>> getColumns(Long datasourceId, String tableName) throws IOException {
        return success(datasourceQueryService.getColumns(datasourceId, tableName));
    }

    /**
     * Obtain all fields based on the data source ID and SQL statement
     */
    @PostMapping("/getColumnsByQuerySql")
    @ApiOperation("根据数据源id和sql语句获取所有字段")
    public R<List<String>> getColumnsByQuerySql(Long datasourceId, String querySql) throws SQLException {
        return success(datasourceQueryService.getColumnsByQuerySql(datasourceId, querySql));
    }

    /**
     * Obtain the top 10 data items based on the data source ID and tableName
     */
    @PostMapping("/getData")
    @ApiOperation("根据数据源id和tableName获取前10条数据")
    public R<List<Map<String, Object>>> getData(Long datasourceId, String tableName) throws SQLException, IOException {
        List<Map<String, Object>> data = datasourceQueryService.getDatas(datasourceId, tableName);
        return success(data);
    }

    @ApiOperation("构建TableInfo")
    @PostMapping("/getTableInfo")
    public R<TableInfo> getTableInfo(HttpServletRequest request, Long dataSourceId, String tableName) throws IOException {
        return datasourceQueryService.getTableInfo(dataSourceId, tableName);
    }
}
