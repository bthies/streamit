import streamit.*;

class AmplifyByN extends Filter
{
    int n;
    AmplifyByN (int n) { super (n); }
    public void init (int nVal)
    {
        input = new Channel(Integer.TYPE, 1);
        output = new Channel (Integer.TYPE, 1);

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
