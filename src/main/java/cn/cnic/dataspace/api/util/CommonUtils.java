package cn.cnic.dataspace.api.util;

import cn.cnic.dataspace.api.config.space.FileOperationFactory;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.model.space.Space;
import cn.hutool.core.util.IdUtil;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.poi.util.IOUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.groups.Default;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;

/**
 * CommonUtils
 *
 * @author wangCc
 * @date 2018/11/2
 */
public final class CommonUtils {

    public static String FILE_SPLIT = File.separator;

    /*public static final String STRONG = FileOperationFactory.getMessageSource().getMessage("STRONG", null, LocaleContextHolder.getLocale());
    public static final String WEAK = FileOperationFactory.getMessageSource().getMessage("WEAK", null, LocaleContextHolder.getLocale());
    public static final String MIDDLE = FileOperationFactory.getMessageSource().getMessage("MIDDLE", null, LocaleContextHolder.getLocale());*/
    public static final String STRONG = "强";

    public static final String WEAK = "弱";

    public static final String MIDDLE = "中";

    public static final String REGX = "[`~!@#$%^&*+={}':;<>/?…\\\\|\"]|\n|\r|\t";

    /**
     * datetime format
     */
    private static final String ISO_DATE_FORMAT = "yyyy-MM-dd";

    private static final String CUSTOM_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    public static final String CUSTOM_DATETIME_MS = "dd-MMM-yyyy HH:mm:ss:SSS";

    // number
    private static final String SYMBOLS = "0123456789";

    /**
     * generate UUID
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * generate snow flake
     */
    public static String generateSnowflake() {
        return String.valueOf(IdUtil.getSnowflake(System.currentTimeMillis() % 32, 0L).nextId());
    }

    /**
     * Obtain the current year yyyy
     */
    public static String getCurrentYear() {
        return String.valueOf(LocalDate.now().getYear());
    }

    /**
     * Get the current date string, yyyy MM dd HH: mm: ss
     */
    public static String getCurrentDateTimeString() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(CUSTOM_DATETIME_FORMAT));
    }

    /**
     * Date formatting, yyyy MM dd HH: mm: ss
     */
    public static String getDateTimeString(LocalDateTime time) {
        return time.format(DateTimeFormatter.ofPattern(CUSTOM_DATETIME_FORMAT));
    }

    public static String getDateTimeString(Date date) {
        return DateFormatUtils.format(date, CUSTOM_DATETIME_FORMAT);
    }

    public static Date getStringToDateTime(String time) throws ParseException {
        return DateUtils.parseDate(time, CUSTOM_DATETIME_FORMAT);
    }

    /**
     * Date formatting, yyyy MM dd
     */
    public static String getDateString(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern(ISO_DATE_FORMAT));
    }

    public static String getDateString(Date date) {
        return DateFormatUtils.format(date, ISO_DATE_FORMAT);
    }

    public static Date getStringToDate(String time) throws ParseException {
        return DateUtils.parseDate(time, ISO_DATE_FORMAT);
    }

    public static Date getStringDate(String time) throws ParseException {
        return DateUtils.parseDate(time, "yyyy-MM");
    }

    /**
     * check file is be used
     */
    public static boolean fileUsed(String filePath) {
        try {
            RandomAccessFile raf = new RandomAccessFile(new File(filePath), "rw");
            raf.close();
            return false;
        } catch (Exception e) {
            System.out.println("File being used " + filePath);
            return true;
        }
    }

    /**
     * //Obtain the time 24 hours before the current time
     */
    public static long yesterday(int hours) {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.set(Calendar.HOUR_OF_DAY, c.get(Calendar.HOUR_OF_DAY) - hours);
        Date time = c.getTime();
        return time.getTime();
    }

    /**
     * //Obtain the time 24 hours before the current time
     */
    public static Date getHour(Date time, int day) {
        Calendar c = Calendar.getInstance();
        c.setTime(time);
        c.add(Calendar.DAY_OF_MONTH, day);
        return c.getTime();
    }

    /**
     * Obtain the current year yyyy
     */
    public static int getCurrentYearTo() {
        return LocalDate.now().getYear();
    }

    /**
     * Get the date of the past few days
     */
    public static Date getPastDate(int past) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) - past);
        Date today = calendar.getTime();
        return today;
    }

    /**
     * Obtain the date in the past month
     */
    public static Date getPastMonth(int past) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - past);
        Date today = calendar.getTime();
        return today;
    }

    /**
     * Obtain the current month mm
     */
    public static int getCurrentMonth() {
        return LocalDate.now().getMonthValue();
    }

    public static int getCurrentMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MONTH) + 1;
    }

    /**
     * Obtain the current date mm
     */
    public static int getCurrentDay() {
        return LocalDate.now().getDayOfMonth();
    }

    public static String getMonthFirstDay() {
        SimpleDateFormat format = new SimpleDateFormat(ISO_DATE_FORMAT);
        Calendar calendar = Calendar.getInstance();
        // Obtain the first day of the month
        calendar.add(Calendar.MONTH, 0);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return format.format(calendar.getTime());
    }

    public static String getMonthLastDay() {
        SimpleDateFormat format = new SimpleDateFormat(ISO_DATE_FORMAT);
        // Obtain the last day of the month
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 0);
        return format.format(calendar.getTime());
    }

    /**Obtain two time difference units to minutes*/
    public static String getMin(Date startDate, Date endDate) {
        String jetLag = "";
        try {
            SimpleDateFormat df = new SimpleDateFormat(CUSTOM_DATETIME_MS);
            Date start = df.parse(df.format(startDate));
            Date end = df.parse(df.format(endDate));
            long length = end.getTime() - start.getTime();
            long day = length / (24 * 60 * 60 * 1000);
            if (day != 0)
                jetLag += day + "天; ";
            long hour = (length / (60 * 60 * 1000) - day * 24);
            if (hour != 0)
                jetLag += hour + "小时; ";
            long min = ((length / (60 * 1000)) - day * 24 * 60 - hour * 60);
            if (min != 0)
                jetLag += min + "分钟; ";
            long sec = (length / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);
            if (sec != 0)
                jetLag += sec + "秒; ";
            if (jetLag.equals(""))
                jetLag += length + "毫秒";
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jetLag;
    }

    /**
     * picture type suffix
     */
    public static boolean isPic(String suffix) {
        return Arrays.asList("BMP", "JPG", "JPEG", "PNG", "GIF", "bmp", "jpg", "jpeg", "png", "gif").contains(suffix);
    }

    /**
     * message for international change
     */
    public static String messageInternational(String code) {
        return FileOperationFactory.getMessageSource().getMessage(code, null, LocaleContextHolder.getLocale());
    }

    public static boolean isPicBase(String base) {
        if (!base.contains(",")) {
            return false;
        }
        String substring = base.substring(0, base.indexOf(","));
        if (!substring.contains("image")) {
            return false;
        }
        List<String> list = Arrays.asList("BMP", "JPG", "JPEG", "PNG", "GIF", "bmp", "jpg", "jpeg", "png", "gif");
        for (String s : list) {
            if (substring.contains(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Match at least three of the four items' uppercase letters, lowercase letters, numbers, special characters'
     */
    public static boolean passVerify(String password) {
        if (password.length() < 8) {
            throw new CommonException(-1, FileOperationFactory.getMessageSource().getMessage("PWD_LENGTH_MIN", null, LocaleContextHolder.getLocale()));
        }
        if (password.length() > 12) {
            throw new CommonException(-1, FileOperationFactory.getMessageSource().getMessage("PWD_LENGTH_MAX", null, LocaleContextHolder.getLocale()));
        }
        String pattern = "^(?![a-zA-Z]+$)(?![A-Z0-9]+$)(?![A-Z\\W_!@#$%^&*`~()-+=]+$)(?![a-z0-9]+$)(?![a-z\\W_!@#$%^&*`~()-+=]+$)(?![0-9\\W_!@#$%^&*`~()-+=]+$)[a-zA-Z0-9\\W_!@#$%^&*`~()-+=]{6,30}$";
        Pattern pA = Pattern.compile(pattern);
        Matcher matcher = pA.matcher(password);
        return matcher.matches();
    }

    /**
     * Verify if the email is legal
     */
    public static boolean isEmail(String email) {
        String rule = "[\\w!#$%&'*+/=?^_`{|}~-]+(?:\\.[\\w!#$%&'*+/=?^_`{|}~-]+)*@(?:[\\w](?:[\\w-]*[\\w])?\\.)+[\\w](?:[\\w-]*[\\w])?";
        Pattern pattern = Pattern.compile(rule);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    /**
     * JSR 303 Validation
     */
    public static List<String> validation(Object obj) {
        Set<ConstraintViolation<Object>> validateSet = Validation.buildDefaultValidatorFactory().getValidator().validate(obj, Default.class);
        if (!CollectionUtils.isEmpty(validateSet)) {
            return validateSet.stream().map(ConstraintViolation::getMessage).collect(toList());
        }
        return Lists.newArrayList();
    }

    public static List<String> validationMap(Map<String, Object> map) {
        List<String> resultList = new ArrayList<>(map.size());
        for (String key : map.keySet()) {
            Object o = map.get(key);
            if (null == o) {
                resultList.add("param : " + key + " is null");
            } else if (StringUtils.isEmpty((String) o) || StringUtils.isEmpty(((String) o).trim())) {
                resultList.add("param : " + key + " is null");
            }
        }
        return resultList;
    }

    /**
     * Password strength
     */
    public static String pwdStrength(String passwordStr) {
        String regexZ = "\\d*";
        String regexS = "[a-zA-Z]+";
        String regexT = "\\W+$";
        String regexZT = "\\D*";
        String regexST = "[\\d\\W]*";
        String regexZS = "\\w*";
        String regexZST = "[\\w\\W]*";
        if (passwordStr.matches(regexZ)) {
            return WEAK;
        }
        if (passwordStr.matches(regexS)) {
            return WEAK;
        }
        if (passwordStr.matches(regexT)) {
            return WEAK;
        }
        if (passwordStr.matches(regexZT)) {
            return MIDDLE;
        }
        if (passwordStr.matches(regexST)) {
            return MIDDLE;
        }
        if (passwordStr.matches(regexZS)) {
            return MIDDLE;
        }
        if (passwordStr.matches(regexZST)) {
            return STRONG;
        }
        return passwordStr;
    }

    /**
     * Obtain the current network IP
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ipAddress = request.getHeader("x-forwarded-for");
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
                // Retrieve the IP configured on the local machine based on the network card
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                ipAddress = inet.getHostAddress();
            }
        }
        // For cases where multiple proxies are used, the first IP is the real IP of the client, and multiple IPs are divided by ','
        if (ipAddress != null && ipAddress.length() > 15) {
            // "***.***.***.***".length() = 15
            if (ipAddress.indexOf(",") > 0) {
                ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
            }
        }
        return ipAddress;
    }

    public static String getShort() {
        // Initialize numbers in English and symbols
        String num = "0123456789";
        String english = "qwertyuiopasdfghjklzxcvbnm";
        String englishBig = "QWERTYUIOPASDFGHJKLZXCVBNM";
        // String symbol = "!@#$%^&*";
        StringBuffer password = new StringBuffer();
        getPass(password, englishBig, 3);
        getPass(password, english, 3);
        getPass(password, english, 3);
        getPass(password, num, 3);
        return password.toString();
    }

    public static String getCode(int n) {
        // Save numbers 0-9 and uppercase and lowercase letters
        String string = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        char[] ch = new char[n];
        for (int i = 0; i < n; i++) {
            Random random = new Random();
            int index = random.nextInt(string.length());
            ch[i] = string.charAt(index);
        }
        String result = String.valueOf(ch);
        return result;
    }

    public static String getVerificationCode(int n) {
        // Save numbers 0-9 and uppercase and lowercase letters
        String string = "23456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ";
        char[] ch = new char[n];
        for (int i = 0; i < n; i++) {
            Random random = new Random();
            int index = random.nextInt(string.length());
            ch[i] = string.charAt(index);
        }
        String result = String.valueOf(ch);
        return result;
    }

    private static void getPass(StringBuffer pass, String str, int length) {
        for (int i = 0; i < length; i++) {
            Random random = new Random();
            int a = random.nextInt(str.length());
            char c = str.charAt(a);
            pass.append(c);
        }
    }

    /**Get Cookies*/
    public static Cookie getCookie(String name, String value, int time, int version) {
        Cookie cookie = new Cookie(name, value);
        // -1: There is no expiration date; 0: Immediate expiration
        cookie.setMaxAge(time);
        cookie.setVersion(version);
        // The valid path for cookies is the website root directory
        cookie.setPath("/");
        // cookie.setDomain(Constants.Url.WEB_PATH);
        // Set the HttpOnly property to prevent attackers from exploiting cross site scripting vulnerabilities for cookie hijacking attacks
        cookie.isHttpOnly();
        return cookie;
    }

    /**
     * Cookie to obtain user information
     */
    public static String getUser(HttpServletRequest request, String param) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(param)) {
                    String value = cookie.getValue();
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Read resourceFile
     */
    public static File getResourceFile(String filePath) {
        try {
            ClassPathResource classPathResource = new ClassPathResource(filePath);
            InputStream inputStream = classPathResource.getInputStream();
            // Generate target file
            File somethingFile = File.createTempFile("subject", ".json");
            try {
                FileUtils.copyInputStreamToFile(inputStream, somethingFile);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
            return somethingFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Read JSON file
     */
    public static String readJsonFile(String fileName) {
        String jsonStr = null;
        FileReader fileReader = null;
        Reader reader = null;
        try {
            File jsonFile = new File(fileName);
            fileReader = new FileReader(jsonFile);
            reader = new InputStreamReader(new FileInputStream(jsonFile), "utf-8");
            int ch = 0;
            StringBuffer sb = new StringBuffer();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            jsonStr = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fileReader) {
                    fileReader.close();
                }
                if (null != reader) {
                    reader.close();
                }
            } catch (IOException i) {
            }
        }
        return jsonStr;
    }

    public static boolean sendEmailCheck(String email) {
        CheckEmail ce = new CheckEmail();
        return ce.checkEmail(email);
    }

    /**
     * Label verification
     */
    public static boolean isSpecialChar(String str) {
        String regEx = "[`~!@#$%^&*+=|{}':;<>/?…]|\n|\r|\t";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.find();
    }

    /**
     * File name verification
     */
    public static boolean isFtpChar(String str) {
        String regEx = "[\\\\/:*?\"<>|]|\n|\r|\t";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.find();
    }

    /**
     * take out special char
     */
    public static String takeOutChar(String str) {
        List<String> list = new ArrayList<>();
        for (char aChar : str.toCharArray()) {
            list.add(String.valueOf(aChar));
        }
        String result = null;
        for (String s : list) {
            final int i = REGX.indexOf(s);
            if (i != -1) {
                result = REGX.substring(i, i + 1);
                break;
            }
        }
        return result;
    }

    /**
     * Map value sorting
     */
    public static Map<Space, Double> sortMapDesc(Map<Space, Double> unsortMap) {
        Map<Space, Double> result = unsortMap.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        return result;
    }

    /**
     * Mongo query time conversion
     */
    public static Date conversionDate(String time, String type) {
        Date date = null;
        SimpleDateFormat startSdf = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // Convert to Greenwich Mean Time Zone
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+0:00"));
        try {
            if (type.equals("start")) {
                date = sdf.parse(sdf.format(startSdf.parse(time)));
            } else {
                date = sdf.parse(sdf.format(startSdf.parse(time)));
                Calendar calendar = new GregorianCalendar();
                calendar.setTime(date);
                calendar.add(Calendar.DATE, 1);
                date = calendar.getTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }

    /**
     * Sort the ASCII code of the key in the Map from small to large and return the concatenation parameters
     */
    public static String ASCLLSort(Map<String, Object> params) {
        String[] sortedKeys = params.keySet().toArray(new String[] {});
        // Sort Request Parameters
        Arrays.sort(sortedKeys);
        StringBuilder s2 = new StringBuilder();
        for (String key : sortedKeys) {
            s2.append(key).append("=").append(params.get(key)).append("&");
        }
        s2.deleteCharAt(s2.length() - 1);
        return s2.toString();
    }

    public static Long getAppKey() {
        char[] nonceChars = new char[6];
        SecureRandom secureRandom = new SecureRandom();
        for (int index = 0; index < nonceChars.length; ++index) {
            nonceChars[index] = SYMBOLS.charAt(secureRandom.nextInt(SYMBOLS.length()));
        }
        return Long.valueOf(new String(nonceChars));
    }

    /**
     * @ Description: Convert base64 encoded string to image
     */
    public static String generateImage(String base64, String path) {
        // Decryption
        String substring = path.substring(0, path.lastIndexOf("/"));
        File file = new File(substring);
        if (!file.exists()) {
            file.mkdirs();
        }
        File file1 = new File(path);
        if (file1.exists()) {
            file1.delete();
        }
        OutputStream out = null;
        try {
            // Decryption
            Base64.Decoder decoder = Base64.getDecoder();
            // Remove the base64 prefix data: image/jpeg; Base64,
            base64 = base64.substring(base64.indexOf(",", 1) + 1);
            byte[] b = decoder.decode(base64);
            // Processing data
            for (int i = 0; i < b.length; ++i) {
                if (b[i] < 0) {
                    b[i] += 256;
                }
            }
            // Save Picture
            out = new FileOutputStream(path);
            out.write(b);
            out.flush();
            // Return the relative path of the image=image classification path+image name+image suffix
            return path;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (null != out) {
                try {
                    out.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }

    /**
     * Convert File Type to BASE64
     */
    public static String fileToBase64(File file) {
        if (!file.exists()) {
            return "";
        }
        try {
            Base64.Encoder encoder = Base64.getEncoder();
            return "data:image/jpg;base64," + encoder.encodeToString(fileToByte(file));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * File type to byte []
     */
    private static byte[] fileToByte(File file) {
        byte[] fileBytes = null;
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            fileBytes = new byte[(int) file.length()];
            fis.read(fileBytes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != fis) {
                try {
                    fis.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
        return fileBytes;
    }

    /**
     * Escape regular special characters ($() *+ []\^ {}, |)
     */
    public static String escapeExprSpecialWord(String keyword) {
        String[] fbsArr = { "\\", "$", "(", ")", "*", "+", ".", "[", "]", "?", "^", "{", "}", "|" };
        for (String key : fbsArr) {
            if (keyword.contains(key)) {
                keyword = keyword.replace(key, "\\" + key);
            }
        }
        return keyword;
    }
}
