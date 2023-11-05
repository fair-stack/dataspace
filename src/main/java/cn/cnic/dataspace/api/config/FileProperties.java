package cn.cnic.dataspace.api.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import java.io.File;
import java.nio.file.Paths;

/**
 * @ Description File Storage Configuration Class
 */
@Data
@Slf4j
@Component
@PropertySource(value = "classpath:application.yml", factory = YamlPropertyLoaderFactory.class)
@ConfigurationProperties(prefix = "file")
public class FileProperties {

    /**
     * File Storage Root Directory File Monitoring Directory
     */
    private String rootDir = System.getProperty("user.dir");

    /**
     * The temporary file directory name for breakpoint continuation is located under rootDir, and the directory ignored by file monitoring scanning
     */
    private String chunkFileDir = "chunkFileTemp";

    /**
     * Default storage path for user avatars
     */
    private String userImgDir = "/Image/usr/";

    /**
     * The default storage location for markdown type files (articles)
     */
    private String documentDir = "/Document/";

    /**
     * The default storage location for images in markdown type files (articles)
     */
    private String documentImgDir = "/Image/Document/";

    /**
     * Default Delimiter
     */
    private String separator = "/";

    /**
     * Text types supported by text editors
     */
    private String[] simText = { "txt", "html", "htm", "asp", "jsp", "xml", "json", "properties", "md", "gitignore", "java", "py", "c", "cpp", "sql", "sh", "bat", "m", "bas", "prg", "cmd" };

    /**
     * Document Type
     */
    private String[] document = { "pdf", "doc", "docs", "xls", "xl", "md" };

    /**
     * Enable file monitoring (default not enabled)
     */
    private Boolean monitor = false;

    /**
     * File monitoring scanning interval (seconds)
     */
    private Long timeInterval = 10L;

    /**
     * WebDAV protocol prefix
     */
    private String webDavPrefix;

    /**
     * ip2region-path
     */
    // private String ip2regionDbPath;
    public String getRootDir() {
        return Paths.get(rootDir).toString();
    }

    public String getChunkFileDir() {
        return chunkFileDir;
    }

    public String getUserImgDir() {
        return Paths.get(userImgDir).toString();
    }

    public String getDocumentImgDir() {
        return Paths.get(documentImgDir).toString();
    }

    public String getDocumentDir() {
        return Paths.get(documentDir).toString();
    }
}
