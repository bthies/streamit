// This is the definition of the helper routines for inlining in Gsm

//class Helper
//{
short shortify(int a)
{
    if (a >= 32767)
	{
	    return 32767;
	}
    else
	{
	    if (a <= -32768)
		{
		    return -32768;
		}
	    else
		{
		    return (short) a;
		}
	}
}
    
short gsm_add(short a, short b)

{
    long ltmp = (long) a + (long) b;
    if (ltmp >= 32767)
	{
	    return 32767;
	}
    else
	{
	    if (ltmp <= -32768)
		{
		    return -32768;
		}
	    else
		{
		    return (short) ltmp;
		}
	}
}

short gsm_sub(short a, short b)
{
    long ltmp = (long) a - (long) b;
    if (ltmp >= 32767)
	{
	    return 32767;
	}
    else
	{
	    if (ltmp <= -32768)
		{
		    return -32768;
		}
	    else
		{
		    return (short) ltmp;
		}
	}
}

short gsm_mult(short a, short b)
{
    long temp = (long) a * (long) b >> 15;
    if (temp >= 32767)
	{
	    return 32767;
	}
    else
	{
	    if (temp <= -32768)
		{
		    return -32768;
		}
	    else
		{
		    return (short) temp;
		}
	}       
}

short gsm_mult_r(short a, short b)
{
    long temp = ((long) a * (long) b) + 16384;
    short answer = (short) (temp >> 15);
    return answer;
}

short gsm_abs(short a)
{
    short answer;
    if (a < 0)
	{
	    if (a == -32768)
		{
		    answer = 32767;
		}
	    else
		{
		    int temp = a * -1;
		    if (temp >= 32767)
			{
			    answer = 32767;
			}
		    else
			{
			    if (temp <= -32768)
				{
				    answer = -32768;
				}
			    else
				{
				    answer = (short) temp;
				}
			}
		}
	}
    else
	{
	    answer = a;
	}
    return answer;
}
//}






