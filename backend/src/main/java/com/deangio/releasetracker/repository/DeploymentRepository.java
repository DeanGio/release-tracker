package com.deangio.releasetracker.repository;

import java.sql.Timestamp;
import java.sql.Types;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import com.deangio.releasetracker.domain.Deployment;
import com.deangio.releasetracker.domain.DeploymentStatus;
import com.deangio.releasetracker.domain.TargetEnvironment;

public class DeploymentRepository {

    private final DataSource dataSource;

    public DeploymentRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private static final String INSERT_SQL = """
            insert into deployment (id, service_name, version, environment, status, start_time, end_time)
            values (?, ?, ?, ?, ?, ?, ?)
            """;

    public void save(Deployment deployment) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(INSERT_SQL)) {

            pstmt.setObject(1, deployment.id());
            pstmt.setString(2, deployment.serviceName());
            pstmt.setString(3, deployment.version());
            pstmt.setString(4, deployment.environment().name());
            pstmt.setString(5, deployment.status().name());
            pstmt.setTimestamp(6, Timestamp.from(deployment.startTime()));

            if (deployment.endTime() != null) {
                pstmt.setTimestamp(7, Timestamp.from(deployment.endTime()));
            } else {
                pstmt.setNull(7, Types.TIMESTAMP_WITH_TIMEZONE);
            }

            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save deployment", e);
        }
    }

    private static final String UPDATE_SQL = """
            UPDATE deployment
            SET status = ?, end_time = ?
            WHERE id = ? AND status = 'IN_PROGRESS'
            """;

    public void update(Deployment updated) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(UPDATE_SQL)) {

            pstmt.setString(1, updated.status().name());
            if (updated.endTime() != null) {
                pstmt.setTimestamp(2, Timestamp.from(updated.endTime()));
            } else {
                pstmt.setNull(2, Types.TIMESTAMP_WITH_TIMEZONE);
            }
            pstmt.setObject(3, updated.id());

            if (pstmt.executeUpdate() == 0) {
                throw new IllegalStateException("Deployment " + updated.id() + " invalid transition");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update deployment", e);
        }
    }

    private static final String FIND_BY_ID_SQL = """
            select id, service_name, version, environment, status, start_time, end_time
            from deployment
            where id = ?
            """;

    public Optional<Deployment> findById(UUID id) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(FIND_BY_ID_SQL)) {

            pstmt.setObject(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(rsToDeployment(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to look up deployment " + id, e);
        }
    }

    private static final String FIND_ALL_SQL = """
            select id, service_name, version, environment, status, start_time, end_time
            from deployment
            order by start_time desc, id desc
            limit ? offset ?
            """;

    public List<Deployment> findAll(int limit, int offset) {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(FIND_ALL_SQL)) {

            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                var resultList = new ArrayList<Deployment>();
                while (rs.next()) {
                    resultList.add(rsToDeployment(rs));
                }
                return resultList;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find deployments", e);
        }
    }

    private Deployment rsToDeployment(ResultSet rs) throws SQLException {
        Timestamp end = rs.getTimestamp("end_time");
        return new Deployment(
                rs.getObject("id", UUID.class),
                rs.getString("service_name"),
                rs.getString("version"),
                TargetEnvironment.valueOf(rs.getString("environment")),
                DeploymentStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("start_time").toInstant(),
                end == null ? null : end.toInstant());
    }

    private static final String COUNT_SQL = "select count(*) from deployment";

    public long count() {
        try (Connection conn = dataSource.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(COUNT_SQL);
                ResultSet rs = pstmt.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count deployments", e);
        }
    }

}
