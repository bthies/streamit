package at.dms.kjc.tilera;

import at.dms.kjc.slicegraph.*;
import at.dms.kjc.backendSupport.*;
import java.io.File;
import at.dms.kjc.JBlock;

public class ProcessFileReader {
    
    protected FilterSliceNode filterNode;
    protected SchedulingPhase phase;
    protected TileraBackEndFactory factory;
    protected TileCodeStore codeStore;
    protected FileInputContent fileInput;
    
    public ProcessFileReader (FilterSliceNode filter, SchedulingPhase phase, TileraBackEndFactory factory) {
        this.filterNode = filter;
        this.fileInput = (FileInputContent)filter.getFilter();
        this.phase = phase;
        this.factory = factory;
        //the file reader should be mapped to tile 0 by the the layout algorithm
        codeStore = TileraBackend.backEndBits.getLayout().getComputeNode(filter).getComputeCode();
    }
     
    public void processFileReader() {
        if (phase == SchedulingPhase.INIT) {
            long fileSize = getFileSizeBytes();
            JBlock block = new JBlock();
            //create the heap with shared and uncacheable attributes
            codeStore.appendTxtToGlobal("ilibHeap fileReadHeap;\n");
            block.addStatement(Util.toStmt("int flags = ILIB_MEM_SHARED | ILIB_MEM_UNCACHEABLE"));
            block.addStatement(Util.toStmt("ilib_mem_create_heap(flags, &fileReadHeap)"));
            
            //malloc enough from the heap to store the entire file
            codeStore.appendTxtToGlobal("void *fileReadBuffer;\n");
            block.addStatement(Util.toStmt("fileReadBuffer = ilib_mem_malloc_heap(fileReadHeap, " + 
                    fileSize + ")"));
            
            //open the file read the file into the buffer on the heap
            codeStore.appendTxtToGlobal("FILE *INPUT;\n");
            block.addStatement(Util.toStmt("INPUT = fopen(\"" + fileInput.getFileName() + "\", \"r\")"));
            block.addStatement(Util.toStmt("fread(fileReadBuffer, 1, " + fileSize + ", INPUT)"));
            block.addStatement(Util.toStmt("fclose(INPUT)"));
            
            codeStore.addStatementFirstToBufferInit(block);
        }
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
