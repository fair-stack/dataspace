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

import cn.cnic.dataspace.api.datax.admin.entity.JobDatasource;
import cn.cnic.dataspace.api.datax.admin.tool.sql.JdbcConnectionFactory;
import cn.cnic.dataspace.api.datax.admin.upgrade.utils.ConnectionUtils;
import cn.cnic.dataspace.api.datax.admin.upgrade.utils.FileUtils;
import cn.cnic.dataspace.api.datax.admin.upgrade.utils.ScriptRunner;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import static cn.cnic.dataspace.api.datax.admin.config.Constant.DEFAULT_JDBC_DATABASE;

public abstract class UpgradeDao extends AbstractBaseDao {

    public static final Logger logger = LoggerFactory.getLogger(UpgradeDao.class);

    private static final String T_VERSION_NAME = "ds_version";

    protected static final DataSource dataSource = getDataSource();

    @Override
    protected void init() {
    }

    /**
     * get datasource
     *
     * @return DruidDataSource
     */
    public static DataSource getDataSource() {
        return getHikariDataSource();
    }

    /**
     * get sql dataSource
     *
     * @return
     */
    private static HikariDataSource getHikariDataSource() {
        JobDatasource jobDatasource = new JdbcConnectionFactory(DEFAULT_JDBC_DATABASE).getDataSource();
        // The default here is to use the hikari data source
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setUsername(jobDatasource.getJdbcUsername());
        dataSource.setPassword(jobDatasource.getJdbcPassword());
        dataSource.setJdbcUrl(jobDatasource.getJdbcUrl());
        dataSource.setDriverClassName(jobDatasource.getJdbcDriverClass());
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(0);
        dataSource.setConnectionTimeout(30000);
        return dataSource;
    }

    /**
     * init schema
     */
    public void initSchema() {
        String initSqlPath = FileUtils.getSqlRootPath() + "sql/create/release-1.0.0_schema/";
        initSchema(initSqlPath);
    }

    /**
     * init scheam
     *
     * @param initSqlPath initSqlPath
     */
    public void initSchema(String initSqlPath) {
        // Execute the ds DDL, it cannot be rolled back
        runInitDDL(initSqlPath);
        // Execute the ds DML, it can be rolled back
        runInitDML(initSqlPath);
    }

    /**
     * run DML
     *
     * @param initSqlPath initSqlPath
     */
    private void runInitDML(String initSqlPath) {
        Connection conn = null;
        // String mysqlSQLFilePath = resourceRoot + "/sql/create/release-1.0.0_schema/mysql/ds_dml.sql";
        String mysqlSQLFilePath = initSqlPath + "ds_dml.sql";
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            // Execute the ds_dml.sql script to import related data of ds
            ScriptRunner initScriptRunner = new ScriptRunner(conn, false, true);
            Reader initSqlReader = new FileReader(new File(mysqlSQLFilePath));
            initScriptRunner.runScript(initSqlReader);
            conn.commit();
        } catch (IOException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error(e1.getMessage(), e1);
            }
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (Exception e) {
            try {
                if (null != conn) {
                    conn.rollback();
                }
            } catch (SQLException e1) {
                logger.error(e1.getMessage(), e1);
            }
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            ConnectionUtils.releaseResource(conn);
        }
    }

    /**
     * run DDL
     *
     * @param initSqlPath initSqlPath
     */
    private void runInitDDL(String initSqlPath) {
        Connection conn = null;
        // String mysqlSQLFilePath = resourceRoot + "/sql/create/release-1.0.0_schema/mysql/ds_ddl.sql";
        String mysqlSQLFilePath = initSqlPath + "ds_ddl.sql";
        try {
            conn = dataSource.getConnection();
            // Execute the ds_ddl.sql script to create the table structure of ds
            ScriptRunner initScriptRunner = new ScriptRunner(conn, true, true);
            Reader initSqlReader = new FileReader(new File(mysqlSQLFilePath));
            initScriptRunner.runScript(initSqlReader);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            ConnectionUtils.releaseResource(conn);
        }
    }

    /**
     * determines whether a table exists
     *
     * @param tableName tableName
     * @return if table exist return true，else return false
     */
    public abstract boolean isExistsTable(String tableName);

    /**
     * get current version
     *
     * @param versionName versionName
     * @return version
     */
    public String getCurrentVersion(String versionName) {
        String sql = String.format("select version from %s", versionName);
        Connection conn = null;
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        String version = null;
        try {
            conn = dataSource.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                version = rs.getString(1);
            }
            return version;
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException("sql: " + sql, e);
        } finally {
            ConnectionUtils.releaseResource(rs, pstmt, conn);
        }
    }

    /**
     * upgrade ds
     *
     * @param schemaDir schema dir
     */
    public void upgradeDS(String schemaDir) {
        upgradeDSDDL(schemaDir);
        upgradeDSDML(schemaDir);
    }

    /**
     * upgrade ds DML
     *
     * @param schemaDir schemaDir
     */
    private void upgradeDSDML(String schemaDir) {
        String schemaVersion = schemaDir.split("_")[0];
        String sqlFilePath = MessageFormat.format("{0}sql/upgrade/{1}/ds_dml.sql", FileUtils.getSqlRootPath(), schemaDir);
        logger.info("sqlSQLFilePath" + sqlFilePath);
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            // Execute the upgraded ds dml
            ScriptRunner scriptRunner = new ScriptRunner(conn, false, true);
            Reader sqlReader = new FileReader(new File(sqlFilePath));
            scriptRunner.runScript(sqlReader);
            if (isExistsTable(T_VERSION_NAME)) {
                // Change version in the version table to the new version
                String upgradeSQL = String.format("update %s set version = ?", T_VERSION_NAME);
                pstmt = conn.prepareStatement(upgradeSQL);
                pstmt.setString(1, schemaVersion);
                pstmt.executeUpdate();
            }
            conn.commit();
        } catch (FileNotFoundException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error(e1.getMessage(), e1);
            }
            logger.error(e.getMessage(), e);
            throw new RuntimeException("sql file not found ", e);
        } catch (IOException e) {
            try {
                conn.rollback();
            } catch (SQLException e1) {
                logger.error(e1.getMessage(), e1);
            }
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (SQLException e) {
            try {
                if (null != conn) {
                    conn.rollback();
                }
            } catch (SQLException e1) {
                logger.error(e1.getMessage(), e1);
            }
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (Exception e) {
            try {
                if (null != conn) {
                    conn.rollback();
                }
            } catch (SQLException e1) {
                logger.error(e1.getMessage(), e1);
            }
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            ConnectionUtils.releaseResource(pstmt, conn);
        }
    }

    /**
     * upgrade ds DDL
     *
     * @param schemaDir schemaDir
     */
    private void upgradeDSDDL(String schemaDir) {
        String sqlFilePath = MessageFormat.format("{0}sql/upgrade/{1}/ds_ddl.sql", FileUtils.getSqlRootPath(), schemaDir);
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dataSource.getConnection();
            String dbName = conn.getCatalog();
            logger.info(dbName);
            conn.setAutoCommit(true);
            // Execute the dolphinscheduler ddl.sql for the upgrade
            ScriptRunner scriptRunner = new ScriptRunner(conn, true, true);
            Reader sqlReader = new FileReader(new File(sqlFilePath));
            scriptRunner.runScript(sqlReader);
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException("sql file not found ", e);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        } finally {
            ConnectionUtils.releaseResource(pstmt, conn);
        }
    }

    /**
     * update version
     *
     * @param version version
     */
    public void updateVersion(String version) {
        // Change version in the version table to the new version
        String versionName = T_VERSION_NAME;
        String upgradeSQL = String.format("update %s set version = ?", versionName);
        PreparedStatement pstmt = null;
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            pstmt = conn.prepareStatement(upgradeSQL);
            pstmt.setString(1, version);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException("sql: " + upgradeSQL, e);
        } finally {
            ConnectionUtils.releaseResource(pstmt, conn);
        }
    }
}
