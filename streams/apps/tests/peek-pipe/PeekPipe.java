import streamit.library.*;

public class PeekPipe extends StreamIt {
    public static void main(String args[]) {
        new PeekPipe().run(args);
    }
    public void init() {
        add(new Filter() {
                int x;
                public void work() {
                    output.pushInt(x++);
                }
                public void init ()
                {
		    x = 0;
                    output = new Channel(Integer.TYPE, 1);
                }
            });
        add(new Filter() {
                public void work() {
                    int i;
                    int sum = 0;
                    for (i=0; i<10; i++) {
                        sum += input.peekInt(i);
                    }
                    System.out.println(sum);
                    input.popInt();
                }
                public void init ()
                {
                    input = new Channel(Integer.TYPE, 1, 10);
                }
            });
    }
}
