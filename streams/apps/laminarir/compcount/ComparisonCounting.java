import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class RandomSource extends Filter // ComparisonCounting.str:10
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
  int seed; // ComparisonCounting.str:11
  public void work() { // ComparisonCounting.str:12
    outputChannel.pushInt(seed); // ComparisonCounting.str:13
    seed = (((65793 * seed) + 4282663) % 8388608); // ComparisonCounting.str:14
  }
  public void init() { // ComparisonCounting.str:10
    seed = 0; // ComparisonCounting.str:11
    setIOTypes(Void.TYPE, Integer.TYPE); // ComparisonCounting.str:10
    addSteadyPhase(0, 0, 1, "work"); // ComparisonCounting.str:12
  }
}
class IntSource extends Filter // ComparisonCounting.str:18
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
  int[] data; // ComparisonCounting.str:19
  int index; // ComparisonCounting.str:20
  int SIZE; // ComparisonCounting.str:18
  public void work() { // ComparisonCounting.str:30
    outputChannel.pushInt(data[index++]); // ComparisonCounting.str:31
    if ((index == SIZE)) { // ComparisonCounting.str:33
      index = 0; // ComparisonCounting.str:33
    } // ComparisonCounting.str:32
  }
  public void init(final int _param_SIZE) { // ComparisonCounting.str:21
    SIZE = _param_SIZE; // ComparisonCounting.str:21
    data = new int[SIZE]; // ComparisonCounting.str:19
    index = 0; // ComparisonCounting.str:20
    setIOTypes(Void.TYPE, Integer.TYPE); // ComparisonCounting.str:18
    addSteadyPhase(0, 0, 1, "work"); // ComparisonCounting.str:30
    data[0] = 503; // ComparisonCounting.str:22
    data[1] = 87; // ComparisonCounting.str:22
    data[2] = 512; // ComparisonCounting.str:22
    data[3] = 61; // ComparisonCounting.str:23
    data[4] = 908; // ComparisonCounting.str:23
    data[5] = 170; // ComparisonCounting.str:23
    data[6] = 897; // ComparisonCounting.str:24
    data[7] = 275; // ComparisonCounting.str:24
    data[8] = 653; // ComparisonCounting.str:24
    data[9] = 426; // ComparisonCounting.str:25
    data[10] = 154; // ComparisonCounting.str:25
    data[11] = 509; // ComparisonCounting.str:25
    data[12] = 612; // ComparisonCounting.str:26
    data[13] = 677; // ComparisonCounting.str:26
    data[14] = 765; // ComparisonCounting.str:26
    data[15] = 703; // ComparisonCounting.str:27
  }
}
class CountSortSplit extends SplitJoin // ComparisonCounting.str:37
{
  private int __param_SIZE;
  public static CountSortSplit __construct(int SIZE)
  {
    CountSortSplit __obj = new CountSortSplit();
    __obj.__param_SIZE = SIZE;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_SIZE);
  }
  public void init(final int SIZE) { // ComparisonCounting.str:37
    setSplitter(DUPLICATE()); // ComparisonCounting.str:38
    add(CountSplit.__construct(SIZE)); // ComparisonCounting.str:39
    add(new Identity(Integer.TYPE)); // ComparisonCounting.str:40
    setJoiner(ROUND_ROBIN(SIZE)); // ComparisonCounting.str:41
  }
}
class CountSplit extends SplitJoin // ComparisonCounting.str:44
{
  private int __param_SIZE;
  public static CountSplit __construct(int SIZE)
  {
    CountSplit __obj = new CountSplit();
    __obj.__param_SIZE = SIZE;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_SIZE);
  }
  public void init(final int SIZE) { // ComparisonCounting.str:44
    setSplitter(DUPLICATE()); // ComparisonCounting.str:45
    for (int i = 0; (i < SIZE); i++) { // ComparisonCounting.str:47
      add(Counter.__construct(i, SIZE)); // ComparisonCounting.str:47
    }; // ComparisonCounting.str:46
    setJoiner(ROUND_ROBIN(1)); // ComparisonCounting.str:48
  }
}
class Counter extends Filter // ComparisonCounting.str:51
{
  private int __param__param_index;
  private int __param__param_SIZE;
  public static Counter __construct(int _param_index, int _param_SIZE)
  {
    Counter __obj = new Counter();
    __obj.__param__param_index = _param_index;
    __obj.__param__param_SIZE = _param_SIZE;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_index, __param__param_SIZE);
  }
  int index; // ComparisonCounting.str:51
  int SIZE; // ComparisonCounting.str:51
  public void work() { // ComparisonCounting.str:52
    int mine = inputChannel.peekInt(index); // ComparisonCounting.str:53
    int count = 0; // ComparisonCounting.str:54
    for (int i = 0; (i < SIZE); i++) { // ComparisonCounting.str:55
      if ((inputChannel.peekInt(i) < mine)) { // ComparisonCounting.str:56
        count = (count + 1); // ComparisonCounting.str:57
      } // ComparisonCounting.str:56
    }; // ComparisonCounting.str:55
    for (int i = 0; (i < SIZE); i++) { // ComparisonCounting.str:60
      inputChannel.popInt(); // ComparisonCounting.str:61
    }; // ComparisonCounting.str:60
    outputChannel.pushInt(count); // ComparisonCounting.str:63
  }
  public void init(final int _param_index, final int _param_SIZE) { // ComparisonCounting.str:51
    index = _param_index; // ComparisonCounting.str:51
    SIZE = _param_SIZE; // ComparisonCounting.str:51
    setIOTypes(Integer.TYPE, Integer.TYPE); // ComparisonCounting.str:51
    addSteadyPhase(SIZE, SIZE, 1, "work"); // ComparisonCounting.str:52
  }
}
class RearrangeIndices extends Filter // ComparisonCounting.str:67
{
  private int __param__param_SIZE;
  public static RearrangeIndices __construct(int _param_SIZE)
  {
    RearrangeIndices __obj = new RearrangeIndices();
    __obj.__param__param_SIZE = _param_SIZE;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_SIZE);
  }
  int SIZE; // ComparisonCounting.str:67
  public void work() { // ComparisonCounting.str:68
    int[] outputArray = new int[(2 * SIZE)]; // ComparisonCounting.str:69
    for (int i = 0; (i < SIZE); i++) { // ComparisonCounting.str:71
      outputArray[inputChannel.peekInt(i)] = inputChannel.peekInt((i + SIZE)); // ComparisonCounting.str:71
    }; // ComparisonCounting.str:70
    for (int i = 0; (i < SIZE); i++) { // ComparisonCounting.str:72
      outputChannel.pushInt(outputArray[i]); // ComparisonCounting.str:73
      inputChannel.popInt(); // ComparisonCounting.str:74
      inputChannel.popInt(); // ComparisonCounting.str:74
    }; // ComparisonCounting.str:72
  }
  public void init(final int _param_SIZE) { // ComparisonCounting.str:67
    SIZE = _param_SIZE; // ComparisonCounting.str:67
    setIOTypes(Integer.TYPE, Integer.TYPE); // ComparisonCounting.str:67
    addSteadyPhase((SIZE * 2), (SIZE * 2), SIZE, "work"); // ComparisonCounting.str:68
  }
}
class IntPrinter extends Filter // ComparisonCounting.str:79
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
  int x; // ComparisonCounting.str:80
  public void work() { // ComparisonCounting.str:81
    x = inputChannel.popInt(); // ComparisonCounting.str:82
    System.out.println(x); // ComparisonCounting.str:83
  }
  public void init() { // ComparisonCounting.str:79
    setIOTypes(Integer.TYPE, Void.TYPE); // ComparisonCounting.str:79
    addSteadyPhase(1, 1, 0, "work"); // ComparisonCounting.str:81
  }
}
public class ComparisonCounting extends StreamItPipeline // ComparisonCounting.str:1
{
  public void init() { // ComparisonCounting.str:1
    int SIZE = 16; // ComparisonCounting.str:2
    add(RandomSource.__construct()); // ComparisonCounting.str:4
    add(CountSortSplit.__construct(SIZE)); // ComparisonCounting.str:5
    add(RearrangeIndices.__construct(SIZE)); // ComparisonCounting.str:6
    add(IntPrinter.__construct()); // ComparisonCounting.str:7
  }
  public static void main(String[] args) {
    ComparisonCounting program = new ComparisonCounting();
    program.run(args);
    FileWriter.closeAll();
  }
}
