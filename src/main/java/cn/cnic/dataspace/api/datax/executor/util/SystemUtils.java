package cn.cnic.dataspace.api.datax.executor.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;

/**
 * @ author maokeluo
 */
public class SystemUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystemUtils.class);

    private static String DATAX_HOME;

    private SystemUtils() {
    }

    /**
     * Obtain the Datax path in the environment variable
     */
    public static String getDataXHomePath() {
        if (StringUtils.isNotEmpty(DATAX_HOME))
            return DATAX_HOME;
        String dataXHome = System.getenv("DATAX_HOME");
        if (StringUtils.isBlank(dataXHome)) {
            // LOGGER.warn ("DATAX_HOME environment variable is NULL");
            return null;
        }
        DATAX_HOME = dataXHome.endsWith(File.separator) ? dataXHome : dataXHome.concat(File.separator);
        // LOGGER.info("DATAX_HOME:{}", DATAX_HOME);
        return DATAX_HOME;
    }
}
