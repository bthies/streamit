import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class DelayOne extends Filter // Lattice.str:5
{
  public static DelayOne __construct()
  {
    DelayOne __obj = new DelayOne();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  float last; // Lattice.str:6
  public void work() { // Lattice.str:10
    outputChannel.pushFloat(last); // Lattice.str:11
    last = inputChannel.popFloat(); // Lattice.str:12
  }
  public void init() { // Lattice.str:7
    setIOTypes(Float.TYPE, Float.TYPE); // Lattice.str:5
    addSteadyPhase(1, 1, 1, "work"); // Lattice.str:10
    last = 0.0f; // Lattice.str:8
  }
}
class LatDel extends SplitJoin // Lattice.str:16
{
  public static LatDel __construct()
  {
    LatDel __obj = new LatDel();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void init() { // Lattice.str:16
    setSplitter(DUPLICATE()); // Lattice.str:17
    add(new Identity(Float.TYPE)); // Lattice.str:18
    add(DelayOne.__construct()); // Lattice.str:19
    setJoiner(ROUND_ROBIN(1)); // Lattice.str:20
  }
}
class LatFilt extends Filter // Lattice.str:24
{
  private float __param__param_k_par;
  public static LatFilt __construct(float _param_k_par)
  {
    LatFilt __obj = new LatFilt();
    __obj.__param__param_k_par = _param_k_par;
    return __obj;
  }
  protected void callInit()
  {
    init(__param__param_k_par);
  }
  float k_par; // Lattice.str:24
  public void work() { // Lattice.str:25
    float e_i = 0; // Lattice.str:26
    float e_bar_i = 0; // Lattice.str:27
    e_i = (inputChannel.peekFloat(0) - (k_par * inputChannel.peekFloat(1))); // Lattice.str:28
    e_bar_i = (inputChannel.peekFloat(1) - (k_par * inputChannel.peekFloat(0))); // Lattice.str:29
    outputChannel.pushFloat(e_i); // Lattice.str:30
    outputChannel.pushFloat(e_bar_i); // Lattice.str:31
    inputChannel.popFloat(); // Lattice.str:32
    inputChannel.popFloat(); // Lattice.str:33
  }
  public void init(final float _param_k_par) { // Lattice.str:24
    k_par = _param_k_par; // Lattice.str:24
    setIOTypes(Float.TYPE, Float.TYPE); // Lattice.str:24
    addSteadyPhase(2, 2, 2, "work"); // Lattice.str:25
  }
}
class ZeroStage extends SplitJoin // Lattice.str:38
{
  public static ZeroStage __construct()
  {
    ZeroStage __obj = new ZeroStage();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void init() { // Lattice.str:38
    setSplitter(DUPLICATE()); // Lattice.str:39
    add(new Identity(Float.TYPE)); // Lattice.str:40
    add(new Identity(Float.TYPE)); // Lattice.str:41
    setJoiner(ROUND_ROBIN(1)); // Lattice.str:42
  }
}
class CompStage extends Pipeline // Lattice.str:46
{
  private float __param_k_par;
  public static CompStage __construct(float k_par)
  {
    CompStage __obj = new CompStage();
    __obj.__param_k_par = k_par;
    return __obj;
  }
  protected void callInit()
  {
    init(__param_k_par);
  }
  public void init(final float k_par) { // Lattice.str:46
    add(LatDel.__construct()); // Lattice.str:47
    add(LatFilt.__construct(k_par)); // Lattice.str:48
  }
}
class LastStage extends Filter // Lattice.str:52
{
  public static LastStage __construct()
  {
    LastStage __obj = new LastStage();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  float x; // Lattice.str:53
  public void work() { // Lattice.str:54
    x = inputChannel.popFloat(); // Lattice.str:55
    System.out.println(x); // Lattice.str:56
    x = inputChannel.popFloat(); // Lattice.str:57
  }
  public void init() { // Lattice.str:52
    setIOTypes(Float.TYPE, Void.TYPE); // Lattice.str:52
    addSteadyPhase(2, 2, 0, "work"); // Lattice.str:54
  }
}
class Counter extends Filter // Lattice.str:61
{
  public static Counter __construct()
  {
    Counter __obj = new Counter();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  float i; // Lattice.str:62
  public void work() { // Lattice.str:67
    outputChannel.pushFloat(i); // Lattice.str:68
    i = 0; // Lattice.str:69
  }
  public void init() { // Lattice.str:63
    setIOTypes(Void.TYPE, Float.TYPE); // Lattice.str:61
    addSteadyPhase(0, 0, 1, "work"); // Lattice.str:67
    i = 1; // Lattice.str:64
  }
}
class RandomSource extends Filter // Lattice.str:73
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
  int seed; // Lattice.str:74
  public void work() { // Lattice.str:75
    outputChannel.pushFloat(seed); // Lattice.str:76
    seed = (((65793 * seed) + 4282663) % 8388608); // Lattice.str:77
  }
  public void init() { // Lattice.str:73
    seed = 0; // Lattice.str:74
    setIOTypes(Void.TYPE, Float.TYPE); // Lattice.str:73
    addSteadyPhase(0, 0, 1, "work"); // Lattice.str:75
  }
}
public class Lattice extends StreamItPipeline // Lattice.str:81
{
  public void init() { // Lattice.str:81
    add(RandomSource.__construct()); // Lattice.str:83
    add(ZeroStage.__construct()); // Lattice.str:84
    for (int i = 2; (i < 10); i++) { // Lattice.str:85
      add(CompStage.__construct(i)); // Lattice.str:85
    }; // Lattice.str:85
    add(LastStage.__construct()); // Lattice.str:86
  }
  public static void main(String[] args) {
    Lattice program = new Lattice();
    program.run(args);
    FileWriter.closeAll();
  }
}
