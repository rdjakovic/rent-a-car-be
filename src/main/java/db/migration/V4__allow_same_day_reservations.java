package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Allows same-day reservations: replaces V1's unnamed {@code CHECK (end_date > start_date)} on
 * reservations with {@code end_date >= start_date}.
 * <p>
 * Written in Java because the unnamed constraint gets a database-generated name that differs
 * between PostgreSQL ("reservations_check") and H2 ("CONSTRAINT_..."); it is looked up through
 * the standard INFORMATION_SCHEMA views, which both databases provide.
 */
public class V4__allow_same_day_reservations extends BaseJavaMigration {

    private static final String OLD_CHECK = "end_date>start_date";

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String constraintName = findOldCheckConstraint(connection);

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE reservations DROP CONSTRAINT \"" + constraintName + "\"");
            statement.execute("ALTER TABLE reservations ADD CONSTRAINT chk_reservations_end_not_before_start "
                    + "CHECK (end_date >= start_date)");
        }
    }

    private static String findOldCheckConstraint(Connection connection) throws SQLException {
        String sql = """
                SELECT cc.constraint_name, cc.check_clause
                FROM information_schema.check_constraints cc
                JOIN information_schema.table_constraints tc
                  ON tc.constraint_schema = cc.constraint_schema
                 AND tc.constraint_name = cc.constraint_name
                WHERE LOWER(tc.table_name) = 'reservations'
                  AND tc.constraint_type = 'CHECK'
                  AND tc.table_schema = CURRENT_SCHEMA
                """;
        List<String> matches = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                if (OLD_CHECK.equals(normalize(rs.getString("check_clause")))) {
                    matches.add(rs.getString("constraint_name"));
                }
            }
        }
        if (matches.size() != 1) {
            throw new IllegalStateException(
                    "Expected exactly one '" + OLD_CHECK + "' check constraint on reservations, found " + matches);
        }
        return matches.getFirst();
    }

    /** "((end_date > start_date))" or "\"END_DATE\" > \"START_DATE\"" -> "end_date>start_date" */
    private static String normalize(String checkClause) {
        return checkClause.replaceAll("[\\s()\"]", "").toLowerCase();
    }
}
