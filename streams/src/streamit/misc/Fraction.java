package streamit.misc;

import java.util.StringTokenizer;
import java.math.BigInteger;

// from http://egghead.psu.edu/~herzog/applets/lin_equation/index.html
// replaced 'long' with BigInteger to allow for arbitrary precision rationals

/******************************************************************************/
public class Fraction        // encapsulates a fraction
/******************************************************************************/
{

    private BigInteger num;
    private BigInteger denom;

    public void set (BigInteger num, BigInteger denom)
    {
        if(denom.equals (BigInteger.ZERO))
            throw new ArithmeticException("Denominator in fraction cannot be zero: "+num+"/"+denom);

        int sign = denom.signum ();
        denom = denom.abs ();
        if(sign < 0)
            num = num.negate ();

        this.num = num;
        this.denom = denom;
    }

    public void set (long num, long denom)
    {
        set (BigInteger.valueOf (num), BigInteger.valueOf (denom));
    }

    /******************************************************************************/
    public Fraction(long num, long denom)            // constructor
    /******************************************************************************/
    {
        set (num, denom);
    }

    /******************************************************************************/
    public Fraction(BigInteger num, BigInteger denom)            // constructor
    /******************************************************************************/
    {
        set (num, denom);
    }

    /******************************************************************************/
    public Fraction(Fraction frac)            // constructor
    /******************************************************************************/
    {
        num = frac.num;
        denom = frac.denom;
    }


    /******************************************************************************/
    public Fraction(String strFrac)            // constructor
    /******************************************************************************/
    {
        int tokens = 0;
        String strTokens[] = new String[2];

        StringTokenizer tokenizer = new StringTokenizer(strFrac,"/");

        try{
            while(tokenizer.hasMoreTokens()){
                strTokens[tokens] = tokenizer.nextToken();
                tokens++;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new NumberFormatException(strFrac + " is not a valid fraction.");
        }

        if(tokens == 1){
            strTokens[1] = new String("1");
        }

        num = new BigInteger (strTokens[0]);
        denom = new BigInteger (strTokens[1]);

        if(denom.equals (BigInteger.ZERO))
            throw new ArithmeticException("Denominator in fraction cannot be zero: "+num.toString ()+"/"+denom.toString ());

        if (denom.signum () == -1)
        {
            num = num.negate ();
            denom = denom.negate ();
        }
    }


    /******************************************************************************/
    public BigInteger getNum()
    /******************************************************************************/
    {
       return num;
    }

    /******************************************************************************/
    public BigInteger getDenom()
    /******************************************************************************/
    {
       return denom;
    }


    /******************************************************************************/
    public Fraction reduce()   // returns a fraction which is this reduced to lowest form
    /******************************************************************************/
    {
        BigInteger sign = BigInteger.ONE;

        if (num.signum () == -1)
        {
            sign = BigInteger.valueOf (-1);
        }

        if(num.equals (denom))
        {
            return (new Fraction (sign, BigInteger.ONE));
        }

        else if (num.equals (BigInteger.ZERO))
        {
            return new Fraction (BigInteger.ZERO, BigInteger.ONE);
        }

        else {
            BigInteger gcd = num.gcd (denom);

            BigInteger newNum = num.divide (gcd);
            BigInteger newDenom = denom.divide (gcd);

            return new Fraction (newNum, newDenom);
        }
    } // end reduce()


    /******************************************************************************/
    public Fraction subtract(Fraction temp) // subtraction operator returns this - temp
    /******************************************************************************/
    {

        return add(new Fraction(temp.num.negate (), temp.denom));
    }


    /******************************************************************************/
    public Fraction add(Fraction temp) // addition operators returns this + temp
    /******************************************************************************/
    {
        BigInteger Dn = denom.multiply (temp.denom);
        BigInteger Nn1 = num.multiply (temp.denom);
        BigInteger Nn2 = temp.num.multiply (denom);

        BigInteger aNum = Nn1.add (Nn2);

        Fraction returnValue = new Fraction(aNum, Dn);

        return returnValue.reduce();

    }


    /******************************************************************************/
    public Fraction multiply(Fraction temp) // multiply returns this * temp
    /******************************************************************************/
    {
        Fraction returnValue = new Fraction(num.multiply (temp.num), denom.multiply (temp.denom));
        return returnValue.reduce();
    }

    /******************************************************************************/
    public Fraction multiply(BigInteger temp) // multiply returns this * temp
    /******************************************************************************/
    {
        Fraction returnValue = new Fraction(num.multiply (temp), denom);
        return returnValue.reduce();
    }

    /******************************************************************************/
    public Fraction divide(Fraction temp) // divide returns this / temp
    /******************************************************************************/
    {
        if(temp.num.equals (BigInteger.ZERO)) throw new ArithmeticException
                              ("Divide by zero error: " + this + " / " + temp );

        Fraction returnValue =  new Fraction( num.multiply (temp.denom), denom.multiply (temp.num));
        return returnValue.reduce();
    }

    /******************************************************************************/
    public Fraction divide(BigInteger temp) // divide returns this / temp
    /******************************************************************************/
    {
        if(temp.equals (BigInteger.ZERO)) throw new ArithmeticException
                              ("Divide by zero error: " + this + " / " + temp );

        Fraction returnValue =  new Fraction( num, denom.multiply (temp));
        return returnValue.reduce();
    }

    /******************************************************************************/
    public boolean equals(Fraction temp) // returns true or false
    /******************************************************************************/
    {
        if( compareTo(temp) == 0)
            return true;
        else
            return false;
    }

    /******************************************************************************/
    public int compareTo(Fraction temp) // returns -1 for this<temp, 0 for this == temp,
                                        // and 1 for this>temp
    /******************************************************************************/
    {
        Fraction test;

        test = subtract(temp);
        return test.num.signum ();
    }


    /******************************************************************************/
    public Fraction inverse() // returns the inverse of this
    /******************************************************************************/
    {
        return new Fraction(denom,num);
    }

    /******************************************************************************/
    public Fraction negate() // returns this * -1
    /******************************************************************************/
    {
        return new Fraction(num.negate (),denom);
    }


    /******************************************************************************/
    public BigInteger getNumerator()
    /******************************************************************************/
    {
        return num;
    }

    /******************************************************************************/
    public BigInteger getDenominator()
    /******************************************************************************/
    {
        return denom;
    }

    /******************************************************************************/
    public String toString()
    /******************************************************************************/
    {
        if(num.equals (BigInteger.ZERO))
            return "0";
        else if(denom.equals (BigInteger.ONE))
            return num.toString ();
        else if(denom.equals (num))
            return "1";
        else
            return new String(num.toString () + "/" + denom.toString ());
    }


}
/******************************************************************************/
/************************* END OF CLASS FRACTION ******************************/
/******************************************************************************/
