import streamit.library.Filter;
import streamit.library.Channel;
import javazoom.jl.player.Player;
import javazoom.jl.player.JavaSoundAudioDevice;

public class SoundOutput extends Filter
{
    final int INPUT_DATA = 216000;
    JavaSoundAudioDevice audio;
    short buffer[];

    SoundOutput(float samplingFrequency, int nChannels)
    {
        super(samplingFrequency, nChannels);
    }

    public void init(float samplingFrequency, int nChannels)
    {
        try
        {
            audio = new JavaSoundAudioDevice();

            audio.createAudioFormat(samplingFrequency, nChannels);
            audio.openImpl();
            audio.setOpen(true);
        } catch (Throwable t)
        {
            //ERROR(t);
        }

        buffer = new short[INPUT_DATA];

        input = new Channel(Short.TYPE, INPUT_DATA);
    }
    public void work()
    {
		System.out.println (".");
        int x;
        for (x = 0; x < INPUT_DATA; x++)
        {
            buffer[x] = input.popShort();
        }

        try
        {
            audio.write(buffer, 0, INPUT_DATA);
        } catch (Throwable t)
        {
            //ERROR(t);
        }
    }
}

