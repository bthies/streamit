import streamit.library.Filter;
import streamit.library.Channel;

public class Antialias extends Filter
{
    public void init ()
    {
        input = new Channel (Float.TYPE, (18 + 1) * 32 + 1);
        output = new Channel (Float.TYPE, (18 + 1) * 32);
        {
            cs [0] = 0.857492925712f;
            cs [1] = 0.881741997318f;
            cs [2] = 0.949628649103f;
            cs [3] = 0.983314592492f;
            cs [4] = 0.995517816065f;
            cs [5] = 0.999160558175f;
            cs [6] = 0.999899195243f;
            cs [7] = 0.999993155067f;
        }
            
        {
            ca [0] = -0.5144957554270f;
            ca [1] = -0.4717319685650f;
            ca [2] = -0.3133774542040f;
            ca [3] = -0.1819131996110f;
            ca [4] = -0.0945741925262f;
            ca [5] = -0.0409655828852f;
            ca [6] = -0.0141985685725f;
            ca [7] = -0.00369997467375f;
        }
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
		    //ERROR ("invalid type");
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
    private float cs[] = new float [8];

    private float ca[] = new float [8];
}
