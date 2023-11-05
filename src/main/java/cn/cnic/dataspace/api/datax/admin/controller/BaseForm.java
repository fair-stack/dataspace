package cn.cnic.dataspace.api.datax.admin.controller;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.cnic.dataspace.api.datax.admin.util.PageUtils;
import cn.cnic.dataspace.api.datax.admin.util.ServletUtils;
import lombok.extern.slf4j.Slf4j;
import javax.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Basic parameter auxiliary class
 */
@Slf4j
public class BaseForm {

    /**
     * Query Parameter Object
     */
    protected Map<String, Object> values = new LinkedHashMap<>();

    /**
     * Current page number
     */
    private Long current = 1L;

    /**
     * Page size
     */
    private Long size = 10L;

    /**
     * Construction method
     */
    public BaseForm() {
        try {
            HttpServletRequest request = ServletUtils.getRequest();
            Enumeration<String> params = request.getParameterNames();
            while (params.hasMoreElements()) {
                String name = params.nextElement();
                String value = StrUtil.trim(request.getParameter(name));
                this.set(name, URLDecoder.decode(value, "UTF-8"));
            }
            this.parsePagingQueryParams();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("BaseControlForm initialize parameters setting error：" + e);
        }
    }

    /**
     * Get page number
     */
    public Long getPageNo() {
        String pageNum = StrUtil.toString(this.get("current"));
        if (!StrUtil.isEmpty(pageNum) && NumberUtil.isNumber(pageNum)) {
            this.current = Long.parseLong(pageNum);
        }
        return this.current;
    }

    /**
     * Get Page Size
     */
    public Long getPageSize() {
        String pageSize = StrUtil.toString(this.get("size"));
        if (StrUtil.isNotEmpty(pageSize) && NumberUtil.isNumber(pageSize) && !"null".equalsIgnoreCase(pageSize)) {
            this.size = Long.parseLong(pageSize);
        }
        return this.size;
    }

    /**
     * Obtain parameter information object
     */
    public Map<String, Object> getParameters() {
        return values;
    }

    /**
     * Obtain values from values based on the key
     */
    public Object get(String name) {
        if (values == null) {
            values = new LinkedHashMap<>();
            return null;
        }
        return this.values.get(name);
    }

    /**
     * Obtain String type values from values based on the key
     */
    public String getString(String key) {
        return StrUtil.toString(get(key));
    }

    /**
     * Get Sort Fields
     */
    public String getSort() {
        return StrUtil.toString(this.values.get("sort"));
    }

    /**
     * Get Sort
     */
    public String getOrder() {
        return StrUtil.toString(this.values.get("order"));
    }

    /**
     * Get Sort
     */
    public String getOrderby() {
        return StrUtil.toString(this.values.get("orderby"));
    }

    /**
     * Parse out mybatis plus pagination query parameters
     */
    public Page getPlusPagingQueryEntity() {
        Page page = new Page();
        // If there is no current, 1000 pieces of data will be returned by default
        page.setCurrent(this.getPageNo());
        page.setSize(this.getPageSize());
        if (ObjectUtil.isNotNull(this.get("ifCount"))) {
            page.setSearchCount(BooleanUtil.toBoolean(this.getString("ifCount")));
        } else {
            // Default to true
            page.setSearchCount(true);
        }
        return page;
    }

    /**
     * Parse page sorting parameters (pageHelper)
     */
    public void parsePagingQueryParams() {
        // Sort Field Parsing
        String orderBy = StrUtil.toString(this.get("orderby")).trim();
        String sortName = StrUtil.toString(this.get("sort")).trim();
        String sortOrder = StrUtil.toString(this.get("order")).trim().toLowerCase();
        if (StrUtil.isEmpty(orderBy) && !StrUtil.isEmpty(sortName)) {
            if (!sortOrder.equals("asc") && !sortOrder.equals("desc")) {
                sortOrder = "asc";
            }
            this.set("orderby", sortName + " " + sortOrder);
        }
    }

    /**
     * Set parameters
     */
    public void set(String name, Object value) {
        if (ObjectUtil.isNotNull(value)) {
            this.values.put(name, value);
        }
    }

    /**
     * Remove parameters
     */
    public void remove(String name) {
        this.values.remove(name);
    }

    /**
     * Clear all parameters
     */
    public void clear() {
        if (values != null) {
            values.clear();
        }
    }

    /**
     * Custom Query Assembly
     */
    protected QueryWrapper<?> pageQueryWrapperCustom(Map<String, Object> map, QueryWrapper<?> queryWrapper) {
        // Parameters related to mybatis plus pagination
        Map<String, Object> pageParams = PageUtils.filterPageParams(map);
        // Filter null values and query related parameters by page
        Map<String, Object> colQueryMap = PageUtils.filterColumnQueryParams(map);
        // Sort operation
        pageParams.forEach((k, v) -> {
            switch(k) {
                case "ascs":
                    queryWrapper.orderByAsc(StrUtil.toUnderlineCase(StrUtil.toString(v)));
                    break;
                case "descs":
                    queryWrapper.orderByDesc(StrUtil.toUnderlineCase(StrUtil.toString(v)));
                    break;
            }
        });
        // Traverse to assemble field query conditions
        colQueryMap.forEach((k, v) -> {
            switch(k) {
                case "pluginName":
                case "datasourceName":
                    queryWrapper.like(StrUtil.toUnderlineCase(k), v);
                    break;
                default:
                    queryWrapper.eq(StrUtil.toUnderlineCase(k), v);
            }
        });
        return queryWrapper;
    }
}
