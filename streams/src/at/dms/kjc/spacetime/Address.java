package at.dms.kjc.spacetime;

import at.dms.util.Utils;
import java.math.BigInteger;

/**
 * This class represents an unsigned 32-bit address in the 
 * SpaceTime backend.  It has functions for adding, subtracting,
 * aligning, and printing unsigned 32-bit values. 
 * 
 * Conceptually, it is used to represent a 32-bit address for memory 
 * that is byte-addressable.  
 * 
 * @author mgordon
 *
 */
public class Address
{
    public static Address MAX_ADDRESS;
    public static Address ZERO;
    private long address;

    static 
    {
        //initialize the max 32-bit value
        MAX_ADDRESS = new Address(Long.valueOf("4294967296", 10).longValue());
        //initialize zero
        ZERO = new Address(0);
    }
 
    /**
     * Return the hexadecimal representation of the unsigned int.
     * @see Address#toHex
     * 
     * @return the hexadecimal representation of the unsigned int.
     */
    public String toString() 
    {
        return Address.toHex(address);
    }

    /**
     * The constructor for Address.  One cannot create an address
     * from outside of the Address class.  They must use Addresses 
     * that are already present, like zero and max.
     * 
     * @param Bint the integer value
     */
    private Address(long Bint) 
    {
        address = Bint;
    }
    
    /**
     * Return the hexadecimal string that represents this 32-bit 
     * unsigned value addr.
     * 
     * @param addr The 32-bit unsigned value.
     * @return The hexadecimal string for addr.
     */
    public static String toHex(long addr) 
    {
        //if zero just return zero
        if (addr == 0)
            return "0x0";
        //convert to a byte array and convert each byte to hex...
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

    /**
     * Add the value of i to this address and return a new address with 
     * this value.
     * 
     * @param i The value to add to this address.
     * @return A new address with i + this.
     */
    public Address add(Address i) 
    {
        return new Address(this.address + i.address);
    }
    
    /**
     * Add the value of i to this address and return a new address with 
     * this value.
     * 
     * @param i The value to add to this address.
     * @return A new address with i + this.
     */
    public Address add(int i) 
    {
        return new Address((long)i + this.address);
    }

    /**
     * Add the value of i to this address and return a new address with 
     * this value, round up the value to the next 32-byte boundary.
     * 
     * @param i The value to add to this address.
     * @return A new address with i + this.
     */
    public Address add32Byte(int i) 
    {
        long result = address + (long)i;
        //find what we have to add to the result to it divisible by 32
        result += (32 - (result % 32));
        return new Address(result);
    }

    /**
     * Return a new Address whose value is the next 32-byte aligned 
     * Address greater than this.
     * 
     * @return a new Address whose value is the next 32-byte aligned 
     * Address greater than this.
     */
    public Address ceil32() 
    {
        return this.add32Byte(0);
    }
    
    /**
     * Return a new Address whose value is this / i.
     * @param i The divisor.
     * @return a new Address whose value is this / i.
     */
    public Address div(int i) 
    {
        if ((address % (long)i) != 0)
            Utils.fail("Remainder of Address divide not zero");
        return new Address(address / (long)i);  
    }

    /**
     * Return a new Address whose value is this * i.
     * @param i The multiplier
     * @return a new Address whose value is this * i.
     */
    public Address mult(int i) 
    {
        return new Address((long)i * this.address);
    
    }
    
    /**
     * Return true if addr is within the range [lb, ub].
     *  
     * @param addr The address to test.
     * @param lb The lower bound.
     * @param ub The upper bound.
     * @return true if addr is within the range [lb, ub].
     */
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

