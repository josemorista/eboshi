/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.CharSequence
 *  java.lang.Class
 *  java.lang.ClassNotFoundException
 *  java.lang.Deprecated
 *  java.lang.Integer
 *  java.lang.Object
 *  java.lang.String
 *  java.util.Set
 *  org.jetbrains.annotations.NotNull
 *  org.testcontainers.containers.ContainerLaunchException
 *  org.testcontainers.containers.JdbcDatabaseContainer
 *  org.testcontainers.utility.DockerImageName
 */
package org.testcontainers.containers;

import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

public class MySQLContainer<SELF extends MySQLContainer<SELF>>
extends JdbcDatabaseContainer<SELF> {
    public static final String NAME = "mysql";
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse((String)"mysql");
    @Deprecated
    public static final String DEFAULT_TAG = "5.7.34";
    @Deprecated
    public static final String IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();
    static final String DEFAULT_USER = "test";
    static final String DEFAULT_PASSWORD = "test";
    private static final String MY_CNF_CONFIG_OVERRIDE_PARAM_NAME = "TC_MY_CNF";
    public static final Integer MYSQL_PORT = 3306;
    private String databaseName = "test";
    private String username = "test";
    private String password = "test";
    private static final String MYSQL_ROOT_USER = "root";

    @Deprecated
    public MySQLContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public MySQLContainer(String dockerImageName) {
        this(DockerImageName.parse((String)dockerImageName));
    }

    public MySQLContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(new DockerImageName[]{DEFAULT_IMAGE_NAME});
        this.addExposedPort(MYSQL_PORT);
    }

    @Deprecated
    @NotNull
    protected Set<Integer> getLivenessCheckPorts() {
        return super.getLivenessCheckPorts();
    }

    protected void configure() {
        this.optionallyMapResourceParameterAsVolume(MY_CNF_CONFIG_OVERRIDE_PARAM_NAME, "/etc/mysql/conf.d", "mysql-default-conf", 16877);
        this.addEnv("MYSQL_DATABASE", this.databaseName);
        if (!MYSQL_ROOT_USER.equalsIgnoreCase(this.username)) {
            this.addEnv("MYSQL_USER", this.username);
        }
        if (this.password != null && !this.password.isEmpty()) {
            this.addEnv("MYSQL_PASSWORD", this.password);
            this.addEnv("MYSQL_ROOT_PASSWORD", this.password);
        } else if (MYSQL_ROOT_USER.equalsIgnoreCase(this.username)) {
            this.addEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "yes");
        } else {
            throw new ContainerLaunchException("Empty password can be used only with the root user");
        }
        this.setStartupAttempts(3);
    }

    public String getDriverClassName() {
        try {
            Class.forName((String)"com.mysql.cj.jdbc.Driver");
            return "com.mysql.cj.jdbc.Driver";
        }
        catch (ClassNotFoundException e) {
            return "com.mysql.jdbc.Driver";
        }
    }

    public String getJdbcUrl() {
        String additionalUrlParams = this.constructUrlParameters("?", "&");
        return "jdbc:mysql://" + this.getHost() + ":" + this.getMappedPort(MYSQL_PORT) + "/" + this.databaseName + additionalUrlParams;
    }

    protected String constructUrlForConnection(String queryString) {
        String url = super.constructUrlForConnection(queryString);
        if (!url.contains((CharSequence)"useSSL=")) {
            String separator = url.contains((CharSequence)"?") ? "&" : "?";
            url = url + separator + "useSSL=false";
        }
        if (!url.contains((CharSequence)"allowPublicKeyRetrieval=")) {
            url = url + "&allowPublicKeyRetrieval=true";
        }
        return url;
    }

    public String getDatabaseName() {
        return this.databaseName;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getTestQueryString() {
        return "SELECT 1";
    }

    public SELF withConfigurationOverride(String s) {
        this.parameters.put((Object)MY_CNF_CONFIG_OVERRIDE_PARAM_NAME, (Object)s);
        return (SELF)((Object)((MySQLContainer)this.self()));
    }

    public SELF withDatabaseName(String databaseName) {
        this.databaseName = databaseName;
        return (SELF)((Object)((MySQLContainer)this.self()));
    }

    public SELF withUsername(String username) {
        this.username = username;
        return (SELF)((Object)((MySQLContainer)this.self()));
    }

    public SELF withPassword(String password) {
        this.password = password;
        return (SELF)((Object)((MySQLContainer)this.self()));
    }
}
