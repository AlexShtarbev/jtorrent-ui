package com.jtorrent.ui;
	
import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

import java.util.Random;
import java.util.concurrent.*;

import com.jtorrent.torrent.TorrentClient;
import com.jtorrent.ui.view.TableController;

import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

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
	
	@SuppressWarnings("unchecked")
	private void testProgress() {
		TableView<TestTask> table = (TableView<TestTask>) _primaryStage.getScene().lookup("#torrentsTable");
		ObservableList<TableColumn<TestTask, ?>> cols = table.getColumns();
		TableColumn<TestTask, String> statusCol = null;
		TableColumn<TestTask, Double> progressCol = null;
		for(TableColumn<TestTask, ?> col : cols) {
			if(col.getId() == null) {
				continue;
			}
			if(col.getId().equals("statusColumn")) {
				statusCol = (TableColumn<TestTask, String>)col;
			}
			
			if(col.getId().equals("progressColumn")) {
				progressCol = (TableColumn<TestTask, Double>)col;
			}
		}
		
		Random rng = new Random();
	    for (int i = 0; i < 20; i++) {
	      table.getItems().add(
	          new TestTask(rng.nextInt(3000) + 2000, rng.nextInt(30) + 20));
	    }
		
		 statusCol.setCellValueFactory(new PropertyValueFactory<TestTask, String>(
			        "message"));
		
		 progressCol.setCellValueFactory(new PropertyValueFactory<TestTask, Double>(
			        "progress"));
			    progressCol
			        .setCellFactory(column -> {
			            return new TableCell<TestTask, Double>() {
			            	
			            	final ProgressBar progress = new ProgressBar();
			            	
			                @Override
			                protected void updateItem(Double item, boolean empty) {
			                    super.updateItem(item, empty);

			                    if (item == null || empty) {
			                        this.setText(null);
			                        this.setGraphic(null);
			                        this.setStyle("");
			                    } else {
			                    	
			                    	if(item < 0) {
			                    		item = new Double(0.0);
			                    	}
			                    	String formattedString = String.format("%4.1f",item*100);
			                    	if(formattedString.length() < 5) {
			                    		formattedString = "  " + formattedString;
			                    	}
			                        this.setText(String.format("%s", formattedString));
			                        this.setAlignment(Pos.CENTER);
			                        setGraphic(progress);
			                        progress.setProgress(item);
			                    }
			                }
			            };
			         });
			    
			    ExecutorService executor = Executors.newFixedThreadPool(table.getItems().size(), new ThreadFactory() {
				      @Override
				      public Thread newThread(Runnable r) {
				        Thread t = new Thread(r);
				        t.setDaemon(true);
				        return t;
				      }
				    });
				    
				    
				    for (TestTask task : table.getItems()) {
				      executor.execute(task);
				    }
		
	}
	
	static class TestTask extends Task<Void> {
	
	    private final int waitTime; // milliseconds
	    private final int pauseTime; // milliseconds
	
	    public static final int NUM_ITERATIONS = 100;
	
		TestTask(int waitTime, int pauseTime) {
			this.waitTime = waitTime;
			this.pauseTime = pauseTime;
		}
	
		@Override
		protected Void call() throws Exception {
			this.updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, 1);
			this.updateMessage("Waiting...");
			Thread.sleep(waitTime);
			this.updateMessage("Running...");
			for (int i = 0; i < NUM_ITERATIONS; i++) {
				double progress = (1.0 * i) / NUM_ITERATIONS;
				updateProgress(progress, 1);
				Thread.sleep(pauseTime);
			}
			this.updateMessage("Done");
			this.updateProgress(1, 1);
			return null;
		}
	
	  }
}

