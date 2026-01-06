package org.example.utils;

import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SCTMobileApp extends Application {

    // 📱 Screen dimensions configuration
    private static final double PHONE_WIDTH = 375;
    private static final double PHONE_HEIGHT = 720;
    private static final double DRAWER_WIDTH = 280; // Drawer menu width

    // 💾 Cloud Database Configuration (TiDB Cloud)
    private static Connection globalConnection;
    private static final String DB_URL = "jdbc:mysql://gateway01.ap-southeast-1.prod.aws.tidbcloud.com:4000/test?useSSL=true&enabledTLSProtocols=TLSv1.2,TLSv1.3";
    private static final String DB_USER = "3kaEmNyc5ZPLN5i.root";
    private static final String DB_PASS = "Xy2JQNOeLMxiqjFU";

    private static final String SESSION_FILE = "sct_session.dat";
    private static String currentUser = null;

    // 🖥️ Global UI Components
    private Stage primaryStage;
    private StackPane rootStackPane;
    private Label globalFooterLabel;
    private Region globalBackgroundRegion;
    private Label drawerGreetingLabel; // Label to update sidebar greeting from anywhere
    private BorderPane mainLayout;
    private StackPane contentAreaStack;
    private VBox drawerMenu;
    private Region drawerOverlay;
    private boolean isDrawerOpen = false;

    private Integer currentEditingId = null;

    // 📝 Form Controls
    private ComboBox<String> typeBox;
    private TextField amountField;
    private Label unitLabel;
    private Button saveBtn;
    private Label statusLabel;
    private Button cancelEditBtn;

    // 🌲 Environmental Impact Controls
    private ProgressBar dailyLimitBar;
    private Label dailyProgressLabel;
    private Label treeEquivalentLabel;
    private static final double DAILY_LIMIT = 20.0;

    /**
     * Display an alert with an English "OK" button
     */
    private void showEnglishAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);

        // Core trick: Clear default buttons and replace with English ones
        alert.getButtonTypes().clear();
        alert.getButtonTypes().add(new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));

        alert.showAndWait();
    }

    // =================================================================================
    // 🎨 Style Constants
    // =================================================================================
    private static final String SEMI_TRANSPARENT_BG = "-fx-background-color: rgba(255, 255, 255, 0.6);";
    private static final String SLIGHTLY_TRANSPARENT_BG = "-fx-background-color: rgba(240, 242, 245, 0.7);";

    private static final Background HEADER_BG = new Background(new BackgroundFill(
            new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#56ab2f")), new Stop(1, Color.web("#a8e063"))),
            CornerRadii.EMPTY, Insets.EMPTY));

    private static final String MAIN_CARD_STYLE =
            "-fx-background-color: rgba(255, 255, 255, 0.7); -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);";

    private static final String IMPACT_CARD_STYLE =
            "-fx-background-color: linear-gradient(to right, rgba(255,255,255,0.7), rgba(241,248,233,0.7)); -fx-background-radius: 15; -fx-border-color: rgba(165,214,167,0.5); -fx-border-radius: 15;";

    private static final String INPUT_STYLE = "-fx-background-radius: 10; -fx-background-color: rgba(255, 255, 255, 0.5); -fx-border-color: transparent; -fx-padding: 10;";

    private static final String BTN_PRIMARY_STYLE = "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;";
    private static final String BTN_INFO_STYLE = "-fx-background-color: #0288D1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;";
    private static final String BTN_WARN_STYLE = "-fx-background-color: #F57C00; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;";
    private static final String BTN_DANGER_STYLE = "-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;";
    private static final String BTN_AI_STYLE = "-fx-background-color: #7B1FA2; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;";

    // Helper method: Unified cloud database connection (Singleton Pattern)
    private Connection getConnection() throws SQLException {
        if (globalConnection == null || globalConnection.isClosed()) {
            globalConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        }
        return globalConnection;
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        initDatabase();
        stage.setTitle("SCT Mobile");
        stage.setResizable(false);

        rootStackPane = new StackPane();

        // Background configuration
        globalBackgroundRegion = new Region();
        try {
            Image bgImage = new Image(getClass().getResource("/background.jpg").toExternalForm());
            if (bgImage.isError()) {
                LinearGradient fallbackGradient = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0.0, Color.web("#134E5E")), new Stop(1.0, Color.web("#71B280")));
                globalBackgroundRegion.setBackground(new Background(new BackgroundFill(fallbackGradient, CornerRadii.EMPTY, Insets.EMPTY)));
            } else {
                BackgroundImage bImage = new BackgroundImage(
                        bgImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER, new BackgroundSize(1.0, 1.0, true, true, false, true)
                );
                globalBackgroundRegion.setBackground(new Background(bImage));
            }
        } catch (Exception e) { e.printStackTrace(); }

        globalFooterLabel = new Label("Developed by Team Horizon");
        globalFooterLabel.setFont(Font.font("Segoe UI", 12));
        globalFooterLabel.setTextFill(Color.web("#FFD700"));
        globalFooterLabel.setPadding(new Insets(0, 0, 20, 0));
        StackPane.setAlignment(globalFooterLabel, Pos.BOTTOM_CENTER);

        rootStackPane.getChildren().addAll(globalBackgroundRegion, globalFooterLabel);

        Scene scene = new Scene(rootStackPane, PHONE_WIDTH, PHONE_HEIGHT);
        stage.setScene(scene);

        String savedUser = loadSession();
        if (savedUser != null && !savedUser.isEmpty()) {
            currentUser = savedUser;
            showMainDashboard();
        } else {
            showLoginScreen();
        }
        stage.show();
    }

    private void switchView(Node newView) {
        newView.setStyle("-fx-background-color: transparent;");
        ObservableList<Node> children = rootStackPane.getChildren();
        Node currentView = children.size() > 2 ? children.get(2) : null;

        if (currentView != null) {
            newView.setTranslateX(PHONE_WIDTH);
            if (!children.contains(newView)) children.add(newView);

            TranslateTransition slideIn = new TranslateTransition(Duration.seconds(0.35), newView);
            slideIn.setToX(0);
            slideIn.setInterpolator(Interpolator.EASE_BOTH);

            TranslateTransition slideOut = new TranslateTransition(Duration.seconds(0.35), currentView);
            slideOut.setToX(-PHONE_WIDTH);
            slideOut.setInterpolator(Interpolator.EASE_BOTH);

            ParallelTransition transition = new ParallelTransition(slideIn, slideOut);
            transition.setOnFinished(e -> children.remove(currentView));
            transition.play();
        } else {
            children.add(newView);
        }
    }

    private void showMainDashboard() {
        mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: transparent;");

        Button menuBtn = new Button("☰");
        menuBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 22px; -fx-cursor: hand; -fx-padding: 0 15 0 0;");
        menuBtn.setOnAction(e -> toggleDrawer());

        Label titleLabel = new Label("SCT Tracker");
        titleLabel.setTextFill(Color.WHITE);
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(5, menuBtn, titleLabel, spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setBackground(HEADER_BG);

        initDrawerMenu();

        contentAreaStack = new StackPane();
        drawerOverlay = new Region();
        drawerOverlay.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
        drawerOverlay.setVisible(false);
        drawerOverlay.setOpacity(0);
        drawerOverlay.setOnMouseClicked(e -> toggleDrawer());

        contentAreaStack.getChildren().addAll(createDashboardView(), drawerOverlay, drawerMenu);
        StackPane.setAlignment(drawerMenu, Pos.TOP_LEFT);

        mainLayout.setTop(header);
        mainLayout.setCenter(contentAreaStack);

        switchView(mainLayout);
        primaryStage.centerOnScreen();

        // Enable Cloud Sync: Refresh data every 10 seconds
        Timeline cloudSync = new Timeline(new KeyFrame(Duration.seconds(10), e -> updateDailyStatus()));
        cloudSync.setCycleCount(Animation.INDEFINITE);
        cloudSync.play();
    }

    private void initDrawerMenu() {
        drawerMenu = new VBox(15);
        drawerMenu.setPrefWidth(DRAWER_WIDTH);
        drawerMenu.setMaxWidth(DRAWER_WIDTH);
        drawerMenu.setStyle("-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 0);");
        drawerMenu.setPadding(new Insets(30, 20, 20, 20));

        drawerGreetingLabel = new Label("Hi, " + getDisplayNickname(currentUser));
        drawerGreetingLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        drawerGreetingLabel.setTextFill(Color.web("#2E7D32"));

        VBox navBox = new VBox(5);

        navBox.getChildren().addAll(
                createMenuBtn("🏠 Dashboard", e -> { changeSubView(createDashboardView()); toggleDrawer(); }),
                createMenuBtn("👤 Profile", e -> { changeSubView(createProfileView()); toggleDrawer(); }),
                createMenuBtn("🏆 Leaderboard", e -> { changeSubView(createLeaderboardView()); toggleDrawer(); }),
                createMenuBtn("📋 Records", e -> { changeSubView(createHistoryView()); toggleDrawer(); }),
                createMenuBtn("📊 Analysis", e -> { changeSubView(createChartView()); toggleDrawer(); }),
                createMenuBtn("🤖 AI Advisor", e -> { changeSubView(createAIAdvisorView()); toggleDrawer(); })
        );
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("🚪 Logout");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setStyle("-fx-background-color: #fce4ec; -fx-text-fill: #d32f2f; -fx-font-weight: bold; -fx-padding: 12; -fx-background-radius: 10; -fx-cursor: hand;");

        logoutBtn.setOnAction(e -> {
            ButtonType confirmBtn = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Logout");
            alert.setHeaderText(null);
            alert.setContentText("Are you sure you want to logout?");
            alert.getButtonTypes().setAll(confirmBtn, cancelBtn);

            Optional<ButtonType> result = alert.showAndWait();

            if (result.isPresent() && result.get() == confirmBtn) {
                toggleDrawer();
                clearSession();
                currentUser = null;
                showLoginScreen();
            }
        });

        drawerMenu.getChildren().addAll(drawerGreetingLabel, new Separator(), navBox, spacer, logoutBtn);
        drawerMenu.setTranslateX(-DRAWER_WIDTH);
    }

    private VBox createProfileView() {
        VBox layout = new VBox(20);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setStyle(SLIGHTLY_TRANSPARENT_BG);

        // --- Top Navigation ---
        HBox topBar = new HBox();
        Button backBtn = new Button("Back");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #134E5E; -fx-font-weight: bold; -fx-cursor: hand;");
        backBtn.setOnAction(e -> changeSubView(createDashboardView()));
        topBar.getChildren().add(backBtn);

        // --- Avatar Area ---
        String currentDisplayName = getDisplayNickname(currentUser);

        Circle avatarBg = new Circle(45, Color.web("#E8F5E9"));
        avatarBg.setStroke(Color.web("#2E7D32"));
        avatarBg.setStrokeWidth(2);
        Label initialLabel = new Label(currentDisplayName.substring(0, 1).toUpperCase());
        initialLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 35));
        initialLabel.setTextFill(Color.web("#2E7D32"));
        StackPane avatarStack = new StackPane(avatarBg, initialLabel);

        // --- Profile Card ---
        VBox profileCard = new VBox(15);
        profileCard.setStyle(MAIN_CARD_STYLE + "-fx-background-color: #FFFFFF;");
        profileCard.setPadding(new Insets(25));
        profileCard.setMaxWidth(340);
        profileCard.setAlignment(Pos.CENTER);

        // 1. Name display and nickname editing
        HBox nameBox = new HBox(10);
        nameBox.setAlignment(Pos.CENTER);
        Label nameLabel = new Label(currentDisplayName);
        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        nameLabel.setTextFill(Color.BLACK);

        Button editNameBtn = new Button();
        // Use SVG Icon
        javafx.scene.shape.SVGPath editIcon = new javafx.scene.shape.SVGPath();
        editIcon.setContent("M3 17.25V21h3.75L17.81 9.93l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z");
        editIcon.setFill(Color.web("#757575"));
        editIcon.setScaleX(0.8);
        editIcon.setScaleY(0.8);
        editNameBtn.setGraphic(editIcon);
        editNameBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 0 0 0 5;");
        editNameBtn.setTooltip(new Tooltip("Edit Nickname"));

        // --- Nickname Modification Logic ---
        editNameBtn.setOnAction(e -> {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Edit Profile");
            dialog.setHeaderText("Change Display Name");

            ButtonType okBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(okBtn, cancelBtn);

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField nicknameField = new TextField();
            nicknameField.setText(currentDisplayName);
            nicknameField.setPromptText("Enter new nickname");

            grid.add(new Label("Nickname:"), 0, 0);
            grid.add(nicknameField, 1, 0);
            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == okBtn) return nicknameField.getText();
                return null;
            });

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                String newNickname = name.trim();

                if (newNickname.isEmpty()) {
                    showEnglishAlert(Alert.AlertType.WARNING, "Warning", "Nickname cannot be empty!");
                    return;
                }
                if (newNickname.equals(currentDisplayName)) return;

                if (isNicknameTaken(newNickname, currentUser)) {
                    showEnglishAlert(Alert.AlertType.ERROR, "Duplicate Nickname",
                            "The nickname '" + newNickname + "' is already taken.\nPlease choose another one.");
                    return;
                }

                if (updateNicknameInDB(currentUser, newNickname)) {
                    nameLabel.setText(newNickname);
                    initialLabel.setText(newNickname.substring(0, 1).toUpperCase());

                    if (drawerGreetingLabel != null) {
                        drawerGreetingLabel.setText("Hi, " + newNickname);
                    }
                    showEnglishAlert(Alert.AlertType.INFORMATION, "Success", "Nickname updated successfully!");
                } else {
                    showEnglishAlert(Alert.AlertType.ERROR, "Error", "Failed to update nickname.");
                }
            });
        });

        nameBox.getChildren().addAll(nameLabel, editNameBtn);

        // 2. Change Password Button
        Button changePassBtn = new Button("🔒 Change Password");
        changePassBtn.setStyle("-fx-background-color: #FFF3E0; -fx-text-fill: #E65100; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold;");
        changePassBtn.setMaxWidth(Double.MAX_VALUE);

        changePassBtn.setOnAction(e -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Security Check");
            dialog.setHeaderText("Update Password");

            PasswordField oldPassField = new PasswordField(); oldPassField.setPromptText("Current Password");
            PasswordField newPassField = new PasswordField(); newPassField.setPromptText("New Password");
            PasswordField confirmPassField = new PasswordField(); confirmPassField.setPromptText("Confirm New Password");

            GridPane grid = new GridPane();
            grid.setHgap(10); grid.setVgap(10);
            grid.setPadding(new Insets(20, 150, 10, 10));
            grid.add(new Label("Old Password:"), 0, 0); grid.add(oldPassField, 1, 0);
            grid.add(new Label("New Password:"), 0, 1); grid.add(newPassField, 1, 1);
            grid.add(new Label("Confirm Password:"), 0, 2); grid.add(confirmPassField, 1, 2);

            dialog.getDialogPane().setContent(grid);

            ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(okButtonType, cancelButtonType);

            Optional<ButtonType> result = dialog.showAndWait();

            if (result.isPresent() && result.get() == okButtonType) {
                String oldPass = oldPassField.getText();
                String newPass = newPassField.getText();
                String confirmPass = confirmPassField.getText();

                if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                    showEnglishAlert(Alert.AlertType.WARNING, "Warning", "All fields are required!"); return;
                }
                if (!newPass.equals(confirmPass)) {
                    showEnglishAlert(Alert.AlertType.ERROR, "Error", "New passwords do not match!"); return;
                }
                if (!verifyCurrentPassword(currentUser, oldPass)) {
                    showEnglishAlert(Alert.AlertType.ERROR, "Error", "Incorrect old password!"); return;
                }
                if (updatePasswordInDB(currentUser, newPass)) {
                    showEnglishAlert(Alert.AlertType.INFORMATION, "Success", "Password updated successfully!");
                } else {
                    showEnglishAlert(Alert.AlertType.ERROR, "Error", "Database connection error.");
                }
            }
        });

        // 3. Statistics Data
        int totalLogs = 0;
        double totalEmission = 0.0;
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*), SUM(emission) FROM carbon_logs WHERE username = ?")) {
            pstmt.setString(1, currentUser);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                totalLogs = rs.getInt(1);
                totalEmission = rs.getDouble(2);
            }
        } catch (SQLException e) { e.printStackTrace(); }

        Label statsTitle = new Label("Your Statistics");
        statsTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        statsTitle.setTextFill(Color.web("#2E7D32"));
        statsTitle.setPadding(new Insets(10, 0, 5, 0));

        VBox statsBox = new VBox(8);
        statsBox.setAlignment(Pos.CENTER);
        Label logsLabel = new Label("Total Records: " + totalLogs);
        logsLabel.setFont(Font.font(14));
        Label totalEmisLabel = new Label("Lifetime CO2: " + String.format("%.2f kg", totalEmission));
        totalEmisLabel.setFont(Font.font(14));
        statsBox.getChildren().addAll(logsLabel, totalEmisLabel);

        profileCard.getChildren().addAll(nameBox, changePassBtn, new Separator(), statsTitle, statsBox);
        Label tipLabel = new Label("Username (ID): " + currentUser);
        tipLabel.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 12));
        tipLabel.setTextFill(Color.GRAY);

        layout.getChildren().addAll(topBar, avatarStack, profileCard, tipLabel);
        return layout;
    }

    private VBox createLeaderboardView() {
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setStyle("-fx-background-color: transparent;");

        HBox topBar = new HBox();
        Button backBtn = new Button("Back");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #134E5E; -fx-font-weight: bold; -fx-cursor: hand;");
        backBtn.setOnAction(e -> changeSubView(createDashboardView()));
        topBar.getChildren().add(backBtn);

        HBox titleBox = new HBox(10);
        titleBox.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("🏆 Carbon Ranking");
        titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 24));
        titleLabel.setTextFill(Color.web("#2E7D32"));

        Button ruleBtn = new Button("ⓘ");
        ruleBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2E7D32; -fx-font-size: 18px; -fx-font-weight: bold; -fx-cursor: hand;");
        ruleBtn.setTooltip(new Tooltip("Ranking Rules"));

        ruleBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Ranking Rule");
            alert.setHeaderText("How is the rank calculated?");
            alert.setContentText("Ranking Rule: \nThe LOWER your total carbon emission, the HIGHER your rank! \n\nLet's aim for the 1st place by reducing emissions! 🌿");
            alert.getButtonTypes().clear();
            alert.getButtonTypes().add(new ButtonType("OK", ButtonBar.ButtonData.OK_DONE));
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-font-family: 'Segoe UI';");
            alert.showAndWait();
        });

        titleBox.getChildren().addAll(titleLabel, ruleBtn);

        HBox toggleGroupContainer = new HBox(10);
        toggleGroupContainer.setAlignment(Pos.CENTER);
        ToggleButton btnDay = new ToggleButton("Day");
        ToggleButton btnWeek = new ToggleButton("Week");
        ToggleButton btnMonth = new ToggleButton("Month");
        ToggleGroup group = new ToggleGroup();
        btnDay.setToggleGroup(group); btnWeek.setToggleGroup(group); btnMonth.setToggleGroup(group);
        btnWeek.setSelected(true);

        String tStyleSel = "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 5 15 5 15;";
        String tStyleUns = "-fx-background-color: rgba(255,255,255,0.6); -fx-text-fill: #333; -fx-background-radius: 15; -fx-cursor: hand; -fx-padding: 5 15 5 15;";
        btnDay.setStyle(tStyleUns); btnWeek.setStyle(tStyleSel); btnMonth.setStyle(tStyleUns);

        VBox rankListContainer = new VBox(10);
        rankListContainer.setAlignment(Pos.TOP_CENTER);

        Runnable refreshRank = () -> {
            rankListContainer.getChildren().clear();
            btnDay.setStyle(btnDay.isSelected() ? tStyleSel : tStyleUns);
            btnWeek.setStyle(btnWeek.isSelected() ? tStyleSel : tStyleUns);
            btnMonth.setStyle(btnMonth.isSelected() ? tStyleSel : tStyleUns);

            String timeFilter;
            if (btnDay.isSelected()) timeFilter = "DATE(created_at) = CURDATE()";
            else if (btnMonth.isSelected()) timeFilter = "DATE_FORMAT(created_at, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')";
            else timeFilter = "created_at >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)";

            String sql = "SELECT username, SUM(emission) as total FROM carbon_logs WHERE " + timeFilter + " GROUP BY username ORDER BY total ASC";

            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                int rank = 1;
                while (rs.next()) {
                    String username = rs.getString("username"); // Get original username
                    double total = rs.getDouble("total");

                    // Convert username to nickname
                    String displayName = getDisplayNickname(username);

                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);
                    row.setPadding(new Insets(12, 15, 12, 15));

                    String baseCardStyle = "-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 4);";
                    if (username.equals(currentUser)) {
                        row.setStyle(baseCardStyle + "-fx-border-color: #2E7D32; -fx-border-width: 2; -fx-border-radius: 15;");
                    } else {
                        row.setStyle(baseCardStyle);
                    }

                    Label lblRank = new Label();
                    lblRank.setPrefWidth(80);
                    lblRank.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

                    if (rank == 1) {
                        lblRank.setText("🥇 1st");
                        lblRank.setStyle("-fx-text-fill: #FFD700;");
                    } else if (rank == 2) {
                        lblRank.setText("🥈 2nd");
                        lblRank.setStyle("-fx-text-fill: #A0A0A0;");
                    } else if (rank == 3) {
                        lblRank.setText("🥉 3rd");
                        lblRank.setStyle("-fx-text-fill: #CD7F32;");
                    } else {
                        lblRank.setText(rank + ".");
                        lblRank.setStyle("-fx-text-fill: #333333;");
                    }

                    Label lblName = new Label(displayName);
                    lblName.setStyle("-fx-text-fill: black; -fx-font-size: 15px; -fx-font-weight: bold;");
                    HBox.setHgrow(lblName, Priority.ALWAYS);

                    Label lblVal = new Label(String.format("%.2f kg", total));
                    lblVal.setStyle("-fx-text-fill: black; -fx-font-size: 15px; -fx-font-weight: bold;");

                    row.getChildren().addAll(lblRank, lblName, lblVal);
                    rankListContainer.getChildren().add(row);
                    rank++;
                }
                if (rank == 1) {
                    Label noData = new Label("No records found.");
                    noData.setTextFill(Color.BLACK);
                    rankListContainer.getChildren().add(noData);
                }
            } catch (SQLException e) { e.printStackTrace(); }
        };

        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> { if (newVal != null) refreshRank.run(); });
        toggleGroupContainer.getChildren().addAll(btnDay, btnWeek, btnMonth);
        refreshRank.run();

        ScrollPane scroll = new ScrollPane(rankListContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        layout.getChildren().addAll(topBar, titleBox, toggleGroupContainer, new Separator(), scroll);
        return layout;
    }

    private Button createMenuBtn(String text, javafx.event.EventHandler<javafx.event.ActionEvent> action) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #333; -fx-font-size: 16px; -fx-padding: 12; -fx-cursor: hand; -fx-background-radius: 8;");
        btn.setOnAction(action);
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + "-fx-background-color: #f1f8e9;"));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace("-fx-background-color: #f1f8e9;", "")));
        return btn;
    }

    private void toggleDrawer() {
        TranslateTransition slide = new TranslateTransition(Duration.seconds(0.3), drawerMenu);
        FadeTransition fade = new FadeTransition(Duration.seconds(0.3), drawerOverlay);

        if (!isDrawerOpen) {
            drawerOverlay.setVisible(true);
            slide.setToX(0);
            fade.setToValue(1);
            isDrawerOpen = true;
        } else {
            slide.setToX(-DRAWER_WIDTH);
            fade.setToValue(0);
            slide.setOnFinished(e -> drawerOverlay.setVisible(false));
            isDrawerOpen = false;
        }
        new ParallelTransition(slide, fade).play();
    }

    private void changeSubView(Node newNode) {
        contentAreaStack.getChildren().set(0, newNode);
    }

    // =================================================================================
    // 🔐 Authentication & Registration
    // =================================================================================

    private boolean authenticateUser(String u, String p) {
        if (u.isEmpty() || p.isEmpty()) return false;
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try {
            Connection conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, u);
                pstmt.setString(2, p);
                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) { return false; }
    }
    private boolean registerUser(String u, String p) {
        if (u.isEmpty() || p.isEmpty()) return false;
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try {
            Connection conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, u);
                pstmt.setString(2, p);
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) { return false; }
    }
    // =================================================================================
    // 🛠️ Data Logic
    // =================================================================================

    private void initDatabase() {
        try {
            Connection conn = getConnection();
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS users (username VARCHAR(100) PRIMARY KEY, password VARCHAR(100), nickname VARCHAR(100))");
                stmt.execute("CREATE TABLE IF NOT EXISTS carbon_logs (id INT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(100), activity_type VARCHAR(100), amount DOUBLE, emission DOUBLE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
    // =================================================================================
    // 🔐 User Info Updates
    // =================================================================================

    // 1. Get nickname
    private String getDisplayNickname(String username) {
        String displayName = username;
        String sql = "SELECT nickname FROM users WHERE username = ?";
        // conn outside try block, prevents auto-closing
        try {
            Connection conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String nick = rs.getString("nickname");
                        if (nick != null && !nick.isEmpty()) displayName = nick;
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return displayName;
    }

    // 2. Update nickname
    private boolean updateNicknameInDB(String username, String newNickname) {
        String sql = "UPDATE users SET nickname = ? WHERE username = ?";
        try {
            Connection conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newNickname);
                pstmt.setString(2, username);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 3. Check if nickname is taken
    private boolean isNicknameTaken(String newNickname, String currentUsername) {
        String sql = "SELECT COUNT(*) FROM users WHERE nickname = ? AND username != ?";
        try {
            Connection conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newNickname);
                pstmt.setString(2, currentUsername);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // 4. Update password
    private boolean updatePasswordInDB(String username, String newPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ?";
        try {
            Connection conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, newPassword);
                pstmt.setString(2, username);
                return pstmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 5. Verify old password
    private boolean verifyCurrentPassword(String username, String inputPassword) {
        String sql = "SELECT password FROM users WHERE username = ?";
        try {
            Connection conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, username);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String dbPassword = rs.getString("password");
                        return dbPassword != null && dbPassword.equals(inputPassword);
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }
    private void saveSession(String username) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SESSION_FILE))) { writer.write(username); } catch (IOException e) { e.printStackTrace(); }
    }

    private String loadSession() {
        File file = new File(SESSION_FILE);
        if (!file.exists()) return null;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) { return reader.readLine(); } catch (IOException e) { return null; }
    }

    private void clearSession() {
        try { Files.deleteIfExists(Paths.get(SESSION_FILE)); } catch (IOException e) { e.printStackTrace(); }
    }

    private void showLoginScreen() {
        StackPane contentPane = new StackPane();
        contentPane.setStyle("-fx-background-color: transparent;");

        Label sloganLabel = new Label("🌿 Small Acts, Big Impact");
        sloganLabel.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 16));
        sloganLabel.setTextFill(Color.WHITE);

        Label logoLabel = new Label("SCT Tracker");
        logoLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        logoLabel.setTextFill(Color.WHITE);
        logoLabel.setEffect(new DropShadow(5, Color.BLACK));

        VBox logoBox = new VBox(10, sloganLabel, logoLabel);
        logoBox.setAlignment(Pos.CENTER);
        logoBox.setScaleX(0); logoBox.setScaleY(0);

        VBox inputBox = new VBox(20);
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setPadding(new Insets(30));
        inputBox.setMaxWidth(320);
        inputBox.setStyle(MAIN_CARD_STYLE);

        TextField userField = new TextField(); userField.setPromptText("Username"); userField.setStyle(INPUT_STYLE);
        PasswordField passField = new PasswordField(); passField.setPromptText("Password"); passField.setStyle(INPUT_STYLE);
        CheckBox autoLoginCheck = new CheckBox("Remember Me"); autoLoginCheck.setTextFill(Color.WHITE);

        Button loginBtn = new Button("Login"); loginBtn.setMaxWidth(Double.MAX_VALUE); loginBtn.setStyle(BTN_PRIMARY_STYLE); loginBtn.setPadding(new Insets(12));
        Button registerBtn = new Button("Create Account");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #004d40; -fx-font-weight: bold; -fx-underline: true; -fx-cursor: hand;");
        Label msgLabel = new Label();
        msgLabel.setEffect(new DropShadow(1, Color.BLACK));

        inputBox.getChildren().addAll(userField, passField, autoLoginCheck, loginBtn, registerBtn, msgLabel);
        inputBox.setOpacity(0); inputBox.setTranslateY(50);

        VBox mainContainer = new VBox(30);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.getChildren().addAll(logoBox, inputBox);
        contentPane.getChildren().add(mainContainer);

        ScaleTransition logoPop = new ScaleTransition(Duration.seconds(1.2), logoBox);
        logoPop.setFromX(0); logoPop.setFromY(0); logoPop.setToX(1); logoPop.setToY(1);
        logoPop.setInterpolator(Interpolator.EASE_OUT);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.8), inputBox);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        TranslateTransition slideUp = new TranslateTransition(Duration.seconds(0.8), inputBox);
        slideUp.setFromY(50); slideUp.setToY(0);
        slideUp.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition showInput = new ParallelTransition(fadeIn, slideUp);
        SequentialTransition sequence = new SequentialTransition(logoPop, new PauseTransition(Duration.seconds(0.2)), showInput);
        sequence.play();

        loginBtn.setOnAction(e -> {
            if (authenticateUser(userField.getText(), passField.getText())) {
                currentUser = userField.getText();
                if (autoLoginCheck.isSelected()) saveSession(currentUser); else clearSession();
                showMainDashboard();
            } else { msgLabel.setText("Invalid login"); msgLabel.setTextFill(Color.RED); }
        });
        registerBtn.setOnAction(e -> showRegisterScreen());

        switchView(contentPane);
        primaryStage.centerOnScreen();
    }

    private void showRegisterScreen() {
        StackPane contentPane = new StackPane();
        contentPane.setStyle("-fx-background-color: transparent;");

        Label sloganLabel = new Label("🌿 Join the Movement");
        sloganLabel.setFont(Font.font("Segoe UI", FontPosture.ITALIC, 16));
        sloganLabel.setTextFill(Color.WHITE);

        Label logoLabel = new Label("New Account");
        logoLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        logoLabel.setTextFill(Color.WHITE);
        logoLabel.setEffect(new DropShadow(5, Color.BLACK));

        VBox logoBox = new VBox(10, sloganLabel, logoLabel);
        logoBox.setAlignment(Pos.CENTER);

        VBox registerBox = new VBox(15);
        registerBox.setAlignment(Pos.CENTER);
        registerBox.setPadding(new Insets(30));
        registerBox.setMaxWidth(320);
        registerBox.setStyle(MAIN_CARD_STYLE);

        TextField userField = new TextField(); userField.setPromptText("Choose Username"); userField.setStyle(INPUT_STYLE);
        PasswordField passField = new PasswordField(); passField.setPromptText("Password"); passField.setStyle(INPUT_STYLE);
        PasswordField confirmPassField = new PasswordField(); confirmPassField.setPromptText("Confirm Password"); confirmPassField.setStyle(INPUT_STYLE);

        Button submitBtn = new Button("Sign Up"); submitBtn.setMaxWidth(Double.MAX_VALUE); submitBtn.setStyle(BTN_PRIMARY_STYLE); submitBtn.setPadding(new Insets(12));
        Button backBtn = new Button("Back to Login"); backBtn.setMaxWidth(Double.MAX_VALUE); backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #004d40; -fx-font-weight: bold; -fx-cursor: hand;");
        Label msgLabel = new Label();
        msgLabel.setEffect(new DropShadow(1, Color.BLACK));

        registerBox.getChildren().addAll(userField, passField, confirmPassField, submitBtn, backBtn, msgLabel);
        VBox mainContainer = new VBox(20);
        mainContainer.setAlignment(Pos.CENTER);
        mainContainer.getChildren().addAll(logoBox, registerBox);
        contentPane.getChildren().add(mainContainer);

        submitBtn.setOnAction(e -> {
            String u = userField.getText(); String p = passField.getText(); String cp = confirmPassField.getText();
            if (u.isEmpty() || p.isEmpty()) { msgLabel.setText("Fields cannot be empty"); msgLabel.setTextFill(Color.RED); }
            else if (!p.equals(cp)) { msgLabel.setText("Passwords do not match"); msgLabel.setTextFill(Color.RED); }
            else if (registerUser(u, p)) {
                msgLabel.setText("Success! Redirecting..."); msgLabel.setTextFill(Color.web("#2E7D32"));
                submitBtn.setDisable(true); backBtn.setDisable(true);
                PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
                pause.setOnFinished(event -> showLoginScreen()); pause.play();
            } else { msgLabel.setText("Username already exists"); msgLabel.setTextFill(Color.RED); }
        });
        backBtn.setOnAction(e -> showLoginScreen());
        switchView(contentPane);
    }

    private StackPane createDashboardView() {
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");

        VBox mainContainer = new VBox(15);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setPadding(new Insets(15));

        VBox impactCard = new VBox(10);
        impactCard.setStyle(IMPACT_CARD_STYLE);
        impactCard.setPadding(new Insets(15));
        impactCard.setMaxWidth(340);

        Label impactTitle = new Label("📊 Daily Carbon Limit (20kg)");
        impactTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        impactTitle.setTextFill(Color.web("#333"));

        dailyLimitBar = new ProgressBar(0);
        dailyLimitBar.setMaxWidth(Double.MAX_VALUE);
        dailyLimitBar.setStyle("-fx-accent: #4CAF50;");

        dailyProgressLabel = new Label("0 / 20 kg");
        dailyProgressLabel.setFont(Font.font(11));

        HBox treeBox = new HBox(10);
        treeBox.setAlignment(Pos.CENTER_LEFT);
        Label treeIcon = new Label("🌲");
        treeIcon.setStyle("-fx-font-size: 24px;");
        treeEquivalentLabel = new Label("Offset needed: 0 trees");
        treeEquivalentLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        treeEquivalentLabel.setTextFill(Color.web("#2E7D32"));
        treeBox.getChildren().addAll(treeIcon, treeEquivalentLabel);

        impactCard.getChildren().addAll(impactTitle, dailyLimitBar, dailyProgressLabel, new Separator(), treeBox);

        VBox inputCard = new VBox(15);
        inputCard.setStyle(MAIN_CARD_STYLE);
        inputCard.setPadding(new Insets(20));
        inputCard.setMaxWidth(340);
        inputCard.setAlignment(Pos.CENTER);

        Label typeLabel = new Label("Select Activity:"); typeLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        HBox labelBox1 = new HBox(typeLabel); labelBox1.setAlignment(Pos.CENTER_LEFT);

        typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Car Travel", "Bus Ride", "Bicycle", "Walking", "Electricity", "Food");
        typeBox.setPromptText("Select..."); typeBox.setMaxWidth(Double.MAX_VALUE);
        typeBox.setStyle("-fx-font-size: 14px; -fx-background-radius: 8; -fx-background-color: rgba(255,255,255,0.5);");

        Label amountLabel = new Label("Amount:"); amountLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        HBox labelBox2 = new HBox(amountLabel); labelBox2.setAlignment(Pos.CENTER_LEFT);

        StackPane amountStack = new StackPane(); amountStack.setAlignment(Pos.CENTER_RIGHT);
        amountField = new TextField(); amountField.setPromptText("e.g. 10"); amountField.setStyle(INPUT_STYLE + "-fx-padding: 10 40 10 10;");
        unitLabel = new Label(""); unitLabel.setTextFill(Color.GRAY); unitLabel.setStyle("-fx-font-weight: bold; -fx-padding: 0 10 0 0;");
        amountStack.getChildren().addAll(amountField, unitLabel);

        typeBox.setOnAction(e -> {
            String selected = typeBox.getValue();
            if (selected != null) {
                if (selected.contains("Car") || selected.contains("Bus") || selected.contains("Walking") || selected.contains("Bicycle")) { unitLabel.setText("km"); }
                else if (selected.contains("Electricity")) { unitLabel.setText("kWh"); }
                else if (selected.contains("Food")) { unitLabel.setText("kg"); }
                else { unitLabel.setText(""); }
            }
        });

        saveBtn = new Button("Save Record"); saveBtn.setMaxWidth(Double.MAX_VALUE); saveBtn.setStyle(BTN_PRIMARY_STYLE); saveBtn.setPadding(new Insets(12)); saveBtn.setFont(Font.font(14));
        cancelEditBtn = new Button("Cancel"); cancelEditBtn.setMaxWidth(Double.MAX_VALUE); cancelEditBtn.setVisible(false); cancelEditBtn.setManaged(false);

        statusLabel = new Label("Ready."); statusLabel.setTextFill(Color.GRAY);

        saveBtn.setOnAction(e -> handleSaveOrUpdate());
        cancelEditBtn.setOnAction(e -> resetForm());

        inputCard.getChildren().addAll(labelBox1, typeBox, labelBox2, amountStack, new Separator(), saveBtn, cancelEditBtn, statusLabel);
        mainContainer.getChildren().addAll(impactCard, inputCard);
        root.getChildren().add(mainContainer);

        updateDailyStatus();
        return root;
    }

    private VBox createAIAdvisorView() {
        VBox layout = new VBox(15); layout.setPadding(new Insets(20)); layout.setAlignment(Pos.TOP_CENTER);
        layout.setStyle(SLIGHTLY_TRANSPARENT_BG);

        HBox topBar = new HBox();
        Button backBtn = new Button("Back"); backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #134E5E; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px;");
        backBtn.setOnAction(e -> changeSubView(createDashboardView()));
        topBar.getChildren().add(backBtn); topBar.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("🤖 AI Analysis"); titleLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22)); titleLabel.setTextFill(Color.web("#333"));

        HBox toggleGroup = new HBox(10); toggleGroup.setAlignment(Pos.CENTER);
        ToggleButton btnWeek = new ToggleButton("Week"); ToggleButton btnMonth = new ToggleButton("Month");
        ToggleGroup group = new ToggleGroup();
        btnWeek.setToggleGroup(group); btnMonth.setToggleGroup(group);
        btnWeek.setSelected(true);

        String tStyleSel = "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-background-radius: 15; -fx-font-weight: bold; -fx-cursor: hand;";
        String tStyleUns = "-fx-background-color: rgba(255,255,255,0.6); -fx-text-fill: #333; -fx-background-radius: 15; -fx-cursor: hand;";
        btnWeek.setStyle(tStyleSel); btnMonth.setStyle(tStyleUns);

        VBox contentBox = new VBox(20); contentBox.setAlignment(Pos.TOP_CENTER);

        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            ToggleButton selected = (ToggleButton) newVal;
            btnWeek.setStyle(selected == btnWeek ? tStyleSel : tStyleUns);
            btnMonth.setStyle(selected == btnMonth ? tStyleSel : tStyleUns);
            String filter = "week";
            if (selected == btnMonth) filter = "month";
            updateAIContent(contentBox, filter);
        });

        toggleGroup.getChildren().addAll(btnWeek, btnMonth);
        updateAIContent(contentBox, "week");
        layout.getChildren().addAll(topBar, titleLabel, toggleGroup, contentBox);
        return layout;
    }

    private void updateAIContent(VBox contentBox, String timeFilter) {
        contentBox.getChildren().clear();
        String dateCondition;
        if (timeFilter.equals("month")) {
            dateCondition = "DATE_FORMAT(created_at, '%Y-%m') = DATE_FORMAT(CURDATE(), '%Y-%m')";
        } else {
            dateCondition = "created_at >= DATE_SUB(CURDATE(), INTERVAL 6 DAY)";
        }

        String sql = "SELECT activity_type, SUM(emission) as total FROM carbon_logs WHERE username=? AND " + dateCondition + " GROUP BY activity_type";
        Map<String, Double> totals = new HashMap<>();
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, currentUser); ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String type = rs.getString("activity_type"); double val = rs.getDouble("total");
                if (type.contains("(")) type = type.substring(0, type.indexOf("(")).trim();
                totals.put(type, val);
            }
        } catch (SQLException e) { e.printStackTrace(); }

        if (totals.isEmpty()) {
            Label emptyLabel = new Label("No data for this period.\nStart tracking to see insights!");
            emptyLabel.setFont(Font.font("Segoe UI", 16)); emptyLabel.setTextFill(Color.GRAY);
            emptyLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER); emptyLabel.setPadding(new Insets(30));
            contentBox.getChildren().add(emptyLabel); return;
        }

        String maxType = ""; double maxVal = -1; String minType = ""; double minVal = Double.MAX_VALUE;
        for (Map.Entry<String, Double> entry : totals.entrySet()) {
            if (entry.getValue() > maxVal) { maxVal = entry.getValue(); maxType = entry.getKey(); }
            if (entry.getValue() < minVal && entry.getValue() > 0) { minVal = entry.getValue(); minType = entry.getKey(); }
        }

        VBox focusCard = new VBox(10);
        focusCard.setStyle("-fx-background-color: rgba(255, 235, 238, 0.9); -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2); -fx-border-color: #ef9a9a; -fx-border-radius: 15;");
        Label focusTitle = new Label("🚨 Highest Emission (" + capitalize(timeFilter) + ")");
        focusTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16)); focusTitle.setTextFill(Color.web("#c62828"));
        Label focusDetail = new Label(maxType + ": " + String.format("%.2f", maxVal) + " kg");
        focusDetail.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        Label focusAdvice = new Label("💡 Tip: " + getContextualAdvice(maxType));
        focusAdvice.setWrapText(true); focusAdvice.setFont(Font.font("Segoe UI", 13));
        focusCard.getChildren().addAll(focusTitle, focusDetail, new Separator(), focusAdvice);

        VBox starCard = new VBox(10);
        starCard.setStyle("-fx-background-color: rgba(232, 245, 233, 0.9); -fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2); -fx-border-color: #a5d6a7; -fx-border-radius: 15;");
        Label starTitle = new Label("🌟 Lowest Emission (" + capitalize(timeFilter) + ")");
        starTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16)); starTitle.setTextFill(Color.web("#2e7d32"));
        Label starDetail = new Label(minType + ": " + String.format("%.2f", minVal) + " kg");
        starDetail.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        Label starPraise = new Label("👍 " + getContextualPraise(minType));
        starPraise.setWrapText(true); starPraise.setFont(Font.font("Segoe UI", 13));
        starCard.getChildren().addAll(starTitle, starDetail, new Separator(), starPraise);

        contentBox.getChildren().addAll(focusCard, starCard);
    }

    private String capitalize(String str) { return str.substring(0, 1).toUpperCase() + str.substring(1); }
    private String getContextualAdvice(String type) {
        if (type.contains("Car")) return "Cars are major polluters. Try carpooling or public transport.";
        if (type.contains("Bus")) return "Public transport is good, but combining trips helps more.";
        if (type.contains("Elec")) return "High energy use. Turn off unused lights and unplug devices.";
        if (type.contains("Food")) return "Meat has high carbon footprint. Try a plant-based meal.";
        if (type.contains("Bicycle") || type.contains("Walking")) return "Perfect! Zero emissions.";
        return "Look for eco-friendly alternatives for this activity.";
    }
    private String getContextualPraise(String type) {
        if (type.contains("Car")) return "You minimized driving. Great for the planet!";
        if (type.contains("Elec")) return "You are very energy efficient.";
        if (type.contains("Food")) return "Your diet choices were eco-friendly.";
        if (type.contains("Bicycle") || type.contains("Walking")) return "Zero emissions! Best way to travel.";
        return "You successfully kept this impact low. Well done!";
    }

    private void updateDailyStatus() {
        double totalToday = 0;
        try {
            Connection conn = getConnection(); // Do not close connection
            try (PreparedStatement pstmt = conn.prepareStatement(
                    "SELECT SUM(emission) FROM carbon_logs WHERE username=? AND DATE(created_at) = CURDATE()")) {
                pstmt.setString(1, currentUser);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) { totalToday = rs.getDouble(1); }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        double progress = totalToday / DAILY_LIMIT;
        dailyLimitBar.setProgress(progress);
        dailyProgressLabel.setText(String.format("%.2f / %.0f kg", totalToday, DAILY_LIMIT));
        if (progress > 1.0) dailyLimitBar.setStyle("-fx-accent: #D32F2F;");
        else if (progress > 0.7) dailyLimitBar.setStyle("-fx-accent: #FBC02D;");
        else dailyLimitBar.setStyle("-fx-accent: #4CAF50;");

        double treesNeeded = totalToday / 10.0;
        treeEquivalentLabel.setText(String.format("Offset needed: %.1f trees", treesNeeded));
    }
    private void handleSaveOrUpdate() {
        try {
            String selectedType = typeBox.getValue();
            if (selectedType == null || selectedType.isEmpty()) { statusLabel.setText("⚠️ Please select activity!"); statusLabel.setTextFill(Color.RED); return; }

            if (amountField.getText().trim().isEmpty()) { statusLabel.setText("⚠️ Please enter amount!"); statusLabel.setTextFill(Color.RED); return; }
            double amount = Double.parseDouble(amountField.getText());
            if (amount <= 0) { statusLabel.setText("❌ Amount must be > 0!"); statusLabel.setTextFill(Color.RED); return; }

            double factor = 0.0;
            String type = selectedType.toLowerCase();

            if (type.contains("car")) {
                factor = 0.25;      // Car
            } else if (type.contains("elec")) {
                factor = 0.5;       // Electricity
            } else if (type.contains("bus")) {
                factor = 0.08;      // Bus
            } else if (type.contains("food")) {
                factor = 0.12;      // Food
            } else if (type.contains("bicycle") || type.contains("walking")) {
                factor = 0.0;       // Bicycle and Walking = 0 emission
            } else {
                factor = 0.05;      // Default
            }
            double emission = amount * factor;

            if (currentEditingId == null) insertData(selectedType, amount, emission);
            else { updateData(currentEditingId, selectedType, amount, emission); resetForm(); }

            amountField.clear();
            statusLabel.setText("Saved! CO2 emitted: " + String.format("%.2f", emission) + " kg");
            statusLabel.setTextFill(Color.GREEN);
            updateDailyStatus();
        } catch (NumberFormatException e) { statusLabel.setText("❌ Invalid Number!"); statusLabel.setTextFill(Color.RED);
        } catch (Exception e) { statusLabel.setText("❌ Error"); statusLabel.setTextFill(Color.RED); }
    }

    private void resetForm() {
        currentEditingId = null;
        typeBox.setValue(null);
        amountField.clear();
        unitLabel.setText("");
        saveBtn.setText("Save Record");
        cancelEditBtn.setVisible(false);
        cancelEditBtn.setManaged(false);
        statusLabel.setText("Ready.");
        statusLabel.setTextFill(Color.GRAY);
    }

    private VBox createHistoryView() {
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        layout.setStyle(SLIGHTLY_TRANSPARENT_BG);

        Button backBtn = new Button("Back");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #134E5E; -fx-font-weight: bold; -fx-cursor: hand;");
        backBtn.setOnAction(e -> changeSubView(createDashboardView()));

        ObservableList<CarbonData> masterData = FXCollections.observableArrayList();
        FilteredList<CarbonData> filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<CarbonData> sortedData = new SortedList<>(filteredData);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search Date or Type...");
        searchField.setStyle("-fx-background-radius: 15; -fx-background-color: rgba(255, 255, 255, 0.6); -fx-padding: 8;");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredData.setPredicate(record -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                if (record.getDate().toLowerCase().contains(lowerCaseFilter)) return true;
                if (record.getType().toLowerCase().contains(lowerCaseFilter)) return true;
                return false;
            });
        });

        // Conn outside try block
        try {
            Connection conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM carbon_logs WHERE username=? ORDER BY emission DESC")) {
                pstmt.setString(1, currentUser);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String t = rs.getString("created_at");
                        String d = (t != null && t.length() >= 10) ? t.substring(5, 10) : t;
                        masterData.add(new CarbonData(rs.getInt("id"), d, rs.getString("activity_type"), rs.getDouble("amount"), rs.getDouble("emission")));
                    }
                }
            }
        } catch (Exception e) { e.printStackTrace(); }

        TableView<CarbonData> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setStyle("-fx-background-color: transparent;");
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedData);

        TableColumn<CarbonData, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<CarbonData, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));

        TableColumn<CarbonData, Double> emissionCol = new TableColumn<>("CO2");
        emissionCol.setCellValueFactory(new PropertyValueFactory<>("emission"));

        table.getColumns().addAll(dateCol, typeCol, emissionCol);

        table.setRowFactory(tv -> new TableRow<CarbonData>() {
            @Override protected void updateItem(CarbonData item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) setStyle("-fx-background-color: rgba(255,255,255,0.4);");
                else {
                    double e = item.getEmission();
                    if (e > 22.0) setStyle("-fx-background-color: rgba(255, 205, 210, 0.7);");
                    else if (e > 7.0) setStyle("-fx-background-color: rgba(255, 249, 196, 0.7);");
                    else setStyle("-fx-background-color: rgba(200, 230, 201, 0.7);");
                }
            }
        });

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER);
        Button editBtn = new Button("Edit"); editBtn.setStyle(BTN_WARN_STYLE); editBtn.setPrefWidth(80);
        Button deleteBtn = new Button("Delete"); deleteBtn.setStyle(BTN_DANGER_STYLE); deleteBtn.setPrefWidth(80);
        btnBox.getChildren().addAll(editBtn, deleteBtn);

        editBtn.setOnAction(e -> {
            CarbonData s = table.getSelectionModel().getSelectedItem();
            if (s != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Edit Record");
                alert.setHeaderText(null);
                alert.setContentText("Modify this record?");
                // Force English buttons
                alert.getButtonTypes().clear();
                ButtonType okBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().addAll(okBtn, cancelBtn);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == okBtn) {
                    changeSubView(createDashboardView());
                    enterEditMode(s);
                }
            }
        });

        deleteBtn.setOnAction(e -> {
            CarbonData s = table.getSelectionModel().getSelectedItem();
            if (s != null) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Delete Record");
                alert.setHeaderText(null);
                alert.setContentText("Are you sure? Cannot be undone.");
                // Force English buttons
                alert.getButtonTypes().clear();
                ButtonType okBtn = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
                ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().addAll(okBtn, cancelBtn);

                Optional<ButtonType> result = alert.showAndWait();
                if (result.isPresent() && result.get() == okBtn) {
                    deleteData(s.getId());
                    masterData.remove(s);
                }
            }
        });

        layout.getChildren().addAll(backBtn, searchField, table, btnBox);
        return layout;
    }
    private BorderPane createChartView() {
        BorderPane contentPane = new BorderPane(); contentPane.setStyle(SEMI_TRANSPARENT_BG);
        Button backBtn = new Button("Back"); backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #333; -fx-font-weight: bold;");
        backBtn.setOnAction(e -> changeSubView(createDashboardView()));

        HBox toolbar = new HBox(10); toolbar.setAlignment(Pos.CENTER_LEFT); toolbar.setPadding(new Insets(10));
        toolbar.setStyle(SLIGHTLY_TRANSPARENT_BG);

        ComboBox<String> chartTypeCombo = new ComboBox<>();
        chartTypeCombo.getItems().addAll("Pie Chart", "Line Chart");
        chartTypeCombo.setValue("Pie Chart");
        chartTypeCombo.setPrefWidth(120);
        chartTypeCombo.setStyle("-fx-background-color: rgba(255,255,255,0.5);");

        HBox toggleContainer = new HBox(5);
        toggleContainer.setAlignment(Pos.CENTER_LEFT);

        ToggleButton btnDay = new ToggleButton("Day");
        ToggleButton btnWeek = new ToggleButton("Week");
        ToggleButton btnMonth = new ToggleButton("Month");
        ToggleGroup group = new ToggleGroup();
        btnDay.setToggleGroup(group); btnWeek.setToggleGroup(group); btnMonth.setToggleGroup(group);
        btnDay.setSelected(true);

        String smallToggleSel = "-fx-background-color: #2E7D32; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-size: 11px; -fx-cursor: hand;";
        String smallToggleUn = "-fx-background-color: rgba(255,255,255,0.6); -fx-text-fill: #333; -fx-background-radius: 10; -fx-font-size: 11px; -fx-cursor: hand;";

        btnDay.setStyle(smallToggleSel);
        btnWeek.setStyle(smallToggleUn);
        btnMonth.setStyle(smallToggleUn);

        toggleContainer.getChildren().addAll(btnDay, btnWeek, btnMonth);

        toolbar.getChildren().addAll(backBtn, chartTypeCombo, toggleContainer);
        contentPane.setTop(toolbar);

        StackPane chartArea = new StackPane(); chartArea.setPadding(new Insets(10));

        Runnable refreshChart = () -> {
            chartArea.getChildren().clear();
            String filter = "day";
            if (btnWeek.isSelected()) filter = "week";
            if (btnMonth.isSelected()) filter = "month";

            btnDay.setStyle(btnDay.isSelected() ? smallToggleSel : smallToggleUn);
            btnWeek.setStyle(btnWeek.isSelected() ? smallToggleSel : smallToggleUn);
            btnMonth.setStyle(btnMonth.isSelected() ? smallToggleSel : smallToggleUn);

            if (chartTypeCombo.getValue().contains("Pie")) {
                chartArea.getChildren().add(createPieChart("all"));
            } else {
                chartArea.getChildren().add(createLineChart(filter));
            }
        };

        chartTypeCombo.setOnAction(e -> {
            boolean isLineChart = chartTypeCombo.getValue().contains("Line");
            toggleContainer.setVisible(isLineChart);
            toggleContainer.setManaged(isLineChart);
            refreshChart.run();
        });

        boolean isLineStart = chartTypeCombo.getValue().contains("Line");
        toggleContainer.setVisible(isLineStart);
        toggleContainer.setManaged(isLineStart);

        group.selectedToggleProperty().addListener((obs, oldVal, newVal) -> { if (newVal != null) refreshChart.run(); });

        refreshChart.run();
        contentPane.setCenter(chartArea);
        return contentPane;
    }

    private VBox createPieChart(String timeFilter) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        String[] colors = { "#EF5350", "#FFA726", "#66BB6A", "#42A5F5", "#AB47BC", "#8D6E63", "#78909C", "#26C6DA", "#FF7043" };

        VBox container = new VBox(10);
        container.setAlignment(Pos.CENTER);
        container.setStyle("-fx-background-color: transparent;");

        // 1. Create container and details label
        Label detailsLabel = new Label("👆 Click on a slice to see details");
        detailsLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        detailsLabel.setTextFill(Color.web("#2E7D32"));
        detailsLabel.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-padding: 8; -fx-background-radius: 10;");

        PieChart chart = new PieChart();
        chart.setLegendVisible(false);
        chart.setLabelsVisible(true);
        chart.setLabelLineLength(10);
        chart.setStyle("-fx-background-color: transparent;");

        FlowPane customLegend = new FlowPane();
        customLegend.setHgap(15);
        customLegend.setVgap(10);
        customLegend.setAlignment(Pos.CENTER);
        customLegend.setPadding(new Insets(10, 10, 20, 10));
        customLegend.setStyle("-fx-background-color: rgba(245, 245, 245, 0.6); -fx-background-radius: 10; -fx-border-color: rgba(224, 224, 224, 0.5); -fx-border-radius: 10;");

        try {
            Connection conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement("SELECT activity_type, SUM(emission) as total FROM carbon_logs WHERE username=? GROUP BY activity_type")) {
                pstmt.setString(1, currentUser);
                try (ResultSet rs = pstmt.executeQuery()) {
                    int colorIndex = 0;
                    while (rs.next()) {
                        String originalType = rs.getString("activity_type");
                        double total = rs.getDouble("total");

                        PieChart.Data data = new PieChart.Data(String.format("%.1f", total), total);
                        String color = colors[colorIndex % colors.length];

                        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                            if (newNode != null) {
                                newNode.setStyle("-fx-pie-color: " + color + ";");
                                newNode.setCursor(javafx.scene.Cursor.HAND);

                                // 2. Click Event: Update text above
                                newNode.setOnMouseClicked(e -> {
                                    detailsLabel.setText(originalType + ": " + String.format("%.2f kg", total));
                                    detailsLabel.setTextFill(Color.web(color)); // Text color matches slice color
                                });
                            }
                        });

                        pieData.add(data);

                        HBox legendItem = new HBox(6);
                        legendItem.setAlignment(Pos.CENTER_LEFT);
                        Circle c = new Circle(6, Color.web(color));
                        Label nameLabel = new Label(originalType);
                        nameLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #333;");
                        legendItem.getChildren().addAll(c, nameLabel);
                        customLegend.getChildren().add(legendItem);

                        colorIndex++;
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        chart.setData(pieData);
        VBox.setVgrow(chart, Priority.ALWAYS);

        if (pieData.isEmpty()) {
            container.getChildren().add(new Label("No data found."));
        } else {
            // 3. Assemble UI
            container.getChildren().addAll(detailsLabel, chart, customLegend);
        }

        return container;
    }
    // Note: Return type changed to VBox
    private VBox createLineChart(String timeFilter) {
        // 1. Create container and details label
        VBox container = new VBox(10);
        container.setAlignment(Pos.CENTER);

        Label detailsLabel = new Label("👆 Click a point to see date & emission");
        detailsLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        detailsLabel.setTextFill(Color.web("#2E7D32"));
        detailsLabel.setStyle("-fx-background-color: rgba(255,255,255,0.8); -fx-padding: 8; -fx-background-radius: 10;");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        if (timeFilter.equals("day")) xAxis.setLabel("Date");
        else if (timeFilter.equals("week")) xAxis.setLabel("Week Start");
        else xAxis.setLabel("Month");
        yAxis.setLabel("Total Emission (kg)");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setLegendSide(Side.BOTTOM);
        VBox.setVgrow(lineChart, Priority.ALWAYS); // Let chart take up remaining space

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(timeFilter.substring(0, 1).toUpperCase() + timeFilter.substring(1) + " Trend");

        String sql;
        if (timeFilter.equals("day")) {
            sql = "SELECT DATE_FORMAT(created_at, '%Y-%m-%d') as time_point, SUM(emission) as total FROM carbon_logs WHERE username=? AND created_at >= DATE_SUB(CURDATE(), INTERVAL 14 DAY) GROUP BY time_point ORDER BY time_point ASC";
        } else if (timeFilter.equals("week")) {
            sql = "SELECT DATE_FORMAT(created_at, '%Y-%u') as time_point, SUM(emission) as total FROM carbon_logs WHERE username=? AND created_at >= DATE_SUB(CURDATE(), INTERVAL 12 WEEK) GROUP BY time_point ORDER BY time_point ASC";
        } else {
            sql = "SELECT DATE_FORMAT(created_at, '%Y-%m') as time_point, SUM(emission) as total FROM carbon_logs WHERE username=? AND created_at >= DATE_SUB(CURDATE(), INTERVAL 12 MONTH) GROUP BY time_point ORDER BY time_point ASC";
        }

        try {
            Connection conn = getConnection();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, currentUser);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String rawDate = rs.getString("time_point");
                        double val = rs.getDouble("total");
                        String displayLabel = (rawDate != null && rawDate.length() >= 10) ? rawDate.substring(5) : rawDate;

                        XYChart.Data<String, Number> data = new XYChart.Data<>(displayLabel, val);

                        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                            if (newNode != null) {
                                newNode.setCursor(javafx.scene.Cursor.HAND);
                                // 2. Click Event: Update text above
                                newNode.setOnMouseClicked(e -> {
                                    detailsLabel.setText("Date: " + displayLabel + " | Emission: " + String.format("%.2f kg", val));
                                });
                            }
                        });

                        series.getData().add(data);
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        lineChart.getData().add(series);

        // 3. Assemble UI
        if (series.getData().isEmpty()) {
            container.getChildren().add(new Label("No data found."));
        } else {
            container.getChildren().addAll(detailsLabel, lineChart);
        }

        return container;
    }
    private void insertData(String t, double a, double e) throws SQLException {
        Connection conn = getConnection(); // Do not close
        try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO carbon_logs (username, activity_type, amount, emission) VALUES (?, ?, ?, ?)")) {
            pstmt.setString(1, currentUser);
            pstmt.setString(2, t);
            pstmt.setDouble(3, a);
            pstmt.setDouble(4, e);
            pstmt.executeUpdate();
        }
    }
    private void updateData(int id, String t, double a, double e) throws SQLException {
        Connection conn = getConnection(); // Do not close
        try (PreparedStatement pstmt = conn.prepareStatement("UPDATE carbon_logs SET activity_type=?, amount=?, emission=? WHERE id=?")) {
            pstmt.setString(1, t);
            pstmt.setDouble(2, a);
            pstmt.setDouble(3, e);
            pstmt.setInt(4, id);
            pstmt.executeUpdate();
        }
    }

    private void deleteData(int id) {
        try {
            Connection conn = getConnection(); // Do not close
            try (PreparedStatement pstmt = conn.prepareStatement("DELETE FROM carbon_logs WHERE id=?")) {
                pstmt.setInt(1, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
    private void enterEditMode(CarbonData data) {
        currentEditingId = data.getId();
        typeBox.setValue(data.getType());
        amountField.setText(String.valueOf(data.getAmount()));
        saveBtn.setText("Update");
        cancelEditBtn.setVisible(true);
        cancelEditBtn.setManaged(true);
        statusLabel.setText("Editing record...");
        statusLabel.setTextFill(Color.web("#F57C00"));
    }

    public static void main(String[] args) { launch(args); }

    public static class CarbonData {
        private final SimpleIntegerProperty id; private final SimpleStringProperty date; private final SimpleStringProperty type; private final SimpleDoubleProperty amount; private final SimpleDoubleProperty emission;
        public CarbonData(int id, String d, String t, double a, double e) { this.id = new SimpleIntegerProperty(id); this.date = new SimpleStringProperty(d); this.type = new SimpleStringProperty(t); this.amount = new SimpleDoubleProperty(a); this.emission = new SimpleDoubleProperty(e); }
        public int getId() { return id.get(); } public String getDate() { return date.get(); } public String getType() { return type.get(); } public double getAmount() { return amount.get(); } public double getEmission() { return emission.get(); }
    }

}