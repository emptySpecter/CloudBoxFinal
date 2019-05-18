package com.geekbrains.april.cloud.box.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CloudBoxClient extends Application {
    private Thread incomeDispatcherThread;
    private IncomeMessagesDispatcher incomeMessagesDispatcher;
    private Stage primaryStage;
    private LoginController loginController;
    private MainController mainController;
    private Properties prop;


    @Override
    public void start(Stage primaryStage) throws Exception {
        prop = new Properties();
        try (InputStream input = CloudBoxClient.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
                throw new IOException();
            }
            //load a properties file from class path
            prop.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        Network.start(Integer.parseInt(prop.getProperty("client.port")));
        incomeMessagesDispatcher = new IncomeMessagesDispatcher(this);
        incomeDispatcherThread = new Thread(incomeMessagesDispatcher);
        incomeDispatcherThread.setDaemon(true);
        incomeDispatcherThread.start();

        this.primaryStage = primaryStage;
        startAuthorization();
    }

    private void startAuthorization() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/login.fxml"));
        try {
            primaryStage.setScene(new Scene(fxmlLoader.load(), 400, 200));
        } catch (IOException e) {
            e.printStackTrace();
        }
        loginController = fxmlLoader.getController();
        primaryStage.setTitle("Authorization");
        primaryStage.show();
    }

    public void onAuthorizationAnswer(boolean isAuthorized) {
        Alert alert;
        if (isAuthorized) {
            alert = new Alert(Alert.AlertType.INFORMATION, "Authorization successful", ButtonType.OK);
        } else {
            alert = new Alert(Alert.AlertType.ERROR, "Authorization failed", ButtonType.OK);
        }
        alert.showAndWait();

        if (isAuthorized) {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/main.fxml"));
            try {
                primaryStage.setScene(new Scene(fxmlLoader.load()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            mainController = fxmlLoader.getController();
            mainController.setUser(loginController.user);
            mainController.setRoot_dir(prop.getProperty("client.root_dir"));
            incomeMessagesDispatcher.setMainController(mainController);
            incomeMessagesDispatcher.setRoot_dir(prop.getProperty("client.root_dir"));
            incomeMessagesDispatcher.setChunksize(Integer.parseInt(prop.getProperty("client.chunksize")));
            primaryStage.setTitle("CloudBox Client");
            primaryStage.centerOnScreen();
            primaryStage.show();
        }
    }

    @Override
    public void stop() throws Exception {
        System.out.println("CLOSING");
        incomeDispatcherThread.interrupt();
        Thread.currentThread().sleep(2000);
        Network.stop();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
