package streamit;

// implements a pipeline - stream already has all the functionality,
// so there's no need to put it in Pipeline - just inherit Stream :)
public class Pipeline extends Stream
{
    public Pipeline () { }
    public Pipeline (int n) { super (n); }
    public Pipeline (int n1, int n2) { super (n1, n2); }
}
