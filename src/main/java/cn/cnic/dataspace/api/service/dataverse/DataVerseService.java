package cn.cnic.dataspace.api.service.dataverse;

import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.queue.ProgressBarThread;
import cn.cnic.dataspace.api.util.HttpClient;
import cn.cnic.dataspace.api.util.ResponseResult;
import cn.cnic.dataspace.api.util.ResultUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Dataverse interface docking
 */
@Service
public class DataVerseService {

    private final static String searchUrl = "https://demo.dataverse.org/api/search?sort=date&order=asc";

    // Download all files corresponding to the dataset
    private final static String downUrl = "https://demo.dataverse.org/api/access/dataset/:persistentId/?persistentId=";

    private final static String api_key = "api_key";

    /**
     * Data Query
     */
    public ResponseResult<Object> searchData(String q, String type, Integer start, Integer perPage, boolean show_relevance, boolean show_facets, String fq, boolean show_entity_ids) {
        HttpClient httpClient = new HttpClient();
        Map<String, String> hearMap = new HashMap<>(1);
        hearMap.put("Dataverse-key", api_key);
        String param = "&start=" + start + "&per_page=" + perPage;
        // if(StringUtils.isNotEmpty(q)){
        // 
        // }
        param += "&q=" + q;
        if (StringUtils.isNotEmpty(type)) {
            param += "&type=" + type;
        }
        String result = "";
        try {
            result = httpClient.doGet(searchUrl + param, hearMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Map map = JSONObject.parseObject(result, Map.class);
        return ResultUtil.success(map);
    }

    /**
     * Data import doi: 10.7910/DVN/1V5NYI
     */
    public ResponseResult<Object> fileImport(String doi) {
        File file = new File("E:\\data\\dataverse\\dataverse_files.zip");
        CloseableHttpResponse response = null;
        InputStream input = null;
        FileOutputStream output = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            HttpGet httpget = new HttpGet(downUrl + doi);
            // httpget.setHeader("Dataverse-key", "4639dca5-ba07-42d3-8cdf-2efa77f860dc");
            response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return ResultUtil.error("无返回数据！");
            }
            input = entity.getContent();
            output = new FileOutputStream(file);
            // Create handling tools
            byte[] datas = new byte[1024];
            int len = 0;
            while ((len = input.read(datas)) != -1) {
                // Loop reading data
                output.write(datas, 0, len);
            }
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new CommonException(e.getMessage());
        } finally {
            // Turn off low-level flow.
            if (input != null) {
                try {
                    input.close();
                } catch (Exception e) {
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (Exception e) {
                }
            }
            if (response != null) {
                try {
                    response.close();
                } catch (Exception e) {
                }
            }
            if (httpclient != null) {
                try {
                    httpclient.close();
                } catch (Exception e) {
                }
            }
        }
        return ResultUtil.success();
    }

    public static void main(String[] args) throws IOException {
        DataVerseService dataVerseService = new DataVerseService();
        String q = "title:data";
        // ResponseResult<Object> responseResult = dataVerseService.searchData(q, "dataset",
        // 1, 10, true, true, "", true);
        ResponseResult<Object> responseResult = dataVerseService.fileImport("doi:10.70122/FK2/PCH8PX");
        // httpUrl(downUrl+"doi:10.70122/FK2/PCH8PX","E:\\data\\dataverse\\dataverse_files.zip");
    }
    // public static void httpUrl(String urlPath,String filePath) throws IOException {
    // try {
    // CloseableHttpClient httpClient = HttpClientBuilder.create().build();
    // HttpGet get = new HttpGet(urlPath);
    // CloseableHttpResponse response = httpClient.execute(get);
    // if (response.getStatusLine().getStatusCode() == 200) {
    // //Obtaining Entities
    // HttpEntity entity = response.getEntity();
    // InputStream inputStream = entity.getContent();
    // byte[] getData = readInputStream(inputStream);
    // File file = new File(filePath);
    // FileOutputStream fos = new FileOutputStream(file);
    // fos.write(getData);
    // fos.close();
    // }
    // } catch (Exception e) {
    // e.printStackTrace();
    // }
    // }
    // 
    // 
    // public static  byte[] readInputStream(InputStream inputStream) throws IOException {
    // byte[] buffer = new byte[1024];
    // int len = 0;
    // ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // while((len = inputStream.read(buffer)) != -1) {
    // bos.write(buffer, 0, len);
    // }
    // bos.close();
    // return bos.toByteArray();
    // }
}
