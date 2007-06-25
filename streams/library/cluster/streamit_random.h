/**
 * This random number generator is based on GNU Classpath's Random
 * class.  The same function is implemented in the StreamIt Java
 * library to make the library and compiler output consistent.
 */

#ifndef __STREAMIT_RANDOM
#define __STREAMIT_RANDOM

// start with arbitrary seed that is shared between Java and C++
// libraries
static long long int streamit_seed = 1182736490827LL;

/**
 * Return the given number of random bits.
 */
static int streamit_random_bits (int bits) {
    streamit_seed = (streamit_seed * 0x5DEECE66DLL + 0xBLL) & ((1LL << 48) - 1);
    // convert to unsigned for the shift, since c++ does not
    // support unsigned shift right
    unsigned long long int unsigned_seed = (unsigned long long int)streamit_seed;
    unsigned_seed = unsigned_seed >> (48 - bits);
    // convert back to signed
    return (long long int)unsigned_seed;
};

/**
 * Set the random seed.
 */
static void streamit_set_seed (long long int _seed) {
    streamit_seed = (_seed * 0x5DEECE66DLL + 0xBLL) & ((1LL << 48) - 1);
}

/**
 * Return random 32-bit int.
 */
static int streamit_random_int() {
    return streamit_random_bits(32);
}

/**
 * Return random double.
 */
static double streamit_random_double() {
    return (((long long int) streamit_random_bits(26) << 27) + streamit_random_bits(27)) / (double) (1LL << 53);
} 

#endif
