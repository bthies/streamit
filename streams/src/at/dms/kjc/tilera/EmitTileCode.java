package at.dms.kjc.tilera;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import at.dms.kjc.backendSupport.EmitCode;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.vanillaSlice.UniBackEndFactory;


/**
 * Emit c code for tiles.
 * 
 * @author mgordon
 *
 */
public class EmitTileCode extends EmitCode {
    
    public EmitTileCode(TileraBackEndFactory backendBits) {
        super(backendBits);
    }
    
    public static void doit(TileraBackEndFactory backendBits) {
        for (Tile tile : backendBits.getComputeNodes().getTiles()) {
            String outputFileName = "tile" + tile.getTileNumber() + ".c";
            try {
                CodegenPrintWriter p = new CodegenPrintWriter(new BufferedWriter(new FileWriter(outputFileName, false)));
                // write out C code
                EmitTileCode codeEmitter = new EmitTileCode(backendBits);
                
                codeEmitter.generateCHeader(p);

                codeEmitter.emitCodeForComputeNode(tile,p);                
                
                codeEmitter.generateMain(p);
                p.close();
                } catch (IOException e) {
                    throw new AssertionError("I/O error on " + outputFileName + ": " + e);
                }           
        }
    }
    
    
    /**
     * Standard code for front of a C file here.
     * 
     */
    public void generateCHeader(CodegenPrintWriter p) {
        p.println("#include <math.h>");     // in case math functions
        p.println("#include <stdio.h>");    // in case of FileReader / FileWriter
        p.println("#include \"structs.h\"");
        p.newLine();
        p.newLine();
    }
}
