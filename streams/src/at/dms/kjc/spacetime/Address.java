package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.math.BigInteger;

public class Address
{
    public static Address MAX_ADDRESS;
    public static Address ZERO;
    private long address;

    static 
    {
	MAX_ADDRESS = new Address(Long.valueOf("4294967296", 10).longValue());
	ZERO = new Address(0);
    }
 
    public String toString() 
    {
	return Address.toHex(address);
    }

    private Address(long Bint) 
    {
	address = Bint;
    }
    
    public static String toHex(long addr) 
    {
	//if zero just return zero
	if (addr == 0)
	    return "0x0";

	String val = "";
	byte[] bytes = (new BigInteger(Long.toString(addr), 10)).toByteArray();
	for (int i = bytes.length - 1; i >=0; i--) {
	    int x = bytes[i] & 0xFF;
	    String cur = Integer.toHexString(x);
	    //expand 0 to 00
	    if (cur.equals("0"))
		cur = "00";
	    
	    val = cur + val;
	}
	
	//remove leading zeros
	int zeros = 0;
	for (int i = 0; i < val.length(); i++) {
	    if (val.charAt(i) == '0')
		zeros++;
	    else 
		break;
	}
	val = val.substring(zeros);
	//return value
	return "0x" + val;
    }

    public Address add(Address i) 
    {
	return new Address(this.address + i.address);
    }
    
    public Address add(int i) 
    {
	return new Address((long)i + this.address);
    }

    public Address add32Byte(int i) 
    {
	long result = address + (long)i;
	//find what we have to add to the result to it divisible by 32
	result += (32 - (result % 32));
	return new Address(result);
    }

    public Address ceil32() 
    {
	return this.add32Byte(0);
    }
    
    
    public Address div(int i) 
    {
	if ((address % (long)i) != 0)
	    Utils.fail("Remainder of Address divide not zero");
	return new Address(address / (long)i);	
    }

    public Address mult(int i) 
    {
	return new Address((long)i * this.address);
	
    }
    
    public static boolean inRange(Address addr, Address lb, Address ub) 
    {
	if (addr.address >= lb.address && addr.address < ub.address)
	    return true;
	return false;
    }
    
    /*
    public boolean lt(Address comp) 
    {
	if (comp.address < this.address)
	    return true;
	return false;
    }

    public boolean gte(Address comp) 
    {
	if (comp.address >= this.address)
	    return true;
	return false;
    }
    */
}

