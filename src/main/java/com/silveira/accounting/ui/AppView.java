package com.silveira.accounting.ui;

import com.silveira.accounting.application.bank.dto.BankPeriodSummary;
import com.silveira.accounting.application.card.CardApplicationService;
import com.silveira.accounting.application.card.service.CardAccountApplicationService;
import com.silveira.accounting.application.card.service.CardFieldReviewApplicationService;
import com.silveira.accounting.application.card.service.CardImportApplicationService;
import com.silveira.accounting.application.card.service.CardStatementApplicationService;
import com.silveira.accounting.application.card.service.CardTransactionApplicationService;
import com.silveira.accounting.database.DatabaseManager;
import com.silveira.accounting.models.bank.BankTransaction;
import com.silveira.accounting.models.bank.BankAccount;
import com.silveira.accounting.models.CreditCardAccount;
import com.silveira.accounting.models.CreditCardAnalysis;
import com.silveira.accounting.models.CreditCardStatement;
import com.silveira.accounting.models.CreditCardTransaction;
import com.silveira.accounting.models.DashboardSummary;
import com.silveira.accounting.models.HouseExpense;
import com.silveira.accounting.models.InternalMovementRecord;
import com.silveira.accounting.models.MonthlySourceTotals;
import com.silveira.accounting.models.MortgageAnalysis;
import com.silveira.accounting.models.MortgageStatement;
import com.silveira.accounting.models.MortgageTransaction;
import com.silveira.accounting.models.NylRecord;
import com.silveira.accounting.models.ReconciliationItem;
import com.silveira.accounting.models.SourceTotals;
import com.silveira.accounting.parsers.CreditCardStatementParser;
import com.silveira.accounting.parsers.NylPdfParser;
import com.silveira.accounting.repositories.HouseExpenseRepository;
import com.silveira.accounting.repositories.InternalMovementRepository;
import com.silveira.accounting.repositories.MortgageAlertRepository;
import com.silveira.accounting.repositories.MortgageStatementRepository;
import com.silveira.accounting.repositories.MortgageTransactionRepository;
import com.silveira.accounting.repositories.NylMonthlyResultRepository;
import com.silveira.accounting.repositories.NylRecordRepository;
import com.silveira.accounting.repositories.ReconciliationRepository;
import com.silveira.accounting.repositories.ReviewMarkRepository;
import com.silveira.accounting.repositories.card.CreditCardAccountRepository;
import com.silveira.accounting.repositories.card.CreditCardStatementRepository;
import com.silveira.accounting.repositories.card.CreditCardStatementFieldReviewRepository;
import com.silveira.accounting.repositories.card.CreditCardTransactionRepository;
import com.silveira.accounting.repositories.card.FinancialAlertRepository;
import com.silveira.accounting.repositories.mortgage.MortgageStatementFieldReviewRepository;
import com.silveira.accounting.services.DashboardService;
import com.silveira.accounting.services.ExcelExportService;
import com.silveira.accounting.services.ImportValidationService;
import com.silveira.accounting.services.MortgageAnalysisService;
import com.silveira.accounting.services.MortgageImportService;
import com.silveira.accounting.services.OcrService;
import com.silveira.accounting.services.ReconciliationService;
import com.silveira.accounting.ui.bank.BankDashboardPanelView;
import com.silveira.accounting.ui.bank.BankModule;
import com.silveira.accounting.ui.bank.BankReconciliationView;
import com.silveira.accounting.ui.bank.BankShellWorkflow;
import com.silveira.accounting.ui.card.BestBuyStatementSummaryView;
import com.silveira.accounting.ui.card.CardAccountFormView;
import com.silveira.accounting.ui.card.CardAccountDetailControls;
import com.silveira.accounting.ui.card.CardAccountSelectorDialogView;
import com.silveira.accounting.ui.card.CardAccountsHubView;
import com.silveira.accounting.ui.card.CitiStatementSummaryView;
import com.silveira.accounting.ui.card.CreditCardStatementSummaryView;
import com.silveira.accounting.ui.card.DiscoverStatementSummaryView;
import com.silveira.accounting.ui.card.CardPeriodDetailView;
import com.silveira.accounting.ui.card.CardTransactionDialogView;
import com.silveira.accounting.ui.common.PeriodActionCardView;
import com.silveira.accounting.ui.mortgage.MortgageStatementSummaryView;
import com.silveira.accounting.utils.Fingerprint;
import com.silveira.accounting.utils.Money;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.StackedBarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AppView {
    private static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] NYL_SECTION_OPTIONS = {
        "Creditos",
        "Deducciones",
        "Tax Withholding",
        "Group Plan Contributions",
        "Office Expenses",
        "Technology Expense",
        "Deferred Compensation",
        "Other Deductions"
    };
    private static final List<String> NYL_DEDUCTION_SECTION_ORDER = List.of(
        "Tax Withholding",
        "Group Plan Contributions",
        "Office Expenses",
        "Technology Expense",
        "Deferred Compensation",
        "Other Deductions"
    );
    private static final List<AgentLedgerTemplate> AGENT_LEDGER_TEMPLATES = List.of(
        AgentLedgerTemplate.header("Credits", "agent-ledger-credit-header"),
        AgentLedgerTemplate.section("Commissions"),
        AgentLedgerTemplate.input("FYC", "Creditos", "credito"),
        AgentLedgerTemplate.input("EAGLE Fees", "Creditos", "credito"),
        AgentLedgerTemplate.input("Renewals", "Creditos", "credito"),
        AgentLedgerTemplate.input("Trails/Other", "Creditos", "credito"),
        AgentLedgerTemplate.total("Total Commissions", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Expense Allowance"),
        AgentLedgerTemplate.input("Life & Annuity Expense Allowance", "Creditos", "credito"),
        AgentLedgerTemplate.total("Total Expense Allowance", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Premium Drawing"),
        AgentLedgerTemplate.input("Premium Drawing Nylic", "Creditos", "credito"),
        AgentLedgerTemplate.total("Total Premium Drawing", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Other Income"),
        AgentLedgerTemplate.input("Life & Annuity Persistency Bonus", "Creditos", "credito"),
        AgentLedgerTemplate.input("NYLAZ Override", "Creditos", "credito"),
        AgentLedgerTemplate.input("ARD", "Creditos", "credito"),
        AgentLedgerTemplate.input("Miscellaneous", "Creditos", "credito"),
        AgentLedgerTemplate.total("Total Other Income", "agent-ledger-total-row"),
        AgentLedgerTemplate.grandTotal("TOTAL CREDITS", "agent-ledger-credit-total"),
        AgentLedgerTemplate.header("Deductions", "agent-ledger-deduction-header"),
        AgentLedgerTemplate.section("Tax Withholding"),
        AgentLedgerTemplate.input("FICA - Medicare", "Tax Withholding", "deduccion"),
        AgentLedgerTemplate.input("FICA - OASDI", "Tax Withholding", "deduccion"),
        AgentLedgerTemplate.total("Total Tax Withholding", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Group Plan Contributions"),
        AgentLedgerTemplate.input("Medical", "Group Plan Contributions", "deduccion"),
        AgentLedgerTemplate.input("Dental", "Group Plan Contributions", "deduccion"),
        AgentLedgerTemplate.input("LTD", "Group Plan Contributions", "deduccion"),
        AgentLedgerTemplate.total("Total Group Plan Contributions", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Office Expenses"),
        AgentLedgerTemplate.input("Rent", "Office Expenses", "deduccion"),
        AgentLedgerTemplate.input("Telephone - Equipment", "Office Expenses", "deduccion"),
        AgentLedgerTemplate.total("Total Office Expenses", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Technology Expenses"),
        AgentLedgerTemplate.input("FT Support", "Technology Expenses", "deduccion"),
        AgentLedgerTemplate.total("Total Technology Expenses", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Deferred Compensation"),
        AgentLedgerTemplate.input("401K Contributions", "Deferred Compensation", "deduccion"),
        AgentLedgerTemplate.total("Total Deferred Compensation", "agent-ledger-total-row"),
        AgentLedgerTemplate.section("Other Deductions"),
        AgentLedgerTemplate.input("NYL-A-PLAN", "Other Deductions", "deduccion"),
        AgentLedgerTemplate.input("NYLIFE ADM LTC Deduction", "Other Deductions", "deduccion"),
        AgentLedgerTemplate.input("Navigator Membership", "Other Deductions", "deduccion"),
        AgentLedgerTemplate.input("Lead Generation Campaigns", "Other Deductions", "deduccion"),
        AgentLedgerTemplate.input("401K Loan Repayment", "Other Deductions", "deduccion"),
        AgentLedgerTemplate.input("Miscellaneous", "Other Deductions", "deduccion"),
        AgentLedgerTemplate.total("Total Other Deductions", "agent-ledger-total-row"),
        AgentLedgerTemplate.grandTotal("TOTAL DEDUCTIONS", "agent-ledger-deduction-total"),
        AgentLedgerTemplate.header("Withdrawals", "agent-ledger-withdrawal-header"),
        AgentLedgerTemplate.input("Jan", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Feb", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Mar", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Apr", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("May", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Jun", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Jul", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Aug", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Sep", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Oct", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Nov", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.input("Dec", "Withdrawals", "withdrawal"),
        AgentLedgerTemplate.grandTotal("TOTAL WITHDRAWALS", "agent-ledger-withdrawal-total"),
        AgentLedgerTemplate.grandTotal("NET BALANCE (Credits - Deductions - Withdrawals)", "agent-ledger-net-total")
    );

    private final BankModule bankModule;
    private final CardAccountApplicationService creditCardAccountRepository;
    private final CardStatementApplicationService creditCardStatementRepository;
    private final CardFieldReviewApplicationService creditCardStatementFieldReviewRepository;
    private final CardTransactionApplicationService creditCardTransactionRepository;
    private final CardImportApplicationService creditCardImports;
    private final HouseExpenseRepository houseExpenseRepository;
    private final InternalMovementRepository internalMovementRepository;
    private final MortgageStatementRepository mortgageStatementRepository;
    private final MortgageStatementFieldReviewRepository mortgageStatementFieldReviewRepository;
    private final MortgageTransactionRepository mortgageTransactionRepository;
    private final MortgageAlertRepository mortgageAlertRepository;
    private final NylMonthlyResultRepository nylMonthlyResultRepository;
    private final NylRecordRepository nylRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final ReviewMarkRepository reviewMarkRepository;
    private final DashboardService dashboardService;
    private final ReconciliationService reconciliationService;
    private final ExcelExportService excelExportService = new ExcelExportService();
    private final MortgageImportService mortgageImportService = new MortgageImportService();
    private final MortgageAnalysisService mortgageAnalysisService = new MortgageAnalysisService();
    private Runnable showCardMovementsTabAction = () -> {};
    private final OcrService ocrService = new OcrService();
    private final ImportValidationService importValidationService = new ImportValidationService();
    private final NylPdfParser nylParser = new NylPdfParser();

    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();
    private Node sidebar;
    private Integer selectedYearValue;
    private Integer selectedMonthValue;
    private String selectedBankAccountAlias;
    private boolean cardImportInProgress;
    private BooleanSupplier navigationGuard;
    private boolean bankMenuExpanded;
    private boolean cardMenuExpanded;
    private boolean mortgageMenuExpanded;
    private boolean nylMenuExpanded;

    public AppView(DatabaseManager databaseManager) {
        bankModule = new BankModule(databaseManager, ocrService);
        CardApplicationService cards = new CardApplicationService(
            new CreditCardAccountRepository(databaseManager),
            new CreditCardStatementRepository(databaseManager),
            new CreditCardTransactionRepository(databaseManager),
            new CreditCardStatementFieldReviewRepository(databaseManager),
            new FinancialAlertRepository(databaseManager)
        );
        creditCardAccountRepository = cards.accounts();
        creditCardStatementRepository = cards.statements();
        creditCardStatementFieldReviewRepository = cards.fieldReviews();
        creditCardTransactionRepository = cards.transactions();
        creditCardImports = cards.imports();
        houseExpenseRepository = new HouseExpenseRepository(databaseManager);
        internalMovementRepository = new InternalMovementRepository(databaseManager);
        mortgageStatementRepository = new MortgageStatementRepository(databaseManager);
        mortgageStatementFieldReviewRepository = new MortgageStatementFieldReviewRepository(databaseManager);
        mortgageTransactionRepository = new MortgageTransactionRepository(databaseManager);
        mortgageAlertRepository = new MortgageAlertRepository(databaseManager);
        nylMonthlyResultRepository = new NylMonthlyResultRepository(databaseManager);
        nylRepository = new NylRecordRepository(databaseManager);
        reconciliationRepository = new ReconciliationRepository(databaseManager);
        reviewMarkRepository = new ReviewMarkRepository(databaseManager);
        dashboardService = new DashboardService(bankModule.transactions(), nylRepository);
        reconciliationService = new ReconciliationService(bankModule.transactions(), nylRepository);
        build();
    }

    public Parent root() {
        return root;
    }

    private void build() {
        rebuildSidebar();
        root.setCenter(content);
        showDashboard();
    }

    private void rebuildSidebar() {
        sidebar = sidebar();
        root.setLeft(sidebar);
    }

    private Node sidebar() {
        Node logo = createSidebarLogo();

        VBox bankSubmenu = new VBox(4);
        bankSubmenu.getStyleClass().add("submenu");
        for (BankAccount account : bankModule.accounts().list()) {
            bankSubmenu.getChildren().add(subnav(account.getAlias(), () -> showBankAccount(account.getAlias())));
        }
        VBox cardSubmenu = new VBox(4);
        cardSubmenu.getStyleClass().add("submenu");
        for (CreditCardAccount account : creditCardAccountRepository.findAll()) {
            cardSubmenu.getChildren().add(subnav(account.getAlias(), () -> showCardAccount(account.getAlias())));
        }
        VBox mortgageSubmenu = new VBox(4);
        mortgageSubmenu.getStyleClass().add("submenu");
        for (String alias : mortgageStatementRepository.findLoanAliases()) {
            mortgageSubmenu.getChildren().add(subnav(alias, () -> showMortgageDetail(alias)));
        }
        mortgageSubmenu.getChildren().add(subnav("Casa - Gastos", this::showHouseExpenses));
        VBox nylSubmenu = new VBox(4);
        nylSubmenu.getStyleClass().add("submenu");
        nylSubmenu.getChildren().add(subnav("Resumen NYL", this::showNyl));
        nylSubmenu.getChildren().add(subnav("Análisis NYL", this::showAnalysis));
        nylSubmenu.getChildren().add(subnav("Agent Ledger", this::showAgentLedger));

        VBox menu = new VBox(8);
        menu.getChildren().addAll(
            nav("Dashboard", this::showDashboard),
            collapsibleNav("Banco", this::showBank, bankSubmenu, () -> bankMenuExpanded, value -> bankMenuExpanded = value),
            collapsibleNav("Tarjetas", this::showCards, cardSubmenu, () -> cardMenuExpanded, value -> cardMenuExpanded = value),
            collapsibleNav("Hipotecas", this::showMortgages, mortgageSubmenu, () -> mortgageMenuExpanded, value -> mortgageMenuExpanded = value),
            nav("Movimientos internos", this::showInternalMovements),
            collapsibleNav("New York Life", this::showNylHub, nylSubmenu, () -> nylMenuExpanded, value -> nylMenuExpanded = value)
        );
        VBox sidebarContent = new VBox(24, logo, new Separator(), menu);
        sidebarContent.getStyleClass().add("sidebar");
        sidebarContent.setPrefWidth(220);
        ScrollPane scroll = new ScrollPane(sidebarContent);
        scroll.getStyleClass().add("sidebar-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPrefWidth(260);
        return scroll;
    }

    private Node createSidebarLogo() {
        try (InputStream logoStream = getClass().getResourceAsStream("/img/logo.png")) {
            if (logoStream == null) {
                Label fallback = new Label("Silveira Accounting");
                fallback.getStyleClass().add("subtitle");
                fallback.setWrapText(true);
                fallback.setMaxWidth(170);
                return fallback;
            }
            ImageView logo = new ImageView(new Image(logoStream));
            logo.setFitWidth(170);
            logo.setPreserveRatio(true);
            return logo;
        } catch (Exception exception) {
            Label fallback = new Label("Silveira Accounting");
            fallback.getStyleClass().add("subtitle");
            fallback.setWrapText(true);
            fallback.setMaxWidth(170);
            return fallback;
        }
    }

    private Button subnav(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("subnav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> runUnlessImporting(action));
        return button;
    }

    private VBox collapsibleNav(String text, Runnable action, VBox submenu, BooleanSupplier expandedGetter, java.util.function.Consumer<Boolean> expandedSetter) {
        submenu.setVisible(expandedGetter.getAsBoolean());
        submenu.setManaged(expandedGetter.getAsBoolean());
        Label title = new Label(text);
        title.getStyleClass().add("nav-button-title");
        Label indicator = new Label(expandedGetter.getAsBoolean() ? "\u2304" : "\u203a");
        indicator.getStyleClass().add("nav-submenu-indicator");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox content = new HBox(8, title, spacer, indicator);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setMaxWidth(Double.MAX_VALUE);
        Button button = nav(text, () -> {
            boolean expanded = !expandedGetter.getAsBoolean();
            expandedSetter.accept(expanded);
            submenu.setVisible(expanded);
            submenu.setManaged(expanded);
            indicator.setText(expanded ? "\u2304" : "\u203a");
            action.run();
        });
        button.setText(null);
        button.setGraphic(content);
        return new VBox(4, button, submenu);
    }

    private Button nav(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> {
            if (warnIfImporting()) {
                return;
            }
            if (!confirmNavigation()) {
                return;
            }
            try {
                action.run();
            } catch (RuntimeException exception) {
                alert(Alert.AlertType.ERROR, "No se pudo abrir " + text, rootCauseMessage(exception));
            }
        });
        return button;
    }

    private void runUnlessImporting(Runnable action) {
        if (warnIfImporting()) {
            return;
        }
        if (!confirmNavigation()) {
            return;
        }
        action.run();
    }

    public boolean confirmNavigation() {
        if (navigationGuard == null) {
            return true;
        }
        boolean canNavigate = navigationGuard.getAsBoolean();
        if (canNavigate) {
            navigationGuard = null;
        }
        return canNavigate;
    }

    private boolean warnIfImporting() {
        if (!cardImportInProgress) {
            return false;
        }
        alert(Alert.AlertType.WARNING, "Importación en curso", "No salgas de esta pantalla hasta que termine la importación del PDF. Puede demorar unos minutos.");
        return true;
    }

    private void sameSize(Button... buttons) {
        for (Button button : buttons) {
            button.setMinWidth(165);
            button.setPrefWidth(165);
        }
    }

    private HBox filters(Runnable refresh) {
        ComboBox<Integer> yearFilter = new ComboBox<>();
        ComboBox<Integer> monthFilter = new ComboBox<>();
        yearFilter.setItems(FXCollections.observableArrayList(null, 2022, 2023, 2024, 2025, 2026));
        monthFilter.setItems(FXCollections.observableArrayList(null, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        yearFilter.setValue(selectedYearValue);
        monthFilter.setValue(selectedMonthValue);
        yearFilter.setPromptText("Año");
        monthFilter.setPromptText("Mes");
        Button apply = new Button("Filtrar");
        apply.getStyleClass().add("primary");
        apply.setOnAction(event -> {
            selectedYearValue = yearFilter.getValue();
            selectedMonthValue = monthFilter.getValue();
            refresh.run();
        });
        return new HBox(10, new Label("Año"), yearFilter, new Label("Mes"), monthFilter, apply);
    }

    private Integer selectedYear() {
        return selectedYearValue;
    }

    private Integer selectedMonth() {
        return selectedMonthValue;
    }

    private void showDashboard() {
        showExecutiveDashboard();
        if (System.currentTimeMillis() >= 0) {
            return;
        }
        VBox sections = new VBox(16);
        Runnable refresh = () -> {
            DashboardSummary summary = dashboardService.summary(selectedYear(), selectedMonth());
            sections.getChildren().setAll(
                dashboardSection("Banco", "Datos importados desde extractos bancarios", "bank-section",
                    card("Total ingresos bancarios", summary.bankIncome(), "bank-card"),
                    card("Recibido en banco desde NYL", summary.nylReceivedBank(), "bank-card"),
                    card("Gastos bancarios", summary.bankExpenses(), "bank-card")),
                dashboardSection("New York Life", "Datos importados o cargados manualmente desde resúmenes NYL", "nyl-section",
                    card("Total comisiones/créditos NYL", summary.nylCredits(), "nyl-card"),
                    card("Total deducciones NYL", summary.nylDeductions(), "nyl-card"),
                    card("Neto NYL", summary.nylNet(), "nyl-card")),
                dashboardSection("Conciliación", "Comparación entre lo esperado por NYL y lo recibido en banco", "reconciliation-section",
                    card("Diferencias pendientes", summary.pendingDifference(), "reconciliation-card"))
            );
        };
        refresh.run();
        VBox page = page("Dashboard", filters(refresh), sections);
        setPage(page);
    }

    private VBox dashboardSection(String title, String subtitle, String styleClass, VBox... cards) {
        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        Label detail = new Label(subtitle);
        detail.getStyleClass().add("section-subtitle");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        for (int i = 0; i < cards.length; i++) {
            grid.add(cards[i], i % 3, i / 3);
        }

        VBox section = new VBox(12, new VBox(2, heading, detail), grid);
        section.getStyleClass().addAll("dashboard-section", styleClass);
        return section;
    }

    private void showExecutiveDashboard() {
        HBox dashboardCharts = new HBox(14, dashboardNylMonthlyChartPanel(), dashboardMortgageDebtPanel());
        dashboardCharts.getStyleClass().add("dashboard-chart-row");
        VBox compactDashboard = new VBox(18, dashboardPaymentsPanel(), dashboardBankPanel(), dashboardCharts);
        compactDashboard.getStyleClass().add("dashboard-dark-panel");
        VBox compactDashboardPage = page("Dashboard", compactDashboard);
        compactDashboardPage.getStyleClass().add("dashboard-page");
        setPage(compactDashboardPage);
        if (System.currentTimeMillis() >= 0) {
            return;
        }
        Label title = new Label("Próximos pagos de tarjetas");
        title.getStyleClass().add("dashboard-hero-title");
        Label subtitle = new Label("Último statement registrado por tarjeta");
        subtitle.getStyleClass().add("dashboard-hero-subtitle");
        VBox hero = new VBox(4, title, subtitle);
        hero.getStyleClass().add("dashboard-hero");

        HBox cards = new HBox(14);
        cards.getStyleClass().add("dashboard-card-grid");
        for (CreditCardAccount account : creditCardAccountRepository.findAll()) {
            Optional<CreditCardStatement> latest = creditCardStatementRepository.findByAccount(account.getAlias(), null, null)
                .stream()
                .findFirst();
            cards.getChildren().add(dashboardCreditCard(account, latest));
        }
        if (cards.getChildren().isEmpty()) {
            Label empty = new Label("No hay tarjetas registradas todavía.");
            empty.getStyleClass().add("dashboard-empty");
            cards.getChildren().add(empty);
        }

        VBox dashboard = new VBox(18, hero, cards);
        dashboard.getStyleClass().add("dashboard-dark-panel");
        setPage(page("Dashboard", dashboard));
        if (System.currentTimeMillis() >= 0) {
            return;
        }
        VBox sections = new VBox(16);
        Runnable refresh = () -> {
            int year = selectedYear() == null ? LocalDate.now().getYear() : selectedYear();
            int month = selectedMonth() == null ? LocalDate.now().getMonthValue() : selectedMonth();
            DashboardSummary summary = dashboardService.summary(year, month);
            SourceTotals bankTotals = bankModule.application().transactions().totals(year, month);
            SourceTotals nylTotals = nylRepository.totals(year, month);
            List<CreditCardStatement> cardStatements = allCardStatements(year, month);
            List<CreditCardTransaction> cardMovements = allCardTransactions(year, month);
            List<MortgageStatement> mortgageStatements = allMortgageStatements(year, month);
            List<MortgageTransaction> mortgageMovements = allMortgageTransactions(year, month);
            long cardPending = cardStatements.stream().filter(CreditCardStatement::isPendingReview).count()
                + cardMovements.stream().filter(CreditCardTransaction::isPendingReview).count();
            long mortgagePending = mortgageStatements.stream().filter(MortgageStatement::isPendingReview).count()
                + mortgageMovements.stream().filter(MortgageTransaction::isPendingReview).count();
            long reconciliationPending = reconciliationService.preview(year, month).stream()
                .filter(item -> !"Conciliado".equals(item.status()))
                .count();
            long totalPending = bankTotals.pendingCount() + nylTotals.pendingCount() + cardPending + mortgagePending + reconciliationPending;
            String status = totalPending == 0 ? "Listo para cierre" : "Pendiente de revision";
            sections.getChildren().setAll(
                dashboardAlerts(year, month, bankTotals.pendingCount(), cardPending, mortgagePending, nylTotals.pendingCount(), reconciliationPending),
                dashboardSection("Estado del mes", monthName(month) + " " + year, "reconciliation-section",
                    textCard("Estado", status, "reconciliation-card"),
                    textCard("Pendientes totales", String.valueOf(totalPending), "pending-total"),
                    textCard("Cruces pendientes", String.valueOf(reconciliationPending), "neutral-total")),
                dashboardSection("Banco", "Datos importados desde extractos bancarios", "bank-section",
                    card("Depósitos", summary.bankIncome(), "bank-card"),
                    card("Recibido en banco desde NYL", summary.nylReceivedBank(), "bank-card"),
                    card("Salidas", summary.bankExpenses(), "bank-card"),
                    textCard("Pendientes Banco", String.valueOf(bankTotals.pendingCount()), "pending-total")),
                dashboardSection("Tarjetas", "Statements, pagos, intereses y deuda al cierre", "card-section",
                    card("Deuda al cierre", cardStatements.stream().filter(s -> !s.isPendingReview()).mapToDouble(CreditCardStatement::getNewBalance).sum(), "nyl-card"),
                    card("Intereses", cardMovements.stream().filter(m -> !m.isPendingReview() && "interes".equalsIgnoreCase(m.getType())).mapToDouble(CreditCardTransaction::getAmount).sum(), "reconciliation-card"),
                    textCard("Pendientes Tarjetas", String.valueOf(cardPending), "pending-total")),
                dashboardSection("Hipotecas", "Pagos mensuales, principal, intereses y escrow", "reconciliation-section",
                    card("Deuda a pagar", mortgageStatements.stream().filter(s -> !s.isPendingReview()).mapToDouble(s -> s.getTotalDue() > 0 ? s.getTotalDue() : s.getPaymentAmountDue()).sum(), "reconciliation-card"),
                    card("Intereses", mortgageStatements.stream().filter(s -> !s.isPendingReview()).mapToDouble(MortgageStatement::getInterestDue).sum(), "reconciliation-card"),
                    textCard("Pendientes Hipotecas", String.valueOf(mortgagePending), "pending-total")),
                dashboardSection("New York Life", "Datos importados o cargados manualmente desde resumenes NYL", "nyl-section",
                    card("Comisiones", summary.nylCredits(), "nyl-card"),
                    card("Deducciones", summary.nylDeductions(), "nyl-card"),
                    card("Neto NYL", summary.nylNet(), "nyl-card"),
                    textCard("Pendientes NYL", String.valueOf(nylTotals.pendingCount()), "pending-total"))
            );
        };
        refresh.run();
        Button download = new Button("Descargar cierre mensual");
        download.setOnAction(event -> exportMonthlyReconciliation());
        HBox header = filters(refresh);
        Region spacer = new Region();
        spacer.setMinWidth(22);
        header.getChildren().addAll(spacer, download);
        setPage(page("Dashboard", header, sections));
    }

    private VBox dashboardPaymentsPanel() {
        Label title = new Label("Proximos pagos");
        title.getStyleClass().add("dashboard-panel-title");
        GridPane table = new GridPane();
        table.getStyleClass().add("dashboard-payments-table");
        ColumnConstraints cardColumn = dashboardPaymentColumn(560);
        cardColumn.setHgrow(Priority.ALWAYS);
        ColumnConstraints numberColumn = dashboardPaymentColumn(130);
        ColumnConstraints dateColumn = dashboardPaymentColumn(150);
        ColumnConstraints amountColumn = dashboardPaymentColumn(140);
        amountColumn.setHalignment(HPos.LEFT);
        ColumnConstraints pdfColumn = dashboardPaymentColumn(170);
        table.getColumnConstraints().setAll(cardColumn, numberColumn, dateColumn, amountColumn, pdfColumn);
        dashboardPaymentCell(table, 0, 0, "Tarjeta", "dashboard-table-header");
        dashboardPaymentCell(table, 1, 0, "Numero", "dashboard-table-header");
        dashboardPaymentCell(table, 2, 0, "Fecha limite", "dashboard-table-header");
        dashboardPaymentCell(table, 3, 0, "Cantidad limite", "dashboard-table-header");
        dashboardPaymentCell(table, 4, 0, "PDF esperado", "dashboard-table-header");
        int row = 1;
        for (CreditCardAccount account : creditCardAccountRepository.findAll()) {
            Optional<CreditCardStatement> latest = creditCardStatementRepository.findByAccount(account.getAlias(), null, null)
                .stream()
                .findFirst();
            String digits = latest.map(CreditCardStatement::getAccountLastDigits)
                .filter(value -> value != null && !value.isBlank())
                .orElse(account.getAccountLastDigits());
            String name = dashboardCardDisplayName(account);
            dashboardPaymentCell(table, 0, row, name, "dashboard-table-text");
            dashboardPaymentCell(table, 1, row, digits == null || digits.isBlank() ? "-" : "**** " + digits, "dashboard-table-muted");
            dashboardPaymentCell(table, 2, row, latest.map(CreditCardStatement::getPaymentDueDate).map(this::shortDate).orElse("Sin fecha"), "dashboard-table-date");
            dashboardPaymentCell(table, 3, row, latest.map(statement -> Money.format(statement.getMinimumPaymentDue())).orElse("-"), "dashboard-table-amount");
            dashboardPaymentCell(table, 4, row, dashboardExpectedCardPdf(digits), "dashboard-table-muted");
            row++;
        }
        if (row == 1) {
            dashboardPaymentCell(table, 0, row, "No hay tarjetas registradas.", "dashboard-table-muted");
        }
        VBox panel = new VBox(10, title, table);
        panel.getStyleClass().add("dashboard-panel");
        return panel;
    }

    private String dashboardExpectedCardPdf(String digits) {
        return switch (digits == null ? "" : digits.trim()) {
            case "0782" -> "17-18 de cada mes";
            case "2512", "2518" -> "03-04 de cada mes";
            case "9497" -> "15-18 de cada mes";
            case "5211" -> "23-24 de cada mes";
            case "5632" -> "06-07 de cada mes";
            default -> "-";
        };
    }

    private String dashboardCardDisplayName(CreditCardAccount account) {
        return account == null ? "-" : account.getAlias();
    }

    private void dashboardPaymentCell(GridPane table, int column, int row, String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setMaxWidth(Double.MAX_VALUE);
        GridPane.setFillWidth(label, true);
        if (column == 3 || column == 4) {
            GridPane.setHalignment(label, HPos.LEFT);
        }
        table.add(label, column, row);
    }

    private ColumnConstraints dashboardPaymentColumn(double width) {
        ColumnConstraints column = new ColumnConstraints();
        column.setMinWidth(width);
        column.setPrefWidth(width);
        return column;
    }

    private VBox dashboardBankPanel() {
        return new BankDashboardPanelView().build(
            bankModule.accounts().list(),
            this::bankPeriodSummaries
        );
    }

    private VBox dashboardNylMonthlyChartPanel() {
        Label title = new Label("Comisiones vs deducciones");
        title.getStyleClass().add("dashboard-panel-title");
        BarChart<String, Number> chart = new BarChart<>(new CategoryAxis(), new NumberAxis());
        chart.getStyleClass().add("nyl-monthly-chart");
        chart.setLegendVisible(true);
        chart.setAnimated(false);
        chart.setTitle("");
        XYChart.Series<String, Number> commissions = new XYChart.Series<>();
        commissions.setName("Comisiones");
        XYChart.Series<String, Number> deductions = new XYChart.Series<>();
        deductions.setName("Deducciones");
        List<NylRecord> records = nylRepository.find(null, null, null, null).stream()
            .filter(record -> !record.isPendingReview())
            .toList();
        Map<Integer, Double> monthlyCommissions = records.stream()
            .filter(record -> record.getRecordType().equals("comision") || record.getRecordType().equals("credito"))
            .collect(Collectors.groupingBy(NylRecord::getMonth, Collectors.summingDouble(NylRecord::getAmount)));
        Map<Integer, Double> monthlyDeductions = records.stream()
            .filter(record -> record.getRecordType().equals("deduccion"))
            .collect(Collectors.groupingBy(NylRecord::getMonth, Collectors.summingDouble(record -> Math.abs(record.getAmount()))));
        for (int month = 1; month <= 12; month++) {
            String label = monthName(month);
            commissions.getData().add(new XYChart.Data<>(label, monthlyCommissions.getOrDefault(month, 0.0)));
            deductions.getData().add(new XYChart.Data<>(label, monthlyDeductions.getOrDefault(month, 0.0)));
        }
        chart.getData().setAll(commissions, deductions);
        VBox panel = new VBox(8, title, chart);
        panel.getStyleClass().add("dashboard-panel");
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private VBox dashboardMortgageDebtPanel() {
        Label title = new Label("Deuda hipoteca Casa");
        title.getStyleClass().add("dashboard-panel-title");
        List<MortgageStatement> statements = mortgageStatementRepository.findByLoan("CasaH", null, null).stream()
            .filter(statement -> !statement.isPendingReview())
            .sorted(Comparator.comparing(MortgageStatement::getStatementDate, Comparator.nullsLast(LocalDate::compareTo)))
            .toList();
        LineChart<String, Number> chart = mortgageDebtChart(statements);
        chart.setTitle("");
        chart.setAnimated(false);
        VBox panel = new VBox(8, title, chart);
        panel.getStyleClass().add("dashboard-panel");
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private VBox dashboardCreditCard(CreditCardAccount account, Optional<CreditCardStatement> latest) {
        String digits = latest.map(CreditCardStatement::getAccountLastDigits)
            .filter(value -> value != null && !value.isBlank())
            .orElse(account.getAccountLastDigits());
        Label cardName = new Label(account.getCardName() == null || account.getCardName().isBlank() ? account.getAlias() : account.getCardName());
        cardName.getStyleClass().add("dashboard-card-name");
        Label number = new Label(digits == null || digits.isBlank() ? "Tarjeta sin numero" : "**** " + digits);
        number.getStyleClass().add("dashboard-card-number");
        Label dueLabel = new Label("Fecha límite de pago");
        dueLabel.getStyleClass().add("dashboard-card-label");
        Label dueValue = new Label(latest.map(CreditCardStatement::getPaymentDueDate).map(this::shortDate).orElse("Sin fecha"));
        dueValue.getStyleClass().add("dashboard-card-date");
        Label amountLabel = new Label("Cantidad límite de pago");
        amountLabel.getStyleClass().add("dashboard-card-label");
        Label amountValue = new Label(latest.map(statement -> Money.format(statement.getMinimumPaymentDue())).orElse("-"));
        amountValue.getStyleClass().add("dashboard-card-amount");
        VBox card = new VBox(10, cardName, number, new Separator(), dueLabel, dueValue, amountLabel, amountValue);
        card.getStyleClass().add("dashboard-credit-card");
        return card;
    }

    private VBox card(String title, double value, String sourceClass) {
        Label label = new Label(title);
        label.getStyleClass().add("card-title");
        Label amount = new Label(Money.format(value));
        amount.getStyleClass().add("card-value");
        VBox card = new VBox(8, label, amount);
        card.getStyleClass().addAll("metric-card", sourceClass);
        card.setMinWidth(250);
        return card;
    }

    private VBox textCard(String title, String value, String sourceClass) {
        Label label = new Label(title);
        label.getStyleClass().add("card-title");
        Label amount = new Label(value);
        amount.getStyleClass().add("card-value");
        VBox card = new VBox(8, label, amount);
        card.getStyleClass().addAll("metric-card", sourceClass);
        card.setMinWidth(250);
        return card;
    }

    private VBox dashboardAlerts(int year, int month, int bankPending, long cardPending, long mortgagePending, int nylPending, long reconciliationPending) {
        VBox alerts = new VBox(8);
        alerts.getStyleClass().addAll("dashboard-section", "alert-section");
        Label title = new Label("Alertas");
        title.getStyleClass().add("section-title");
        alerts.getChildren().add(title);
        List<String> lines = new java.util.ArrayList<>();
        LocalDate today = LocalDate.now();
        for (CreditCardStatement statement : allCardStatements(year, month)) {
            if (statement.isPendingReview() || statement.getPaymentDueDate() == null) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(today, statement.getPaymentDueDate());
            if (days <= 3) {
                lines.add(paymentAlert("Tarjeta " + statement.getAccountAlias(), days)
                    + " Pago mínimo: " + Money.format(statement.getMinimumPaymentDue())
                    + ". Deuda cierre: " + Money.format(statement.getNewBalance()) + ".");
            }
        }
        for (MortgageStatement statement : allMortgageStatements(year, month)) {
            if (statement.isPendingReview() || statement.getPaymentDueDate() == null) {
                continue;
            }
            long days = ChronoUnit.DAYS.between(today, statement.getPaymentDueDate());
            if (days <= 3) {
                double due = statement.getTotalDue() > 0 ? statement.getTotalDue() : statement.getPaymentAmountDue();
                lines.add(paymentAlert("Hipoteca " + statement.getLoanAlias(), days)
                    + " Deuda a pagar este mes: " + Money.format(due) + ".");
            }
        }
        if (bankPending > 0) lines.add("Banco tiene " + bankPending + " movimientos pendientes de revision.");
        if (cardPending > 0) lines.add("Tarjetas tiene " + cardPending + " registros/movimientos pendientes de revision.");
        if (mortgagePending > 0) lines.add("Hipotecas tiene " + mortgagePending + " registros/movimientos pendientes de revision.");
        if (nylPending > 0) lines.add("New York Life tiene " + nylPending + " registros pendientes de revision.");
        if (reconciliationPending > 0) lines.add("Hay " + reconciliationPending + " cruces NYL/Banco pendientes o con diferencia.");
        boolean cardInterest = allCardTransactions(year, month).stream()
            .anyMatch(movement -> !movement.isPendingReview() && "interes".equalsIgnoreCase(movement.getType()) && Math.abs(movement.getAmount()) > 0);
        if (cardInterest) lines.add("Hay tarjetas generando intereses este mes.");
        if (lines.isEmpty()) lines.add("Sin alertas criticas para el mes seleccionado.");
        for (String line : lines) {
            Label label = new Label(line);
            label.getStyleClass().add("section-subtitle");
            alerts.getChildren().add(label);
        }
        return alerts;
    }

    private String paymentAlert(String source, long days) {
        if (days < 0) {
            return source + " tiene el pago vencido.";
        }
        if (days == 0) {
            return source + " vence hoy.";
        }
        if (days == 1) {
            return source + " vence mañana.";
        }
        return source + " vence en " + days + " días.";
    }

    private void showBank() {
        bankShellWorkflow().showHub();
    }

    private void showBankAccount(String accountAlias) {
        bankShellWorkflow().showAccount(accountAlias);
    }

    private BankShellWorkflow bankShellWorkflow() {
        return new BankShellWorkflow(
            bankModule.application(),
            bankModule.accounts(),
            bankModule.accountDetails(),
            bankModule.accountWorkflow(),
            bankModule.imports(),
            bankModule.periods(),
            excelExportService,
            new BankShellWorkflow.Config(
                (title, nodes) -> setPage(page(title, nodes)),
                this::setDarkHubPage,
                this::setPage,
                this::backButton,
                this::promptText,
                this::alert,
                this::confirm,
                this::choosePdf,
                this::chooseExcel,
                this::showProcessing,
                this::rootCauseMessage,
                (title, table, confirm, warningNode) -> showReview(title, table, confirm, warningNode),
                this::rebuildSidebar,
                () -> selectedBankAccountAlias,
                alias -> selectedBankAccountAlias = alias,
                (year, month) -> {
                    selectedYearValue = year;
                    selectedMonthValue = month;
                },
                this::selectedYear,
                this::selectedMonth,
                this::reviewMarkLabel
            )
        );
    }
    private void showCards() {
        CardAccountsHubView.Hub hub = new CardAccountsHubView().build(
            creditCardAccountRepository.findAll(),
            this::showAddCreditCard,
            this::editCreditCardFromHub,
            this::deleteCreditCardFromHub,
            this::showCardAccount
        );
        if (hub.empty()) {
            setPage(page("Tarjetas", hub.content(), hub.actions()));
        } else {
            setDarkHubPage("Tarjetas", hub.content(), hub.actions());
        }
    }

    private void showAddCreditCard() {
        showCreditCardAccountDialog(null);
    }

    private void editCreditCardFromHub() {
        chooseCreditCardAccount("Editar tarjeta", "No hay tarjetas para editar.")
            .ifPresent(this::showCreditCardAccountDialog);
    }

    private void deleteCreditCardFromHub() {
        chooseCreditCardAccount("Eliminar tarjeta", "No hay tarjetas para eliminar.").ifPresent(account -> {
            if (!confirm("Eliminar tarjeta", "Se eliminaran la tarjeta, sus estados, movimientos y alertas.", "Eliminar")) {
                return;
            }
            creditCardAccountRepository.delete(account.getAlias());
            rebuildSidebar();
            showCards();
        });
    }

    private Optional<CreditCardAccount> chooseCreditCardAccount(String title, String emptyMessage) {
        List<CreditCardAccount> accounts = creditCardAccountRepository.findAll();
        if (accounts.isEmpty()) {
            alert(Alert.AlertType.INFORMATION, "Sin tarjetas", emptyMessage);
            return Optional.empty();
        }
        return new CardAccountSelectorDialogView().show(title, accounts);
    }

    private void showCreditCardAccountDialog(CreditCardAccount current) {
        boolean editing = current != null;
        new CardAccountFormView().show(current).ifPresent(account -> {
            if (editing) {
                creditCardAccountRepository.update(current.getAlias(), account);
            } else {
                creditCardAccountRepository.save(account);
            }
            rebuildSidebar();
            showCardAccount(account.getAlias());
        });
    }

    private void showCardAccount(String alias) {
        TableView<CreditCardStatement> statements = creditCardStatementTable();
        HBox totals = new HBox(12);
        totals.getStyleClass().add("totals-panel");
        VBox statementCards = new VBox(10);
        Runnable refreshTotals = () -> totals.getChildren().setAll(cardAccumulatedTotalsNodes(alias, selectedYear(), selectedMonth()));
        Runnable refresh = () -> {
            statements.setItems(FXCollections.observableArrayList(creditCardStatementRepository.findByAccount(alias, selectedYear(), selectedMonth())));
            refreshTotals.run();
        };
        refresh.run();
        VBox monthlyCards = monthlyCardCards(alias, statements, totals, statementCards);

        CardAccountDetailControls controls = new CardAccountDetailControls(selectedYearValue, selectedMonthValue);
        controls.filter().setOnAction(event -> {
            selectedYearValue = controls.year().getValue();
            selectedMonthValue = controls.month().getValue();
            refresh.run();
            monthlyCards.getChildren().setAll(monthlyCardCards(alias, statements, totals, statementCards).getChildren());
        });
        controls.importPdf().setOnAction(event -> importCreditCardPdf(alias, refresh));
        controls.analysis().setOnAction(event -> showCreditCardAnalysis(alias));
        controls.addStatement().setOnAction(event -> addManualCreditCardStatement(alias, () -> showCardAccount(alias)));
        Label note = helperNote("Haz clic en una card para abrir el detalle completo de ese periodo.");
        setPage(page("Tarjeta - " + alias, backButton("Volver a Tarjetas", this::showCards), controls.actions(), totals, note, monthlyCards));
    }

    private void showCardPeriodDetail(String alias, int year, int month) {
        selectedYearValue = year;
        selectedMonthValue = month;
        TableView<CreditCardStatement> statements = creditCardStatementTable();
        TableView<CreditCardTransaction> movements = creditCardTransactionTable();
        HBox totals = new HBox(12);
        totals.getStyleClass().add("totals-panel");
        VBox statementCards = new VBox(10);
        Runnable refreshTotals = () -> totals.getChildren().setAll(cardPeriodActivityTotalsNodes(statements.getItems()));
        Runnable refresh = () -> {
            statements.setItems(FXCollections.observableArrayList(creditCardStatementRepository.findByAccount(alias, year, month)));
            movements.setItems(FXCollections.observableArrayList(creditCardTransactionRepository.findByAccount(alias, year, month)));
            refreshTotals.run();
            refreshCreditCardStatementCards(statements, statementCards, refreshTotals);
        };
        refresh.run();

        CardPeriodDetailView.View periodView = new CardPeriodDetailView().build(
            statementCards,
            movements,
            () -> saveVisibleCardStatements(statements, refresh),
            () -> addManualCreditCardMovement(statements, movements),
            () -> {
                movements.requestFocus();
                saveVisibleCardMovements(statements, movements, refresh);
            }
        );
        showCardMovementsTabAction = periodView.showMovements();
        String periodTitle = statements.getItems().isEmpty()
            ? String.format("%02d/%d", month, year)
            : cardPeriodTitle(statements.getItems(), new MonthlySourceTotals(year, month, 0, 0, 0, 0, 0));
        setPage(page(
            "Tarjeta - " + alias + " - " + periodTitle,
            backButton("Volver a la tarjeta", () -> showCardAccount(alias)),
            totals,
            periodView.tabs()
        ));
    }

    private void importCreditCardPdf(String alias, Runnable refresh) {
        File file = choosePdf();
        if (file == null) {
            return;
        }
        cardImportInProgress = true;
        showProcessing("Importando tarjeta", "Leyendo el PDF de la tarjeta. Si es escaneado, se usara OCR y puede tardar unos minutos.");
        Task<CreditCardStatementParser.ParsedCreditCardStatement> task = new Task<>() {
            @Override
            protected CreditCardStatementParser.ParsedCreditCardStatement call() {
                return creditCardImports.importPdf(file.toPath());
            }
        };
        task.setOnSucceeded(event -> {
            cardImportInProgress = false;
            CreditCardStatementParser.ParsedCreditCardStatement parsed = task.getValue();
            creditCardImports.saveImported(alias, parsed);
            showCardAccount(alias);
        });
        task.setOnFailed(event -> {
            cardImportInProgress = false;
            alert(Alert.AlertType.ERROR, "No se pudo importar tarjeta", rootCauseMessage(task.getException()));
            showCardAccount(alias);
        });
        Thread thread = new Thread(task, "silveira-card-import");
        thread.setDaemon(true);
        thread.start();
    }

    private void addManualCreditCardStatement(String alias, Runnable refresh) {
        CreditCardStatement statement = creditCardStatementRepository.createManual(alias, LocalDate.now());
        showCreditCardPeriodDialog(List.of(statement), refresh);
    }

    private void addManualCreditCardMovement(TableView<CreditCardStatement> statements, TableView<CreditCardTransaction> table) {
        long statementId = statements.getItems().isEmpty() ? 0 : statements.getItems().get(0).getId();
        CreditCardTransaction movement = creditCardTransactionRepository.createManual(statementId, LocalDate.now());
        table.getItems().add(movement);
        table.getSelectionModel().select(movement);
    }

    private void saveVisibleCardStatements(TableView<CreditCardStatement> statements, Runnable refresh) {
        creditCardStatementRepository.saveVisible(statements.getItems());
        refresh.run();
        alert(Alert.AlertType.INFORMATION, "Resumen guardado", "Resumen visible guardado con su estado actual.");
    }

    private void saveVisibleCardMovements(TableView<CreditCardStatement> statements, TableView<CreditCardTransaction> movements, Runnable refresh) {
        long statementId = statements.getItems().isEmpty() ? 0 : statements.getItems().get(0).getId();
        creditCardTransactionRepository.saveVisible(statementId, movements.getItems());
        refresh.run();
        alert(Alert.AlertType.INFORMATION, "Movimientos guardados", "Movimientos visibles guardados con su estado actual.");
    }

    private void showCreditCardTransactionDialog(CreditCardStatement statement, Runnable refresh) {
        if (statement == null || statement.getId() <= 0) {
            alert(Alert.AlertType.WARNING, "Resumen sin guardar", "Guarda primero el resumen de la tarjeta antes de añadir movimientos.");
            return;
        }
        new CardTransactionDialogView().show(statement).ifPresent(form -> {
            if (form.transactionDate() == null || form.postDate() == null || form.description().isBlank()) {
                alert(Alert.AlertType.WARNING, "Movimiento incompleto", "Indica fecha, posteo y descripción.");
                return;
            }
            CreditCardTransaction movement = new CreditCardTransaction(
                0,
                statement.getId(),
                form.transactionDate(),
                form.postDate(),
                form.description().trim(),
                Money.parse(form.amount()),
                form.type().isBlank() ? "gasto" : form.type().trim(),
                form.category().isBlank() ? "manual" : form.category().trim()
            );
            movement.setPendingReview(!form.reviewed());
            movement.setReviewRequired(!form.reviewed());
            movement.setReviewNotes(form.notes());
            movement.setId(creditCardTransactionRepository.save(statement.getId(), movement));
            refresh.run();
        });
    }

    private void showCreditCardAnalysis(String alias) {
        List<CreditCardStatement> reviewed = creditCardStatementRepository.findByAccount(alias, null, null).stream()
            .filter(statement -> !statement.isPendingReview())
            .filter(statement -> statement.getStatementEndDate() != null)
            .sorted(Comparator.comparing(CreditCardStatement::getStatementEndDate))
            .toList();
        HBox totals = new HBox(12,
            miniTotal("Deuda cierre", Money.format(reviewed.stream().mapToDouble(CreditCardStatement::getNewBalance).sum()), "expense-total"),
            miniTotal("Pagos", Money.format(reviewed.stream().mapToDouble(CreditCardStatement::getPayments).sum()), "income-total"),
            miniTotal("Compras", Money.format(reviewed.stream().mapToDouble(CreditCardStatement::getTransactions).sum()), "expense-total"),
            miniTotal("Intereses", Money.format(reviewed.stream().mapToDouble(CreditCardStatement::getInterestCharged).sum()), "urgent-total")
        );
        totals.getStyleClass().add("totals-panel");
        setPage(page("Analisis de tarjeta - " + alias, backButton("Volver al detalle", () -> showCardAccount(alias)), totals));
    }

    private void refreshCreditCardStatementCards(TableView<CreditCardStatement> table, VBox cards, Runnable refreshTotals) {
        cards.getChildren().clear();
        for (CreditCardStatement statement : table.getItems()) {
            cards.getChildren().add(horizontalStatementScroll(editableCreditCardStatementCard(statement, table, cards, refreshTotals)));
        }
    }

    private List<CreditCardTransaction> visibleCardMovements(String alias, Integer year, Integer month) {
        if (month == null) {
            return creditCardTransactionRepository.findByAccount(alias, null, null);
        }
        return creditCardTransactionRepository.findByAccount(alias, year, month);
    }

    private ScrollPane horizontalStatementScroll(Node statementCard) {
        ScrollPane scroll = new ScrollPane(statementCard);
        scroll.setFitToHeight(true);
        scroll.setFitToWidth(false);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPannable(true);
        scroll.setMaxWidth(Double.MAX_VALUE);
        return scroll;
    }

    private VBox editableCreditCardStatementCard(CreditCardStatement statement, TableView<CreditCardStatement> table, VBox cards, Runnable refreshTotals) {
        if (isBestBuyStatement(statement)) {
            return editableBestBuyCreditCardStatementCard(statement, table, cards, refreshTotals);
        }
        if (isDiscoverStatement(statement)) {
            return editableDiscoverCreditCardStatementCard(statement, table, cards, refreshTotals);
        }
        if (isCitiStatement(statement)) {
            return editableCitiCreditCardStatementCard(statement, table, cards, refreshTotals);
        }
        if (!isCapitalOneStatement(statement)) {
            return editableGenericCreditCardStatementCard(statement, table, cards, refreshTotals);
        }
        CreditCardStatementSummaryView summaryView = new CreditCardStatementSummaryView();
        List<String> fieldKeys = summaryView.fieldKeys(statement);
        boolean defaultReviewed = !statement.isPendingReview();
        VBox card = summaryView.build(
            statement,
            fieldName -> creditCardStatementFieldReviewRepository.isReviewed(statement.getId(), fieldName, defaultReviewed),
            (fieldName, reviewed) -> {
                updateCreditCardStatementFieldReview(statement, fieldName, reviewed, fieldKeys);
                table.refresh();
                refreshTotals.run();
                refreshCreditCardStatementCards(table, cards, refreshTotals);
            },
            reviewed -> {
                updateAllCreditCardStatementFieldReviews(statement, fieldKeys, reviewed);
                table.refresh();
                refreshTotals.run();
                refreshCreditCardStatementCards(table, cards, refreshTotals);
            },
            () -> showCreditCardPeriodDialog(List.of(statement), () -> {
                table.refresh();
                refreshTotals.run();
                refreshCreditCardStatementCards(table, cards, refreshTotals);
            })
        );
        card.setOnMouseClicked(event -> table.getSelectionModel().select(statement));
        return card;
    }

    private VBox editableBestBuyCreditCardStatementCard(CreditCardStatement statement, TableView<CreditCardStatement> table, VBox cards, Runnable refreshTotals) {
        BestBuyStatementSummaryView summaryView = new BestBuyStatementSummaryView();
        List<String> fieldKeys = summaryView.fieldKeys(statement);
        boolean defaultReviewed = !statement.isPendingReview();
        List<CreditCardTransaction> transactions = creditCardTransactionsForStatement(statement);
        VBox card = summaryView.build(
            statement,
            transactions,
            fieldName -> creditCardStatementFieldReviewRepository.isReviewed(statement.getId(), fieldName, defaultReviewed),
            (fieldName, reviewed) -> {
                updateCreditCardStatementFieldReview(statement, fieldName, reviewed, fieldKeys);
                table.refresh();
                refreshTotals.run();
                refreshCreditCardStatementCards(table, cards, refreshTotals);
            },
            reviewed -> {
                updateAllCreditCardStatementFieldReviews(statement, fieldKeys, reviewed);
                for (CreditCardTransaction transaction : transactions) {
                    updateCreditCardMovementReview(transaction, reviewed);
                }
                table.refresh();
                refreshTotals.run();
                refreshCreditCardStatementCards(table, cards, refreshTotals);
            },
            (transaction, reviewed) -> {
                updateCreditCardMovementReview(transaction, reviewed);
                refreshTotals.run();
                refreshCreditCardStatementCards(table, cards, refreshTotals);
            },
            () -> showCreditCardPeriodDialog(List.of(statement), () -> {
                table.refresh();
                refreshTotals.run();
                refreshCreditCardStatementCards(table, cards, refreshTotals);
            })
        );
        card.setOnMouseClicked(event -> table.getSelectionModel().select(statement));
        return card;
    }

    private VBox editableDiscoverCreditCardStatementCard(CreditCardStatement statement, TableView<CreditCardStatement> table, VBox cards, Runnable refreshTotals) {
        DiscoverStatementSummaryView summaryView = new DiscoverStatementSummaryView();
        List<String> fieldKeys = summaryView.fieldKeys(statement);
        boolean defaultReviewed = !statement.isPendingReview();
        List<CreditCardTransaction> transactions = creditCardTransactionsForStatement(statement);
        VBox card = summaryView.build(
            statement,
            transactions,
            fieldName -> creditCardStatementFieldReviewRepository.isReviewed(statement.getId(), fieldName, defaultReviewed),
            (fieldName, reviewed) -> {
                updateCreditCardStatementFieldReview(statement, fieldName, reviewed, fieldKeys);
                table.refresh();
                refreshTotals.run();
            },
            reviewed -> {
                updateAllCreditCardStatementFieldReviews(statement, fieldKeys, reviewed);
                for (CreditCardTransaction transaction : transactions) {
                    updateCreditCardMovementReview(transaction, reviewed);
                }
                table.refresh();
                refreshTotals.run();
                refreshCreditCardStatementCards(table, cards, refreshTotals);
            },
            (transaction, reviewed) -> {
                updateCreditCardMovementReview(transaction, reviewed);
                refreshTotals.run();
                table.refresh();
            },
            () -> showCardMovementsTabAction.run(),
            () -> showCreditCardPeriodDialog(List.of(statement), () -> {
                table.refresh();
                refreshTotals.run();
                refreshCreditCardStatementCards(table, cards, refreshTotals);
            })
        );
        card.setOnMouseClicked(event -> table.getSelectionModel().select(statement));
        return card;
    }

    private VBox editableCitiCreditCardStatementCard(CreditCardStatement statement, TableView<CreditCardStatement> table, VBox cards, Runnable refreshTotals) {
        CitiStatementSummaryView summaryView = new CitiStatementSummaryView();
        List<String> fieldKeys = summaryView.fieldKeys(statement);
        boolean defaultReviewed = !statement.isPendingReview();
        VBox card = summaryView.build(
            statement,
            fieldName -> creditCardStatementFieldReviewRepository.isReviewed(statement.getId(), fieldName, defaultReviewed),
            (fieldName, reviewed) -> {
                updateCreditCardStatementFieldReview(statement, fieldName, reviewed, fieldKeys);
                table.refresh();
                refreshTotals.run();
                refreshCreditCardStatementCards(table, cards, refreshTotals);
            },
            reviewed -> {
                updateAllCreditCardStatementFieldReviews(statement, fieldKeys, reviewed);
                table.refresh();
                refreshTotals.run();
                refreshCreditCardStatementCards(table, cards, refreshTotals);
            },
            () -> showCreditCardPeriodDialog(List.of(statement), () -> {
                table.refresh();
                refreshTotals.run();
                refreshCreditCardStatementCards(table, cards, refreshTotals);
            })
        );
        card.setOnMouseClicked(event -> table.getSelectionModel().select(statement));
        return card;
    }

    private VBox editableGenericCreditCardStatementCard(CreditCardStatement statement, TableView<CreditCardStatement> table, VBox cards, Runnable refreshTotals) {
        VBox card = monthlyActionCard(
            cardStatementTitle(statement),
            "Saldo anterior: " + Money.format(statement.getPreviousBalance()),
            "Saldo usado: " + Money.format(statement.getNewBalance()),
            "Pago minimo: " + Money.format(statement.getMinimumPaymentDue()),
            "Pendiente revision: " + (statement.isPendingReview() ? "Si" : "No"),
            () -> table.getSelectionModel().select(statement)
        );
        Button edit = new Button("Editar datos");
        edit.setOnAction(event -> showCreditCardPeriodDialog(List.of(statement), () -> {
            table.refresh();
            refreshTotals.run();
            refreshCreditCardStatementCards(table, cards, refreshTotals);
        }));
        card.getChildren().add(edit);
        card.getStyleClass().add("monthly-card");
        return card;
    }

    private boolean isCapitalOneStatement(CreditCardStatement statement) {
        String bank = text(statement.getBankName()).toLowerCase(java.util.Locale.ROOT);
        String alias = text(statement.getAccountAlias()).toLowerCase(java.util.Locale.ROOT);
        String card = text(statement.getCardName()).toLowerCase(java.util.Locale.ROOT);
        return bank.contains("capital one") || alias.contains("capitalone") || card.contains("capital one");
    }

    private boolean isBestBuyStatement(CreditCardStatement statement) {
        String bank = text(statement.getBankName()).toLowerCase(java.util.Locale.ROOT);
        String alias = text(statement.getAccountAlias()).toLowerCase(java.util.Locale.ROOT);
        String card = text(statement.getCardName()).toLowerCase(java.util.Locale.ROOT);
        return bank.contains("best buy") || alias.contains("bestbuy") || alias.contains("best_buy") || card.contains("best buy");
    }

    private boolean isCitiStatement(CreditCardStatement statement) {
        String bank = text(statement.getBankName()).toLowerCase(java.util.Locale.ROOT);
        String alias = text(statement.getAccountAlias()).toLowerCase(java.util.Locale.ROOT);
        String card = text(statement.getCardName()).toLowerCase(java.util.Locale.ROOT);
        return bank.contains("citi") || alias.contains("citi") || card.contains("citi");
    }

    private boolean isDiscoverStatement(CreditCardStatement statement) {
        String bank = text(statement.getBankName()).toLowerCase(java.util.Locale.ROOT);
        String alias = text(statement.getAccountAlias()).toLowerCase(java.util.Locale.ROOT);
        String card = text(statement.getCardName()).toLowerCase(java.util.Locale.ROOT);
        return bank.contains("discover") || alias.contains("discover") || card.contains("discover");
    }

    private List<CreditCardTransaction> creditCardTransactionsForStatement(CreditCardStatement statement) {
        if (statement.getId() <= 0 || statement.getStatementEndDate() == null) {
            return List.of();
        }
        return creditCardTransactionRepository
            .findByAccount(statement.getAccountAlias(), statement.getStatementEndDate().getYear(), statement.getStatementEndDate().getMonthValue())
            .stream()
            .filter(transaction -> transaction.getStatementId() == statement.getId())
            .toList();
    }

    private void updateCreditCardStatementFieldReview(CreditCardStatement statement, String fieldName, boolean reviewed, List<String> fieldKeys) {
        creditCardStatementFieldReviewRepository.setReviewed(statement.getId(), fieldName, reviewed);
        updateCreditCardStatementReview(statement, allCreditCardStatementFieldsReviewed(statement, fieldKeys));
    }

    private void updateAllCreditCardStatementFieldReviews(CreditCardStatement statement, List<String> fieldKeys, boolean reviewed) {
        creditCardStatementFieldReviewRepository.setReviewed(statement.getId(), fieldKeys, reviewed);
        updateCreditCardStatementReview(statement, reviewed);
    }

    private boolean allCreditCardStatementFieldsReviewed(CreditCardStatement statement, List<String> fieldKeys) {
        boolean defaultReviewed = !statement.isPendingReview();
        return fieldKeys.stream()
            .allMatch(fieldName -> creditCardStatementFieldReviewRepository.isReviewed(statement.getId(), fieldName, defaultReviewed));
    }

    private void updateCreditCardStatementReview(CreditCardStatement statement, boolean reviewed) {
        statement.setPendingReview(!reviewed);
        statement.setReviewRequired(!reviewed);
        if (reviewed && (statement.getReviewNotes() == null || statement.getReviewNotes().isBlank() || statement.getReviewNotes().startsWith("Revisar"))) {
            statement.setReviewNotes("Revisado");
        } else if (!reviewed && "Revisado".equalsIgnoreCase(statement.getReviewNotes())) {
            statement.setReviewNotes("Revisar contra el PDF original");
        }
        if (statement.getId() > 0) {
            creditCardStatementRepository.updateRecord(statement);
        }
    }

    private void updateCreditCardMovementReview(CreditCardTransaction movement, boolean reviewed) {
        movement.setPendingReview(!reviewed);
        movement.setReviewRequired(!reviewed);
        if (reviewed && (movement.getReviewNotes() == null || movement.getReviewNotes().isBlank() || movement.getReviewNotes().startsWith("Revisar"))) {
            movement.setReviewNotes("Revisado");
        }
        if (movement.getId() > 0) {
            creditCardTransactionRepository.update(movement);
        }
    }

    private String cardStatementTitle(CreditCardStatement statement) {
        LocalDate start = statement.getStatementStartDate();
        LocalDate end = statement.getStatementEndDate();
        if (start != null && end != null) {
            return shortDate(start) + " - " + shortDate(end);
        }
        if (end != null) {
            return monthName(end.getMonthValue()) + " " + end.getYear();
        }
        return text(statement.getAccountAlias()).isBlank() ? "Resumen de tarjeta" : statement.getAccountAlias();
    }

    private void showMortgages() {
        List<String> aliases = mortgageStatementRepository.findLoanAliases();
        Button add = new Button("+ Anadir hipoteca");
        add.getStyleClass().add("primary");
        add.setOnAction(event -> showAddMortgageLoan());
        Button houseExpenses = new Button("Casa - Gastos");
        houseExpenses.setOnAction(event -> showHouseExpenses());
        HBox actions = new HBox(10, add, houseExpenses);
        HBox cards = new HBox(14);
        cards.getStyleClass().add("monthly-card-row");
        for (String alias : aliases) {
            VBox card = monthlyActionCard(alias, "Statements: " + mortgageStatementRepository.findByLoan(alias, null, null).size(), "", "", "", () -> showMortgageDetail(alias));
            card.getStyleClass().add("monthly-card");
            cards.getChildren().add(card);
        }
        setDarkHubPage("Hipotecas", cards, actions);
    }

    private void showAddMortgageLoan() {
        Optional<String> alias = promptText("Nueva hipoteca", "Alias de la hipoteca");
        if (alias.isEmpty() || alias.get().isBlank()) {
            return;
        }
        String value = alias.get().trim();
        mortgageStatementRepository.saveLoan(value, "", "", "", "");
        rebuildSidebar();
        showMortgageDetail(value);
    }

    private void showMortgageDetail(String alias) {
        TableView<MortgageStatement> statements = mortgageStatementTable();
        TableView<MortgageTransaction> movements = mortgageTransactionTable();
        HBox totals = new HBox(12);
        totals.getStyleClass().add("totals-panel");
        VBox statementSummaries = new VBox(10);
        statementSummaries.getStyleClass().add("statement-card-list");
        Runnable refreshTotals = () -> totals.getChildren().setAll(mortgageTotalsNodes(statements.getItems(), movements.getItems()));
        Runnable refresh = () -> {
            statements.setItems(FXCollections.observableArrayList(mortgageStatementRepository.findByLoan(alias, selectedYear(), selectedMonth())));
            movements.setItems(FXCollections.observableArrayList(mortgageTransactionRepository.findByLoan(alias, selectedYear(), selectedMonth())));
            refreshTotals.run();
            refreshMortgageStatementSummaries(statements, statementSummaries, refreshTotals);
        };
        refresh.run();
        ComboBox<Integer> year = new ComboBox<>(FXCollections.observableArrayList(null, 2022, 2023, 2024, 2025, 2026));
        year.setValue(selectedYearValue);
        ComboBox<Integer> month = new ComboBox<>(FXCollections.observableArrayList(null, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        month.setValue(selectedMonthValue);
        Button filter = new Button("Aplicar filtros");
        filter.getStyleClass().add("primary");
        filter.setOnAction(event -> {
            selectedYearValue = year.getValue();
            selectedMonthValue = month.getValue();
            refresh.run();
        });
        Button importPdf = new Button("Importar PDF");
        importPdf.getStyleClass().add("primary");
        importPdf.setOnAction(event -> importMortgagePdf(alias, refresh));
        Button analysis = new Button("Ver analisis");
        analysis.setOnAction(event -> showMortgageAnalysis(alias));
        Button addStatement = new Button("Entrada manual");
        addStatement.setOnAction(event -> addManualMortgageStatement(alias, statements, refresh));
        Button save = new Button("Guardar visibles");
        save.setOnAction(event -> saveVisibleMortgageRows(statements, movements, refresh));
        VBox actions = actionHeader(
            new HBox(10, new Label("Ano"), year, new Label("Mes"), month, filter),
            new HBox(10, importPdf, analysis, addStatement, save)
        );
        Button saveStatements = new Button("Guardar");
        saveStatements.getStyleClass().add("primary");
        saveStatements.setOnAction(event -> saveVisibleMortgageRows(statements, movements, refresh));
        HBox statementActions = new HBox(10, saveStatements);
        statementActions.setAlignment(Pos.CENTER_LEFT);
        Button saveMovements = new Button("Guardar");
        saveMovements.getStyleClass().add("primary");
        saveMovements.setOnAction(event -> saveVisibleMortgageRows(statements, movements, refresh));
        HBox movementActions = new HBox(10, saveMovements);
        movementActions.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(movements, Priority.ALWAYS);
        TabPane tabs = new TabPane(
            tab("Statements", new VBox(6, statementActions, statementSummaries)),
            tab("Movimientos", new VBox(10, movementActions, movements))
        );
        VBox.setVgrow(tabs, Priority.ALWAYS);
        setPage(page("Hipoteca - " + alias, backButton("Volver a Hipotecas", this::showMortgages), actions, totals, monthlyMortgageCards(alias, statements, movements, totals, statementSummaries), tabs));
    }

    private void refreshMortgageStatementSummaries(TableView<MortgageStatement> table, VBox summaries, Runnable refreshTotals) {
        summaries.getChildren().clear();
        for (MortgageStatement statement : table.getItems()) {
            summaries.getChildren().add(horizontalStatementScroll(editableMortgageStatementSummary(statement, table, summaries, refreshTotals)));
        }
    }

    private VBox editableMortgageStatementSummary(MortgageStatement statement, TableView<MortgageStatement> table, VBox summaries, Runnable refreshTotals) {
        VBox summary = new MortgageStatementSummaryView().build(
            statement,
            mortgageTransactionsForStatement(statement),
            fieldName -> mortgageStatementFieldReviewRepository.isReviewed(statement.getId(), fieldName, !statement.isPendingReview()),
            (fieldName, reviewed) -> {
                updateMortgageStatementFieldReview(statement, fieldName, reviewed);
                table.refresh();
                refreshTotals.run();
            },
            (fieldName, value) -> {
                updateMortgageStatementAmount(statement, fieldName, value);
                table.refresh();
                refreshTotals.run();
            },
            reviewed -> {
                updateMortgageStatementReview(statement, reviewed);
                updateAllMortgageStatementFieldReviews(statement, reviewed);
                table.refresh();
                refreshTotals.run();
                refreshMortgageStatementSummaries(table, summaries, refreshTotals);
            },
            (transaction, reviewed) -> {
                updateMortgageTransactionReview(transaction, reviewed);
                refreshTotals.run();
            },
            (transaction, fieldName, value) -> {
                updateMortgageTransactionAmount(transaction, fieldName, value);
                refreshTotals.run();
            },
            () -> {
                showMortgageStatementDialog(statement, () -> {
                    table.refresh();
                    refreshTotals.run();
                    refreshMortgageStatementSummaries(table, summaries, refreshTotals);
                });
            },
            () -> {
                if (!confirm("Eliminar periodo de hipoteca", "Se eliminaran este statement, sus movimientos y alertas.\n\nEsta accion no se puede deshacer.", "Eliminar periodo")) {
                    return;
                }
                if (statement.getId() > 0) {
                    mortgageStatementRepository.delete(statement.getId());
                }
                showMortgageDetail(statement.getLoanAlias());
            }
        );
        summary.setOnMouseClicked(event -> table.getSelectionModel().select(statement));
        return summary;
    }

    private List<MortgageTransaction> mortgageTransactionsForStatement(MortgageStatement statement) {
        if (statement.getId() <= 0 || statement.getStatementDate() == null) {
            return List.of();
        }
        return mortgageTransactionRepository
            .findByLoan(statement.getLoanAlias(), statement.getStatementDate().getYear(), statement.getStatementDate().getMonthValue())
            .stream()
            .filter(transaction -> transaction.getStatementId() == statement.getId())
            .toList();
    }

    private List<String> mortgageStatementFieldKeys() {
        return List.of(
            "principal_due",
            "interest_due",
            "escrow_due",
            "regular_monthly_payment",
            "past_due_amount",
            "fees",
            "other_fees_and_charges",
            "total_due",
            "original_principal_balance",
            "outstanding_principal_balance",
            "escrow_balance",
            "unapplied_funds",
            "past_paid_principal_since_last_statement",
            "past_paid_principal_year_to_date",
            "past_paid_interest_since_last_statement",
            "past_paid_interest_year_to_date",
            "past_paid_escrow_since_last_statement",
            "past_paid_escrow_year_to_date",
            "past_paid_total_since_last_statement",
            "past_paid_total_year_to_date"
        );
    }

    private void updateMortgageStatementAmount(MortgageStatement statement, String fieldName, double value) {
        switch (fieldName) {
            case "principal_due" -> statement.setPrincipalDue(value);
            case "interest_due" -> statement.setInterestDue(value);
            case "escrow_due" -> statement.setEscrowDue(value);
            case "regular_monthly_payment" -> statement.setRegularMonthlyPayment(value);
            case "past_due_amount" -> statement.setPastDueAmount(value);
            case "fees" -> statement.setFees(value);
            case "other_fees_and_charges" -> statement.setOtherFeesAndCharges(value);
            case "total_due" -> {
                statement.setTotalDue(value);
                statement.setPaymentAmountDue(value);
            }
            case "original_principal_balance" -> statement.setOriginalPrincipalBalance(value);
            case "outstanding_principal_balance" -> statement.setOutstandingPrincipalBalance(value);
            case "escrow_balance" -> statement.setEscrowBalance(value);
            case "unapplied_funds" -> statement.setUnappliedFunds(value);
            case "past_paid_principal_since_last_statement" -> statement.setPastPaidPrincipalSinceLastStatement(value);
            case "past_paid_principal_year_to_date" -> statement.setPastPaidPrincipalYearToDate(value);
            case "past_paid_interest_since_last_statement" -> statement.setPastPaidInterestSinceLastStatement(value);
            case "past_paid_interest_year_to_date" -> statement.setPastPaidInterestYearToDate(value);
            case "past_paid_escrow_since_last_statement" -> statement.setPastPaidEscrowSinceLastStatement(value);
            case "past_paid_escrow_year_to_date" -> statement.setPastPaidEscrowYearToDate(value);
            case "past_paid_total_since_last_statement" -> statement.setPastPaidTotalSinceLastStatement(value);
            case "past_paid_total_year_to_date" -> statement.setPastPaidTotalYearToDate(value);
            default -> {
                return;
            }
        }
        if (statement.getId() > 0) {
            mortgageStatementRepository.updateRecord(statement);
        }
    }

    private void updateMortgageTransactionAmount(MortgageTransaction transaction, String fieldName, double value) {
        switch (fieldName) {
            case "total" -> transaction.setTotal(value);
            case "principal" -> transaction.setPrincipal(value);
            case "interest" -> transaction.setInterest(value);
            case "escrow" -> transaction.setEscrow(value);
            case "fees" -> transaction.setFees(value);
            case "unapplied" -> transaction.setUnapplied(value);
            case "corporate_advance" -> transaction.setCorporateAdvance(value);
            case "other" -> transaction.setOther(value);
            default -> {
                return;
            }
        }
        if (transaction.getId() > 0) {
            mortgageTransactionRepository.update(transaction);
        }
    }

    private void updateMortgageStatementFieldReview(MortgageStatement statement, String fieldName, boolean reviewed) {
        if (!mortgageStatementFieldReviewRepository.hasReviews(statement.getId())) {
            mortgageStatementFieldReviewRepository.setReviewed(statement.getId(), mortgageStatementFieldKeys(), !statement.isPendingReview());
        }
        mortgageStatementFieldReviewRepository.setReviewed(statement.getId(), fieldName, reviewed);
        boolean allReviewed = mortgageStatementFieldKeys().stream()
            .allMatch(key -> mortgageStatementFieldReviewRepository.isReviewed(statement.getId(), key, !statement.isPendingReview()));
        updateMortgageStatementReview(statement, allReviewed);
    }

    private void updateAllMortgageStatementFieldReviews(MortgageStatement statement, boolean reviewed) {
        mortgageStatementFieldReviewRepository.setReviewed(statement.getId(), mortgageStatementFieldKeys(), reviewed);
    }

    private void updateMortgageStatementReview(MortgageStatement statement, boolean reviewed) {
        statement.setPendingReview(!reviewed);
        statement.setReviewRequired(!reviewed);
        if (reviewed && (statement.getReviewNotes() == null || statement.getReviewNotes().isBlank() || statement.getReviewNotes().startsWith("Revisar") || statement.getReviewNotes().startsWith("OCR:"))) {
            statement.setReviewNotes("Revisado");
        } else if (!reviewed && "Revisado".equalsIgnoreCase(statement.getReviewNotes())) {
            statement.setReviewNotes("Revisar contra el PDF original");
        }
        if (statement.getId() > 0) {
            mortgageStatementRepository.updateRecord(statement);
        }
    }

    private void updateMortgageTransactionReview(MortgageTransaction transaction, boolean reviewed) {
        transaction.setPendingReview(!reviewed);
        transaction.setReviewRequired(!reviewed);
        if (reviewed && (transaction.getReviewNotes() == null || transaction.getReviewNotes().isBlank() || transaction.getReviewNotes().startsWith("Revisar"))) {
            transaction.setReviewNotes("Revisado");
        } else if (!reviewed && "Revisado".equalsIgnoreCase(transaction.getReviewNotes())) {
            transaction.setReviewNotes("Revisar contra el PDF original");
        }
        if (transaction.getId() > 0) {
            mortgageTransactionRepository.update(transaction);
        }
    }

    private void importMortgagePdf(String alias, Runnable refresh) {
        File file = choosePdf();
        if (file == null) {
            return;
        }
        try {
            var parsed = mortgageImportService.importPdf(file.toPath());
            MortgageStatement statement = parsed.statement();
            statement.setLoanAlias(alias);
            long statementId = mortgageStatementRepository.save(statement);
            mortgageTransactionRepository.saveAll(statementId, parsed.transactions());
            mortgageAlertRepository.saveAll(statementId, mortgageAnalysisService.analyze(statement).alerts());
            if (statement.getStatementDate() != null) {
                selectedYearValue = statement.getStatementDate().getYear();
                selectedMonthValue = statement.getStatementDate().getMonthValue();
            }
            rebuildSidebar();
            showMortgageDetail(alias);
        } catch (RuntimeException exception) {
            alert(Alert.AlertType.ERROR, "No se pudo importar hipoteca", rootCauseMessage(exception));
        }
    }

    private void addManualMortgageStatement(String alias, TableView<MortgageStatement> table, Runnable refresh) {
        MortgageStatement statement = new MortgageStatement();
        statement.setLoanAlias(alias);
        statement.setStatementDate(LocalDate.now());
        statement.setPaymentDueDate(LocalDate.now().plusDays(21));
        statement.setPendingReview(true);
        statement.setReviewRequired(true);
        statement.setReviewNotes("Anadido manualmente");
        table.getItems().add(statement);
        table.getSelectionModel().select(statement);
        refresh.run();
    }

    private void updateMortgageTransactionIfSaved(MortgageTransaction movement) {
        if (movement.getId() > 0) {
            mortgageTransactionRepository.update(movement);
        }
    }

    private void showMortgageStatementDialog(MortgageStatement statement, Runnable refresh) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar statement de hipoteca");
        TextField statementDate = new TextField(dateText(statement.getStatementDate()));
        TextField dueDate = new TextField(dateText(statement.getPaymentDueDate()));
        TextField totalDue = moneyField(mortgageDueAmount(statement));
        TextField principal = moneyField(statement.getPrincipalDue());
        TextField interest = moneyField(statement.getInterestDue());
        TextField escrow = moneyField(statement.getEscrowDue());
        TextField original = moneyField(statement.getOriginalPrincipalBalance());
        TextField outstanding = moneyField(statement.getOutstandingPrincipalBalance());
        TextField escrowBalance = moneyField(statement.getEscrowBalance());
        TextField paidPrincipal = moneyField(statement.getPastPaidPrincipalSinceLastStatement());
        TextField paidInterest = moneyField(statement.getPastPaidInterestSinceLastStatement());
        TextField paidEscrow = moneyField(statement.getPastPaidEscrowSinceLastStatement());
        TextField paidTotal = moneyField(statement.getPastPaidTotalSinceLastStatement());
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Statement Date"), statementDate);
        form.addRow(1, new Label("Payment Due Date"), dueDate);
        form.addRow(2, new Label("Total Due"), totalDue);
        form.addRow(3, new Label("Principal"), principal);
        form.addRow(4, new Label("Interest"), interest);
        form.addRow(5, new Label("Escrow"), escrow);
        form.addRow(6, new Label("Original Principal Balance"), original);
        form.addRow(7, new Label("Outstanding Principal Balance"), outstanding);
        form.addRow(8, new Label("Escrow Balance"), escrowBalance);
        form.addRow(9, new Label("Past Principal"), paidPrincipal);
        form.addRow(10, new Label("Past Interest"), paidInterest);
        form.addRow(11, new Label("Past Escrow"), paidEscrow);
        form.addRow(12, new Label("Past Total"), paidTotal);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(result -> {
            statement.setStatementDate(parseDateOrNull(statementDate.getText()));
            statement.setPaymentDueDate(parseDateOrNull(dueDate.getText()));
            double due = Money.parse(totalDue.getText());
            statement.setTotalDue(due);
            statement.setPaymentAmountDue(due);
            statement.setPrincipalDue(Money.parse(principal.getText()));
            statement.setInterestDue(Money.parse(interest.getText()));
            statement.setEscrowDue(Money.parse(escrow.getText()));
            statement.setOriginalPrincipalBalance(Money.parse(original.getText()));
            statement.setOutstandingPrincipalBalance(Money.parse(outstanding.getText()));
            statement.setEscrowBalance(Money.parse(escrowBalance.getText()));
            statement.setPastPaidPrincipalSinceLastStatement(Money.parse(paidPrincipal.getText()));
            statement.setPastPaidInterestSinceLastStatement(Money.parse(paidInterest.getText()));
            statement.setPastPaidEscrowSinceLastStatement(Money.parse(paidEscrow.getText()));
            statement.setPastPaidTotalSinceLastStatement(Money.parse(paidTotal.getText()));
            if (statement.getId() > 0) {
                mortgageStatementRepository.updateRecord(statement);
            }
            refresh.run();
        });
    }

    private void showMortgageTransactionDialog(MortgageTransaction movement, Runnable refresh) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar movimiento de hipoteca");
        TextField date = new TextField(dateText(movement.getTransactionDate()));
        TextField description = new TextField(text(movement.getDescription()));
        TextField total = moneyField(movement.getTotal());
        TextField principal = moneyField(movement.getPrincipal());
        TextField interest = moneyField(movement.getInterest());
        TextField escrow = moneyField(movement.getEscrow());
        TextField fees = moneyField(movement.getFees());
        TextField unapplied = moneyField(movement.getUnapplied());
        TextField other = moneyField(movement.getOther());
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Fecha"), date);
        form.addRow(1, new Label("Descripcion"), description);
        form.addRow(2, new Label("Total"), total);
        form.addRow(3, new Label("Principal"), principal);
        form.addRow(4, new Label("Interest"), interest);
        form.addRow(5, new Label("Escrow"), escrow);
        form.addRow(6, new Label("Fees"), fees);
        form.addRow(7, new Label("Unapplied"), unapplied);
        form.addRow(8, new Label("Other"), other);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(result -> {
            movement.setTransactionDate(parseDateOrNull(date.getText()));
            movement.setDescription(description.getText());
            movement.setTotal(Money.parse(total.getText()));
            movement.setPrincipal(Money.parse(principal.getText()));
            movement.setInterest(Money.parse(interest.getText()));
            movement.setEscrow(Money.parse(escrow.getText()));
            movement.setFees(Money.parse(fees.getText()));
            movement.setUnapplied(Money.parse(unapplied.getText()));
            movement.setOther(Money.parse(other.getText()));
            updateMortgageTransactionIfSaved(movement);
            refresh.run();
        });
    }

    private TextField moneyField(double amount) {
        TextField field = new TextField(Money.format(amount));
        field.setPrefWidth(150);
        return field;
    }

    private String dateText(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private double mortgageDueAmount(MortgageStatement statement) {
        return statement.getTotalDue() > 0 ? statement.getTotalDue() : statement.getPaymentAmountDue();
    }

    private void saveVisibleMortgageRows(TableView<MortgageStatement> statements, TableView<MortgageTransaction> movements, Runnable refresh) {
        for (MortgageStatement statement : statements.getItems()) {
            if (statement.getId() > 0) {
                mortgageStatementRepository.updateRecord(statement);
            } else {
                statement.setId(mortgageStatementRepository.save(statement));
            }
        }
        for (MortgageTransaction movement : movements.getItems()) {
            long statementId = statements.getItems().isEmpty() ? 0 : statements.getItems().get(0).getId();
            if (movement.getId() <= 0 && statementId > 0) {
                movement.setId(mortgageTransactionRepository.save(statementId, movement));
            }
        }
        refresh.run();
        alert(Alert.AlertType.INFORMATION, "Hipoteca guardada", "Cambios visibles guardados.");
    }

    private void showMortgageAnalysis(String alias) {
        List<MortgageStatement> statements = mortgageStatementRepository.findByLoan(alias, null, null).stream()
            .filter(statement -> statement.getStatementDate() != null)
            .sorted(Comparator.comparing(MortgageStatement::getStatementDate))
            .toList();
        List<MortgageStatement> source = mortgageReviewedOrAll(statements);
        HBox totals = new HBox(12);
        totals.getChildren().setAll(mortgageAnalysisTotalsNodes(source));
        totals.getStyleClass().add("totals-panel");
        setPage(page("Analisis de hipoteca - " + alias, backButton("Volver al detalle", () -> showMortgageDetail(alias)), totals, mortgageDebtChart(source), mortgagePaymentChart(source)));
    }

    private LineChart<String, Number> mortgageDebtChart(List<MortgageStatement> statements) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Mes");
        yAxis.setLabel("Deuda principal pendiente");
        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Evolucion de deuda pendiente");
        chart.setLegendVisible(false);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        List<Double> debtValues = new ArrayList<>();
        double initialDebt = mortgageInitialDebt(statements);
        double debt = initialDebt;
        if (initialDebt > 0) {
            series.getData().add(new XYChart.Data<>("Initial Debt", initialDebt));
            debtValues.add(initialDebt);
        }
        for (MortgageStatement statement : statements) {
            if (statement.getStatementDate() == null) {
                continue;
            }
            debt -= statement.getPastPaidPrincipalSinceLastStatement();
            double value = debt > 0 ? debt : statement.getOutstandingPrincipalBalance();
            series.getData().add(new XYChart.Data<>(monthName(statement.getStatementDate().getMonthValue()) + " " + statement.getStatementDate().getYear(), value));
            debtValues.add(value);
        }
        configureDebtAxis(yAxis, debtValues);
        chart.getData().add(series);
        chart.setMinHeight(320);
        return chart;
    }

    private void configureDebtAxis(NumberAxis yAxis, List<Double> values) {
        if (values.isEmpty()) {
            return;
        }
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double spread = Math.max(max - min, Math.max(max * 0.002, 1000));
        double padding = spread * 0.25;
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(Math.max(0, min - padding));
        yAxis.setUpperBound(max + padding);
        yAxis.setTickUnit(Math.max(100, spread / 5));
    }

    private StackedBarChart<String, Number> mortgagePaymentChart(List<MortgageStatement> statements) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Mes");
        yAxis.setLabel("Importe");
        StackedBarChart<String, Number> chart = new StackedBarChart<>(xAxis, yAxis);
        chart.setTitle("Payment breakdown by month");
        XYChart.Series<String, Number> principal = new XYChart.Series<>();
        principal.setName("Principal");
        XYChart.Series<String, Number> interest = new XYChart.Series<>();
        interest.setName("Interest");
        XYChart.Series<String, Number> escrow = new XYChart.Series<>();
        escrow.setName("Escrow");
        for (MortgageStatement statement : statements) {
            if (statement.getStatementDate() == null) {
                continue;
            }
            String label = monthName(statement.getStatementDate().getMonthValue()) + " " + statement.getStatementDate().getYear();
            principal.getData().add(new XYChart.Data<>(label, statement.getPastPaidPrincipalSinceLastStatement()));
            interest.getData().add(new XYChart.Data<>(label, statement.getPastPaidInterestSinceLastStatement()));
            escrow.getData().add(new XYChart.Data<>(label, statement.getPastPaidEscrowSinceLastStatement()));
        }
        chart.getData().addAll(principal, interest, escrow);
        chart.setMinHeight(340);
        return chart;
    }

    private void showHouseExpenses() {
        Map<Long, String> originalRows = new HashMap<>();
        TableView<HouseExpense> table = houseExpenseTable(null, originalRows);
        Runnable refresh = () -> {
            table.setItems(FXCollections.observableArrayList(houseExpenseRepository.findByLoan(null, null, null)));
            captureHouseExpenseRows(table, originalRows);
        };
        refresh.run();
        Button add = new Button("Anadir gasto");
        add.getStyleClass().add("primary");
        add.setOnAction(event -> {
            HouseExpense expense = new HouseExpense(0, "", LocalDate.now(), "Gasto manual", "", 0, "", "");
            table.getItems().add(expense);
            table.getSelectionModel().select(expense);
        });
        Button save = new Button("Guardar cambios");
        save.setOnAction(event -> saveHouseExpenseChanges(table, originalRows));
        setPage(page("Casa - Gastos", backButton("Volver a Hipotecas", this::showMortgages), new HBox(10, add, save), table));
    }

    private void showInternalMovements() {
        TableView<InternalMovementRecord> table = internalMovementTable();
        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(internalMovementRepository.findManual(selectedYear(), selectedMonth())));
        refresh.run();
        ComboBox<Integer> year = new ComboBox<>(FXCollections.observableArrayList(null, 2022, 2023, 2024, 2025, 2026));
        year.setValue(selectedYearValue);
        ComboBox<Integer> month = new ComboBox<>(FXCollections.observableArrayList(null, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        month.setValue(selectedMonthValue);
        Button filter = new Button("Aplicar filtros");
        filter.getStyleClass().add("primary");
        filter.setOnAction(event -> {
            selectedYearValue = year.getValue();
            selectedMonthValue = month.getValue();
            refresh.run();
        });
        Button add = new Button("Anadir movimiento");
        add.setOnAction(event -> table.getItems().add(new InternalMovementRecord(0, "manual", System.nanoTime(), LocalDate.now(), "", "", 0, "Movimiento manual", "pendiente", false, true)));
        Button save = new Button("Guardar visibles");
        save.setOnAction(event -> {
            for (InternalMovementRecord movement : table.getItems()) {
                movement.setManual(true);
                long id = internalMovementRepository.save(movement);
                movement.setId(id);
            }
            refresh.run();
        });
        VBox actions = actionHeader(new HBox(10, new Label("Ano"), year, new Label("Mes"), month, filter), new HBox(10, add, save));
        setPage(page("Movimientos internos", actions, table));
    }

    private TableView<InternalMovementRecord> internalMovementTable() {
        TableView<InternalMovementRecord> table = new TableView<>();
        table.setEditable(true);
        TableColumn<InternalMovementRecord, String> date = new TableColumn<>("Fecha");
        date.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDate() == null ? "" : data.getValue().getDate().toString()));
        date.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        date.setOnEditCommit(event -> event.getRowValue().setDate(parseDateOrNull(event.getNewValue())));
        TableColumn<InternalMovementRecord, String> from = new TableColumn<>("Desde");
        from.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFrom()));
        from.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        from.setOnEditCommit(event -> event.getRowValue().setFrom(event.getNewValue()));
        TableColumn<InternalMovementRecord, String> to = new TableColumn<>("Hacia");
        to.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTo()));
        to.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        to.setOnEditCommit(event -> event.getRowValue().setTo(event.getNewValue()));
        TableColumn<InternalMovementRecord, Double> amount = new TableColumn<>("Importe");
        amount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getAmount()).asObject());
        amount.setCellFactory(commitOnFocusLostCellFactory(twoDecimalConverter()));
        amount.setOnEditCommit(event -> event.getRowValue().setAmount(event.getNewValue()));
        TableColumn<InternalMovementRecord, String> description = new TableColumn<>("Descripcion");
        description.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        description.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        description.setOnEditCommit(event -> event.getRowValue().setDescription(event.getNewValue()));
        TableColumn<InternalMovementRecord, Boolean> reviewed = new TableColumn<>("Revisado");
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(data.getValue().isReviewed()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> getTableView().getItems().get(getIndex()).setReviewed(checkBox.isSelected()));
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(Boolean.TRUE.equals(item));
                    setGraphic(checkBox);
                }
            }
        });
        TableColumn<InternalMovementRecord, Void> delete = new TableColumn<>("Eliminar");
        delete.setCellFactory(column -> new TableCell<>() {
            private final Button button = new Button("Eliminar");
            {
                button.setOnAction(event -> {
                    InternalMovementRecord movement = getTableView().getItems().get(getIndex());
                    if (movement.getId() > 0) {
                        internalMovementRepository.delete(movement.getId());
                    }
                    getTableView().getItems().remove(movement);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        });
        table.getColumns().setAll(date, from, to, amount, description, reviewed, delete);
        return table;
    }

    private String last4(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return digits.isBlank() ? "nueva" : digits;
        }
        return digits.substring(digits.length() - 4);
    }

    private void showNylHub() {
        HBox cards = new HBox(14,
            nylHubCard("Resumen NYL", this::showNyl),
            nylHubCard("Análisis NYL", this::showAnalysis),
            nylHubCard("Agent Ledger", this::showAgentLedger)
        );
        cards.getStyleClass().add("monthly-card-row");
        setDarkHubPage("New York Life", cards);
    }

    private VBox nylHubCard(String title, Runnable action) {
        Label label = new Label(title);
        label.getStyleClass().add("monthly-card-title");
        VBox card = new VBox(6, label);
        card.getStyleClass().add("monthly-card");
        card.setOnMouseClicked(event -> action.run());
        return card;
    }

    private void showNyl() {
        TableView<NylRecord> table = nylTable();
        HBox totals = new HBox(12);
        totals.getStyleClass().add("totals-panel");
        Runnable refreshTotals = () -> totals.getChildren().setAll(nylTotalsNodes(nylRepository.totals(selectedYear(), null)));
        refreshTotals.run();
        VBox monthlyCards = new VBox(10);
        Runnable refresh = () -> {
            table.setItems(FXCollections.observableArrayList(nylRepository.find(selectedYear(), selectedMonth(), null, null)));
            refreshTotals.run();
            monthlyCards.getChildren().setAll(monthlyNylCards(table, totals));
        };
        refresh.run();

        ComboBox<Integer> year = new ComboBox<>(FXCollections.observableArrayList(null, 2022, 2023, 2024, 2025, 2026));
        year.setValue(selectedYearValue);
        year.setPromptText("Año");
        year.getStyleClass().add("compact-combo");
        ComboBox<Integer> month = new ComboBox<>(FXCollections.observableArrayList(null, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        month.setValue(selectedMonthValue);
        month.setPromptText("Mes");
        month.getStyleClass().add("compact-combo");
        TextField concept = new TextField();
        concept.setPromptText("Concepto");
        concept.getStyleClass().add("concept-field");
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("", "comision", "credito", "deduccion", "withdrawal", "ajuste", "otro"));
        type.setPromptText("Tipo");
        type.getStyleClass().add("type-combo");
        Button filter = new Button("Aplicar filtros");
        filter.getStyleClass().add("primary");
        filter.setOnAction(event -> {
            selectedYearValue = year.getValue();
            selectedMonthValue = month.getValue();
            table.setItems(FXCollections.observableArrayList(nylRepository.find(selectedYear(), selectedMonth(), concept.getText(), type.getValue())));
            refreshTotals.run();
            monthlyCards.getChildren().setAll(monthlyNylCards(table, totals));
        });
        Button importPdf = new Button("Importar PDF");
        importPdf.getStyleClass().add("primary");
        importPdf.setOnAction(event -> importNyl(refresh));
        Button manual = new Button("Entrada manual");
        manual.setOnAction(event -> manualNyl(refresh));
        Button pending = new Button("Pendientes por revisar");
        pending.setOnAction(event -> showPendingNylReview(refresh));
        Button saveProgress = new Button("Guardar progreso");
        saveProgress.setOnAction(event -> saveVisibleNylRows(table, refresh, false));
        Button saveReviewed = new Button("Guardar revisados");
        saveReviewed.getStyleClass().add("primary");
        saveReviewed.setOnAction(event -> saveVisibleNylRows(table, refresh, true));
        Button addRecord = new Button("Añadir registro");
        addRecord.setOnAction(event -> addMissingNylRow(table));
        VBox actions = actionHeader(
            new HBox(10, new Label("Año"), year, new Label("Mes"), month, concept, type, filter),
            new HBox(10, importPdf, manual, pending, saveProgress, saveReviewed)
        );
        HBox tableActions = new HBox(10, addRecord);
        tableActions.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setMinHeight(420);
        table.setPrefHeight(520);
        setPage(page("New York Life", backButton("Volver a New York Life", this::showNylHub), actions, totals, monthlyCards, tableActions, table));
    }

    private void showReconciliation() {
        int yearValue = selectedYear() == null ? LocalDate.now().getYear() : selectedYear();
        int monthValue = selectedMonth() == null ? LocalDate.now().getMonthValue() : selectedMonth();
        TableView<ReconciliationItem> table = reconciliationTable();
        Runnable refresh = () -> table.setItems(FXCollections.observableArrayList(reconciliationService.preview(selectedYear(), selectedMonth())));
        refresh.run();
        ComboBox<Integer> year = new ComboBox<>(FXCollections.observableArrayList(null, 2022, 2023, 2024, 2025, 2026));
        year.setValue(selectedYearValue == null ? yearValue : selectedYearValue);
        year.setPromptText("Año");
        year.getStyleClass().add("compact-combo");
        ComboBox<Integer> month = new ComboBox<>(FXCollections.observableArrayList(null, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        month.setValue(selectedMonthValue == null ? monthValue : selectedMonthValue);
        month.setPromptText("Mes");
        month.getStyleClass().add("compact-combo");
        final TabPane[] tabsRef = new TabPane[1];
        Button apply = new Button("Aplicar mes");
        apply.getStyleClass().add("primary");
        apply.setOnAction(event -> {
            selectedYearValue = year.getValue();
            selectedMonthValue = month.getValue();
            refresh.run();
            tabsRef[0].getTabs().setAll(reconciliationTabs(table));
        });
        Button download = new Button("Descargar conciliacion mensual");
        download.getStyleClass().add("primary");
        download.setOnAction(event -> exportMonthlyReconciliation());
        TextArea notes = new TextArea();
        notes.setPromptText("Notas de conciliacion");
        notes.setPrefRowCount(3);
        Button auto = new Button("Guardar conciliados");
        auto.getStyleClass().add("primary");
        auto.setOnAction(event -> {
            reconciliationRepository.saveMatches(table.getItems(), notes.getText());
            alert(Alert.AlertType.INFORMATION, "Conciliación guardada", "Se guardaron las coincidencias exactas.");
            refresh.run();
        });
        VBox.setVgrow(table, Priority.ALWAYS);
        TabPane tabs = new TabPane();
        tabsRef[0] = tabs;
        tabs.getTabs().setAll(reconciliationTabs(table));
        VBox actions = actionHeader(new HBox(10, new Label("Año"), year, new Label("Mes"), month, apply), new HBox(10, download, auto));
        setPage(page("Conciliación", actions, tabs, notes));
    }

    private List<Tab> reconciliationTabs(TableView<ReconciliationItem> table) {
        return List.of(
            tab("Resumen mensual", reconciliationSummaryView()),
            tab("Banco", reconciliationBankView()),
            tab("Tarjetas", reconciliationCardsView()),
            tab("Hipotecas", reconciliationMortgageView()),
            tab("New York Life", reconciliationNylView()),
            tab("NYL vs Banco", reconciliationNylBankView()),
            tab("Cruces NYL/Banco", table)
        );
    }

    private Tab tab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }

    private VBox reconciliationSummaryView() {
        int year = selectedYear() == null ? LocalDate.now().getYear() : selectedYear();
        int month = selectedMonth() == null ? LocalDate.now().getMonthValue() : selectedMonth();
        List<BankTransaction> bankRows = bankModule.application().transactions().find(year, month, null, null);
        List<NylRecord> nyl = nylRepository.find(year, month, null, null);
        List<CreditCardStatement> statements = allCardStatements(year, month);
        List<CreditCardTransaction> movements = allCardTransactions(year, month);
        List<MortgageStatement> mortgageStatements = allMortgageStatements(year, month);
        List<MortgageTransaction> mortgageMovements = allMortgageTransactions(year, month);
        HBox totals = new HBox(12,
            miniTotal("Banco pendientes", String.valueOf(bankRows.stream().filter(BankTransaction::isPendingReview).count()), "pending-total"),
            miniTotal("Tarjetas pendientes", String.valueOf(statements.stream().filter(CreditCardStatement::isPendingReview).count() + movements.stream().filter(CreditCardTransaction::isPendingReview).count()), "pending-total"),
            miniTotal("Hipotecas pendientes", String.valueOf(mortgageStatements.stream().filter(MortgageStatement::isPendingReview).count() + mortgageMovements.stream().filter(MortgageTransaction::isPendingReview).count()), "pending-total"),
            miniTotal("NYL pendientes", String.valueOf(nyl.stream().filter(NylRecord::isPendingReview).count()), "pending-total"),
            miniTotal("Cruces pendientes", String.valueOf(reconciliationService.preview(year, month).stream().filter(item -> !"Conciliado".equals(item.status())).count()), "neutral-total")
        );
        totals.getStyleClass().add("totals-panel");
        Label title = new Label(monthName(month) + " " + year);
        title.getStyleClass().add("section-title");
        Label detail = new Label("Vista mensual para revisar si Banco, Tarjetas, Hipotecas y New York Life estan listos para cierre.");
        detail.getStyleClass().add("section-subtitle");
        return new VBox(12, title, detail, totals);
    }

    private VBox reconciliationBankView() {
        int year = selectedYear() == null ? LocalDate.now().getYear() : selectedYear();
        int month = selectedMonth() == null ? LocalDate.now().getMonthValue() : selectedMonth();
        List<BankReconciliationView.AccountSummary> summaries = bankModule.accounts().list().stream()
            .map(account -> new BankReconciliationView.AccountSummary(
                account.getAlias(),
                bankModule.application().transactions().totals(year, month, account.getAlias())
            ))
            .toList();
        return new BankReconciliationView(bankModule.application()).bankByAccount(summaries, this::showBankAccount);
    }

    private VBox reconciliationCardsView() {
        HBox cards = new HBox(12);
        cards.getStyleClass().add("monthly-card-row");
        int year = selectedYear() == null ? LocalDate.now().getYear() : selectedYear();
        int month = selectedMonth() == null ? LocalDate.now().getMonthValue() : selectedMonth();
        for (CreditCardAccount account : creditCardAccountRepository.findAll()) {
            List<CreditCardStatement> statements = creditCardStatementRepository.findByAccount(account.getAlias(), year, month);
            List<CreditCardTransaction> movements = creditCardTransactionRepository.findByAccount(account.getAlias(), year, month);
            double debt = statements.stream().filter(s -> !s.isPendingReview()).mapToDouble(CreditCardStatement::getNewBalance).sum();
            double interest = movements.stream().filter(m -> !m.isPendingReview() && "interes".equalsIgnoreCase(m.getType())).mapToDouble(CreditCardTransaction::getAmount).sum();
            long pending = statements.stream().filter(CreditCardStatement::isPendingReview).count() + movements.stream().filter(CreditCardTransaction::isPendingReview).count();
            cards.getChildren().add(monthlyActionCard(account.getAlias(),
                "Deuda: " + Money.format(debt),
                "Intereses: " + Money.format(interest),
                "Movimientos: " + movements.size(),
                "Pendientes: " + pending,
                () -> showCardAccount(account.getAlias())));
        }
        return new VBox(10, sectionLabel("Tarjetas"), cards);
    }

    private VBox reconciliationMortgageView() {
        HBox cards = new HBox(12);
        cards.getStyleClass().add("monthly-card-row");
        int year = selectedYear() == null ? LocalDate.now().getYear() : selectedYear();
        int month = selectedMonth() == null ? LocalDate.now().getMonthValue() : selectedMonth();
        for (String alias : mortgageStatementRepository.findLoanAliases()) {
            List<MortgageStatement> statements = mortgageStatementRepository.findByLoan(alias, year, month);
            List<MortgageTransaction> movements = mortgageTransactionRepository.findByLoan(alias, year, month);
            double totalDue = statements.stream().filter(s -> !s.isPendingReview()).mapToDouble(s -> s.getTotalDue() > 0 ? s.getTotalDue() : s.getPaymentAmountDue()).sum();
            double principal = statements.stream().filter(s -> !s.isPendingReview()).mapToDouble(MortgageStatement::getPrincipalDue).sum();
            double interest = statements.stream().filter(s -> !s.isPendingReview()).mapToDouble(MortgageStatement::getInterestDue).sum();
            long pending = statements.stream().filter(MortgageStatement::isPendingReview).count() + movements.stream().filter(MortgageTransaction::isPendingReview).count();
            cards.getChildren().add(monthlyActionCard(alias,
                "Deuda a pagar: " + Money.format(totalDue),
                "Principal: " + Money.format(principal),
                "Intereses: " + Money.format(interest),
                "Pendientes: " + pending,
                () -> showMortgageDetail(alias)));
        }
        return new VBox(10, sectionLabel("Hipotecas"), cards);
    }

    private VBox reconciliationNylView() {
        int year = selectedYear() == null ? LocalDate.now().getYear() : selectedYear();
        int month = selectedMonth() == null ? LocalDate.now().getMonthValue() : selectedMonth();
        SourceTotals totals = nylRepository.totals(year, month);
        HBox cards = new HBox(12,
            miniTotal("Comisiones", Money.format(totals.income()), "income-total"),
            miniTotal("Deducciones", Money.format(Math.abs(totals.expenses())), "expense-total"),
            miniTotal("Neto", Money.format(totals.net()), "net-total"),
            miniTotal("Pendientes", String.valueOf(totals.pendingCount()), "pending-total")
        );
        cards.getStyleClass().add("totals-panel");
        return new VBox(10, sectionLabel("New York Life"), cards);
    }

    private VBox reconciliationNylBankView() {
        int year = selectedYear() == null ? LocalDate.now().getYear() : selectedYear();
        int month = selectedMonth() == null ? LocalDate.now().getMonthValue() : selectedMonth();
        List<BankAccount> accounts = bankModule.accounts().list();
        BankAccount selectedAccount = accounts.stream()
            .filter(account -> selectedBankAccountAlias != null && selectedBankAccountAlias.equals(account.getAlias()))
            .findFirst()
            .orElse(accounts.stream().filter(account -> account.getAlias().toLowerCase(java.util.Locale.ROOT).contains("15705")).findFirst().orElse(null));
        String accountAlias = selectedAccount == null ? null : selectedAccount.getAlias();
        SourceTotals nylTotals = nylRepository.totals(year, month);
        List<BankTransaction> bankNyl = bankModule.application().transactions().find(year, month, "New York Life", null, accountAlias).stream()
            .filter(transaction -> !transaction.isPendingReview())
            .filter(transaction -> transaction.getAmount() > 0)
            .toList();
        return new BankReconciliationView(bankModule.application()).nylBank(
            new BankReconciliationView.NylBankSummary(selectedAccount, nylTotals, bankNyl)
        );
    }

    private void showAnalysis() {
        BarChart<String, Number> monthly = new BarChart<>(new CategoryAxis(), new NumberAxis());
        monthly.setTitle("Comisiones vs Deducciones por mes");
        monthly.setLegendVisible(true);
        monthly.getStyleClass().add("nyl-monthly-chart");
        Runnable refresh = () -> {
            List<NylRecord> records = nylRepository.find(selectedYear(), null, null, null);
            records = records.stream().filter(record -> !record.isPendingReview()).toList();
            XYChart.Series<String, Number> credits = new XYChart.Series<>();
            credits.setName("Comisiones/Créditos");
            XYChart.Series<String, Number> deductions = new XYChart.Series<>();
            deductions.setName("Deducciones");
            Map<Integer, Double> monthlyCredits = records.stream()
                .filter(r -> r.getRecordType().equals("comision") || r.getRecordType().equals("credito"))
                .collect(Collectors.groupingBy(NylRecord::getMonth, Collectors.summingDouble(NylRecord::getAmount)));
            Map<Integer, Double> monthlyDeductions = records.stream()
                .filter(r -> r.getRecordType().equals("deduccion"))
                .collect(Collectors.groupingBy(NylRecord::getMonth, Collectors.summingDouble(r -> Math.abs(r.getAmount()))));
            for (int month = 1; month <= 12; month++) {
                String label = monthName(month);
                credits.getData().add(new XYChart.Data<>(label, monthlyCredits.getOrDefault(month, 0.0)));
                deductions.getData().add(new XYChart.Data<>(label, monthlyDeductions.getOrDefault(month, 0.0)));
            }
            monthly.getData().setAll(credits, deductions);

        };
        refresh.run();
        VBox.setVgrow(monthly, Priority.ALWAYS);
        setPage(page("Análisis NYL", backButton("Volver a New York Life", this::showNylHub), monthly));
    }

    private void showAgentLedger() {
        int year = selectedYear() == null ? LocalDate.now().getYear() : selectedYear();
        selectedYearValue = year;
        TableView<AgentLedgerRow> table = agentLedgerTable(year);
        ComboBox<Integer> yearFilter = new ComboBox<>(FXCollections.observableArrayList(2022, 2023, 2024, 2025, 2026, LocalDate.now().getYear()));
        yearFilter.setValue(year);
        yearFilter.getStyleClass().add("compact-combo");
        Button apply = new Button("Aplicar año");
        apply.getStyleClass().add("primary");
        apply.setOnAction(event -> {
            selectedYearValue = yearFilter.getValue();
            showAgentLedger();
        });
        Button add = new Button("Añadir registro");
        add.setOnAction(event -> showAgentLedgerAddDialog(yearFilter.getValue() == null ? LocalDate.now().getYear() : yearFilter.getValue()));
        HBox actions = new HBox(10, new Label("Año"), yearFilter, apply, add);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(table, Priority.ALWAYS);
        setPage(page("Agent Ledger", backButton("Volver a New York Life", this::showNylHub), actions, table));
    }

    private TableView<AgentLedgerRow> agentLedgerTable(int year) {
        TableView<AgentLedgerRow> table = new TableView<>(FXCollections.observableArrayList(agentLedgerRows(year)));
        table.setEditable(true);
        table.getStyleClass().add("agent-ledger-table");
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setRowFactory(view -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(AgentLedgerRow item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeIf(style -> style.startsWith("agent-ledger-"));
                if (!empty && item != null && item.template().styleClass() != null) {
                    getStyleClass().add(item.template().styleClass());
                }
            }
        });
        TableColumn<AgentLedgerRow, String> category = new TableColumn<>("Category");
        category.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().categoryText()));
        category.setPrefWidth(190);
        TableColumn<AgentLedgerRow, String> concept = new TableColumn<>("Sub-Category");
        concept.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().conceptText()));
        concept.setPrefWidth(270);
        table.getColumns().add(category);
        table.getColumns().add(concept);
        TableColumn<AgentLedgerRow, String> firstMonthColumn = null;
        for (int month = 1; month <= 12; month++) {
            final int monthValue = month;
            TableColumn<AgentLedgerRow, String> monthColumn = new TableColumn<>(agentLedgerMonthName(month));
            monthColumn.setCellValueFactory(data -> new SimpleStringProperty(agentLedgerCellText(data.getValue(), monthValue)));
            monthColumn.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
            monthColumn.setOnEditCommit(event -> updateAgentLedgerCell(event.getRowValue(), year, monthValue, event.getNewValue()));
            monthColumn.setPrefWidth(104);
            table.getColumns().add(monthColumn);
            if (firstMonthColumn == null) {
                firstMonthColumn = monthColumn;
            }
        }
        TableColumn<AgentLedgerRow, String> ytd = new TableColumn<>("YTD Total");
        ytd.setCellValueFactory(data -> new SimpleStringProperty(agentLedgerYtdText(data.getValue())));
        ytd.setPrefWidth(120);
        table.getColumns().add(ytd);
        TableColumn<AgentLedgerRow, Void> actions = new TableColumn<>("Acciones");
        TableColumn<AgentLedgerRow, String> editableColumn = firstMonthColumn;
        actions.setCellFactory(column -> new TableCell<>() {
            private final Button edit = new Button("Editar");
            private final Button save = new Button("Guardar");
            private final Button delete = new Button("Eliminar");
            private final HBox buttons = new HBox(6, edit, save, delete);
            {
                edit.setOnAction(event -> {
                    AgentLedgerRow row = getTableView().getItems().get(getIndex());
                    getTableView().getSelectionModel().select(row);
                    getTableView().scrollTo(row);
                    getTableView().requestFocus();
                    if (editableColumn != null) {
                        getTableView().edit(getIndex(), editableColumn);
                    }
                });
                save.setOnAction(event -> {
                    AgentLedgerRow row = getTableView().getItems().get(getIndex());
                    saveAgentLedgerRow(row);
                    getTableView().refresh();
                });
                delete.setOnAction(event -> {
                    AgentLedgerRow row = getTableView().getItems().get(getIndex());
                    deleteAgentLedgerRow(row, year);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                AgentLedgerRow row = empty ? null : getTableView().getItems().get(getIndex());
                setGraphic(row != null && row.template().editable() ? buttons : null);
            }
        });
        actions.setPrefWidth(230);
        table.getColumns().add(actions);
        return table;
    }

    private List<AgentLedgerRow> agentLedgerRows(int year) {
        List<AgentLedgerRow> rows = AGENT_LEDGER_TEMPLATES.stream().map(AgentLedgerRow::new).toList();
        Map<String, AgentLedgerRow> byKey = rows.stream()
            .filter(row -> row.template().editable())
            .collect(Collectors.toMap(row -> agentLedgerKey(row.template()), row -> row, (left, right) -> left, LinkedHashMap::new));
        Map<String, AgentLedgerRow> byConceptAndType = rows.stream()
            .filter(row -> row.template().editable())
            .collect(Collectors.toMap(row -> agentLedgerConceptTypeKey(row.template().label(), row.template().type()), row -> row, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<AgentLedgerRow>> byConcept = rows.stream()
            .filter(row -> row.template().editable())
            .collect(Collectors.groupingBy(row -> normalizeAgentLedgerToken(row.template().label()), LinkedHashMap::new, Collectors.toList()));
        for (NylRecord record : nylRepository.find(year, null, null, null)) {
            AgentLedgerRow row = byKey.get(agentLedgerKey(record.getSection(), record.getConcept(), record.getRecordType()));
            if (row == null) {
                row = byConceptAndType.get(agentLedgerConceptTypeKey(record.getConcept(), record.getRecordType()));
            }
            if (row == null) {
                List<AgentLedgerRow> conceptMatches = byConcept.get(normalizeAgentLedgerToken(record.getConcept()));
                if (conceptMatches != null && conceptMatches.size() == 1) {
                    row = conceptMatches.get(0);
                }
            }
            if (row != null) {
                row.records(record.getMonth()).add(record);
            }
        }
        recalculateAgentLedgerRows(rows, year);
        return rows;
    }

    private void recalculateAgentLedgerRows(List<AgentLedgerRow> rows, int year) {
        double[] sectionTotals = new double[13];
        double[] creditTotals = new double[13];
        double[] deductionTotals = new double[13];
        double[] withdrawalTotals = new double[13];
        for (AgentLedgerRow row : rows) {
            AgentLedgerTemplate template = row.template();
            if ("SECTION".equals(template.kind())) {
                sectionTotals = new double[13];
            } else if ("INPUT".equals(template.kind())) {
                for (int month = 1; month <= 12; month++) {
                    double value = agentLedgerInputValue(row, year, month);
                    row.setDisplayValue(month, value);
                    sectionTotals[month] += value;
                    if ("deduccion".equals(template.type())) {
                        deductionTotals[month] += value;
                    } else if ("withdrawal".equals(template.type())) {
                        withdrawalTotals[month] += value;
                    } else {
                        creditTotals[month] += value;
                    }
                }
            } else if ("TOTAL".equals(template.kind())) {
                for (int month = 1; month <= 12; month++) {
                    row.setDisplayValue(month, sectionTotals[month]);
                }
                sectionTotals = new double[13];
            } else if (template.label().equals("TOTAL CREDITS")) {
                for (int month = 1; month <= 12; month++) row.setDisplayValue(month, creditTotals[month]);
            } else if (template.label().equals("TOTAL DEDUCTIONS")) {
                for (int month = 1; month <= 12; month++) row.setDisplayValue(month, deductionTotals[month]);
            } else if (template.label().equals("TOTAL WITHDRAWALS")) {
                for (int month = 1; month <= 12; month++) row.setDisplayValue(month, withdrawalTotals[month]);
            } else if (template.label().startsWith("NET BALANCE")) {
                for (int month = 1; month <= 12; month++) {
                    row.setDisplayValue(month, creditTotals[month] - deductionTotals[month] - withdrawalTotals[month]);
                }
            }
        }
    }

    private double agentLedgerInputValue(AgentLedgerRow row, int year, int month) {
        double raw = row.rawValue(month);
        if (!"withdrawal".equals(row.template().type())) {
            return Math.abs(raw);
        }
        int rowMonth = agentLedgerMonthNumber(row.template().label());
        if (rowMonth != month) {
            return 0;
        }
        if (Math.abs(raw) >= 0.005) {
            return Math.abs(raw);
        }
        return nylMonthlyResultRepository.findPdfResult(year, month)
            .map(Math::abs)
            .orElse(0.0);
    }

    private String agentLedgerCellText(AgentLedgerRow row, int month) {
        if (!row.template().hasValues()) {
            return "";
        }
        double value = row.displayValue(month);
        return Math.abs(value) < 0.005 ? "-" : String.format(java.util.Locale.US, "%.2f", value);
    }

    private String agentLedgerYtdText(AgentLedgerRow row) {
        if (!row.template().hasValues()) {
            return "";
        }
        double total = 0;
        for (int month = 1; month <= 12; month++) {
            total += row.displayValue(month);
        }
        return Math.abs(total) < 0.005 ? "-" : String.format(java.util.Locale.US, "%.2f", total);
    }

    private void updateAgentLedgerCell(AgentLedgerRow row, int year, int month, String rawValue) {
        if (row == null || !row.template().editable()) {
            return;
        }
        String value = rawValue == null ? "" : rawValue.trim();
        List<NylRecord> records = new ArrayList<>(row.records(month));
        if (value.isBlank() || "-".equals(value)) {
            if (!records.isEmpty() && confirm("Eliminar registro", "Se eliminaran los registros de " + row.template().label() + " en " + monthName(month) + " " + year + ".", "Eliminar")) {
                records.stream().filter(record -> record.getId() > 0).forEach(record -> nylRepository.delete(record.getId()));
            }
            showAgentLedger();
            return;
        }
        double amount = normalizeAgentLedgerAmount(row.template(), Money.parse(value));
        if (records.isEmpty()) {
            String fingerprint = Fingerprint.of(year + "|" + month + "|" + row.template().label() + "|" + row.template().type() + "|" + amount + "|agent-ledger");
            NylRecord record = new NylRecord(0, year, month, row.template().label(), row.template().section(), row.template().type(), amount, "agent_ledger", fingerprint, "manual", false, false, "Agent Ledger");
            nylRepository.saveAll(List.of(record));
        } else {
            double current = records.stream().mapToDouble(NylRecord::getAmount).sum();
            NylRecord first = records.get(0);
            first.setAmount(first.getAmount() + (amount - current));
            first.setSection(row.template().section());
            first.setRecordType(row.template().type());
            nylRepository.updateRecord(first);
        }
        showAgentLedger();
    }

    private void deleteAgentLedgerRow(AgentLedgerRow row, int year) {
        List<NylRecord> records = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            records.addAll(row.records(month));
        }
        if (records.isEmpty()) {
            alert(Alert.AlertType.INFORMATION, "Agent Ledger", "No hay registros para eliminar en esta fila.");
            return;
        }
        if (!confirm("Eliminar fila", "Se eliminaran " + records.size() + " registros de '" + row.template().label() + "' en " + year + ".", "Eliminar")) {
            return;
        }
        records.stream().filter(record -> record.getId() > 0).forEach(record -> nylRepository.delete(record.getId()));
        showAgentLedger();
    }

    private void saveAgentLedgerRow(AgentLedgerRow row) {
        if (row == null || !row.template().editable()) {
            return;
        }
        for (int month = 1; month <= 12; month++) {
            for (NylRecord record : row.records(month)) {
                record.setSection(row.template().section());
                record.setRecordType(row.template().type());
                record.setAmount(normalizeAgentLedgerAmount(row.template(), record.getAmount()));
                if (record.getId() > 0) {
                    nylRepository.updateRecord(record);
                }
            }
        }
    }

    private void showAgentLedgerAddDialog(int year) {
        DatePicker date = new DatePicker(LocalDate.of(year, Math.max(1, selectedMonthValue == null ? 1 : selectedMonthValue), 1));
        TextField concept = new TextField();
        concept.setPromptText("Concepto");
        ComboBox<String> section = new ComboBox<>(FXCollections.observableArrayList(NYL_SECTION_OPTIONS));
        section.setValue("Creditos");
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("comision", "credito", "deduccion", "withdrawal", "ajuste", "otro"));
        type.setValue("credito");
        TextField amount = new TextField();
        amount.setPromptText("Importe");
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Fecha"), date);
        form.addRow(1, new Label("Concepto"), concept);
        form.addRow(2, new Label("Apartado"), section);
        form.addRow(3, new Label("Tipo"), type);
        form.addRow(4, new Label("Importe"), amount);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Añadir registro Agent Ledger");
        dialog.setHeaderText("Añadir registro Agent Ledger");
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(result -> {
            LocalDate value = date.getValue();
            AgentLedgerTemplate template = new AgentLedgerTemplate("INPUT", concept.getText(), section.getValue(), type.getValue(), "", true, true);
            double parsedAmount = normalizeAgentLedgerAmount(template, Money.parse(amount.getText()));
            String fingerprint = Fingerprint.of(value.getYear() + "|" + value.getMonthValue() + "|" + concept.getText() + "|" + type.getValue() + "|" + parsedAmount + "|agent-ledger-manual");
            NylRecord record = new NylRecord(0, value.getYear(), value.getMonthValue(), concept.getText(), section.getValue(), type.getValue(), parsedAmount, "agent_ledger", fingerprint, "manual", false, false, "Agent Ledger");
            nylRepository.saveAll(List.of(record));
            selectedYearValue = value.getYear();
            selectedMonthValue = value.getMonthValue();
            showAgentLedger();
        });
    }

    private double normalizeAgentLedgerAmount(AgentLedgerTemplate template, double amount) {
        if ("deduccion".equalsIgnoreCase(template.type()) || "withdrawal".equalsIgnoreCase(template.type())) {
            return -Math.abs(amount);
        }
        return Math.abs(amount);
    }

    private String agentLedgerKey(AgentLedgerTemplate template) {
        return agentLedgerKey(template.section(), template.label(), template.type());
    }

    private String agentLedgerKey(String section, String concept, String type) {
        return normalizeAgentLedgerToken(section) + "|" + normalizeAgentLedgerToken(concept) + "|" + normalizeAgentLedgerToken(type);
    }

    private String agentLedgerConceptTypeKey(String concept, String type) {
        return normalizeAgentLedgerToken(concept) + "|" + normalizeAgentLedgerToken(type);
    }

    private String normalizeAgentLedgerToken(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceAll("[^A-Za-z0-9]", "")
            .toLowerCase();
        return switch (normalized) {
            case "lifeannuitypersistencybonus" -> "lifeannuitypersistencybonus";
            case "telephone" -> "telephoneequipment";
            case "nylifeadmltc" -> "nylifeadmltcdeduction";
            case "401kloadreapyment" -> "401kloanrepayment";
            case "technologyexpense" -> "technologyexpenses";
            default -> normalized;
        };
    }

    private String agentLedgerMonthName(int month) {
        return switch (month) {
            case 1 -> "Jan";
            case 2 -> "Feb";
            case 3 -> "Mar";
            case 4 -> "Apr";
            case 5 -> "May";
            case 6 -> "Jun";
            case 7 -> "Jul";
            case 8 -> "Aug";
            case 9 -> "Sep";
            case 10 -> "Oct";
            case 11 -> "Nov";
            case 12 -> "Dec";
            default -> "";
        };
    }

    private int agentLedgerMonthNumber(String label) {
        return switch (normalizeAgentLedgerToken(label)) {
            case "jan" -> 1;
            case "feb" -> 2;
            case "mar" -> 3;
            case "apr" -> 4;
            case "may" -> 5;
            case "jun" -> 6;
            case "jul" -> 7;
            case "aug" -> 8;
            case "sep" -> 9;
            case "oct" -> 10;
            case "nov" -> 11;
            case "dec" -> 12;
            default -> 0;
        };
    }

    private void showReports() {
        showReportsHub();
        if (System.currentTimeMillis() >= 0) {
            return;
        }
        Button export = new Button("Exportar Excel completo");
        export.getStyleClass().add("primary");
        export.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Guardar reporte Excel");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
            chooser.setInitialFileName("silveira-reportes.xlsx");
            File file = chooser.showSaveDialog(root.getScene().getWindow());
            if (file == null) {
                return;
            }
            excelExportService.export(file.toPath(),
                bankModule.accountDetails().findRows(selectedYear(), selectedMonth(), null, null, null),
                nylRepository.find(selectedYear(), selectedMonth(), null, null),
                reconciliationService.preview(selectedYear(), selectedMonth()));
            alert(Alert.AlertType.INFORMATION, "Reporte exportado", file.getAbsolutePath());
        });
        setPage(page("Reportes", filters(() -> {}), export));
    }

    private void showReportsHub() {
        Runnable refresh = () -> {};
        Button complete = new Button("Exportar completo");
        complete.getStyleClass().add("primary");
        complete.setOnAction(event -> exportCompleteReport());
        Button monthlyPackage = new Button("Paquete contable mensual");
        monthlyPackage.getStyleClass().add("primary");
        monthlyPackage.setOnAction(event -> exportMonthlyReconciliation());
        Button bankMonthly = new Button("Banco mensual");
        bankMonthly.setOnAction(event -> exportAllBankMonthly());
        Button cardsMonthly = new Button("Tarjetas mensual");
        cardsMonthly.setOnAction(event -> exportAllCardsMonthly());
        Button nylMonthly = new Button("NYL mensual");
        nylMonthly.setOnAction(event -> {
            int year = selectedYear() == null ? LocalDate.now().getYear() : selectedYear();
            int month = selectedMonth() == null ? LocalDate.now().getMonthValue() : selectedMonth();
            exportMonthlyNyl(year, month);
        });
        Button reconciliationMonthly = new Button("Conciliación mensual");
        reconciliationMonthly.setOnAction(event -> exportMonthlyReconciliation());

        VBox exports = new VBox(14,
            dashboardSection("Paquetes", "Exportaciones completas para cierre o archivo contable", "reconciliation-section",
                textCard("Completo", "Banco + NYL + Conciliación", "neutral-total"),
                textCard("Mensual", "Banco + Tarjetas + NYL + Cruces", "neutral-total")),
            new HBox(10, complete, monthlyPackage),
            dashboardSection("Por fuente", "Descargas separadas del mes seleccionado", "bank-section",
                textCard("Banco", "Todas las cuentas", "bank-card"),
                textCard("Tarjetas", "Todas las tarjetas", "nyl-card"),
                textCard("NYL", "Resumen mensual", "nyl-card")),
            new HBox(10, bankMonthly, cardsMonthly, nylMonthly, reconciliationMonthly)
        );
        setPage(page("Reportes", filters(refresh), exports));
    }

    private void exportCompleteReport() {
        File file = chooseExcel("silveira-reportes.xlsx");
        if (file == null) {
            return;
        }
        excelExportService.export(file.toPath(),
            bankModule.accountDetails().findRows(selectedYear(), selectedMonth(), null, null, null),
            nylRepository.find(selectedYear(), selectedMonth(), null, null),
            reconciliationService.preview(selectedYear(), selectedMonth()));
        alert(Alert.AlertType.INFORMATION, "Reporte exportado", file.getAbsolutePath());
    }

    private void exportAllBankMonthly() {
        int year = selectedYear() == null ? LocalDate.now().getYear() : selectedYear();
        int month = selectedMonth() == null ? LocalDate.now().getMonthValue() : selectedMonth();
        bankShellWorkflow().exportAllMonthly(year, month);
    }

    private void exportAllCardsMonthly() {
        int year = selectedYear() == null ? LocalDate.now().getYear() : selectedYear();
        int month = selectedMonth() == null ? LocalDate.now().getMonthValue() : selectedMonth();
        File file = chooseExcel("tarjetas-todas-" + year + "-" + String.format("%02d", month) + ".xlsx");
        if (file == null) {
            return;
        }
        excelExportService.exportCreditCardMonthly(file.toPath(), allCardStatements(year, month), allCardTransactions(year, month));
        alert(Alert.AlertType.INFORMATION, "Tarjetas exportadas", file.getAbsolutePath());
    }

    private void importNyl(Runnable refresh) {
        File file = choosePdf();
        if (file == null) {
            return;
        }
        List<NylRecord> parsed;
        String extractedText = "";
        try {
            parsed = nylParser.parse(file.toPath());
            extractedText = new com.silveira.accounting.parsers.PdfTextExtractor().extract(file.toPath());
        } catch (RuntimeException exception) {
            boolean runOcr = confirm(
                "No se pudo leer el PDF",
                exception.getMessage() + "\n\nEl archivo parece ser un PDF escaneado. La app puede intentar OCR para prellenar datos, pero todo quedara marcado para revision y nada se guardara sin confirmar.",
                "Intentar OCR"
            );
            if (!runOcr) {
                return;
            }
            processNylOcr(file.toPath(), refresh);
            return;
        }
        showNylReview(parsed, extractedText, refresh);
    }

    private void processNylOcr(Path pdf, Runnable refresh) {
        showProcessing("Procesando OCR", "Leyendo el PDF escaneado y corrigiendo orientacion. Esto puede tardar unos minutos.");
        Task<NylImportPreview> task = new Task<>() {
            @Override
            protected NylImportPreview call() {
                OcrService.OcrResult ocr = ocrService.extractText(pdf);
                List<NylRecord> records = nylParser.parseText(ocr.text(), pdf.getFileName().toString(), "ocr_revisado", true);
                return new NylImportPreview(records, ocr.text());
            }
        };
        task.setOnSucceeded(event -> showNylReview(task.getValue().records(), task.getValue().text(), refresh));
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            boolean openManual = confirm(
                "OCR no disponible",
                error.getMessage() + "\n\nPuedes cargarlo manualmente para no detener el cierre contable.",
                "Abrir entrada manual"
            );
            if (openManual) {
                manualNyl(refresh);
            } else {
                showNyl();
            }
        });
        Thread thread = new Thread(task, "silveira-nyl-ocr");
        thread.setDaemon(true);
        thread.start();
    }

    private void showNylReview(List<NylRecord> parsed, String extractedText, Runnable refresh) {
        Set<String> existing = nylRepository.existingFingerprints(parsed);
        List<NylRecord> newRecords = parsed.stream()
            .filter(record -> !existing.contains(record.getFingerprint()))
            .sorted(Comparator.comparing(NylRecord::getSection)
                .thenComparingInt(NylRecord::getYear)
                .thenComparingInt(NylRecord::getMonth)
                .thenComparing(NylRecord::getConcept))
            .toList();
        TableView<NylRecord> review = nylTable();
        review.setItems(FXCollections.observableArrayList(newRecords));
        double declaredTotal = existing.isEmpty() ? nylParser.detectDeclaredTotal(extractedText) : Double.NaN;
        List<String> warnings = importValidationService.validateNyl(newRecords, declaredTotal);
        if (!existing.isEmpty()) {
            warnings.add(existing.size() + " registros ya existian y se ocultaron de esta revision.");
        }
        VBox warningBox = warningBox(warnings);
        HBox monthLegend = monthLegend(newRecords);
        VBox reviewSummary = reviewSummaryBox(newRecords, existing.size());
        Button addMissing = new Button("Añadir registro faltante");
        addMissing.setOnAction(event -> addMissingNylRow(review));
        Button showExisting = new Button("Ver ya guardados");
        showExisting.setDisable(existing.isEmpty());
        showExisting.setOnAction(event -> showExistingNylRecords(existing, newRecords, extractedText, refresh));
        Button savePending = new Button("Guardar progreso");
        savePending.getStyleClass().add("primary");
        savePending.setOnAction(event -> saveNylRows(List.copyOf(review.getItems()), review, List.of(), refresh, true));
        Button saveReviewed = new Button("Guardar revisados");
        saveReviewed.getStyleClass().add("primary");
        saveReviewed.setOnAction(event -> saveProgressNylRows(review, warnings, refresh));
        showReview("Revisión New York Life", review, () -> {
            saveAllReviewedNylRows(review, warnings, refresh);
        }, new VBox(10, warningBox, reviewSummary, monthNavigator(newRecords, review), monthLegend, new HBox(10, addMissing, showExisting, savePending, saveReviewed)));
    }

    private void showExistingNylRecords(Set<String> fingerprints, List<NylRecord> newRecords, String extractedText, Runnable refresh) {
        TableView<NylRecord> table = nylTable();
        table.setItems(FXCollections.observableArrayList(nylRepository.findByFingerprints(fingerprints)));
        setPage(page("NYL ya guardados", backButton("Volver a revisión NYL", () -> showNylReview(newRecords, extractedText, refresh)),
            new Label("Estos registros ya existian y por eso se ocultaron de la nueva importacion."), table));
    }

    private HBox monthNavigator(List<NylRecord> records, TableView<NylRecord> table) {
        HBox box = new HBox(8);
        box.getStyleClass().add("month-legend");
        records.stream()
            .map(NylRecord::getMonth)
            .distinct()
            .sorted()
            .forEach(month -> {
                Button button = new Button(monthName(month));
                button.getStyleClass().addAll("month-chip", "month-" + month);
                button.setOnAction(event -> {
                    for (NylRecord record : table.getItems()) {
                        if (record.getMonth() == month) {
                            table.scrollTo(record);
                            table.getSelectionModel().select(record);
                            break;
                        }
                    }
                });
                box.getChildren().add(button);
            });
        return box;
    }

    private void showPendingNylReview(Runnable refresh) {
        List<NylRecord> pending = nylRepository.findPendingReview();
        TableView<NylRecord> table = nylTable();
        table.setItems(FXCollections.observableArrayList(pending));
        VBox summary = reviewSummaryBox(pending, 0);
        Button saveReviewed = new Button("Guardar cambios de revisión");
        saveReviewed.getStyleClass().add("primary");
        saveReviewed.setOnAction(event -> {
            for (NylRecord record : table.getItems()) {
                if (record.getId() > 0) {
                    nylRepository.updateRecord(record);
                }
            }
            refresh.run();
            showNyl();
        });
        setPage(page("NYL pendientes por revisar", backButton("Volver a New York Life", this::showNyl), summary, table, saveReviewed));
    }

    private void saveVisibleNylRows(TableView<NylRecord> table, Runnable refresh, boolean reviewedOnly) {
        List<NylRecord> rows = table.getItems().stream()
            .filter(record -> !reviewedOnly || !record.isPendingReview())
            .toList();
        if (rows.isEmpty()) {
            alert(Alert.AlertType.INFORMATION, "Nada para guardar", reviewedOnly
                ? "No hay filas marcadas como revisadas en esta vista."
                : "No hay filas visibles para guardar.");
            return;
        }
        for (NylRecord record : rows) {
            if (record.getId() > 0) {
                nylRepository.updateRecord(record);
            }
        }
        List<NylRecord> newRows = rows.stream()
            .filter(record -> record.getId() == 0)
            .toList();
        if (!newRows.isEmpty()) {
            nylRepository.saveAll(newRows);
        }
        refresh.run();
        alert(Alert.AlertType.INFORMATION, "NYL guardado", rows.size() + " registros actualizados.");
    }

    private void saveProgressNylRows(TableView<NylRecord> table, List<String> warnings, Runnable refresh) {
        if (table.getItems().isEmpty()) {
            alert(Alert.AlertType.INFORMATION, "Nada para guardar", "No hay filas en la revision.");
            return;
        }
        saveNylRows(List.copyOf(table.getItems()), table, warnings, refresh, true);
    }

    private void saveAllReviewedNylRows(TableView<NylRecord> table, List<String> warnings, Runnable refresh) {
        boolean pending = table.getItems().stream().anyMatch(NylRecord::isReviewRequired);
        if (pending) {
            boolean proceed = confirm(
                "Quedan filas sin revisar",
                "Todavia hay filas marcadas como Revisar. Puedes guardar solo las revisadas o confirmar todo cuando termines.",
                "Guardar todo igualmente"
            );
            if (!proceed) {
                return;
            }
        }
        saveNylRows(List.copyOf(table.getItems()), table, warnings, refresh, false);
    }

    private void saveNylRows(List<NylRecord> rows, TableView<NylRecord> table, List<String> warnings, Runnable refresh, boolean keepPending) {
        if (!keepPending && !warnings.isEmpty()) {
            boolean proceed = confirm(
                "Guardar con alertas",
                "Hay alertas de revision en esta importacion.\n\n" + String.join("\n", warnings) + "\n\nConfirma solo si ya revisaste los datos contra el PDF original.",
                "Guardar revisado"
            );
            if (!proceed) {
                return;
            }
        }
        for (NylRecord row : rows) {
            if (keepPending) {
                boolean remainsPending = row.isPendingReview();
                row.setPendingReview(remainsPending);
                row.setReviewRequired(remainsPending);
            }
        }
        NylRecordRepository.SaveResult result = nylRepository.saveAll(rows);
        if (!result.newConcepts().isEmpty()) {
            alert(Alert.AlertType.WARNING, "Conceptos nuevos", String.join(", ", result.newConcepts()));
        }
        table.getItems().removeAll(rows);
        rememberNylPeriod(rows);
        refresh.run();
        if (keepPending) {
            long pending = rows.stream().filter(NylRecord::isPendingReview).count();
            long reviewed = rows.size() - pending;
            alert(Alert.AlertType.INFORMATION, "Progreso guardado", reviewed + " revisados y " + pending + " pendientes quedaron guardados.");
            showNyl();
        } else {
            showNylImportTotals(rows, result.inserted());
        }
    }

    private void rememberNylPeriod(List<NylRecord> rows) {
        rows.stream()
            .max(Comparator.comparingInt(NylRecord::getYear).thenComparingInt(NylRecord::getMonth))
            .ifPresent(record -> {
                selectedYearValue = record.getYear();
                selectedMonthValue = record.getMonth();
            });
    }

    private void manualNyl(Runnable refresh) {
        DatePicker date = new DatePicker(LocalDate.now());
        TextField concept = new TextField();
        concept.setPromptText("Concepto");
        ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("comision", "credito", "deduccion", "withdrawal", "ajuste", "otro"));
        type.setValue("comision");
        TextField amount = new TextField();
        amount.setPromptText("Importe");
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Fecha"), date);
        form.addRow(1, new Label("Concepto"), concept);
        form.addRow(2, new Label("Tipo"), type);
        form.addRow(3, new Label("Importe"), amount);
        Button save = new Button("Guardar entrada manual");
        save.getStyleClass().add("primary");
        save.setOnAction(event -> {
            LocalDate value = date.getValue();
            double parsedAmount = Money.parse(amount.getText());
            String fingerprint = Fingerprint.of(value.getYear() + "|" + value.getMonthValue() + "|" + concept.getText() + "|" + type.getValue() + "|" + parsedAmount);
            NylRecord record = new NylRecord(0, value.getYear(), value.getMonthValue(), concept.getText(), type.getValue(), parsedAmount, "manual", fingerprint, "manual", false, "");
            NylRecordRepository.SaveResult result = nylRepository.saveAll(List.of(record));
            alert(Alert.AlertType.INFORMATION, "Entrada manual", result.inserted() + " registro guardado.");
            selectedYearValue = value.getYear();
            selectedMonthValue = value.getMonthValue();
            refresh.run();
            showNyl();
        });
        setPage(page("Entrada manual NYL", backButton("Volver a New York Life", this::showNyl), form, save));
    }

    private void showReview(String title, TableView<?> table, Runnable confirm) {
        showReview(title, table, confirm, null);
    }

    private void showReview(String title, TableView<?> table, Runnable confirm, javafx.scene.Node warningNode) {
        Button save = new Button("Confirmar y guardar");
        save.getStyleClass().add("primary");
        save.setOnAction(event -> confirm.run());
        Button cancel = new Button("Cancelar");
        cancel.setOnAction(event -> showDashboard());
        VBox.setVgrow(table, Priority.ALWAYS);
        if (warningNode == null) {
            setPage(page(title, new Label("Revise y corrija los datos antes de guardar."), table, new HBox(10, save, cancel)));
        } else {
            setPage(page(title, new Label("Revise y corrija los datos antes de guardar."), warningNode, table, new HBox(10, save, cancel)));
        }
    }

    private VBox warningBox(List<String> warnings) {
        VBox box = new VBox(4);
        if (warnings.isEmpty()) {
            Label ok = new Label("Validacion inicial sin alertas. Revise igualmente antes de guardar.");
            ok.getStyleClass().add("review-ok");
            box.getChildren().add(ok);
            return box;
        }
        box.getStyleClass().add("review-warning");
        Label title = new Label("Alertas de revision");
        title.getStyleClass().add("review-warning-title");
        box.getChildren().add(title);
        for (String warning : warnings) {
            box.getChildren().add(new Label(warning));
        }
        return box;
    }

    private HBox monthLegend(List<NylRecord> records) {
        HBox legend = new HBox(8);
        legend.getStyleClass().add("month-legend");
        records.stream()
            .map(NylRecord::getMonth)
            .distinct()
            .sorted()
            .forEach(month -> {
                Label label = new Label(monthName(month));
                label.getStyleClass().addAll("month-chip", "month-" + month);
                legend.getChildren().add(label);
            });
        return legend;
    }

    private VBox monthlyTotalsBox(List<NylRecord> records) {
        Label title = new Label("Totales por apartado y mes");
        title.getStyleClass().add("section-title");
        if (records.isEmpty()) {
            Label empty = new Label("No hay registros nuevos para totalizar.");
            empty.getStyleClass().add("section-subtitle");
            VBox box = new VBox(6, title, empty);
            box.getStyleClass().add("totals-box");
            return box;
        }

        Set<Integer> months = records.stream()
            .map(NylRecord::getMonth)
            .collect(Collectors.toCollection(TreeSet::new));
        Map<String, Map<Integer, Double>> totals = records.stream()
            .collect(Collectors.groupingBy(NylRecord::getSection, LinkedHashMap::new,
                Collectors.groupingBy(NylRecord::getMonth, Collectors.summingDouble(NylRecord::getAmount))));

        GridPane grid = new GridPane();
        grid.getStyleClass().add("totals-grid");
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(headerLabel("Apartado"), 0, 0);
        int column = 1;
        for (Integer month : months) {
            grid.add(headerLabel(monthName(month)), column++, 0);
        }
        grid.add(headerLabel("Total"), column, 0);

        int row = 1;
        for (Map.Entry<String, Map<Integer, Double>> section : totals.entrySet()) {
            grid.add(bodyLabel(section.getKey()), 0, row);
            double sectionTotal = 0;
            column = 1;
            for (Integer month : months) {
                double value = section.getValue().getOrDefault(month, 0.0);
                sectionTotal += value;
                grid.add(moneyLabel(value), column++, row);
            }
            grid.add(moneyLabel(sectionTotal), column, row);
            row++;
        }

        VBox box = new VBox(10, title, grid);
        box.getStyleClass().add("totals-box");
        return box;
    }

    private HBox totalsPanel(String source, SourceTotals totals) {
        HBox panel = new HBox(12,
            miniTotal("OK", String.valueOf(totals.reviewedCount()), "neutral-total"),
            miniTotal("Pendientes", String.valueOf(totals.pendingCount()), "pending-total"),
            miniTotal("Ingresos OK", Money.format(totals.income()), "income-total"),
            miniTotal("Salidas OK", Money.format(totals.expenses()), "expense-total"),
            miniTotal("Neto OK", Money.format(totals.net()), "net-total")
        );
        panel.getStyleClass().add("totals-panel");
        return panel;
    }

    private VBox monthlyNylCards(TableView<NylRecord> table, HBox totalsPanel) {
        Label title = new Label("Resumen mensual NYL");
        title.getStyleClass().add("section-title");
        HBox cards = new HBox(12);
        cards.getStyleClass().add("monthly-card-row");

        VBox general = monthlyActionCard("General", "Ver todos", "", "", "", () -> {
            selectedMonthValue = null;
            table.setItems(FXCollections.observableArrayList(nylRepository.find(selectedYear(), null, null, null)));
            totalsPanel.getChildren().setAll(nylTotalsNodes(nylRepository.totals(selectedYear(), null)));
        });
        general.getStyleClass().add("monthly-card-general");
        cards.getChildren().add(general);

        for (MonthlySourceTotals total : nylRepository.monthlyTotals(null)) {
            VBox card = monthlyActionCard(
                monthName(total.month()) + " " + total.year(),
                "",
                "",
                "",
                "",
                () -> {
                    selectedYearValue = total.year();
                    selectedMonthValue = total.month();
                    table.setItems(FXCollections.observableArrayList(nylRepository.find(total.year(), total.month(), null, null)));
                    totalsPanel.getChildren().setAll(nylTotalsNodes(nylRepository.totals(total.year(), total.month())));
                }
            );
            addReviewMark(card, "nyl", "", total.year(), total.month());
            addMonthlyCardLine(card, "Comisiones: " + Money.format(total.credits()));
            if (total.year() == 2026 && total.month() >= 1 && total.month() <= 4) {
                addNylDeductionBreakdown(card, total.year(), total.month());
                addMonthlyCardLine(card, "Total deducciones: " + Money.format(total.deductions()), "monthly-card-value-strong");
            } else {
                addMonthlyCardLine(card, "Deducciones: " + Money.format(total.deductions()));
            }
            addMonthlyCardLine(card, "Resultado: " + Money.format(total.net()));
            Optional<Double> pdfResult = nylMonthlyResultRepository.findPdfResult(total.year(), total.month());
            Optional<Double> nylBank = nylMonthlyResultRepository.findNylBank(total.year(), total.month());
            addMonthlyCardLine(card, "Dif. PDF vs NYL: " + pdfResult.map(value -> Money.format(value - total.net())).orElse("-"), "monthly-card-value-danger");
            addMonthlyCardDivider(card);
            addMonthlyCardLine(card, "Reporte-NYL: " + pdfResult.map(Money::format).orElse("Pendiente"), "monthly-card-value-strong");
            addMonthlyCardLine(card, "Ingreso NYL Banco: " + nylBank.map(Money::format).orElse("Pendiente"), "monthly-card-value-strong");
            addMonthlyCardLine(card, "Dif. Reporte vs Banco: " + pdfResult.flatMap(report -> nylBank.map(bank -> Money.format(bank - report))).orElse("-"), "monthly-card-value-danger");
            addMonthlyCardLine(card, "Pendientes: " + total.pendingCount());
            Button editPdfResult = new Button("Editar");
            editPdfResult.setOnAction(event -> {
                event.consume();
                showNylPdfResultDialog(total.year(), total.month(), total.net(), () -> {
                    table.setItems(FXCollections.observableArrayList(nylRepository.find(total.year(), total.month(), null, null)));
                    totalsPanel.getChildren().setAll(nylTotalsNodes(nylRepository.totals(total.year(), total.month())));
                });
            });
            HBox cardActions = new HBox(8, editPdfResult, monthlyExportButton(() -> exportMonthlyNyl(total.year(), total.month())));
            cardActions.getStyleClass().add("bank-monthly-actions");
            card.getChildren().add(cardActions);
            card.getStyleClass().add("monthly-card");
            cards.getChildren().add(card);
        }

        VBox box = new VBox(10, title, horizontalStatementScroll(cards));
        box.getStyleClass().add("monthly-section");
        return box;
    }

    private void addNylDeductionBreakdown(VBox card, int year, int month) {
        Map<String, Double> totals = new LinkedHashMap<>();
        for (String section : NYL_DEDUCTION_SECTION_ORDER) {
            totals.put(section, 0.0);
        }
        for (NylRecord record : nylRepository.find(year, month, null, null)) {
            if (record.isPendingReview() || !"deduccion".equalsIgnoreCase(record.getRecordType())) {
                continue;
            }
            String section = record.getSection() == null || record.getSection().isBlank() ? "Other Deductions" : record.getSection();
            if (!totals.containsKey(section)) {
                section = "Other Deductions";
            }
            totals.put(section, totals.get(section) + Math.abs(record.getAmount()));
        }
        totals.entrySet().stream()
            .filter(entry -> Math.abs(entry.getValue()) > 0.004)
            .forEach(entry -> addMonthlyCardLine(card, entry.getKey() + ": " + Money.format(entry.getValue())));
    }

    private List<javafx.scene.Node> nylTotalsNodes(SourceTotals totals) {
        return List.of(
            miniTotal("Comisiones", Money.format(totals.income()), "income-total"),
            miniTotal("Deducciones", Money.format(Math.abs(totals.expenses())), "expense-total"),
            nylFlowChart(totals)
        );
    }

    private void showNylPdfResultDialog(int year, int month, double calculatedResult, Runnable refresh) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar datos NYL");
        dialog.setHeaderText(monthName(month) + " " + year);
        TextField pdfResult = new TextField(nylMonthlyResultRepository.findPdfResult(year, month)
            .map(Money::format)
            .orElse(""));
        TextField nylBank = new TextField(nylMonthlyResultRepository.findNylBank(year, month)
            .map(Money::format)
            .orElse(""));
        Label calculated = new Label("Resultado calculado: " + Money.format(calculatedResult));
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.addRow(0, new Label("Reporte-NYL"), pdfResult);
        form.addRow(1, new Label("NYL-Banco"), nylBank);
        form.addRow(2, calculated);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(result -> {
            try {
                nylMonthlyResultRepository.savePdfResult(year, month, Money.parse(pdfResult.getText()));
                nylMonthlyResultRepository.saveNylBank(year, month, Money.parse(nylBank.getText()));
                selectedYearValue = year;
                selectedMonthValue = month;
                refresh.run();
                showNyl();
            } catch (NumberFormatException exception) {
                alert(Alert.AlertType.ERROR, "Importe no válido", "Revisa Reporte-NYL y NYL-Banco. Usa formato 1234.56 o $1,234.56.");
            }
        });
    }

    private VBox nylFlowChart(SourceTotals totals) {
        double commissions = Math.max(0, totals.income());
        double deductions = Math.abs(Math.min(0, totals.expenses()));
        double max = Math.max(Math.max(commissions, deductions), 1.0);
        VBox chart = new VBox(8,
            bankFlowHeader(),
            bankFlowBar("Comisiones", commissions, max, "bank-flow-income-fill"),
            bankFlowBar("Deducciones", deductions, max, "bank-flow-expense-fill")
        );
        chart.getStyleClass().add("bank-flow-chart");
        HBox.setHgrow(chart, Priority.ALWAYS);
        return chart;
    }

    private Label bankFlowHeader() {
        Label label = new Label("Movimiento del periodo");
        label.getStyleClass().add("bank-flow-title");
        return label;
    }

    private HBox bankFlowBar(String title, double amount, double max, String fillStyle) {
        Label name = new Label(title);
        name.getStyleClass().add("bank-flow-label");
        StackPane track = new StackPane();
        track.getStyleClass().add("bank-flow-track");
        Region fill = new Region();
        fill.getStyleClass().add(fillStyle);
        double width = 220 * (amount / max);
        fill.setMinWidth(width);
        fill.setPrefWidth(width);
        fill.setMaxWidth(width);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        track.getChildren().add(fill);
        HBox.setHgrow(track, Priority.ALWAYS);
        Label value = new Label(Money.format(amount));
        value.getStyleClass().add("bank-flow-value");
        HBox row = new HBox(10, name, track, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private List<javafx.scene.Node> cardTotalsNodes(SourceTotals totals) {
        return List.of(
            miniTotal("OK", String.valueOf(totals.reviewedCount()), "neutral-total"),
            miniTotal("Pendientes", String.valueOf(totals.pendingCount()), "pending-total"),
            miniTotal("Deuda OK", Money.format(totals.income()), "income-total"),
            miniTotal("Pago mínimo OK", Money.format(totals.expenses()), "pending-total"),
            miniTotal("Intereses OK", Money.format(totals.net()), "expense-total")
        );
    }

    private SourceTotals totalsFromCardStatements(List<CreditCardStatement> statements) {
        int reviewed = (int) statements.stream().filter(statement -> !statement.isPendingReview()).count();
        int pending = (int) statements.stream().filter(CreditCardStatement::isPendingReview).count();
        double debt = statements.stream().filter(statement -> !statement.isPendingReview()).mapToDouble(CreditCardStatement::getNewBalance).sum();
        double minimum = statements.stream().filter(statement -> !statement.isPendingReview()).mapToDouble(CreditCardStatement::getMinimumPaymentDue).sum();
        double interest = statements.stream().filter(statement -> !statement.isPendingReview()).mapToDouble(CreditCardStatement::getInterestCharged).sum();
        return new SourceTotals(reviewed, pending, debt, minimum, interest);
    }

    private List<javafx.scene.Node> cardAccumulatedTotalsNodes(String alias, Integer year, Integer throughMonth) {
        List<CreditCardStatement> statements = creditCardStatementRepository.findByAccount(alias, year, null).stream()
            .filter(statement -> throughMonth == null || (statement.getStatementEndDate() != null && statement.getStatementEndDate().getMonthValue() == throughMonth))
            .toList();
        return cardPeriodActivityTotalsNodes(statements);
    }

    private List<javafx.scene.Node> cardPeriodActivityTotalsNodes(List<CreditCardStatement> statements) {
        List<CreditCardStatement> reviewed = statements.stream().filter(statement -> !statement.isPendingReview()).toList();
        List<CreditCardStatement> source = reviewed.isEmpty() ? statements : reviewed;
        double payments = source.stream().mapToDouble(CreditCardStatement::getPayments).sum();
        double purchases = source.stream().mapToDouble(CreditCardStatement::getTransactions).sum();
        double interest = source.stream().mapToDouble(CreditCardStatement::getInterestCharged).sum();
        return List.of(
            miniTotal("Pagos (al Banco)", Money.format(payments), "income-total"),
            miniTotal("Compras", Money.format(purchases), "expense-total"),
            miniTotal("Intereses", Money.format(interest), "urgent-total")
        );
    }

    private CreditCardStatement nextDueStatement(List<CreditCardStatement> statements) {
        return statements.stream()
            .filter(statement -> statement.getPaymentDueDate() != null)
            .min(Comparator.comparing(CreditCardStatement::getPaymentDueDate))
            .orElse(statements.isEmpty() ? null : statements.get(0));
    }

    private List<javafx.scene.Node> mortgageTotalsNodes(List<MortgageStatement> statements, List<MortgageTransaction> movements) {
        List<MortgageStatement> source = mortgageReviewedOrAll(statements);
        MortgageStatement latest = source.stream()
            .max(Comparator
                .comparing(MortgageStatement::getStatementDate, Comparator.nullsFirst(LocalDate::compareTo))
                .thenComparingLong(MortgageStatement::getId))
            .orElse(null);
        double debtPaid = source.stream().mapToDouble(MortgageStatement::getPastPaidPrincipalSinceLastStatement).sum();
        double outstandingDebt = latest == null ? 0 : latest.getOutstandingPrincipalBalance();
        return List.of(
            miniTotal("Initial Debt", Money.format(mortgageInitialDebt(source)), "neutral-total"),
            miniTotal("Debt Paid", Money.format(debtPaid), "income-total"),
            miniTotal("Outstanding Debt", Money.format(outstandingDebt), "expense-total")
        );
    }

    private List<javafx.scene.Node> mortgageAnalysisTotalsNodes(List<MortgageStatement> statements) {
        List<javafx.scene.Node> totals = new ArrayList<>(mortgageTotalsNodes(statements, List.of()));
        totals.add(miniTotal("Interest", Money.format(mortgagePaidInterest(statements)), "expense-total"));
        totals.add(miniTotal("Escrow", Money.format(mortgagePaidEscrow(statements)), "pending-total"));
        return totals;
    }

    private List<MortgageStatement> mortgageReviewedOrAll(List<MortgageStatement> statements) {
        List<MortgageStatement> reviewed = statements.stream().filter(s -> !s.isPendingReview()).toList();
        return reviewed.isEmpty() ? statements : reviewed;
    }

    private double mortgageInitialDebt(List<MortgageStatement> statements) {
        return statements.stream()
            .max(Comparator
                .comparing(MortgageStatement::getStatementDate, Comparator.nullsFirst(LocalDate::compareTo))
                .thenComparingLong(MortgageStatement::getId))
            .map(MortgageStatement::getOriginalPrincipalBalance)
            .orElse(0.0);
    }

    private VBox monthlyMortgageCards(String alias, TableView<MortgageStatement> table, TableView<MortgageTransaction> movementTable, HBox totalsPanel, VBox statementSummaries) {
        Label title = new Label("Resumen mensual Hipoteca");
        title.getStyleClass().add("section-title");
        HBox cards = new HBox(12);
        cards.getStyleClass().add("monthly-card-row");

        VBox general = monthlyActionCard("General", "Ver todos", "", "", "", () -> {
            table.setItems(FXCollections.observableArrayList(mortgageStatementRepository.findByLoan(alias, selectedYear(), null)));
            movementTable.setItems(FXCollections.observableArrayList(mortgageTransactionRepository.findByLoan(alias, selectedYear(), null)));
            totalsPanel.getChildren().setAll(mortgageTotalsNodes(table.getItems(), movementTable.getItems()));
            refreshMortgageStatementSummaries(table, statementSummaries, () -> totalsPanel.getChildren().setAll(mortgageTotalsNodes(table.getItems(), movementTable.getItems())));
        });
        general.getStyleClass().add("monthly-card-general");
        cards.getChildren().add(general);

        for (MonthlySourceTotals total : mortgageStatementRepository.monthlyTotals(alias, null)) {
            List<MortgageStatement> monthStatements = mortgageStatementRepository.findByLoan(alias, total.year(), total.month());
            VBox card = monthlyActionCard(
                monthName(total.month()) + " " + total.year(),
                "Principal: " + Money.format(mortgagePaidPrincipal(monthStatements)),
                "Interest: " + Money.format(mortgagePaidInterest(monthStatements)),
                "Escrow: " + Money.format(mortgagePaidEscrow(monthStatements)),
                "Deuda pendiente: " + Money.format(mortgageOutstandingPrincipal(monthStatements)),
                () -> {
                    table.setItems(FXCollections.observableArrayList(mortgageStatementRepository.findByLoan(alias, total.year(), total.month())));
                    movementTable.setItems(FXCollections.observableArrayList(mortgageTransactionRepository.findByLoan(alias, total.year(), total.month())));
                    totalsPanel.getChildren().setAll(mortgageTotalsNodes(table.getItems(), movementTable.getItems()));
                    refreshMortgageStatementSummaries(table, statementSummaries, () -> totalsPanel.getChildren().setAll(mortgageTotalsNodes(table.getItems(), movementTable.getItems())));
                }
            );
            addReviewMark(card, "mortgage", alias, total.year(), total.month());
            card.getChildren().add(monthlyExportButton(() -> exportMonthlyMortgage(alias, total.year(), total.month())));
            card.getStyleClass().add("monthly-card");
            cards.getChildren().add(card);
        }

        VBox box = new VBox(10, title, cards);
        box.getStyleClass().add("monthly-section");
        return box;
    }

    private double mortgagePaidPrincipal(List<MortgageStatement> statements) {
        return statements.stream()
            .mapToDouble(MortgageStatement::getPastPaidPrincipalSinceLastStatement)
            .sum();
    }

    private double mortgagePaidInterest(List<MortgageStatement> statements) {
        return statements.stream()
            .mapToDouble(MortgageStatement::getPastPaidInterestSinceLastStatement)
            .sum();
    }

    private double mortgagePaidEscrow(List<MortgageStatement> statements) {
        return statements.stream()
            .mapToDouble(MortgageStatement::getPastPaidEscrowSinceLastStatement)
            .sum();
    }

    private double mortgageOutstandingPrincipal(List<MortgageStatement> statements) {
        return statements.stream()
            .max(Comparator
                .comparing(MortgageStatement::getStatementDate, Comparator.nullsFirst(LocalDate::compareTo))
                .thenComparingLong(MortgageStatement::getId))
            .map(MortgageStatement::getOutstandingPrincipalBalance)
            .orElse(0.0);
    }

    private void exportMonthlyMortgage(String alias, int year, int month) {
        File target = chooseExcel("hipoteca_" + alias + "_" + year + "_" + String.format("%02d", month) + ".xlsx");
        if (target == null) {
            return;
        }
        excelExportService.exportMortgageMonthly(
            target.toPath(),
            mortgageStatementRepository.findByLoan(alias, year, month),
            mortgageTransactionRepository.findByLoan(alias, year, month)
        );
        alert(Alert.AlertType.INFORMATION, "Exportacion lista", "Se descargo el resumen mensual de hipoteca.");
    }

    private VBox monthlyCardCards(String alias, TableView<CreditCardStatement> table, HBox totalsPanel, VBox statementCards) {
        Label title = new Label("Resumen mensual Tarjeta");
        title.getStyleClass().add("section-title");
        FlowPane cards = new FlowPane(12, 12);
        cards.getStyleClass().add("monthly-card-row");

        for (MonthlySourceTotals total : creditCardStatementRepository.monthlyTotals(alias, null)) {
            List<CreditCardStatement> allMonthlyStatements = creditCardStatementRepository.findByAccount(alias, total.year(), total.month());
            List<CreditCardStatement> monthlyStatements = allMonthlyStatements.stream()
                .filter(statement -> !statement.isPendingReview())
                .toList();
            double payments = monthlyStatements.stream().mapToDouble(CreditCardStatement::getPayments).sum();
            double purchases = monthlyStatements.stream().mapToDouble(CreditCardStatement::getTransactions).sum();
            double interest = monthlyStatements.stream().mapToDouble(CreditCardStatement::getInterestCharged).sum();
            VBox card = new PeriodActionCardView().build(
                cardPeriodTitle(allMonthlyStatements, total),
                reviewMarkLabel("card", alias, total.year(), total.month()),
                () -> {
                    selectedYearValue = total.year();
                    selectedMonthValue = total.month();
                    showCardPeriodDetail(alias, total.year(), total.month());
                }
            );
            card.getChildren().get(0).getStyleClass().add("card-period-title-row");
            CreditCardStatement openingStatement = cardOpeningStatement(allMonthlyStatements);
            CreditCardStatement closingStatement = cardClosingStatement(allMonthlyStatements);
            if (openingStatement != null) {
                addMonthlyCardLine(card, "Saldo inicial: " + Money.format(openingStatement.getPreviousBalance()));
            }
            addMonthlyCardLine(card, "Pagos al banco: " + Money.format(payments));
            addMonthlyCardLine(card, "Compras: " + Money.format(purchases));
            addMonthlyCardLine(card, "Intereses: " + Money.format(interest));
            if (closingStatement != null) {
                addMonthlyCardLine(card, "Saldo usado: " + Money.format(closingStatement.getNewBalance()));
                addMonthlyCardLine(card, "Límite de crédito: " + Money.format(closingStatement.getCreditLimit()));
                addMonthlyCardLine(card, "Crédito disponible: " + Money.format(closingStatement.getAvailableCredit()), "monthly-card-value-strong");
            }
            Button editPeriod = new Button("Editar datos");
            editPeriod.setOnAction(event -> {
                event.consume();
                showCreditCardPeriodDialog(allMonthlyStatements, () -> {
                    table.setItems(FXCollections.observableArrayList(creditCardStatementRepository.findByAccount(alias, selectedYear(), selectedMonth())));
                    Runnable visibleTotals = () -> totalsPanel.getChildren().setAll(cardAccumulatedTotalsNodes(alias, selectedYear(), selectedMonth()));
                    visibleTotals.run();
                    refreshCreditCardStatementCards(table, statementCards, visibleTotals);
                    refreshCreditCardMonthlyCards(alias, table, totalsPanel, statementCards, cards);
                });
            });
            Button deletePeriod = new Button("Eliminar");
            deletePeriod.getStyleClass().add("danger-button");
            deletePeriod.setOnAction(event -> {
                event.consume();
                deleteCreditCardPeriod(alias, allMonthlyStatements, table, totalsPanel, statementCards, cards);
            });
            Button download = monthlyExportButton(() -> exportMonthlyCard(alias, total.year(), total.month()));
            download.setText("Descargar");
            HBox mainActions = new HBox(8, editPeriod, download);
            mainActions.getStyleClass().add("card-monthly-actions");
            HBox deleteActions = new HBox(8, deletePeriod);
            deleteActions.getStyleClass().add("card-monthly-actions");
            deleteActions.getStyleClass().add("card-monthly-delete-actions");
            card.getChildren().addAll(mainActions, deleteActions);
            card.getStyleClass().add("monthly-card");
            cards.getChildren().add(card);
        }

        VBox box = new VBox(10, title, cards);
        box.getStyleClass().add("monthly-section");
        return box;
    }

    private CreditCardStatement cardOpeningStatement(List<CreditCardStatement> statements) {
        return statements.stream()
            .min(Comparator
                .comparing(CreditCardStatement::getStatementStartDate, Comparator.nullsLast(LocalDate::compareTo))
                .thenComparingLong(CreditCardStatement::getId))
            .orElse(null);
    }

    private CreditCardStatement cardClosingStatement(List<CreditCardStatement> statements) {
        return statements.stream()
            .max(Comparator
                .comparing(CreditCardStatement::getStatementEndDate, Comparator.nullsFirst(LocalDate::compareTo))
                .thenComparingLong(CreditCardStatement::getId))
            .orElse(null);
    }

    private void refreshCreditCardMonthlyCards(String alias, TableView<CreditCardStatement> table, HBox totalsPanel, VBox statementCards, FlowPane cards) {
        FlowPane refreshed = (FlowPane) monthlyCardCards(alias, table, totalsPanel, statementCards).getChildren().get(1);
        cards.getChildren().setAll(refreshed.getChildren());
    }

    private void deleteCreditCardPeriod(String alias, List<CreditCardStatement> statements, TableView<CreditCardStatement> table, HBox totalsPanel, VBox statementCards, FlowPane cards) {
        if (statements.isEmpty()) {
            return;
        }
        String period = cardPeriodTitle(statements, new MonthlySourceTotals(
            statements.get(0).getStatementEndDate() == null ? selectedYear() : statements.get(0).getStatementEndDate().getYear(),
            statements.get(0).getStatementEndDate() == null ? selectedMonth() : statements.get(0).getStatementEndDate().getMonthValue(),
            0, 0, 0, 0, 0
        ));
        boolean proceed = confirm(
            "Eliminar periodo de tarjeta",
            "Se eliminaran " + statements.size() + " resumen(es) de tarjeta del periodo " + period + ", junto con sus movimientos y alertas.\n\nEsta accion no se puede deshacer.",
            "Eliminar periodo"
        );
        if (!proceed) {
            return;
        }
        for (CreditCardStatement statement : statements) {
            if (statement.getId() > 0) {
                creditCardStatementRepository.delete(statement.getId());
            }
        }
        table.setItems(FXCollections.observableArrayList(creditCardStatementRepository.findByAccount(alias, selectedYear(), selectedMonth())));
        Runnable refreshTotals = () -> totalsPanel.getChildren().setAll(cardAccumulatedTotalsNodes(alias, selectedYear(), selectedMonth()));
        refreshTotals.run();
        refreshCreditCardStatementCards(table, statementCards, refreshTotals);
        refreshCreditCardMonthlyCards(alias, table, totalsPanel, statementCards, cards);
    }

    private LocalDate cardPeriodStart(List<CreditCardStatement> statements) {
        return statements.stream()
            .map(CreditCardStatement::getStatementStartDate)
            .filter(java.util.Objects::nonNull)
            .min(LocalDate::compareTo)
            .orElse(null);
    }

    private LocalDate cardPeriodEnd(List<CreditCardStatement> statements) {
        return statements.stream()
            .map(CreditCardStatement::getStatementEndDate)
            .filter(java.util.Objects::nonNull)
            .max(LocalDate::compareTo)
            .orElse(null);
    }

    private String cardPeriodDate(LocalDate date) {
        return date == null ? "Sin fecha" : shortDate(date);
    }

    private String cardPeriodTitle(List<CreditCardStatement> statements, MonthlySourceTotals fallback) {
        LocalDate start = cardPeriodStart(statements);
        LocalDate end = cardPeriodEnd(statements);
        if (start != null && end != null) {
            return cardPeriodDate(start) + " - " + cardPeriodDate(end);
        }
        return monthName(fallback.month()) + " " + fallback.year();
    }

    private void showCreditCardPeriodDialog(List<CreditCardStatement> statements, Runnable refresh) {
        if (statements.isEmpty()) {
            alert(Alert.AlertType.INFORMATION, "Sin resumen", "No hay resumen de tarjeta para editar en este mes.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar datos de tarjeta");
        ComboBox<CreditCardStatement> selector = new ComboBox<>(FXCollections.observableArrayList(statements));
        selector.setConverter(new StringConverter<>() {
            @Override
            public String toString(CreditCardStatement statement) {
                return statement == null ? "" : cardStatementTitle(statement);
            }

            @Override
            public CreditCardStatement fromString(String value) {
                return selector.getValue();
            }
        });
        selector.setValue(statements.get(0));
        DatePicker start = new DatePicker(selector.getValue().getStatementStartDate());
        DatePicker end = new DatePicker(selector.getValue().getStatementEndDate());
        DatePicker due = new DatePicker(selector.getValue().getPaymentDueDate());
        DatePicker nextClosing = new DatePicker(selector.getValue().getNextClosingDate());
        TextField previous = cardDialogMoneyField(selector.getValue().getPreviousBalance());
        TextField payments = cardDialogMoneyField(selector.getValue().getPayments());
        TextField credits = cardDialogMoneyField(selector.getValue().getOtherCredits());
        TextField purchases = cardDialogMoneyField(selector.getValue().getTransactions());
        TextField transfers = cardDialogMoneyField(selector.getValue().getBalanceTransfers());
        TextField cash = cardDialogMoneyField(selector.getValue().getCashAdvances());
        TextField fees = cardDialogMoneyField(selector.getValue().getFeesCharged());
        TextField interest = cardDialogMoneyField(selector.getValue().getInterestCharged());
        TextField newBalance = cardDialogMoneyField(selector.getValue().getNewBalance());
        TextField minimum = cardDialogMoneyField(selector.getValue().getMinimumPaymentDue());
        TextField limit = cardDialogMoneyField(selector.getValue().getCreditLimit());
        TextField available = cardDialogMoneyField(selector.getValue().getAvailableCredit());
        TextField cashLimit = cardDialogMoneyField(selector.getValue().getCashAdvanceLimit());
        TextField cashAvailable = cardDialogMoneyField(selector.getValue().getAvailableCashAdvanceCredit());
        TextField rewardsBalance = cardDialogMoneyField(selector.getValue().getRewardsBalance());
        TextField rewardsPrevious = cardDialogMoneyField(selector.getValue().getRewardsPreviousBalance());
        TextField rewardsEarned = cardDialogMoneyField(selector.getValue().getRewardsEarned());
        TextField rewardsRedeemed = cardDialogMoneyField(selector.getValue().getRewardsRedeemed());
        CheckBox reviewed = new CheckBox("Revisado");
        reviewed.setSelected(!selector.getValue().isPendingReview());
        TextArea notes = new TextArea(text(selector.getValue().getReviewNotes()));
        notes.setPrefRowCount(2);
        selector.setOnAction(event -> {
            CreditCardStatement selected = selector.getValue();
            start.setValue(selected == null ? null : selected.getStatementStartDate());
            end.setValue(selected == null ? null : selected.getStatementEndDate());
            due.setValue(selected == null ? null : selected.getPaymentDueDate());
            nextClosing.setValue(selected == null ? null : selected.getNextClosingDate());
            cardDialogSetMoney(previous, selected == null ? 0 : selected.getPreviousBalance());
            cardDialogSetMoney(payments, selected == null ? 0 : selected.getPayments());
            cardDialogSetMoney(credits, selected == null ? 0 : selected.getOtherCredits());
            cardDialogSetMoney(purchases, selected == null ? 0 : selected.getTransactions());
            cardDialogSetMoney(transfers, selected == null ? 0 : selected.getBalanceTransfers());
            cardDialogSetMoney(cash, selected == null ? 0 : selected.getCashAdvances());
            cardDialogSetMoney(fees, selected == null ? 0 : selected.getFeesCharged());
            cardDialogSetMoney(interest, selected == null ? 0 : selected.getInterestCharged());
            cardDialogSetMoney(newBalance, selected == null ? 0 : selected.getNewBalance());
            cardDialogSetMoney(minimum, selected == null ? 0 : selected.getMinimumPaymentDue());
            cardDialogSetMoney(limit, selected == null ? 0 : selected.getCreditLimit());
            cardDialogSetMoney(available, selected == null ? 0 : selected.getAvailableCredit());
            cardDialogSetMoney(cashLimit, selected == null ? 0 : selected.getCashAdvanceLimit());
            cardDialogSetMoney(cashAvailable, selected == null ? 0 : selected.getAvailableCashAdvanceCredit());
            cardDialogSetMoney(rewardsBalance, selected == null ? 0 : selected.getRewardsBalance());
            cardDialogSetMoney(rewardsPrevious, selected == null ? 0 : selected.getRewardsPreviousBalance());
            cardDialogSetMoney(rewardsEarned, selected == null ? 0 : selected.getRewardsEarned());
            cardDialogSetMoney(rewardsRedeemed, selected == null ? 0 : selected.getRewardsRedeemed());
            reviewed.setSelected(selected != null && !selected.isPendingReview());
            notes.setText(selected == null ? "" : text(selected.getReviewNotes()));
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        int row = 0;
        if (statements.size() > 1) {
            form.addRow(row++, new Label("Resumen"), selector);
        }
        form.addRow(row++, new Label("Statement Start Date"), start);
        form.addRow(row++, new Label("Statement End Date"), end);
        form.addRow(row++, new Label("Payment Due Date"), due);
        form.addRow(row++, new Label("Next Statement Closing Date"), nextClosing);
        form.addRow(row++, new Label("Previous Balance"), previous);
        form.addRow(row++, new Label("Payments and Credits"), payments);
        form.addRow(row++, new Label("Other Credits"), credits);
        form.addRow(row++, new Label("Purchases"), purchases);
        form.addRow(row++, new Label("Balance Transfers"), transfers);
        form.addRow(row++, new Label("Cash Advances"), cash);
        form.addRow(row++, new Label("Fees Charged"), fees);
        form.addRow(row++, new Label("Interest Charged"), interest);
        form.addRow(row++, new Label("New Balance"), newBalance);
        form.addRow(row++, new Label("Minimum Payment Due"), minimum);
        form.addRow(row++, new Label("Credit Line"), limit);
        form.addRow(row++, new Label("Credit Line Available"), available);
        form.addRow(row++, new Label("Cash Advance Credit Line"), cashLimit);
        form.addRow(row++, new Label("Cash Advance Credit Line Available"), cashAvailable);
        form.addRow(row++, new Label("Rewards Balance"), rewardsBalance);
        form.addRow(row++, new Label("Previous Rewards"), rewardsPrevious);
        form.addRow(row++, new Label("Earned This Period"), rewardsEarned);
        form.addRow(row++, new Label("Redeemed This Period"), rewardsRedeemed);
        form.addRow(row++, new Label("Review"), reviewed);
        form.addRow(row, new Label("Notes"), notes);
        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(560);
        scroll.setPrefViewportHeight(620);
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(result -> {
            CreditCardStatement selected = selector.getValue();
            if (selected == null) {
                return;
            }
            if (start.getValue() == null || end.getValue() == null) {
                alert(Alert.AlertType.WARNING, "Periodo incompleto", "Indica fecha Desde y Hasta para el resumen de tarjeta.");
                return;
            }
            if (start.getValue().isAfter(end.getValue())) {
                alert(Alert.AlertType.WARNING, "Periodo no valido", "La fecha Desde no puede ser posterior a Hasta.");
                return;
            }
            try {
                selected.setStatementStartDate(start.getValue());
                selected.setStatementEndDate(end.getValue());
                selected.setPaymentDueDate(due.getValue());
                selected.setNextClosingDate(nextClosing.getValue());
                selected.setPreviousBalance(cardDialogMoney(previous));
                selected.setPayments(cardDialogMoney(payments));
                selected.setOtherCredits(cardDialogMoney(credits));
                selected.setTransactions(cardDialogMoney(purchases));
                selected.setBalanceTransfers(cardDialogMoney(transfers));
                selected.setCashAdvances(cardDialogMoney(cash));
                selected.setFeesCharged(cardDialogMoney(fees));
                selected.setInterestCharged(cardDialogMoney(interest));
                selected.setNewBalance(cardDialogMoney(newBalance));
                selected.setMinimumPaymentDue(cardDialogMoney(minimum));
                selected.setCreditLimit(cardDialogMoney(limit));
                selected.setAvailableCredit(cardDialogMoney(available));
                selected.setCashAdvanceLimit(cardDialogMoney(cashLimit));
                selected.setAvailableCashAdvanceCredit(cardDialogMoney(cashAvailable));
                selected.setRewardsBalance(cardDialogMoney(rewardsBalance));
                selected.setRewardsPreviousBalance(cardDialogMoney(rewardsPrevious));
                selected.setRewardsEarned(cardDialogMoney(rewardsEarned));
                selected.setRewardsRedeemed(cardDialogMoney(rewardsRedeemed));
                selected.setPendingReview(!reviewed.isSelected());
                selected.setReviewRequired(!reviewed.isSelected());
                selected.setReviewNotes(notes.getText());
                if (selected.getId() > 0) {
                    creditCardStatementRepository.updateRecord(selected);
                }
                refresh.run();
            } catch (RuntimeException exception) {
                alert(Alert.AlertType.ERROR, "No se pudieron editar los datos", rootCauseMessage(exception));
            }
        });
    }

    private TextField cardDialogMoneyField(double value) {
        TextField field = new TextField();
        cardDialogSetMoney(field, value);
        return field;
    }

    private void cardDialogSetMoney(TextField field, double value) {
        field.setText(String.format(java.util.Locale.US, "%.2f", value));
    }

    private double cardDialogMoney(TextField field) {
        return Money.parse(field.getText());
    }

    private List<BankPeriodSummary> bankPeriodSummaries(String accountAliasFilter) {
        return bankModule.accountDetails().periodSummaries(accountAliasFilter);
    }

    private String shortDate(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private VBox monthlyActionCard(String title, String line1, String line2, String line3, String line4, Runnable action) {
        Label heading = new Label(title);
        heading.getStyleClass().add("monthly-card-title");
        GridPane lines = new GridPane();
        lines.getStyleClass().add("monthly-card-grid");
        int row = 0;
        for (String line : List.of(line1, line2, line3, line4)) {
            if (!line.isBlank()) {
                row = addMonthlyCardGridLine(lines, row, line);
            }
        }
        VBox box = new VBox(0, heading, lines);
        box.setOnMouseClicked(event -> action.run());
        return box;
    }

    private void addReviewMark(VBox card, String source, String accountAlias, int year, int month) {
        if (card.getChildren().isEmpty() || !(card.getChildren().get(0) instanceof Label currentHeading)) {
            return;
        }
        Label heading = new Label(currentHeading.getText());
        heading.getStyleClass().addAll(currentHeading.getStyleClass());
        Label mark = reviewMarkLabel(source, accountAlias, year, month);
        HBox titleRow = new HBox(heading, mark);
        titleRow.getStyleClass().add("monthly-card-title-row");
        HBox.setHgrow(heading, Priority.ALWAYS);
        card.getChildren().set(0, titleRow);
    }

    private Label reviewMarkLabel(String source, String accountAlias, int year, int month) {
        Label mark = new Label();
        mark.getStyleClass().add("review-mark-toggle");
        refreshReviewMarkLabel(mark, source, accountAlias, year, month);
        mark.setOnMouseClicked(event -> {
            event.consume();
            reviewMarkRepository.toggle(source, accountAlias, year, month);
            refreshReviewMarkLabel(mark, source, accountAlias, year, month);
        });
        return mark;
    }

    private void refreshReviewMarkLabel(Label mark, String source, String accountAlias, int year, int month) {
        boolean marked = reviewMarkRepository.isMarked(source, accountAlias, year, month);
        mark.setText(marked ? "\uD83D\uDD12" : "\u25A1");
        mark.getStyleClass().removeAll("review-mark-on", "review-mark-off");
        mark.getStyleClass().add(marked ? "review-mark-on" : "review-mark-off");
    }

    private int addMonthlyCardGridLine(GridPane grid, int row, String text) {
        return addMonthlyCardGridLine(grid, row, text, null);
    }

    private int addMonthlyCardGridLine(GridPane grid, int row, String text, String valueStyleClass) {
        int separator = text.indexOf(": ");
        if (separator <= 0) {
            Label label = new Label(text);
            label.getStyleClass().add("monthly-card-line");
            grid.add(label, 0, row, 2, 1);
            return row + 1;
        }
        Label label = new Label(text.substring(0, separator));
        label.getStyleClass().add("monthly-card-line");
        Label value = new Label(text.substring(separator + 2));
        value.getStyleClass().add("monthly-card-value");
        if (valueStyleClass != null && !valueStyleClass.isBlank()) {
            value.getStyleClass().add(valueStyleClass);
        }
        grid.add(label, 0, row);
        grid.add(value, 1, row);
        return row + 1;
    }

    private void addMonthlyCardDivider(VBox card) {
        if (card.getChildren().isEmpty() || !(card.getChildren().get(card.getChildren().size() - 1) instanceof GridPane grid)) {
            return;
        }
        Separator separator = new Separator();
        separator.getStyleClass().add("monthly-card-divider");
        grid.add(separator, 0, nextMonthlyCardGridRow(grid), 2, 1);
    }

    private void addMonthlyCardLine(VBox card, String text) {
        addMonthlyCardLine(card, text, "monthly-card-line");
    }

    private void addMonthlyCardLine(VBox card, String text, String styleClass) {
        if (text == null || text.isBlank()) {
            return;
        }
        text = text.replaceFirst("\\s+.*Dif\\.:.*$", "");
        if ("monthly-card-line".equals(styleClass) && !card.getChildren().isEmpty() && card.getChildren().get(card.getChildren().size() - 1) instanceof GridPane grid) {
            addMonthlyCardGridLine(grid, nextMonthlyCardGridRow(grid), text);
            return;
        }
        if (styleClass.startsWith("monthly-card-value-") && !card.getChildren().isEmpty() && card.getChildren().get(card.getChildren().size() - 1) instanceof GridPane grid) {
            addMonthlyCardGridLine(grid, nextMonthlyCardGridRow(grid), text, styleClass);
            return;
        }
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        card.getChildren().add(label);
    }

    private int nextMonthlyCardGridRow(GridPane grid) {
        return grid.getChildren().stream()
            .map(GridPane::getRowIndex)
            .mapToInt(row -> row == null ? 0 : row)
            .max()
            .orElse(-1) + 1;
    }

    private Button monthlyExportButton(Runnable action) {
        Button button = new Button("Descargar mes");
        button.setOnAction(event -> {
            event.consume();
            action.run();
        });
        return button;
    }

    private void exportMonthlyNyl(int year, int month) {
        File file = chooseExcel("nyl-" + year + "-" + String.format("%02d", month) + ".xlsx");
        if (file == null) {
            return;
        }
        excelExportService.exportNylMonthly(file.toPath(), nylRepository.find(year, month, null, null));
        alert(Alert.AlertType.INFORMATION, "Mes exportado", file.getAbsolutePath());
    }

    private void exportMonthlyCard(String alias, int year, int month) {
        File file = chooseExcel("tarjeta-" + safeFileName(alias) + "-" + year + "-" + String.format("%02d", month) + ".xlsx");
        if (file == null) {
            return;
        }
        excelExportService.exportCreditCardMonthly(
            file.toPath(),
            creditCardStatementRepository.findByAccount(alias, year, month),
            creditCardTransactionRepository.findByAccount(alias, year, month)
        );
        alert(Alert.AlertType.INFORMATION, "Mes exportado", file.getAbsolutePath());
    }

    private void exportMonthlyReconciliation() {
        int year = selectedYear() == null ? LocalDate.now().getYear() : selectedYear();
        int month = selectedMonth() == null ? LocalDate.now().getMonthValue() : selectedMonth();
        File file = chooseExcel("conciliacion-" + year + "-" + String.format("%02d", month) + ".xlsx");
        if (file == null) {
            return;
        }
        excelExportService.exportReconciliationMonthly(
            file.toPath(),
            bankModule.application().transactions().find(year, month, null, null),
            allCardStatements(year, month),
            allCardTransactions(year, month),
            allMortgageStatements(year, month),
            allMortgageTransactions(year, month),
            nylRepository.find(year, month, null, null),
            reconciliationService.preview(year, month)
        );
        alert(Alert.AlertType.INFORMATION, "Conciliación exportada", file.getAbsolutePath());
    }

    private List<CreditCardStatement> allCardStatements(int year, int month) {
        return creditCardAccountRepository.findAll().stream()
            .flatMap(account -> creditCardStatementRepository.findByAccount(account.getAlias(), year, month).stream())
            .toList();
    }

    private List<CreditCardTransaction> allCardTransactions(int year, int month) {
        return creditCardAccountRepository.findAll().stream()
            .flatMap(account -> creditCardTransactionRepository.findByAccount(account.getAlias(), year, month).stream())
            .toList();
    }

    private List<MortgageStatement> allMortgageStatements(int year, int month) {
        return mortgageStatementRepository.findLoanAliases().stream()
            .flatMap(alias -> mortgageStatementRepository.findByLoan(alias, year, month).stream())
            .toList();
    }

    private List<MortgageTransaction> allMortgageTransactions(int year, int month) {
        return mortgageStatementRepository.findLoanAliases().stream()
            .flatMap(alias -> mortgageTransactionRepository.findByLoan(alias, year, month).stream())
            .toList();
    }

    private File chooseExcel(String initialFileName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar Excel mensual");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        chooser.setInitialFileName(initialFileName);
        return chooser.showSaveDialog(root.getScene().getWindow());
    }

    private String safeFileName(String value) {
        return value == null || value.isBlank() ? "general" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private VBox miniTotal(String title, String value, String styleClass) {
        Label label = new Label(title);
        label.getStyleClass().add("mini-total-title");
        Label amount = new Label(value);
        amount.getStyleClass().add("mini-total-value");
        VBox box = new VBox(4, label, amount);
        box.getStyleClass().addAll("mini-total", styleClass);
        return box;
    }

    private Label sectionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private VBox reviewSummaryBox(List<NylRecord> records, int hiddenExisting) {
        long reviewCount = records.stream().filter(NylRecord::isReviewRequired).count();
        long creditCount = records.stream().filter(record -> "Creditos".equals(record.getSection())).count();
        long deductionCount = records.stream().filter(record -> "Deducciones".equals(record.getSection())).count();
        Label title = new Label("Revisión pendiente");
        title.getStyleClass().add("section-title");
        Label detail = new Label(records.size() + " registros nuevos: " + creditCount + " créditos, " + deductionCount + " deducciones, " + reviewCount + " marcados para revisar. " + hiddenExisting + " ya existentes ocultos.");
        detail.getStyleClass().add("section-subtitle");
        VBox box = new VBox(6, title, detail);
        box.getStyleClass().add("totals-box");
        return box;
    }

    private void addMissingNylRow(TableView<NylRecord> table) {
        NylRecord context = table.getSelectionModel().getSelectedItem();
        if (context == null && !table.getItems().isEmpty()) {
            context = table.getItems().get(0);
        }
        int year = selectedYear() != null ? selectedYear() : (context == null ? LocalDate.now().getYear() : context.getYear());
        int month = selectedMonth() != null ? selectedMonth() : (context == null ? LocalDate.now().getMonthValue() : context.getMonth());
        String fingerprint = Fingerprint.of(year + "|" + month + "|registro faltante|credito|0.0|" + System.nanoTime());
        NylRecord record = new NylRecord(0, year, month, "Registro faltante", "Creditos", "credito", 0, "manual_en_revision", fingerprint, "manual", true, true, "Añadido manualmente durante revision");
        table.getItems().add(record);
        table.scrollTo(record);
        table.getSelectionModel().select(record);
    }

    private void showNylImportTotals(List<NylRecord> savedRecords, int inserted) {
        VBox totals = monthlyTotalsBox(savedRecords);
        Label message = new Label(inserted + " registros nuevos guardados. Estos totales ya se calculan con las cifras revisadas y confirmadas.");
        message.getStyleClass().add("review-ok");
        Button back = new Button("Volver a New York Life");
        back.getStyleClass().add("primary");
        back.setOnAction(event -> showNyl());
        setPage(page("Totales NYL confirmados", message, totals, back));
    }

    private Label headerLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("totals-header");
        return label;
    }

    private Label bodyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("totals-body");
        return label;
    }

    private Label moneyLabel(double value) {
        Label label = new Label(Money.format(value));
        label.getStyleClass().add(value < 0 ? "totals-money-negative" : "totals-money");
        return label;
    }

    private String monthName(int month) {
        return switch (month) {
            case 1 -> "Enero";
            case 2 -> "Febrero";
            case 3 -> "Marzo";
            case 4 -> "Abril";
            case 5 -> "Mayo";
            case 6 -> "Junio";
            case 7 -> "Julio";
            case 8 -> "Agosto";
            case 9 -> "Septiembre";
            case 10 -> "Octubre";
            case 11 -> "Noviembre";
            case 12 -> "Diciembre";
            default -> "Mes " + month;
        };
    }

    private void showProcessing(String title, String message) {
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(64, 64);
        Label heading = new Label(title);
        heading.getStyleClass().add("processing-title");
        Label detail = new Label(message);
        detail.getStyleClass().add("processing-detail");
        detail.setWrapText(true);
        VBox box = new VBox(16, progress, heading, detail);
        box.getStyleClass().add("processing-box");
        box.setAlignment(Pos.CENTER);
        setPage(page(title, box));
    }

    private File choosePdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importar PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        return chooser.showOpenDialog(root.getScene().getWindow());
    }

    private TableView<MortgageStatement> mortgageStatementTable() {
        TableView<MortgageStatement> table = new TableView<>();
        table.setEditable(true);
        TableColumn<MortgageStatement, Integer> year = new TableColumn<>("Año");
        year.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStatementDate() == null ? 0 : data.getValue().getStatementDate().getYear()).asObject());
        TableColumn<MortgageStatement, Integer> month = new TableColumn<>("Mes");
        month.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStatementDate() == null ? 0 : data.getValue().getStatementDate().getMonthValue()).asObject());
        TableColumn<MortgageStatement, String> statementDate = mortgageDateColumn("Statement Date\n(Fecha del estado)", MortgageStatement::getStatementDate, MortgageStatement::setStatementDate);
        TableColumn<MortgageStatement, String> dueDate = mortgageDateColumn("Payment Due Date\n(Fecha límite de pago)", MortgageStatement::getPaymentDueDate, MortgageStatement::setPaymentDueDate);
        TableColumn<MortgageStatement, Double> totalDue = mortgageMoneyColumn("Total Due\n(Deuda a pagar\neste mes)", s -> s.getTotalDue() > 0 ? s.getTotalDue() : s.getPaymentAmountDue(), MortgageStatement::setTotalDue);
        TableColumn<MortgageStatement, Boolean> reviewed = mortgageStatementReviewedColumn();
        TableColumn<MortgageStatement, Double> principal = mortgageMoneyColumn("Principal\n(Pago principal\nmensual)", MortgageStatement::getPrincipalDue, MortgageStatement::setPrincipalDue);
        TableColumn<MortgageStatement, Double> interest = mortgageMoneyColumn("Interest\n(Intereses)", MortgageStatement::getInterestDue, MortgageStatement::setInterestDue);
        TableColumn<MortgageStatement, Double> escrow = mortgageMoneyColumn("Escrow\n(Reserva impuestos/seguros)", MortgageStatement::getEscrowDue, MortgageStatement::setEscrowDue);
        TableColumn<MortgageStatement, Double> fees = mortgageMoneyColumn("Fees\n(Cargos)", MortgageStatement::getFees, MortgageStatement::setFees);
        TableColumn<MortgageStatement, Double> debt = mortgageMoneyColumn("Outstanding Principal\n(Deuda principal\npendiente)", MortgageStatement::getOutstandingPrincipalBalance, MortgageStatement::setOutstandingPrincipalBalance);
        TableColumn<MortgageStatement, Double> original = mortgageMoneyColumn("Original Principal\n(Principal original)", MortgageStatement::getOriginalPrincipalBalance, MortgageStatement::setOriginalPrincipalBalance);
        TableColumn<MortgageStatement, Double> rate = mortgageMoneyColumn("Interest Rate\n(Tasa de interés)", MortgageStatement::getInterestRate, MortgageStatement::setInterestRate);
        TableColumn<MortgageStatement, String> maturity = mortgageDateColumn("Maturity Date\n(Fecha finalizacion)", MortgageStatement::getMaturityDate, MortgageStatement::setMaturityDate);
        TableColumn<MortgageStatement, String> servicer = new TableColumn<>("Servicer\n(Entidad)");
        servicer.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getServicerName()));
        servicer.setCellFactory(TextFieldTableCell.forTableColumn());
        servicer.setOnEditCommit(event -> event.getRowValue().setServicerName(event.getNewValue()));
        TableColumn<MortgageStatement, String> status = new TableColumn<>("Revisión");
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isPendingReview() ? "Pdte revision" : "OK"));
        TableColumn<MortgageStatement, String> notes = new TableColumn<>("Notas");
        notes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReviewNotes()));
        notes.setCellFactory(TextFieldTableCell.forTableColumn());
        notes.setOnEditCommit(event -> event.getRowValue().setReviewNotes(event.getNewValue()));
        notes.setPrefWidth(240);
        TableColumn<MortgageStatement, Void> delete = new TableColumn<>("Eliminar");
        delete.setCellFactory(column -> new TableCell<>() {
            private final Button button = new Button("Eliminar");
            {
                button.setOnAction(event -> {
                    MortgageStatement statement = getTableView().getItems().get(getIndex());
                    if (statement.getId() > 0) {
                        mortgageStatementRepository.delete(statement.getId());
                    }
                    getTableView().getItems().remove(statement);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        });
        table.getColumns().setAll(year, month, statementDate, dueDate, totalDue, reviewed, principal, interest, escrow, fees, debt, original, rate, maturity, servicer, status, notes, delete);
        return table;
    }

    private TableColumn<MortgageStatement, Boolean> mortgageStatementReviewedColumn() {
        TableColumn<MortgageStatement, Boolean> reviewed = new TableColumn<>("Revisado");
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(!data.getValue().isPendingReview()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    MortgageStatement statement = getTableView().getItems().get(getIndex());
                    boolean isReviewed = checkBox.isSelected();
                    updateMortgageStatementReview(statement, isReviewed);
                    getTableView().refresh();
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(Boolean.TRUE.equals(item));
                    setGraphic(checkBox);
                }
            }
        });
        return reviewed;
    }

    private TableColumn<MortgageStatement, Double> mortgageMoneyColumn(String title, java.util.function.ToDoubleFunction<MortgageStatement> getter, java.util.function.BiConsumer<MortgageStatement, Double> setter) {
        TableColumn<MortgageStatement, Double> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleDoubleProperty(getter.applyAsDouble(data.getValue())).asObject());
        column.setCellFactory(TextFieldTableCell.forTableColumn(twoDecimalConverter()));
        column.setOnEditCommit(event -> setter.accept(event.getRowValue(), event.getNewValue()));
        column.setPrefWidth(130);
        return column;
    }

    private TableColumn<MortgageStatement, String> mortgageDateColumn(String title, java.util.function.Function<MortgageStatement, LocalDate> getter, java.util.function.BiConsumer<MortgageStatement, LocalDate> setter) {
        TableColumn<MortgageStatement, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue()) == null ? "" : getter.apply(data.getValue()).toString()));
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> setter.accept(event.getRowValue(), parseDateOrNull(event.getNewValue())));
        column.setPrefWidth(120);
        return column;
    }

    private TableView<MortgageTransaction> mortgageTransactionTable() {
        TableView<MortgageTransaction> table = new TableView<>();
        table.setEditable(true);
        TableColumn<MortgageTransaction, String> date = new TableColumn<>("Fecha");
        date.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTransactionDate() == null ? "" : data.getValue().getTransactionDate().toString()));
        date.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        date.setOnEditCommit(event -> {
            event.getRowValue().setTransactionDate(parseDateOrNull(event.getNewValue()));
            updateMortgageTransactionIfSaved(event.getRowValue());
        });
        TableColumn<MortgageTransaction, String> description = new TableColumn<>("Descripción");
        description.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        description.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        description.setOnEditCommit(event -> {
            event.getRowValue().setDescription(event.getNewValue());
            updateMortgageTransactionIfSaved(event.getRowValue());
        });
        description.setPrefWidth(320);
        TableColumn<MortgageTransaction, Double> total = mortgageTxMoneyColumn("Total\n(Total)", MortgageTransaction::getTotal, MortgageTransaction::setTotal);
        TableColumn<MortgageTransaction, Boolean> reviewed = mortgageTransactionReviewedColumn();
        TableColumn<MortgageTransaction, Double> principal = mortgageTxMoneyColumn("Principal\n(Pago principal\nmensual)", MortgageTransaction::getPrincipal, MortgageTransaction::setPrincipal);
        TableColumn<MortgageTransaction, Double> interest = mortgageTxMoneyColumn("Interest\n(Intereses)", MortgageTransaction::getInterest, MortgageTransaction::setInterest);
        TableColumn<MortgageTransaction, Double> escrow = mortgageTxMoneyColumn("Escrow\n(Reserva)", MortgageTransaction::getEscrow, MortgageTransaction::setEscrow);
        TableColumn<MortgageTransaction, Double> fees = mortgageTxMoneyColumn("Fees\n(Cargos)", MortgageTransaction::getFees, MortgageTransaction::setFees);
        TableColumn<MortgageTransaction, Double> unapplied = mortgageTxMoneyColumn("Unapplied\n(No aplicado)", MortgageTransaction::getUnapplied, MortgageTransaction::setUnapplied);
        TableColumn<MortgageTransaction, Double> other = mortgageTxMoneyColumn("Other\n(Otros)", MortgageTransaction::getOther, MortgageTransaction::setOther);
        TableColumn<MortgageTransaction, String> status = new TableColumn<>("Revisión");
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isPendingReview() ? "Pdte revision" : "OK"));
        TableColumn<MortgageTransaction, String> notes = new TableColumn<>("Notas");
        notes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReviewNotes()));
        notes.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        notes.setOnEditCommit(event -> {
            event.getRowValue().setReviewNotes(event.getNewValue());
            updateMortgageTransactionIfSaved(event.getRowValue());
        });
        notes.setPrefWidth(240);
        TableColumn<MortgageTransaction, Void> actions = new TableColumn<>("Acciones");
        actions.setPrefWidth(160);
        actions.setCellFactory(column -> new TableCell<>() {
            private final Button edit = new Button("Editar");
            private final Button delete = new Button("Eliminar");
            private final HBox buttons = new HBox(6, edit, delete);
            {
                edit.setOnAction(event -> {
                    MortgageTransaction movement = getTableView().getItems().get(getIndex());
                    showMortgageTransactionDialog(movement, () -> getTableView().refresh());
                });
                delete.getStyleClass().add("danger-button");
                delete.setOnAction(event -> {
                    MortgageTransaction movement = getTableView().getItems().get(getIndex());
                    if (movement.getId() > 0) {
                        mortgageTransactionRepository.delete(movement.getId());
                    }
                    getTableView().getItems().remove(movement);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
        table.getColumns().setAll(date, description, total, reviewed, principal, interest, escrow, fees, unapplied, other, status, notes, actions);
        return table;
    }

    private TableColumn<MortgageTransaction, Boolean> mortgageTransactionReviewedColumn() {
        TableColumn<MortgageTransaction, Boolean> reviewed = new TableColumn<>("Revisado");
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(!data.getValue().isPendingReview()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    MortgageTransaction row = getTableView().getItems().get(getIndex());
                    boolean isReviewed = checkBox.isSelected();
                    updateMortgageTransactionReview(row, isReviewed);
                    getTableView().refresh();
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(Boolean.TRUE.equals(item));
                    setGraphic(checkBox);
                }
            }
        });
        return reviewed;
    }

    private TableColumn<MortgageTransaction, Double> mortgageTxMoneyColumn(String title, java.util.function.ToDoubleFunction<MortgageTransaction> getter, java.util.function.BiConsumer<MortgageTransaction, Double> setter) {
        TableColumn<MortgageTransaction, Double> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleDoubleProperty(getter.applyAsDouble(data.getValue())).asObject());
        column.setCellFactory(commitOnFocusLostCellFactory(twoDecimalConverter()));
        column.setOnEditCommit(event -> {
            setter.accept(event.getRowValue(), event.getNewValue());
            updateMortgageTransactionIfSaved(event.getRowValue());
        });
        column.setPrefWidth(110);
        return column;
    }

    private TableView<HouseExpense> houseExpenseTable(Runnable rowsChanged, Map<Long, String> originalRows) {
        TableView<HouseExpense> table = new TableView<>();
        table.getStyleClass().add("house-expenses-table");
        table.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setEditable(true);
        TableColumn<HouseExpense, String> date = new TableColumn<>("Fecha");
        date.setCellValueFactory(data -> new SimpleStringProperty(formatShortDate(data.getValue().getExpenseDate())));
        date.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        date.setOnEditCommit(event -> {
            event.getRowValue().setExpenseDate(parseDateOrNull(event.getNewValue()));
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        date.setPrefWidth(120);
        date.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, String> mortgage = new TableColumn<>("Hipoteca");
        mortgage.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLoanAlias()));
        mortgage.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        mortgage.setOnEditCommit(event -> {
            event.getRowValue().setLoanAlias(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        mortgage.setPrefWidth(130);
        mortgage.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, String> description = new TableColumn<>("Descripción");
        description.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        description.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        description.setOnEditCommit(event -> {
            event.getRowValue().setDescription(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        description.setPrefWidth(260);
        description.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, String> provider = new TableColumn<>("Proveedor");
        provider.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProvider()));
        provider.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        provider.setOnEditCommit(event -> {
            event.getRowValue().setProvider(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        provider.setPrefWidth(180);
        provider.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, Double> amount = new TableColumn<>("Importe");
        amount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getAmount()).asObject());
        amount.setCellFactory(commitOnFocusLostCellFactory(twoDecimalConverter()));
        amount.setOnEditCommit(event -> {
            event.getRowValue().setAmount(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        amount.setPrefWidth(110);
        amount.setStyle("-fx-alignment: CENTER-RIGHT;");
        TableColumn<HouseExpense, String> invoice = new TableColumn<>("Factura");
        invoice.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getInvoice()));
        invoice.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        invoice.setOnEditCommit(event -> {
            event.getRowValue().setInvoice(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        invoice.setPrefWidth(160);
        invoice.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, String> paymentSource = new TableColumn<>("Pagado con");
        paymentSource.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPaymentSource()));
        paymentSource.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(houseExpensePaymentSourceOptions())));
        paymentSource.setOnEditCommit(event -> {
            event.getRowValue().setPaymentSource(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        paymentSource.setPrefWidth(210);
        paymentSource.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, Boolean> reviewed = houseExpenseReviewedColumn(rowsChanged);
        reviewed.setStyle("-fx-alignment: CENTER;");
        TableColumn<HouseExpense, String> notes = new TableColumn<>("Nota");
        notes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNotes()));
        notes.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        notes.setOnEditCommit(event -> {
            event.getRowValue().setNotes(event.getNewValue());
            updateHouseExpenseIfSaved(event.getRowValue(), rowsChanged);
        });
        notes.setPrefWidth(240);
        notes.setStyle("-fx-alignment: CENTER-LEFT;");
        TableColumn<HouseExpense, Void> document = houseExpenseDocumentColumn(rowsChanged, originalRows);
        TableColumn<HouseExpense, Void> actions = new TableColumn<>("Acciones");
        actions.setCellFactory(column -> new TableCell<>() {
            private final Button save = new Button("Guardar");
            private final Button edit = new Button("Editar");
            private final Button delete = new Button("Eliminar");
            private final HBox buttons = new HBox(6, save, edit, delete);
            {
                save.setOnAction(event -> {
                    HouseExpense expense = getTableView().getItems().get(getIndex());
                    saveHouseExpense(expense);
                    originalRows.put(expense.getId(), houseExpenseSnapshot(expense));
                    refreshRowsChanged(rowsChanged);
                    getTableView().refresh();
                });
                edit.setOnAction(event -> {
                    HouseExpense expense = getTableView().getItems().get(getIndex());
                    getTableView().getSelectionModel().select(expense);
                    getTableView().scrollTo(expense);
                    getTableView().requestFocus();
                    getTableView().edit(getIndex(), date);
                });
                delete.setOnAction(event -> {
                    HouseExpense expense = getTableView().getItems().get(getIndex());
                    if (expense.getId() > 0) {
                        deleteHouseExpenseDocumentFile(expense);
                        houseExpenseRepository.delete(expense.getId());
                        originalRows.remove(expense.getId());
                    }
                    getTableView().getItems().remove(expense);
                    refreshRowsChanged(rowsChanged);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
        actions.setPrefWidth(230);
        table.getColumns().setAll(date, mortgage, description, provider, amount, invoice, paymentSource, document, reviewed, notes, actions);
        return table;
    }

    private List<String> houseExpensePaymentSourceOptions() {
        List<String> options = new ArrayList<>();
        options.add("");
        for (BankAccount account : bankModule.accounts().list()) {
            options.add("Cuenta: " + account.getAlias());
        }
        for (CreditCardAccount account : creditCardAccountRepository.findAll()) {
            options.add("Tarjeta: " + account.getAlias());
        }
        return options;
    }

    private TableColumn<HouseExpense, Void> houseExpenseDocumentColumn(Runnable rowsChanged, Map<Long, String> originalRows) {
        TableColumn<HouseExpense, Void> document = new TableColumn<>("Documento");
        document.setCellFactory(column -> new TableCell<>() {
            private final Button attach = new Button("Adjuntar");
            private final Button view = new Button("Ver");
            private final Button change = new Button("Cambiar");
            private final Button remove = new Button("Quitar");
            private final HBox buttons = new HBox(6);
            {
                attach.setOnAction(event -> attachHouseExpenseDocument(currentExpense(), originalRows, rowsChanged));
                view.setOnAction(event -> openHouseExpenseDocument(currentExpense()));
                change.setOnAction(event -> attachHouseExpenseDocument(currentExpense(), originalRows, rowsChanged));
                remove.setOnAction(event -> removeHouseExpenseDocument(currentExpense(), originalRows, rowsChanged));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                buttons.getChildren().clear();
                if (!empty) {
                    HouseExpense expense = currentExpense();
                    if (hasHouseExpenseDocument(expense)) {
                        buttons.getChildren().addAll(view, change, remove);
                    } else {
                        buttons.getChildren().add(attach);
                    }
                }
                setGraphic(empty ? null : buttons);
            }

            private HouseExpense currentExpense() {
                return getTableView().getItems().get(getIndex());
            }
        });
        document.setPrefWidth(220);
        return document;
    }

    private TableColumn<HouseExpense, Boolean> houseExpenseReviewedColumn(Runnable rowsChanged) {
        TableColumn<HouseExpense, Boolean> reviewed = new TableColumn<>("Revisado");
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(!data.getValue().isPendingReview()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                setAlignment(Pos.CENTER);
                checkBox.setOnAction(event -> {
                    HouseExpense expense = getTableView().getItems().get(getIndex());
                    boolean isReviewed = checkBox.isSelected();
                    expense.setPendingReview(!isReviewed);
                    expense.setReviewRequired(!isReviewed);
                    updateHouseExpenseIfSaved(expense, rowsChanged);
                    getTableView().refresh();
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(Boolean.TRUE.equals(item));
                    setGraphic(checkBox);
                }
            }
        });
        return reviewed;
    }

    private void updateHouseExpenseIfSaved(HouseExpense expense, Runnable rowsChanged) {
        refreshRowsChanged(rowsChanged);
    }

    private void tableRefresh(TableView<?> table) {
        if (table != null) {
            table.refresh();
        }
    }

    private void captureHouseExpenseRows(TableView<HouseExpense> table, Map<Long, String> originalRows) {
        originalRows.clear();
        for (HouseExpense expense : table.getItems()) {
            if (expense.getId() > 0) {
                originalRows.put(expense.getId(), houseExpenseSnapshot(expense));
            }
        }
    }

    private boolean confirmHouseExpenseNavigation(TableView<HouseExpense> table, Map<Long, String> originalRows, Runnable refresh) {
        if (!hasUnsavedHouseExpenseChanges(table, originalRows)) {
            return true;
        }
        ButtonType save = new ButtonType("Guardar");
        ButtonType discard = new ButtonType("Salir sin guardar");
        Alert alert = new Alert(Alert.AlertType.WARNING,
            "Hay cambios en Casa - Gastos que todavia no has guardado. ¿Quieres guardarlos antes de salir?",
            save,
            discard,
            ButtonType.CANCEL
        );
        alert.setTitle("Cambios sin guardar");
        alert.setHeaderText("Cambios sin guardar");
        alert.getDialogPane().setMinWidth(620);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == ButtonType.CANCEL) {
            return false;
        }
        if (result.get() == save) {
            try {
                saveHouseExpenseChanges(table, originalRows);
            } catch (RuntimeException exception) {
                alert(Alert.AlertType.ERROR, "No se pudieron guardar los gastos", rootCauseMessage(exception));
                return false;
            }
        } else {
            refresh.run();
        }
        return true;
    }

    private boolean hasUnsavedHouseExpenseChanges(TableView<HouseExpense> table, Map<Long, String> originalRows) {
        for (HouseExpense expense : table.getItems()) {
            if (expense.getId() == 0) {
                return true;
            }
            if (!houseExpenseSnapshot(expense).equals(originalRows.get(expense.getId()))) {
                return true;
            }
        }
        return false;
    }

    private void saveHouseExpenseChanges(TableView<HouseExpense> table, Map<Long, String> originalRows) {
        for (HouseExpense expense : table.getItems()) {
            if (expense.getId() == 0 || !houseExpenseSnapshot(expense).equals(originalRows.get(expense.getId()))) {
                saveHouseExpense(expense);
                originalRows.put(expense.getId(), houseExpenseSnapshot(expense));
            }
        }
        table.refresh();
    }

    private void saveHouseExpense(HouseExpense expense) {
        if (expense.getId() == 0) {
            long id = houseExpenseRepository.save(expense);
            expense.setId(id);
        } else {
            houseExpenseRepository.update(expense);
        }
    }

    private void attachHouseExpenseDocument(HouseExpense expense, Map<Long, String> originalRows, Runnable rowsChanged) {
        if (expense == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Adjuntar documento");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Documentos e imagenes", "*.pdf", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp", "*.tif", "*.tiff", "*.doc", "*.docx", "*.xls", "*.xlsx"),
            new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );
        File selected = chooser.showOpenDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (selected == null) {
            return;
        }
        try {
            if (expense.getId() == 0) {
                saveHouseExpense(expense);
            }
            Path folder = Path.of("data", "documentos", "casa-gastos", String.valueOf(expense.getId()));
            Files.createDirectories(folder);
            String originalName = selected.getName();
            Path target = folder.resolve(UUID.randomUUID() + extension(originalName));
            Files.copy(selected.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            deleteHouseExpenseDocumentFile(expense);
            expense.setDocumentPath(target.toString());
            expense.setDocumentName(originalName);
            houseExpenseRepository.update(expense);
            originalRows.put(expense.getId(), houseExpenseSnapshot(expense));
            refreshRowsChanged(rowsChanged);
        } catch (IOException | RuntimeException exception) {
            alert(Alert.AlertType.ERROR, "No se pudo adjuntar", rootCauseMessage(exception));
        }
    }

    private void openHouseExpenseDocument(HouseExpense expense) {
        if (!hasHouseExpenseDocument(expense)) {
            alert(Alert.AlertType.INFORMATION, "Sin documento", "Este gasto no tiene documento adjunto.");
            return;
        }
        Path path = Path.of(expense.getDocumentPath());
        if (!Files.exists(path)) {
            alert(Alert.AlertType.WARNING, "Documento no encontrado", "No se encontro el archivo adjunto en:\n" + path);
            return;
        }
        try {
            Desktop.getDesktop().open(path.toFile());
        } catch (IOException | RuntimeException exception) {
            alert(Alert.AlertType.ERROR, "No se pudo abrir", rootCauseMessage(exception));
        }
    }

    private void removeHouseExpenseDocument(HouseExpense expense, Map<Long, String> originalRows, Runnable rowsChanged) {
        if (expense == null || !hasHouseExpenseDocument(expense)) {
            return;
        }
        deleteHouseExpenseDocumentFile(expense);
        expense.setDocumentPath(null);
        expense.setDocumentName(null);
        if (expense.getId() > 0) {
            houseExpenseRepository.update(expense);
            originalRows.put(expense.getId(), houseExpenseSnapshot(expense));
        }
        refreshRowsChanged(rowsChanged);
    }

    private boolean hasHouseExpenseDocument(HouseExpense expense) {
        return expense != null && expense.getDocumentPath() != null && !expense.getDocumentPath().isBlank();
    }

    private void deleteHouseExpenseDocumentFile(HouseExpense expense) {
        if (!hasHouseExpenseDocument(expense)) {
            return;
        }
        try {
            Path path = Path.of(expense.getDocumentPath()).normalize();
            Path documentsRoot = Path.of("data", "documentos", "casa-gastos").normalize();
            if (path.startsWith(documentsRoot)) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // If the file is locked or already gone, keep the data operation moving.
        }
    }

    private String extension(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }

    private String houseExpenseSnapshot(HouseExpense expense) {
        return String.join("\u001F",
            text(expense.getLoanAlias()),
            expense.getExpenseDate() == null ? "" : expense.getExpenseDate().toString(),
            text(expense.getDescription()),
            text(expense.getProvider()),
            String.valueOf(expense.getAmount()),
            text(expense.getInvoice()),
            text(expense.getPaymentSource()),
            text(expense.getNotes()),
            text(expense.getDocumentPath()),
            text(expense.getDocumentName()),
            String.valueOf(expense.isReviewRequired()),
            String.valueOf(expense.isPendingReview())
        );
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private void refreshRowsChanged(Runnable rowsChanged) {
        if (rowsChanged != null) {
            rowsChanged.run();
        }
    }

    private TableView<CreditCardStatement> creditCardStatementTable() {
        TableView<CreditCardStatement> table = new TableView<>();
        table.setEditable(true);

        TableColumn<CreditCardStatement, Integer> year = new TableColumn<>("Año");
        year.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStatementEndDate() == null ? 0 : data.getValue().getStatementEndDate().getYear()).asObject());
        TableColumn<CreditCardStatement, Integer> month = new TableColumn<>("Mes");
        month.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStatementEndDate() == null ? 0 : data.getValue().getStatementEndDate().getMonthValue()).asObject());

        TableColumn<CreditCardStatement, String> startDate = cardDateColumn("Inicio ciclo", CreditCardStatement::getStatementStartDate, CreditCardStatement::setStatementStartDate);
        TableColumn<CreditCardStatement, String> endDate = cardDateColumn("Cierre ciclo", CreditCardStatement::getStatementEndDate, CreditCardStatement::setStatementEndDate);
        TableColumn<CreditCardStatement, Double> newBalance = cardMoneyColumn("Deuda cierre", CreditCardStatement::getNewBalance, CreditCardStatement::setNewBalance);
        TableColumn<CreditCardStatement, Boolean> reviewed = new TableColumn<>("Revisado");
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(!data.getValue().isPendingReview()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    CreditCardStatement statement = getTableView().getItems().get(getIndex());
                    boolean isReviewed = checkBox.isSelected();
                    updateCreditCardStatementReview(statement, isReviewed);
                    getTableView().refresh();
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(Boolean.TRUE.equals(item));
                    setGraphic(checkBox);
                }
            }
        });

        TableColumn<CreditCardStatement, Double> minimum = cardMoneyColumn("Pago mínimo", CreditCardStatement::getMinimumPaymentDue, CreditCardStatement::setMinimumPaymentDue);
        TableColumn<CreditCardStatement, String> dueDate = cardDateColumn("Fecha límite de pago", CreditCardStatement::getPaymentDueDate, CreditCardStatement::setPaymentDueDate);
        TableColumn<CreditCardStatement, Double> previous = cardMoneyColumn("Saldo anterior", CreditCardStatement::getPreviousBalance, CreditCardStatement::setPreviousBalance);
        TableColumn<CreditCardStatement, Double> payments = cardMoneyColumn("Pagos", CreditCardStatement::getPayments, CreditCardStatement::setPayments);
        TableColumn<CreditCardStatement, Double> credits = cardMoneyColumn("Créditos", CreditCardStatement::getOtherCredits, CreditCardStatement::setOtherCredits);
        TableColumn<CreditCardStatement, Double> purchases = cardMoneyColumn("Compras", CreditCardStatement::getTransactions, CreditCardStatement::setTransactions);
        TableColumn<CreditCardStatement, Double> cash = cardMoneyColumn("Cash advances", CreditCardStatement::getCashAdvances, CreditCardStatement::setCashAdvances);
        TableColumn<CreditCardStatement, Double> fees = cardMoneyColumn("Fees", CreditCardStatement::getFeesCharged, CreditCardStatement::setFeesCharged);
        TableColumn<CreditCardStatement, Double> interest = cardMoneyColumn("Intereses", CreditCardStatement::getInterestCharged, CreditCardStatement::setInterestCharged);
        TableColumn<CreditCardStatement, Double> limit = cardMoneyColumn("Limite banco", CreditCardStatement::getCreditLimit, CreditCardStatement::setCreditLimit);
        TableColumn<CreditCardStatement, Double> available = cardMoneyColumn("Crédito disponible", CreditCardStatement::getAvailableCredit, CreditCardStatement::setAvailableCredit);
        TableColumn<CreditCardStatement, String> status = new TableColumn<>("Revisión");
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isPendingReview() ? "Pdte revision" : "OK"));
        TableColumn<CreditCardStatement, String> notes = new TableColumn<>("Notas");
        notes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReviewNotes()));
        notes.setCellFactory(TextFieldTableCell.forTableColumn());
        notes.setOnEditCommit(event -> event.getRowValue().setReviewNotes(event.getNewValue()));
        notes.setPrefWidth(260);
        TableColumn<CreditCardStatement, Void> delete = new TableColumn<>("Eliminar");
        delete.setCellFactory(column -> new TableCell<>() {
            private final Button button = new Button("Eliminar");
            {
                button.setOnAction(event -> {
                    CreditCardStatement statement = getTableView().getItems().get(getIndex());
                    if (statement.getId() > 0) {
                        creditCardStatementRepository.delete(statement.getId());
                    }
                    getTableView().getItems().remove(statement);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        });

        table.getColumns().setAll(year, month, startDate, endDate, newBalance, reviewed, minimum, dueDate, previous, payments, credits, purchases, cash, fees, interest, limit, available, status, notes, delete);
        return table;
    }

    private TableColumn<CreditCardStatement, Double> cardMoneyColumn(String title, java.util.function.ToDoubleFunction<CreditCardStatement> getter, java.util.function.BiConsumer<CreditCardStatement, Double> setter) {
        TableColumn<CreditCardStatement, Double> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleDoubleProperty(getter.applyAsDouble(data.getValue())).asObject());
        column.setCellFactory(TextFieldTableCell.forTableColumn(twoDecimalConverter()));
        column.setOnEditCommit(event -> setter.accept(event.getRowValue(), event.getNewValue()));
        column.setPrefWidth(120);
        return column;
    }

    private TableColumn<CreditCardStatement, String> cardDateColumn(String title, java.util.function.Function<CreditCardStatement, LocalDate> getter, java.util.function.BiConsumer<CreditCardStatement, LocalDate> setter) {
        TableColumn<CreditCardStatement, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue()) == null ? "" : getter.apply(data.getValue()).toString()));
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> setter.accept(event.getRowValue(), parseDateOrNull(event.getNewValue())));
        column.setPrefWidth(120);
        return column;
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        DateTimeFormatter[] formats = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            SHORT_DATE_FORMAT,
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("M/d/yy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yy")
        };
        RuntimeException last = null;
        for (DateTimeFormatter format : formats) {
            try {
                return LocalDate.parse(trimmed, format);
            } catch (RuntimeException exception) {
                last = exception;
            }
        }
        throw last == null ? new IllegalArgumentException("Fecha no valida: " + value) : last;
    }

    private String formatShortDate(LocalDate date) {
        return date == null ? "" : date.format(SHORT_DATE_FORMAT);
    }

    private StringConverter<Double> twoDecimalConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(Double value) {
                return value == null ? "" : String.format(java.util.Locale.US, "%.2f", value);
            }

            @Override
            public Double fromString(String value) {
                if (value == null || value.isBlank()) {
                    return 0.0;
                }
                return Money.parse(value);
            }
        };
    }

    private StringConverter<String> stringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(String value) {
                return value == null ? "" : value;
            }

            @Override
            public String fromString(String value) {
                return value == null ? "" : value;
            }
        };
    }

    private <S, T> javafx.util.Callback<TableColumn<S, T>, TableCell<S, T>> commitOnFocusLostCellFactory(StringConverter<T> converter) {
        return column -> new TableCell<>() {
            private TextField textField;

            @Override
            public void startEdit() {
                if (!isEditable() || !getTableView().isEditable() || !getTableColumn().isEditable()) {
                    return;
                }
                super.startEdit();
                if (textField == null) {
                    textField = new TextField();
                    textField.setOnAction(event -> commitCurrentEdit());
                    textField.setOnKeyPressed(event -> {
                        if (event.getCode() == KeyCode.ESCAPE) {
                            cancelEdit();
                        }
                    });
                    textField.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
                        if (wasFocused && !isFocused && isEditing()) {
                            commitCurrentEdit();
                        }
                    });
                }
                textField.setText(converter.toString(getItem()));
                setText(null);
                setGraphic(textField);
                textField.selectAll();
                textField.requestFocus();
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(converter.toString(getItem()));
                setGraphic(null);
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setGraphic(null);
                } else if (isEditing()) {
                    if (textField != null) {
                        textField.setText(converter.toString(item));
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(converter.toString(item));
                    setGraphic(null);
                }
            }

            private void commitCurrentEdit() {
                try {
                    commitEdit(converter.fromString(textField.getText()));
                } catch (RuntimeException exception) {
                    cancelEdit();
                    alert(Alert.AlertType.ERROR, "Valor no valido", "Revisa el valor introducido antes de guardar.");
                }
            }
        };
    }

    private TableView<CreditCardTransaction> creditCardTransactionTable() {
        TableView<CreditCardTransaction> table = new TableView<>();
        table.setEditable(true);

        TableColumn<CreditCardTransaction, String> transactionDate = cardTransactionDateColumn("Fecha", CreditCardTransaction::getTransactionDate, (movement, date) -> {
            movement.setTransactionDate(date);
            movement.setPostDate(date);
        });
        TableColumn<CreditCardTransaction, String> description = new TableColumn<>("Descripción");
        description.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        description.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        description.setOnEditCommit(event -> event.getRowValue().setDescription(event.getNewValue()));
        description.setPrefWidth(360);
        TableColumn<CreditCardTransaction, Double> amount = new TableColumn<>("Importe");
        amount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getAmount()).asObject());
        amount.setCellFactory(commitOnFocusLostCellFactory(twoDecimalConverter()));
        amount.setOnEditCommit(event -> event.getRowValue().setAmount(event.getNewValue()));
        amount.setPrefWidth(110);
        TableColumn<CreditCardTransaction, Boolean> reviewed = new TableColumn<>("Revisado");
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(!data.getValue().isPendingReview()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    CreditCardTransaction movement = getTableView().getItems().get(getIndex());
                    boolean isReviewed = checkBox.isSelected();
                    updateCreditCardMovementReview(movement, isReviewed);
                    getTableView().refresh();
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(Boolean.TRUE.equals(item));
                    setGraphic(checkBox);
                }
            }
        });
        TableColumn<CreditCardTransaction, String> type = new TableColumn<>("Tipo");
        type.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        type.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        type.setOnEditCommit(event -> event.getRowValue().setType(event.getNewValue()));
        TableColumn<CreditCardTransaction, String> category = new TableColumn<>("Categoría");
        category.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategory()));
        category.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        category.setOnEditCommit(event -> event.getRowValue().setCategory(event.getNewValue()));
        TableColumn<CreditCardTransaction, String> status = new TableColumn<>("Revisión");
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isPendingReview() ? "Pdte revision" : "OK"));
        TableColumn<CreditCardTransaction, String> notes = new TableColumn<>("Notas");
        notes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReviewNotes()));
        notes.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        notes.setOnEditCommit(event -> event.getRowValue().setReviewNotes(event.getNewValue()));
        notes.setPrefWidth(260);
        TableColumn<CreditCardTransaction, Void> delete = new TableColumn<>("Eliminar");
        delete.setCellFactory(column -> new TableCell<>() {
            private final Button button = new Button("Eliminar");
            {
                button.setOnAction(event -> {
                    CreditCardTransaction movement = getTableView().getItems().get(getIndex());
                    if (movement.getId() > 0) {
                        creditCardTransactionRepository.delete(movement.getId());
                    }
                    getTableView().getItems().remove(movement);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        });

        table.getColumns().setAll(transactionDate, description, amount, reviewed, type, category, status, notes, delete);
        return table;
    }

    private TableColumn<CreditCardTransaction, String> cardTransactionDateColumn(String title, java.util.function.Function<CreditCardTransaction, LocalDate> getter, java.util.function.BiConsumer<CreditCardTransaction, LocalDate> setter) {
        TableColumn<CreditCardTransaction, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(getter.apply(data.getValue()) == null ? "" : getter.apply(data.getValue()).toString()));
        column.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        column.setOnEditCommit(event -> setter.accept(event.getRowValue(), parseDateOrNull(event.getNewValue())));
        column.setPrefWidth(110);
        return column;
    }

    private TableView<NylRecord> nylTable() {
        TableView<NylRecord> table = new TableView<>();
        table.setEditable(true);
        table.setRowFactory(view -> new javafx.scene.control.TableRow<>() {
            @Override
            protected void updateItem(NylRecord item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeIf(style -> style.startsWith("month-row-"));
                if (!empty && item != null) {
                    getStyleClass().add("month-row-" + item.getMonth());
                }
            }
        });
        TableColumn<NylRecord, Integer> year = new TableColumn<>("Año");
        year.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getYear()).asObject());
        year.setCellFactory(commitOnFocusLostCellFactory(new IntegerStringConverter()));
        year.setOnEditCommit(event -> {
            event.getRowValue().setYear(event.getNewValue());
            nylRepository.recordCorrection(event.getRowValue().getFingerprint(), "year", String.valueOf(event.getOldValue()), String.valueOf(event.getNewValue()), "edicion en tabla");
        });
        TableColumn<NylRecord, Integer> month = new TableColumn<>("Mes");
        month.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getMonth()).asObject());
        month.setCellFactory(commitOnFocusLostCellFactory(new IntegerStringConverter()));
        month.setOnEditCommit(event -> {
            event.getRowValue().setMonth(event.getNewValue());
            nylRepository.recordCorrection(event.getRowValue().getFingerprint(), "month", String.valueOf(event.getOldValue()), String.valueOf(event.getNewValue()), "edicion en tabla");
        });
        TableColumn<NylRecord, String> concept = new TableColumn<>("Concepto");
        concept.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getConcept()));
        concept.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        concept.setOnEditCommit(event -> {
            event.getRowValue().setConcept(event.getNewValue());
            nylRepository.recordCorrection(event.getRowValue().getFingerprint(), "concept", event.getOldValue(), event.getNewValue(), "edicion en tabla");
        });
        concept.setPrefWidth(360);
        TableColumn<NylRecord, String> section = new TableColumn<>("Apartado");
        section.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSection()));
        section.setCellFactory(ComboBoxTableCell.forTableColumn(NYL_SECTION_OPTIONS));
        section.setOnEditCommit(event -> {
            event.getRowValue().setSection(event.getNewValue());
            event.getRowValue().setAmount(normalizedAmount(event.getRowValue(), event.getRowValue().getAmount()));
            nylRepository.recordCorrection(event.getRowValue().getFingerprint(), "section", event.getOldValue(), event.getNewValue(), "edicion en tabla");
            table.refresh();
        });
        TableColumn<NylRecord, String> type = new TableColumn<>("Tipo");
        type.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRecordType()));
        type.setCellFactory(ComboBoxTableCell.forTableColumn("credito", "deduccion"));
        type.setOnEditCommit(event -> {
            event.getRowValue().setRecordType(event.getNewValue());
            event.getRowValue().setAmount(normalizedAmount(event.getRowValue(), event.getRowValue().getAmount()));
            nylRepository.recordCorrection(event.getRowValue().getFingerprint(), "record_type", event.getOldValue(), event.getNewValue(), "edicion en tabla");
            table.refresh();
        });
        TableColumn<NylRecord, Double> amount = new TableColumn<>("Importe");
        amount.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getAmount()).asObject());
        amount.setCellFactory(commitOnFocusLostCellFactory(twoDecimalConverter()));
        amount.setOnEditCommit(event -> {
            event.getRowValue().setAmount(normalizedAmount(event.getRowValue(), event.getNewValue()));
            nylRepository.recordCorrection(event.getRowValue().getFingerprint(), "amount", String.valueOf(event.getOldValue()), String.valueOf(event.getNewValue()), "edicion en tabla");
            table.refresh();
        });
        TableColumn<NylRecord, Boolean> reviewed = new TableColumn<>("Revisado");
        reviewed.setCellValueFactory(data -> new SimpleBooleanProperty(!data.getValue().isPendingReview()).asObject());
        reviewed.setCellFactory(column -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.setOnAction(event -> {
                    NylRecord record = getTableView().getItems().get(getIndex());
                    boolean isReviewed = checkBox.isSelected();
                    record.setPendingReview(!isReviewed);
                    record.setReviewRequired(!isReviewed);
                    if (isReviewed && (record.getReviewNotes() == null || record.getReviewNotes().isBlank() || record.getReviewNotes().startsWith("OCR:"))) {
                        record.setReviewNotes("Revisado");
                    }
                    getTableView().refresh();
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(Boolean.TRUE.equals(item));
                    setGraphic(checkBox);
                }
            }
        });
        TableColumn<NylRecord, String> status = new TableColumn<>("Estado");
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getImportStatus()));
        TableColumn<NylRecord, String> review = new TableColumn<>("Revisión");
        review.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().isPendingReview() ? "Pdte revision" : "OK"));
        TableColumn<NylRecord, String> notes = new TableColumn<>("Notas");
        notes.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getReviewNotes()));
        notes.setCellFactory(commitOnFocusLostCellFactory(stringConverter()));
        notes.setOnEditCommit(event -> {
            event.getRowValue().setReviewNotes(event.getNewValue());
            nylRepository.recordCorrection(event.getRowValue().getFingerprint(), "review_notes", event.getOldValue(), event.getNewValue(), "edicion en tabla");
        });
        notes.setPrefWidth(260);
        TableColumn<NylRecord, Void> actions = new TableColumn<>("Acciones");
        actions.setCellFactory(column -> new TableCell<>() {
            private final Button save = new Button("Guardar");
            private final Button edit = new Button("Editar");
            private final Button delete = new Button("Eliminar");
            private final HBox buttons = new HBox(6, save, edit, delete);
            {
                save.setOnAction(event -> {
                    NylRecord record = getTableView().getItems().get(getIndex());
                    record.setAmount(normalizedAmount(record, record.getAmount()));
                    if (record.getId() > 0) {
                        nylRepository.updateRecord(record);
                    }
                    getTableView().refresh();
                });
                edit.setOnAction(event -> {
                    NylRecord record = getTableView().getItems().get(getIndex());
                    getTableView().getSelectionModel().select(record);
                    getTableView().scrollTo(record);
                    getTableView().requestFocus();
                    getTableView().edit(getIndex(), year);
                });
                delete.setOnAction(event -> {
                    NylRecord record = getTableView().getItems().get(getIndex());
                    if (record.getId() > 0) {
                        nylRepository.delete(record.getId());
                    }
                    getTableView().getItems().remove(record);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });
        actions.setPrefWidth(230);
        table.getColumns().setAll(year, month, section, concept, type, amount, reviewed, status, review, notes, actions);
        return table;
    }

    private double normalizedAmount(NylRecord record, double amount) {
        boolean deduction = "deduccion".equalsIgnoreCase(record.getRecordType()) || "Deducciones".equalsIgnoreCase(record.getSection());
        if (deduction) {
            return -Math.abs(amount);
        }
        return Math.abs(amount);
    }

    private record AgentLedgerTemplate(String kind, String label, String section, String type, String styleClass, boolean editable, boolean hasValues) {
        static AgentLedgerTemplate header(String label, String styleClass) {
            return new AgentLedgerTemplate("HEADER", label, "", "", styleClass, false, false);
        }

        static AgentLedgerTemplate section(String label) {
            return new AgentLedgerTemplate("SECTION", label, "", "", "agent-ledger-section-row", false, false);
        }

        static AgentLedgerTemplate input(String label, String section, String type) {
            return new AgentLedgerTemplate("INPUT", label, section, type, "agent-ledger-input-row", true, true);
        }

        static AgentLedgerTemplate total(String label, String styleClass) {
            return new AgentLedgerTemplate("TOTAL", label, "", "", styleClass, false, true);
        }

        static AgentLedgerTemplate grandTotal(String label, String styleClass) {
            return new AgentLedgerTemplate("GRAND", label, "", "", styleClass, false, true);
        }
    }

    private static class AgentLedgerRow {
        private final AgentLedgerTemplate template;
        private final Map<Integer, List<NylRecord>> recordsByMonth = new LinkedHashMap<>();
        private final double[] displayValues = new double[13];

        AgentLedgerRow(AgentLedgerTemplate template) {
            this.template = template;
            for (int month = 1; month <= 12; month++) {
                recordsByMonth.put(month, new ArrayList<>());
            }
        }

        AgentLedgerTemplate template() {
            return template;
        }

        String categoryText() {
            return "HEADER".equals(template.kind()) || "GRAND".equals(template.kind()) ? template.label() : "";
        }

        String conceptText() {
            return "HEADER".equals(template.kind()) || "GRAND".equals(template.kind()) ? "" : template.label();
        }

        List<NylRecord> records(int month) {
            return recordsByMonth.getOrDefault(month, List.of());
        }

        double rawValue(int month) {
            return records(month).stream().mapToDouble(NylRecord::getAmount).sum();
        }

        double displayValue(int month) {
            return displayValues[month];
        }

        void setDisplayValue(int month, double value) {
            displayValues[month] = value;
        }
    }

    private static class InternalMovement {
        private long stateId;
        private String sourceType;
        private long sourceId;
        private LocalDate date;
        private String from;
        private String to;
        private String type;
        private String description;
        private double amount;
        private String status;
        private boolean reviewed;
        private boolean manual;
        private boolean totalRow;
        private boolean editing;

        InternalMovement(long stateId, String sourceType, long sourceId, LocalDate date, String from, String to, String type,
                         String description, double amount, String status, boolean reviewed, boolean manual, boolean totalRow) {
            this.stateId = stateId;
            this.sourceType = sourceType;
            this.sourceId = sourceId;
            this.date = date;
            this.from = from;
            this.to = to;
            this.type = type;
            this.description = description;
            this.amount = amount;
            this.status = status;
            this.reviewed = reviewed;
            this.manual = manual;
            this.totalRow = totalRow;
        }

        LocalDate date() { return date; }
        String from() { return from; }
        String to() { return to; }
        String type() { return type; }
        String description() { return description; }
        double amount() { return amount; }
        String status() { return status; }
        long stateId() { return stateId; }
        String sourceType() { return sourceType; }
        long sourceId() { return sourceId; }
        boolean reviewed() { return reviewed; }
        boolean manual() { return manual; }
        boolean totalRow() { return totalRow; }
        boolean editing() { return editing; }
        void setStateId(long stateId) { this.stateId = stateId; }
        void setDate(LocalDate date) { this.date = date; }
        void setFrom(String from) { this.from = from; }
        void setTo(String to) { this.to = to; }
        void setDescription(String description) { this.description = description; }
        void setAmount(double amount) { this.amount = amount; }
        void setStatus(String status) { this.status = status; }
        void setReviewed(boolean reviewed) { this.reviewed = reviewed; }
        void setEditing(boolean editing) { this.editing = editing; }
    }

    private record NylImportPreview(List<NylRecord> records, String text) {
    }

    private TableView<ReconciliationItem> reconciliationTable() {
        TableView<ReconciliationItem> table = new TableView<>();
        TableColumn<ReconciliationItem, Number> year = new TableColumn<>("Año");
        year.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().year()));
        TableColumn<ReconciliationItem, Number> month = new TableColumn<>("Mes");
        month.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().month()));
        TableColumn<ReconciliationItem, String> bank = new TableColumn<>("Banco");
        bank.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().bankDescription()));
        bank.setPrefWidth(300);
        TableColumn<ReconciliationItem, String> nyl = new TableColumn<>("NYL");
        nyl.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().nylConcept()));
        nyl.setPrefWidth(260);
        TableColumn<ReconciliationItem, Number> difference = new TableColumn<>("Diferencia");
        difference.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().difference()));
        TableColumn<ReconciliationItem, String> status = new TableColumn<>("Estado");
        status.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status()));
        table.getColumns().setAll(year, month, bank, nyl, difference, status);
        return table;
    }

    private VBox page(String title, javafx.scene.Node... nodes) {
        Label heading = new Label(title);
        heading.getStyleClass().add("heading");
        VBox box = new VBox(18);
        box.getChildren().add(heading);
        box.getChildren().addAll(nodes);
        box.setPadding(new Insets(28));
        box.getStyleClass().add("page");
        return box;
    }

    private void setDarkHubPage(String title, javafx.scene.Node... nodes) {
        VBox hub = page(title, nodes);
        hub.getStyleClass().add("dark-hub-page");
        setPage(hub);
    }

    private Button backButton(String text, Runnable action) {
        Button button = new Button("← " + text);
        button.getStyleClass().add("back-button");
        button.setOnAction(event -> runUnlessImporting(action));
        return button;
    }

    private VBox actionHeader(javafx.scene.Node... rows) {
        VBox header = new VBox(10);
        header.getStyleClass().add("action-header");
        for (javafx.scene.Node row : rows) {
            if (row instanceof HBox hBox) {
                hBox.setAlignment(Pos.CENTER_LEFT);
                hBox.getStyleClass().add("action-row");
            }
            header.getChildren().add(row);
        }
        return header;
    }

    private Label helperNote(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-subtitle");
        label.setWrapText(true);
        return label;
    }

    private void setPage(Parent page) {
        ScrollPane scroll = new ScrollPane(page);
        if (page.getStyleClass().contains("dark-hub-page") || page.getStyleClass().contains("dashboard-page")) {
            scroll.getStyleClass().add("dark-page-scroll");
        }
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        content.getChildren().setAll(scroll);
    }

    private void alert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.getDialogPane().setMinWidth(560);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.toString() : current.getMessage();
    }

    private boolean confirm(String title, String message, String confirmText) {
        ButtonType confirm = new ButtonType(confirmText);
        Alert alert = new Alert(Alert.AlertType.WARNING, message, confirm, ButtonType.CANCEL);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.getDialogPane().setMinWidth(620);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == confirm;
    }

    private Optional<String> promptText(String title, String message) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        dialog.setContentText(message);
        dialog.getDialogPane().setMinWidth(560);
        dialog.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        return dialog.showAndWait();
    }
}
