package cn.cnic.dataspace.api.datax.admin.tool.datax.dto;

import cn.cnic.dataspace.api.datax.admin.dto.ImportFromDataSourceVo;
import cn.cnic.dataspace.api.datax.admin.dto.MongoDBReaderDto;
import cn.cnic.dataspace.api.datax.admin.mapper.JobDatasourceMapper;
import cn.cnic.dataspace.api.datax.admin.tool.database.ColumnInfo;
import cn.cnic.dataspace.api.datax.core.util.Constants;
import com.beust.jcommander.internal.Lists;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;

public class BuildMongoDBDto extends BuildDto {

    public BuildMongoDBDto(ImportFromDataSourceVo importFromDataSourceVo, JobDatasourceMapper jobDatasourceMapper) {
        super(importFromDataSourceVo, jobDatasourceMapper);
    }

    @Override
    void setReaderColumns() {
        int writerColSize = importFromDataSourceVo.getWriterColumns().size();
        List<ColumnInfo> readerColumns = null;
        // If the reader column is longer than the writer column, truncate the same length column
        if (writerColSize > importFromDataSourceVo.getReaderColumns().size()) {
            readerColumns = importFromDataSourceVo.getReaderColumns();
        } else {
            readerColumns = importFromDataSourceVo.getReaderColumns().subList(0, writerColSize);
        }
        List<String> collect = readerColumns.stream().map(var -> var.getName() + Constants.SPLIT_SCOLON + var.getType()).collect(Collectors.toList());
        dataXJsonBuildDto.setReaderColumns(collect);
    }

    @Override
    void buildMongoDBReader() {
        MongoDBReaderDto mongoDBReaderDto = new MongoDBReaderDto();
        Integer whereType = importFromDataSourceVo.getWhereType();
        if (whereType == null) {
            whereType = 0;
        }
        String query = "";
        switch(whereType) {
            case 0:
                query = "{}";
                break;
            case 1:
                query = convert2JsonString(importFromDataSourceVo.getFilterConditions());
                break;
            case 2:
                query = importFromDataSourceVo.getWhere();
                break;
            default:
        }
        // Here, in order to prevent the custom input from being empty and querying all
        if (StringUtils.isEmpty(query)) {
            query = "{}";
        }
        mongoDBReaderDto.setQueryJson(query);
        dataXJsonBuildDto.setMongoDBReader(mongoDBReaderDto);
    }

    private static String convert2JsonString(List<ImportFromDataSourceVo.FilterCondition> filterConditions) {
        if (CollectionUtils.isEmpty(filterConditions)) {
            return "{}";
        }
        Map<String, List<ImportFromDataSourceVo.FilterCondition>> maps = new HashMap<>();
        filterConditions.forEach(var -> {
            List<ImportFromDataSourceVo.FilterCondition> cons = maps.get(var.getColName());
            if (CollectionUtils.isEmpty(cons)) {
                List<ImportFromDataSourceVo.FilterCondition> newCons = Lists.newArrayList();
                newCons.add(var);
                maps.put(var.getColName(), newCons);
            } else {
                cons.add(var);
            }
        });
        Query query = new Query();
        maps.entrySet().stream().forEach(var -> {
            if (var.getValue().size() > 1) {
                Criteria criteria = new Criteria();
                Criteria[] criterias = new Criteria[var.getValue().size()];
                for (int i = 0; i < var.getValue().size(); i++) {
                    criterias[i] = getCriteria(var.getValue().get(i));
                }
                criteria.andOperator(criterias);
                query.addCriteria(criteria);
            } else {
                query.addCriteria(getCriteria(var.getValue().get(0)));
            }
        });
        return query.getQueryObject().toJson();
    }

    private static Criteria getCriteria(ImportFromDataSourceVo.FilterCondition var) {
        if ("=".equals(var.getOperType())) {
            return Criteria.where(var.getColName()).is(var.getVal());
        } else if (">".equals(var.getOperType())) {
            return Criteria.where(var.getColName()).gt(var.getVal());
        } else if ("<".equals(var.getOperType())) {
            return Criteria.where(var.getColName()).lt(var.getVal());
        } else if (">=".equals(var.getOperType())) {
            return Criteria.where(var.getColName()).gte(var.getVal());
        } else if ("<=".equals(var.getOperType())) {
            return Criteria.where(var.getColName()).lte(var.getVal());
        } else if ("like".equals(var.getOperType())) {
            return Criteria.where(var.getColName()).regex(compile("^.*" + var.getVal() + ".*$", CASE_INSENSITIVE));
        } else {
            return Criteria.where(var.getColName()).is(var.getVal());
        }
    }
}
