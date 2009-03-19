package at.dms.kjc.smp;

import at.dms.kjc.slicegraph.*;
import at.dms.kjc.backendSupport.*;
import java.io.File;

import at.dms.kjc.*;

import java.util.HashMap;
import java.util.HashSet;

public class ProcessFileReader {
    
    protected FilterSliceNode filterNode;
    protected SchedulingPhase phase;
    protected SMPBackEndFactory factory;
    protected CoreCodeStore codeStore;
    protected FileInputContent fileInput;
    protected static HashMap<FilterSliceNode, Core> allocatingTiles;
    protected Core allocatingTile;
    protected OutputSliceNode fileOutput;  
    protected static HashMap<FilterSliceNode, JMethodDeclaration> PPMethods;
    protected static HashSet<String> fileNames;
    
    static {
        allocatingTiles = new HashMap<FilterSliceNode, Core>();
        PPMethods = new HashMap<FilterSliceNode, JMethodDeclaration>();
        fileNames = new HashSet<String>();
    }
    
    public ProcessFileReader (FilterSliceNode filter, SchedulingPhase phase, SMPBackEndFactory factory) {
        this.filterNode = filter;
        this.fileInput = (FileInputContent)filter.getFilter();
        this.phase = phase;
        this.factory = factory;
        this.allocatingTile = nextAllocatingTile();
        this.fileOutput = filter.getParent().getTail();
        codeStore = allocatingTile.getComputeCode();
    }
     
    public void processFileReader() {
        if (phase == SchedulingPhase.INIT) {
            fileNames.add(fileInput.getFileName());
            allocateAndCommunicateAddrs();
        }
        for (InterSliceEdge edge : fileOutput.getDestSet(SchedulingPhase.STEADY)) {
            generateCode(edge.getDest().getNextFilter());
        }
    }

    private void generateCode(FilterSliceNode dsFilter) {
        //TODO: fix so that file reader code is not created twice!
        FileReaderCode fileReaderCode;
        InputRotatingBuffer destBuf = InputRotatingBuffer.getInputBuffer(dsFilter);
        fileReaderCode = new FileReaderRemoteReads(destBuf);
        
        CoreCodeStore codeStore = SMPBackend.scheduler.getComputeNode(dsFilter).getComputeCode();
        
        switch (phase) {
        case INIT : generateInitCode(fileReaderCode, codeStore, destBuf); break;
        case PRIMEPUMP : generatePPCode(dsFilter, fileReaderCode, codeStore, destBuf); break;
        case STEADY : generateSteadyCode(fileReaderCode, codeStore, destBuf); break;
        }
    }
    
    private void generateInitCode(FileReaderCode fileReaderCode, CoreCodeStore codeStore, 
            InputRotatingBuffer destBuf) {
        JBlock statements = new JBlock(fileReaderCode.commandsInit);
        //create a method 
        JMethodDeclaration initMethod = new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
                CStdType.Void,
                "__File_Reader_Init__",
                JFormalParameter.EMPTY,
                CClassType.EMPTY,
                statements,
                null,
                null);


        codeStore.addMethod(initMethod);
        codeStore.addInitStatement(new JExpressionStatement(null,
                new JMethodCallExpression(null, new JThisExpression(null),
                        initMethod.getName(), new JExpression[0]), null));
    }

    private void generatePPCode(FilterSliceNode node, FileReaderCode fileReaderCode, CoreCodeStore codeStore, 
            InputRotatingBuffer destBuf) {
        if (!PPMethods.containsKey(node)) {
            JBlock statements = new JBlock(fileReaderCode.commandsSteady);
            //create a method 
            JMethodDeclaration ppMethod = new JMethodDeclaration(null, at.dms.kjc.Constants.ACC_PUBLIC,
                    CStdType.Void,
                    "__File_Reader_PrimePump__",
                    JFormalParameter.EMPTY,
                    CClassType.EMPTY,
                    statements,
                    null,
                    null);


            codeStore.addMethod(ppMethod);
            PPMethods.put(node, ppMethod);
        }

        codeStore.addInitStatement(new JExpressionStatement(null,
                new JMethodCallExpression(null, new JThisExpression(null),
                        PPMethods.get(node).getName(), new JExpression[0]), null));
    }
    
    private void generateSteadyCode(FileReaderCode fileReaderCode, CoreCodeStore codeStore, 
            InputRotatingBuffer destBuf) {
        
        JBlock steadyBlock = new JBlock(fileReaderCode.commandsSteady);
        
        codeStore.addSteadyLoopStatement(steadyBlock);
    }
    
    private void allocateAndCommunicateAddrs() {
        long fileSize = getFileSizeBytes();
        JBlock block = new JBlock();

        codeStore.appendTxtToGlobal(fileInput.getType() + "*fileReadBuffer;\n");
        codeStore.appendTxtToGlobal("int fileReadIndex__n" + codeStore.getParent().getUniqueId() + " = 0;\n");

        //open the file read the file into the buffer on the heap
        /*
        codeStore.appendTxtToGlobal("int INPUT;\n");
        block.addStatement(Util.toStmt("struct stat statbuf"));
        block.addStatement(Util.toStmt("INPUT = open(\"" + fileInput.getFileName() + "\", O_RDWR)"));
        block.addStatement(Util.toStmt("fstat(INPUT, &statbuf)"));
        block.addStatement(Util.toStmt("fileReadBuffer = (" + fileInput.getType() + "*)mmap(NULL, statbuf.st_size, PROT_READ | PROT_WRITE, MAP_PRIVATE, INPUT, 0)"));
        */
        codeStore.appendTxtToGlobal("FILE *INPUT;\n");
        block.addStatement(Util.toStmt("struct stat statbuf"));
        block.addStatement(Util.toStmt("stat(\"" + fileInput.getFileName() + "\", &statbuf)"));
        block.addStatement(Util.toStmt("fileReadBuffer = (" + fileInput.getType() + " *)malloc(statbuf.st_size)"));
        block.addStatement(Util.toStmt("INPUT = fopen(\"" + fileInput.getFileName() + "\", \"r\")"));
        block.addStatement(Util.toStmt("if(fread((void *)fileReadBuffer, " + fileInput.getType().getSizeInC() + 
        								", statbuf.st_size / " + fileInput.getType().getSizeInC() + ", INPUT) != statbuf.st_size)"));
        block.addStatement(Util.toStmt("  printf(\"Error reading %lu bytes of input file\\n\", (unsigned long)statbuf.st_size)"));

        for (Core other : SMPBackend.chip.getCores()) {
            if (codeStore.getParent() == other) 
                continue;

            other.getComputeCode().appendTxtToGlobal("int fileReadIndex__n" + other.getUniqueId() + " = 0;\n");
        }

        codeStore.addStatementFirstToBufferInit(block);
    }


    /**
     * @return The tile we should allocate this file reader on.  Remember that 
     * the file reader is allocated to off-chip memory.  We just cycle through the tiles
     * if there is more than one file reader, one reader per tile.
     */
    private Core nextAllocatingTile() {
        if(allocatingTiles.get(filterNode) != null)
            return allocatingTiles.get(filterNode);
        
        for (Core tile : SMPBackend.chip.getCores()) {
            if (!allocatingTiles.containsValue(tile)) {
                allocatingTiles.put(filterNode, tile);
                return tile;
            }
/*
	    else if(allocatingTiles.get(filterNode).equals(tile)) {
		return tile;
	    }
*/
        }
        assert false : "Too many file readers for this chip (one per tile)!";
        return null;
    }
    
    private long getFileSizeBytes() {
        long size = 0;
        
        try {
            File inputFile = new File(fileInput.getFileName());
            size = inputFile.length();
            System.out.println("Input file " + fileInput.getFileName() + " has size " + size);
        } catch (Exception e) {
            System.err.println("Error opening input file: " + fileInput.getFileName());
            System.exit(1);
        }
        return size;
    }
    
}
