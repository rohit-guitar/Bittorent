
import org.apache.log4j.Logger;


public class log4jDemo {
	static Logger log = Logger.getLogger(log4jDemo.class.getName());
	
	public static void main(String[] args){
		log.debug("This is debug test");
		log.error("Checking error :- Error");
		log.fatal("Checking error Fatal");
		log.warn("Checking error Warn");
		log.trace("Checking error :- Trace");
		log.info("Checking error :- Info");
	}
	
}
