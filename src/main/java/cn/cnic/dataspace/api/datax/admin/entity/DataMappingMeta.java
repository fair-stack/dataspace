package cn.cnic.dataspace.api.datax.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import lombok.Data;
import org.apache.ibatis.type.ArrayTypeHandler;
import java.util.List;

@Data
@TableName(value = "data_mapping_meta", autoResultMap = true)
public class DataMappingMeta {

    @TableId
    private Long id;

    private String dataMappingId;

    private String userId;

    @TableField(typeHandler = FastjsonTypeHandler.class)
    private List<String> showColumns;
}
