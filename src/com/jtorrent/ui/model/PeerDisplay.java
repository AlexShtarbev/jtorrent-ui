package com.jtorrent.ui.model;

import com.jtorrent.peer.Peer;
import com.jtorrent.ui.utils.Utils;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PeerDisplay {

	private final StringProperty _ip;
	private final StringProperty _peerId;
	private final StringProperty _downloadSpeed;
	private final StringProperty _uploadSpeed;
	
	public PeerDisplay(Peer peer) {
		_ip = new SimpleStringProperty(peer.getIP());
		_peerId = new SimpleStringProperty(peer.getHexPeerID());
		
		_downloadSpeed =  new SimpleStringProperty(Utils.formatSize((long) peer.getDownloadRate().
				rate(), true) + "/s");
		
		_uploadSpeed =  new SimpleStringProperty(Utils.formatSize((long) peer.getUploadRate().
				rate(), true) + "/s");
	}
	
	public StringProperty getIp() {
		return _ip;
	}
	
	public StringProperty getPeerId() {
		return _peerId;
	}
	
	public StringProperty getDownloadSpeed() {
		return _downloadSpeed;
	}
	
	public StringProperty getUploadSpeed() {
		return _uploadSpeed;
	}
}
