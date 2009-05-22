package at.dms.kjc.cluster;

import java.io.FileWriter;
import at.dms.kjc.KjcOptions;
import at.dms.kjc.common.CodegenPrintWriter;


/**
 * Generates cluster-setup.txt for cluster back end.
 * 
 * <p>Gives default values for frequency_of_checkpoints: 0,
 * <br/>Number of bytes to buffer before writing to socket.
 * <br/>and Number of steady state iterations, usually overridden by -i switch.
 * </p>
 * 
 * @author Janis
 *
 */
public class GenerateSetupFile {
    public static void generateSetupFile() {

        CodegenPrintWriter p = new CodegenPrintWriter();


        p.print("frequency_of_checkpoints 0    // Must be a multiple of 1000 or 0 for disabled.\n");
        p.print("outbound_data_buffer 1000     // Number of bytes to buffer before writing to socket. Must be <= 1400 or 0 for disabled\n");
        if(KjcOptions.iterations != -1)
            p.print("number_of_iterations " + KjcOptions.iterations + "    // Number of steady state iterations can be overriden by parameter -i <number>\n");
        else
            p.print("number_of_iterations 10000    // Number of steady state iterations can be overriden by parameter -i <number>\n");

        try {
            FileWriter fw = new FileWriter("cluster-setup.txt");
            fw.write(p.getString());
            fw.close();
        }
        catch (Exception e) {
            System.err.println("Unable to write cluster setup file");
        }   
    }
}

