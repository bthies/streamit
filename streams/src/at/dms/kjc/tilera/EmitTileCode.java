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
import at.dms.kjc.KjcOptions;

/**
 * Emit c code for tiles.
 * 
 * @author mgordon
 *
 */
public class EmitTileCode extends EmitCode {
    
    public static final String MAIN_FILE = "main.c";
    public static final String GROUP_PREFIX = "ILIB_GROUP_";
    
    public EmitTileCode(TileraBackEndFactory backendBits) {
        super(backendBits);
    }
    
    public static void doit(TileraBackEndFactory backendBits) {
        try {
            //generate the makefile that will compile all the tile executeables
            generateMakefile(backendBits);
            //create the file with the main function that will initialize ilib and layout the processes
            generateMainFunction(backendBits);
            
            //for all the tiles, add a barrier at the end of the steady state, do it here because we are done
            //with all code gen
            TileCodeStore.addBarrierSteady();
            
            for (Tile tile : TileraBackend.chip.getAbstractTiles()) {
                // if no code was written to this tile's code store, then skip it
                if (!tile.getComputeCode().shouldGenerateCode())
                    continue;
                String outputFileName = "tile" + tile.getTileNumber() + ".c";
                
                CodegenPrintWriter p = new CodegenPrintWriter(new BufferedWriter(new FileWriter(outputFileName, false)));
                // write out C code
                EmitTileCode codeEmitter = new EmitTileCode(backendBits);

                codeEmitter.generateCHeader(p, tile);

                codeEmitter.emitCodeForComputeNode(tile,p);                

                codeEmitter.generateMain(p, tile);
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
        
        p.println("    ilibProcParam params[" + TileraBackend.chip.getAbstractTiles().size() + "];");
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
        
        p.println("    if (ilib_proc_exec(" + TileraBackend.chip.abstractSize() + ", params)  != ILIB_SUCCESS)"); 
        p.println("        ilib_die(\"Failure in 'ilib_proc_exec()'.\");"); 
        
        p.println("    // This process gets replaced by the new ones, so we should never get here."); 
        p.println("    return 0;");
        p.println("}");
        p.close();
    }
    
    public void generateCHeader(CodegenPrintWriter p, Tile t) {
        generateIncludes(p);
        p.println("ilibStatus ignore_status;");
        
        //defines for static network
        p.println("#define SN_NORTH  1 /**< North edge */");
        p.println("#define SN_EAST   2 /**< East edge */");
        p.println("#define SN_SOUTH  3 /**< South edge */");
        p.println("#define SN_WEST   4 /**< West edge */");
        p.println("#define SN_MAIN   5 /**< Main Processor */");

        p.println("#define MAIN_INPUT(input) (input << 12)");
        p.println("#define WEST_INPUT(input) (input << 9)");
        p.println("#define SOUTH_INPUT(input) (input << 6)");
        p.println("#define EAST_INPUT(input) (input << 3)");
        p.println("#define NORTH_INPUT(input) (input)");
        
        //THE NUMBER OF SS ITERATIONS FOR EACH cycle counting block 
        p.println("#define ITERATIONS " + KjcOptions.numbers);
        
        
    }
    
    public static String staticNetworkBarrierRouting(Tile tile) {
        String code = "__insn_mtspr(SPR_SNSTATIC, (";
        int gXSize = TileraBackend.chip.abstractXSize();
        int gYSize = TileraBackend.chip.abstractYSize();
        
        if (gXSize == 1 && gYSize == 1)
            return "";
        
        assert gXSize % 2 == 0 &&
               gYSize % 2 == 0  : "Only even row / col sizes are supported";
        int row = tile.getY();
        int col = tile.getX();
        
        //SNAKE THE ITEMS AROUND THE CHIP IN A CIRCUIT
        String route = "";
        
        if (row == 0 && col == 0) {
            route = "MAIN_INPUT(SN_EAST) | SOUTH_INPUT(SN_MAIN)"; 
        } else if (col % 2 == 0) {
            if (row == 0) { //not 0,0
                route = "MAIN_INPUT(SN_EAST) | WEST_INPUT(SN_MAIN)"; 
            } else if (row == 1 && col > 0) {
                route = "MAIN_INPUT(SN_WEST) | SOUTH_INPUT(SN_MAIN)"; 
            } else if (row == gYSize -1) {
                route = "MAIN_INPUT(SN_NORTH) | EAST_INPUT(SN_MAIN)"; 
            } else {
                route = "MAIN_INPUT(SN_NORTH) | SOUTH_INPUT(SN_MAIN)"; 
            }
        } else {
            //odd col
            if (row == 0 && col == gXSize -1) {
                route = "MAIN_INPUT(SN_SOUTH) | WEST_INPUT(SN_MAIN)"; 
            } else if (row == 0) {
                route = "MAIN_INPUT(SN_EAST) | WEST_INPUT(SN_MAIN)"; 
            } else if (row == 1 && col < gXSize - 1) {
                route = "MAIN_INPUT(SN_SOUTH) | EAST_INPUT(SN_MAIN)";
            } else if (row == gYSize - 1) {
                route = "MAIN_INPUT(SN_WEST) | NORTH_INPUT(SN_MAIN)";
            } else {
                route = "MAIN_INPUT(SN_SOUTH) | NORTH_INPUT(SN_MAIN)";
            }
        }
        
        assert !route.equals("");
        code += route + "));";
        /*System.out.println(TileraBackend.chip.getTranslatedTileNumber(tile.getTileNumber()) +
                " (" + row + ", " + col + ") " + route);*/
        return code;
    }
    
    /**
     * Standard code for front of a C file here.
     * 
     */
    public static void generateIncludes(CodegenPrintWriter p) {
        p.println("#include <math.h>");     // in case math functions
        p.println("#include <ilib.h>");     // for ilib
        p.println("#include <stdio.h>");    // in case of FileReader / FileWriter
        if (KjcOptions.fixedpoint)
            p.println("#include \"fixed.h\"");
        p.println("#include \"structs.h\"");
        p.println("#include <sys/archlib.h>");
        if (KjcOptions.profile)
            p.println("#include <sys/profiler.h>");
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
        if (KjcOptions.fixedpoint)
            p.println("CC = $(BIN)tile-c++");
        else
            p.println("CC = $(BIN)tile-cc");
        p.println("CFLAGS = -Os -OPT:Olimit=0");
        p.println("SIM = $(BIN)tile-sim");
        p.println("MONITOR = $(BIN)tile-monitor");
        p.println("LDFLAGS = -static");
        p.println();
        p.println("EXECUTABLES = $(BOOT_EXE) $(TILES)");
        p.println("BOOT_EXE = main");
        p.print("TILES = ");
        for (Tile tile : TileraBackend.chip.getAbstractTiles()) {
            // if no code was written to this tile's code store, then skip it
            if (!tile.getComputeCode().shouldGenerateCode())
                continue;
            p.println("tile" + tile.getTileNumber() + " \\");
        }
        p.println();
        p.println("COMMON_ARGS = \\");
        p.println("  --magic-os \\");
        p.println("  --config tile64 \\");
        if (KjcOptions.profile) 
            p.println("  --xml-profile profile.xml \\");
        p.println("  $(foreach exe,$(EXECUTABLES), --upload $(exe),$(exe)) \\");
        p.println("  -- $(BOOT_EXE)");
        p.println();
        p.println("MONITOR_ARGS = \\");
        for (String fileName : ProcessFileReader.fileNames) {
            p.println("  --upload " + fileName + " " + fileName + "\\");
        }
        p.println("  $(foreach exe,$(EXECUTABLES), --upload $(exe) $(exe)) \\");
        p.println("  -- $(BOOT_EXE)");
        p.println();
        
        p.println();
        p.println("run: $(EXECUTABLES)");
        p.println("\t$(SIM) \\");
        p.println("\t$(COMMON_ARGS)");
        p.println();
        p.println("monitor: $(EXECUTABLES)");
        p.println("\t$(MONITOR) \\");
        p.println("\t--simulator \\");
        p.println("\t--image tile64 \\");
        p.println("\t--xml-profile profile.xml \\");
        p.println("\t--config tile64 \\");
        p.println("\t$(MONITOR_ARGS)");
        p.println();
        p.println("pci: $(EXECUTABLES)");
        p.println("\t$(MONITOR) \\");
        p.println("\t--pci \\");
        p.println("\t$(MONITOR_ARGS)");
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
        p.println("main.o: main.c" + (KjcOptions.fixedpoint ? " fixed" : ""));
        p.println("\t$(CC) $(CFLAGS) -c $< -o $@");

        p.println("main: main.o");
        p.println("\t$(CC) $(LDFLAGS) $< -lilib -lm -o $@"); 

       
        for (Tile tile : TileraBackend.chip.getAbstractTiles()) {
            // if no code was written to this tile's code store, then skip it
            if (!tile.getComputeCode().shouldGenerateCode())
                continue;
            p.println();
            String file = "tile" + tile.getTileNumber();
            String fixed = (KjcOptions.fixedpoint ? " fixed" : "");
            p.println(file + ".o: " + file + ".c" + fixed);
            p.println("\t$(CC) $(CFLAGS) -c $< -o $@");

            p.println(file + ": " + file  + ".o");
            p.println("\t$(CC) $(LDFLAGS) $< -lilib -lm -o $@"); 
        }
        
        if (KjcOptions.fixedpoint) {
          p.println();
          p.println("fixed: $(STREAMIT_HOME)/library/fixed_point/fixed.h");
          p.println("\tcp $(STREAMIT_HOME)/library/fixed_point/fixed.h .");
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
        System.out.println("Optimizing...");
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
        Set <RotatingBuffer> outputBuffers = OutputRotatingBuffer.getOutputBuffersOnTile((Tile)n);
        Set <InputRotatingBuffer> inputBuffers = InputRotatingBuffer.getInputBuffersOnTile((Tile)n);
        
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
    public void generateMain(CodegenPrintWriter p, Tile t) {
        p.println();
        p.println();
        p.println("// main() Function Here");
        // dumb template to override
        p.println("int main(int argc, char** argv) {\n");
        p.indent();
        if (KjcOptions.profile)
            p.println("profiler_disable();");
        p.println(staticNetworkBarrierRouting(t));
        p.println("__insn_mtspr(SPR_SNCTL, 0x2);");
        p.println("ilib_init();");
        p.println(TileCodeStore.bufferInitMethName + "();");
        p.println(backendbits.getComputeNodes().getNthComputeNode(0).getComputeCode().getMainFunction().getName() + "();");
        p.println("ilib_finish();");
        p.println("return 0;");
        p.outdent();
        p.println("}");
    }
}
