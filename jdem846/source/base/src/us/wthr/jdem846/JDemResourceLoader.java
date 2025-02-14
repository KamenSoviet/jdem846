package us.wthr.jdem846;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class JDemResourceLoader
{
	
	private static Throwable lastException;
	
	protected JDemResourceLoader()
	{
		
	}
	

	
	public static InputStream getAsInputStream(String url) throws FileNotFoundException
	{
		try {
			Resource resource = new Resource(url);
			return resource.getAsInputStream();
		} catch (Exception ex) {
			ex.printStackTrace();
			setLastException(ex);
			return null;
		}
	}
	
	public static OutputStream getAsOutputStream(String url) throws FileNotFoundException
	{
		try {
			Resource resource = new Resource(url);
			return resource.getAsOutputStream();
		} catch (Exception ex) {
			ex.printStackTrace();
			setLastException(ex);
			return null;
		}
	}
	
	public static File getAsFile(String url)
	{
		try {
			Resource resource = new Resource(url);
			return resource.getAsFile();
		} catch (Exception ex) {
			ex.printStackTrace();
			setLastException(ex);
			return null;
		}
	}
	
	
	public static URL getAsURL(String url)
	{
		try {
			Resource resource = new Resource(url);
			return resource.getAsURL();
		} catch (Exception ex) {
			ex.printStackTrace();
			setLastException(ex);
			return null;
		}	
	}
	
	
	protected static void setLastException(Throwable thrown)
	{
		JDemResourceLoader.lastException = thrown;
	}
	
	public static Throwable getLastException()
	{
		return JDemResourceLoader.lastException;
	}
	
}
