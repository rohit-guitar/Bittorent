

public class peerProcess {
    
	private static Torrent mainTorrent;  
	public static final ReadConfig config =  new ReadConfig();
	public static final FileSplitter fs = new FileSplitter();
	
	public peerProcess(String peerId) {
			mainTorrent = new Torrent(peerId);
	}
	public static void main(String[] args) {
		
        peerProcess main = new peerProcess(args[0]);
        
        try {
			Thread.currentThread().sleep(1500);
		} catch(InterruptedException ie) {
		}		
        
        ShutdownHook sHook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(sHook);
		mainTorrent.initialPeer();
		
		new Thread( new Runnable() {
		    public void run() {
		        mainTorrent.file_download();
            }
        }).start();
		
		mainTorrent.start();	
    }
    
    private static class ShutdownHook extends Thread {
		public void run() {
			mainTorrent.shutdownTorrent();		
		}		
	}

   
}
