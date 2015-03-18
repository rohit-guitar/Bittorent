
import java.util.Properties;


public class ReadConfig
{
	public static Properties configFile;
	public ReadConfig()
	{
		configFile = new java.util.Properties();
		try {
			configFile.load(this.getClass().getClassLoader().
			getResourceAsStream("Common.cfg"));
			}
		catch(Exception eta){
			eta.printStackTrace();
		}

   }

	public String getProperty(String key)
	{
		String value = this.configFile.getProperty(key);
		return value;
	}
	
}


