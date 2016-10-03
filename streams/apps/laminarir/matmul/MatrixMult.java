import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class RandomSource extends Filter // MatrixMult.str:8
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
  int seed; // MatrixMult.str:9
  public void work() { // MatrixMult.str:10
    outputChannel.pushFloat(seed); // MatrixMult.str:11
    seed = (((65793 * seed) + 4282663) % 8388608); // MatrixMult.str:12
  }
  public void init() { // MatrixMult.str:8
    seed = 0; // MatrixMult.str:9
    setIOTypes(Void.TYPE, Float.TYPE); // MatrixMult.str:8
    addSteadyPhase(0, 0, 1, "work"); // MatrixMult.str:10
  }
}
class MatrixMultiply extends Pipeline // MatrixMult.str:16
{
  private int __param_x0;
  private int __param_y0;
  private int __param_x1;
  private int __param_y1;
  public static MatrixMultiply __construct(int x0, int y0, int x1, int y1)
  {
    MatrixMultiply __obj = new MatrixMultiply();
    __obj.__param_x0 = x0;
    __obj.__param_y0 = y0;
    __obj.__param_x1 = x1;
    __obj.__param_y1 = y1;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_x0, __param_y0, __param_x1, __param_y1);
  }
  public void init(final int x0, final int y0, final int x1, final int y1) { // MatrixMult.str:16
    add(RearrangeDuplicateBoth.__construct(x0, y0, x1, y1)); // MatrixMult.str:18
    add(MultiplyAccumulateParallel.__construct(x0, x0)); // MatrixMult.str:19
  }
}
class RearrangeDuplicateBoth extends SplitJoin // MatrixMult.str:22
{
  private int __param_x0;
  private int __param_y0;
  private int __param_x1;
  private int __param_y1;
  public static RearrangeDuplicateBoth __construct(int x0, int y0, int x1, int y1)
  {
    RearrangeDuplicateBoth __obj = new RearrangeDuplicateBoth();
    __obj.__param_x0 = x0;
    __obj.__param_y0 = y0;
    __obj.__param_x1 = x1;
    __obj.__param_y1 = y1;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_x0, __param_y0, __param_x1, __param_y1);
  }
  public void init(final int x0, final int y0, final int x1, final int y1) { // MatrixMult.str:23
    setSplitter(WEIGHTED_ROUND_ROBIN((x0 * y0), (x1 * y1))); // MatrixMult.str:24
    add(DuplicateRows.__construct(x1, x0)); // MatrixMult.str:26
    add(RearrangeDuplicate.__construct(x0, y0, x1, y1)); // MatrixMult.str:30
    setJoiner(ROUND_ROBIN(1)); // MatrixMult.str:31
  }
}
class RearrangeDuplicate extends Pipeline // MatrixMult.str:34
{
  private int __param_x0;
  private int __param_y0;
  private int __param_x1;
  private int __param_y1;
  public static RearrangeDuplicate __construct(int x0, int y0, int x1, int y1)
  {
    RearrangeDuplicate __obj = new RearrangeDuplicate();
    __obj.__param_x0 = x0;
    __obj.__param_y0 = y0;
    __obj.__param_x1 = x1;
    __obj.__param_y1 = y1;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_x0, __param_y0, __param_x1, __param_y1);
  }
  public void init(final int x0, final int y0, final int x1, final int y1) { // MatrixMult.str:34
    add(Transpose.__construct(x1, y1)); // MatrixMult.str:35
    add(DuplicateRows.__construct(y0, (x1 * y1))); // MatrixMult.str:36
  }
}
class Transpose extends SplitJoin // MatrixMult.str:39
{
  private int __param_x;
  private int __param_y;
  public static Transpose __construct(int x, int y)
  {
    Transpose __obj = new Transpose();
    __obj.__param_x = x;
    __obj.__param_y = y;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_x, __param_y);
  }
  public void init(final int x, final int y) { // MatrixMult.str:39
    setSplitter(ROUND_ROBIN(1)); // MatrixMult.str:40
    for (int i = 0; (i < x); i++) { // MatrixMult.str:41
      add(new Identity(Float.TYPE)); // MatrixMult.str:41
    }; // MatrixMult.str:41
    setJoiner(ROUND_ROBIN(y)); // MatrixMult.str:42
  }
}
class MultiplyAccumulateParallel extends SplitJoin // MatrixMult.str:45
{
  private int __param_x;
  private int __param_n;
  public static MultiplyAccumulateParallel __construct(int x, int n)
  {
    MultiplyAccumulateParallel __obj = new MultiplyAccumulateParallel();
    __obj.__param_x = x;
    __obj.__param_n = n;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_x, __param_n);
  }
  public void init(final int x, final int n) { // MatrixMult.str:45
    setSplitter(ROUND_ROBIN((x * 2))); // MatrixMult.str:46
    for (int i = 0; (i < n); i++) { // MatrixMult.str:47
      add(MultiplyAccumulate.__construct(x)); // MatrixMult.str:47
    }; // MatrixMult.str:47
    setJoiner(ROUND_ROBIN(1)); // MatrixMult.str:48
  }
}
class MultiplyAccumulate extends Filter // MatrixMult.str:51
{
  private int __param__param_rowLength;
  public static MultiplyAccumulate __construct(int _param_rowLength)
  {
    MultiplyAccumulate __obj = new MultiplyAccumulate();
    __obj.__param__param_rowLength = _param_rowLength;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_rowLength);
  }
  int rowLength; // MatrixMult.str:51
  public void work() { // MatrixMult.str:52
    float result = 0; // MatrixMult.str:53
    for (int x = 0; (x < rowLength); x++) { // MatrixMult.str:54
      result += (inputChannel.peekFloat(0) * inputChannel.peekFloat(1)); // MatrixMult.str:55
      inputChannel.popFloat(); // MatrixMult.str:56
      inputChannel.popFloat(); // MatrixMult.str:57
    }; // MatrixMult.str:54
    outputChannel.pushFloat(result); // MatrixMult.str:59
  }
  public void init(final int _param_rowLength) { // MatrixMult.str:51
    rowLength = _param_rowLength; // MatrixMult.str:51
    setIOTypes(Float.TYPE, Float.TYPE); // MatrixMult.str:51
    addSteadyPhase((rowLength * 2), (rowLength * 2), 1, "work"); // MatrixMult.str:52
  }
}
class DuplicateRows extends Pipeline // MatrixMult.str:63
{
  private int __param_x;
  private int __param_y;
  public static DuplicateRows __construct(int x, int y)
  {
    DuplicateRows __obj = new DuplicateRows();
    __obj.__param_x = x;
    __obj.__param_y = y;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_x, __param_y);
  }
  public void init(final int x, final int y) { // MatrixMult.str:63
    add(DuplicateRowsInternal.__construct(x, y)); // MatrixMult.str:64
  }
}
class DuplicateRowsInternal extends SplitJoin // MatrixMult.str:67
{
  private int __param_times;
  private int __param_length;
  public static DuplicateRowsInternal __construct(int times, int length)
  {
    DuplicateRowsInternal __obj = new DuplicateRowsInternal();
    __obj.__param_times = times;
    __obj.__param_length = length;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_times, __param_length);
  }
  public void init(final int times, final int length) { // MatrixMult.str:67
    setSplitter(DUPLICATE()); // MatrixMult.str:68
    for (int i = 0; (i < times); i++) { // MatrixMult.str:69
      add(new Identity(Float.TYPE)); // MatrixMult.str:69
    }; // MatrixMult.str:69
    setJoiner(ROUND_ROBIN(length)); // MatrixMult.str:70
  }
}
class FloatSource extends Filter // MatrixMult.str:73
{
  private float __param__param_maxNum;
  public static FloatSource __construct(float _param_maxNum)
  {
    FloatSource __obj = new FloatSource();
    __obj.__param__param_maxNum = _param_maxNum;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_maxNum);
  }
  float num; // MatrixMult.str:74
  float maxNum; // MatrixMult.str:73
  public void work() { // MatrixMult.str:78
    outputChannel.pushFloat(num); // MatrixMult.str:79
    num++; // MatrixMult.str:80
    if ((num == maxNum)) { // MatrixMult.str:81
      num = 0; // MatrixMult.str:81
    } // MatrixMult.str:81
  }
  public void init(final float _param_maxNum) { // MatrixMult.str:75
    maxNum = _param_maxNum; // MatrixMult.str:75
    setIOTypes(Void.TYPE, Float.TYPE); // MatrixMult.str:73
    addSteadyPhase(0, 0, 1, "work"); // MatrixMult.str:78
    num = 0; // MatrixMult.str:76
  }
}
class FloatPrinter extends Filter // MatrixMult.str:85
{
  public static FloatPrinter __construct()
  {
    FloatPrinter __obj = new FloatPrinter();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  float x; // MatrixMult.str:86
  public void work() { // MatrixMult.str:87
    x = inputChannel.popFloat(); // MatrixMult.str:88
    System.out.println(x); // MatrixMult.str:89
  }
  public void init() { // MatrixMult.str:85
    setIOTypes(Float.TYPE, Void.TYPE); // MatrixMult.str:85
    addSteadyPhase(1, 1, 0, "work"); // MatrixMult.str:87
  }
}
class Sink extends Filter // MatrixMult.str:93
{
  public static Sink __construct()
  {
    Sink __obj = new Sink();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  int x; // MatrixMult.str:94
  public void work() { // MatrixMult.str:98
    inputChannel.popFloat(); // MatrixMult.str:99
    x++; // MatrixMult.str:100
    if ((x == 100)) { // MatrixMult.str:101
      System.out.println("done.."); // MatrixMult.str:102
      x = 0; // MatrixMult.str:103
    } // MatrixMult.str:101
  }
  public void init() { // MatrixMult.str:95
    setIOTypes(Float.TYPE, Void.TYPE); // MatrixMult.str:93
    addSteadyPhase(1, 1, 0, "work"); // MatrixMult.str:98
    x = 0; // MatrixMult.str:96
  }
}
public class MatrixMult extends StreamItPipeline // MatrixMult.str:1
{
  public void init() { // MatrixMult.str:1
    add(RandomSource.__construct()); // MatrixMult.str:3
    add(MatrixMultiply.__construct(10, 10, 10, 10)); // MatrixMult.str:4
    add(FloatPrinter.__construct()); // MatrixMult.str:5
  }
  public static void main(String[] args) {
    MatrixMult program = new MatrixMult();
    program.run(args);
    FileWriter.closeAll();
  }
}
