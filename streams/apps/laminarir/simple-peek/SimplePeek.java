import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class RandomSource extends Filter // SimplePeek.str:7
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
  int seed; // SimplePeek.str:8
  public void work() { // SimplePeek.str:9
    outputChannel.pushInt(seed); // SimplePeek.str:10
    seed = (((65793 * seed) + 4282663) % 8388608); // SimplePeek.str:11
  }
  public void init() { // SimplePeek.str:7
    seed = 0; // SimplePeek.str:8
    setIOTypes(Void.TYPE, Integer.TYPE); // SimplePeek.str:7
    addSteadyPhase(0, 0, 1, "work"); // SimplePeek.str:9
  }
}
class MovingAverage extends Filter // SimplePeek.str:15
{
  public static MovingAverage __construct()
  {
    MovingAverage __obj = new MovingAverage();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // SimplePeek.str:16
    outputChannel.pushInt(((inputChannel.peekInt(0) + inputChannel.peekInt(1)) / 2)); // SimplePeek.str:17
    inputChannel.popInt(); // SimplePeek.str:18
  }
  public void init() { // SimplePeek.str:15
    setIOTypes(Integer.TYPE, Integer.TYPE); // SimplePeek.str:15
    addSteadyPhase(2, 1, 1, "work"); // SimplePeek.str:16
  }
}
class Printer extends Filter // SimplePeek.str:22
{
  public static Printer __construct()
  {
    Printer __obj = new Printer();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void work() { // SimplePeek.str:23
    System.out.println(inputChannel.popInt()); // SimplePeek.str:24
  }
  public void init() { // SimplePeek.str:22
    setIOTypes(Integer.TYPE, Void.TYPE); // SimplePeek.str:22
    addSteadyPhase(1, 1, 0, "work"); // SimplePeek.str:23
  }
}
public class SimplePeek extends StreamItPipeline // SimplePeek.str:1
{
  public void init() { // SimplePeek.str:1
    add(RandomSource.__construct()); // SimplePeek.str:2
    add(MovingAverage.__construct()); // SimplePeek.str:3
    add(Printer.__construct()); // SimplePeek.str:4
  }
  public static void main(String[] args) {
    SimplePeek program = new SimplePeek();
    program.run(args);
    FileWriter.closeAll();
  }
}
