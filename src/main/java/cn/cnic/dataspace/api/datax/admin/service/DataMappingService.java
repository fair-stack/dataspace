package cn.cnic.dataspace.api.datax.admin.service;

import cn.cnic.dataspace.api.datax.admin.dto.DataMappingDto;
import cn.cnic.dataspace.api.datax.admin.entity.DataMappingMeta;
import cn.cnic.dataspace.api.datax.admin.dto.ImportFromDataSourceVo;
import cn.cnic.dataspace.api.datax.admin.dto.QueryDataMappingDataVO;
import cn.cnic.dataspace.api.datax.admin.entity.DataMapping;
import cn.cnic.dataspace.api.util.Token;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.api.R;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DataMappingService extends IService<DataMapping> {

    /**
     * Delete structured data
     */
    R<Boolean> delete(String spaceId, String userId, String dataMappingId);

    /**
     * Copying structured data
     */
    R<Boolean> copy(String spaceId, String currentUserId, Long dataMappingId, String newName);

    /**
     * Structured data renaming
     */
    R<Boolean> rename(String spaceId, String userId, String dataMappingId, String newName);

    /**
     * Is the updated structured data publicly available
     */
    R<Boolean> updatePublic(String spaceId, String userId, String dataMappingId, String isPublic);

    /**
     * Import Excel locally
     */
    R<Boolean> importExcel(String spaceId, String userId, MultipartFile file, String isHeader, String sheetName, Integer sheetNum);

    /**
     * Export Excel to local
     */
    void exportExcel(HttpServletResponse response, String spaceId, String userId, Long dataMappingId) throws IOException;

    /**
     * Export Excel to space
     */
    R<Boolean> exportExcelToSpace(HttpServletRequest request, String spaceId, Token currentUser, Long dataMappingId, String hash);

    /**
     * Import Excel from space
     */
    R<Boolean> importExcelFromSpaceFile(HttpServletRequest request, String spaceId, String userId, String hash, String isHeader, String sheetName, Integer sheetNum, int fromType);

    /**
     * Incremental Import to Excel
     */
    R<Boolean> incrementImportExcelFromFile(HttpServletRequest request, String spaceId, String userId, String hash, String isHeader, String sheetName, int fromType, Long dataMappingId);

    /**
     * Query structured data
     */
    R<Map<String, Object>> getData(String spaceId, String userId, String dataMappingId, Integer isReturnId, Integer current, Integer size);

    /**
     */
    R<Map<String, Object>> getDataBySortAndFilter(String spaceId, String userId, QueryDataMappingDataVO queryDataMappingDataVO);

    /**
     * Query structured data schema
     */
    R<Map<String, Object>> getSchema(String spaceId, String userId, String dataMappingId);

    /**
     * Paging query structured data list
     */
    IPage<DataMappingDto> selectByPage(String spaceId, String userId, String name, Integer current, Integer size);

    /**
     * Query structured data list
     */
    List<DataMappingDto> selectAll(String spaceId, String currentUserId, String name);

    /**
     * Query structured data list and corresponding data volume
     */
    List<DataMappingDto> selectAllWithDataCount(String spaceId, String currentUserId, String name);

    /**
     * Query basic information of structured data
     */
    R<DataMappingDto> getBasicInfo(String spaceId, Long dataMappingId);

    /**
     * Obtain all sheetNames for space Excel files
     */
    R<List<String>> getSheetNames(HttpServletRequest request, String spaceId, String hash);

    /**
     * Read the sheetName of the file
     */
    R<Map<String, Object>> getSheetNames(HttpServletRequest request, String spaceId, MultipartFile file);

    /**
     * Configure Import from Data Source
     */
    R<Boolean> importFromDataSource(String spaceId, String currentUserId, ImportFromDataSourceVo importFromDataSourceVo);

    /**
     * Update Import from Data Source
     */
    R<Boolean> updateImportFromDataSource(String spaceId, String currentUserId, ImportFromDataSourceVo importFromDataSourceVo);

    /**
     * Query data source access task information
     */
    R<ImportFromDataSourceVo> getImportFromDataSource(String spaceId, String currentUserId, Long dataMappingId);

    /**
     */
    R<Boolean> stopImportFromDataSource(String spaceId, String currentUserId, Long dataMappingId);

    /**
     */
    R<Boolean> startImportFromDataSource(String spaceId, String currentUserId, Long dataMappingId);

    /**
     */
    R<Boolean> triggerImportFromDataSource(String spaceId, String currentUserId, Long dataMappingId);

    R<Map<String, Object>> getStaInfo(String spaceId);

    R<DataMappingMeta> getColumnMeta(String currentUserId, String spaceId, String dataMappingId);

    void setColumnMeta(String currentUserId, String spaceId, DataMappingMeta dataMappingMeta);

    void updateColumnMeta(String currentUserId, String spaceId, DataMappingMeta dataMappingMeta);
}
