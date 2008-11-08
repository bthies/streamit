/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.library;

public class ChannelConnectFilter extends Filter
{
    Class type;
    public ChannelConnectFilter (Class ioType)
    {
        super ();
        type = ioType;
    }

    public ChannelConnectFilter () { 
        super (); 
    }

    public void init ()
    {
        if (type != null)
            {
                inputChannel = new Channel (type, 1);
                outputChannel = new Channel (type, 1);
            }
        addSteadyPhase(1, 1, 1, "work");
    }

    public void work()
    {
        passOneData (inputChannel, outputChannel);
    }

    void useChannels (Channel in, Channel out)
    {
        assert inputChannel == null && outputChannel == null;
        assert in != null && out != null;
        assert in != out;
        assert out.getType ().getName ().equals (in.getType ().getName ()):
            "Mismatched tape types.  Source output is " +
            in.getType().getName() + ", but sink input is " +
            out.getType().getName() + "(Source is " + in.source + ", sink is " + out.sink;

        inputChannel = in;
        outputChannel = out;

        inputChannel.setSink (this);
        outputChannel.setSource (this);
    }
}
