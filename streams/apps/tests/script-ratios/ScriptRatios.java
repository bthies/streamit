import streamit.library.*;

class ScriptFilter extends Filter {
    int PEEK;
    int POP;
    int PUSH;

    public ScriptFilter(int PEEK, int POP, int PUSH) {
        super (PEEK, POP, PUSH);
    }

    public void init(int PEEK, int POP, int PUSH) {
        input = new Channel(Float.TYPE, POP, PEEK);
        output = new Channel(Float.TYPE, PUSH);
        this.PEEK = PEEK;
        this.POP = POP;
        this.PUSH = PUSH;
    }

    public void work() {
        for (int i=0; i<PEEK; i++) {
            input.peekFloat(i);
        }
        for (int i=0; i<POP; i++) {
            input.popFloat();
        }
        for (int i=0; i<PUSH; i++) {
            output.pushFloat(i);
        }
    }
}

public class ScriptRatios extends StreamIt {
    public static void main(String[] args) {
        new ScriptRatios().run(args);
    }

    public void init() {
        add(new FloatOneSource());
        add(new ScriptFilter(10, 5, 1));
        add(new FloatPrinter());
    }
}
