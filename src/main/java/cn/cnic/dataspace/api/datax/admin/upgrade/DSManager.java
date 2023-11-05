/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.cnic.dataspace.api.datax.admin.upgrade;

import cn.cnic.dataspace.api.datax.admin.upgrade.utils.SchemaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * upgrade manager
 */
public class DSManager {

    private static final Logger logger = LoggerFactory.getLogger(DSManager.class);

    UpgradeDao upgradeDao;

    /**
     * init upgrade dao
     */
    private void initUpgradeDao() {
        upgradeDao = MysqlUpgradeDao.getInstance();
    }

    /**
     * constructor init
     */
    public DSManager() {
        initUpgradeDao();
    }

    /**
     * init DolphinScheduler
     */
    public void initDS() {
        // Determines whether the ds table structure has been init
        if (upgradeDao.isExistsTable("ds_version")) {
            logger.info("The database has been initialized. Skip the initialization step");
            return;
        }
        this.initDSSchema();
    }

    /**
     * init ds Schema
     */
    public void initDSSchema() {
        logger.info("Start initializing the ds manager table structure");
        upgradeDao.initSchema();
    }

    /**
     * upgrade ds
     *
     * @throws Exception if error throws Exception
     */
    public void upgradeDS() throws Exception {
        // Gets a list of all upgrades
        List<String> schemaList = SchemaUtils.getAllSchemaList();
        if (schemaList == null || schemaList.size() == 0) {
            logger.info("There is no schema to upgrade!");
        } else {
            // Gets the version of the current system
            String version = upgradeDao.getCurrentVersion("ds_version");
            // The target version of the upgrade
            String schemaVersion = "";
            for (String schemaDir : schemaList) {
                schemaVersion = schemaDir.split("_")[0];
                if (SchemaUtils.isAGreatVersion(schemaVersion, version)) {
                    logger.info("upgrade ds metadata version from {} to {}", version, schemaVersion);
                    logger.info("Begin upgrading ds's table structure");
                    upgradeDao.upgradeDS(schemaDir);
                    version = schemaVersion;
                }
            }
        }
        // Assign the value of the version field in the version table to the version of the product
        upgradeDao.updateVersion(SchemaUtils.getSoftVersion());
    }
}
