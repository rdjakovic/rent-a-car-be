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
 * Pick-up and return of rentals: adds the ACTIVE reservation status (car handed over, not yet back)
 * and records odometer readings and times at pick-up and return.
 * <p>
 * Written in Java for the same reason as V4: V1's status check on reservations is unnamed, so its
 * name differs between PostgreSQL and H2 and is looked up through INFORMATION_SCHEMA.
 */
public class V7__add_reservation_pickup_and_return extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        String statusCheck = findStatusCheckConstraint(connection);

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE reservations DROP CONSTRAINT \"" + statusCheck + "\"");
            statement.execute("ALTER TABLE reservations ADD CONSTRAINT chk_reservations_status "
                    + "CHECK (status IN ('PENDING', 'CONFIRMED', 'ACTIVE', 'CANCELLED', 'COMPLETED'))");

            statement.execute("ALTER TABLE reservations ADD COLUMN mileage_out INTEGER");
            statement.execute("ALTER TABLE reservations ADD COLUMN mileage_in INTEGER");
            statement.execute("ALTER TABLE reservations ADD COLUMN picked_up_at TIMESTAMP WITH TIME ZONE");
            statement.execute("ALTER TABLE reservations ADD COLUMN returned_at TIMESTAMP WITH TIME ZONE");
            statement.execute("ALTER TABLE reservations ADD CONSTRAINT chk_reservations_mileage_out_not_negative "
                    + "CHECK (mileage_out >= 0)");
            statement.execute("ALTER TABLE reservations ADD CONSTRAINT chk_reservations_mileage_in_not_below_out "
                    + "CHECK (mileage_in >= mileage_out)");
        }
    }

    /** The check listing the reservation statuses, e.g. "status IN ('PENDING', ...)" or "status = ANY (ARRAY['PENDING', ...])". */
    private static String findStatusCheckConstraint(Connection connection) throws SQLException {
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
                String clause = rs.getString("check_clause").toLowerCase();
                if (clause.contains("status") && clause.contains("'pending'") && clause.contains("'completed'")) {
                    matches.add(rs.getString("constraint_name"));
                }
            }
        }
        if (matches.size() != 1) {
            throw new IllegalStateException("Expected exactly one status check constraint on reservations, found " + matches);
        }
        return matches.getFirst();
    }
}
