/* This is a custom random-number generator used for StreamIt
 * programs.  We have re-implemented this generator in the C library
 * to enable results to match betwen the library and the compiler.
 *
 * The random number generator is borrowed from GNU Classpath's
 * implementation of Random.
 */

package streamit.misc;

public class StreamItRandom {

    // start with arbitrary seed that is shared between Java and C++
    // libraries
    private static long seed = 1182736490827L;

    /**
     * Set the random seed.
     */
    public static void setSeed (long _seed) {
        seed = (_seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
    }

    /**
     * Return given number of random bits.
     */
    private static int nextBits(int bits) {
        seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1);
        return (int) (seed >>> (48 - bits));
    }

    /**
     * Return random int.
     */
    public static int randomInt() {
        return nextBits(32);
    }

    /**
     * Return random double.
     */
    public static double randomDouble() {
        System.out.println("seed = " + seed + " (" + (seed % 100) + ")");
        return (((long) nextBits(26) << 27) + nextBits(27)) / (double) (1L << 53);
    }
}
