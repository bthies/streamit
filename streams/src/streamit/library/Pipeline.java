package streamit;

/**
 *  implements a pipeline - stream already has all the functionality,
 *  so there's no need to put it in Pipeline - just inherit Stream :)
 */

public class Pipeline extends Stream
{
    /**
     * Default constructor
     */
    public Pipeline () { }

    /**
     * Constructor with an int.  Just calls the parent (as every construtor
     * should (more or less)
     */
    public Pipeline (int n) { super (n); }

    /**
     * Constructor with two ints.
     */
    public Pipeline (int x, int y) { super (x, y); }
}
