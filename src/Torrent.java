import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.Collections;
import java.util.concurrent.*;

public class Torrent extends Thread {
	private final ExecutorService task = Executors.newCachedThreadPool();
	private List<Peer> peers;
	private int pieceNUM;
	private int LISTENING_PORT=6008;
	private String local_id;
	private ServerSocket listener;
	private HashMap<Integer, ArrayList<Peer>> peerTracker;
	public  boolean isRunning;
	private boolean[] piecesAsked;
	private int doneNUM;
	private boolean[] pieceHas;
	public boolean is_Running;
	private boolean shut_down;

	public Torrent(String peerID) {
		LISTENING_PORT = 6008;                                                                             
		local_id = peerID;                 
		this.shut_down = false;
		long total = Long.parseLong(peerProcess.config.getProperty("FileSize"));
		long piece = Long.parseLong(peerProcess.config.getProperty("PieceSize"));
		long out = total/piece;
		if(total%piece ==0){this.pieceNUM=(int)out;}
		else{this.pieceNUM=(int)out+1;}
		
		piecesAsked = new boolean[pieceNUM];
		for (int i = 0; i < pieceNUM - 1; i++) {               
			piecesAsked[i] = false;
		}
	}

	public void initialPeer() {
        peers = Collections.synchronizedList(new ArrayList<Peer>());                                                                       //List of Peer objects (instance variable)
        readFile("peerInfo.cfg");
			for(int i=0;i<peers.size();i++){
			Peer newpeer = peers.get(i);    		
			addNewPeer(newpeer);
			if(newpeer.startHandshake(local_id)) {
				newpeer.start();				
				send_Havemsg(newpeer);
			} else {
				peers.remove(newpeer);
			}
		}
	}		

		// server socket listening and creat new peer which is acorresponding to  a remote machine
		public void run() {


			try {
				listener = new ServerSocket(LISTENING_PORT);
				System.out.println("Listening on port " + LISTENING_PORT);
				while (isRunning) {

					final Socket incomingConn = listener.accept(); 
					task.execute(new Runnable(){
						public void run(){
							add_Peer(incomingConn);
						}
					});

				}
			}catch (IOException e) {
				e.printStackTrace();
			}  	

		}
		public void start_Peer_Threads(){

		}

		/* add new Peer, send handshake msg adn start peer thread*/
		public void addNewPeer(Peer newPeer) {
			if (checkDuplicatePeer(newPeer)) {
				return;
			} 
			if(!newPeer.startHandshake(local_id)) {
				newPeer.disconnect_Peer();
				return;
			}
			peers.add(newPeer);
			newPeer.start();
			send_Havemsg(newPeer);
		}   

		/* add new Peer, send handshake msg adn start peer thread, take socket as parameter*/
		public void add_Peer(final Socket incomingsock){
			Peer newPeer =new Peer(incomingsock, this);
			if (checkDuplicatePeer(newPeer)) {
				newPeer.disconnect_Peer();
				return;
			}    	
			if(newPeer.answer_Handshake(local_id)){
				peers.add(newPeer);
				newPeer.start();
				send_Havemsg(newPeer);
			}else {
				newPeer.disconnect_Peer();
				return;
			}
		}

		private boolean checkDuplicatePeer(Peer peer) {
			synchronized (peers) {
				for (Peer existPeer : peers) {
					if (existPeer.getPeerId().equals(peer.getPeerId())) {
						return true;
					}
				}
			}
			return false;
		}


		public void send_Havemsg(Peer peer) {
			for (int i = 0; i < pieceNUM; i++) {
				if (pieceHas[i])
					peer.sendHaveMessage(i);
			}
		}


		public void file_download() {
			if(getDoneNUM()==pieceNUM) {
				System.out.println("Skipping download(), file already done");
				
				return;    		
			}
			/* for each piece, generate a list which contain the peer of having the corresponding piece*/
			peerTracker = new HashMap<Integer, ArrayList<Peer>>();
			for (int i = 0; i < pieceNUM; i++) {
				peerTracker.put(i, new ArrayList<Peer>());
				synchronized(peers){
					for(Peer peer : peers){
						boolean[] pieceStatus=peer.getPieceStatus();
						for(i=0;i<pieceStatus.length;i++){
							if (pieceStatus[i]== true)
								peerTracker.get(i).add(peer);
						}
					}
				}
			}


			/* send interested msg to every peer which has the lacking piece*/
			for (int i = 0; i < pieceNUM; i++) {
				if (pieceHas[i]==false) {
					for (Peer peer : peerTracker.get(i)) {
						peer.setMeInterested(true);
						peer.sendInterested();
					}
				}
			}		

			// send request msg
			piecesAsked= new boolean[pieceNUM];
			while(is_Running) {
				if (getDoneNUM() == pieceNUM) {
					break;
				}
				for (int i = 0; i < pieceNUM; i++) {
					if (piecesAsked[i] || pieceHas[i]) // do not request when already requested or already downloaded 
						continue;
					ArrayList<Peer> availablePeers = peerTracker.get(i);
					if (!availablePeers.isEmpty()) {
						int available = -1;
						for (int j = 0; j < availablePeers.size(); j++) {
							// peer which is Connected unchoking and not requested yet is available
							Peer temp= availablePeers.get(j);
							if (temp.is_Connected() && !temp.reqCheck && !temp.Choking()) {
								available = j;
								break;
							}
						}
						if (available == -1) {
							continue;
						} else {
							piecesAsked[i] = true;
							request_Piece(availablePeers.remove(available), i);
						}
					}
				}
				try {
					Thread.sleep(5);
				} catch(Exception e) {
					e.printStackTrace();
				}
			} 

		}


		// send request message to the denoting peers
		public void request_Piece(Peer peer, int index) {
			try{          
				peer.reqCheck = true;

				peer.sendRequestMessage(index); 
				peer.sendRequestToOut();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		
		 public void request_Receiv_ed(AbstractMessage.Request request, Peer peer) throws IOException {
		        int index = request.getreqIndex();
		        if (index >= 0 && index < pieceNUM) {
		        	Path path = Paths.get("path/to/file");
		        	byte[] data = Files.readAllBytes(path);
		                AbstractMessage.Piece pm = new AbstractMessage.Piece(index, data);
		                peer.sendPiece(pm);
		            }
		        else {
		            
		            peer.disconnect_Peer();
		        }
		    }

		public void pieceReceived(AbstractMessage.Piece rvfPiece, Peer peer) {
			synchronized(peer.pendingReqs) {
				AbstractMessage.Request expectReq = peer.pendingReqs.peek();
				int index = rvfPiece.getPieceIndex();
				if (index == expectReq.getreqIndex()) {
					if (!peer.pendingReqs.isEmpty())
						peer.pendingReqs.poll();
				}
			}
			peer.reqNext();
			pieceHas[rvfPiece.getPieceIndex()]=true;
			writePiece(rvfPiece.getPiece(), rvfPiece.getPieceIndex());
			incrementDone();
			broadcast(rvfPiece.getPieceIndex());
		}
		//Broadcasting have message for piece just received
		public void broadcast(int index) {
			synchronized(peers) {
				for (Peer peer : peers) {
					if (peer.is_Connected())
						peer.sendHaveMessage(index);
				}
			}
		}

		public void removePeer(Peer a){
			this.peers.remove(a);
		}

		public synchronized int getDoneNUM() {
			return doneNUM;
		}

		private synchronized void incrementDone() {
			doneNUM++;
		}

		public int getPort() {
			return LISTENING_PORT;
		}
		public int getpieceNUM() {
			return pieceNUM;
		}    

		public void writePiece(byte[] piece, int index) {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("Piece");
			stringBuilder.append(index);
			stringBuilder.append(".dat");

			String filename=stringBuilder.toString();
			try {
				PrintWriter f = new PrintWriter(new FileWriter(filename));
				f.println(piece);
				f.flush();
				f.close();
			} catch (IOException ex) {

			}
		}

		public void readFile(String s1){
			try{
				FileReader fr = new FileReader(s1);
				BufferedReader br = new BufferedReader(fr);
				String s;
				String[] str=null;
				while((s = br.readLine()) != null) { //reading file till the current peer is founds
					str=s.split("\\s+"); 
					if(str[0].equals(local_id)){
						this.pieceHas= new boolean[this.pieceNUM];
						for(int i=0;i<this.pieceHas.length;i++){
							if(Integer.parseInt(str[3])==1){
								this.pieceHas[i]=true;
							}
							else{
								this.pieceHas[i]=false;
							}
						}
						break;
						
					}
					else peers.add(new Peer(str[0],str[1],Integer.parseInt(str[2]),Integer.parseInt(str[3]),this.pieceNUM,this)); 
				}
				fr.close();
			}
			catch (Exception e){
				System.err.println("Error: " + e.getMessage());
			}

		}


		public void shutdownTorrent() {

			if(!shut_down) {
				shut_down = true;
				this.is_Running = false;
				synchronized(peers) {
					for (Peer peer : peers) {
						peer.disconnect_Peer();
					}
				}
			}
		}

	}