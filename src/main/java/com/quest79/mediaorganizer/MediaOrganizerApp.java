package com.quest79.mediaorganizer;

import com.quest79.mediaorganizer.ui.MainView;
import com.quest79.mediaorganizer.util.AppLog;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class MediaOrganizerApp extends Application {

    @Override
    public void init() {
        AppLog.configure();
    }

    @Override
    public void start(Stage stage) {
        MainView view = new MainView(stage);
        Scene scene = new Scene(view, 1500, 880);
        scene.getStylesheets().add(
                MediaOrganizerApp.class.getResource("/styles.css").toExternalForm()
        );

        stage.setTitle("Media Organizer");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
