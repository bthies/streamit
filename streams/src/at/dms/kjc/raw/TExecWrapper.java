package at.dms.kjc.raw;

import java.io.*;

public class TExecWrapper 
{
    public static void main(String[] args) throws Exception
    {
	Process jProcess = Runtime.getRuntime().exec(args);
	
	jProcess.waitFor();
	BufferedReader jInStream =  new BufferedReader(new InputStreamReader(jProcess.getInputStream()));
	while (jInStream.ready())
	    System.out.println(jInStream.readLine());

	System.exit(0);
    }
}
