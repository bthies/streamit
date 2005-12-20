package at.dms.kjc.spacedynamic;

import at.dms.util.Utils;
import at.dms.kjc.*;

/*******************************************************************************
 * Abstract Class that represents io devices that can be attached to IOPorts
 ******************************************************************************/

public abstract class IODevice {
    /** The port interfaced to by this device */
    protected IOPort port;
    /** true if this IODevice communicates over the GDN **/
    protected boolean isDynamic;

    public abstract String toString();

    public IOPort getPort() {
        return port;
    }

    public boolean isDynamic() {
        return isDynamic;
    }


    public void connect(IOPort p) {
        port = p;
    }

    public boolean isFileReader() {
        return this instanceof FileReaderDevice;
    }

}
