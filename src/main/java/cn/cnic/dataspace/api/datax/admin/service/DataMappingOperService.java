package cn.cnic.dataspace.api.datax.admin.service;

import cn.cnic.dataspace.api.datax.admin.dto.*;
import com.baomidou.mybatisplus.extension.api.R;
import java.util.List;
import java.util.Map;

/**
 * Structured Data Operations
 */
public interface DataMappingOperService {

    /**
     * Single point update data
     */
    R<Boolean> updateData(String spaceId, String userId, Long dataMappingId, String primaryKeyVal, String col, String data);

    /**
     * Modify column names or types
     */
    R<Boolean> alterColNameAndType(String spaceId, String currentUserId, Long dataMappingId, String oldColName, String newColName, String type);

    /**
     * Delete Column
     */
    R<Boolean> dropCol(String spaceId, String currentUserId, DropColVO dropColVO);

    /**
     * Add one or more columns
     */
    R<Boolean> addEmptyCol(String spaceId, String currentUserId, AddEmptyColVO addEmptyColVO);

    /**
     * Add a row of data
     */
    R<Boolean> addLine(String spaceId, String userId, AddLineVo addLineVo);

    /**
     * Copy one or more rows of data
     */
    R<Boolean> copyLine(String spaceId, String userId, CopyLineVO copyLineVO);

    /**
     * Copy n columns
     */
    R<Boolean> copyAddCol(String spaceId, String currentUserId, CopyAddColVO copyAddColVO);

    /**
     * Clear n columns
     */
    R<Boolean> setCol2Null(String spaceId, String currentUserId, SetCol2NullVO setCol2NullVO);

    /**
     * Delete a row of data
     */
    R<Boolean> deleteLine(String spaceId, String userId, DeleteLineVO deleteLineVO);

    /**
     * Merge multiple columns into one column
     */
    R<Boolean> mergeCol(String spaceId, String currentUserId, MergeColVO mergeColVO);

    /**
     * Split one column into two columns
     */
    R<Boolean> split(String spaceId, String currentUserId, Long dataMappingId, String splitCol, String split, String left, String right);

    /**
     * Full text search
     */
    R<Map<String, Object>> searchData(String spaceId, String userId, String dataMappingId, String searchVal, Integer current, Integer size);

    /**
     * Capitalize
     */
    R<Boolean> toUpper(String spaceId, String currentUserId, Long dataMappingId, String colName);

    /**
     * Convert to lowercase
     */
    R<Boolean> toLower(String spaceId, String currentUserId, Long dataMappingId, String colName);

    /**
     * Add Prefix
     */
    R<Boolean> addPrex(String spaceId, String currentUserId, Long dataMappingId, String colName, String prex);

    /**
     * Add suffix
     */
    R<Boolean> addSuffix(String spaceId, String currentUserId, Long dataMappingId, String colName, String suffix);

    /**
     * Replace value
     */
    R<Boolean> replace(String spaceId, String currentUserId, Long dataMappingId, String colName, String search, String replace);

    R<List<Map<String, String>>> getGroupVal(String spaceId, String currentUserId, QueryDataMappingGroupVO queryDataMappingGroupVO);
}
