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

	BufferedReader jErrStream =  new BufferedReader(new InputStreamReader(jProcess.getErrorStream()));
	while (jErrStream.ready())
	    System.out.println(jErrStream.readLine());
	System.exit(0);
    }
}
