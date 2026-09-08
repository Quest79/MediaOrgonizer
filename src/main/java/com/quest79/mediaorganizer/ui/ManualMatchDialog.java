package com.quest79.mediaorganizer.ui;

import com.quest79.mediaorganizer.metadata.MetadataCandidate;
import com.quest79.mediaorganizer.metadata.MetadataProvider;
import com.quest79.mediaorganizer.metadata.MetadataQuery;
import com.quest79.mediaorganizer.model.MediaFile;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

public final class ManualMatchDialog extends Dialog<MetadataCandidate> {

    private final MetadataProvider provider;
    private final MediaFile file;
    private final TextField queryField = new TextField();
    private final Button searchButton = new Button("Search");
    private final Label statusLabel = new Label();
    private final ListView<MetadataCandidate> results = new ListView<>();
    private final ButtonType useMatch = new ButtonType("Use Match", ButtonBar.ButtonData.OK_DONE);

    public ManualMatchDialog(
            Window owner,
            MetadataProvider provider,
            MediaFile file,
            String initialQuery
    ) {
        this.provider = provider;
        this.file = file;

        initOwner(owner);
        setTitle("Find Metadata Match");
        setHeaderText("Choose what this file actually belongs to");

        queryField.setText(initialQuery == null ? "" : initialQuery);
        queryField.setPromptText("Search anime title...");
        HBox.setHgrow(queryField, Priority.ALWAYS);

        HBox searchRow = new HBox(8, queryField, searchButton);

        results.setPrefHeight(420);
        results.setCellFactory(list -> new CandidateCell());

        VBox content = new VBox(
                10,
                new Label("Search " + provider.displayName()),
                searchRow,
                statusLabel,
                results
        );
        content.setPadding(new Insets(8));
        content.setPrefWidth(720);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(useMatch, ButtonType.CANCEL);

        Button useButton = (Button) getDialogPane().lookupButton(useMatch);
        useButton.setDisable(true);

        results.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) ->
                useButton.setDisable(selected == null)
        );

        searchButton.setOnAction(event -> search());
        queryField.setOnAction(event -> search());

        setResultConverter(button -> button == useMatch
                ? results.getSelectionModel().getSelectedItem()
                : null);

        Platform.runLater(this::search);
    }

    private void search() {
        String queryText = queryField.getText() == null ? "" : queryField.getText().trim();
        if (queryText.isBlank()) {
            statusLabel.setText("Enter a title to search.");
            return;
        }

        searchButton.setDisable(true);
        results.getItems().clear();
        statusLabel.setText("Searching " + provider.displayName() + " for “" + queryText + "”...");

        MetadataQuery query = new MetadataQuery(
                queryText,
                file.parsed().year(),
                file.parsed().season(),
                file.parsed().episodeStart(),
                file.parsed().absoluteEpisode(),
                file.parsed().kind()
        );

        provider.search(query).whenComplete((matches, error) ->
                Platform.runLater(() -> {
                    searchButton.setDisable(false);

                    if (error != null) {
                        Throwable cause = error.getCause() == null ? error : error.getCause();
                        statusLabel.setText(provider.displayName() + " search failed: " + cause.getMessage());
                        return;
                    }

                    results.getItems().setAll(matches);
                    if (matches.isEmpty()) {
                        statusLabel.setText("No matches found. Try a shorter or alternate title.");
                    } else {
                        statusLabel.setText(matches.size() + " possible matches. Select the correct one.");
                        results.getSelectionModel().selectFirst();
                    }
                })
        );
    }

    private static final class CandidateCell extends ListCell<MetadataCandidate> {
        @Override
        protected void updateItem(MetadataCandidate item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                return;
            }

            setText(item.displayText());
            setWrapText(true);
        }
    }
}
