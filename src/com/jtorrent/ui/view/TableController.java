package com.jtorrent.ui.view;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.jtorrent.peer.Peer;
import com.jtorrent.peer.PeerManager;
import com.jtorrent.peer.PeerManager.Rates;
import com.jtorrent.storage.FileStore;
import com.jtorrent.torrent.TorrentClient;
import com.jtorrent.torrent.TorrentSession;
import com.jtorrent.torrent.restore.RestoreManager;
import com.jtorrent.ui.MainApp;
import com.jtorrent.ui.model.PeerDisplay;
import com.jtorrent.ui.utils.Utils;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class TableController {
	
	private enum Action {
		START,
		STOP,
		REMOVE
	}
	
	// Table view fxids.
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
    
    // Tabs
    @FXML
    private TabPane _tabPane;
    
    // Info tab
    @FXML
    private Label _totalSizeLabel;
    @FXML
    private Label _createdOnLabel;
    @FXML
    private Label _createdByLabel;
    @FXML
    private Label _hashLabel;
    @FXML
    private Label _commentLabel;
    @FXML
    private Label _piecesLabel;
    @FXML
    private Label _downloadedLabel;
    @FXML
    private Label _uploadedLabel;
    @FXML
    private Label _remainingLabel;
    @FXML
    private ListView<String> _filesListView;
    
    // Peers tab
    @FXML
    private TableView<PeerDisplay> _peersTable;
    @FXML
    private TableColumn<PeerDisplay, String> _ipColumn;
    @FXML
    private TableColumn<PeerDisplay, String> _peerIdColumn;
    @FXML
    private TableColumn<PeerDisplay, String> _downloadSpeedColumn;
    @FXML
    private TableColumn<PeerDisplay, String> _uploadSpeedColumn;
    
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
    
    /////////////////////////////// Initialization  ///////////////////////////////
    
    @FXML
    private void initialize() {
    	initTableView();
    	
    	initNameColumn();
    	
    	initSizeColumn();
    	
    	initStatusColumn();
    	
    	initProgressColumn();
    
    	initDownloadColumn();
    	
    	initUploadColumn();
    	
    	updateInfoTab(null);
    	
    	_torrentTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> updateInfoTab(newValue));
    	// Tabs
    	initTabPane();
    	initializePeersTable();
    }
    
    private void initTableView() {
    	_torrentTable.setRowFactory( tv -> {
    	    TableRow<TorrentTask> row = new TableRow<>();
    	    row.setOnMouseClicked(event -> {
    	    	// On double click open the file location of the downloading(ed)
    	    	// resources in the default system file explorer.
    	        if (event.getClickCount() == 2 && (! row.isEmpty()) ) {
    	            TorrentSession session = row.getItem().getTorrentSession();
    	            FileStore store = session.getFileStore();
    	            try {
					    Desktop.getDesktop().open(new File(store.getParentName()));
					} catch (IOException e) {
					    e.printStackTrace();
					}
    	        }
    	    });
    	    return row ;
    	});
    }
    
    private void initNameColumn() {
    	_nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
    			Utils.formatFileName(cellData.getValue().getTorrentSession().getTorrentFileName())));
    }
    
    private void initSizeColumn() {
    	_sizeColumn.setCellValueFactory(cellData -> {
    		TorrentSession session = cellData.getValue().getTorrentSession();
    		long length = session.getMetaInfo().getInfoDictionary().getLength();
    		String humanReadble = Utils.formatSize(length, true);
    		return new SimpleStringProperty(humanReadble);
    	});
    	
    	_sizeColumn.setStyle("-fx-alignment: CENTER;");
    }
    
    private void initStatusColumn() {
    	_statusColumn.setCellValueFactory(new PropertyValueFactory<TorrentTask, String>(
		        "message"));
    	_statusColumn.setCellFactory(column -> {
	        return new TableCell<TorrentTask, String>() {
	        	
	            @Override
	            protected void updateItem(String item, boolean empty) {
	                super.updateItem(item, empty);
	
	                if (item == null || empty || item.isEmpty()) {
	                    this.setText(null);
	                    this.setGraphic(null);
	                    this.setStyle("");
	                } else {
	                	int idx = item.indexOf('|');
	                	setText(item.substring(0, idx));
	                	setAlignment(Pos.CENTER);
	                }
	            }
	        };
	     });
    }
    
    private void initProgressColumn() {
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
		                    		setText("");
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
    }
    
    private void initDownloadColumn() {
    	_downloadColumn.setCellValueFactory(new PropertyValueFactory<TorrentTask, String>(
		        "message"));
    	
    	_downloadColumn.setCellFactory(column -> {
            return new TableCell<TorrentTask, String>() {
            	
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty || item.isEmpty()) {
                        this.setText(null);
                        this.setGraphic(null);
                        this.setStyle("");
                    } else {
                    	int idxStart = item.indexOf('|');
                    	int idxEnd = item.lastIndexOf('|');
                    	setText(item.substring(idxStart + 1, idxEnd));
                    	setAlignment(Pos.CENTER);
                    }
                }
            };
         });
    }
   
    private void initUploadColumn() {
    	_uploadColumn.setCellValueFactory(new PropertyValueFactory<TorrentTask, String>(
		        "message"));
    
    	_uploadColumn.setCellFactory(column -> {
            return new TableCell<TorrentTask, String>() {
            	
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty || item.isEmpty()) {
                        this.setText(null);
                        this.setGraphic(null);
                        this.setStyle("");
                    } else {
                    	int idx = item.lastIndexOf('|');
                    	setText(item.substring(idx + 1));
                    	setAlignment(Pos.CENTER);
                    }
                }
            };
         });
    }
    
    /////////////////////////////// Event handling  ///////////////////////////////
    
    public void addNewTorrent(TorrentSession torrentSession) {
    	if(_torrentFiles.contains(torrentSession.getTorrentFileName())) {
    		showDialog(AlertType.INFORMATION, "Torrent added", null,
    				"Torrent already added to list.");
            return;
    	}
    	
    	try {
    		_torrentClient.startNewSession(torrentSession);
    		submitSession(torrentSession);
    	} catch (Exception e) {
			// Error occurred.
    		showDialog(AlertType.WARNING, "Cannot start torrent", "Error selecting torrent",
    				e.getLocalizedMessage());
		}
    }
    
    public void addNewTorrent(String fileName, String directory) {
    	if( _torrentFiles.contains(fileName)) {
    		showDialog(AlertType.INFORMATION, "Torrent added", null,
    				"Torrent already added to list.");
            return;
    	}
    	
    	try {
    		// Start the torrent download
    		TorrentSession torrentSession = _torrentClient.startNewSession(fileName, directory);
    		submitSession(torrentSession);
		} catch (Exception e) {
			// Error occurred.
			showDialog(AlertType.WARNING, "Cannot start torrent", "Error selecting torrent",
					e.getLocalizedMessage());
		}
    }
    
    private void submitSession(TorrentSession torrentSession) {
    	TorrentTask task = new TorrentTask(torrentSession);
		// Add the torrent to the table
		_torrentTable.getItems().add(task);
		// Start the UI update task.
		_executor.submit(task);		

		// Add the torrent to the list.
		_torrentFiles.add(torrentSession.getTorrentFileName());
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
    
    @FXML
    public void handleRemoveTorrent() {
    	TorrentTask task = _torrentTable.getSelectionModel().getSelectedItem();
    	if(task == null) {
    		return;
    	}
    	int selectedIndex = _torrentTable.getSelectionModel().getSelectedIndex();
        if (selectedIndex < 0) {
        	return;
        } 
        task.remove();
        _torrentTable.getItems().remove(selectedIndex);
        runInBackground(Action.REMOVE, _torrentClient, task.getTorrentSession());
    }
    
    @FXML
    public void handleNewTorrent() {
    	// The file chooser for selecting a torrent.
    	final FileChooser fileChooser = new FileChooser();
    	configureFileChooser(fileChooser);
    	File torrentFile = fileChooser.showOpenDialog(_mainApp.getPrimaryStage());
        if (torrentFile == null) {
        	return;
        }
        
        // The file chooser for a destination for the torrent file(s).
        final DirectoryChooser directoryChooser = new DirectoryChooser();
        configureDirectoryChooser(directoryChooser, torrentFile);
        File directory = directoryChooser.showDialog(_mainApp.getPrimaryStage());
        if(directory == null) {
        	return;
        }
        
        addNewTorrent(torrentFile.getAbsolutePath(), directory.getAbsolutePath());
    }
    
    private void configureFileChooser(final FileChooser fileChooser) {      
		fileChooser.setTitle("Select a .torrent to open");
		// Open the file chooser to select from the user's home directory.
		fileChooser.setInitialDirectory(
		    new File(System.getProperty("user.home"))
		);                 
		fileChooser.getExtensionFilters().addAll(
		    new FileChooser.ExtensionFilter("All files", "*.*"),
		    new FileChooser.ExtensionFilter("Torrents", "*.torrent")
		);
    }
    
    private void configureDirectoryChooser(final DirectoryChooser chooser, File torrentFile) {
    	chooser.setTitle("Save in");
    	
    	chooser.setInitialDirectory(new File(torrentFile.getParent())); 
    }
    
    private class TorrentTask extends Task<Void> {
    	
    	private static final int UPDATE_SLEEP_INTERVAL_SECONDS = 1;
    	private static final int TAB_UPDATE_CYCLE = 3;
    	
    	private final TorrentSession _torrentSession;
    	private volatile boolean _remove;
    	
    	public TorrentTask(TorrentSession torrentSession) {
			_torrentSession = torrentSession;
		}

		@Override
		protected Void call() throws Exception {
			updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, 1);
			updateMessage("Adding|NaN|NaN");
			
			String status = null;
			String message = null;
			String format = "%s|%s/s|%s/s";
			
			// The update cycle for refreshing the tabs.The updates must not be 
			// done too often as the update requests will overwhelm the fx thread
			// and it will block. To remedy this, updates are sent every few cycles
			// to avoid eventual 
			int update = TAB_UPDATE_CYCLE;
			while(!_remove) {
				if(_torrentSession.isQueuing()) {
					updateProgress(ProgressIndicator.INDETERMINATE_PROGRESS, 1);
					status = "Queuing";
					updateMessage(status);
				} else if (_torrentSession.isChecking()) {
					double progress = _torrentSession.getCheckedPiecesPrgress() * 100;
					updateProgress(progress, 100);
					status = "Checking";
				} else if (_torrentSession.isDownloading()) {
					double progress = _torrentSession.getPieceRepository().completedPercent();
					updateProgress(progress, 100);
					status = "Downloading";
				} else if (_torrentSession.isFinilizing()) {
					updateProgress(1, 1);
				} else if (_torrentSession.isSeeding()) {
					updateProgress(1, 1);
					status = "Seeding";
				} else if (_torrentSession.isStopped()) {
					double progress = _torrentSession.getPieceRepository().completedPercent();
					updateProgress(progress, 100);
					status = "Stopped";
				}
				
				if(_torrentSession.isDownloading() || _torrentSession.isFinilizing() 
						|| _torrentSession.isSeeding()) {
					// If the currently selected item is this one, then the info tab
					// needs to be updated to reflect the new state changes.
					if(update == 0) {
						updateTab();
						update = TAB_UPDATE_CYCLE;
					} else {
						update--;
					}
				}
				
				Rates rates = _torrentSession.getPeerManager().getRates();
				String downloadRate = Utils.formatSize((long)rates.getDownloadRate(), true);
				String uploadRate = Utils.formatSize((long)rates.getUploadRate(), true);
				
				message = String.format(format, status, downloadRate, uploadRate);
				updateMessage(message);
				
				TimeUnit.SECONDS.sleep(UPDATE_SLEEP_INTERVAL_SECONDS);
			}
			
			return null;
		}
		
		private void updateTab() {
			// The task runs as a daemon, so it is basically a background thread.
			// To update the UI, the following methods need to be sent to the FX
			// thread. This is accomplished using the Platform.runLater() method.
			if(this.equals(_torrentTable.getSelectionModel().getSelectedItem())) {
				if(isInfoTabSelected()) {					
					Platform.runLater(new Runnable() {
						
						@Override
						public void run() {
							setDownloadedLabel(_torrentSession);
							setRaminingLabel(_torrentSession);
						}
					});				
				} else {
					Platform.runLater(new Runnable() {
						
						@Override
						public void run() {
							updatePeersTab(TorrentTask.this);
						}
					});		
				}
			}
		}
		
		public TorrentSession getTorrentSession() {
			return _torrentSession;
		}
		
		public void startTorrentSession() throws Exception{
			runInBackground(Action.START, _torrentClient, _torrentSession);
		}
    	
		public void stopTorrentSession() {
			runInBackground(Action.STOP, _torrentClient, _torrentSession);
		}	
		
		public boolean isRemoved() {
			return _remove;
		}
		
		public void remove() {
			_remove = true;
		}
    }
    
    private void runInBackground(Action action, final TorrentClient torrentClient,
    		final TorrentSession torrentSession) {
    	Runnable rn = new Runnable() {
			
			@Override
			public void run() {
				try {
					switch(action) {
					case START: 
						torrentClient.resumeTorrentSession(torrentSession);
						break;
					case STOP:
						torrentClient.stopTorrentSession(torrentSession);
						break;
					case REMOVE:
						torrentClient.removeTorrentSession(torrentSession);
						break;
					}
				} catch (Exception e){
					Platform.runLater(new Runnable() {
						
						@Override
						public void run() {
							showDialog(AlertType.ERROR, "Error executing operation", "", e.getMessage());
						}
					});
				}
				
			}	
		};
		_executor.execute(rn);
    }
    
    /////////////////////////////// Tabs ///////////////////////////////
    private void initTabPane() {
    	// Update immediately the tab pane when the user selects a new one.
    	_tabPane.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
    		TorrentTask task = _torrentTable.getSelectionModel().getSelectedItem();
    		if(task == null) {
    			return;
    		}
    		
            if(newTab.getText().equals("Info")) {
            	updateInfoTab(task);
            } else if (newTab.getText().equals("Peers")) {
            	updatePeersTab(task);
            }
        });
    }
    public void updateTabPane(TorrentTask task) {
    	if(isInfoTabSelected()) {
    		updateInfoTab(task);
    	} else {
    		updatePeersTab(task);
    	}
    }
    
    private boolean isInfoTabSelected() {
    	String selectedTab = _tabPane.getSelectionModel().getSelectedItem().getText();
    	return selectedTab.equals("Info");
    }
    
    ///////////////////////////// Info tab /////////////////////////////
    public void updateInfoTab(TorrentTask task) {    	
    	if (task != null) {
    		TorrentSession session = task.getTorrentSession();
    		setTotalSizeLabel(session);
    		setCreatedOnLabel(session);
    		setCreatedByLabel(session);
    		setHashLabel(session);
    		setCommentLabel(session);
    		setPiecesLabel(session);
    		setDownloadedLabel(session);
    		setRaminingLabel(session);
    		setUploadedLabel(session);
    		populateFilesListView(session);
    	} else {
    		_totalSizeLabel.setText("");
    		_createdOnLabel.setText("");
    		_createdByLabel.setText("");
    		_hashLabel.setText("");
    		_commentLabel.setText("");
    		_piecesLabel.setText("");
    		_downloadedLabel.setText("");
    		_remainingLabel.setText("");
    		_uploadedLabel.setText("");
    	}
    }
    
    private void setTotalSizeLabel(TorrentSession session) {
    	long length = session.getMetaInfo().getInfoDictionary().getLength();
		String humanReadble = Utils.formatSize(length, true);
    	_totalSizeLabel.setText(humanReadble);
    }
    
    private void setCreatedOnLabel(TorrentSession session) {
    	Date createdOn = session.getMetaInfo().getCreationDate();
    	String date = "";
    	if (createdOn != null) {
    		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-mm-yyyyy hh:mm:ss"); 
    		date = dateFormat.format(createdOn);
    	}
    	
    	_createdOnLabel.setText(date);
    }
    
    private void setCreatedByLabel(TorrentSession session) {
    	String createdBy = session.getMetaInfo().getCreatedBy();
    	if(createdBy == null) {
    		createdBy = "";
    	}
    	_createdByLabel.setText(createdBy);
    }
    
    private void setHashLabel(TorrentSession session) {
    	_hashLabel.setText(session.getMetaInfo().getHexInfoHash());
    }
    
    private void setCommentLabel(TorrentSession session) {
    	String comment = session.getMetaInfo().getComment();
    	if(comment == null) {
    		comment = "";
    	}
    	_commentLabel.setText(comment);
    }
    
    private void setPiecesLabel(TorrentSession session) {
    	_piecesLabel.setText(Integer.toString(session.getPieceRepository().size()));
    }
    
    private void setDownloadedLabel(TorrentSession session) {
    	_downloadedLabel.setText(Utils.formatSize(session.getSessionInfo().getDownloaded(), true));
    }
    
    private void setRaminingLabel(TorrentSession session) {
    	_remainingLabel.setText(Utils.formatSize(session.getSessionInfo().getLeft(), true));
    }
    
    private void setUploadedLabel(TorrentSession session) {
    	_uploadedLabel.setText(Utils.formatSize(session.getSessionInfo().getUploaded(), true));
    }
    
    private void populateFilesListView(TorrentSession session) {
    	_filesListView.getItems().clear();
    	_filesListView.getItems().addAll(session.getFileNames());
    }
    
    ///////////////////////////// Peers tab /////////////////////////////
    
    private void initializePeersTable() {
    	_ipColumn.setCellValueFactory(cellData -> cellData.getValue().getIp());
    	_peerIdColumn.setCellValueFactory(cellData -> cellData.getValue().getPeerId());
    	_downloadSpeedColumn.setCellValueFactory(cellData -> cellData.getValue().getDownloadSpeed());
    	_uploadSpeedColumn.setCellValueFactory(cellData -> cellData.getValue().getUploadSpeed());
    }
    
    public void updatePeersTab(TorrentTask task) {
    	if (task != null) {
    		List<PeerDisplay> peersToDisplay = new LinkedList<>();
    		PeerManager peerManager = task.getTorrentSession().getPeerManager();
    		Set<Peer> connectedPeers = peerManager.getConnectedPeers();
    		for(Peer peer : connectedPeers) {
    			peersToDisplay.add(new PeerDisplay(peer));
    		}
    		
    		_peersTable.setItems(FXCollections.observableList(peersToDisplay));
    	}
    }
}
