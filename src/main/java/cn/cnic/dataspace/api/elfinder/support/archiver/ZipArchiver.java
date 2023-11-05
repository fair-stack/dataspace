package cn.cnic.dataspace.api.elfinder.support.archiver;

import cn.cnic.dataspace.api.config.space.FileOperationFactory;
import cn.cnic.dataspace.api.elfinder.core.Target;
import cn.cnic.dataspace.api.elfinder.core.Volume;
import cn.cnic.dataspace.api.exception.CommonException;
import cn.cnic.dataspace.api.util.CommonUtils;
import cn.cnic.dataspace.api.util.FileSizeComputer;
import cn.cnic.dataspace.api.util.SvnUtil;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;

/**
 * Zip Archiver Implementation.
 */
@Slf4j
public class ZipArchiver extends AbstractArchiver implements Archiver {

    private static final CRC32 crc32 = new CRC32();

    @Override
    public String getMimeType() {
        return "application/zip";
    }

    @Override
    public String getExtension() {
        return ".zip";
    }

    @Override
    public ArchiveEntry createArchiveEntry(String targetPath, long targetSize, byte[] targetBytes) {
        ZipArchiveEntry zipEntry = new ZipArchiveEntry(targetPath);
        zipEntry.setSize(targetSize);
        zipEntry.setMethod(ZipEntry.STORED);
        if (targetBytes != null) {
            zipEntry.setCrc(crc32Checksum(targetBytes));
        }
        return zipEntry;
    }

    @Override
    public ArchiveOutputStream createArchiveOutputStream(BufferedOutputStream bufferedOutputStream) {
        return new ZipArchiveOutputStream(bufferedOutputStream);
    }

    @Override
    public ArchiveInputStream createArchiveInputStream(BufferedInputStream bufferedInputStream) throws IOException {
        return new ZipArchiveInputStream(bufferedInputStream);
    }

    @Override
    public Target compress(Target... targets) throws IOException, RuntimeException, ZipException {
        // this one
        Target compressTarget = null;
        for (Target target : targets) {
            Volume volume = target.getVolume();
            // String targetPath = volume.getParent(target).toString().substring(0, SvnUtil.EXAMPLE.length()) + "/" + volume.getPath(target);
            String targetPath = volume.getParent(target).toString() + "/" + volume.getName(target);
            final File file = new File(targetPath + ".zip");
            if (file.exists()) {
                throw new RuntimeException(CommonUtils.messageInternational("ZIP_ERROR"));
            }
            File fileTarget;
            File[] fs = new File(targetPath).listFiles();
            if (Objects.requireNonNull(fs).length > 0) {
                net.lingala.zip4j.core.ZipFile zipFile = new net.lingala.zip4j.core.ZipFile(targetPath + ".zip");
                ZipParameters parameters = new ZipParameters();
                parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
                parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
                for (File f : Objects.requireNonNull(fs)) {
                    if (f.isDirectory()) {
                        zipFile.addFolder(f.getPath(), parameters);
                    } else {
                        zipFile.addFile(f, parameters);
                    }
                }
                fileTarget = zipFile.getFile();
            } else {
                // empty folder
                throw new RuntimeException(CommonUtils.messageInternational("ZIP_EMPTY_ERROR"));
            }
            compressTarget = target.getVolume().fromPath(fileTarget.toString());
        }
        return compressTarget;
    }

    @Override
    public Target decompress(Target targetCompress) throws IOException {
        // this one
        Target decompressTarget;
        final Volume volume = targetCompress.getVolume();
        // gets the compress target infos
        final String src = targetCompress.toString();
        final String dest = removeExtension(src);
        // create zipFile instance to read the compress target
        // contents  and auto close it
        try (ZipFile zipFile = new ZipFile(src)) {
            // creates the decompress target infos
            Path decompressDir = Paths.get(dest);
            // creates a new decompress folder to not override if already exists
            // if you do not want this behavior, just comment this line
            decompressDir = createFile(false, decompressDir.getParent(), decompressDir);
            decompressTarget = volume.fromPath(decompressDir.toString());
            // creates the dest folder if not exists
            volume.createFolder(decompressTarget, false, false);
            // get space path
            String rootDir = FileOperationFactory.getSpaceUrl().getRootDir();
            String replace = src.replaceAll(rootDir, "");
            String[] pathSplit = replace.split(CommonUtils.FILE_SPLIT);
            String spaceId = pathSplit.length >= 3 ? pathSplit[2] : "";
            String spacePath = rootDir + "/" + pathSplit[1] + "/" + pathSplit[2];
            // get the compress target list entry
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            AtomicLong atomicLong = new AtomicLong();
            while (entries.hasMoreElements()) {
                final ZipArchiveEntry zipArchiveEntry = entries.nextElement();
                atomicLong.getAndAdd(zipArchiveEntry.getSize());
                Long spaceSize = FileOperationFactory.getSpaceRepository().findById(spaceId).get().getSpaceSize();
                Long usageSize = FileSizeComputer.FORK_JOIN_POOL.invoke(new FileSizeComputer(new File(spacePath)));
                if (zipFile.canReadEntryData(zipArchiveEntry) && usageSize + atomicLong.get() < spaceSize) {
                    // get the entry infos
                    final String entryName = zipArchiveEntry.getName();
                    // read
                    final InputStream archiveInputStream = zipFile.getInputStream(zipArchiveEntry);
                    final Target target = volume.fromPath(Paths.get(decompressDir.toString(), entryName).toString());
                    final Target parent = volume.getParent(target);
                    // create parent folder if not exists
                    if (parent != null && !volume.exists(parent)) {
                        volume.createFolder(parent, false, false);
                    }
                    if (!zipArchiveEntry.isDirectory()) {
                        // open streams to write the decompress target contents and auto close it
                        try (BufferedOutputStream outputStream = new BufferedOutputStream(volume.openOutputStream(target))) {
                            // write  out buffer
                            byte[] buffer = new byte[1073741824];
                            int n;
                            while (-1 != (n = archiveInputStream.read(buffer))) {
                                outputStream.write(buffer, 0, n);
                                /* if (FileSizeComputer.FORK_JOIN_POOL.invoke(new FileSizeComputer(new File(spacePath))) >= spaceSize) {
                                    throw new CommonException(CommonUtils.messageInternational("SPACE_FULL"));
                                }*/
                            }
                        }
                    }
                } else {
                    throw new CommonException(CommonUtils.messageInternational("SPACE_FULL"));
                }
            }
        }
        return decompressTarget;
    }

    public static long crc32Checksum(byte[] bytes) {
        crc32.update(bytes);
        long checksum = crc32.getValue();
        crc32.reset();
        return checksum;
    }
}
