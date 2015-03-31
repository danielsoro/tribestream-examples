/*
 * Tomitribe Confidential
 *
 * Copyright(c) Tomitribe Corporation. 2014
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package org.supertribe.arquillian;

import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schemaextractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaextractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schemaloader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.exception.Exceptions;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.CacheService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.openejb.arquillian.common.Files;
import org.jboss.arquillian.container.spi.event.container.AfterUnDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.TestClass;

import java.io.File;
import java.io.InputStream;
import java.util.List;


public class LdapServerLifecycleExecuter {

    private File apachedsDir;

    private DirectoryService service;
    private LdapServer server;

    private void startLdapServer() throws Exception {
        apachedsDir = new File(System.getProperty("java.io.tmpdir"), "apacheds");
        Files.deleteOnExit(apachedsDir);
        apachedsDir.mkdirs();

        initDirectoryService(apachedsDir);

        server = new LdapServer();
        int serverPort = 12389;
        server.setTransports(new TcpTransport(serverPort));
        server.setDirectoryService(service);

        server.start();
    }

    private void stopLdapServer() throws Exception {
        server.stop();
        service.shutdown();

        // TODO: wipe apachedsDir
    }

    private void initSchemaPartition() throws Exception {
        final InstanceLayout instanceLayout = this.service.getInstanceLayout();

        final File schemaPartitionDirectory = new File(
                instanceLayout.getPartitionsDirectory(), "schema");

        if (schemaPartitionDirectory.exists()) {
            System.out
                    .println("schema partition already exists, skipping schema extraction");
        } else {
            final SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(
                    instanceLayout.getPartitionsDirectory());
            extractor.extractOrCopy();
        }

        final SchemaLoader loader = new LdifSchemaLoader(
                schemaPartitionDirectory);
        final SchemaManager schemaManager = new DefaultSchemaManager(loader);

        schemaManager.loadAllEnabled();

        final List<Throwable> errors = schemaManager.getErrors();

        if (errors.size() != 0) {
            throw new Exception(I18n.err(I18n.ERR_317,
                    Exceptions.printErrors(errors)));
        }

        this.service.setSchemaManager(schemaManager);

        final LdifPartition schemaLdifPartition = new LdifPartition(
                schemaManager, service.getDnFactory());
        schemaLdifPartition.setPartitionPath(schemaPartitionDirectory.toURI());

        final SchemaPartition schemaPartition = new SchemaPartition(
                schemaManager);
        schemaPartition.setWrappedPartition(schemaLdifPartition);
        this.service.setSchemaPartition(schemaPartition);
    }

    private void initDirectoryService(final File workDir) throws Exception {
        this.service = new DefaultDirectoryService();
        this.service.setInstanceLayout(new InstanceLayout(workDir));

        final CacheService cacheService = new CacheService();
        cacheService.initialize(this.service.getInstanceLayout());

        this.service.setCacheService(cacheService);

        this.initSchemaPartition();

        final JdbmPartition systemPartition = new JdbmPartition(
                this.service.getSchemaManager(), service.getDnFactory());
        systemPartition.setId("system");
        systemPartition.setPartitionPath(new File(this.service
                .getInstanceLayout().getPartitionsDirectory(), systemPartition
                .getId()).toURI());
        systemPartition.setSuffixDn(new Dn(ServerDNConstants.SYSTEM_DN));
        systemPartition.setSchemaManager(this.service.getSchemaManager());

        this.service.setSystemPartition(systemPartition);

        this.service.getChangeLog().setEnabled(false);
        this.service.setDenormalizeOpAttrsEnabled(true);

        this.service.startup();

        service.getChangeLog().setEnabled(false);
        service.setDenormalizeOpAttrsEnabled(true);

        service.startup();

        final InputStream is = getClass().getResourceAsStream("/import.ldif");

        final LdifReader reader = new LdifReader(is);
        while (reader.hasNext()) {
            final LdifEntry ldifEntry = reader.next();
            service.getAdminSession().add(new DefaultEntry(service.getSchemaManager(), ldifEntry.getEntry()));
        }

        reader.close();
        service.sync();
    }

    public void executeBeforeDeploy(@Observes BeforeDeploy event, TestClass testClass) throws Exception {
        startLdapServer();
    }

    public void executeAfterUnDeploy(@Observes AfterUnDeploy event, TestClass testClass) throws Exception {
        stopLdapServer();
    }
}
