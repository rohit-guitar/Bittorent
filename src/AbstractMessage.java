import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;




public abstract class AbstractMessage {
	/*This class is to define the common structure to the messages*/
	public static final byte chokeid = 0;
    public static final byte unchokeid = 1;
    public static final byte interestedid = 2;
    public static final byte notinterestedid = 3;
    public static final byte haveid = 4;
    public static final byte bitfieldid = 5;
    public static final byte requestid = 6;
    public static final byte pieceid = 7;
    public static final byte keepaliveid=126;
    public static final Choke choke = new Choke();
    public static final UnChoke unchoke = new UnChoke();
    public static final Interested interested = new Interested();
    public static final NotInterested notinterested = new NotInterested();
    public static final KeepAlive keepalive = new KeepAlive();
   
    private final byte ID;
    private final int len;
    /**/
    public AbstractMessage(final int length,final byte id){
    	this.ID=id;
    	this.len=length;
    }
    
    public byte getID(){return this.ID;}
    public int getLen(){return this.len;}
    
    
    public static class Choke extends AbstractMessage{
    	private Choke(){
    		super(1,chokeid);
    	}
    }
    
    public static class UnChoke extends AbstractMessage{

		public UnChoke() {
			super(1, unchokeid);
		}
    	
    }
    
    public static class Interested extends AbstractMessage{

		public Interested() {
			super(1,interestedid);
		}
    	
    }
    
    public static class NotInterested extends AbstractMessage{

		public NotInterested() {
			super(1, notinterestedid);
		}
    	 	
    }
    
    public static class KeepAlive extends AbstractMessage{

		public KeepAlive() {
			super(0, keepaliveid);
		}
    	
    }
    
    public static class Have extends AbstractMessage{
    	private int index;
    	public int getIndex(){return this.index;}
		public Have(int a) {
			super(5, haveid);
			this.index=a;
		}
    	
    }
    
    public static class BitField extends AbstractMessage{
    	private byte[] bitField;
    	public byte[] getBitField(){return bitField;}

		public BitField(byte[] bField) {
			super(1+bField.length, bitfieldid);
			this.bitField=bField;
			
		}
    	
    }
    
    public static class Piece extends AbstractMessage{
    	private int pieceIndex; 
        private byte[] piece; // piece 
       
        public int getPieceIndex(){return pieceIndex;}
        public byte[] getPiece(){return piece;}
        

		public Piece(int a,byte[] data) {
			super(5+data.length, pieceid);
			this.pieceIndex=a;
			this.piece=data;
		}
    	
    }
    
    public static class Request extends AbstractMessage{
    	private int reqIndex;
    	
    	public int getreqIndex(){return this.reqIndex;}
    	
    	public Request(int a){
    		super(5,requestid);
    		this.reqIndex=a;
    	}
    	
    }
    
    
    //Read the message form the feed peer and Decode a message 
    
    public static final AbstractMessage decodeMessage(final DataInputStream feedPeer) throws SocketException{
    	AbstractMessage decodeMess = null;
    	
    	try {
			if(CommonResource.checkDataOut(feedPeer)){
				//log file input stream is null or not available
			}
			else{
				int messLen= feedPeer.readInt();
				if(messLen==0){return AbstractMessage.keepalive;}
				byte readId = feedPeer.readByte();
				switch(readId){
					case AbstractMessage.haveid:
						decodeMess = new Have(feedPeer.readInt());
						break;
					case AbstractMessage.requestid:
						decodeMess= new Request(feedPeer.readInt());
						break;
					case AbstractMessage.bitfieldid:
						byte[] bitField = new byte[messLen-1]; // 1 for type
						feedPeer.readFully(bitField);
						decodeMess = new BitField(bitField);
						break;
					case AbstractMessage.pieceid:
						int pIndex = feedPeer.readInt();
						byte[] data = new byte[messLen-5]; //2 int and 1 for type 
						feedPeer.readFully(data);
						decodeMess =  new Piece(pIndex, data);
						break;
					case AbstractMessage.chokeid:
						decodeMess = AbstractMessage.choke;
						break;
					case AbstractMessage.unchokeid:
						decodeMess = AbstractMessage.unchoke;
						break;
					case AbstractMessage.interestedid:
						decodeMess = AbstractMessage.interested;
						break;
					case AbstractMessage.notinterestedid:
						decodeMess= AbstractMessage.notinterested;
					default:
						break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return decodeMess;
    }
    
    
    
    //Encode a message and the target peer Output Stream Object
    public static final void encodeMessage(final AbstractMessage message,final DataOutputStream targetPeer) {
    	try {
			targetPeer.writeInt(message.getLen());
			if(message.getID() != keepaliveid){ //interested, not interested , choke , Unchoke covered
				targetPeer.writeByte(message.getID());
			}
			if(message.getLen()>1){ //have , piece, bitfield , request
				switch (message.getID()){
					case AbstractMessage.bitfieldid:
						BitField temp=(BitField)message;
						targetPeer.write(temp.getBitField());
						break;
					case AbstractMessage.haveid:
						Have have = (Have)message;
						targetPeer.writeInt(have.getIndex());
						break;
					case AbstractMessage.requestid:
						Request req = (Request)message;
						targetPeer.writeInt(req.getreqIndex());
						break;
					case AbstractMessage.pieceid:
						Piece p= (Piece) message;
						targetPeer.writeInt(p.getPieceIndex());
						targetPeer.write(p.getPiece());
						break;
					default:
						break;
						
				}
				targetPeer.flush();
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    	
    	
    }
    
    public static final void sendHandShake(String peerID){
    	try {
			byte[] protocolName = CommonResource.handshakeHead.getBytes("ASCII");
			
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    }
    
    
    
    
}
