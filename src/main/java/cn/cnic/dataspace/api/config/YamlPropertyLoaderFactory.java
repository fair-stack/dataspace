package cn.cnic.dataspace.api.config;

import cn.cnic.dataspace.api.util.CommonUtils;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.EncodedResource;
import java.io.IOException;
import java.util.List;

/**
 * Yml resource loading factory class
 */
public class YamlPropertyLoaderFactory extends DefaultPropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        if (null == resource) {
            super.createPropertySource(name, resource);
        }
        List<PropertySource<?>> sourceList = new YamlPropertySourceLoader().load(resource.getResource().getFilename(), resource.getResource());
        if (sourceList.size() < 1) {
            throw new RuntimeException(CommonUtils.messageInternational("CONFIG_FILE"));
        }
        return sourceList.get(0);
    }
}
