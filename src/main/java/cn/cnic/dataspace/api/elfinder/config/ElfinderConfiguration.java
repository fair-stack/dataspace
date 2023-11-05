package cn.cnic.dataspace.api.elfinder.config;

import cn.cnic.dataspace.api.elfinder.param.Node;
import cn.cnic.dataspace.api.elfinder.param.Thumbnail;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * ElfinderConfiguration
 *
 * @author wangCc
 * @date 2021-3-26 11:12:29
 */
@Data
@Component
@PropertySource(value = "classpath:application.yml")
@ConfigurationProperties(prefix = "file-manager")
public class ElfinderConfiguration {

    private Thumbnail thumbnail;

    private List<Node> volumes;

    /**
     * Default unrestricted
     */
    private Long maxUploadSize = -1L;
}
