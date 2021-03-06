import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


public class FileSplitter
	{
	//public static final long floppySize = (long)(1.4 * 1024 * 1024);
	public static long chunkSize = Long.parseLong(peerProcess.config.getProperty("PieceSize"));
	
	public void mainHandler(String s,int action) throws Exception
		{
			
		try
			{
			if (action==1)
				split(s);
			else if (action==2)
				join(s);
			else
				{
				System.out.println("The first argument must be an option:");
				System.out.println("\t-split: split the specified file");
				System.out.println("\t-join: join all splitter outfiles with the specified base filename");
				System.exit(0);
				}
			}
		catch (FileNotFoundException e)
			{
			System.out.println("File not found: " + s);
			}
		catch (IOException e)
			{
			System.out.println("IO Error");
			}
		}
		
	public static void split(String filename) throws FileNotFoundException, IOException
		{
		BufferedInputStream in = new BufferedInputStream(new FileInputStream("peer_1001/"+filename));
		File f = new File("peer_1001/TheFile.dat");
		long fileSize = f.length();
		System.out.println("FileSize+  ="+fileSize);
		int subfile;
		for (subfile = 0; subfile < fileSize / chunkSize; subfile++)
			{
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("peer_1001/"+filename + "." + subfile));
			
			for (int currentByte = 0; currentByte < chunkSize; currentByte++)
				{
					out.write(in.read());
				}
			out.close();
			}
		
		if (fileSize != chunkSize * (subfile - 1))
			{
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("peer_1001/"+filename + "." + subfile));
			
			int b;
			while ((b = in.read()) != -1)
				out.write(b);
				out.close();			
			}
		
		in.close();
		}
		
	
	public static void join(String baseFilename) throws IOException
		{
		int numberParts = getNumberParts(baseFilename);

		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(baseFilename));
		for (int part = 0; part < numberParts; part++)
			{
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(baseFilename + "." + part));

			int b;
			while ( (b = in.read()) != -1 )
				out.write(b);

			in.close();
			}
		out.close();
		}
	
		public static int getNumberParts(String baseFilename) throws IOException
		{

		File directory = new File(baseFilename).getAbsoluteFile().getParentFile();
		final String justFilename = new File(baseFilename).getName();
		String[] matchingFiles = directory.list(new FilenameFilter()
			{
			public boolean accept(File dir, String name)
				{
				return name.startsWith(justFilename) && name.substring(justFilename.length()).matches("^\\.\\d+$");
				}
			});
		return matchingFiles.length;
		}
		
		public static void mkdir(String t){
			File file = new File("peer_"+t);
			if (!file.exists()) {
				if (file.mkdir()) {
					System.out.println("Directory is created!");
				} else {
					System.out.println("Failed to create directory!");
				}
			}
		 
		}
		
	}
