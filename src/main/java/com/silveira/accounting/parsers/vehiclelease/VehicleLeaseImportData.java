package com.silveira.accounting.parsers.vehiclelease;

import com.silveira.accounting.models.vehiclelease.VehicleLeaseAccount;
import com.silveira.accounting.models.vehiclelease.VehicleLeaseStatement;

public record VehicleLeaseImportData(VehicleLeaseAccount account, VehicleLeaseStatement statement) {
}
