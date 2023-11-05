package cn.cnic.dataspace.api.datax.admin.dto.mapper;

import cn.cnic.dataspace.api.datax.admin.dto.DataMappingDto;
import cn.cnic.dataspace.api.datax.admin.entity.DataMapping;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DataMappingStructMapper extends BaseStructMapper<DataMapping, DataMappingDto> {
}
