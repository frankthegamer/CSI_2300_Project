package organizer;

import javafx.animation.FadeTransition;   // JavaFX imports
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;   
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.nio.file.*;             // java imports
import java.util.*;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONArray;             //json imports
import org.json.JSONObject;

import organizer.rule.FileCategoryRule;   // Rule imports
import organizer.rule.FileExtensionRule;
import organizer.rule.LastModifiedRule;
import organizer.rule.NameHasRule;
import organizer.rule.Rule;
import organizer.rule.StringContainedRule;

public class NeatFileApp extends Application {

    private NeatFileLogic organizer = new NeatFileLogic();
    
    
    private NeatGroup currentGroup;
    private Set<Path> watchDirs = new HashSet<>();
    private Path configPath = Paths.get("groups.json");
    private List<NeatGroup> groups = new ArrayList<>();
    private volatile boolean running = true;

    //UI elements
    private ListView<String> watchDirsListView;

    private Stage primaryStage;
    private ComboBox<String> groupComboBox;
    private ListView<String> rulesListView;
    private Label targetDirLabel;
    private StackPane targetIconPane;
    
    private Tooltip targetTooltip = new Tooltip();




    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        
        BorderPane mainContent = new BorderPane();
        StackPane root = new StackPane(mainContent); 
        Scene scene = new Scene(root, 1000, 600);

        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());

        //Title
        Label titleLabel = new Label("NeatFile");
        titleLabel.setId("app-title");
        

        //Main layout
        HBox workflowBox = new HBox(100);
        workflowBox.setPadding(new javafx.geometry.Insets(20));
        workflowBox.setStyle("-fx-background-color: #f0f0f0;");
        workflowBox.setAlignment(Pos.CENTER);


        // Group Selection Section
        VBox groupBox = new VBox(10);
        groupBox.setPrefWidth(180);
        Label groupLabel = new Label("Select Group");
        groupLabel.setStyle("-fx-font-weight: bold;");
        groupComboBox = new ComboBox<>();
        groupComboBox.setPrefWidth(120);
        groupComboBox.setOnAction(e -> updateCurrentGroup());
        
        Button addGroupBtn = new Button("\u2795");   // add group btn
        addGroupBtn.setPrefWidth(70);
        addGroupBtn.setOnAction(e -> createNewGroup());

        Button deleteGroupBtn = new Button("\u2796"); // delete group btn
        deleteGroupBtn.setPrefWidth(70); 
        deleteGroupBtn.setOnAction(e -> deleteCurrentGroup());

        addGroupBtn.getStyleClass().add("group-btn");
        deleteGroupBtn.getStyleClass().add("group-btn");

        // ComboBox and AddButton
        HBox groupInputBox = new HBox(5);
        groupInputBox.getChildren().addAll(groupComboBox, addGroupBtn, deleteGroupBtn);
        groupInputBox.setAlignment(Pos.CENTER_LEFT);
        
        groupBox.getChildren().addAll(groupLabel, groupInputBox);

        // anchor pane for top border alignment
        AnchorPane topPane = new AnchorPane();
        topPane.setStyle("-fx-background-color: #f0f0f0;");
        topPane.setPadding(new javafx.geometry.Insets(10));

        topPane.getChildren().addAll(titleLabel, groupBox);

        AnchorPane.setTopAnchor(groupBox, 0.0);
        AnchorPane.setLeftAnchor(groupBox, 10.0);

        AnchorPane.setTopAnchor(titleLabel, 10.0);
        AnchorPane.setLeftAnchor(titleLabel, 0.0);
        AnchorPane.setRightAnchor(titleLabel, 0.0);
        titleLabel.setAlignment(Pos.CENTER);

        mainContent.setTop(topPane);

        
        // Watch path selection
        VBox watchPathsBox = new VBox(10);
        watchPathsBox.setPrefWidth(300);
        
        Label watchPathsLabel = new Label("Watch Paths:");
        watchPathsLabel.getStyleClass().add("section-label");
        watchPathsLabel.setMaxWidth(Double.MAX_VALUE);
        watchPathsLabel.setAlignment(Pos.CENTER);


        watchDirsListView = new ListView<>();
        watchDirsListView.getItems().addAll(watchDirs.stream().map(Path::toString).toList());
        watchDirsListView.setPrefHeight(200);

        // delete functionality for watch paths
        watchDirsListView.setOnMouseClicked(event -> {
            String selectedPath = watchDirsListView.getSelectionModel().getSelectedItem();
            if (selectedPath != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Confirm Deletion");
                confirm.setHeaderText("Remove Watch Path?");
                confirm.setContentText("Are you sure you want to delete this path?\n" + selectedPath);
            
                Optional<ButtonType> result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    Path pathToRemove = Paths.get(selectedPath);
                    watchDirs.remove(pathToRemove);
                    watchDirsListView.getItems().remove(selectedPath);
                    System.out.println("Removed watch path: " + selectedPath);
                    if (currentGroup != null) {
                        currentGroup.setWatchDirectories(new HashSet<>(watchDirs));
                    }
                }
            }
        });

        Button addSourceButton = new Button("\u2795");
        addSourceButton.setPrefWidth(50);
        addSourceButton.setOnAction(e -> createSourceUI(primaryStage));
        watchPathsBox.getChildren().addAll(watchPathsLabel, watchDirsListView, addSourceButton);
        

        // Rules Section
        VBox rulesBox = new VBox(10);
        rulesBox.setPrefWidth(300);
        rulesBox.setAlignment(Pos.TOP_CENTER);

        Label rulesLabel = new Label("Rules:");
        rulesLabel.getStyleClass().add("section-label");
        rulesLabel.setMaxWidth(Double.MAX_VALUE);
        rulesLabel.setAlignment(Pos.CENTER);

        rulesListView = new ListView<>();
        rulesListView.setPrefHeight(200);

        // delete functionality for rules
        rulesListView.setOnMouseClicked(event -> {
            String selectedRule = rulesListView.getSelectionModel().getSelectedItem();
            if (selectedRule != null && currentGroup != null) {

                // Find the rule to remove by matching its toString() representation
                Rule ruleToRemove = currentGroup.getRules().stream()
                        .filter(rule -> rule.toString().equals(selectedRule))
                        .findFirst()
                        .orElse(null);
                        if (ruleToRemove != null) {
                            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                            confirm.setTitle("Confirm Deletion");
                            confirm.setHeaderText("Remove Rule?");
                            confirm.setContentText("Are you sure you want to delete this rule?\n" + selectedRule);
                        
                            Optional<ButtonType> result = confirm.showAndWait();
                            if (result.isPresent() && result.get() == ButtonType.OK) {
                                currentGroup.removeRule(ruleToRemove);
                                rulesListView.getItems().clear();
                                currentGroup.getRules().forEach(rule -> rulesListView.getItems().add(rule.toString()));
                                System.out.println("Removed rule: " + selectedRule);
                            }
                        }
            }
        });

        FlowPane ruleButtonsBox = new FlowPane();
        ruleButtonsBox.setHgap(10);
        ruleButtonsBox.setVgap(10); 
        ruleButtonsBox.setPrefWrapLength(280); 
        ruleButtonsBox.setAlignment(Pos.CENTER);

        Button addStringRuleBtn = new Button("String Rule");
        Button addExtensionRuleBtn = new Button("Extension Rule");
        Button addCategoryRuleBtn = new Button("Category Rule");
        Button addNameRuleBtn = new Button("Name Rule");
        Button addLastModifiedRuleBtn = new Button("Last Modified Rule");
        ruleButtonsBox.getChildren().addAll(addStringRuleBtn, addExtensionRuleBtn, addCategoryRuleBtn,
                addNameRuleBtn, addLastModifiedRuleBtn);
        rulesBox.getChildren().addAll(rulesLabel, rulesListView, ruleButtonsBox);

        // Button events for rules
        addCategoryRuleBtn.setOnAction(e -> createCategoryRuleUI());
        addExtensionRuleBtn.setOnAction(e -> createExtensionRuleUI());
        addStringRuleBtn.setOnAction(e -> createStringRuleUI());
        addNameRuleBtn.setOnAction(e -> createNameRuleUI());
        addLastModifiedRuleBtn.setOnAction(e -> createLastModifiedRuleUI());

        // Target Path Section
        VBox targetPathBox = new VBox(10);
        targetPathBox.setPrefWidth(200);
        
        Label targetPathLabel = new Label("Target Path:");
        targetPathLabel.getStyleClass().add("section-label");
        targetPathLabel.setMaxWidth(Double.MAX_VALUE);
        targetPathLabel.setAlignment(Pos.CENTER);


        targetDirLabel = new Label("");
        targetDirLabel.setText("");
        targetDirLabel.setStyle("""
                -fx-text-fill: gray;
                -fx-font-size: 15px;
                -fx-font-weight: bold;
                """);

        ImageView folderIcon = new ImageView(new Image(getClass().getResourceAsStream("folder_icon.png")));
        folderIcon.setFitWidth(150);
        folderIcon.setFitHeight(150);

        targetIconPane = new StackPane(folderIcon, targetDirLabel);
        Tooltip.install(targetIconPane, targetTooltip); // attach tooltip initially

        targetIconPane.setPrefSize(160, 60);
        targetIconPane.setStyle("""
            -fx-border-color: #ccc;
            -fx-border-radius: 5px;
            -fx-padding: 5px;
            """);

        targetIconPane.setOnMouseEntered(e -> {
            targetIconPane.setStyle("""
                -fx-border-color: #aaa;
                -fx-border-radius: 5px;
                -fx-background-color: #eaeaea;
                -fx-padding: 5px;
                -fx-cursor: hand;
            """);
        });
        
        targetIconPane.setOnMouseExited(e -> {
            targetIconPane.setStyle("""
                -fx-border-color: #ccc;
                -fx-border-radius: 5px;
                -fx-background-color: transparent;
                -fx-padding: 5px;
            """);
        });
        

        targetIconPane.setOnMouseClicked(e -> createTargetUI(primaryStage));


        targetPathBox.getChildren().addAll(targetPathLabel, targetIconPane);


        // workFlowBox add all sections
        workflowBox.getChildren().addAll(watchPathsBox, rulesBox, targetPathBox);
        mainContent.setCenter(workflowBox);

        // Bottom: Finalize Button
        Button finalizeBtn = new Button("Finalize");
        finalizeBtn.setStyle("-fx-font-size: 24px; -fx-padding: 10px;");
        finalizeBtn.setOnAction(e -> finalizeGroups());
        mainContent.setBottom(finalizeBtn);
        BorderPane.setAlignment(finalizeBtn, javafx.geometry.Pos.CENTER);
        BorderPane.setMargin(finalizeBtn, new javafx.geometry.Insets(10));

        // Load existing groups from json
        groups.clear();
        try {
            String content = Files.readString(configPath);
            JSONArray jsonGroups = new JSONArray(content);
            for (int i = 0; i < jsonGroups.length(); i++) {
                JSONObject jsonGroup = jsonGroups.getJSONObject(i);
                Set<Path> watchDirs = new HashSet<>(jsonGroup.getJSONArray("watchDirectories")
                        .toList().stream().map(Object::toString).map(Paths::get).toList());
                Path targetDir = Paths.get(jsonGroup.getString("targetDirectory"));

                NeatGroup group = new NeatGroup(watchDirs, targetDir);
                groups.add(group);

                JSONArray jsonRules = jsonGroup.getJSONArray("rules");
                for (int j = 0; j < jsonRules.length(); j++) {
                    Rule rule = Rule.fromJSON(jsonRules.getJSONObject(j));
                    group.addRule(rule);
                }

                this.watchDirs.addAll(watchDirs);
            }
        } catch (IOException e) {
            System.out.println("No existing groups.json found or failed to load: " + e.getMessage());
        }

        // fill up group dropdown
        updateGroupComboBox();

        primaryStage.setScene(scene);
        primaryStage.setTitle("NeatFile");
        primaryStage.show();

        startManualScanner();
        primaryStage.setOnCloseRequest(e -> shutdown());
    }

    private void createSourceUI(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Watch Directory");
        java.io.File selectedDir = chooser.showDialog(stage);
        if (selectedDir != null) {
            Path path = selectedDir.toPath();
            watchDirs.add(path);
            watchDirsListView.getItems().clear();
            watchDirsListView.getItems().addAll(watchDirs.stream().map(Path::toString).toList());
            if (currentGroup != null) {
                currentGroup.setWatchDirectories(new HashSet<>(watchDirs));
            }
        }
    }

    private void createTargetUI(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Target Directory");
        java.io.File selectedDir = chooser.showDialog(stage);
        if (selectedDir != null) {
            Path path = selectedDir.toPath();

            String fullPath = path.toString();
            String folderName = path.getFileName().toString();
            targetDirLabel.setText(folderName);
            targetTooltip.setText(fullPath); // updates the shared tooltip
            targetDirLabel.setTooltip(null); // prevent weird overlap


            if (currentGroup != null) {
                currentGroup.setTargetDirectory(path);
            }
        }
    }

    private void createNewGroup() {
        NeatGroup group = new NeatGroup(new HashSet<>(), null);
        groups.add(group);
        updateGroupComboBox();
        groupComboBox.getSelectionModel().selectLast(); // will call updateCurrentGroup()

    }

    private void updateGroupComboBox() {
        groupComboBox.getItems().clear();
        for (int i = 0; i < groups.size(); i++) {
            groupComboBox.getItems().add("Group " + (i + 1));
        }

        if(!groups.isEmpty()){
            groupComboBox.getSelectionModel().selectFirst(); // select the first group by default
            updateCurrentGroup(); // update UI with first group's details
        }
    }

    private void deleteCurrentGroup() {
        int selectedIndex = groupComboBox.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < groups.size()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Deletion");
            confirm.setHeaderText("Delete Group?");
            confirm.setContentText("Are you sure you want to delete Group " + (selectedIndex + 1) + "?");
    
            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                groups.remove(selectedIndex);
                if (groups.isEmpty()) {
                    currentGroup = null;
                    watchDirs.clear();
                    watchDirsListView.getItems().clear();
                    rulesListView.getItems().clear();
                    targetDirLabel.setText(""); // reset target path label
                    targetDirLabel.setTooltip(new Tooltip("Click to choose target folder"));
                } else {
                    groupComboBox.getSelectionModel().selectFirst();
                    updateCurrentGroup();
                }
                updateGroupComboBox();
            }
        }
    }
    

    private void updateCurrentGroup() {
        int selectedIndex = groupComboBox.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            currentGroup = groups.get(selectedIndex);
            // Update watch directories
            watchDirs.clear();
            watchDirs.addAll(currentGroup.getWatchDirectories());
            watchDirsListView.getItems().clear();
            watchDirsListView.getItems().addAll(watchDirs.stream().map(Path::toString).toList());
            // Update rules
            rulesListView.getItems().clear();
            currentGroup.getRules().forEach(rule -> rulesListView.getItems().add(rule.toString()));
            // Update target directory
            Path targetDir = currentGroup.getTargetDirectory();
            if (targetDir != null) {
                String fullPath = targetDir.toString();
                String folderName = targetDir.getFileName().toString();
                targetDirLabel.setText(folderName);
                targetTooltip.setText(fullPath);
            } else {
                targetDirLabel.setText("");
                targetTooltip.setText("Click to choose target folder");
            }
        }
    }
    

    private void createCategoryRuleUI() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add Category Rule");
        dialog.setHeaderText("Select a file category");

        ComboBox<String> input = new ComboBox<>();
        input.getItems().addAll("Image", "Document", "Audio", "Video");
        input.setValue("Image");

        dialog.getDialogPane().setContent(input);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> button == ButtonType.OK ? input.getValue() : null);

        dialog.showAndWait().ifPresent(category -> {
            if (currentGroup != null) {
                Rule rule = new FileCategoryRule(category);
                currentGroup.addRule(rule);
                rulesListView.getItems().add(rule.toString());
            }
        });
    }

    private void createExtensionRuleUI() {
        TextInputDialog dialog = new TextInputDialog("png,jpg");
        dialog.setTitle("Add Extension Rule");
        dialog.setHeaderText("Enter file extensions (comma-separated)");
        dialog.setContentText("Extensions:");

        dialog.showAndWait().ifPresent(extensions -> {
            if (currentGroup != null) {
                Rule rule = new FileExtensionRule(new HashSet<>(Arrays.asList(extensions.split(","))));
                currentGroup.addRule(rule);
                rulesListView.getItems().add(rule.toString());
            }
        });
    }

    private void createStringRuleUI() {
        Dialog<Rule> dialog = new Dialog<>();
        dialog.setTitle("Add String Rule");
        dialog.setHeaderText("Enter a string to match in file content");

        VBox content = new VBox(10);
        TextField input = new TextField("text");
        CheckBox caseCheck = new CheckBox("Case Sensitive");
        CheckBox regexCheck = new CheckBox("Regex");
        content.getChildren().addAll(new Label("String:"), input, caseCheck, regexCheck);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return new StringContainedRule(input.getText(), caseCheck.isSelected(), regexCheck.isSelected());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(rule -> {
            if (currentGroup != null) {
                currentGroup.addRule(rule);
                rulesListView.getItems().add(rule.toString());
            }
        });
    }

    private void createNameRuleUI() {
        Dialog<Rule> dialog = new Dialog<>();
        dialog.setTitle("Add Name Rule");
        dialog.setHeaderText("Enter a string to match in file name");

        VBox content = new VBox(10);
        TextField input = new TextField("note");
        CheckBox caseCheck = new CheckBox("Case Sensitive");
        CheckBox regexCheck = new CheckBox("Regex");
        content.getChildren().addAll(new Label("String:"), input, caseCheck, regexCheck);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                return new NameHasRule(input.getText(), caseCheck.isSelected(), regexCheck.isSelected());
            }
            return null;
        });

        dialog.showAndWait().ifPresent(rule -> {
            if (currentGroup != null) {
                currentGroup.addRule(rule);
                rulesListView.getItems().add(rule.toString());
            }
        });
    }

    private void createLastModifiedRuleUI() {     // UI for last modified rule
        TextInputDialog dialog = new TextInputDialog(" ");
        dialog.setTitle("Add Last Modified Rule");
        dialog.setHeaderText("Enter the number of days (matches files modified longer ago than this)");
        dialog.setContentText("Days:");

        dialog.showAndWait().ifPresent(days -> {
            if (currentGroup != null) {
                try{
                    long daysValue = Long.parseLong(days);
                    if(daysValue < 0){
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Invalid Input");
                        alert.setHeaderText("Number of days cannot be negative");
                        alert.setContentText("Please enter a non-negative number of days.");
                        alert.showAndWait();
                        return;
                    }
                    Rule rule = new LastModifiedRule(daysValue);
                    currentGroup.addRule(rule);
                    rulesListView.getItems().add(rule.toString());
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Input");
                    alert.setHeaderText("Invalid number of days");
                    alert.setContentText("Please enter a valid number.");
                    alert.showAndWait();
                }
            }
        });
    }


    private void finalizeGroups() {  // saves groups to json file
        organizer.clearGroups();
        
        JSONArray jsonGroups = new JSONArray();
        for (NeatGroup group : groups) {
            JSONObject json = new JSONObject();
            json.put("watchDirectories", group.getWatchDirectories().stream().map(Path::toString).toList());

            Path targetDir = group.getTargetDirectory();        // null check for target directory
            if (targetDir == null) {
                System.err.println("Skipping group with no target directory");
                continue; 
            }
            
            json.put("targetDirectory", group.getTargetDirectory().toString());
            json.put("rules", group.getRules().stream().map(Rule::toJSON).toList());
            jsonGroups.put(json);
        }
        try (FileWriter file = new FileWriter("groups.json")) {
            file.write(jsonGroups.toString(2));
            System.out.println("Groups successfully saved to groups.json");
            finalizeStatus((Stage) groupComboBox.getScene().getWindow(), "Changes saved!");

        } catch (IOException e) {
            System.err.println("Failed to write to groups.json: " + e.getMessage());
            e.printStackTrace();
        }

        for (NeatGroup group : groups) {
            addGroupToOrganizer(group);
        }        
    }


    private void startManualScanner() {             // had to use this as backup because watch service wasn't working on my system
        Thread manualScannerThread = new Thread(() -> {
            while (running) {
                try {
                    for (NeatGroup group : groups) {
                        for (Path dir : group.getWatchDirectories()) {
                            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                                for (Path file : stream) {
                                    if (Files.isRegularFile(file)) {
                                        System.out.println("[Manual Scan] Checking file: " + file);
                                        organizer.processFile(file);
                                    }
                                }
                            } catch (IOException e) {
                                System.out.println("Failed to scan folder: " + dir + " - " + e.getMessage());
                            }
                        }
                    }
    
                    Thread.sleep(5000); // 5 seconds between scans
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    
        manualScannerThread.setDaemon(true);
        manualScannerThread.start();
    }
    

    private void shutdown() {
        running = false;

    }

    public boolean addGroupToOrganizer(NeatGroup group) {
        boolean success = organizer.addGroup(group);
        if (!success) {
            finalizeStatus((Stage) groupComboBox.getScene().getWindow(), "Duplicate group not added.");
        }
        return success;
    }
    

   public void finalizeStatus(Stage stage, String message) {   // status message for saving groups
        Label update = new Label(message);
        update.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 8px; -fx-background-radius: 6px;");
        update.setOpacity(0);

        StackPane root = (StackPane) stage.getScene().getRoot();
        root.getChildren().add(update);
        StackPane.setAlignment(update, Pos.BOTTOM_CENTER);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.3), update);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition pause = new PauseTransition(Duration.seconds(1.5));

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.3), update);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        SequentialTransition seq = new SequentialTransition(fadeIn, pause, fadeOut);
        seq.setOnFinished(e -> root.getChildren().remove(update));
        seq.play();
    }


    public static void main(String[] args) {
        launch(args);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
    
}
