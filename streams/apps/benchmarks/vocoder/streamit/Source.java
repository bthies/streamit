import streamit.library.*;
import streamit.library.io.*;

class RandSource extends Filter
{
  final int length = 6;
  int i = 0;
  int x = 0;
  
    public void work() {
        output.pushInt(x);
	if (i < length)
	  x = 2 * x + 1;
	else
	  x = (x - 1) / 2;

        i++;
	if (i == (length * 2)) {
	  x = 1; i = 0;
	}
	//        System.out.println(("Random: "+i));
    }
    public void init() {
        output = new Channel(Integer.TYPE, 1);
    }
    public RandSource() {
        super();
    }
}

class PlateauSource extends Filter
{
  int length = 6;
  int i = 0;
  int x = 0;
  int up = 1;
  
    public void work() {
        output.pushInt(x);
	if (i == length) {
	  i = 0;
	  if (up == 1) {
	    x++;
	  } else {
	    x--;
	  }
	  if (x == length) {
	    up = -1; i = 0;
	  }
	  if (x == 0) {
	    up = 1;
	  }
	} else {
	  i++;
	}

	//        System.out.println(("Random: "+i));
    }
    public void init(int length) {
      this.length = length;
        output = new Channel(Integer.TYPE, 1);
    }
    public PlateauSource(int length) {
        super(length);
    }
}

class StepSource extends Filter
{
  int x, length;
  int up;

  public void work() {
    output.pushInt(x);
    if (x == length) { up = 0;} else if (x == 0) { up = 1; }
//      x = x + ((up == 1) ? 1 : -1);
    if (up == 1) {
      x += 1;
    } else {
      x -= 1;
    }
//      x = x + 1;
//      x = (x + 1) % length;
  }
  public void init(int len) {
    this.length = len;
    this.up = 1;
    this.x = 0;
    output = new Channel(Integer.TYPE, 1);
  }
  public StepSource(int length) {
    super(length);
  }
}

class AddSource extends Filter
{
  float x, length;

  public void work() {
    output.pushFloat(x);
    x += length;
  }
  public void init(float len) {
    this.length = len;
    output = new Channel(Float.TYPE, 1);
  }
  public AddSource(float len) {
    super(len);
  }
}

class ModularFilter extends Filter {
  float mod;
  public void work() {
    output.pushFloat(input.popFloat() % mod);
  }
  public void init(float mod) {
    this.mod = mod;
    output = new Channel(Float.TYPE, 1);
    input = new Channel(Float.TYPE, 1);
  }
  public ModularFilter(float mod) {
    super(mod);
  }
}

class FunkySource extends Pipeline {
  public void init(final float c) {
    add(new SplitJoin() {
	public void init() {
//  	  setSplitter(VOID());
	  add(new SineSource(c * 4f));
	  add(new AddSource(c));
	  setJoiner(ROUND_ROBIN());
	}
      });
    add(new Filter() {
	public void init() {
	  output = new Channel(Float.TYPE, 1);
	  input = new Channel(Float.TYPE, 2);
	}
	public void work() {
	  output.pushFloat(input.popFloat() + input.popFloat());
	}
      });
  }
  public FunkySource(float c) {
    super(c);
  }
}

class SineSource extends Filter
{
  float theta, prev;
  
  public void work() {
    prev = prev + theta;
    output.pushFloat((float)Math.sin(prev));
  }
  public void init(float theta) {
    this.theta = theta; this.prev = theta * 3.2f;
    output = new Channel(Float.TYPE, 1);
  }
  public SineSource(float theta) {
    super(theta);
  }
}

class SineFilter extends Filter
{
  float theta;
  
  public void work() {
    output.pushFloat((float)Math.sin(theta*input.popFloat()));
  }
  public void init(float theta) {
    this.theta = theta;
    input = new Channel(Float.TYPE, 1);
    output = new Channel(Float.TYPE, 1);
  }
  public SineFilter(float theta) {
    super(theta);
  }
}

class WaveReader extends Filter
{
  private boolean first = true;
  private short channels, bitsPerSample;
  private int size;
  public WaveReader() {
  }

  public void init() {
    input = new Channel(Short.TYPE, 22);
    output = new Channel(Short.TYPE, 22);
  }

  short next() {
    short current = input.popShort();
    output.pushShort(current);
    return current;
  }

  int   nextInt() {
    return (int) (next() &0xffff) | (next() << 16);
  }

  char[] nextId(char[] id) {
    for(int i=0; i < id.length / 2; i++) {
      short current = next();
      id[(i << 1)] = (char) (current & 0x7F);
      id[(i << 1) + 1] = (char) (current >> 8);
    }
    return id;
  }

  public void work() {
    if (first) {
      first = false;
      char GROUP_ID[] = new char[4];
      char RIFF_TYPE[] = new char[4];
      char CHUNK_ID[] = new char[4];
      int  chunkSize;

      nextId(GROUP_ID);
      chunkSize = nextInt();
      System.out.println(String.valueOf(GROUP_ID));
      if (!String.valueOf(GROUP_ID).equalsIgnoreCase("RIFF"))
	System.exit(-1);
      System.out.println("Size: " + Integer.toHexString(chunkSize));

      nextId(RIFF_TYPE);
      System.out.println(String.valueOf(RIFF_TYPE));
      if (!String.valueOf(RIFF_TYPE).equalsIgnoreCase("WAVE"))
	System.exit(-1);

      nextId(CHUNK_ID);

      while (!String.valueOf(CHUNK_ID).equalsIgnoreCase("fmt ")) {
	chunkSize = nextInt();
	for(int i=0; i < chunkSize / 2; i++)
	  next();
	nextId(CHUNK_ID);
      }

      chunkSize = nextInt();
      if (next() != 1 || chunkSize != 16) {
	System.err.println("Error: Cannot handle compressed WAVE files");
	System.exit(-1);
      }

      channels = next();
      System.out.println("Channels: " + channels);
      int dataRate = nextInt();
      System.out.println("Data Rate: " + dataRate + "hz");
      int avgBytesPerSec = nextInt();
      short blockAlign = next();
      short bitsPerSample = next();
      System.out.println(bitsPerSample + " * " + channels + " = " +
			 blockAlign * 8);
      if (bitsPerSample != 16) {
	System.err.println("Error: Can only handle 16 bit samples (" +
			   bitsPerSample + " bits)");
	System.exit(-1);
      }

      nextId(CHUNK_ID);

      while (!String.valueOf(CHUNK_ID).equalsIgnoreCase("data")) {
	chunkSize = nextInt();
	for(int i=0; i < chunkSize / 2; i++)
	  next();
	nextId(CHUNK_ID);
      }
      chunkSize = nextInt();
      System.out.println("Size: " + Integer.toHexString(chunkSize));
//        next();
      //System.out.println("Size: " + chunkSize);
      //      input.popShort();
    } else {
      for (int i=0; i < 22; i++)
	next();
    }
      //      output.pushShort(input.popShort());
  }
}

class WaveHeader extends Filter {
  float speed, freq;

  public WaveHeader(float speed, float freq) { super(speed,freq);}
  public void  init(float speed, float freq) {
    this.speed = speed; this.freq = freq;
    input = new Channel(Short.TYPE, 22);
    output = new Channel(Short.TYPE, 22);
  }
  private short next() {
    return input.popShort();
  }
  private void pass() {
    send(next());
  }
  private void send(short s) {
    output.pushShort(s);
  }
  private void sendInt(int i) {
    send((short) (i & 0xffff));
    send((short) (i >> 16));
  }
  private int nextInt() {
    return (int) (next() &0xffff) | (next() << 16);
  }


  public void work() {
    /** Structure is: "RIFF" file_length "WAVE" "fmt "
     *          chunk_length compression channels sample_rate data_rate
     *       bytes_per_frame bits_per_sample "data" length
     **/
    pass(); pass(); //"RIFF"
    int file_length = nextInt();
    //file_length is data chunk + 36 bytes of header info
    file_length = (int) Math.round((file_length - 36)* speed) + 36;
    sendInt(file_length);
    pass(); pass(); //"WAVE"
    pass(); pass(); //"fmt "
    pass(); pass(); //fmt chunk_length; must be 16
    pass(); //compression; must be 1
    pass(); //channels; for now, assuming 2 channels
    pass(); pass(); //sample_rate; don't care about it
    pass(); pass(); //data_rate; should be same
    pass(); pass(); //same bytes_per_frame and bits_per_sample (16)
    pass(); pass(); //"data"
    int samples = nextInt();
    samples = (int) Math.round(samples * speed);
    sendInt(samples);
  }
}

class WaveSplitter extends Filter
{
  private boolean first = true;
  private short channels, bitsPerSample;
  private int size;
  private int channel, current;
  public WaveSplitter(int c) {
    super(c);
  }

  public void init(int c) {
    this.channel = c; this.current = 0;
    input = new Channel(Short.TYPE, 1);
    output = new Channel(Short.TYPE, 1);
  }

  short next() {
    short temp = input.popShort();
    output.pushShort(temp);
    return temp;
  }

  int   nextInt() {
    return (int) (next() &0xffff) | (next() << 16);
  }

  char[] nextId(char[] id) {
    for(int i=0; i < id.length / 2; i++) {
      short current = next();
      id[(i << 1)] = (char) (current & 0x7F);
      id[(i << 1) + 1] = (char) (current >> 8);
    }
    return id;
  }

  public void work() {
    if (first) {
      first = false;
      char GROUP_ID[] = new char[4];
      char RIFF_TYPE[] = new char[4];
      char CHUNK_ID[] = new char[4];
      int  chunkSize;

      nextId(GROUP_ID);
      chunkSize = nextInt();
      System.out.println(String.valueOf(GROUP_ID));
      if (!String.valueOf(GROUP_ID).equalsIgnoreCase("RIFF"))
	System.exit(-1);
      System.out.println("Size: " + Integer.toHexString(chunkSize));

      nextId(RIFF_TYPE);
      System.out.println(String.valueOf(RIFF_TYPE));
      if (!String.valueOf(RIFF_TYPE).equalsIgnoreCase("WAVE"))
	System.exit(-1);

      nextId(CHUNK_ID);

      while (!String.valueOf(CHUNK_ID).equalsIgnoreCase("fmt ")) {
	chunkSize = nextInt();
	for(int i=0; i < chunkSize / 2; i++)
	  next();
	nextId(CHUNK_ID);
      }

      chunkSize = nextInt();
      if (next() != 1 || chunkSize != 16) {
	System.err.println("Error: Cannot handle compressed WAVE files");
	System.exit(-1);
      }

      channels = input.popShort();
      output.pushShort((short)1);
      System.out.println("Channels: " + channels);
      int dataRate = nextInt();
      System.out.println("Data Rate: " + dataRate + "hz");
      int avgBytesPerSec = nextInt();
      short blockAlign = input.popShort();
      output.pushShort((short)(blockAlign / channels));
      short bitsPerSample = next();
      System.out.println(bitsPerSample + " * " + channels + " = " +
			 blockAlign * 8);
      if (bitsPerSample != 16) {
	System.err.println("Error: Can only handle 16 bit samples (" +
			   bitsPerSample + " bits)");
	System.exit(-1);
      }

      nextId(CHUNK_ID);

      while (!String.valueOf(CHUNK_ID).equalsIgnoreCase("data")) {
	chunkSize = nextInt();
	for(int i=0; i < chunkSize / 2; i++)
	  next();
	nextId(CHUNK_ID);
      }
      chunkSize = nextInt();
      System.out.println("Size: " + Integer.toHexString(chunkSize));

      //      output.pushFloat((float) next());
      //System.out.println("Size: " + chunkSize);
      //      input.popShort();
    }
    if (current % channels == channel) {
      next();
      current++;
    }
    else {
      current++;
      input.popShort();
    }
      
//        output.pushFloat((float) next());
      //      output.pushShort(input.popShort());
  }

}
