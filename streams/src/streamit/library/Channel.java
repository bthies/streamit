// a channel is an I/O FIFO queue to go between filters

package streamit;

public class Channel {

    // the channel should be constructed with a 0-length array
    // indicating the type that will be held in this channel.
    public Channel(Object type) {
    }

    // PUSH OPERATIONS ----------------------------------------------

    // push something of type <type>
    public void push(Object o) {}

    // push an int
    public void pushInt(int i) {}

    // push a char
    public void pushChar(char c) {}

    // push a double
    public void pushDouble(double d) {}

    // push a String
    public void pushString(String str) {}

    // POP OPERATIONS ----------------------------------------------

    // pop something of type <type>
    public Object pop() { return null; }

    // pop an int
    public int popInt() { return 0; }

    // pop a char
    public char popChar() { return 'x'; }

    // pop a double
    public double popDouble() { return 0; }

    // pop a String
    public String popString() { return null; }

    // PEEK OPERATIONS ----------------------------------------------

    // peek at something of type <type>
    public Object peek(int index) { return null; }

    // peek at an int
    public int peekInt(int index) { return 0; }

    // peek at a char
    public char peekChar(int index) { return 'x'; }

    // peek at a double
    public double peekDouble(int index) { return 0; }

    // peek at a String
    public String peekString(int index) { return null; }

}
