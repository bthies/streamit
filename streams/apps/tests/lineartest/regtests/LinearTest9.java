import streamit.*;
class Complex extends Structure {
  public float real;
  public float imag;
}
public class LinearTest9 extends StreamIt
{
  public static void main(String[] args) {
    LinearTest9 program = new LinearTest9();
    program.run(args);
  }
  public void init() {
    add(new FloatSource());
    add(new LinearPipe());
    add(new FloatSink());
  }
}
class FloatSource extends Filter
{
  float x;
  public void work() {
    output.pushFloat(x);
    x = (x + 1);
  }
  public void init() {
    output = new Channel(Float.TYPE, 1);
    x = 0;
  }
}
class FloatSink extends Filter
{
  public void work() {
    System.out.println(input.popFloat());
  }
  public void init() {
    input = new Channel(Float.TYPE, 1, 1);
  }
}
class FloatIdentity extends Filter
{
  public void work() {
    output.pushFloat(input.popFloat());
  }
  public void init() {
    input = new Channel(Float.TYPE, 1, 1);
    output = new Channel(Float.TYPE, 1);
  }
}
class LinearFilter extends Filter
{
  public void work() {
    float t1 = input.popFloat();
    float t2 = input.popFloat();
    float t3 = input.popFloat();
    output.pushFloat((t1 + (t2 + t3)));
  }
  public void init() {
    input = new Channel(Float.TYPE, 3, 3);
    output = new Channel(Float.TYPE, 1);
  }
}
class LinearPipe extends Pipeline
{
  public void init() {
    add(new LinearFilter());
    add(new FloatIdentity());
  }
}
