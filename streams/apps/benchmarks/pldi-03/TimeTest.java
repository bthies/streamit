import streamit.*;
class Complex extends Structure {
  public float real;
  public float imag;
}
class Adder extends Filter
{
  int N;
  public void work() {
    float sum = 0;
    for (int i = 0; (i < N); i++) {
      sum += input.popFloat();
    };
    output.pushFloat(sum);
  }
  public Adder(int N) {
    super(N);
  }
  public void init(final int N) {
    this.N = N;
    input = new Channel(Float.TYPE, N);
    output = new Channel(Float.TYPE, 1);
  }
}
class BandPassFilter extends Pipeline
{
  public BandPassFilter(float gain, float ws, float wp, int numSamples) {
    super(gain, ws, wp, numSamples);
  }
  public void init(final float gain, final float ws, final float wp, final int numSamples) {
    add(new LowPassFilter(1, wp, numSamples));
    add(new HighPassFilter(gain, ws, numSamples));
  }
}
class BandStopFilter extends Pipeline
{
  public BandStopFilter(float gain, float wp, float ws, int numSamples) {
    super(gain, wp, ws, numSamples);
  }
  public void init(final float gain, final float wp, final float ws, final int numSamples) {
    add(new SplitJoin() {
          public void init() {
        setSplitter(DUPLICATE());
        add(new LowPassFilter(gain, wp, numSamples));
        add(new HighPassFilter(gain, ws, numSamples));
        setJoiner(ROUND_ROBIN(1));
      }
}
);
    add(new Adder(2));
  }
}
class Compressor extends Filter
{
  int M;
  public void work() {
    output.pushFloat(input.popFloat());
    for (int i = 0; (i < (M - 1)); i++) {
      input.popFloat();
    };
  }
  public Compressor(int M) {
    super(M);
  }
  public void init(final int M) {
    this.M = M;
    input = new Channel(Float.TYPE, M, M);
    output = new Channel(Float.TYPE, 1);
  }
}
class Expander extends Filter
{
  int L;
  public void work() {
    output.pushFloat(input.popFloat());
    for (int i = 0; (i < (L - 1)); i++) {
      output.pushFloat(0);
    };
  }
  public Expander(int L) {
    super(L);
  }
  public void init(final int L) {
    this.L = L;
    input = new Channel(Float.TYPE, 1, 1);
    output = new Channel(Float.TYPE, L);
  }
}
class FloatPrinter extends Filter
{
  public void work() {
    System.out.println(input.popFloat());
  }
  public void init() {
    input = new Channel(Float.TYPE, 1);
  }
}
class FloatSink extends Filter
{
  public void work() {
    input.popFloat();
  }
  public void init() {
    input = new Channel(Float.TYPE, 1);
  }
}
class HighPassFilter extends Filter
{
  float[] h;
  float g;
  float ws;
  int N;
  public void work() {
    float sum = 0;
    for (int i = 0; (i < N); i++) {
      sum += (h[i] * input.peekFloat(i));
    };
    output.pushFloat(sum);
    input.popFloat();
  }
  public HighPassFilter(float g, float ws, int N) {
    super(g, ws, N);
  }
  public void init(final float g, final float ws, final int N) {
    this.g = g;
    this.ws = ws;
    this.N = N;
    h = new float[N];
    input = new Channel(Float.TYPE, 1, N);
    output = new Channel(Float.TYPE, 1);
    int OFFSET = (N / 2);
    float cutoffFreq = (3.141592653589793f - ws);
    for (int i = 0; (i < N); i++) {
      int idx = (i + 1);
      int sign = (((i % 2) == 0) ? 1 : -1);
      if ((idx == OFFSET)) {
        h[i] = (((sign * g) * cutoffFreq) / 3.141592653589793f);
      } else {
        h[i] = (((sign * g) * (float)Math.sin((cutoffFreq * (idx - OFFSET)))) / (3.141592653589793f * (idx - OFFSET)));
      };
    };
  }
}
class LowPassFilter extends Filter
{
  float[] h;
  float g;
  float cutoffFreq;
  int N;
  public void work() {
    float sum = 0;
    for (int i = 0; (i < N); i++) {
      sum += (h[i] * input.peekFloat(i));
    };
    output.pushFloat(sum);
    input.popFloat();
  }
  public LowPassFilter(float g, float cutoffFreq, int N) {
    super(g, cutoffFreq, N);
  }
  public void init(final float g, final float cutoffFreq, final int N) {
    this.g = g;
    this.cutoffFreq = cutoffFreq;
    this.N = N;
    h = new float[N];
    input = new Channel(Float.TYPE, 1, N);
    output = new Channel(Float.TYPE, 1);
    int OFFSET = (N / 2);
    for (int i = 0; (i < N); i++) {
      int idx = (i + 1);
      if ((idx == OFFSET)) {
        h[i] = ((g * cutoffFreq) / 3.141592653589793f);
      } else {
        h[i] = ((g * (float)Math.sin((cutoffFreq * (idx - OFFSET)))) / (3.141592653589793f * (idx - OFFSET)));
      };
    };
  }
}
public class TimeTest extends StreamIt
{
  public static void main(String[] args) {
    TimeTest program = new TimeTest();
    program.run(args);
  }
  public void init() {
    add(new FloatSource(16));
    add(new LowPassFilter(1, (3.141592653589793f / 3), 255));
    add(new FloatPrinter());
  }
}
class FloatSource extends Filter
{
  int index;
  float[] sourceData;
  int size;
  public void work() {
    output.pushFloat(sourceData[index]);
    index = ((index + 1) % size);
  }
  public FloatSource(int size) {
    super(size);
  }
  public void init(final int size) {
    this.size = size;
    sourceData = new float[size];
    output = new Channel(Float.TYPE, 1);
    for (int i = 0; (i < size); i++) {
      sourceData[i] = (i + 0.0010f);
    };
    index = 0;
  }
}
