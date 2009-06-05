package at.dms.kjc.smp;

import java.io.*;

/**
 * This class represents the structs.h file that will contain typedefs for 
 * our generated c code.  It is just a wrapper for a stringbuffer with some
 * append methods.
 * 
 * @author mgordon
 *
 */
public class Structs_h {
    
    private StringBuffer buf;
    
    /**
     * Create a new structs.h text file with an empty string buffer.
     */
    public Structs_h() {
        buf = new StringBuffer();

        buf.append("typedef struct __Complex__ {\n");
        buf.append("  float real;\n");
        buf.append("  float imag;\n");
        buf.append("} __Complex;\n");
        buf.append("typedef int bit\n");
    }
    
    /**
     * Append text to structs.h 
     *  
     * @param text The text to append
     */
    public void addText(String text) {
        buf.append(text);
    }
    
    /**
     * Append text plus a newline to the structs.h file
     * 
     * @param text The text to append
     */
    public void addLine(String text) {
        buf.append(text + "\n");
    }
    
    /**
     * Append text plus a semicolon and a newline to the structs.h file
     * 
     * @param text The text to append
     */
    public void addLineSC(String text) {
        buf.append(text + ";\n");
    }
    
    public void writeToFile() {
        try {
            FileWriter fw = new FileWriter("structs.h");
            fw.write(buf.toString());
            fw.close();
        }
        catch (IOException e) {
            System.err.println("Error writing structs.h file!");
        }
    }
}
