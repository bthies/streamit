/*
 * HelloWorld6.c: semi-translated (out of Kopi) "Hello World"
 * $Id: HelloWorld6.c.java,v 1.3 2003-09-29 09:07:09 thies Exp $
 */

import streamit.library.internal.*;

class StreamitGenerated
{
    class HelloWorld6_1_data
    {
        StreamContext c;
        int x;
    }

    void HelloWorld6_1_init(HelloWorld6_1_data d, Object p)
    {
        StreaMIT.set_stream_type(d.c, StreaMIT.FILTER);
        StreaMIT.set_push(d.c, 1);
        StreaMIT.set_work(d.c, HelloWorld6_1_work);
        d.x = 0;
    }

    void HelloWorld6_1_work(Object dv, Tape in_tape, Tape out_tape)
    {
        HelloWorld6_1_data d = dv;
        d.x++;
        StreaMIT.PUSH(out_tape, Integer.TYPE, d.x);
    }

    class HelloWorld6_2_data
    {
        StreamContext c;
    }

    void HelloWorld6_2_init(HelloWorld6_2_data d, Object p)
    {
        StreaMIT.set_stream_type(d.c, StreaMIT.FILTER);
        StreaMIT.set_pop(d.c, 1);
        StreaMIT.set_work(d.c, HelloWorld6_2_work);
    }

    /* dzm: Way magical. */
    void HelloWorld6_2_work(Object dv, Tape in_tape, Tape out_tape)
    {
        StreaMIT.printf("%d\n", StreaMIT.POP(in_tape, Integer.TYPE));
    }

    class HelloWorld6_data
    {
        StreamContext c;
        HelloWorld6_1_data child1;
        HelloWorld6_2_data child2;
    }

    void HelloWorld6_init(HelloWorld6_data d, Object p)
    {
        StreaMIT.set_stream_type(d.c, StreaMIT.PIPELINE);
        StreaMIT.set_work(d.c, HelloWorld6_work);

        d.child1 = StreaMIT.malloc(StreaMIT.sizeof(HelloWorld6_1_data.class));
        d.child1.c = StreaMIT.create_context(d.child1);
        StreaMIT.register_child(d.c, d.child1.c);
        HelloWorld6_1_init(d.child1, d);

        d.child2 = StreaMIT.malloc(StreaMIT.sizeof(HelloWorld6_2_data.class));
        d.child2.c = StreaMIT.create_context(d.child2);
        StreaMIT.register_child(d.c, d.child2.c);
        HelloWorld6_2_init(d.child2, d);
    }

    void HelloWorld6_work(Object dv, Tape in_tape, Tape out_tape)
    {
        int itape[] = new int [1];
        HelloWorld6_data d = dv;
        d.child1.x++;
        itape[0] = d.child1.x;
        StreaMIT.printf("%d\n", itape[0]);
    }

    int main(int argc, char argv[][])
    {
        HelloWorld6_data test =
            StreaMIT.malloc(StreaMIT.sizeof(HelloWorld6_data.class));
        test.c = StreaMIT.create_context(test);
        HelloWorld6_init(test, null);

        StreaMIT.streamit_run(test.c);
    }
}

