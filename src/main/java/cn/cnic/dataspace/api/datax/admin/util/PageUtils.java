package cn.cnic.dataspace.api.datax.admin.util;

import cn.hutool.core.lang.Filter;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;

/**
 * Paging tool class
 */
public class PageUtils {

    /**
     * Used to save keywords used for pagination queries
     */
    public static final List<String> PAGE_QUERY_KEY_LIST = ImmutableList.of("current", "size", "sortBy", "orderby", "order", "sort", "ifCount", "ascs", "descs");

    /**
     * Filter parameters, null values, etc. of pageHelper
     */
    public static Map<String, Object> filterColumnQueryParams(Map<String, Object> map) {
        return MapUtil.filter(map, (Filter<Map.Entry<String, Object>>) e -> {
            if (StrUtil.isBlank(StrUtil.toString(e.getValue()))) {
                return false;
            }
            if (PAGE_QUERY_KEY_LIST.contains(e.getKey())) {
                return false;
            }
            return true;
        });
    }

    /**
     * Return the parameters used by pageHelper
     */
    public static Map<String, Object> filterPageParams(Map<String, Object> map) {
        return MapUtil.filter(map, (Filter<Map.Entry<String, Object>>) e -> {
            if (StrUtil.isBlank(StrUtil.toString(e.getValue()))) {
                return false;
            }
            if (PAGE_QUERY_KEY_LIST.contains(e.getKey())) {
                return true;
            }
            return false;
        });
    }
}
