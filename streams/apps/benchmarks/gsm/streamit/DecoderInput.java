// This is what used to be DecoderInput--now it will need to be
// inlined into every class that was using it.

//class DecoderInput
//{

//member variables!
public short[] mLarParameters = new short[8];
public short[] mLtpOffset = new short[4];
public short[] mLtpGain = new short[4];
public short[] mRpeGridPosition = new short[4];
public short[] mRpeMagnitude = new short[4];
public short[][] mSequence = new short[4][13];

public void getParameters(short[] input)
{
    int input_index = 0;
    int num_bits = 0;
    for(int i = 0; i < 8; i++)
	{
	
	    switch(i)
		{
		case 0:
		case 1:      num_bits = 6;
		    break;
		case 2:      
		case 3:      num_bits = 5;
		    break;
		case 4:      
		case 5:      num_bits = 4;
		    break;
		case 6:      
		case 7:      num_bits = 3;
		    break;
		}
	
	  
	    mLarParameters[i] = 0;
	    for (int j = 0; j < num_bits; j++, input_index++)
		{
		    mLarParameters[i] |= input[input_index] << (num_bits - 1 - i);
		}
	}
    
    //Sub-frames 1 through 4!
    for (int k = 0; k < 4; k++)
	{
	    mLtpOffset[k] = 0;
	    for (int l = 0; l < 7; l++)
		{
		    mLtpOffset[k] |= input[input_index] << (6 - l);
		    input_index++;
		}
	    mLtpGain[k] = 0;
	    for (int l = 0; l < 2; l++)
		{
		    mLtpGain[k] |= input[input_index] << (1 - l);
		    input_index++;
		}
	    mRpeGridPosition[k] = 0;
	    for (int l = 0; l < 2; l++)
		{
		    mRpeGridPosition[k] |= input[input_index] << (1 - l);
		    input_index++;
		}
	    mRpeMagnitude[k] = 0;
	    for (int l = 0; l < 6; l++)
		{
		    mRpeMagnitude[k] |= input[input_index] << (5 - l);
		    input_index++;
		}
	    for(int l = 0; l < 13; l++)
		{
		    mSequence[k][l] = 0;
		    for (int m = 0; m < 3; m++)
			{
			    mSequence[k][l] |= input[input_index] << (2 - m);
			    input_index++;
			}
	    
		}
	}
    //System.out.println(input_index);
    //System.out.println(input.length - input_index);
    
}

//}


