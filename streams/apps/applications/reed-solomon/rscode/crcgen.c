/*****************************
 * 
 * CRC-CCITT generator simulator for byte wide data.
 *
 *
 * CRC-CCITT = x^16 + x^12 + x^5 + 1
 *
 *
 * Henry Minsky (1991)
 * 
 * CRC code lifted from "C Programmer's Guide To Serial Communication" 
 * by Joe Campbell
 *
 ******************************/
 
 
#include "ecc.h"

BIT16 crchware(BIT16 data, BIT16 genpoly, BIT16 accum);

/* Computes the CRC-CCITT checksum on array of byte data, length len
*/
BIT16 crc_ccitt(unsigned char *msg, int len)
{
	int i;
	BIT16 acc = 0;

	for (i = 0; i < len; i++) {
		acc = crchware((BIT16) msg[i], (BIT16) 0x1021, acc);
	}
	
	return(acc);
}
	
/* models crc hardware (minor variation on polynomial division algorithm) */
BIT16 crchware(BIT16 data, BIT16 genpoly, BIT16 accum)
{
	static BIT16 i;
	data <<= 8;
	for (i = 8; i > 0; i--) {
		if ((data ^ accum) & 0x8000)
			accum = ((accum << 1) ^ genpoly) & 0xFFFF;
		else
			accum = (accum<<1) & 0xFFFF;
		data = (data<<1) & 0xFFFF;
	}
	return (accum);
}
		
