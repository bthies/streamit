package at.dms.kjc.spacetime;

import at.dms.kjc.slicegraph.*;

public class FileState 
{
    private StreamingDram parent;
    private PredefinedContent file;

    //determines if this filereader/writer
    //has been visited by rawify in the various stages
    private boolean visitedInit = false;
    private boolean visitedPP = false;
    private boolean visitedSteady = false;

    public FileState(PredefinedContent file, StreamingDram parent)
    {
        this.parent = parent;
        this.file = file;
    }
    
    public PredefinedContent getContent() 
    {
        return file;
    }
    
    public boolean isFP() 
    {
        if (isReader())
            return ((FileInputContent)file).isFP();
        else
            return ((FileOutputContent)file).isFP();
    }
    
    public boolean isReader() 
    {
        return (file instanceof FileInputContent);
    }

    public String getFileName() 
    {
        if (isReader())
            return ((FileInputContent)file).getFileName();
        else
            return ((FileOutputContent)file).getFileName();
    }
    
}
