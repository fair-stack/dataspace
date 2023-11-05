/*
 * Copyright 2017 Guilherme Chaguri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.cnic.dataspace.api.ftp.minimalftp;

import cn.cnic.dataspace.api.filehandle.Control;
import cn.cnic.dataspace.api.model.file.FileMapping;
import cn.cnic.dataspace.api.util.FileUtils;
import lombok.SneakyThrows;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Guilherme Chaguri
 */
public class Utils {

    // Permission Categories
    public static final int CAT_OWNER = 6;

    public static final int CAT_GROUP = 3;

    public static final int CAT_PUBLIC = 0;

    // Permission Types
    public static final int TYPE_READ = 2;

    public static final int TYPE_WRITE = 1;

    public static final int TYPE_EXECUTE = 0;

    public static Map<String, List<Integer>> serPort = new HashMap<>();

    public static Map<Integer, ServerSocket> serMap = new HashMap<>();

    // Time
    private static final SimpleDateFormat mdtmFormat = new SimpleDateFormat("YYYYMMddHHmmss", Locale.ENGLISH);

    private static final SimpleDateFormat hourFormat = new SimpleDateFormat("MMM dd HH:mm", Locale.ENGLISH);

    private static final SimpleDateFormat yearFormat = new SimpleDateFormat("MMM dd YYYY", Locale.ENGLISH);

    private static final long sixMonths = 183L * 24L * 60L * 60L * 1000L;

    public static String toListTimestamp(long time) {
        // Intended Format
        // May 26 21:50
        // Feb 12 2015
        Date date = new Date(time);
        if (System.currentTimeMillis() - time > sixMonths) {
            return yearFormat.format(date);
        }
        return hourFormat.format(date);
    }

    public static String toMdtmTimestamp(long time) {
        return mdtmFormat.format(new Date(time));
    }

    public static long fromMdtmTimestamp(String time) throws ParseException {
        return mdtmFormat.parse(time).getTime();
    }

    @SneakyThrows
    public static String format(Control fs, File file) {
        // Intended Format
        // -rw-rw-rw-   1 owner   group    7045120 Aug 08  5:24 video.mp4
        // -rw-rw-rw-   1 owner   group        380 May 26 21:50 data.txt
        // drwxrwxrwx   3 owner   group          0 Oct 12  8:21 directory
        return String.format("%s %3d %-8s %-8s %8d %s %s\r\n", getPermission(fs, file), fs.getHardLinks(file), fs.getOwner(file), fs.getGroup(file), fs.getSizeBytes(file.toPath(), true), toListTimestamp(FileUtils.getLastModifiedTime(file.toPath())), file.getName());
    }

    @SneakyThrows
    public static String format(FileMapping fileMapping) {
        // Intended Format
        // -rw-rw-rw-   1 owner   group    7045120 Aug 08  5:24 video.mp4
        // -rw-rw-rw-   1 owner   group        380 May 26 21:50 data.txt
        // drwxrwxrwx   3 owner   group          0 Oct 12  8:21 directory
        return String.format("%s %3d %-8s %-8s %8d %s %s\r\n", (fileMapping.getType() == 1 ? "drwxrwxrwx" : "-rw-rw-rw-"), fileMapping.getType() == 1 ? 3 : 1, fileMapping.getAuthor().getPersonName(), fileMapping.getAuthor().getPersonName(), fileMapping.getSize(), toListTimestamp(fileMapping.getUpdateTime().getTime()), fileMapping.getName());
    }

    public static String getPermission(Control fs, File file) {
        // Intended Format
        // -rw-rw-rw-
        // -rwxrwxrwx
        // drwxrwxrwx
        String perm = "";
        int perms = fs.getPermissions(file);
        perm += FileUtils.isFolder(file.toPath()) ? 'd' : '-';
        perm += hasPermission(perms, CAT_OWNER + TYPE_READ) ? 'r' : '-';
        perm += hasPermission(perms, CAT_OWNER + TYPE_WRITE) ? 'w' : '-';
        perm += hasPermission(perms, CAT_OWNER + TYPE_EXECUTE) ? 'x' : '-';
        perm += hasPermission(perms, CAT_GROUP + TYPE_READ) ? 'r' : '-';
        perm += hasPermission(perms, CAT_GROUP + TYPE_WRITE) ? 'w' : '-';
        perm += hasPermission(perms, CAT_GROUP + TYPE_EXECUTE) ? 'x' : '-';
        perm += hasPermission(perms, CAT_PUBLIC + TYPE_READ) ? 'r' : '-';
        perm += hasPermission(perms, CAT_PUBLIC + TYPE_WRITE) ? 'w' : '-';
        perm += hasPermission(perms, CAT_PUBLIC + TYPE_EXECUTE) ? 'x' : '-';
        return perm;
    }

    @SneakyThrows
    public static String getFacts(Control fs, File file, String[] options) {
        // Intended Format
        // modify=20170808052431;size=7045120;type=file;perm=rfadw; video.mp4
        // modify=20170526215012;size=380;type=file;perm=rfadw; data.txt
        // modify=20171012082146;size=0;type=dir;perm=elfpcm; directory
        String facts = "";
        boolean dir = FileUtils.isFolder(file.toPath());
        for (String opt : options) {
            opt = opt.toLowerCase();
            if (opt.equals("modify")) {
                facts += "modify=" + Utils.toMdtmTimestamp(FileUtils.getLastModifiedTime(file.toPath())) + ";";
            } else if (opt.equals("size")) {
                facts += "size=" + fs.getSizeBytes(file.toPath(), true) + ";";
            } else if (opt.equals("type")) {
                facts += "type=" + (dir ? "dir" : "file") + ";";
            } else if (opt.equals("perm")) {
                int perms = fs.getPermissions(file);
                String perm = "";
                if (hasPermission(perms, CAT_OWNER + TYPE_READ)) {
                    perm += dir ? "el" : "r";
                }
                if (hasPermission(perms, CAT_OWNER + TYPE_WRITE)) {
                    perm += "f";
                    perm += dir ? "pcm" : "adw";
                }
                facts += "perm=" + perm + ";";
            }
        }
        facts += " " + file.getName() + "\r\n";
        return facts;
    }

    @SneakyThrows
    public static String getFacts(FileMapping fileMapping, String[] options) {
        // Intended Format
        // modify=20170808052431;size=7045120;type=file;perm=rfadw; video.mp4
        // modify=20170526215012;size=380;type=file;perm=rfadw; data.txt
        // modify=20171012082146;size=0;type=dir;perm=elfpcm; directory
        String facts = "";
        boolean dir = fileMapping.getType() == 1;
        for (String opt : options) {
            opt = opt.toLowerCase();
            if (opt.equals("modify")) {
                facts += "modify=" + Utils.toMdtmTimestamp(fileMapping.getUpdateTime().getTime()) + ";";
            } else if (opt.equals("size")) {
                facts += "size=" + fileMapping.getSize() + ";";
            } else if (opt.equals("type")) {
                facts += "type=" + (dir ? "dir" : "file") + ";";
            } else if (opt.equals("perm")) {
                String perm = dir ? "elfpcm" : "rfadw";
                // if (hasPermission(perms, CAT_OWNER + TYPE_READ)) {
                // perm += dir ? "el" : "r";
                // }
                // if (hasPermission(perms, CAT_OWNER + TYPE_WRITE)) {
                // perm += "f";
                // perm += dir ? "pcm" : "adw";
                // }
                facts += "perm=" + perm + ";";
            }
        }
        facts += " " + fileMapping.getName() + "\r\n";
        return facts;
    }

    public static void write(OutputStream out, byte[] bytes, int len, boolean ascii) throws IOException {
        if (ascii) {
            // ASCII - Add \r before \n when necessary
            byte lastByte = 0;
            for (int i = 0; i < len; i++) {
                byte b = bytes[i];
                if (b == '\n' && lastByte != '\r') {
                    out.write('\r');
                }
                out.write(b);
                lastByte = b;
            }
        } else {
            // Binary - Keep all \r\n as is
            out.write(bytes, 0, len);
        }
    }

    public static InputStream readFileSystem(Control fs, File file, long start, boolean ascii) throws IOException {
        if (ascii && start > 0) {
            InputStream in = new BufferedInputStream(fs.readFile(file, 0));
            long offset = 0;
            // Count \n as two bytes for skipping
            while (start >= offset++) {
                int c = in.read();
                if (c == -1) {
                    throw new IOException("Couldn't skip this file. End of the file was reached");
                } else if (c == '\n') {
                    offset++;
                }
            }
            return in;
        } else {
            return fs.readFile(file, start);
        }
    }

    public static boolean hasPermission(int perms, int perm) {
        return (perms >> perm & 1) == 1;
    }

    public static int setPermission(int perms, int perm, boolean hasPermission) {
        perm = 1 << perm;
        return hasPermission ? perms | perm : perms & ~perm;
    }

    public static int fromOctal(String perm) {
        return Integer.parseInt(perm, 8);
    }

    public static ServerSocket createServer(int port, int backlog, InetAddress address, SSLContext context, boolean ssl) throws IOException {
        if (ssl) {
            if (context == null) {
                throw new NullPointerException("The SSL context is null");
            }
            return context.getServerSocketFactory().createServerSocket(port, backlog, address);
        }
        return new ServerSocket(port, backlog, address);
    }

    public static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
        }
    }

    public static void portCheck(int port, String ip) {
        if (serPort.containsKey(ip)) {
            List<Integer> integers = serPort.get(ip);
            integers.add(port);
            serPort.put(ip, integers);
        } else {
            List<Integer> integers = new ArrayList<>();
            integers.add(port);
            serPort.put(ip, integers);
        }
        if (serMap.containsKey(port)) {
            ServerSocket serverSocket = serMap.get(port);
            try {
                serverSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Obtain FTP access IP
    public static String getIpAddr(String host) {
        if (null == host || host.trim().equals("")) {
            return null;
        }
        boolean ip = isIP(host);
        if (!ip) {
            InetAddress[] serverIP = getServerIP(host);
            if (null != serverIP && serverIP.length > 0) {
                InetAddress inetAddress = serverIP[0];
                return inetAddress.getHostAddress();
            } else {
                return null;
            }
        } else {
            return host;
        }
    }

    /**
     * Obtain the IP address of a certain domain name
     */
    private static InetAddress[] getServerIP(String domain) {
        InetAddress[] myServer = null;
        try {
            myServer = InetAddress.getAllByName(domain);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return myServer;
    }

    private static boolean isIP(String addr) {
        if (addr.length() < 7 || addr.length() > 15 || "".equals(addr)) {
            return false;
        }
        /**
         * Determine IP format and range
         */
        String rexp = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pat = Pattern.compile(rexp);
        Matcher mat = pat.matcher(addr);
        boolean ipAddress = mat.find();
        return ipAddress;
    }
    // public static int getPort(){
    // int port = PORT;
    // if(PORT >= 1000){
    // PORT = 30301;
    // }else {
    // PORT = PORT + 1;
    // }
    // return port;
    // }
}
