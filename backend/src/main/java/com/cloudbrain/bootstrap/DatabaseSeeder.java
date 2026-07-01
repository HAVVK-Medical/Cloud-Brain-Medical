package com.cloudbrain.bootstrap;

import java.util.Locale;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private static final long PATIENT_ID = 5001L;
    private static final long DOCTOR_ID = 3001L;
    private static final long ADMIN_ID = 4001L;
    private static final long DEPARTMENT_ID = 1L;

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedDepartment();
        seedPatient();
        seedDoctor();
        seedAdmin();
    }

    private void seedDepartment() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from department where code = ?",
                Integer.class,
                "internal-medicine"
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                "insert into department (id, code, name, type, status) values (?, ?, ?, ?, ?)",
                DEPARTMENT_ID,
                "internal-medicine",
                "内科",
                "门诊",
                "ACTIVE"
        );
        restartIdentity("department", DEPARTMENT_ID + 1);
    }

    private void seedPatient() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from patient where username = ?",
                Integer.class,
                "patient01"
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                """
                        insert into patient
                        (id, username, password_hash, phone, name, gender, age, allergy_history, medical_history, id_card_number, remark, status)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                PATIENT_ID,
                "patient01",
                passwordEncoder.encode("patient123"),
                "13800000001",
                "患者一号",
                "男",
                30,
                "无",
                "示例病历",
                "110101199001010011",
                "示例患者",
                "ACTIVE"
        );
        restartIdentity("patient", PATIENT_ID + 1);
    }

    private void seedDoctor() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from doctor where username = ?",
                Integer.class,
                "doctor01"
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                """
                        insert into doctor
                        (id, username, password_hash, name, department_id, title, specialty, introduction, status)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                DOCTOR_ID,
                "doctor01",
                passwordEncoder.encode("doctor123"),
                "医生一号",
                DEPARTMENT_ID,
                "主任医师",
                "心内科",
                "示例医生",
                "ACTIVE"
        );
        restartIdentity("doctor", DOCTOR_ID + 1);
    }

    private void seedAdmin() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(1) from admin where username = ?",
                Integer.class,
                "admin"
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
                """
                        insert into admin
                        (id, username, password_hash, name, role, status)
                        values (?, ?, ?, ?, ?, ?)
                        """,
                ADMIN_ID,
                "admin",
                passwordEncoder.encode("admin123"),
                "管理员",
                "ADMIN",
                "ACTIVE"
        );
        restartIdentity("admin", ADMIN_ID + 1);
    }

    private void restartIdentity(String tableName, long nextValue) {
        String productName = jdbcTemplate.execute((java.sql.Connection connection) ->
                connection.getMetaData().getDatabaseProductName());
        if (productName == null) {
            return;
        }
        String normalized = productName.toLowerCase(Locale.ROOT);
        if (normalized.contains("mysql")) {
            jdbcTemplate.execute("alter table " + tableName + " auto_increment = " + nextValue);
            return;
        }
        if (normalized.contains("h2") || normalized.contains("kingbase") || normalized.contains("postgresql")) {
            jdbcTemplate.execute("alter table " + tableName + " alter column id restart with " + nextValue);
        }
    }
}
