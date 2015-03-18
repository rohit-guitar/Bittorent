import java.io.DataInputStream;
import java.io.IOException;
import java.util.TimerTask;


public class CommonResource {
	
	public static final String handshakeHead = "P2PFILESHARINGPROJ";
	
	public static final boolean checkDataOut(final DataInputStream stream) throws IOException{
		return ((stream.available()<0) || (stream==null));
	}
	
	public static class keepAliveTask extends TimerTask{
		private Peer peer;
		
		public keepAliveTask(Peer p){
			this.peer=p;
		}
		@Override
		public void run() {
			try {
				peer.sendKeepMess();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	} 
}
