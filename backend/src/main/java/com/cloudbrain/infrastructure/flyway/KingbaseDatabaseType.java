package com.cloudbrain.infrastructure.flyway;

import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;
import org.flywaydb.database.postgresql.PostgreSQLDatabaseType;

public class KingbaseDatabaseType extends PostgreSQLDatabaseType {

    private static final String JDBC_PREFIX = "jdbc:kingbase8:";
    private static final String P6SPY_JDBC_PREFIX = "jdbc:p6spy:kingbase8:";
    private static final String DRIVER_CLASS = "com.kingbase8.Driver";
    private static final String P6SPY_DRIVER_CLASS = "com.p6spy.engine.spy.P6SpyDriver";

    @Override
    public String getName() {
        return "KingbaseES";
    }

    @Override
    public List<String> getSupportedEngines() {
        return List.of("KingbaseES");
    }

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public boolean handlesJDBCUrl(String url) {
        return url != null && (url.startsWith(JDBC_PREFIX) || url.startsWith(P6SPY_JDBC_PREFIX));
    }

    @Override
    public String getDriverClass(String url, ClassLoader classLoader) {
        if (url != null && url.startsWith(P6SPY_JDBC_PREFIX)) {
            return P6SPY_DRIVER_CLASS;
        }
        return DRIVER_CLASS;
    }

    @Override
    public boolean handlesDatabaseProductNameAndVersion(String productName, String productVersion, Connection connection) {
        return productName != null && productName.toLowerCase(Locale.ROOT).contains("kingbase");
    }

    @Override
    public Database<?> createDatabase(
            Configuration configuration,
            JdbcConnectionFactory jdbcConnectionFactory,
            StatementInterceptor statementInterceptor) {
        return new KingbaseDatabase(configuration, jdbcConnectionFactory, statementInterceptor);
    }
}
