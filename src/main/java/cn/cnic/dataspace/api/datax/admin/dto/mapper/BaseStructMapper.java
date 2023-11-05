package cn.cnic.dataspace.api.datax.admin.dto.mapper;

import org.mapstruct.InheritConfiguration;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.MapperConfig;
import org.mapstruct.Mapping;
import java.util.List;
import java.util.stream.Stream;

@MapperConfig
public interface BaseStructMapper<SOURCE, TARGET> {

    /**
     * Map attributes with the same name
     */
    TARGET sourceToTarget(SOURCE var1);

    /**
     * Reverse, mapping attributes with the same name
     */
    @InheritInverseConfiguration(name = "sourceToTarget")
    SOURCE targetToSource(TARGET var1);

    /**
     * Map attributes with the same name in a collection form
     */
    @InheritConfiguration(name = "sourceToTarget")
    List<TARGET> sourceToTarget(List<SOURCE> var1);

    /**
     * Reverse, map attributes with the same name, set form
     */
    @InheritConfiguration(name = "targetToSource")
    List<SOURCE> targetToSource(List<TARGET> var1);

    /**
     * Map attributes with the same name, in the form of a collection stream
     */
    List<TARGET> sourceToTarget(Stream<SOURCE> stream);

    /**
     * Reverse, map attributes with the same name, in the form of a collection flow
     */
    List<SOURCE> targetToSource(Stream<TARGET> stream);
}
