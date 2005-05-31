import java.io.*;

/**
 * Converts file of text floats (1 per line) to binary floats.
 *
 * Usage:
 * java ConvertFile <input-filename> <output-filename>
 *
 * In the streamit source tree, $STREAMIT_HOME/misc/dat2bin.pl might
 * do the same thing (didn't discover it until after writing this).
 */
public class ConvertFile {
    public static void main(String[] args) throws Exception {
	// open
	BufferedReader in = new BufferedReader(new FileReader(args[0]));
	DataOutputStream out = new DataOutputStream(new FileOutputStream(args[1]));

	// write
	String line;
	do {
	    line = in.readLine();
	    if (line!=null) {
		Float num = new Float(line);
		out.writeInt(endianFlip(Float.floatToIntBits(num.floatValue())));
	    }
	} while (line!=null);
	
	// close
	in.close();
	out.close();
    }

    private static int endianFlip (int x) {
        int x0, x1, x2, x3;
        x0 = (x >> 24) & 0xff;
        x1 = (x >> 16) & 0xff;
        x2 = (x >> 8) & 0xff;
        x3 = (x >> 0) & 0xff;

        return (x0 | (x1 << 8) | (x2 << 16) | (x3 << 24));
    }
}
