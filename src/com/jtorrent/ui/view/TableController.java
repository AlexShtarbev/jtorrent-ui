package com.jtorrent.ui.view;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.jtorrent.torrent.TorrentClient;
import com.jtorrent.torrent.TorrentSession;
import com.jtorrent.torrent.restore.RestoreManager;
import com.jtorrent.ui.MainApp;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class TableController {
	
	@FXML
    private TableView<TorrentTask> _torrentTable;
    @FXML
    private TableColumn<TorrentTask, String> _nameColumn;
    @FXML
    private TableColumn<TorrentTask, String> _sizeColumn;
    @FXML
    private TableColumn<TorrentTask, String> _statusColumn;
    @FXML
    private TableColumn<TorrentTask, Double> _progressColumn;
    @FXML
    private TableColumn<TorrentTask, String> _downloadColumn;
    @FXML
    private TableColumn<TorrentTask, String> _uploadColumn;
    
    // Reference to the main application.
    private MainApp _mainApp;
    
    private final List<String> _torrentFiles;    
    private TorrentClient _torrentClient;
    
    private final ExecutorService _executor;
    
    public TableController() {
    	_torrentFiles = new LinkedList<>();
    	_executor =  Executors.newCachedThreadPool(new ThreadFactory() {
    	      @Override
    	      public Thread newThread(Runnable r) {
    	        Thread t = new Thread(r);
    	        t.setDaemon(true);
    	        return t;
    	      }
    	    });
	}    

    /**
     * Initializes the controller class. This method is automatically called
     * after the fxml file has been loaded.
     */
    @FXML
    private void initialize() {
    	initTable();
    }
    
    public void setMainApp(MainApp mainApp) {
    	_mainApp = mainApp;
    }
    
    public void setTorrentClient(TorrentClient torrentClient) {
    	_torrentClient = torrentClient;
    	restorePreviousSession();
    }
    
    
    private void restorePreviousSession() {
    	RestoreManager restoreManger = _torrentClient.getRestoreManager();
    	try {
			List<TorrentSession> sessions = restoreManger.restroreTorrentSessions(_torrentClient.getConnectionService(),
					_torrentClient.getClientPeer());
			for(TorrentSession session: sessions) {
				addNewTorrent(session);
			}
		} catch (Exception e) {
			// TODO handle quietly
			throw new IllegalStateException("Unable to restore torrent sessions: " + e.getMessage());
		}
    }
    
    private void initTable() {
    	_nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
    			formatFileName(cellData.getValue().getTorrentSession().getTorrentFileName())));
    	_sizeColumn.setCellValueFactory(cellData -> {
    		TorrentSession session = cellData.getValue().getTorrentSession();
    		long length = session.getMetaInfo().getInfoDictionary().getLength();
    		String humanReadble = formatSize(length, true);
    		return new SimpleStringProperty(humanReadble);
    	});
    	
    	_statusColumn.setCellValueFactory(new PropertyValueFactory<TorrentTask, String>(
			        "message"));
    			
    	_progressColumn.setCellValueFactory(new PropertyValueFactory<TorrentTask, Double>(
			        "progress"));
    	_progressColumn
			        .setCellFactory(column -> {
			            return new TableCell<TorrentTask, Double>() {
			            	
			            	final ProgressBar _progressBar = new ProgressBar();
			            	
			                @Override
			                protected void updateItem(Double item, boolean empty) {
			                    super.updateItem(item, empty);

			                    if (item == null || empty) {
			                        this.setText(null);
			                        this.setGraphic(null);
			                        this.setStyle("");
			                    } else {
			                    	setGraphic(_progressBar);		
			                    	setAlignment(Pos.CENTER);
			                        setGraphic(_progressBar);
			                        
			                        // If the cell receives a negative value, then the operation
			                        // of adding an item is still on going and should display
			                        // an indeterminate status.
			                    	if(item < 0) {
			                    		_progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
			                    		return;
			                    	}
			                    	
			                    	String formattedString = String.format("%4.1f",item*100);
			                    	if(formattedString.length() < 5) {
			                    		formattedString = "  " + formattedString;
			                    	}
			                        setText(String.format("%s%%", formattedString));
			                        
			                        _progressBar.setProgress(item);
			                    }
			                }
			            };
			         });
    
    	_downloadColumn.setCellValueFactory(new PropertyValueFactory<TorrentTask, String>(
		        "message"));
    	_uploadColumn.setCellValueFactory(new PropertyValueFactory<TorrentTask, String>(
		        "message"));
    }
    
    private String formatFileName(String fileName) {
    	// FIrst change the path to be POSIX friendly.
    	int idx = fileName.replaceAll("\\\\", "/").lastIndexOf("/");
    	// Extract the name, including the file extension.
    	String nameAndExt = idx >= 0 ? fileName.substring(idx + 1) : fileName;
    	// Find where the name ends.
    	idx = nameAndExt.lastIndexOf('.');
    	// Extract the name of the file.
    	String name = nameAndExt.substring(0, idx);
    	return name;
    }
    
    public static String formatSize(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
   
    public void addNewTorrent(TorrentSession torrentSession) {
    	if(isInTable(torrentSession)) {
    		showDialog(AlertType.INFORMATION, "Torrent added", null,
    				"Torrent already added to list.");
            return;
    	}
    	
    	try {
			TorrentTask task = new TorrentTask(torrentSession, _torrentClient);
			// Add the torrent to the table
			_torrentTable.getItems().add(task);
			// Start the UI update task.
			_executor.submit(task);		

    		// Add the torrent to the list.
			_torrentFiles.add(torrentSession.getTorrentFileName());
			// Start the torrent download
			_torrentClient.startNewSession(torrentSession);
    	} catch (Exception e) {
			// Error occurred.
    		showDialog(AlertType.WARNING, "Cannot start torrent", "Error selecting torrent",
    				e.getLocalizedMessage());
		}
    }
    
    public void addNewTorrent(String fileName, String directory) {
    	try {
			TorrentSession torrentSession = _torrentClient.startNewSession(fileName, directory);
			addNewTorrent(torrentSession);
		} catch (Exception e) {
			// Error occurred.
			showDialog(AlertType.WARNING, "Cannot start torrent", "Error selecting torrent",
					e.getLocalizedMessage());
		}
    }
    
    public boolean isInTable(TorrentSession torrentSession) {
    	return _torrentFiles.contains(torrentSession.getTorrentFileName());
    }
    
    /**
     * Displays a dialog. If you do not want to dialog to use a parameter, set it to null.
     * @param type
     * @param title
     * @param header
     * @param content
     */
    private void showDialog(AlertType type, String title, String header, String content) {
    	Alert alert = new Alert(type);
        alert.initOwner(_mainApp.getPrimaryStage());
        if(title != null) {
        	alert.setTitle(title);
        }
        
        if(header != null) {
        	alert.setHeaderText(header);
        }
        
        if(content != null) {
        	alert.setContentText(content);
        }

        alert.showAndWait();
    }
    
    @FXML
    public void handleStartTorrent() {
    	TorrentTask task = _torrentTable.getSelectionModel().getSelectedItem();
    	if(task == null) {
    		return;
    	}
    	
    	if(!task.isRemoved()) {
    		try {
				task.startTorrentSession();
			} catch (Exception e) {
				// TODO - handle quietly
			}
    	}
    }
  
    @FXML
    public void handleStopTorrent() {
    	TorrentTask task = _torrentTable.getSelectionModel().getSelectedItem();
    	if(task == null) {
    		return;
    	}
    	
    	if(!task.isRemoved()) {
    		task.stopTorrentSession();			
    	}
    }
    
 private static class TorrentTask extends Task<Void> {
    	
    	private final TorrentSession _torrentSession;
    	private final TorrentClient _torrentClient;
    	private volatile boolean _remove;
    	
    	public TorrentTask(TorrentSession torrentSession, TorrentClient torrentClient) {
			_torrentSession = torrentSession;
			_torrentClient = torrentClient;
		}

		@Override
		protected Void call() throws Exception {
			updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, 1);
			updateMessage("Adding");
			while(!_remove) {
				if(_torrentSession.isQueuing()) {
					updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, 1);
					updateMessage("Queuing");
				} else if (_torrentSession.isChecking()) {
					double progress = _torrentSession.getCheckedPiecesPrgress() * 100;
					updateProgress(progress, 100);
					updateMessage("Checking");
				} else if (_torrentSession.isDownloading()) {
					double progress = _torrentSession.getPieceRepository().completedPercent();
					updateProgress(progress, 100);
					updateMessage("Downloading");
				} else if (_torrentSession.isFinilizing()) {
					updateProgress(1, 1);
					updateMessage("Finilizing");
				} else if (_torrentSession.isSeeding()) {
					updateProgress(1, 1);
					updateMessage("Seeding");
				} else if (_torrentSession.isStopped()) {
					double progress = _torrentSession.getPieceRepository().completedPercent();
					updateProgress(progress, 100);
					updateMessage("Stopped");
				}
			}
			
			return null;
		}
		
		public TorrentSession getTorrentSession() {
			return _torrentSession;
		}
    	
		public void stopTorrentSession() {
			_torrentClient.stopTorrentSession(_torrentSession);
		}
		
		public void startTorrentSession() throws Exception{
			_torrentClient.resumeTorrentSession(_torrentSession);
		}
		
		public boolean isRemoved() {
			return _remove;
		}
    }
}
