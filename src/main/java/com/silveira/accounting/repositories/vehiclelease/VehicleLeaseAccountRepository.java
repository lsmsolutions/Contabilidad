package com.silveira.accounting.repositories.vehiclelease;

import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VehicleLeaseAccountRepository {
    private final DatabaseManager databaseManager;

    public VehicleLeaseAccountRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(VehicleLeaseAccount account) {
        String sql = """
            INSERT INTO vehicle_lease_accounts(
                alias, provider_name, vehicle_year, make, model, trim_name, account_number, vin, maturity_date, notes
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(alias) DO UPDATE SET
                provider_name=excluded.provider_name,
                vehicle_year=excluded.vehicle_year,
                make=excluded.make,
                model=excluded.model,
                trim_name=excluded.trim_name,
                account_number=excluded.account_number,
                vin=excluded.vin,
                maturity_date=excluded.maturity_date,
                notes=COALESCE(vehicle_lease_accounts.notes, excluded.notes)
            """;
        try (Connection connection = databaseManager.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, account.getAlias());
            statement.setString(2, account.getProviderName());
            statement.setInt(3, account.getVehicleYear());
            statement.setString(4, account.getMake());
            statement.setString(5, account.getModel());
            statement.setString(6, account.getTrim());
            statement.setString(7, account.getAccountNumber());
            statement.setString(8, account.getVin());
            setDate(statement, 9, account.getMaturityDate());
            statement.setString(10, account.getNotes());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo guardar el contrato de vehiculo", exception);
        }
    }

    public List<VehicleLeaseAccount> findAll() {
        String sql = "SELECT * FROM vehicle_lease_accounts ORDER BY alias";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<VehicleLeaseAccount> accounts = new ArrayList<>();
            while (result.next()) {
                accounts.add(map(result));
            }
            return accounts;
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudieron consultar los contratos de vehiculo", exception);
        }
    }

    public Optional<VehicleLeaseAccount> findByAlias(String alias) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM vehicle_lease_accounts WHERE alias=?")) {
            statement.setString(1, alias);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("No se pudo consultar el contrato de vehiculo", exception);
        }
    }

    private VehicleLeaseAccount map(ResultSet result) throws SQLException {
        VehicleLeaseAccount account = new VehicleLeaseAccount();
        account.setId(result.getLong("id"));
        account.setAlias(result.getString("alias"));
        account.setProviderName(result.getString("provider_name"));
        account.setVehicleYear(result.getInt("vehicle_year"));
        account.setMake(result.getString("make"));
        account.setModel(result.getString("model"));
        account.setTrim(result.getString("trim_name"));
        account.setAccountNumber(result.getString("account_number"));
        account.setVin(result.getString("vin"));
        account.setMaturityDate(date(result.getString("maturity_date")));
        account.setNotes(result.getString("notes"));
        return account;
    }

    private void setDate(PreparedStatement statement, int index, LocalDate value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value.toString());
        }
    }

    private LocalDate date(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}
