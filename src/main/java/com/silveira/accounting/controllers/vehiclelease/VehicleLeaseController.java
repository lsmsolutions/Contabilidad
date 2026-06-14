package com.silveira.accounting.controllers.vehiclelease;

import com.silveira.accounting.application.vehiclelease.VehicleLeaseApplicationService;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseStatement;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class VehicleLeaseController {
    private final VehicleLeaseApplicationService application;

    public VehicleLeaseController(VehicleLeaseApplicationService application) {
        this.application = application;
    }

    public VehicleLeaseStatement importPdf(Path pdf) {
        return application.importPdf(pdf);
    }

    public List<VehicleLeaseAccount> accounts() {
        return application.accounts();
    }

    public Optional<VehicleLeaseAccount> account(String alias) {
        return application.account(alias);
    }

    public List<VehicleLeaseStatement> statements(String alias) {
        return application.statements(alias);
    }

    public void update(VehicleLeaseStatement statement) {
        application.update(statement);
    }

    public void delete(VehicleLeaseStatement statement) {
        application.delete(statement);
    }

    public boolean isFieldReviewed(VehicleLeaseStatement statement, String field) {
        return application.isFieldReviewed(statement, field);
    }

    public void setFieldReviewed(VehicleLeaseStatement statement, String field, boolean reviewed) {
        application.setFieldReviewed(statement, field, reviewed);
    }

    public void setAllReviewed(VehicleLeaseStatement statement, boolean reviewed) {
        application.setAllReviewed(statement, reviewed);
    }
}
