import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class RandomSource extends Filter // RadixSort.str:10
{
  public static RandomSource __construct()
  {
    RandomSource __obj = new RandomSource();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  int seed; // RadixSort.str:11
  public void work() { // RadixSort.str:12
    outputChannel.pushInt(seed); // RadixSort.str:13
    seed = (((65793 * seed) + 4282663) % 8388608); // RadixSort.str:14
  }
  public void init() { // RadixSort.str:10
    seed = 0; // RadixSort.str:11
    setIOTypes(Void.TYPE, Integer.TYPE); // RadixSort.str:10
    addSteadyPhase(0, 0, 1, "work"); // RadixSort.str:12
  }
}
class SortPipe extends Pipeline // RadixSort.str:18
{
  private int __param_SIZE;
  private int __param_MAX_VALUE;
  public static SortPipe __construct(int SIZE, int MAX_VALUE)
  {
    SortPipe __obj = new SortPipe();
    __obj.__param_SIZE = SIZE;
    __obj.__param_MAX_VALUE = MAX_VALUE;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_SIZE, __param_MAX_VALUE);
  }
  public void init(final int SIZE, final int MAX_VALUE) { // RadixSort.str:18
    for (int i = 1; (i < MAX_VALUE); i = (i * 2)) { // RadixSort.str:19
      add(Sort.__construct(SIZE, i)); // RadixSort.str:20
    }; // RadixSort.str:19
  }
}
class Sort extends Filter // RadixSort.str:24
{
  private int __param__param_SIZE;
  private int __param__param_radix;
  public static Sort __construct(int _param_SIZE, int _param_radix)
  {
    Sort __obj = new Sort();
    __obj.__param__param_SIZE = _param_SIZE;
    __obj.__param__param_radix = _param_radix;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_SIZE, __param__param_radix);
  }
  int SIZE; // RadixSort.str:24
  int radix; // RadixSort.str:24
  public void work() { // RadixSort.str:25
    int[] ordering = new int[SIZE]; // RadixSort.str:26
    int j = 0; // RadixSort.str:28
    for (int i = 0; (i < SIZE); i++) { // RadixSort.str:29
      int current; // RadixSort.str:30
      current = inputChannel.popInt(); // RadixSort.str:30
      if (((current & radix) == 0)) { // RadixSort.str:31
        outputChannel.pushInt(current); // RadixSort.str:32
      } else { // RadixSort.str:33
        ordering[j] = current; // RadixSort.str:34
        j = (j + 1); // RadixSort.str:35
      } // RadixSort.str:31
    }; // RadixSort.str:29
    for (int i = 0; (i < j); i++) { // RadixSort.str:38
      outputChannel.pushInt(ordering[i]); // RadixSort.str:39
    }; // RadixSort.str:38
  }
  public void init(final int _param_SIZE, final int _param_radix) { // RadixSort.str:24
    SIZE = _param_SIZE; // RadixSort.str:24
    radix = _param_radix; // RadixSort.str:24
    setIOTypes(Integer.TYPE, Integer.TYPE); // RadixSort.str:24
    addSteadyPhase(SIZE, SIZE, SIZE, "work"); // RadixSort.str:25
  }
}
class IntSource extends Filter // RadixSort.str:48
{
  private int __param__param_SIZE;
  public static IntSource __construct(int _param_SIZE)
  {
    IntSource __obj = new IntSource();
    __obj.__param__param_SIZE = _param_SIZE;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_SIZE);
  }
  int[] data; // RadixSort.str:49
  int index; // RadixSort.str:50
  int SIZE; // RadixSort.str:48
  public void work() { // RadixSort.str:60
    outputChannel.pushInt(data[index++]); // RadixSort.str:61
    if ((index == SIZE)) { // RadixSort.str:63
      index = 0; // RadixSort.str:63
    } // RadixSort.str:62
  }
  public void init(final int _param_SIZE) { // RadixSort.str:51
    SIZE = _param_SIZE; // RadixSort.str:51
    data = new int[SIZE]; // RadixSort.str:49
    index = 0; // RadixSort.str:50
    setIOTypes(Void.TYPE, Integer.TYPE); // RadixSort.str:48
    addSteadyPhase(0, 0, 1, "work"); // RadixSort.str:60
    data[0] = 503; // RadixSort.str:52
    data[1] = 87; // RadixSort.str:52
    data[2] = 512; // RadixSort.str:52
    data[3] = 61; // RadixSort.str:53
    data[4] = 908; // RadixSort.str:53
    data[5] = 170; // RadixSort.str:53
    data[6] = 897; // RadixSort.str:54
    data[7] = 275; // RadixSort.str:54
    data[8] = 653; // RadixSort.str:54
    data[9] = 426; // RadixSort.str:55
    data[10] = 154; // RadixSort.str:55
    data[11] = 509; // RadixSort.str:55
    data[12] = 612; // RadixSort.str:56
    data[13] = 677; // RadixSort.str:56
    data[14] = 765; // RadixSort.str:56
    data[15] = 703; // RadixSort.str:57
  }
}
class IntPrinter extends Filter // RadixSort.str:69
{
  public static IntPrinter __construct()
  {
    IntPrinter __obj = new IntPrinter();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  int x; // RadixSort.str:70
  public void work() { // RadixSort.str:71
    x = inputChannel.popInt(); // RadixSort.str:72
    System.out.println(x); // RadixSort.str:73
  }
  public void init() { // RadixSort.str:69
    setIOTypes(Integer.TYPE, Void.TYPE); // RadixSort.str:69
    addSteadyPhase(1, 1, 0, "work"); // RadixSort.str:71
  }
}
public class RadixSort extends StreamItPipeline // RadixSort.str:1
{
  public void init() { // RadixSort.str:1
    int SIZE = 16; // RadixSort.str:2
    int MAX_VALUE = 2048; // RadixSort.str:3
    add(RandomSource.__construct()); // RadixSort.str:5
    add(SortPipe.__construct(SIZE, MAX_VALUE)); // RadixSort.str:6
    add(IntPrinter.__construct()); // RadixSort.str:7
  }
  public static void main(String[] args) {
    RadixSort program = new RadixSort();
    program.run(args);
    FileWriter.closeAll();
  }
}
