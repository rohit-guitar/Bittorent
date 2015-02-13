import java.io.DataInputStream;
import java.io.IOException;


public class CommonResource {
	
	public static final String handshakeHead = "P2PFILESHARINGPROJ";
	
	public static final boolean checkDataOut(final DataInputStream stream) throws IOException{
		return ((stream.available()<0) || (stream==null));
	}
}
