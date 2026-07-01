package com.cloudbrain.infrastructure.flyway;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KingbaseDatabaseTypeTest {

    private final KingbaseDatabaseType databaseType = new KingbaseDatabaseType();

    @Test
    void handlesKingbaseJdbcUrlsOnly() {
        assertThat(databaseType.handlesJDBCUrl("jdbc:kingbase8://127.0.0.1:54321/cloudbrain_medical")).isTrue();
        assertThat(databaseType.handlesJDBCUrl("jdbc:p6spy:kingbase8://127.0.0.1:54321/cloudbrain_medical")).isTrue();
        assertThat(databaseType.handlesJDBCUrl("jdbc:postgresql://127.0.0.1:5432/cloudbrain_medical")).isFalse();
        assertThat(databaseType.handlesJDBCUrl(null)).isFalse();
    }

    @Test
    void usesKingbaseDriverForNativeUrls() {
        assertThat(databaseType.getDriverClass("jdbc:kingbase8://127.0.0.1:54321/cloudbrain_medical", null))
                .isEqualTo("com.kingbase8.Driver");
        assertThat(databaseType.getDriverClass("jdbc:p6spy:kingbase8://127.0.0.1:54321/cloudbrain_medical", null))
                .isEqualTo("com.p6spy.engine.spy.P6SpyDriver");
    }

    @Test
    void recognizesKingbaseProductNames() {
        assertThat(databaseType.handlesDatabaseProductNameAndVersion("KingbaseES", "V009R001", null)).isTrue();
        assertThat(databaseType.handlesDatabaseProductNameAndVersion("KingbaseES V8", "V008R006", null)).isTrue();
        assertThat(databaseType.handlesDatabaseProductNameAndVersion("PostgreSQL", "16", null)).isFalse();
        assertThat(databaseType.handlesDatabaseProductNameAndVersion(null, null, null)).isFalse();
    }
}
