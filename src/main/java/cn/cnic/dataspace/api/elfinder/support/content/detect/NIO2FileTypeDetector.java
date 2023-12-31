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
package cn.cnic.dataspace.api.elfinder.support.content.detect;

import cn.cnic.dataspace.api.exception.CommonException;
import org.apache.tika.Tika;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import static cn.cnic.dataspace.api.exception.ExceptionType.SYSTEM_ERROR;

/**
 * NIO file type detector implementation that uses Tika API.
 */
public class NIO2FileTypeDetector extends java.nio.file.spi.FileTypeDetector implements Detector {

    private final Tika tika = new Tika();

    /**
     * Gets mime type from the given input stream.
     *
     * @return the mime type.
     * @throws IOException if the stream can not be read.
     */
    @Override
    public String detect(InputStream inputStream) throws IOException {
        return tika.detect(inputStream);
    }

    /**
     * Gets mime type from the given file path.
     *
     * @return the mime type.
     * @throws IOException if the file can not be read.
     */
    @Override
    public String detect(Path path) {
        if (Files.isDirectory(path)) {
            return "directory";
        }
        String detect = null;
        if (new File(path.toString()).exists()) {
            try {
                detect = tika.detect(path);
            } catch (IOException e) {
                throw new CommonException(SYSTEM_ERROR);
            }
        }
        return detect;
    }

    /**
     * Gets mime type from the given file path.
     *
     * @return the mime type.
     * @throws IOException if the file can not be read.
     */
    @Override
    public String probeContentType(Path path) throws IOException {
        return detect(path);
    }
}
