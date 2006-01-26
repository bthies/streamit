package at.dms.kjc.rstream;


/**
 * This class stores various string names that are used in different
 * passes such as the name of the functions produced in the C code
 * and different variables used in the C code produced.
 *
 * @author Michael Gordon
 * 
 */

public class Names 
{
    /*** fields for the var names we introduce ***/
    public static String recvBuffer = "__RECVBUFFER__";
    public static String recvBufferSize = "__RECVBUFFERSIZE__";
    public static String recvBufferBits = "__RECVBUFFERBITS__";

    //the output buffer for ratematching
    public static String sendBuffer = "__SENDBUFFER__";
    public static String sendBufferIndex = "__SENDBUFFERINDEX__";
    public static String rateMatchSendMethod = "__RATEMATCHSEND__";
    
    //recvBufferIndex points to the beginning of the tape
    public static String recvBufferIndex = "__RECVBUFFERINDEX__";
    //recvIndex points to the end of the tape
    public static String recvIndex = "_RECVINDEX__";

    public static String simpleIndex = "__SIMPLEINDEX__";
    
    public static String exeIndex = "__EXEINDEX__";
    public static String exeIndex1 = "__EXEINDEX__1__";

    public static String ARRAY_INDEX = "__ARRAY_INDEX__";
    public static String ARRAY_COPY = "__ARRAY_COPY__";
    
    public static String initSchedFunction = "__RAWINITSCHED__";
    public static String steadySchedFunction = "__RAWSTEADYSCHED__";
    
    public static String receiveMethod = "static_receive_to_mem";
    public static String structReceiveMethodPrefix = "__popPointer";
    public static String arrayReceiveMethod = "__array_receive__";

    public static String main = "__MAIN__";

    public static String fscanf = "fscanf";
    // RMR { add fread, frwrite, sizeof, and address-of operator
    public static String fread  = "fread";
    public static String fwrite = "fwrite";
    public static String sizeof = "sizeof";
    public static String addressof = "&";
    // } RMR
    public static String fprintf = "fprintf";
    // AD { add for timing calls
    //       need some of #include <sys/time.h> #include <sys/times.h>
    //       #include <sys/types.h> #include <unistd.h>
    public static String times = "times";
    public static String sysconf = "sysconf";
    public static String _SC_CLK_TCK = "_SC_CLK_TCK";
    // } AD
}

