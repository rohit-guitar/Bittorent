import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.border.AbstractBorder;



public class Peer extends Thread{
	public boolean reqCheck;
	private String ID;
	private String ipAdd;
	private int port;
	private Socket skt;
	private boolean chock,aChock;
	private boolean interest,aInterest;
	private Torrent torrent;
	private boolean is_run , is_connect;
    public Queue <AbstractMessage.Request> pendingReqs = new LinkedList<AbstractMessage.Request>();
    private DataOutputStream send_out;
    private boolean[] pStatus; // keep track of the pieces 
    private SendOutThread outThread;
    private int pieceCount;
    
    //Own peer
    public Peer(String ID, String ipadd, int port, int no_pieces, Torrent torrent){
    	this.ID=ID;
    	this.ipAdd=ipadd;
    	this.port=port;
    	this.torrent=torrent;
    	this.pStatus = new boolean[no_pieces];
    	chock = aChock = true;
    	interest = aInterest = is_connect = false;
    	this.pieceCount=0;
    }
    
    //for the other connecting peers
    public Peer(Socket sk , Torrent torrent){
    	this.skt= sk;
    	this.torrent = torrent;
    	this.pStatus = new boolean [torrent.getNoPieces()];
    	is_connect = true;
    	ID = null;
    	chock = aChock = true;
    	interest = aInterest = false;
    	this.pieceCount=0;
    	try{
    		DataOutputStream dOut= new DataOutputStream(skt.getOutputStream());
    		this.outThread = new SendOutThread(dOut, this);
    		this.outThread.start();
    	}
    	catch (IOException e){
    		System.exit(1);
    	}
    }
    
    public String getPeerId(){
    	return this.ID;
    }
    
    public void connect_peer() {
        try {
            skt = new Socket(this.ipAdd, this.port);
            this.send_out = new DataOutputStream(this.skt.getOutputStream());
            outThread = new SendOutThread(send_out, this);
            outThread.start();
            this.is_run = true;
            is_connect = true;
        } catch (IOException e) {
            torrent.removePeer(this);
           }
    }
    
    public void disconnect_Peer() {
        if (is_connect) {
            this.is_run = false;
            this.is_connect = false;
            //this.pouthread.shutdown();
            if(torrent.is_Running)
				torrent.removePeer(this);
            try{
                this.skt.close();
            } catch (IOException e) {
            }
        }
    }
    
    
    @Override
    	public void run(){
    		boolean bitfieldCheck = true; // we need to check if bitfield is first message
    		try{
    			this.skt.setSoTimeout(120000);  // to block to input stream
    		}
    		catch(SocketException e){
    			
    		}
    		while (is_run){
    			try {
					final AbstractMessage recvMessage = AbstractMessage.decodeMessage(new DataInputStream(this.skt.getInputStream()));
					if(recvMessage == null){
						disconnect_Peer();
					}
					switch(recvMessage.getID()){
						case AbstractMessage.chokeid:
							this.chock=true;
							break;
						case AbstractMessage.unchokeid:
							this.chock=false;
							if(reqCheck){
								reqNext();
							}
							break;
						case AbstractMessage.interestedid:
							setInterested(true);
							setMeChok(false);
							sendUnchokeMessage();
							break;
						case AbstractMessage.notinterestedid:
							setInterested(false);
							break;
						case AbstractMessage.haveid:
							this.pStatus[((AbstractMessage.Have)recvMessage).getIndex()]=true;
							pieceCount++;
							break;
						case AbstractMessage.bitfieldid:
							if(bitfieldCheck){setCompletedFromBitfield(((AbstractMessage.BitField)recvMessage).getBitField());}
							else{disconnect_Peer();}
							break;
						case AbstractMessage.requestid:
							AbstractMessage.Request req = (AbstractMessage.Request)recvMessage;
							if(!aChock){
								//torrent to get request received 
							}
							break;
						case AbstractMessage.pieceid:
							AbstractMessage.Piece piece = (AbstractMessage.Piece)recvMessage;
							//call the function in torrent to get piece
							break;
						case AbstractMessage.keepaliveid:break;
						default:
							
					}
					bitfieldCheck=false;
					
					
				} catch (SocketTimeoutException e) {
					e.printStackTrace();
					disconnect_Peer();
				} catch (IOException e) {
					e.printStackTrace();
					disconnect_Peer();
				}
    			
    			if ((torrent.getDoneCount() == torrent.getNoPieces()) && (pieceCount == torrent.getNoPieces())) {
                    disconnect_Peer();
                }

    		}
    	}
    
    private ByteBuffer buildHandshakeMessage(String localPeerID) {
        byte[] handshake = new byte[32];
        try {
            byte[] head = CommonResource.handshakeHead.getBytes("ASCII");
            System.arraycopy(head, 0, handshake, 0, head.length);
            Arrays.fill(handshake, 18, 28, (byte)0);
            System.arraycopy(localPeerID.getBytes("ASCII"), 0, handshake, 28, localPeerID.length());  
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return ByteBuffer.wrap(handshake);
    }
    
    private void sendHandshake(String localPeerID) {
        try {
            ByteBuffer handshake = buildHandshakeMessage(localPeerID);
            if(!is_connect) {
                this.connect_peer();
            }
            DataOutputStream sendOut = new DataOutputStream(skt.getOutputStream());
            sendOut.write(handshake.array());
            sendOut.flush();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    private boolean verifyHandshake() {
        try {
            DataInputStream serverIn = new DataInputStream(skt.getInputStream());
            byte[] response = new byte[32];
            skt.setSoTimeout(500);
            skt.setSoTimeout(0);
            if (Arrays.equals(CommonResource.handshakeHead.getBytes(Charset.forName("UTF-8")), Arrays.copyOfRange(response, 0, 17)))
                if (Arrays.equals(ID.getBytes("ASCII"), Arrays.copyOfRange(response, 28, 31)))
                    return true;
        } catch (SocketTimeoutException e) {
            disconnect_Peer();
            return false;
        } catch (IOException e) {
            disconnect_Peer();
            e.printStackTrace();
        }
        return false;
    }
    
    public boolean startHandshake(String localPeerID) {
        sendHandshake(localPeerID);
        return verifyHandshake();
    }
    
    private boolean initPeerFromHandshake() throws IOException {
        DataInputStream serverIn = new DataInputStream(skt.getInputStream());
            byte head[] = new byte[18];
            serverIn.readFully(head);
            if (Arrays.equals(CommonResource.handshakeHead.getBytes(Charset.forName("UTF-8")), head)) {
                byte zeroBytes[] = new byte[10];
                serverIn.readFully(zeroBytes);
                byte peerid[] = new byte[4];
                serverIn.readFully(peerid);
                ID = new String(peerid, "ASCII");
                return true; 
            }
        
        return false;
    }
    
    public boolean answer_Handshake(String localPeerID) {
        try {
            if(initPeerFromHandshake()) {
                sendHandshake(localPeerID);
                return true;
            }
            } catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }

 
	private LinkedList<AbstractMessage> getOutQueue(){
    	return this.outThread.getOutQueue();
    }
    
    public String getipAddress() {
        return this.ipAdd;
    }

    public int getPort() {
        return this.port;
    }


    public Socket getSocket() {
        return skt;
    }

    public synchronized boolean Choking() { // Current peer chocking
        return chock;
    }

    public synchronized boolean aChoking() { // I am chocking
        return aChock;
    }

    public synchronized boolean Interested() {
        return interest;
    }
    
    public synchronized boolean aInterested() {
        return aInterest;
    }

    
    public synchronized void setInterested(boolean interest) {
        this.interest = interest;
    }

    public synchronized void setMeChok(boolean aChock) {
        this.aChock = aChock;
    }
    public synchronized void setMeInterested(boolean aInterested) {
        this.aInterest = aInterested;
    }
    
    public boolean is_Connected() {
        return is_connect;
    }
    
    public boolean is_Running() {
        return this.is_run;
    }
    
    public boolean[] getPieceStatus() {
        return pStatus;
    }
    
  
    private boolean isSet(byte[] bitfield, int bit) {
        int index = bit / 8;
        byte b = bitfield[index];
        return (b >> (7 - bit) & 1) == 1;
    }
    
    public void setCompletedFromBitfield(byte[] bitfield) {
        for (int x = 0; x < pStatus.length; x++) {
            if (isSet(bitfield, x)) {
                pStatus[x] = true;
                pieceCount++;
            }
        }
    }
    
    public void setCompletedByIndex(int pieceIndex) {
        pStatus[pieceIndex] = true;
    }
    
 
    
    
	
	public synchronized void sendChokeMessage() throws IOException{
		AbstractMessage.encodeMessage(AbstractMessage.choke, send_out);
	}
	
	public synchronized void sendUnchokeMessage(){
		LinkedList<AbstractMessage> outQueue = getOutQueue();
		synchronized (outQueue) {
			outQueue.addLast(AbstractMessage.unchoke);
			outQueue.notifyAll();
		}
	}
	
	public void sendInterested(){
		LinkedList<AbstractMessage> outQueue = getOutQueue();
		synchronized (outQueue) {
			outQueue.addLast(AbstractMessage.interested);
			outQueue.notifyAll();
		}
	}
	
	public synchronized void sendUninterestedMessage(){
		AbstractMessage.encodeMessage(AbstractMessage.notinterested, send_out);
	}
	
	public synchronized void sendBitField(byte[] bitfield) throws IOException{
		AbstractMessage.encodeMessage(new AbstractMessage.BitField(bitfield), send_out);
	}
	
	public synchronized void sendRequestMessage(int index) throws IOException{
		synchronized (pendingReqs) {
			pendingReqs.offer(new AbstractMessage.Request(index));
		}	
	}
	
	public synchronized void reqNext(){
		synchronized (pendingReqs) {
			if(!pendingReqs.isEmpty()){
				LinkedList<AbstractMessage> outQueue = getOutQueue();
				synchronized (outQueue) {
					outQueue.addFirst(pendingReqs.peek());
					outQueue.notifyAll();
				}
			}
		}
	}
	
	public synchronized void sendKeepMess() throws IOException{
		LinkedList<AbstractMessage> outQueue = getOutQueue();
		synchronized (outQueue) {
			outQueue.addLast(AbstractMessage.keepalive);
			outQueue.notifyAll();
		}
	}
	
	public void sendRequestToOut(){
		synchronized (pendingReqs) {
			LinkedList<AbstractMessage> outQueue = getOutQueue();
			synchronized (outQueue) {
				outQueue.addFirst(pendingReqs.peek());
				outQueue.notifyAll();
			}
		}
	} 
	
	public synchronized void sendHaveMessage(int index){
		LinkedList<AbstractMessage> outQueue = getOutQueue();
		synchronized (outQueue) {
			outQueue.addLast(new AbstractMessage.Have(index));
			outQueue.notifyAll();
		}
	}
	
	public synchronized void sendPiece(int index){
		LinkedList<AbstractMessage> outQueue = getOutQueue();
		synchronized (outQueue) {
			outQueue.addLast(new AbstractMessage.Have(index));
			outQueue.notifyAll();
		}
	}	
	
	
}
