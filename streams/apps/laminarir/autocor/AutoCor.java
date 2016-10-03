import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class RandomSource extends Filter // AutoCor.str:8
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
  int seed; // AutoCor.str:9
  public void work() { // AutoCor.str:10
    outputChannel.pushFloat(seed); // AutoCor.str:11
    seed = (((65793 * seed) + 4282663) % 8388608); // AutoCor.str:12
  }
  public void init() { // AutoCor.str:8
    seed = 0; // AutoCor.str:9
    setIOTypes(Void.TYPE, Float.TYPE); // AutoCor.str:8
    addSteadyPhase(0, 0, 1, "work"); // AutoCor.str:10
  }
}
class Cor1 extends SplitJoin // AutoCor.str:19
{
  private int __param_N;
  private int __param_NLAGS;
  public static Cor1 __construct(int N, int NLAGS)
  {
    Cor1 __obj = new Cor1();
    __obj.__param_N = N;
    __obj.__param_NLAGS = NLAGS;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_N, __param_NLAGS);
  }
  public void init(final int N, final int NLAGS) { // AutoCor.str:19
    setSplitter(DUPLICATE()); // AutoCor.str:20
    for (int lag = 0; (lag < NLAGS); lag++) { // AutoCor.str:21
      add(AnonFilter_a0.__construct(N, lag)); // AutoCor.str:23
    }; // AutoCor.str:21
    setJoiner(ROUND_ROBIN(1)); // AutoCor.str:34
  }
}
class OneSource extends Filter // AutoCor.str:37
{
  public static OneSource __construct()
  {
    OneSource __obj = new OneSource();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  float n; // AutoCor.str:38
  public void work() { // AutoCor.str:40
    outputChannel.pushFloat(n); // AutoCor.str:40
    n += 0.01f; // AutoCor.str:40
  }
  public void init() { // AutoCor.str:39
    setIOTypes(Void.TYPE, Float.TYPE); // AutoCor.str:37
    addSteadyPhase(0, 0, 1, "work"); // AutoCor.str:40
    n = 0; // AutoCor.str:39
  }
}
class FloatPrinter extends Filter // AutoCor.str:43
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
  float x; // AutoCor.str:44
  public void work() { // AutoCor.str:45
    x = inputChannel.popFloat(); // AutoCor.str:46
    System.out.println(x); // AutoCor.str:47
  }
  public void init() { // AutoCor.str:43
    setIOTypes(Float.TYPE, Void.TYPE); // AutoCor.str:43
    addSteadyPhase(1, 1, 0, "work"); // AutoCor.str:45
  }
}
class AnonFilter_a0 extends Filter // AutoCor.str:23
{
  private int __param__param_N;
  private int __param__param_lag;
  public static AnonFilter_a0 __construct(int _param_N, int _param_lag)
  {
    AnonFilter_a0 __obj = new AnonFilter_a0();
    __obj.__param__param_N = _param_N;
    __obj.__param__param_lag = _param_lag;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_N, __param__param_lag);
  }
  int N; // AutoCor.str:23
  int lag; // AutoCor.str:23
  public void work() { // AutoCor.str:24
    float sum = 0; // AutoCor.str:25
    for (int i = 0; (i < (N - lag)); i++) { // AutoCor.str:27
      sum += (inputChannel.peekFloat(i) * inputChannel.peekFloat((i + lag))); // AutoCor.str:27
    }; // AutoCor.str:26
    for (int i = 0; (i < N); i++) { // AutoCor.str:29
      inputChannel.popFloat(); // AutoCor.str:29
    }; // AutoCor.str:28
    outputChannel.pushFloat(sum); // AutoCor.str:30
  }
  public void init(final int _param_N, final int _param_lag) { // AutoCor.str:23
    N = _param_N; // AutoCor.str:23
    lag = _param_lag; // AutoCor.str:23
    setIOTypes(Float.TYPE, Float.TYPE); // AutoCor.str:23
    addSteadyPhase(N, N, 1, "work"); // AutoCor.str:24
  }
}
public class AutoCor extends StreamItPipeline // AutoCor.str:1
{
  public void init() { // AutoCor.str:1
    add(RandomSource.__construct()); // AutoCor.str:3
    add(Cor1.__construct(32, 128)); // AutoCor.str:4
    add(FloatPrinter.__construct()); // AutoCor.str:5
  }
  public static void main(String[] args) {
    AutoCor program = new AutoCor();
    program.run(args);
    FileWriter.closeAll();
  }
}
