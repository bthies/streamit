package at.dms.kjc.spacetime;

public class FileState 
{
    private boolean reader;
    private StreamingDram parent;
    private String file;

    //determines if this filereader/writer
    //has been visited by rawify in the various stages
    private boolean visitedInit = false;
    private boolean visitedPP = false;
    private boolean visitedSteady = false;

    public FileState(boolean reader, StreamingDram parent,
		     String file) 
    {
	this.reader = reader;
	this.parent = parent;
	this.file = file;
    }
    
    public void setVisited(boolean init, boolean primepump) 
    {
	if (init)
	    visitedInit = true;
	else if (primepump)
	    visitedPP = true;
	else
	    visitedSteady = true;
    }
    
    public boolean isVisited(boolean init, boolean primepump) 
    {
	if (init)
	    return visitedInit;
	else if (primepump)
	    return visitedPP;
	else 
	    return visitedSteady;
    }
    
    public boolean isReader() 
    {
	return reader;
    }
}
