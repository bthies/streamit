package streamit;

import java.util.*;

public class WeightedRoundRobinJoiner extends Joiner {
    List srcsWeight = new ArrayList ();
    int inputCount = 0;

    void AddWeight (Integer weight)
    {
        ASSERT (weight != null && weight.intValue () >= 0);

        srcsWeight.add (weight);
    }

    public boolean IsInputUsed (int index)
    {
        ASSERT (index < srcsWeight.size ());
        return ((Integer)srcsWeight.get(index)).intValue () != 0;
    }

    public void ConnectGraph ()
    {
        // do I even have anything to do?
        ASSERT (srcs.size () == srcsWeight.size ());
        super.ConnectGraph ();
    }

    public void Work ()
    {
        while (inputCount == ((Integer)srcsWeight.get (inputIndex)).intValue ())
        {
            inputCount = 0;
            inputIndex = (inputIndex + 1) % srcs.size ();
        }

        PassOneData (input [inputIndex], output);
        inputCount++;
    }
}
