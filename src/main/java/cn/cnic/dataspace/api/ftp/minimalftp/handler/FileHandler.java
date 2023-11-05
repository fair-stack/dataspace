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
package cn.cnic.dataspace.api.ftp.minimalftp.handler;

import cn.cnic.dataspace.api.config.space.FileOperationFactory;
import cn.cnic.dataspace.api.filehandle.Control;
import cn.cnic.dataspace.api.filehandle.ControlImpl;
import cn.cnic.dataspace.api.ftp.minimalftp.FTPConnection;
import cn.cnic.dataspace.api.ftp.minimalftp.Utils;
import cn.cnic.dataspace.api.ftp.minimalftp.api.ResponseException;
import cn.cnic.dataspace.api.ftp.minimalftp.api.SpaceListener;
import cn.cnic.dataspace.api.model.space.SpaceSimple;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.model.file.FileMapping;
import cn.cnic.dataspace.api.util.FileUtils;
import cn.cnic.dataspace.api.util.SpaceRoleEnum;
import cn.cnic.dataspace.api.util.SpaceSizeControl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles file management commands
 *
 * @author Guilherme Chaguri
 */
@Slf4j
@SuppressWarnings("unchecked")
public class FileHandler {

    private final FTPConnection con;

    private final SpaceListener sl;

    private final String sp = "/";

    // private final String ssp = "\\\\";
    private Control control;

    private File cwd = null;

    private File rnFile = null;

    private long start = 0;

    private String spacePath = null;

    private String spaceId = null;

    private String spaceCode = null;

    // private int rootLength = 0;
    public FileHandler(FTPConnection connection) {
        this.con = connection;
        this.sl = connection.getServer().getSpaceListener();
        this.control = new ControlImpl(con);
    }

    public void setFileSystem(Control control) {
        this.control = control;
        this.cwd = control.getRoot();
    }

    public void registerCommands() {
        // Change Working Directory
        con.registerCommand("CWD", "CWD <file>", this::cwd);
        // Change to Parent Directory
        con.registerCommand("CDUP", "CDUP", this::cdup);
        // Retrieve Working Directory
        con.registerCommand("PWD", "PWD", this::pwd);
        // Create Directory
        con.registerCommand("MKD", "MKD <file>", this::mkd);
        // Delete Directory
        con.registerCommand("RMD", "RMD <file>", this::rmd);
        // Delete File
        con.registerCommand("DELE", "DELE <file>", this::dele);
        // List Files
        con.registerCommand("LIST", "LIST [file]", this::list);
        // List File Names
        con.registerCommand("NLST", "NLST [file]", this::nlst);
        // Retrieve File
        con.registerCommand("RETR", "RETR <file>", this::retr);
        // Store File
        con.registerCommand("STOR", "STOR <file>", this::stor);
        // Store Random File
        con.registerCommand("STOU", "STOU [file]", this::stou);
        // Append File
        con.registerCommand("APPE", "APPE <file>", this::appe);
        // Restart from a position
        con.registerCommand("REST", "REST <bytes>", this::rest);
        // Abort all data transfers
        con.registerCommand("ABOR", "ABOR", this::abor);
        // Allocate Space (Obsolete)
        con.registerCommand("ALLO", "ALLO <size>", this::allo);
        // Rename From
        con.registerCommand("RNFR", "RNFR <file>", this::rnfr);
        // Rename To
        con.registerCommand("RNTO", "RNTO <file>", this::rnto);
        // Structure Mount (Obsolete)
        con.registerCommand("SMNT", "SMNT <file>", this::smnt);
        // Change Permissions
        con.registerSiteCommand("CHMOD", "CHMOD <perm> <file>", this::site_chmod);
        // Modification Time (RFC 3659)
        con.registerCommand("MDTM", "MDTM <file>", this::mdtm);
        // File Size (RFC 3659)
        con.registerCommand("SIZE", "SIZE <file>", this::size);
        // File Information (RFC 3659)
        con.registerCommand("MLST", "MLST <file>", this::mlst);
        // List Files Information (RFC 3659)
        con.registerCommand("MLSD", "MLSD <file>", this::mlsd);
        // Change Working Directory (RFC 775) (Obsolete)
        con.registerCommand("XCWD", "XCWD <file>", this::cwd);
        // Change to Parent Directory (RFC 775) (Obsolete)
        con.registerCommand("XCUP", "XCUP", this::cdup);
        // Retrieve Working Directory (RFC 775) (Obsolete)
        con.registerCommand("XPWD", "XPWD", this::pwd);
        // Create Directory (RFC 775) (Obsolete)
        con.registerCommand("XMKD", "XMKD <file>", this::mkd);
        // Delete Directory (RFC 775) (Obsolete)
        con.registerCommand("XRMD", "XRMD <file>", this::rmd);
        // Change Modified Time (draft-somers-ftp-mfxx-04)
        con.registerCommand("MFMT", "MFMT <time> <file>", this::mfmt);
        // MD5 Digest (draft-twine-ftpmd5-00) (Obsolete)
        con.registerCommand("MD5", "MD5 <file>", this::md5);
        // MD5 Digest (draft-twine-ftpmd5-00) (Obsolete)
        con.registerCommand("MMD5", "MMD5 <file1, file2, ...>", this::mmd5);
        // Hash Digest (draft-bryan-ftpext-hash-02)
        con.registerCommand("HASH", "HASH <file>", this::hash);
        // Base Commands (RFC 5797)
        con.registerFeature("base");
        // Obsolete Commands (RFC 5797)
        con.registerFeature("hist");
        // Restart in stream mode (RFC 3659)
        con.registerFeature("REST STREAM");
        // Modification Time (RFC 3659)
        con.registerFeature("MDTM");
        // File Size (RFC 3659)
        con.registerFeature("SIZE");
        // File Information (RFC 3659)
        con.registerFeature("MLST Type*;Size*;Modify*;Perm*;");
        // TVFS Mechanism (RFC 3659)
        con.registerFeature("TVFS");
        // Change Modified Time (draft-somers-ftp-mfxx-04)
        con.registerFeature("MFMT");
        // MD5 Digest (draft-twine-ftpmd5-00)
        con.registerFeature("MD5");
        // Hash Digest (draft-bryan-ftpext-hash-02)
        con.registerFeature("HASH MD5;SHA-1;SHA-256");
        con.registerOption("MLST", "Type;Size;Modify;Perm;");
        con.registerOption("HASH", "MD5");
    }

    private File getFile(String path) throws IOException {
        if (path.equals("...") || path.equals("..")) {
            return cwd.toPath().getParent().toFile();
        } else if (path.equals("/")) {
            return control.getRoot();
        } else if (path.startsWith("/")) {
            return control.findFile(control.getRoot(), path.substring(1));
        } else {
            return control.findFile(cwd, path);
        }
    }

    private String getFilePath(String path) throws IOException {
        if (path.equals("...") || path.equals("..")) {
            return cwd.toPath().getParent().toString();
        } else if (path.equals("/")) {
            return control.getRoot().getPath();
        } else if (path.startsWith("/")) {
            return control.findFile(control.getRoot(), path.substring(1)).getPath();
        } else {
            return control.findFile(cwd, path).getPath();
        }
    }

    /**
     * Transfer
     */
    private void cwd(String path) throws IOException {
        Map<String, String> result = sl.saveSpaceId(con.getUsername(), path);
        if (!result.get("code").equals("500")) {
            spacePath = result.get("path");
            spaceCode = result.get("path").substring(1);
            spaceId = result.get("spaceId");
        }
        File dir = getFile(path);
        if (FileUtils.isFolder(dir.toPath())) {
            if (ftpAuth()) {
                con.sendResponse(403, "没有权限操作此空间!");
                return;
            }
            cwd = dir;
            con.sendResponse(250, "The working directory was changed");
        } else {
            con.sendResponse(550, "Not a valid directory");
        }
    }

    private void cdup() throws IOException {
        cwd = cwd.toPath().getParent().toFile();
        con.sendResponse(200, "The working directory was changed");
    }

    private void pwd() {
        String path = "/" + control.getPath(cwd);
        con.sendResponse(257, '"' + path + '"' + " CWD Name");
    }

    private void allo() {
        // Obsolete command. Accepts the command but takes no action
        con.sendResponse(200, "There's no need to allocate space");
    }

    private void rnfr(String path) throws IOException {
        rnFile = getFile(path);
        con.sendResponse(350, "Rename request received");
    }

    /**
     * File renaming
     */
    private void rnto(String path) throws IOException {
        if (StringUtils.isEmpty(spacePath)) {
            con.sendResponse(550, "Please enter the correct spatial identification code!");
            return;
        }
        if (ftpAuth()) {
            con.sendResponse(403, "没有权限操作此空间!");
            return;
        }
        if (rnFile == null) {
            con.sendResponse(503, "No rename request was received");
            return;
        }
        String username = con.getUsername();
        File sou = rnFile;
        File tar = getFile(path);
        if (!sou.getPath().contains(spaceCode) || !tar.getPath().contains(spaceCode)) {
            con.sendResponse(550, "File not found");
            return;
        }
        if (sl.renameFile(username, sou.getName(), tar.getName())) {
            con.sendResponse(550, "The file name cannot contain special characters!");
            return;
        }
        try {
            if (sou.getParent().equals(tar.getParent())) {
                control.rename(rnFile.toPath(), tar.toPath(), SpaceSvnLog.FTP);
            } else {
                control.move(rnFile.toPath(), tar.toPath(), SpaceSvnLog.FTP);
            }
        } catch (RuntimeException e) {
            con.sendResponse(403, "权限不足,请联系空间管理员!");
            return;
        }
        rnFile = null;
        con.sendResponse(250, "File successfully renamed");
    }

    /**
     * Upload files
     */
    private void stor(String path) throws IOException {
        if (StringUtils.isEmpty(spacePath)) {
            con.sendResponse(550, "Please enter the correct spatial identification code!");
            return;
        }
        if (ftpAuth()) {
            con.sendResponse(403, "没有权限操作此空间!");
            return;
        }
        File file = getFile(path);
        if (!file.getPath().contains(spaceCode)) {
            con.sendResponse(550, "File not found");
            return;
        }
        String username = this.con.getUsername();
        con.sendResponse(150, "Receiving a file stream for " + path);
        if (sl.uploadFile(username, path)) {
            con.sendResponse(550, "The file name cannot contain special characters!");
            return;
        }
        try {
            control.validateFileMakePermissions(con.getUsername(), spaceId);
        } catch (RuntimeException e) {
            con.sendResponse(403, "权限不足,请联系空间管理员!");
            return;
        }
        // Verify space capacity
        if (SpaceSizeControl.validation(spaceId)) {
            con.sendResponse(550, "The space capacity is full, please upload the file after expansion!");
            return;
        }
        boolean exists = file.exists();
        long size = exists ? file.length() : 0l;
        control.receiveStream(control.writeFile(file, start), file, spaceId, exists, size, (start > 0 ? 1 : 2));
        start = 0;
    }

    private void stou(String[] args) throws IOException {
        File file = null;
        String ext = ".tmp";
        if (ftpAuth()) {
            con.sendResponse(403, "没有权限操作此空间!");
            return;
        }
        try {
            control.validateFileMakePermissions(con.getUsername(), spaceId);
        } catch (RuntimeException e) {
            con.sendResponse(403, "权限不足,请联系空间管理员!");
            return;
        }
        // Verify space capacity
        if (SpaceSizeControl.validation(spaceId)) {
            con.sendResponse(550, "The space capacity is full, please upload the file after expansion!");
            return;
        }
        if (args.length > 0) {
            file = getFile(args[0]);
            int i = args[0].lastIndexOf('.');
            if (i > 0) {
                ext = args[0].substring(i);
            }
        }
        while (file != null && FileUtils.exists(file.toPath())) {
            // Quick way to generate simple random names
            // It's not the "perfect" solution, as it only uses hexadecimal characters
            // But definitely enough for file names
            String name = UUID.randomUUID().toString().replace("-", "");
            file = control.findFile(cwd, name + ext);
        }
        con.sendResponse(150, "File: " + control.getPath(file));
        boolean exists = file.exists();
        long size = exists ? file.length() : 0l;
        control.receiveStream(control.writeFile(file, 0), file, spaceId, exists, size, 2);
        // receiveStream(control.writeFile(file, 0));
    }

    private void appe(String path) throws IOException {
        if (ftpAuth()) {
            con.sendResponse(403, "没有权限操作此空间!");
            return;
        }
        try {
            control.validateFileMakePermissions(con.getUsername(), spaceId);
        } catch (RuntimeException e) {
            con.sendResponse(403, "权限不足,请联系空间管理员!");
            return;
        }
        // Verify space capacity
        if (SpaceSizeControl.validation(spaceId)) {
            con.sendResponse(550, "The space capacity is full, please upload the file after expansion!");
            return;
        }
        File file = getFile(path);
        con.sendResponse(150, "Receiving a file stream for " + path);
        boolean exists = FileUtils.exists(file.toPath());
        long size = exists ? file.length() : 0l;
        control.receiveStream(control.writeFile(file, exists ? control.getSizeBytes(file.toPath(), true) : 0), file, spaceId, exists, size, (exists ? 1 : 2));
        // receiveStream(control.writeFile(file, exists ? control.getSizeBytes(file.toPath(), true) : 0));
    }

    /**
     * File Download
     */
    private void retr(String path) throws IOException {
        if (ftpAuth()) {
            con.sendResponse(403, "没有权限操作此空间!");
            return;
        }
        File file = getFile(path);
        String username = con.getUsername();
        if (username.contains(".") || username.length() > 6) {
            try {
                control.validateFileOtherPermissions(username, spaceId, file.getPath(), "down");
            } catch (RuntimeException e) {
                con.sendResponse(403, "权限不足,请联系空间管理员!");
                return;
            }
        }
        con.sendResponse(150, "Sending the file stream for " + path + " (" + control.getSizeBytes(file.toPath(), true) + " bytes)");
        sendStream(Utils.readFileSystem(control, file, start, con.isAsciiMode()));
        control.stateDown(spaceId, file.length(), "ftp");
        start = 0;
    }

    private void rest(String byteStr) {
        long bytes = Long.parseLong(byteStr);
        if (bytes >= 0) {
            start = bytes;
            con.sendResponse(350, "Restarting at " + bytes + ". Ready to receive a RETR or STOR command");
        } else {
            con.sendResponse(501, "The number of bytes should be greater or equal to 0");
        }
    }

    private void abor() throws IOException {
        con.abortDataTransfers();
        con.sendResponse(226, "All transfers were aborted successfully");
    }

    private void list(String[] args) throws IOException {
        con.sendResponse(150, "Sending file list...");
        // "-l" is not present in any specification, but chrome uses it
        // TODO remove this when the bug gets fixed
        // https://bugs.chromium.org/p/chromium/issues/detail?id=706905
        if (StringUtils.isEmpty(spacePath)) {
            con.sendResponse(550, "Please enter the correct spatial identification code!");
            return;
        }
        String dir = args.length > 0 && !args[0].equals("-l") ? getFilePath(args[0]) : control.getRoot().getPath() + spacePath;
        // File dir = args.length > 0 && !args[0].equals("-l") ? getFile(args[0]) : new File(control.getRoot().getPath(), spacePath);
        if (!dir.contains(spaceCode)) {
            con.sendResponse(550, "Not a directory");
            return;
        }
        if (!dir.equals(control.getRoot().getPath() + spacePath)) {
            if (!control.isFolder(dir, spaceId)) {
                con.sendResponse(550, "Not a directory");
                return;
            }
        } else {
            dir = "/";
        }
        StringBuilder data = new StringBuilder();
        // Get subordinate files
        List<FileMapping> fileMappingList = control.getFileMappingList(dir, spaceId);
        for (FileMapping fileMapping : fileMappingList) {
            data.append(Utils.format(fileMapping));
        }
        // for (File file : control.listFiles(dir)) {
        // if (".svn".equals(file.getName())) {
        // continue;
        // }
        // data.append(Utils.format(control, file));
        // }
        con.sendData(data.toString().getBytes(StandardCharsets.UTF_8));
        con.sendResponse(226, "The list was sent");
    }

    private void nlst(String[] args) throws IOException {
        con.sendResponse(150, "Sending file list...");
        // "-l" is not present in any specification, but chrome uses it
        // TODO remove this when the bug gets fixed
        // https://bugs.chromium.org/p/chromium/issues/detail?id=706905
        // Object dir = args.length > 0 && !args[0].equals("-l") ? getFile(args[0]) : cwd;
        if (StringUtils.isEmpty(spacePath)) {
            con.sendResponse(550, "Please enter the correct spatial identification code!");
            return;
        }
        String dir = args.length > 0 && !args[0].equals("-l") ? getFilePath(args[0]) : control.getRoot().getPath() + spacePath;
        // File dir = args.length > 0 && !args[0].equals("-l") ? getFile(args[0]) : new File(control.getRoot().getPath(), spacePath);
        if (!dir.contains(spaceCode)) {
            con.sendResponse(550, "Not a directory");
            return;
        }
        if (!dir.equals(control.getRoot().getPath() + spacePath)) {
            if (!control.isFolder(dir, spaceId)) {
                con.sendResponse(550, "Not a directory");
                return;
            }
        } else {
            dir = "/";
        }
        StringBuilder data = new StringBuilder();
        // Get subordinate files
        List<FileMapping> fileMappingList = control.getFileMappingList(dir, spaceId);
        for (FileMapping fileMapping : fileMappingList) {
            data.append(fileMapping.getName()).append("\r\n");
        }
        // for (File file : control.listFiles(dir)) {
        // if (".svn".equals(file.getName())) {
        // continue;
        // }
        // data.append(file.getName()).append("\r\n");
        // }
        con.sendData(data.toString().getBytes(StandardCharsets.UTF_8));
        con.sendResponse(226, "The list was sent");
    }

    /**
     * Delete folder
     */
    private void rmd(String path) throws IOException {
        if (StringUtils.isEmpty(spacePath)) {
            con.sendResponse(550, "Please enter the correct spatial identification code!");
            return;
        }
        if (ftpAuth()) {
            con.sendResponse(403, "没有权限操作此空间!");
            return;
        }
        File file = getFile(path);
        if (!file.getPath().contains(spaceCode) || file.getName().equals(spaceCode)) {
            con.sendResponse(403, "Insufficient permissions (the directory does not support operations)!");
            return;
        }
        if (!FileUtils.isFolder(file.toPath())) {
            con.sendResponse(550, "Not a directory");
            return;
        }
        sl.deleteFile(con.getUsername(), path, "folder");
        try {
            control.delete(file.toPath(), SpaceSvnLog.FTP);
        } catch (RuntimeException e) {
            con.sendResponse(403, "权限不足,请联系空间管理员!");
            return;
        }
        con.sendResponse(250, '"' + path + '"' + " Directory Deleted");
    }

    /**
     * Delete files
     */
    private void dele(String path) throws IOException {
        if (StringUtils.isEmpty(spacePath)) {
            con.sendResponse(550, "Please enter the correct spatial identification code!");
            return;
        }
        if (ftpAuth()) {
            con.sendResponse(403, "没有权限操作此空间!");
            return;
        }
        File file = getFile(path);
        String path1 = file.getPath();
        if (!path1.contains(spaceCode)) {
            con.sendResponse(403, "Insufficient permissions (the directory does not support operations)!");
            return;
        }
        if (FileUtils.isFolder(file.toPath())) {
            con.sendResponse(550, "Not a file");
            return;
        }
        sl.deleteFile(con.getUsername(), path, "file");
        try {
            control.delete(file.toPath(), SpaceSvnLog.FTP);
        } catch (RuntimeException e) {
            con.sendResponse(403, "权限不足,请联系空间管理员!");
            return;
        }
        con.sendResponse(250, '"' + path + '"' + " File Deleted");
    }

    /**
     * Create File
     */
    private void mkd(String path) throws IOException {
        if (StringUtils.isEmpty(spacePath)) {
            con.sendResponse(550, "Please enter the correct spatial identification code!");
            return;
        }
        if (ftpAuth()) {
            con.sendResponse(403, "没有权限操作此空间!");
            return;
        }
        File file = getFile(path);
        if (!file.getPath().contains(spaceCode)) {
            con.sendResponse(403, "Insufficient permissions (the directory does not support operations)!");
            return;
        }
        if (sl.mkdirFile(con.getUsername(), file.getName())) {
            con.sendResponse(550, "The file name cannot contain special characters!");
            return;
        }
        try {
            control.createFolder(file.toPath(), false, true, SpaceSvnLog.FTP);
        } catch (RuntimeException r) {
            con.sendResponse(403, "权限不足,请联系空间管理员!");
            return;
        }
        con.sendResponse(257, '"' + path + '"' + " Directory Created");
    }

    private void smnt() {
        // Obsolete command. The server should respond with a 502 code
        con.sendResponse(502, "SMNT is not implemented in this server");
    }

    private void site_chmod(String[] cmd) throws IOException {
        if (cmd.length <= 1) {
            con.sendResponse(501, "Missing parameters");
            return;
        }
        control.chmod(getFile(cmd[1]), Utils.fromOctal(cmd[0]));
        con.sendResponse(200, "The file permissions were successfully changed");
    }

    private void mdtm(String path) throws IOException {
        File file = getFile(path);
        con.sendResponse(213, Utils.toMdtmTimestamp(FileUtils.getLastModifiedTime(file.toPath())));
    }

    private void size(String path) throws IOException {
        File file = getFile(path);
        con.sendResponse(213, Long.toString(control.getSizeBytes(file.toPath(), true)));
    }

    private void mlst(String[] args) throws IOException {
        // File file = args.length > 0 ? getFile(args[0]) : cwd;
        if (StringUtils.isEmpty(spacePath)) {
            con.sendResponse(550, "Please enter the correct spatial identification code!");
            return;
        }
        String dir = args.length > 0 ? getFilePath(args[0]) : control.getRoot().getPath() + spacePath;
        // File dir = args.length > 0 ? getFile(args[0]) : new File((control.getRoot()).getPath(), spacePath);
        if (!dir.contains(spaceCode)) {
            con.sendResponse(550, "File not found");
            return;
        }
        if (!dir.equals(control.getRoot().getPath() + spacePath)) {
            if (!control.isFolder(dir, spaceId)) {
                con.sendResponse(550, "File not found");
                return;
            }
        } else {
            dir = "/";
        }
        FileMapping fileMapping = control.getFileMapping(dir, spaceId);
        String[] options = con.getOption("MLST").split(";");
        String facts = Utils.getFacts(fileMapping, options);
        con.sendResponse(250, "- Listing " + fileMapping.getName() + "\r\n" + facts);
        con.sendResponse(250, "End");
    }

    private void mlsd(String[] args) throws IOException {
        // File file = args.length > 0 ? getFile(args[0]) : cwd;
        if (StringUtils.isEmpty(spacePath)) {
            con.sendResponse(550, "Please enter the correct spatial identification code!");
            return;
        }
        String file = args.length > 0 ? getFilePath(args[0]) : control.getRoot().getPath() + spacePath;
        // File file = args.length > 0 ? getFile(args[0]) : new File((control.getRoot()).getPath(), spacePath);
        if (!file.contains(spaceCode)) {
            con.sendResponse(550, "File not found");
            return;
        }
        if (!file.equals(control.getRoot().getPath() + spacePath)) {
            if (!control.isFolder(file, spaceId)) {
                con.sendResponse(550, "Not a directory");
                return;
            }
        } else {
            file = "/";
        }
        if (file.equals("/")) {
            SpaceSimple spaceSimple = FileOperationFactory.getCacheLoading().getSpaceSimple(spaceId);
            con.sendResponse(150, "欢迎访问 " + spaceSimple.getSpaceName() + " 空间.");
        } else {
            con.sendResponse(150, "Sending file information list...");
        }
        String[] options = con.getOption("MLST").split(";");
        StringBuilder data = new StringBuilder();
        List<FileMapping> fileMappingList = control.getFileMappingList(file, spaceId);
        for (FileMapping fileMapping : fileMappingList) {
            data.append(Utils.getFacts(fileMapping, options));
        }
        // for (File f : control.listFiles(file)) {
        // if (".svn".equals(f.getName())) {
        // continue;
        // }
        // data.append(Utils.getFacts(control, f, options));
        // }
        con.sendData(data.toString().getBytes(StandardCharsets.UTF_8));
        con.sendResponse(226, "The file list was sent!");
    }

    private void mfmt(String[] args) throws IOException {
        if (args.length < 2) {
            con.sendResponse(501, "Missing arguments");
            return;
        }
        File file = getFile(args[1]);
        long time;
        if (!FileUtils.exists(file.toPath())) {
            con.sendResponse(550, "File not found");
            return;
        }
        try {
            time = Utils.fromMdtmTimestamp(args[0]);
        } catch (ParseException ex) {
            con.sendResponse(500, "Couldn't parse the time");
            return;
        }
        control.touch(file, time);
        con.sendResponse(213, "Modify=" + args[0] + "; " + control.getPath(file));
    }

    private void md5(String path) throws IOException {
        String p = path = path.trim();
        if (p.length() > 2 && p.startsWith("\"") && p.endsWith("\"")) {
            // Remove the quotes
            p = p.substring(1, p.length() - 1).trim();
        }
        try {
            File file = getFile(p);
            byte[] digest = control.getDigest(file, "MD5");
            String md5 = new BigInteger(1, digest).toString(16);
            con.sendResponse(251, path + " " + md5);
        } catch (NoSuchAlgorithmException ex) {
            // Shouldn't ever happen
            con.sendResponse(504, ex.getMessage());
        }
    }

    private void mmd5(String args) throws IOException {
        String[] paths = args.split(",");
        StringBuilder response = new StringBuilder();
        try {
            for (String path : paths) {
                String p = path = path.trim();
                if (p.length() > 2 && p.startsWith("\"") && p.endsWith("\"")) {
                    // Remove the quotes
                    p = p.substring(1, p.length() - 1).trim();
                }
                File file = getFile(p);
                byte[] digest = control.getDigest(file, "MD5");
                String md5 = new BigInteger(1, digest).toString(16);
                if (response.length() > 0) {
                    response.append(", ");
                }
                response.append(path).append(" ").append(md5);
            }
            con.sendResponse(paths.length == 1 ? 251 : 252, response.toString());
        } catch (NoSuchAlgorithmException ex) {
            // Shouldn't ever happen
            con.sendResponse(504, ex.getMessage());
        }
    }

    private void hash(String path) throws IOException {
        try {
            File file = getFile(path);
            String hash = con.getOption("HASH");
            byte[] digest = control.getDigest(file, hash);
            String hex = new BigInteger(1, digest).toString(16);
            // TODO RANG
            con.sendResponse(213, String.format("%s 0-%s %s %s", hash, control.getSizeBytes(file.toPath(), true), hex, file.getName()));
        } catch (NoSuchAlgorithmException ex) {
            con.sendResponse(504, ex.getMessage());
        }
    }

    /**
     * Sends a stream asynchronously, sending a response after it's done
     *
     * @param in The stream
     */
    private void sendStream(InputStream in) {
        new Thread(() -> {
            try {
                con.sendData(in);
                con.sendResponse(226, "File sent!");
            } catch (ResponseException ex) {
                con.sendResponse(ex.getCode(), ex.getMessage());
            } catch (Exception ex) {
                con.sendResponse(451, ex.getMessage());
            }
        }).start();
    }

    /**
     * Receives a stream asynchronously, sending a response after it's done
     *
     * @param out The stream
     */
    private void receiveStream(OutputStream out) {
        new Thread(() -> {
            try {
                con.receiveData(out);
                con.sendResponse(226, "File received!");
            } catch (ResponseException ex) {
                con.sendResponse(ex.getCode(), ex.getMessage());
            } catch (Exception ex) {
                con.sendResponse(451, ex.getMessage());
            }
        }).start();
    }

    /**
     * Access FTP permission verification
     */
    private boolean ftpAuth() {
        if (con.getUsername().contains(".") || con.getUsername().length() > 6) {
            try {
                FileOperationFactory.getSpaceStatisticConfig().validateSpacePermissions(con.getUsername(), spaceId, SpaceRoleEnum.F_OTHER_FTP.getRole());
            } catch (RuntimeException e) {
                return true;
            }
        }
        return false;
    }
}
