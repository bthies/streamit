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

    public ChannelConnectFilter () { super (); }

    public void init ()
    {
        if (type != null)
        {
            input = new Channel (type, 1);
            output = new Channel (type, 1);
        }
    }

    public void work()
    {
        passOneData (input, output);
    }

    void useChannels (Channel in, Channel out)
    {
        ASSERT (input == null && output == null);
        ASSERT (in != null && out != null);
        ASSERT (in != out);
        ASSERT (out.getType ().getName ().equals (in.getType ().getName ()),
		"Mismatched tape types.  Source output is " + in.getType().getName() + ", but sink input is " + out.getType().getName() );

        input = in;
        output = out;

        input.setSink (this);
        output.setSource (this);
    }
}
