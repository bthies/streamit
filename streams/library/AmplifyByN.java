import streamit.*;

class AmplifyByN extends Filter
{
    int n;
    AmplifyByN (int n) { super (n); }
    Channel input = new Channel(Integer.TYPE, 1);
    Channel output = new Channel (Integer.TYPE, 1);

    public void initIO ()
    {
        streamInput = input;
        streamOutput = output;
    }

    public void init (int nVal)
    {
        n = nVal;
    }

    public void work()
    {
        output.pushInt(input.popInt() * n);
    }

    void setN(int nVal)
    {
        n = nVal;
    }
}
