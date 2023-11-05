package cn.cnic.dataspace.api.util;

import cn.cnic.dataspace.api.model.IpInfo;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class Ip2regionAnalysis {

    // Singleton object
    private volatile static Ip2regionAnalysis analysis;

    // Database location
    public static final String dbPath = "/ip2region/ip2region.xdb";

    // Global Cache Query Object
    private static Searcher searcher;

    /**
     * Initialize, download the IP database file and convert it into a file output stream
     */
    private Ip2regionAnalysis() {
        // 1. Load the entire xdb from dbPath into memory.
        byte[] cBuff;
        try {
            InputStream resourceAsStream = this.getClass().getResourceAsStream(dbPath);
            cBuff = inputStreamToByteArray(resourceAsStream);
        } catch (Exception e) {
            return;
        }
        // 2. Create a completely memory based query object using the cBuff mentioned above.
        try {
            searcher = Searcher.newWithBuffer(cBuff);
        } catch (Exception e) {
            return;
        }
    }

    /**
     * InputStream to byte array
     */
    private byte[] inputStreamToByteArray(InputStream inputStream) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int num;
            while ((num = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, num);
            }
            byteArrayOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[] {};
    }

    /**
     * Close
     */
    public void searcherClose() {
        if (searcher != null) {
            try {
                searcher.close();
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    /**
     * Obtain IP resolution singleton
     */
    public static Ip2regionAnalysis getInstance() {
        if (analysis == null) {
            synchronized (Ip2regionAnalysis.class) {
                if (analysis == null) {
                    analysis = new Ip2regionAnalysis();
                }
            }
        }
        return analysis;
    }

    /**
     * @ param IP address
     */
    public String getIpInfo(String ip) {
        String region = "";
        try {
            region = searcher.search(ip);
        } catch (Exception e) {
            log.error("Ip解析失败：{}", e.toString());
        }
        return region;
    }

    /**
     * *
     */
    public IpInfo getIpInfoBean(String ip) {
        String ipInfo = getIpInfo(ip);
        IpInfo info = new IpInfo();
        // Country/Region/Province/City/ISP
        if (!"".equals(ipInfo)) {
            // String[] split = StrUtil.split(ipInfo, "|");
            String[] split = ipInfo.split("|");
            info.setCountry(split[0]);
            info.setRegion(split[1]);
            info.setProvince(split[2]);
            info.setCity(split[3]);
            info.setIsp(split[4]);
        } else {
            return null;
        }
        return info;
    }
}
