package streamit.library;

import java.util.*;

public class WeightedRoundRobinJoiner extends Joiner
{
    List srcsWeight = new ArrayList();

    void addWeight(Integer weight)
    {
        ASSERT(weight != null && weight.intValue() >= 0);

        srcsWeight.add(weight);
    }

    public boolean isInputUsed(int index)
    {
        ASSERT(index < srcsWeight.size());
        return ((Integer) srcsWeight.get(index)).intValue() != 0;
    }

    public void connectGraph()
    {
        // do I even have anything to do?
        ASSERT(srcs.size() == srcsWeight.size());
        super.connectGraph();
    }

    public void work()
    {
        ASSERT(srcsWeight.size() == srcs.size());

        int inputIndex;
        for (inputIndex = 0; inputIndex < srcs.size(); inputIndex++)
        {
            int inputCount;

            for (inputCount =
                ((Integer) srcsWeight.get(inputIndex)).intValue();
                inputCount > 0;
                inputCount--)
            {
                passOneData(input[inputIndex], output);
            }
        }
    }

    public int[] getWeights()
    {
        int numChildren = srcs.size();
        int[] weights = new int[numChildren];

        int i;
        for (i = 0; i < numChildren; i++)
        {
            if (srcs.get(i) != null && ((Stream) srcs.get(i)).output != null)
            {
                weights[i] = ((Integer) srcsWeight.get(i)).intValue();
            }
        }

        return weights;
    }

    public int getProduction()
    {
        int numChildren = srcs.size();
        int outputTotal = 0;

        int i;
        for (i = 0; i < numChildren; i++)
        {
            if (srcs.get(i) != null && ((Stream) srcs.get(i)).output != null)
            {
                outputTotal += ((Integer) srcsWeight.get(i)).intValue();
            }
        }

        return outputTotal;
    }
}
