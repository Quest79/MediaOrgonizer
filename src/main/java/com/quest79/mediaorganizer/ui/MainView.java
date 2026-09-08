package com.quest79.mediaorganizer.ui;

import com.quest79.mediaorganizer.model.MediaFile;
import com.quest79.mediaorganizer.model.MatchStatus;
import com.quest79.mediaorganizer.scanner.MediaScanner;
import com.quest79.mediaorganizer.settings.LocalSettings;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MainView extends BorderPane {

    private static final Logger LOG = Logger.getLogger(MainView.class.getName());

    private final Stage stage;
    private final LocalSettings settings = new LocalSettings();
    private final MediaScanner scanner = new MediaScanner();
    private final ObservableList<MediaFile> rows = FXCollections.observableArrayList();

    private final TextField folderField = new TextField();
    private final Button browseButton = new Button("Browse");
    private final Button scanButton = new Button("Scan");
    private final Button manualMatchButton = new Button("Manual Match");
    private final Button applyButton = new Button("Apply Changes");
    private final Button undoButton = new Button("Undo");
    private final TableView<MediaFile> table = new TableView<>(rows);
    private final ProgressBar progressBar = new ProgressBar(0);
    private final Label statusLabel = new Label("Choose a media folder to begin.");
    private final Label countLabel = new Label("0 files");
    private final Label highConfidenceLabel = new Label("0 high confidence");
    private final Label reviewLabel = new Label("0 need review");
    private final TextArea details = new TextArea();

    public MainView(Stage stage) {
        this.stage = stage;
        getStyleClass().add("app-root");
        setTop(buildHeader());
        setCenter(buildContent());
        setBottom(buildFooter());
        configureActions();
        restoreLastFolder();
    }

    private VBox buildHeader() {
        Label title = new Label("Media Organizer");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Scan first. Understand the files. Preview everything. Change nothing until you approve it.");
        subtitle.getStyleClass().add("app-subtitle");

        folderField.setPromptText("Select a movie, anime, or TV library folder...");
        HBox.setHgrow(folderField, Priority.ALWAYS);

        scanButton.getStyleClass().add("primary-button");

        HBox chooser = new HBox(10, folderField, browseButton, scanButton);
        chooser.setAlignment(Pos.CENTER_LEFT);

        HBox stats = new HBox(12,
                statCard(countLabel),
                statCard(highConfidenceLabel),
                statCard(reviewLabel)
        );

        VBox header = new VBox(8, title, subtitle, chooser, stats);
        header.setPadding(new Insets(22, 24, 14, 24));
        header.getStyleClass().add("header");
        return header;
    }

    private HBox statCard(Label label) {
        HBox box = new HBox(label);
        box.getStyleClass().add("stat-card");
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private SplitPane buildContent() {
        configureTable();

        details.setEditable(false);
        details.setWrapText(true);
        details.setPromptText("Select a scanned file to see parsing evidence.");
        details.getStyleClass().add("details-area");

        Label detailsTitle = new Label("Selected file");
        detailsTitle.getStyleClass().add("section-title");

        manualMatchButton.setDisable(true);
        manualMatchButton.setTooltip(new Tooltip("Enabled in the manual metadata-match milestone."));

        VBox detailPane = new VBox(10, detailsTitle, details, new Separator(), manualMatchButton);
        detailPane.setPadding(new Insets(14));
        detailPane.getStyleClass().add("detail-pane");
        VBox.setVgrow(details, Priority.ALWAYS);

        SplitPane split = new SplitPane(table, detailPane);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.76);
        split.setPadding(new Insets(0, 24, 0, 24));
        return split;
    }

    private HBox buildFooter() {
        progressBar.setPrefWidth(220);
        progressBar.setVisible(false);

        applyButton.setDisable(true);
        undoButton.setDisable(true);
        applyButton.setTooltip(new Tooltip("Filesystem writes are intentionally disabled in this milestone."));
        undoButton.setTooltip(new Tooltip("Undo becomes available with operation history."));

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox footer = new HBox(10, progressBar, statusLabel, spacer, undoButton, applyButton);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(14, 24, 18, 24));
        return footer;
    }

    private void configureTable() {
        table.setPlaceholder(new Label("No scan results yet."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<MediaFile, String> status = column("Status", 120,
                file -> file.status().label());
        status.setCellFactory(column -> new StatusCell());

        TableColumn<MediaFile, String> path = column("Current path", 270, MediaFile::relativePath);
        TableColumn<MediaFile, String> kind = column("Type", 120, file -> file.parsed().kind().label());
        TableColumn<MediaFile, String> title = column("Parsed title", 220, file -> file.parsed().title());
        TableColumn<MediaFile, String> year = column("Year", 70,
                file -> value(file.parsed().year()));
        TableColumn<MediaFile, String> season = column("Season", 70,
                file -> file.parsed().seasonText());
        TableColumn<MediaFile, String> episode = column("Episode", 85,
                file -> file.parsed().episodeText());
        TableColumn<MediaFile, String> confidence = column("Confidence", 95,
                file -> file.parsed().confidenceText());
        TableColumn<MediaFile, String> release = column("Release info", 210,
                file -> file.parsed().releaseSummary());
        TableColumn<MediaFile, String> preview = column("Normalized preview", 280,
                file -> file.parsed().normalizedPreview());

        table.getColumns().addAll(
                status, path, kind, title, year, season, episode,
                confidence, release, preview
        );

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            showDetails(selected);
        });
    }

    private TableColumn<MediaFile, String> column(
            String name,
            double width,
            java.util.function.Function<MediaFile, String> extractor
    ) {
        TableColumn<MediaFile, String> column = new TableColumn<>(name);
        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(extractor.apply(cell.getValue())));
        return column;
    }

    private void configureActions() {
        browseButton.setOnAction(event -> chooseFolder());
        scanButton.setOnAction(event -> startScan());
        folderField.setOnAction(event -> startScan());
    }

    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Media Library");

        Path existing = parseSelectedFolder();
        if (existing != null && existing.toFile().isDirectory()) {
            chooser.setInitialDirectory(existing.toFile());
        }

        File selected = chooser.showDialog(stage);
        if (selected != null) {
            folderField.setText(selected.getAbsolutePath());
            settings.setLastLibrary(selected.toPath());
        }
    }

    private void startScan() {
        Path root = parseSelectedFolder();
        if (root == null || !root.toFile().isDirectory()) {
            statusLabel.setText("Select a valid folder first.");
            return;
        }

        settings.setLastLibrary(root);
        setScanning(true);
        rows.clear();
        details.clear();
        statusLabel.setText("Scanning " + root + "...");

        AtomicInteger done = new AtomicInteger();

        Task<List<MediaFile>> task = new Task<>() {
            @Override
            protected List<MediaFile> call() throws Exception {
                return scanner.scan(root, (current, total) -> {
                    done.set(current);
                    if (total > 0) {
                        updateProgress(current, total);
                        if (current == 1 || current % 50 == 0 || current == total) {
                            updateMessage("Parsing " + current + " of " + total + " media files...");
                        }
                    }
                });
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(event -> {
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            rows.setAll(task.getValue());
            updateStats();
            statusLabel.setText("Scan complete. No files were modified.");
            setScanning(false);
            if (!rows.isEmpty()) {
                table.getSelectionModel().selectFirst();
            }
        });

        task.setOnFailed(event -> {
            progressBar.progressProperty().unbind();
            statusLabel.textProperty().unbind();
            Throwable error = task.getException();
            LOG.log(Level.SEVERE, "Media scan failed.", error);
            statusLabel.setText("Scan failed: " + (error == null ? "Unknown error" : error.getMessage()));
            setScanning(false);
        });

        Thread thread = new Thread(task, "media-library-scan");
        thread.setDaemon(true);
        thread.start();
    }

    private void setScanning(boolean scanning) {
        browseButton.setDisable(scanning);
        scanButton.setDisable(scanning);
        folderField.setDisable(scanning);
        progressBar.setVisible(scanning);
        if (!scanning) {
            progressBar.setProgress(0);
        }
    }

    private void updateStats() {
        long high = rows.stream().filter(file -> file.status() == MatchStatus.HIGH_CONFIDENCE).count();
        long review = rows.stream().filter(file ->
                file.status() == MatchStatus.NEEDS_REVIEW || file.status() == MatchStatus.UNKNOWN
        ).count();

        countLabel.setText(rows.size() + (rows.size() == 1 ? " file" : " files"));
        highConfidenceLabel.setText(high + " high confidence");
        reviewLabel.setText(review + " need review");
    }

    private void showDetails(MediaFile file) {
        if (file == null) {
            details.clear();
            return;
        }

        StringBuilder text = new StringBuilder();
        text.append("Current path\n").append(file.path()).append("\n\n");
        text.append("Program interpretation\n");
        text.append("Type: ").append(file.parsed().kind().label()).append("\n");
        text.append("Title: ").append(file.parsed().title()).append("\n");
        text.append("Year: ").append(value(file.parsed().year())).append("\n");
        text.append("Season: ").append(file.parsed().seasonText()).append("\n");
        text.append("Episode: ").append(file.parsed().episodeText()).append("\n");
        text.append("Confidence: ").append(file.parsed().confidenceText()).append("\n");
        text.append("Release info: ").append(file.parsed().releaseSummary()).append("\n\n");
        text.append("Normalized preview\n").append(file.parsed().normalizedPreview()).append("\n\n");
        text.append("Why it was parsed this way\n");
        for (String reason : file.parsed().evidence()) {
            text.append("• ").append(reason).append("\n");
        }
        text.append("\nFilesystem action\nNone. Preview-only milestone.");

        details.setText(text.toString());
        Platform.runLater(() -> details.positionCaret(0));
    }

    private Path parseSelectedFolder() {
        String text = folderField.getText();
        if (text == null || text.isBlank()) return null;
        try {
            return Path.of(text.trim()).toAbsolutePath().normalize();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void restoreLastFolder() {
        settings.lastLibrary().ifPresent(path -> folderField.setText(path.toString()));
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private static final class StatusCell extends TableCell<MediaFile, String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            getStyleClass().removeAll(
                    "status-high", "status-parsed", "status-review", "status-unknown",
                    "status-duplicate", "status-conflict"
            );

            if (empty || item == null) {
                setText(null);
                return;
            }

            setText(item);
            String normalized = item.toLowerCase(Locale.ROOT);
            if (normalized.contains("high")) getStyleClass().add("status-high");
            else if (normalized.equals("parsed")) getStyleClass().add("status-parsed");
            else if (normalized.contains("review")) getStyleClass().add("status-review");
            else if (normalized.contains("duplicate")) getStyleClass().add("status-duplicate");
            else if (normalized.contains("conflict")) getStyleClass().add("status-conflict");
            else getStyleClass().add("status-unknown");
        }
    }
}
