import streamit.*;

class OneSource extends Filter
{
    int i;
    public void init ()
    {
        output = new Channel(Float.TYPE, 1);
	i=0;
    }

    public void work()
    {
        output.pushFloat(i++);
    }
}
