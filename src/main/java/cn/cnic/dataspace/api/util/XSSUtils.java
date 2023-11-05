package cn.cnic.dataspace.api.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XSSUtils {

    public static String stripXSS(String value) {
        if (value != null) {
            // Define regular expressions for scripts
            String script = "<script[^>]*?>[\\s\\S]*?<\\/script>";
            // Define regular expressions for style
            String style = "<style[^>]*?>[\\s\\S]*?<\\/style>";
            // Define regular expressions for HTML tags
            String html = "<[^>]+>";
            // Filter special characters
            String regEx = "[`~!@#$%^*()+=|';'\\[\\]<>/?~！@#￥%……*（）——+|【】‘；：”“’。，、？]";
            Pattern p_script = Pattern.compile(script, Pattern.CASE_INSENSITIVE);
            Matcher m_script = p_script.matcher(value);
            // Filter script tags
            value = m_script.replaceAll("");
            Pattern p_style = Pattern.compile(style, Pattern.CASE_INSENSITIVE);
            Matcher m_style = p_style.matcher(value);
            // Filter style labels
            value = m_style.replaceAll("");
            Pattern p_html = Pattern.compile(html, Pattern.CASE_INSENSITIVE);
            Matcher m_html = p_html.matcher(value);
            // Filter HTML tags
            value = m_html.replaceAll("");
            Pattern p = Pattern.compile(regEx);
            Matcher m = p.matcher(value);
            value = m.replaceAll("");
            // ----- ---------------
            value = value.replaceAll("", "");
            Pattern scriptPattern = Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("src[\r\n]*=[\r\n]*\\\'(.*?)\\\'", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("src[\r\n]*=[\r\n]*\\\"(.*?)\\\"", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("</script>", Pattern.CASE_INSENSITIVE);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("<script(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("eval\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("e­xpression\\((.*?)\\)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile("onload(.*?)=", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
            value = scriptPattern.matcher(value).replaceAll("");
            scriptPattern = Pattern.compile(".*<.*", Pattern.CASE_INSENSITIVE);
            value = scriptPattern.matcher(value).replaceAll("");
        }
        return value;
    }
}
