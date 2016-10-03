import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class A extends Filter // Figure1.str:12
{
  public static A __construct()
  {
    A __obj = new A();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  int seed; // Figure1.str:13
  public void work() { // Figure1.str:15
    outputChannel.pushFloat(frand()); // Figure1.str:15
  }
  public float frand() { // Figure1.str:16
    seed = (((65793 * seed) + 4282663) % 8388608); // Figure1.str:17
    return ((seed < 0) ? -seed : seed); // Figure1.str:18
  }
  public void init() { // Figure1.str:12
    seed = 0; // Figure1.str:13
    setIOTypes(Void.TYPE, Float.TYPE); // Figure1.str:12
    addSteadyPhase(0, 0, 1, "work"); // Figure1.str:14
  }
}
class B extends Filter // Figure1.str:21
{
  public static B __construct()
  {
    B __obj = new B();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // Figure1.str:22
    float __sa0 = inputChannel.popFloat(); // Figure1.str:23
    float __sa1 = inputChannel.popFloat(); // Figure1.str:23
    outputChannel.pushFloat((__sa0 + (__sa1 / 2))); // Figure1.str:23
  }
  public void init() { // Figure1.str:21
    setIOTypes(Float.TYPE, Float.TYPE); // Figure1.str:21
    addSteadyPhase(2, 2, 1, "work"); // Figure1.str:22
  }
}
class C extends Filter // Figure1.str:25
{
  public static C __construct()
  {
    C __obj = new C();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // Figure1.str:26
    float __sa2 = inputChannel.popFloat(); // Figure1.str:27
    float __sa3 = inputChannel.popFloat(); // Figure1.str:27
    outputChannel.pushFloat((float)Math.sqrt((__sa2 * __sa3))); // Figure1.str:27
  }
  public void init() { // Figure1.str:25
    setIOTypes(Float.TYPE, Float.TYPE); // Figure1.str:25
    addSteadyPhase(2, 2, 1, "work"); // Figure1.str:26
  }
}
class D extends Filter // Figure1.str:29
{
  public static D __construct()
  {
    D __obj = new D();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // Figure1.str:30
    System.out.println(inputChannel.popFloat()); // Figure1.str:31
    System.out.println(inputChannel.popFloat()); // Figure1.str:32
  }
  public void init() { // Figure1.str:29
    setIOTypes(Float.TYPE, Void.TYPE); // Figure1.str:29
    addSteadyPhase(2, 2, 0, "work"); // Figure1.str:30
  }
}
class AnonFilter_a4 extends SplitJoin // Figure1.str:3
{
  public static AnonFilter_a4 __construct()
  {
    AnonFilter_a4 __obj = new AnonFilter_a4();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void init() { // Figure1.str:4
    setSplitter(DUPLICATE()); // Figure1.str:5
    add(B.__construct()); // Figure1.str:6
    add(C.__construct()); // Figure1.str:7
    setJoiner(ROUND_ROBIN(1)); // Figure1.str:8
  }
}
public class Figure1 extends StreamItPipeline // Figure1.str:1
{
  public void init() { // Figure1.str:1
    add(A.__construct()); // Figure1.str:2
    add(AnonFilter_a4.__construct()); // Figure1.str:3
    add(D.__construct()); // Figure1.str:10
  }
  public static void main(String[] args) {
    Figure1 program = new Figure1();
    program.run(args);
    FileWriter.closeAll();
  }
}
