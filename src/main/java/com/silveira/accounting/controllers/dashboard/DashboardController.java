package com.silveira.accounting.controllers.dashboard;

import com.silveira.accounting.application.dashboard.DashboardApplicationService;
import com.silveira.accounting.application.dashboard.DashboardSnapshot;

public class DashboardController {
    private final DashboardApplicationService application;

    public DashboardController(DashboardApplicationService application) {
        this.application = application;
    }

    public DashboardSnapshot snapshot() {
        return application.snapshot();
    }
}
