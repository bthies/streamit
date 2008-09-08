package at.dms.kjc.tilera;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import at.dms.kjc.JFieldDeclaration;
import at.dms.kjc.JMethodDeclaration;
import at.dms.kjc.JStatement;
import at.dms.kjc.backendSupport.Channel;
import at.dms.kjc.backendSupport.ComputeNode;
import at.dms.kjc.backendSupport.EmitCode;
import at.dms.kjc.common.CodegenPrintWriter;
import at.dms.kjc.sir.SIRCodeUnit;

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
    
    
    
    /**
     * This will generate the main function for the executable that creates the application (e.g., 
     * spawns the filter code for all the tiles).
     * 
     * @param backendBits The backend factory
     * @throws IOException
     */
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
        for (int i = 0; i < TileraBackend.chip.abstractSize(); i++) {
            Tile tile = TileraBackend.chip.getTranslatedTile(i);
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
        p.println("ilibStatus ignore_status;");
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
        p.println("#include <sys/archlib.h>");
        p.println("#include <pass.h>");
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
        p.println("CFLAGS = -O3");
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
        p.println("  $(foreach exe,$(EXECUTABLES), --upload $(exe),$(exe)) \\");
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
    
    /**
     * Given a ComputeNode and a CodegenPrintWrite, print all code for the ComputeNode.
     * Channel information relevant to the ComputeNode is printed based on data in the
     * BackEndFactory passed when this class was instantiated.
     * 
     * @param n The ComputeNode to emit code for.
     * @param p The CodegenPrintWriter (left open on return).
     */
    public void emitCodeForComputeStore (SIRCodeUnit fieldsAndMethods,
            ComputeNode n, CodegenPrintWriter p, CodeGen codegen) {
        
        // Standard final optimization of a code unit before code emission:
        // unrolling and constant prop as allowed, DCE, array destruction into scalars.
        (new at.dms.kjc.sir.lowering.FinalUnitOptimize()).optimize(fieldsAndMethods);
        
        p.println("// code for tile " + n.getUniqueId());
        
        p.println(((Tile)n).getComputeCode().getGlobalText());
        
        // generate function prototypes for methods so that they can call each other
        // in C.
        codegen.setDeclOnly(true);
        for (JMethodDeclaration method : fieldsAndMethods.getMethods()) {
            method.accept(codegen);
        }
        p.println("");
        codegen.setDeclOnly(false);

        // generate code for ends of channels that connect to code on this ComputeNode
        Set <RotatingBuffer> outputBuffers = OutputRotatingBufferDMA.getBuffersOnTile((Tile)n);
        Set <RotatingBuffer> inputBuffers = InputRotatingBuffer.getBuffersOnTile((Tile)n);
        
        // externs
        for (RotatingBuffer c : outputBuffers) {
            if (c.writeDeclsExtern() != null) {
                for (JStatement d : c.writeDeclsExtern()) { d.accept(codegen); }
            }
        }
       
        for (RotatingBuffer c : inputBuffers) {
            if (c.readDeclsExtern() != null) {
                for (JStatement d : c.readDeclsExtern()) { d.accept(codegen); }
            }
        }

        for (RotatingBuffer c : outputBuffers) {
            if (c.dataDecls() != null) {
                // wrap in #ifndef for case where different ends have
                // are in different files that eventually get concatenated.
                p.println();
                p.println("#ifndef " + c.getIdent() + "_CHANNEL_DATA");
                for (JStatement d : c.dataDecls()) { d.accept(codegen); p.println();}
                p.println("#define " + c.getIdent() + "_CHANNEL_DATA");
                p.println("#endif");
            }
        }
        
        for (RotatingBuffer c : inputBuffers) {
            if (c.dataDecls() != null && ! outputBuffers.contains(c)) {
                p.println("#ifndef " + c.getIdent() + "_CHANNEL_DATA");
                for (JStatement d : c.dataDecls()) { d.accept(codegen); p.println();}
                p.println("#define " + c.getIdent() + "_CHANNEL_DATA");
                p.println("#endif");
            }
        }

        for (RotatingBuffer c : outputBuffers) {
            p.println("/* output buffer " + "(" + c.getIdent() + " of " + c.getFilterNode() + ") */");
            if (c.writeDecls() != null) {
                for (JStatement d : c.writeDecls()) { d.accept(codegen); p.println();}
            }
            if (c.pushMethod() != null) { c.pushMethod().accept(codegen); }
        }

        for (RotatingBuffer c : inputBuffers) {
            p.println("/* input buffer (" + c.getIdent() + " of " + c.getFilterNode() + ") */");
            if (c.readDecls() != null) {
                for (JStatement d : c.readDecls()) { d.accept(codegen); p.println();}
            }
            if (c.peekMethod() != null) { c.peekMethod().accept(codegen); }
            if (c.assignFromPeekMethod() != null) { c.assignFromPeekMethod().accept(codegen); }
            if (c.popMethod() != null) { c.popMethod().accept(codegen); }
            if (c.assignFromPopMethod() != null) { c.assignFromPopMethod().accept(codegen); }
            if (c.popManyMethod() != null) { c.popManyMethod().accept(codegen); }
         }
        p.println("");
        
        // generate declarations for fields
        for (JFieldDeclaration field : fieldsAndMethods.getFields()) {
            field.accept(codegen);
        }
        p.println("");
        
        //handle the buffer initialization method separately because we do not want it
        //optimized (it is not in the methods list of the code store
        ((Tile)n).getComputeCode().getBufferInitMethod().accept(codegen);
        
        // generate functions for methods
        codegen.setDeclOnly(false);
        for (JMethodDeclaration method : fieldsAndMethods.getMethods()) {
            method.accept(codegen);
        }
    }
    
    /**
     * Generate a "main" function for the current tile (this is used for filter code).
     */
    public void generateMain(CodegenPrintWriter p) {
        p.println();
        p.println();
        p.println("// main() Function Here");
        // dumb template to override
        p.println(
"int main(int argc, char** argv) {\n");
        p.indent();
        p.println("ilib_init();");
        p.println(TileCodeStore.bufferInitMethName + "();");
        p.println(
backendbits.getComputeNodes().getNthComputeNode(0).getComputeCode().getMainFunction().getName()
+ "();");
        p.println("ilib_finish();");
        p.println("return 0;");
        p.outdent();
        p.println("}");
    }
}
