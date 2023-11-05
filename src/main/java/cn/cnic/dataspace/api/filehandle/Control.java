package cn.cnic.dataspace.api.filehandle;

import cn.cnic.dataspace.api.elfinder.service.VolumeHandler;
import cn.cnic.dataspace.api.model.file.FileMapping;
import cn.cnic.dataspace.api.model.file.UploadFile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface Control<F extends Object> {

    boolean isFolder(String path, String spaceId);

    List<FileMapping> getFileMappingList(String path, String spaceId);

    FileMapping getFileMapping(String path, String spaceId);

    FileMapping getFileMappingAsHash(String hash, String spaceId);

    void validateFileMakePermissions(String email, String spaceId);

    void validateFileOtherPermissions(String email, String spaceId, String path, String type);

    void validateWebDavFileDownPermissions(String path);

    void validateWebDavFileCreatePermission(String path);

    void validateWebDavFileEditPermission(String path);

    long getFileSize(String path, String spaceId);

    // Copying web files and folders
    void webCreateAndCopy(VolumeHandler src, VolumeHandler dst, String dent) throws IOException;

    // Web file creation
    void createFile(Path path) throws IOException;

    // Web file writing
    void write(String filePath, String data, OutputStream output, String encoding) throws IOException;

    // Web sharding and merging
    void uploadMerge(List<UploadFile> uploadFiles, String realPath);

    // WebDAV file upload copy create file
    void createAndCopyFile(File file, InputStream in) throws IOException;

    // WebDAV file upload copy create file
    void createAndCopyFileNoAuth(File file, InputStream in) throws IOException;

    // FTP file upload copy create file renew file
    void receiveStream(OutputStream out, File file, String spaceId, boolean exists, long size, int ty);

    // File mobility supports web, webDAV, and FTP
    void move(Path origin, Path destination, String move) throws IOException;

    // Create a folder for a unified entrance
    void createFolder(Path path, boolean upload, boolean record, String type) throws IOException;

    // Unified entry file deletion
    void delete(Path path, String move) throws IOException;

    // Unified Entry File Renaming
    void rename(Path origin, Path destination, String move) throws IOException;

    // FTP file download outflow statistics
    void stateDown(String spaceId, long data, String type);

    String spaceId(Path path);

    // Web data upload inflow statistics - useless
    void stateUpload(Path fromTarget, long data);

    // It won't work after that
    List<Path> listChildrenNotHidden(Path dir) throws IOException;

    // I didn't use the search afterwards
    List<Path> search(Path path, String target) throws IOException;

    long getSizeBytes(Path path, boolean recursive) throws IOException;

    /* ftp  */
    /**
     * Retrieves the root file object
     * @return The file object
     */
    File getRoot();

    /**
     * Gets the relative path of a file from the file system's root
     * @param file The file object
     * @return The relative path
     */
    String getPath(F file);

    /**
     * Gets whether the file exists
     * @param file The file object
     * @return {@code true} if the file exists
     */
    boolean exists(F file);

    /**
     * Checks if the file is a directory
     * @param file The file object
     * @return {@code true} if the file is a directory
     */
    boolean isDirectory(F file);

    /**
     * Gets the permission number
     * @param file The file object
     * @return The octal permission number in decimal
     */
    int getPermissions(F file);

    /**
     * Gets the file size
     * @param file The file object
     * @return The file size in bytes
     */
    long getSize(F file);

    /**
     * Gets the modified time.
     * @param file The file object
     * @return The modified time in millis
     */
    long getLastModified(F file);

    /**
     * Gets the amount of hard links.
     * @param file The file object
     * @return The number of hard links
     */
    int getHardLinks(F file);

    /**
     * Gets the file name
     * @param file The file object
     * @return The file name
     */
    String getName(F file);

    /**
     * Gets the file owner
     * @param file The file object
     * @return The owner name
     */
    String getOwner(F file);

    /**
     * Gets the file group
     * @param file The file object
     * @return The group name
     */
    String getGroup(F file);

    /**
     * Gets (or calculates) the hash digest of a file.
     *
     * The algorithms "MD5", "SHA-1" and "SHA-256" are required to be implemented
     *
     * @param file The file object
     * @param algorithm The digest algorithm
     * @return The hash digest
     * @throws NoSuchAlgorithmException When the algorithm is not implement
     * @throws IOException When an error occurs
     */
    default byte[] getDigest(F file, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest d = MessageDigest.getInstance(algorithm);
        InputStream in = readFile(file, 0);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = in.read(bytes)) != -1) {
            d.update(bytes, 0, length);
        }
        return d.digest();
    }

    /**
     * Gets the parent directory of a file.
     *
     * This method should check for file access permissions
     *
     * @param file The file object
     * @return The parent file
     * @throws java.io.FileNotFoundException When there's no permission to access the file
     * @throws IOException When an error occurs
     */
    F getParent(F file) throws IOException;

    /**
     * Lists file names, including directories of a directory inside the file system.
     *
     * This method should check for file access permissions
     *
     * @param dir The directory file object
     * @return A file array
     * @throws IOException When an error occurs
     */
    File[] listFiles(F dir) throws IOException;

    /**
     * Finds a file based on the path.
     *
     * This method should check for file access permissions
     *
     * @param path The path
     * @return The found file
     * @throws java.io.FileNotFoundException When there's no permission to access the file or the file doesn't exist
     * @throws IOException When an error occurs
     */
    F findFile(String path) throws IOException;

    /**
     * Finds a file based on the path.
     *
     * This method should check for file access permissions
     *
     * @param cwd The base directory
     * @param path The path
     * @return The found file
     * @throws java.io.FileNotFoundException When there's no permission to access the file or the file doesn't exist
     * @throws IOException When an error occurs
     */
    File findFile(F cwd, String path) throws IOException;

    /**
     * Reads a file into an input stream
     * @param file The file object
     * @param start The position in bytes to start reading from
     * @return The input stream of the file
     * @throws IOException When an error occurs
     */
    InputStream readFile(F file, long start) throws IOException;

    /**
     * Writes a file into an output stream.
     *
     * If the file does not exist, creates the file
     *
     * @param file The file object
     * @param start The position in bytes to start writing to
     * @return The output stream of the file
     * @throws IOException When an error occurs
     */
    OutputStream writeFile(F file, long start) throws IOException;

    /**
     * Changes the permissions of a file
     * @param file The file object
     * @param perms The permissions number
     * @throws IOException When an error occurs
     */
    void chmod(F file, int perms) throws IOException;

    /**
     * Updates the modified time of a file
     * @param file The file object
     * @param time The new time in milliseconds
     * @throws IOException When an error occurs
     */
    void touch(F file, long time) throws IOException;
}
