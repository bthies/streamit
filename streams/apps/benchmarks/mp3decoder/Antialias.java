import streamit.Filter;
import streamit.Channel;

public class Antialias extends Filter
{
    public void init ()
    {
        input = new Channel (Float.TYPE, (18 + 1) * 32 + 1);
        output = new Channel (Float.TYPE, (18 + 1) * 32);
    }
    
    float data [] = new float [(18 + 1) * 32];
    
    public void work ()
    {
        float type = input.popFloat ();
        
        int sb18lim=0;
        
        if (type == 0)
        {
            sb18lim = 0;
        } else 
        if (type == 1)
        {
            sb18lim = 18 + 1;
        } else
        if (type == 2)
        {
            sb18lim = (18 + 1) * 31;
        } else
		{
		    ERROR ("invalid type");
		}
		
		{
		    int i;
		    for (i=0;i<(18+1)*32;i++)
		    {
		        data [i] = input.popFloat ();
		    }
		}

		{
    		int sb18;
            for (sb18 = 0; sb18 < sb18lim; sb18 += (18 + 1))
            {
                int ss;
                for (ss = 0; ss < 8; ss++)
                {
                    int src_indx1 = sb18 + 17 + 1 - ss;
                    int src_indx2 = sb18 + 18 + 2 + ss;
                    float bu = data [src_indx1];
                    float bd = data [src_indx2];
                    data[src_indx1] = (bu * cs[ss]) - (bd * ca[ss]);
                    data[src_indx2] = (bd * cs[ss]) + (bu * ca[ss]);
                }
            }
		}
		
        {
            int i;
            for (i=0;i<(18+1)*32;i++)
            {
                output.pushFloat (data [i]);
            }
        }
    }

    private final float cs[] =
        {
            0.857492925712f,
            0.881741997318f,
            0.949628649103f,
            0.983314592492f,
            0.995517816065f,
            0.999160558175f,
            0.999899195243f,
            0.999993155067f };

    private final float ca[] =
        {
            -0.5144957554270f,
            -0.4717319685650f,
            -0.3133774542040f,
            -0.1819131996110f,
            -0.0945741925262f,
            -0.0409655828852f,
            -0.0141985685725f,
            -0.00369997467375f };


}
