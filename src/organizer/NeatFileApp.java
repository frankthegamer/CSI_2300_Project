package organizer;

import javafx.application.Application;   // JavaFX imports
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;


import java.nio.file.*;             // java imports
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.io.FileWriter;
import java.io.IOException;

import org.json.JSONArray;             //json imports
import org.json.JSONObject;

import organizer.rule.FileCategoryRule;   // Rule imports
import organizer.rule.FileExtensionRule;
import organizer.rule.LastAccessedRule;
import organizer.rule.NameHasRule;
import organizer.rule.Rule;
import organizer.rule.StringContainedRule;

public class NeatFileApp extends Application {

    private NeatFileLogic organizer = new NeatFileLogic();
    private NeatGroup currentGroup;
    private Set<Path> watchDirs = new HashSet<>();
    private Path configPath = Paths.get("groups.json");
    private WatchService watchService;
    private List<NeatGroup> groups = new ArrayList<>();
    private Thread watchServiceThread;
    private volatile boolean running = true;

    //UI elements
    private ListView<String> watchDirsListView;
    private ComboBox<String> groupComboBox;
    private ListView<String> rulesListView;
    private TextField targetDirField;


    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
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

        root.setTop(topPane);

        
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

        Button addStringRuleBtn = new Button("String Rule");
        Button addExtensionRuleBtn = new Button("Extension Rule");
        Button addCategoryRuleBtn = new Button("Category Rule");
        Button addNameRuleBtn = new Button("Name Rule");
        Button addLastAccessedRuleBtn = new Button("Last Accessed Rule");
        ruleButtonsBox.getChildren().addAll(addStringRuleBtn, addExtensionRuleBtn, addCategoryRuleBtn,
                addNameRuleBtn, addLastAccessedRuleBtn);
        rulesBox.getChildren().addAll(rulesLabel, rulesListView, ruleButtonsBox);

        // Button events for rules
        addCategoryRuleBtn.setOnAction(e -> createCategoryRuleUI());
        addExtensionRuleBtn.setOnAction(e -> createExtensionRuleUI());
        addStringRuleBtn.setOnAction(e -> createStringRuleUI());
        addNameRuleBtn.setOnAction(e -> createNameRuleUI());
        addLastAccessedRuleBtn.setOnAction(e -> createLastAccessedRuleUI());

        // Target Path Section
        VBox targetPathBox = new VBox(10);
        targetPathBox.setPrefWidth(200);
        
        Label targetPathLabel = new Label("Target Path:");
        targetPathLabel.getStyleClass().add("section-label");
        targetPathLabel.setMaxWidth(Double.MAX_VALUE);
        targetPathLabel.setAlignment(Pos.CENTER);


        targetDirField = new TextField();
        targetDirField.setEditable(false);
        Button selectTargetBtn = new Button("Select Target Dir");
        selectTargetBtn.setOnAction(e -> createTargetUI(primaryStage));
        targetPathBox.getChildren().addAll(targetPathLabel, targetDirField, selectTargetBtn);

        // workFlowBox add all sections
        workflowBox.getChildren().addAll(watchPathsBox, rulesBox, targetPathBox);
        root.setCenter(workflowBox);

        // Bottom: Finalize Button
        Button finalizeBtn = new Button("Finalize");
        finalizeBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");
        finalizeBtn.setOnAction(e -> finalizeGroups());
        root.setBottom(finalizeBtn);
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

        // Populate group dropdown
        updateGroupComboBox();

        try {
            watchService = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            System.err.println("Failed to create WatchService: " + e.getMessage());
            return;
        }

        primaryStage.setScene(scene);
        primaryStage.setTitle("NeatFile");
        primaryStage.show();

        startFileWatcher();
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
            targetDirField.setText(path.toString());
            if (currentGroup != null) {
                currentGroup.setTargetDirectory(path);
            }
        }
    }

    private void createNewGroup() {
        NeatGroup group = new NeatGroup(new HashSet<>(), null);
        groups.add(group);
        updateGroupComboBox();
        groupComboBox.getSelectionModel().selectLast();

        targetDirField.clear(); // clear target path field
        watchDirsListView.getItems().clear(); // clear the watch path list
        rulesListView.getItems().clear();     // clear rules list if needed
    }

    private void updateGroupComboBox() {
        groupComboBox.getItems().clear();
        for (int i = 0; i < groups.size(); i++) {
            groupComboBox.getItems().add("Group " + (i + 1));
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
                    targetDirField.clear();
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
                targetDirField.setText(targetDir.toString());
            } else {
                targetDirField.clear(); // Or setText("No target selected") if you'd prefer
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

    private void createLastAccessedRuleUI() {
        TextInputDialog dialog = new TextInputDialog(" ");
        dialog.setTitle("Add Last Accessed Rule");
        dialog.setHeaderText("Enter the number of days (matches files last accessed longer ago than this)");
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
                    Rule rule = new LastAccessedRule(daysValue);
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

    private void finalizeGroups() {
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
        } catch (IOException e) {
            System.err.println("Failed to write to groups.json: " + e.getMessage());
            e.printStackTrace();
        }

        for(NeatGroup group : groups) {
            if(!organizer.addGroup(group)){
                System.out.println("Failed to add group to Organizer: " + group);
            };
        }

        updateWatchDirectories(watchService);
    }

    private void startFileWatcher() {
        watchServiceThread = new Thread(() -> {
            try {
                watchService = FileSystems.getDefault().newWatchService();
                updateWatchDirectories(watchService);

                while (running && !Thread.currentThread().isInterrupted()) {
                    try {
                        WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                        if (key != null) {
                            for (WatchEvent<?> event : key.pollEvents()) {
                                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE ||
                                        event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                    Path fileName = (Path) event.context();
                                    Path dir = (Path) key.watchable();
                                    Path fullPath = dir.resolve(fileName);
                                    
                                    System.out.println("Detected change in directory: " + dir);
                                    System.out.println("Full file path: " + fullPath);
                                    
                                    organizer.processFile(fullPath);
                                }
                            }
                            key.reset();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                watchService.close();
            } catch (IOException e) {
                if (running) {
                    System.err.println("WatchService error: " + e.getMessage());
                }
            } finally {
                try {
                    if (watchService != null) {
                        watchService.close();
                    }
                } catch (Exception e) {
                    System.err.println("Failed to close WatchService: " + e.getMessage());
                }
            }
        });
        watchServiceThread.setDaemon(true);
        watchServiceThread.start();
    }

    private void startManualScanner() {
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
    

    private void updateWatchDirectories(WatchService watchService) {
        try {
            System.out.println("Updating watcher with directories:");

            Set<Path> allDirs = groups.stream()
            .flatMap(g -> g.getWatchDirectories().stream())
            .collect(Collectors.toSet());
            for (Path dir : allDirs) {
                if (Files.isDirectory(dir)) {
                    dir.register(watchService,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY);
                    System.out.println("Registered watch directory: " + dir);
                } else {
                    System.out.println("Skipping watch directory: " + dir);
                }
            }
        } catch (IOException e) {
            System.err.println("Error updating watch directories: " + e.getMessage());
        }
    }

    private void shutdown() {
        running = false;
        if (watchServiceThread != null) {
            watchServiceThread.interrupt();
            try {
                watchServiceThread.join();
            } catch (InterruptedException e) {
                System.err.println("Interrupted while shutting down: " + e.getMessage());
            }
        }
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            System.err.println("Failed to close WatchService: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        launch(args);
    }
}
