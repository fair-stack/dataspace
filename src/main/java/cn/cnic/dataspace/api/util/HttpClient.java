package cn.cnic.dataspace.api.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.List;
import java.util.Map;

@Slf4j
public class HttpClient {

    final String CONTENT_TYPE_TEXT_JSON = "text/json";

    /**
     * GET - with parameters (placing parameters in key value pair classes and then in URI to obtain HttpGet instances through URI)
     */
    public String doGetWayTwo(List<NameValuePair> params, String url, Map<String, String> header) {
        // parameter
        URI uri = null;
        try {
            url = url.trim();
            int i = url.indexOf(":");
            String scheme = url.substring(0, i);
            String substring = url.substring(i + 3);
            int i1 = substring.indexOf("/");
            String host = substring.substring(0, i1);
            String path = substring.substring(i1);
            uri = new URIBuilder().setScheme(scheme).setHost(host).setPath(path).setParameters(params).build();
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        // Create Get Request
        HttpGet httpGet = new HttpGet(uri);
        if (header != null && header.size() > 0) {
            for (String key : header.keySet()) {
                httpGet.setHeader(key, header.get(key));
            }
        }
        return doGetResult(httpGet);
    }

    public String doGet(String url, Map<String, String> header) {
        HttpGet httpGet = new HttpGet(url.trim());
        if (header != null && header.size() > 0) {
            for (String key : header.keySet()) {
                httpGet.setHeader(key, header.get(key));
            }
        }
        return doGetResult(httpGet);
    }

    public String doGetWayTwo(String url) throws MalformedURLException, URISyntaxException {
        // Create Get Request
        URL contr = new URL(url.trim());
        URI uri = new URI(contr.getProtocol(), contr.getAuthority(), contr.getPath(), contr.getQuery(), null);
        HttpGet httpGet = new HttpGet(uri);
        return doGetResult(httpGet);
    }

    public String doGetWayTwo(List<NameValuePair> params, String url) {
        URI uri = null;
        try {
            url = url.trim();
            int i = url.indexOf(":");
            String scheme = url.substring(0, i);
            String substring = url.substring(i + 3);
            int i1 = substring.indexOf("/");
            String host = substring.substring(0, i1);
            String path = substring.substring(i1);
            uri = new URIBuilder().setScheme(scheme).setHost(host).setPath(path).setParameters(params).build();
        } catch (URISyntaxException e1) {
            e1.printStackTrace();
        }
        HttpGet httpGet = new HttpGet(uri);
        return doGetResult(httpGet);
    }

    /**
     * Pass a cookie
     */
    public String doGetCookie(String url, String cookie) {
        // Create Get Request
        HttpGet httpGet = new HttpGet(url.trim());
        httpGet.setHeader("Cookie", cookie);
        return doGetResult(httpGet);
    }

    public String doGetHeader(String url, String header) {
        // Create Get Request
        HttpGet httpGet = new HttpGet(url.trim());
        httpGet.setHeader("secretKey", header);
        return doGetResult(httpGet);
    }

    public int doGetRetStatus(String url) {
        HttpGet httpGet = new HttpGet(url);
        // Obtain the HttpClient (which can be understood as: you need to have a browser first; note: in fact, HttpClient is different from a browser)
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        // Response model
        CloseableHttpResponse response = null;
        try {
            // configuration information
            // Set connection timeout in milliseconds
            RequestConfig // Set connection timeout in milliseconds
            // Set request timeout in milliseconds
            // Set request timeout in milliseconds
            // Socket read/write timeout (in milliseconds)
            // Socket read/write timeout (in milliseconds)
            requestConfig = // Set whether redirection is allowed (default to true)
            RequestConfig.custom().// Set whether redirection is allowed (default to true)
            setConnectTimeout(2 * 1000).setConnectionRequestTimeout(2 * 1000).setSocketTimeout(2 * 1000).setRedirectsEnabled(true).build();
            // Apply the above configuration information to this Get request
            httpGet.setConfig(requestConfig);
            // Execute (send) Get request by client
            response = httpClient.execute(httpGet);
            return response.getStatusLine().getStatusCode();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // Release resources
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    private String doGetResult(HttpGet httpGet) {
        // Obtain the HttpClient (which can be understood as: you need to have a browser first; note: in fact, HttpClient is different from a browser)
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String result = "";
        // Response model
        CloseableHttpResponse response = null;
        try {
            // configuration information
            // Set connection timeout in milliseconds
            RequestConfig // Set connection timeout in milliseconds
            // Set request timeout in milliseconds
            // Set request timeout in milliseconds
            // Socket read/write timeout (in milliseconds)
            // Socket read/write timeout (in milliseconds)
            requestConfig = // Set whether redirection is allowed (default to true)
            RequestConfig.custom().// Set whether redirection is allowed (default to true)
            setConnectTimeout(120000).setConnectionRequestTimeout(120000).setSocketTimeout(120000).setRedirectsEnabled(true).build();
            // Apply the above configuration information to this Get request
            httpGet.setConfig(requestConfig);
            // Execute (send) Get request by client
            response = httpClient.execute(httpGet);
            // Obtain response entities from the response model
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                result = EntityUtils.toString(responseEntity);
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // Release resources
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Post Form Value Transfer
     */
    public String doPostJsonWayTwo(List<NameValuePair> params, String url) {
        // Create Post Request
        HttpPost httpPost = new HttpPost(url.trim());
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return doPostSend(httpPost);
    }

    /**
     * Post form value transfer+serious request header
     */
    public String doPostJsonWayTwo(List<NameValuePair> params, String url, String token) {
        // Create Post Request
        HttpPost httpPost = new HttpPost(url.trim());
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        httpPost.setHeader("Authorization", token);
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return doPostSend(httpPost);
    }

    /**
     * Post request passing JSON
     */
    public String doPostJsonWayTwo(String param, String url) {
        // Create Post Request
        HttpPost httpPost = new HttpPost(url.trim());
        httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
        StringEntity se = new StringEntity(param, "UTF-8");
        se.setContentType(CONTENT_TYPE_TEXT_JSON);
        httpPost.setEntity(se);
        return doPostSend(httpPost);
    }

    /**
     * Post request passing JSON+authentication request header
     */
    public String doPostJsonAuth(String param, String url, String token) {
        // Create Post Request
        HttpPost httpPost = new HttpPost(url.trim());
        httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
        httpPost.setHeader("Authorization", token);
        StringEntity se = new StringEntity(param, "UTF-8");
        se.setContentType(CONTENT_TYPE_TEXT_JSON);
        httpPost.setEntity(se);
        return doPostSend(httpPost);
    }

    public String doPostHeader(List<NameValuePair> params, String url, Map<String, String> header) {
        // Create Post Request
        HttpPost httpPost = new HttpPost(url.trim());
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        if (header != null && header.size() > 0) {
            for (String key : header.keySet()) {
                httpPost.setHeader(key, header.get(key));
            }
        }
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return doPostSend(httpPost);
    }

    public String doPostHeader(String param, String url, Map<String, String> header) {
        // Create Post Request
        HttpPost httpPost = new HttpPost(url.trim());
        httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
        if (header != null && header.size() > 0) {
            for (String key : header.keySet()) {
                httpPost.setHeader(key, header.get(key));
            }
        }
        StringEntity se = new StringEntity(param, "UTF-8");
        se.setContentType(CONTENT_TYPE_TEXT_JSON);
        httpPost.setEntity(se);
        return doPostSend(httpPost);
    }

    private String doPostSend(HttpPost httpPost) {
        // Obtain the HttpClient (which can be understood as: you need to have a browser first; note: in fact, HttpClient is different from a browser)
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        String result = "";
        // Response model
        CloseableHttpResponse response = null;
        try {
            // configuration information
            // Set connection timeout in milliseconds
            RequestConfig // Set connection timeout in milliseconds
            // Set request timeout in milliseconds
            // Set request timeout in milliseconds
            // Socket read/write timeout (in milliseconds)
            // Socket read/write timeout (in milliseconds)
            requestConfig = // Set whether redirection is allowed (default to true)
            RequestConfig.custom().// Set whether redirection is allowed (default to true)
            setConnectTimeout(150000).setConnectionRequestTimeout(150000).setSocketTimeout(150000).setRedirectsEnabled(true).build();
            // Apply the above configuration information to this Post request
            httpPost.setConfig(requestConfig);
            // Execute (send) a Post request by the client
            response = httpClient.execute(httpPost);
            // Obtain response entities from the response model
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                result = EntityUtils.toString(responseEntity);
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // Release resources
                if (httpClient != null) {
                    httpClient.close();
                }
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Technology Cloud Request
     */
    public String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url.trim());
            // Open a connection between and URL
            URLConnection conn = realUrl.openConnection();
            // Set universal request properties
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // Sending a POST request must be set to the following two lines
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // Obtain the output stream corresponding to the URLConnection object
            out = new PrintWriter(conn.getOutputStream());
            // Send request parameters
            out.print(param);
            // Buffer of flush output stream
            out.flush();
            // Define the BufferedReader input stream to read the response of the URL
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！" + e);
            e.printStackTrace();
        } finally // Use finally blocks to close output and input streams
        {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }
}
