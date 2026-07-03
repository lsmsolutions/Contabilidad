package com.silveira.accounting.composition;

import com.silveira.accounting.application.card.CardApplicationService;
import com.silveira.accounting.application.mortgage.MortgageApplicationService;
import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.repositories.NylMonthlyResultRepository;
import com.silveira.accounting.repositories.NylRecordRepository;
import com.silveira.accounting.repositories.ReviewMarkRepository;
import com.silveira.accounting.repositories.card.CreditCardAccountRepository;
import com.silveira.accounting.repositories.card.CreditCardStatementFieldReviewRepository;
import com.silveira.accounting.repositories.card.CreditCardStatementRepository;
import com.silveira.accounting.repositories.card.CreditCardTransactionRepository;
import com.silveira.accounting.repositories.card.FinancialAlertRepository;
import com.silveira.accounting.repositories.mortgage.HouseExpenseRepository;
import com.silveira.accounting.repositories.mortgage.MortgageAlertRepository;
import com.silveira.accounting.repositories.mortgage.MortgageStatementFieldReviewRepository;
import com.silveira.accounting.repositories.mortgage.MortgageStatementRepository;
import com.silveira.accounting.repositories.mortgage.MortgageTransactionRepository;
import com.silveira.accounting.services.ExcelExportService;
import com.silveira.accounting.services.MortgageAnalysisService;
import com.silveira.accounting.services.MortgageImportService;
import com.silveira.accounting.services.OcrService;
import com.silveira.accounting.ui.AppDependencies;
import com.silveira.accounting.ui.AppView;
import com.silveira.accounting.ui.AppViewActions;
import com.silveira.accounting.ui.bank.BankModule;
import com.silveira.accounting.ui.bank.BankWorkflow;
import com.silveira.accounting.ui.card.CardWorkflow;
import com.silveira.accounting.ui.dashboard.DashboardModule;
import com.silveira.accounting.ui.dashboard.DashboardWorkflow;
import com.silveira.accounting.ui.internalmovement.InternalMovementModule;
import com.silveira.accounting.ui.internalmovement.InternalMovementWorkflow;
import com.silveira.accounting.ui.investment.InvestmentModule;
import com.silveira.accounting.ui.investment.InvestmentWorkflow;
import com.silveira.accounting.ui.mortgage.HouseExpenseWorkflow;
import com.silveira.accounting.ui.mortgage.MortgageDetailWorkflow;
import com.silveira.accounting.ui.mortgage.MortgageWorkflow;
import com.silveira.accounting.ui.nyl.AgentLedgerWorkflow;
import com.silveira.accounting.ui.nyl.NylModule;
import com.silveira.accounting.ui.nyl.NylWorkflow;
import com.silveira.accounting.ui.vehiclelease.VehicleLeaseModule;
import com.silveira.accounting.ui.vehiclelease.VehicleLeaseWorkflow;

public class ApplicationComposition {
    private final DatabaseManager databaseManager;

    public ApplicationComposition(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public AppView appView() {
        return new AppView(dependencies());
    }

    private AppDependencies dependencies() {
        AppViewActions actions = new AppViewActions();
        ExcelExportService excelExportService = new ExcelExportService();
        OcrService ocrService = new OcrService();

        BankModule bankModule = new BankModule(databaseManager, ocrService);
        BankWorkflow bankWorkflow = new BankWorkflow(
            bankModule,
            excelExportService,
            new BankWorkflow.Config(
                (title, nodes) -> actions.setPage(actions.page(title, nodes)),
                actions::setDarkHubPage,
                actions::setPage,
                actions::backButton,
                actions::promptText,
                actions::alert,
                actions::confirm,
                actions::choosePdf,
                actions::chooseExcel,
                actions::showProcessing,
                actions::rootCauseMessage,
                actions::showReview,
                actions::rebuildSidebar,
                actions::selectedBankAccountAlias,
                actions::setSelectedBankAccountAlias,
                actions::selectedPeriodChanged,
                actions::selectedYearValue,
                actions::selectedMonthValue,
                (source, accountAlias, year, month) -> (javafx.scene.control.Label) actions.reviewMarkLabel(source, accountAlias, year, month)
            )
        );

        InvestmentModule investmentModule = new InvestmentModule(databaseManager);
        InvestmentWorkflow investmentWorkflow = new InvestmentWorkflow(
            investmentModule.controller(),
            new InvestmentWorkflow.Config(
                actions::setPage,
                actions::rebuildSidebar,
                actions::owner,
                actions::alert,
                actions::rootCauseMessage,
                actions::confirm,
                actions::reviewMarkLabel
            )
        );

        VehicleLeaseModule vehicleLeaseModule = new VehicleLeaseModule(databaseManager);
        VehicleLeaseWorkflow vehicleLeaseWorkflow = new VehicleLeaseWorkflow(
            vehicleLeaseModule.controller(),
            new VehicleLeaseWorkflow.Config(
                actions::setPage,
                actions::rebuildSidebar,
                actions::owner,
                actions::alert,
                actions::rootCauseMessage,
                actions::confirm
            )
        );

        CardApplicationService cards = new CardApplicationService(
            new CreditCardAccountRepository(databaseManager),
            new CreditCardStatementRepository(databaseManager),
            new CreditCardTransactionRepository(databaseManager),
            new CreditCardStatementFieldReviewRepository(databaseManager),
            new FinancialAlertRepository(databaseManager)
        );
        CardWorkflow cardWorkflow = new CardWorkflow(
            cards.accounts(),
            cards.statements(),
            cards.transactions(),
            cards.imports(),
            cards.reviews(),
            excelExportService,
            new CardWorkflow.Config(
                actions::setPage,
                actions::page,
                actions::setDarkHubPage,
                actions::backButton,
                actions::rebuildSidebar,
                actions::selectedYearValue,
                actions::selectedMonthValue,
                actions::selectedYearValue,
                actions::selectedMonthValue,
                actions::selectedPeriodChanged,
                actions::helperNote,
                actions::reviewMarkLabel,
                actions::choosePdf,
                actions::chooseExcel,
                actions::showProcessing,
                actions::importingChanged,
                actions::rootCauseMessage,
                actions::parseDate,
                actions::safeFileName,
                actions::stringCellFactory,
                actions::moneyCellFactory,
                actions::confirm,
                actions::alert
            )
        );

        InternalMovementModule internalMovementModule = new InternalMovementModule(databaseManager);
        InternalMovementWorkflow internalMovementWorkflow = new InternalMovementWorkflow(
            internalMovementModule.controller(),
            new InternalMovementWorkflow.Config(
                actions::selectedYearValue,
                actions::selectedMonthValue,
                actions::setSelectedYearValue,
                actions::setSelectedMonthValue,
                actions::setPage,
                actions::alert
            )
        );

        MortgageApplicationService mortgageApplication = new MortgageApplicationService(
            new MortgageStatementRepository(databaseManager),
            new MortgageTransactionRepository(databaseManager),
            new MortgageAlertRepository(databaseManager),
            new MortgageStatementFieldReviewRepository(databaseManager),
            new HouseExpenseRepository(databaseManager)
        );

        MortgageDetailWorkflow mortgageDetailWorkflow = new MortgageDetailWorkflow(
            mortgageApplication,
            new MortgageImportService(),
            new MortgageAnalysisService(),
            excelExportService,
            new MortgageDetailWorkflow.Config(
                actions::selectedYearValue,
                actions::selectedMonthValue,
                actions::setSelectedYearValue,
                actions::setSelectedMonthValue,
                actions::rebuildSidebar,
                actions::showMortgages,
                actions::setPage,
                actions::backButton,
                actions::choosePdf,
                actions::chooseExcel,
                actions::alert,
                actions::rootCauseMessage,
                actions::confirm,
                actions::addReviewMark,
                actions::monthName
            )
        );

        HouseExpenseWorkflow houseExpenseWorkflow = new HouseExpenseWorkflow(
            mortgageApplication.houseExpenses(),
            new HouseExpenseWorkflow.Config(
                actions::owner,
                () -> bankModule.accounts().list(),
                () -> cards.accounts().findAll(),
                actions::setPage,
                actions::showMortgages,
                actions::backButton,
                actions::alert,
                actions::rootCauseMessage
            )
        );
        MortgageWorkflow mortgageWorkflow = new MortgageWorkflow(
            mortgageApplication,
            mortgageDetailWorkflow,
            houseExpenseWorkflow,
            new MortgageWorkflow.Config(
                actions::promptText,
                actions::rebuildSidebar,
                actions::setDarkHubPage,
                actions::confirm
            )
        );

        NylMonthlyResultRepository nylMonthlyResultRepository = new NylMonthlyResultRepository(databaseManager);
        NylRecordRepository nylRepository = new NylRecordRepository(databaseManager);
        NylModule nylModule = new NylModule(nylRepository, nylMonthlyResultRepository, ocrService);

        AgentLedgerWorkflow agentLedgerWorkflow = new AgentLedgerWorkflow(
            nylModule.controller(),
            new AgentLedgerWorkflow.Config(
                actions::selectedYearValue,
                actions::selectedMonthValue,
                actions::setSelectedYearValue,
                actions::setSelectedMonthValue,
                actions::setPage,
                actions::showNylHub,
                actions::backButton,
                actions::alert,
                actions::confirm
            )
        );

        NylWorkflow nylWorkflow = new NylWorkflow(
            nylModule.controller(),
            excelExportService,
            new NylWorkflow.Config(
                actions::selectedYearValue,
                actions::selectedMonthValue,
                actions::setSelectedYearValue,
                actions::setSelectedMonthValue,
                actions::setPage,
                agentLedgerWorkflow::showAgentLedger,
                actions::backButton,
                actions::choosePdf,
                actions::chooseExcel,
                actions::alert,
                actions::confirm,
                actions::showReview,
                actions::monthlyActionCard,
                actions::addReviewMark,
                actions::addMonthlyCardLine,
                actions::addMonthlyCardDivider,
                actions::monthlyExportButton,
                actions::miniTotal,
                actions::horizontalStatementScroll
            )
        );

        ReviewMarkRepository reviewMarkRepository = new ReviewMarkRepository(databaseManager);

        DashboardModule dashboardModule = new DashboardModule(
            cards.accounts(),
            cards.statements(),
            bankModule.application(),
            nylRepository,
            mortgageApplication
        );
        DashboardWorkflow dashboardWorkflow = new DashboardWorkflow(
            dashboardModule.controller(),
            new DashboardWorkflow.Config(
                actions::setPage,
                actions::monthName,
                mortgageDetailWorkflow::debtChart
            )
        );

        return new AppDependencies(
            actions,
            bankModule,
            bankWorkflow,
            investmentWorkflow,
            vehicleLeaseWorkflow,
            cards.accounts(),
            cardWorkflow,
            internalMovementWorkflow,
            mortgageApplication,
            houseExpenseWorkflow,
            mortgageWorkflow,
            nylWorkflow,
            agentLedgerWorkflow,
            reviewMarkRepository,
            dashboardWorkflow
        );
    }
}
