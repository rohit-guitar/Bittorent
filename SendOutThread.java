import java.io.DataOutputStream;
import java.util.LinkedList;
import java.util.Timer;


public class SendOutThread extends Thread{
	private LinkedList<AbstractMessage> outQueue;
	private final DataOutputStream out;
	private Timer time;
	private final Peer peer;
	private boolean is_running;
	private CommonResource.keepAliveTask kAliveTask;
	
	public SendOutThread(DataOutputStream dOut,Peer p){
		this.out=dOut;
		this.peer=p;
		this.time = new Timer();
		this.outQueue = new LinkedList<AbstractMessage>();
		this.is_running = true;
	}
	
	public LinkedList<AbstractMessage> getOutQueue(){
		return this.outQueue;
	}
	@Override
	public void run(){
		while(is_running){
			kAliveTask= new CommonResource.keepAliveTask(peer); 
			time.schedule(kAliveTask, 100000);
			synchronized (outQueue){
				while(is_running && outQueue.isEmpty()){
					try {
						outQueue.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					try{
						// we need to cancel the timer task
						kAliveTask.cancel();
						final AbstractMessage sendMess = outQueue.removeFirst();
						AbstractMessage.encodeMessage(sendMess, out);
					}
					catch(Exception e){}
				}
			}
		}
	}
	
	public void disconnect(){
		this.kAliveTask.cancel();
		this.time.cancel();
		is_running = false;
		synchronized (outQueue) {
			outQueue.notifyAll();
		}
	}
	
}
