import streamit.*;

class AmplifyByN extends Filter
{
    int N;
    AmplifyByN (int n) { super (n); }
    public void Init(int Nval)
    {
        input = new Channel (Integer.TYPE);
        output = new Channel (Integer.TYPE);
        N = Nval;
    }

    public void InitCount ()
    {
        inCount = 1;
        outCount = 1;
    }

    public void Work()
    {
        output.PushInt(input.PopInt() * N);
    }

    void SetN(int Nval)
    {
        N = Nval;
    }
}
