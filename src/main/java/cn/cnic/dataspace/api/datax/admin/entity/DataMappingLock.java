package cn.cnic.dataspace.api.datax.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("data_mapping_lock")
@Data
public class DataMappingLock {

    @TableId(value = "data_mapping_id", type = IdType.INPUT)
    private Long dataMappingId;
}
