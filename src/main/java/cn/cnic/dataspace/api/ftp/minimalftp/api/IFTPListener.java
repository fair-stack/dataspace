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
package cn.cnic.dataspace.api.ftp.minimalftp.api;

import cn.cnic.dataspace.api.ftp.minimalftp.FTPConnection;

/**
 * Listens for events
 * @author Guilherme Chaguri
 */
public interface IFTPListener {

    /**
     * Triggered when a new connection is created
     * @param con The new connection
     */
    void onConnected(FTPConnection con);

    /**
     * Triggered when a connection disconnects
     * @param con The connection
     */
    void onDisconnected(FTPConnection con);
}
