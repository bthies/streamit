package at.dms.kjc.spacedynamic;

import at.dms.util.Utils;
import at.dms.kjc.*;

public abstract class IODevice
{
    protected IOPort port;
    
    public abstract String toString();
    
    public IOPort getPort() 
    {
	return port;
    }

    public void connect(IOPort p) 
    {
	port = p;
    }

    public boolean isFileReader() 
    {
	return this instanceof FileReaderDevice;
    }
    
}
