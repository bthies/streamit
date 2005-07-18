
import java.io.*;
import streamit.library.*;

class RaytracerUtil {


    public static GridInfo parseVox(String fname,  
				    int num_tris, 
				    Triangle9[] tris) 
    {
	GridInfo ginfo = new GridInfo();  
	ginfo.grid_dim = new float3();
	ginfo.grid_max = new float3();
	ginfo.grid_min = new float3();
	ginfo.grid_vsize = new float3();
	ginfo.trilist_offsets = new int[2048];
	ginfo.trilist = new int[2048];
	//ginfo.tDelta = new float3();
	//ginfo.step = new int[3];
	//ginfo.outno = new int[3];
	
	LEDataInputStream di = null;
	try {
	    di = new LEDataInputStream(
				       new BufferedInputStream(
							       new FileInputStream(fname)));
	    ginfo.grid_dim.x = (float)di.readInt();
	    ginfo.grid_dim.y = (float)di.readInt();
	    ginfo.grid_dim.z = (float)di.readInt();
	    
	    ginfo.grid_min.x = di.readFloat();
	    ginfo.grid_min.y = di.readFloat();
	    ginfo.grid_min.z = di.readFloat();
	    
	    ginfo.grid_max.x = di.readFloat();
	    ginfo.grid_max.y = di.readFloat();
	    ginfo.grid_max.z = di.readFloat();
	    
	    ginfo.grid_vsize.x = di.readFloat();
	    ginfo.grid_vsize.y = di.readFloat();
	    ginfo.grid_vsize.z = di.readFloat();
	    
	    System.out.println("Reading bitvector... ");
	    int dim_x = (int)ginfo.grid_dim.x;
	    dim_x += (4 - dim_x % 4);
	    int dim_y = (int)ginfo.grid_dim.y;
	    dim_y += (4 - dim_y % 4);
	    int dim_z = (int)ginfo.grid_dim.z;
	    dim_z += (4 - dim_z % 4);
	    int num_entries = dim_x * dim_y * dim_z / 64 + 1;
	    di.skipBytes(num_entries * 8);
	    
	    System.out.println("Reading triangle list offsets..." + ginfo.grid_dim.x + '*' + ginfo.grid_dim.y + '*' + ginfo.grid_dim.z);
	    int i = 0;
	    for(int x = 0; x < ginfo.grid_dim.x; x++){
		for(int y = 0; y < ginfo.grid_dim.y; y++){
		    for(int z = 0; z < ginfo.grid_dim.z; z++){
			ginfo.trilist_offsets[i++] = di.readInt();
		    }
		}
	    }

	    int trilist_size = di.readInt();
	    System.out.println("Reading triangle list..." + trilist_size);
	    for(i = 0; i < trilist_size; i++) {
		ginfo.trilist[i] = di.readInt();
	    }

	    num_tris = di.readInt();
	    System.out.println("Reading triangle data..." + num_tris);
	    for( i = 0; i < num_tris; i++){
		tris[i].v0.x = di.readFloat(); 
		tris[i].v0.y = di.readFloat();
		tris[i].v0.z = di.readFloat();
		
		tris[i].v1.x = di.readFloat(); 
		tris[i].v1.y = di.readFloat();
		tris[i].v1.z = di.readFloat();
		
		tris[i].v2.x = di.readFloat(); 
		tris[i].v2.y = di.readFloat();
		tris[i].v2.z = di.readFloat();

		tris[i].n0.x = di.readFloat(); 
		tris[i].n0.y = di.readFloat();
		tris[i].n0.z = di.readFloat();

		tris[i].n1.x = di.readFloat(); 
		tris[i].n1.y = di.readFloat();
		tris[i].n1.z = di.readFloat();

		tris[i].n2.x = di.readFloat(); 
		tris[i].n2.y = di.readFloat();
		tris[i].n2.z = di.readFloat();

		tris[i].c0.x = di.readFloat(); 
		tris[i].c0.y = di.readFloat();
		tris[i].c0.z = di.readFloat();

		tris[i].c1.x = di.readFloat(); 
		tris[i].c1.y = di.readFloat();
		tris[i].c1.z = di.readFloat();

		tris[i].c2.x = di.readFloat(); 
		tris[i].c2.y = di.readFloat();
		tris[i].c2.z = di.readFloat();
	    }
	    
	    System.out.println("Done");
	}catch (IOException e) {
	    e.printStackTrace();
	}
	finally {
	    try {
		if (di != null)di.close();
	    } catch (Exception ee) {}
	}
	return ginfo;
    }

    static FileOutputStream fos;
    static DataOutputStream dos;

    public static int beginPPMWriter(String name, int w, int h) {
	try {
	    fos = new FileOutputStream(name);
	    dos = new DataOutputStream(fos);
	} catch (Exception ex) {
	    System.err.println("Could not open "+name+" for output!");
	    return -1;
	}

	try {
	    dos.writeBytes("P6\n");
	    dos.writeBytes(w+" "+h+"\n");
	    dos.writeBytes("255\n");
	} catch (IOException ex) {
	    System.err.println("Error while writing PPM file!");
	    java.lang.System.exit(-1);
	}
	return 0;
    }

    public static void sendToPPMWriter(int fid, float4 data) {
	try {
	    dos.writeByte((byte)(data.x * 255.0));
	    dos.writeByte((byte)(data.y * 255.0));
	    dos.writeByte((byte)(data.z * 255.0));
	} catch (IOException ex) {
	    System.err.println("Error while writing PPM file!");
	    java.lang.System.exit(-1);
	}
	return;
    } 
    
    public static void endPPMWriter(int fid) {
	if (fid == 0) {
	    try {
		dos.close();
		fos.close();
	    } catch (IOException ex) {
		System.err.println("Error while closing PPM file!");
		java.lang.System.exit(-1);
	    }
	}
    }

    public static int currentTimeMillis() {
	return (int)(java.lang.System.currentTimeMillis() & 0xffffffff);
    }

}
