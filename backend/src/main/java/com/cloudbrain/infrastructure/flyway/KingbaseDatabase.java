package com.cloudbrain.infrastructure.flyway;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.internal.jdbc.JdbcConnectionFactory;
import org.flywaydb.core.internal.jdbc.StatementInterceptor;
import org.flywaydb.database.postgresql.PostgreSQLDatabase;

public class KingbaseDatabase extends PostgreSQLDatabase {

    public KingbaseDatabase(
            Configuration configuration,
            JdbcConnectionFactory jdbcConnectionFactory,
            StatementInterceptor statementInterceptor) {
        super(configuration, jdbcConnectionFactory, statementInterceptor);
    }

    @Override
    public void ensureSupported(Configuration configuration) {
        notifyDatabaseIsNotFormallySupported();
    }
}
