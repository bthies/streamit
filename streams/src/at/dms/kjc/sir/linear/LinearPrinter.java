package at.dms.kjc.sir.linear;

/** Control point for printing messages **/
public class LinearPrinter {
    /** flag to control output generation. **/
    private static boolean outputEnabled = false;
    public static boolean getOutput() {
	return outputEnabled;
    }
    public static void setOutput(boolean outFlag) {
	outputEnabled = outFlag;
    }
    public static void println(String message) {
	if (outputEnabled) {
	    System.out.println(message);
	}
    }
    public static void print(String message) {
	if (outputEnabled) {
	    System.out.print(message);
	}
    }
    public static void warn(String message) {
	System.err.println("WARNING: " + message);
    }
}
