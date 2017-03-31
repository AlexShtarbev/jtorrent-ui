package com.jtorrent.ui;
	
import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

import com.jtorrent.torrent.TorrentClient;
import com.jtorrent.ui.view.TableController;

public class MainApp extends Application {
	private Stage _primaryStage;
    private BorderPane _rootLayout;
	
    private TorrentClient _torrentClient;
    private TableController _tableController;
    
	@Override
	public void start(Stage primaryStage) {
		_primaryStage = primaryStage;
        _primaryStage.setTitle("jTorrent");

    	_torrentClient = new TorrentClient();
    	_torrentClient.start();

		initRootLayout();
		loadTableLayout();		
	}
	
	public void initRootLayout() {
		try {
            // Load root layout from FXML file.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
	        _rootLayout = (BorderPane) loader.load();
	      
            // Show the scene containing the root layout.
            Scene scene = new Scene(_rootLayout);
            _primaryStage.setScene(scene);
            _primaryStage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	public void loadTableLayout() {
		try {
			FXMLLoader loader = new FXMLLoader();
	        loader.setLocation(MainApp.class.getResource("view/TableLayout.fxml"));
	        AnchorPane tableLayout = (AnchorPane) loader.load();
	        
	        _rootLayout.setCenter(tableLayout);
	        
	        // Set up the controller.
	        _tableController = loader.getController();
	        _tableController.setMainApp(this);
	        _tableController.setTorrentClient(_torrentClient);
		} catch (IOException e) {
			// TODO - fail gracefully.
			e.printStackTrace();
		}
	}
	
    public Stage getPrimaryStage() {
        return _primaryStage;
    }
    
	public static void main(String[] args) {
		launch(args);
	}
}

