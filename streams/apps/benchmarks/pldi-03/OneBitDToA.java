import streamit.*;
class Complex extends Structure {
  public float real;
  public float imag;
}
public class OneBitDToA extends StreamIt
{
  public static void main(String[] args) {
    OneBitDToA program = new OneBitDToA();
    program.run(args);
  }
  public void init() {
    add(new DataSource());
    add(new NoiseShaper());
    add(new DataSink());
  }
}
class NoiseShaper extends FeedbackLoop
{
  public void init() {
    setJoiner(WEIGHTED_ROUND_ROBIN(1, 1));
    setBody(new Pipeline() {
          public void init() {
        add(new AdderFilter());
        add(new QuantizerAndError());
      }
}
);
    setLoop(new Delay());
    setSplitter(WEIGHTED_ROUND_ROBIN(1, 1));
    /* enqueue(0) */;
  }
}
class DataSource extends Filter
{
  int SIZE = 100;
  float[] data;
  int index;
  public void work() {
    output.pushFloat(data[index]);
    index = ((index + 1) % SIZE);
  }
  public void init() {
    data = new float[SIZE];
    output = new Channel(Float.TYPE, 1);
    for (int i = 0; (i < SIZE); i++) {
      float t = i;
      data[i] = (((float)Math.sin(((2 * 3.141592653589793f) * (t / SIZE))) + (float)Math.sin((((2 * 3.141592653589793f) * ((1.7f * t) / SIZE)) + (3.141592653589793f / 3)))) + (float)Math.sin((((2 * 3.141592653589793f) * ((2.1f * t) / SIZE)) + (3.141592653589793f / 5))));
    };
    index = 0;
  }
}
class DataSink extends Filter
{
  public void work() {
    System.out.println(input.popFloat());
  }
  public void init() {
    input = new Channel(Float.TYPE, 1);
  }
}
class DataSelector extends Filter
{
  public void work() {
    input.popFloat();
    output.pushFloat(input.popFloat());
  }
  public void init() {
    input = new Channel(Float.TYPE, 2);
    output = new Channel(Float.TYPE, 1);
  }
}
class QuantizerAndError extends Filter
{
  public void work() {
    float inputValue = input.popFloat();
    float outputValue;
    if ((inputValue < 0)) {
      outputValue = -1;
    } else {
      outputValue = 1;
    };
    float errorValue = (outputValue - inputValue);
    output.pushFloat(outputValue);
    output.pushFloat(errorValue);
  }
  public void init() {
    input = new Channel(Float.TYPE, 1);
    output = new Channel(Float.TYPE, 2);
  }
}
class AdderFilter extends Filter
{
  public void work() {
    output.pushFloat((input.popFloat() + input.popFloat()));
  }
  public void init() {
    input = new Channel(Float.TYPE, 2);
    output = new Channel(Float.TYPE, 1);
  }
}
class Delay extends Filter
{
  float state;
  public void work() {
    output.pushFloat(state);
    state = input.popFloat();
  }
  public void init() {
    input = new Channel(Float.TYPE, 1);
    output = new Channel(Float.TYPE, 1);
    state = 0;
  }
}
