import streamit.library.Pipeline;
import streamit.library.SplitJoin;
import streamit.library.Filter;
import streamit.library.Channel;

class FloatIdentity extends Filter
{
    public FloatIdentity()
    {
        super();
    }
    public void init()
    {
        input = new Channel(Float.TYPE, 1);
        output = new Channel(Float.TYPE, 1);
    }
    public void work()
    {
        output.pushFloat(input.popFloat());
    }
}

public class FilterBank extends Pipeline
{
    public void init()
    {
        add(new Antialias());
        add(new IMDCT());

        // overlapping:
        add(new Filter()
        {
            public void init()
            {
                this.input = new Channel(Float.TYPE, 36, 36 + 1152);
                this.output = new Channel(Float.TYPE, 18);
            }

            public void work()
            {
                int i;
                for (i = 0; i < 18; i++)
                {
                    float future1 = this.input.peekFloat(1152);
                    float future2 = this.input.peekFloat(18);
                    this.output.pushFloat(future1 + future2);
                    this.input.popFloat();
                }
                for (i = 0; i < 18; i++)
                {
                    this.input.popFloat();
                }
            }
        });

        // frequency inversion
        add(new SplitJoin()
        {
            public void init()
            {
                int x;
                this.setSplitter(ROUND_ROBIN());

                for (x = 0; x < 18; x++)
                {
                    if (x % 2 == 0)
                    {
                        this.add(new FloatIdentity());
                    } else
                    {
                        this.add(new Pipeline () 
                        {
                            public void init ()
                            {
                                this.add (new SplitJoin()
                                {
                                    public void init()
                                    {
                                        this.setSplitter(ROUND_ROBIN());
                                        this.add(new FloatIdentity());
                                        this.add(new Filter()
                                        {
                                            public void init()
                                            {
                                                this.input = new Channel(Float.TYPE, 1);
                                                this.output = new Channel(Float.TYPE, 1);
                                            }
                                            public void work()
                                            {
                                                this.output.pushFloat(-this.input.popFloat());
                                            }
                                        });
                                        this.setJoiner(ROUND_ROBIN());
                                    }
                                });
                            }
                        });
                    }
                }

                // join the 18 producers and put them in entire 32-sample
                // subband pieces
                setJoiner(
                    WEIGHTED_ROUND_ROBIN(
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32,
                        32));
            }
        });

        // polyphase synthesis
        add(new PCMSynthesis());
    }
}
