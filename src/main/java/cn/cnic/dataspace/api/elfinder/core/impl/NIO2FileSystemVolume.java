/*
 * #%L
 * %%
 * Copyright (C) 2015 Trustsystems Desenvolvimento de Sistemas, LTDA.
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the Trustsystems Desenvolvimento de Sistemas, LTDA. nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package cn.cnic.dataspace.api.elfinder.core.impl;

import cn.cnic.dataspace.api.config.space.FileOperationFactory;
import cn.cnic.dataspace.api.elfinder.core.Target;
import cn.cnic.dataspace.api.elfinder.core.Volume;
import cn.cnic.dataspace.api.elfinder.service.VolumeHandler;
import cn.cnic.dataspace.api.elfinder.support.content.detect.Detector;
import cn.cnic.dataspace.api.elfinder.support.content.detect.NIO2FileTypeDetector;
import cn.cnic.dataspace.api.filehandle.Control;
import cn.cnic.dataspace.api.filehandle.ControlImpl;
import cn.cnic.dataspace.api.middle.FileOperationHandler;
import cn.cnic.dataspace.api.middle.FileOperationHandlerImpl;
import cn.cnic.dataspace.api.model.space.SpaceSvnLog;
import cn.cnic.dataspace.api.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * NIO Filesystem Volume Implementation.
 */
@Slf4j
public class NIO2FileSystemVolume implements Volume {

    private final String alias;

    private final Path rootDir;

    private final Detector detector;

    private final FileOperationHandler FileOperationHandler;

    private final Control control;

    public NIO2FileSystemVolume(String alias, Path rootDir) {
        this.alias = alias;
        this.rootDir = rootDir;
        this.detector = new NIO2FileTypeDetector();
        FileOperationHandler = new FileOperationHandlerImpl();
        control = new ControlImpl();
        createRootDir();
    }

    private void createRootDir() {
        try {
            Target target = fromPath(rootDir);
            if (!exists(target)) {
                createFolder(target, false, false);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to create root dir folder", e);
        }
    }

    public Path fromTarget(Target target) {
        return ((NIO2FileSystemTarget) target).getPath();
    }

    private Target fromPath(Path path) {
        return fromPath(this, path);
    }

    private static Target fromPath(NIO2FileSystemVolume volume, Path path) {
        return new NIO2FileSystemTarget(volume, path);
    }

    public Path getRootDir() {
        return rootDir;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public void createFile(Target target) throws IOException {
        control.createFile(fromTarget(target));
    }

    @Override
    public void webCreateAndCopy(VolumeHandler src, VolumeHandler dst, String dent) throws IOException {
        control.webCreateAndCopy(src, dst, dent);
    }

    @Override
    public void move(Target src, Target dst) throws IOException {
        control.move(fromTarget(src), fromTarget(dst), SpaceSvnLog.ELFINDER);
    }

    // @Override
    // public void createFileRecord(Target target) throws IOException {
    // FileOperationHandler.createFileRecord(fromTarget(target));
    // }
    @Override
    public void uploadFile(Target target) throws IOException {
        FileOperationHandler.uploadFile(fromTarget(target));
    }

    @Override
    public void uploadFile(Target target, boolean record) throws IOException {
        FileOperationHandler.uploadFile(fromTarget(target), record);
    }

    @Override
    public void createFolder(Target target, boolean upload, boolean record) throws IOException {
        control.createFolder(fromTarget(target), upload, record, SpaceSvnLog.ELFINDER);
    }

    @Override
    public void delete(Target target) throws IOException {
        control.delete(fromTarget(target), SpaceSvnLog.ELFINDER);
    }

    // @Override
    // public void deleteFolder(Target target,boolean judge) throws IOException {
    // FileOperationHandler.deleteFolder(fromTarget(target),judge);
    // }
    @Override
    public void write(String filePath, String data, OutputStream output, String encoding) throws IOException {
        control.write(filePath, data, output, encoding);
    }

    // @Override
    // public void paste(String fileName, List<String> removed, String destination, boolean isFolder, int count,boolean cut) {
    // try {
    // control.paste(fileName, removed, destination, isFolder, count,cut);
    // } catch (IOException ioException) {
    // ioException.printStackTrace();
    // }
    // }
    @Override
    public void stateUpload(Target target, long data) {
        control.stateUpload(fromTarget(target), data);
    }

    @Override
    public boolean exists(Target target) {
        return FileUtils.exists(fromTarget(target));
    }

    @Override
    public Target fromPath(String relativePath) {
        String rootDir = getRootDir().toString();
        Path path;
        if (relativePath.startsWith(rootDir)) {
            path = Paths.get(relativePath);
        } else {
            path = Paths.get(rootDir, relativePath);
        }
        return fromPath(path);
    }

    @Override
    public long getLastModified(Target target) throws IOException {
        return FileUtils.getLastModifiedTime(fromTarget(target));
    }

    @Override
    public String getMimeType(Target target) throws IOException {
        Path path = fromTarget(target);
        return detector.detect(path);
    }

    @Override
    public String getName(Target target) {
        return fromTarget(target).getFileName().toString();
    }

    @Override
    public Target getParent(Target target) {
        Path path = fromTarget(target).getParent();
        ;
        return fromPath(path);
    }

    @Override
    public String getPath(Target target) {
        Path rootPath = getRootDir();
        Path path = fromTarget(target);
        String relativePath = "";
        String r = rootPath.toString().trim();
        String p = path.toString().trim();
        if (!p.equalsIgnoreCase(r) && p.startsWith(r)) {
            relativePath = path.subpath(rootPath.getNameCount(), path.getNameCount()).toString();
        }
        return relativePath;
    }

    @Override
    public Target getRoot() {
        return fromPath(getRootDir());
    }

    @Override
    public long getSize(Target target) throws IOException {
        Path path = fromTarget(target);
        boolean recursiveSize = FileUtils.isFolder(path);
        return control.getSizeBytes(path, recursiveSize);
    }

    @Override
    public boolean isFolder(Target target) {
        return FileUtils.isFolder(fromTarget(target));
    }

    @Override
    public boolean isRoot(Target target) throws IOException {
        return Files.isSameFile(getRootDir(), fromTarget(target));
    }

    @Override
    public boolean hasChildFolder(Target target) throws IOException {
        Path dir = fromTarget(target);
        if (FileUtils.isFolder(dir)) {
            File file = new File(dir.toString());
            return Objects.requireNonNull(file.listFiles()).length > 0;
        }
        return false;
    }

    @Override
    public Target[] listChildren(Target target) throws IOException {
        List<Path> childrenResultList = control.listChildrenNotHidden(fromTarget(target));
        List<Target> targets = new ArrayList<>(childrenResultList.size());
        for (Path path : childrenResultList) {
            targets.add(fromPath(path));
        }
        return targets.toArray(new Target[0]);
    }

    @Override
    public InputStream openInputStream(Target target) throws IOException {
        return Files.newInputStream(fromTarget(target));
    }

    @Override
    public OutputStream openOutputStream(Target target) throws IOException {
        return Files.newOutputStream(fromTarget(target));
    }

    @Override
    public void rename(Target origin, Target destination) throws IOException {
        control.rename(fromTarget(origin), fromTarget(destination), SpaceSvnLog.ELFINDER);
    }

    @Override
    public List<Target> search(String target) throws IOException {
        String spaceId = control.spaceId(getRootDir());
        List<Path> searchResultList = control.search(getRootDir(), target);
        List<Target> targets = new ArrayList<>(searchResultList.size());
        List<String> pathList = new ArrayList<>(searchResultList.size());
        for (Path path : searchResultList) {
            targets.add(fromPath(path));
            pathList.add(path.toString());
        }
        // List<String> result = FileOperationFactory.getSpaceStatisticConfig().searchFileData(target, spaceId);
        // for (String res : result) {
        // if(!pathList.contains(res)){
        // targets.add(fromPath(new File(res).toPath()));
        // }
        // }
        return Collections.unmodifiableList(targets);
    }
}
