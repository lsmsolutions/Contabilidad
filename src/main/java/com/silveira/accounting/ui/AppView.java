package com.silveira.accounting.ui;

import com.silveira.accounting.application.card.service.CardAccountApplicationService;
import com.silveira.accounting.application.mortgage.MortgageApplicationService;
import com.silveira.accounting.models.bank.BankAccount;
import com.silveira.accounting.models.CreditCardAccount;
import com.silveira.accounting.repositories.ReviewMarkRepository;
import com.silveira.accounting.ui.bank.BankModule;
import com.silveira.accounting.ui.bank.BankWorkflow;
import com.silveira.accounting.ui.card.CardWorkflow;
import com.silveira.accounting.ui.dashboard.DashboardWorkflow;
import com.silveira.accounting.ui.mortgage.HouseExpenseWorkflow;
import com.silveira.accounting.ui.mortgage.MortgageWorkflow;
import com.silveira.accounting.ui.nyl.NylWorkflow;
import com.silveira.accounting.ui.nyl.AgentLedgerWorkflow;
import com.silveira.accounting.ui.investment.InvestmentWorkflow;
import com.silveira.accounting.ui.internalmovement.InternalMovementWorkflow;
import com.silveira.accounting.ui.vehiclelease.VehicleLeaseWorkflow;
import com.silveira.accounting.ui.workspace.WorkspaceView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;

import java.io.InputStream;
import java.util.List;
import java.util.function.BooleanSupplier;

public class AppView {
    private final BankModule bankModule;
    private final BankWorkflow bankWorkflow;
    private final InvestmentWorkflow investmentWorkflow;
    private final VehicleLeaseWorkflow vehicleLeaseWorkflow;
    private final CardAccountApplicationService creditCardAccountRepository;
    private final CardWorkflow cardWorkflow;
    private final InternalMovementWorkflow internalMovementWorkflow;
    private final MortgageApplicationService mortgageApplication;
    private final HouseExpenseWorkflow houseExpenseWorkflow;
    private final MortgageWorkflow mortgageWorkflow;
    private final NylWorkflow nylWorkflow;
    private final AgentLedgerWorkflow agentLedgerWorkflow;
    private final ReviewMarkRepository reviewMarkRepository;
    private final DashboardWorkflow dashboardWorkflow;

    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();
    private final AppUiSupport ui = new AppUiSupport(
        () -> root.getScene() == null ? null : root.getScene().getWindow(),
        this::setPage,
        this::runUnlessImporting
    );
    private Node sidebar;
    private boolean sidebarVisible = true;
    private Integer selectedYearValue;
    private Integer selectedMonthValue;
    private String selectedBankAccountAlias;
    private boolean cardImportInProgress;
    private BooleanSupplier navigationGuard;
    private boolean bankMenuExpanded;
    private boolean cardMenuExpanded;
    private boolean mortgageMenuExpanded;
    private boolean investmentMenuExpanded;
    private boolean vehicleLeaseMenuExpanded;
    private boolean nylMenuExpanded;
    private boolean workspaceBackContext;

    public AppView(AppDependencies dependencies) {
        bankModule = dependencies.bankModule();
        bankWorkflow = dependencies.bankWorkflow();
        investmentWorkflow = dependencies.investmentWorkflow();
        vehicleLeaseWorkflow = dependencies.vehicleLeaseWorkflow();
        creditCardAccountRepository = dependencies.creditCardAccountRepository();
        cardWorkflow = dependencies.cardWorkflow();
        internalMovementWorkflow = dependencies.internalMovementWorkflow();
        mortgageApplication = dependencies.mortgageApplication();
        houseExpenseWorkflow = dependencies.houseExpenseWorkflow();
        mortgageWorkflow = dependencies.mortgageWorkflow();
        nylWorkflow = dependencies.nylWorkflow();
        agentLedgerWorkflow = dependencies.agentLedgerWorkflow();
        reviewMarkRepository = dependencies.reviewMarkRepository();
        dashboardWorkflow = dependencies.dashboardWorkflow();
        dependencies.actions().connect(actionsDelegate());
        build();
    }

    private AppViewActions.Delegate actionsDelegate() {
        return new AppViewActions.Delegate(
            () -> selectedYearValue,
            () -> selectedMonthValue,
            value -> selectedYearValue = value,
            value -> selectedMonthValue = value,
            this::setPage,
            ui::page,
            ui::setDarkHubPage,
            this::rebuildSidebar,
            () -> root.getScene() == null ? null : root.getScene().getWindow(),
            () -> selectedBankAccountAlias,
            alias -> selectedBankAccountAlias = alias,
            ui::alert,
            ui::rootCauseMessage,
            ui::showProcessing,
            ui::showProcessing,
            ui::confirm,
            ui::promptText,
            this::showMortgages,
            this::showNylHub,
            ui::backButton,
            ui::choosePdf,
            ui::chooseExcel,
            this::addReviewMark,
            ui::monthName,
            (year, month) -> {
                selectedYearValue = year;
                selectedMonthValue = month;
            },
            ui::helperNote,
            this::reviewMarkLabel,
            ui::parseDateOrNull,
            ui::safeFileName,
            importing -> cardImportInProgress = importing,
            () -> ui.commitOnFocusLostCellFactory(ui.stringConverter(), ui::alert),
            () -> ui.commitOnFocusLostCellFactory(ui.twoDecimalConverter(), ui::alert),
            this::showReview,
            ui::monthlyActionCard,
            ui::addMonthlyCardLine,
            ui::addMonthlyCardDivider,
            ui::monthlyExportButton,
            ui::miniTotal,
            ui::horizontalStatementScroll
        );
    }

    public Parent root() {
        return root;
    }

    private void build() {
        rebuildSidebar();
        root.setCenter(content);
        setSidebarVisible(false);
        showWorkspace();
    }

    private void rebuildSidebar() {
        sidebar = sidebar();
        root.setLeft(sidebarVisible ? sidebar : null);
    }

    private Node sidebar() {
        Node logo = createSidebarLogo();

        VBox bankSubmenu = new VBox(4);
        bankSubmenu.getStyleClass().add("submenu");
        bankSubmenu.getChildren().add(ledgerSubnav("Bank Ledger", this::showBankLedger));
        for (BankAccount account : bankModule.accounts().list()) {
            bankSubmenu.getChildren().add(subnav(account.getAlias(), () -> showBankAccount(account.getAlias())));
        }
        VBox cardSubmenu = new VBox(4);
        cardSubmenu.getStyleClass().add("submenu");
        cardSubmenu.getChildren().add(ledgerSubnav("Card Ledger", this::showCardLedger));
        for (CreditCardAccount account : creditCardAccountRepository.findAll()) {
            cardSubmenu.getChildren().add(subnav(account.getAlias(), () -> showCardAccount(account.getAlias())));
        }
        VBox mortgageSubmenu = new VBox(4);
        mortgageSubmenu.getStyleClass().add("submenu");
        mortgageSubmenu.getChildren().add(ledgerSubnav("Mortgage Ledger", this::showMortgageLedger));
        for (String alias : mortgageApplication.statements().findLoanAliases()) {
            mortgageSubmenu.getChildren().add(subnav(alias, () -> showMortgageDetail(alias)));
        }
        VBox vehicleLeaseSubmenu = new VBox(4);
        vehicleLeaseSubmenu.getStyleClass().add("submenu");
        vehicleLeaseSubmenu.getChildren().add(ledgerSubnav("Vehicle Ledger", vehicleLeaseWorkflow::showVehicleLedger));
        for (var account : vehicleLeaseWorkflow.accounts()) {
            vehicleLeaseSubmenu.getChildren().add(subnav(account.getAlias(), () -> vehicleLeaseWorkflow.showVehicleLeaseDetail(account.getAlias())));
        }
        VBox investmentSubmenu = new VBox(4);
        investmentSubmenu.getStyleClass().add("submenu");
        for (var account : investmentWorkflow.accounts()) {
            investmentSubmenu.getChildren().add(subnav(account.getAlias(), () -> investmentWorkflow.showInvestmentDetail(account.getAlias())));
        }
        VBox nylSubmenu = new VBox(4);
        nylSubmenu.getStyleClass().add("submenu");
        nylSubmenu.getChildren().add(subnav("Resumen NYL", this::showNyl));
        nylSubmenu.getChildren().add(subnav("Análisis NYL", this::showAnalysis));
        nylSubmenu.getChildren().add(ledgerSubnav("Agent Ledger", agentLedgerWorkflow::showAgentLedger));

        VBox menu = new VBox(8);
        menu.getChildren().addAll(
            nav("Inicio", this::showWorkspace),
            nav("Dashboard", this::showDashboard),
            collapsibleNav("Banco", this::showBank, bankSubmenu, () -> bankMenuExpanded, value -> bankMenuExpanded = value),
            collapsibleNav("Tarjetas", this::showCards, cardSubmenu, () -> cardMenuExpanded, value -> cardMenuExpanded = value),
            collapsibleNav("Hipotecas", this::showMortgages, mortgageSubmenu, () -> mortgageMenuExpanded, value -> mortgageMenuExpanded = value),
            collapsibleNav("Inversiones", investmentWorkflow::showInvestments, investmentSubmenu, () -> investmentMenuExpanded, value -> investmentMenuExpanded = value),
            collapsibleNav("Vehicle Leases", vehicleLeaseWorkflow::showVehicleLeases, vehicleLeaseSubmenu, () -> vehicleLeaseMenuExpanded, value -> vehicleLeaseMenuExpanded = value),
            nav("Movimientos internos", internalMovementWorkflow::showInternalMovements),
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
        button.setOnAction(event -> runUnlessImporting(() -> {
            workspaceBackContext = false;
            action.run();
        }));
        return button;
    }

    private Button ledgerSubnav(String text, Runnable action) {
        Button button = subnav(text, action);
        button.getStyleClass().add("ledger-subnav-button");
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
                workspaceBackContext = false;
                action.run();
            } catch (RuntimeException exception) {
                ui.alert(Alert.AlertType.ERROR, "No se pudo abrir " + text, ui.rootCauseMessage(exception));
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
        ui.alert(Alert.AlertType.WARNING, "Importación en curso", "No salgas de esta pantalla hasta que termine la importación del PDF. Puede demorar unos minutos.");
        return true;
    }

    private void showWorkspace() {
        workspaceBackContext = false;
        setPage(new WorkspaceView().build(
            this::toggleSidebar,
            List.of(
                new WorkspaceView.LinkGroup("Financial", List.of(
                    new WorkspaceView.WorkspaceLink("Banco", () -> openFromWorkspace(this::showBank)),
                    new WorkspaceView.WorkspaceLink("Tarjetas", () -> openFromWorkspace(this::showCards)),
                    new WorkspaceView.WorkspaceLink("Hipotecas", () -> openFromWorkspace(this::showMortgages)),
                    new WorkspaceView.WorkspaceLink("Inversiones", () -> openFromWorkspace(investmentWorkflow::showInvestments)),
                    new WorkspaceView.WorkspaceLink("Vehicle Leases", () -> openFromWorkspace(vehicleLeaseWorkflow::showVehicleLeases))
                )),
                new WorkspaceView.LinkGroup("Operations", List.of(
                    new WorkspaceView.WorkspaceLink("Movimientos internos", () -> openFromWorkspace(internalMovementWorkflow::showInternalMovements)),
                    new WorkspaceView.WorkspaceLink("Dashboard", () -> openFromWorkspace(this::showDashboard))
                )),
                new WorkspaceView.LinkGroup("Insurance & Commissions", List.of(
                    new WorkspaceView.WorkspaceLink("New York Life", () -> openFromWorkspace(this::showNylHub))
                ))
            )
        ));
    }

    private void openFromWorkspace(Runnable action) {
        workspaceBackContext = true;
        action.run();
    }

    private void toggleSidebar() {
        setSidebarVisible(!sidebarVisible);
    }

    private void setSidebarVisible(boolean visible) {
        sidebarVisible = visible;
        root.setLeft(visible ? sidebar : null);
    }

    private void showDashboard() {
        dashboardWorkflow.showDashboard();
    }

    private void showBank() {
        bankWorkflow.showBank();
    }

    private void showBankLedger() {
        bankWorkflow.showBankLedger();
    }

    private void showBankAccount(String accountAlias) {
        bankWorkflow.showBankAccount(accountAlias);
    }

    private void showCards() {
        cardWorkflow.showCards();
    }

    private void showCardLedger() {
        cardWorkflow.showCardLedger();
    }

    private void showCardAccount(String alias) {
        cardWorkflow.showCardAccount(alias);
    }

    private void showMortgages() {
        mortgageWorkflow.showMortgages();
    }

    private void showMortgageLedger() {
        mortgageWorkflow.showMortgageLedger();
    }

    private void showMortgageDetail(String alias) {
        mortgageWorkflow.showMortgageDetail(alias);
    }

    private void showNylHub() {
        nylWorkflow.showNylHub();
    }

    private void showNyl() {
        nylWorkflow.showNyl();
    }

    private void showAnalysis() {
        nylWorkflow.showAnalysis();
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
            setPage(ui.page(title, new Label("Revise y corrija los datos antes de guardar."), table, new HBox(10, save, cancel)));
        } else {
            setPage(ui.page(title, new Label("Revise y corrija los datos antes de guardar."), warningNode, table, new HBox(10, save, cancel)));
        }
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

    private void setPage(Parent page) {
        Parent displayedPage = workspaceBackContext && !page.getStyleClass().contains("workspace-page")
            ? pageWithWorkspaceBackButton(page)
            : page;
        if (displayedPage.getStyleClass().contains("self-scroll-page")) {
            content.getChildren().setAll(displayedPage);
            return;
        }
        ScrollPane scroll = new ScrollPane(displayedPage);
        if (displayedPage.getStyleClass().contains("dark-hub-page") || displayedPage.getStyleClass().contains("dashboard-page")) {
            scroll.getStyleClass().add("dark-page-scroll");
        }
        scroll.setFitToWidth(!displayedPage.getStyleClass().contains("horizontal-page"));
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setFitToHeight(false);
        content.getChildren().setAll(scroll);
    }

    private Parent pageWithWorkspaceBackButton(Parent page) {
        Button backToWorkspace = ui.backButton("Volver a Inicio", this::showWorkspace);
        if (page instanceof VBox vBox) {
            boolean alreadyAdded = vBox.getChildren().stream()
                .anyMatch(node -> node.getStyleClass().contains("workspace-back-button"));
            if (!alreadyAdded) {
                backToWorkspace.getStyleClass().add("workspace-back-button");
                vBox.getChildren().add(0, backToWorkspace);
            }
            return vBox;
        }
        VBox wrapper = new VBox(18, backToWorkspace, page);
        backToWorkspace.getStyleClass().add("workspace-back-button");
        wrapper.setPadding(new Insets(28));
        wrapper.getStyleClass().add("page");
        return wrapper;
    }

}
