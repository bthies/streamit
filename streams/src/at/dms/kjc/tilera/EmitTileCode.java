package at.dms.kjc.tilera;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import at.dms.kjc.backendSupport.EmitCode;
import at.dms.kjc.common.CodegenPrintWriter;

/**
 * Emit c code for tiles.
 * 
 * @author mgordon
 *
 */
public class EmitTileCode extends EmitCode {
    
    public static final String MAIN_FILE = "main.c";
    
    public EmitTileCode(TileraBackEndFactory backendBits) {
        super(backendBits);
    }
    
    public static void doit(TileraBackEndFactory backendBits) {
        try {
            //generate the makefile that will compile all the tile executeables
            generateMakefile(backendBits);
            //create the file with the main function that will initialize ilib and layout the processes
            generateMainFunction(backendBits);
            
            for (Tile tile : backendBits.getComputeNodes().getTiles()) {
                // if no code was written to this tile's code store, then skip it
                if (!tile.getComputeCode().shouldGenerateCode())
                    continue;
                String outputFileName = "tile" + tile.getTileNumber() + ".c";

                CodegenPrintWriter p = new CodegenPrintWriter(new BufferedWriter(new FileWriter(outputFileName, false)));
                // write out C code
                EmitTileCode codeEmitter = new EmitTileCode(backendBits);

                codeEmitter.generateCHeader(p);

                codeEmitter.emitCodeForComputeNode(tile,p);                

                codeEmitter.generateMain(p);
                p.close();               
            }
        } catch (IOException e) {
            throw new AssertionError("I/O error" + e);
        }
    }
    
    private static void generateMainFunction(TileraBackEndFactory backendBits) throws IOException {
        CodegenPrintWriter p = new CodegenPrintWriter(new BufferedWriter(new FileWriter(MAIN_FILE, false)));
        generateIncludes(p);
        p.println("int main() {");
        p.println("    ilib_init();");
        
        int tilesWithCompute = backendBits.getComputeNodes().tilesWithCompute();
        
        p.println("    ilibProcParam params[" + tilesWithCompute + "];");
        p.println("    memset(params, 0, sizeof(params));"); 
        p.println("    //spawning application");
        int processNumber = 0;
        // create the ilib calls to spawn the appropriate binary on each mapped tile
        for (Tile tile : backendBits.getComputeNodes().getTiles()) {
            // if no code was written to this tile's code store, then skip it
            if (!tile.getComputeCode().shouldGenerateCode())
                continue;
            
            p.println("    params[" + processNumber + "].num_procs = 1;"); 
            p.println("    params[" + processNumber + "].binary_name = \"tile" + tile.getTileNumber() + "\";");
            p.println("    params[" + processNumber + "].tiles.x = " + tile.getX() + ";");
            p.println("    params[" + processNumber + "].tiles.y = " + tile.getY() + ";");
            p.println("    params[" + processNumber + "].tiles.width = 1;");
            p.println("    params[" + processNumber + "].tiles.height = 1;");
            p.println();
            
            processNumber++;
        }
        
        p.println("    if (ilib_proc_exec(" + tilesWithCompute + ", params)  != ILIB_SUCCESS)"); 
        p.println("        ilib_die(\"Failure in 'ilib_proc_exec()'.\");"); 
        
        p.println("    // This process gets replaced by the new ones, so we should never get here."); 
        p.println("    return 0;");
        p.println("}");
        p.close();
    }
    
    public void generateCHeader(CodegenPrintWriter p) {
        generateIncludes(p);
    }
    
    /**
     * Standard code for front of a C file here.
     * 
     */
    public static void generateIncludes(CodegenPrintWriter p) {
        p.println("#include <math.h>");     // in case math functions
        p.println("#include <ilib.h>");     // for ilib
        p.println("#include <stdio.h>");    // in case of FileReader / FileWriter
        p.println("#include \"structs.h\"");
        p.newLine();
        p.newLine();
    }

    private static void generateMakefile(TileraBackEndFactory backendBits) throws IOException {
        CodegenPrintWriter p = new CodegenPrintWriter(new BufferedWriter(new FileWriter("Makefile", false)));

        p.println("ifdef TILERA_ROOT");
        p.println("BIN = $(TILERA_ROOT)/bin/");
        p.println("endif");
        p.println();
        p.println("CC = $(BIN)tile-cc");
        p.println("SIM = $(BIN)tile-sim");
        p.println();
        p.println("EXECUTABLES = $(BOOT_EXE) $(TILES)");
        p.println("BOOT_EXE = main");
        p.print("TILES = ");
        for (Tile tile : backendBits.getComputeNodes().getTiles()) {
            // if no code was written to this tile's code store, then skip it
            if (!tile.getComputeCode().shouldGenerateCode())
                continue;
            p.println("tile" + tile.getTileNumber() + " \\");
        }
        p.println();
        p.println("COMMON_ARGS = \\");
        p.println("  --config tile64 \\");
        p.println("  $(foreach exe,$(EXECUTABLES), --upload $(exe) $(exe)) \\");
        p.println("  -- $(BOOT_EXE)");
        p.println();
        p.println("run: $(EXECUTABLES)");
        p.println("\t$(SIM) \\");
        p.println("\t$(COMMON_ARGS)");
        p.println();
        p.println("test: $(EXECUTABLES)");
        p.println("\t$(SIM) \\");
        p.println("\t$(COMMON_ARGS) > output.run");
        p.println("\tdiff output.run output.txt");
        p.println();
        p.println("build: $(EXECUTABLES)");
        p.println();
        p.println("clean:");
        p.println("\trm -f *.o $(EXECUTABLES)");
        p.println();
        p.println("main.o: main.c");
        p.println("\t$(CC) $(CFLAGS) -c $< -o $@");

        p.println("main: main.o");
        p.println("\t$(CC) $(LDFLAGS) $< -lilib -o $@"); 

       
        for (Tile tile : backendBits.getComputeNodes().getTiles()) {
            // if no code was written to this tile's code store, then skip it
            if (!tile.getComputeCode().shouldGenerateCode())
                continue;
            p.println();
            String file = "tile" + tile.getTileNumber();
            p.println(file + ".o: " + file + ".c");
            p.println("\t$(CC) $(CFLAGS) -c $< -o $@");

            p.println(file + ": " + file  + ".o");
            p.println("\t$(CC) $(LDFLAGS) $< -lilib -o $@"); 
        }
        
        p.println();
        p.println(" .PHONY: run test build clean");
        p.close();

    }
}
