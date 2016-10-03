import java.io.Serializable;
import streamit.library.*;
import streamit.library.io.*;
import streamit.misc.StreamItRandom;
class Complex extends Structure implements Serializable {
  float real;
  float imag;
}
class RandomSource extends Filter // SimpleTwoStage.str:7
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
  int seed; // SimpleTwoStage.str:8
  public void work() { // SimpleTwoStage.str:9
    outputChannel.pushInt(seed); // SimpleTwoStage.str:10
    seed = (((65793 * seed) + 4282663) % 8388608); // SimpleTwoStage.str:11
  }
  public void init() { // SimpleTwoStage.str:7
    seed = 0; // SimpleTwoStage.str:8
    setIOTypes(Void.TYPE, Integer.TYPE); // SimpleTwoStage.str:7
    addSteadyPhase(0, 0, 1, "work"); // SimpleTwoStage.str:9
  }
}
class DuplicateFirstItem extends Filter // SimpleTwoStage.str:15
{
  public static DuplicateFirstItem __construct()
  {
    DuplicateFirstItem __obj = new DuplicateFirstItem();
    return __obj;
  }
  protected void callInit()
  {
    init();
  }
  public void prework() { // SimpleTwoStage.str:16
    outputChannel.pushInt(inputChannel.peekInt(0)); // SimpleTwoStage.str:17
    outputChannel.pushInt(inputChannel.peekInt(0)); // SimpleTwoStage.str:18
    inputChannel.popInt(); // SimpleTwoStage.str:19
  }
  public void work() { // SimpleTwoStage.str:21
    outputChannel.pushInt(inputChannel.popInt()); // SimpleTwoStage.str:22
  }
  public void init() { // SimpleTwoStage.str:15
    setIOTypes(Integer.TYPE, Integer.TYPE); // SimpleTwoStage.str:15
    addInitPhase(1, 1, 2, "prework"); // SimpleTwoStage.str:16
    addSteadyPhase(1, 1, 1, "work"); // SimpleTwoStage.str:21
  }
}
class Printer extends Filter // SimpleTwoStage.str:26
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
  public void work() { // SimpleTwoStage.str:27
    System.out.println(inputChannel.popInt()); // SimpleTwoStage.str:28
  }
  public void init() { // SimpleTwoStage.str:26
    setIOTypes(Integer.TYPE, Void.TYPE); // SimpleTwoStage.str:26
    addSteadyPhase(1, 1, 0, "work"); // SimpleTwoStage.str:27
  }
}
public class SimpleTwoStage extends StreamItPipeline // SimpleTwoStage.str:1
{
  public void init() { // SimpleTwoStage.str:1
    add(RandomSource.__construct()); // SimpleTwoStage.str:2
    add(DuplicateFirstItem.__construct()); // SimpleTwoStage.str:3
    add(Printer.__construct()); // SimpleTwoStage.str:4
  }
  public static void main(String[] args) {
    SimpleTwoStage program = new SimpleTwoStage();
    program.run(args);
    FileWriter.closeAll();
  }
}
