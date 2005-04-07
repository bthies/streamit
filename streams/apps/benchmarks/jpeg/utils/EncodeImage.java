package standardizer;

import Encoding.JpegEncoder;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.*;

public class EncodeImage {

  public static void main(String[] args) {
  System.out.println("Encoder");
  try {
      OutputStream os = new FileOutputStream("input.jpg");
      Image image = Toolkit.getDefaultToolkit().getImage("output.jpg");
      JpegEncoder je = new JpegEncoder(image, 80, os);
      je.Compress();
    } catch (Exception e) {
      System.out.println("Some weird error");
    } 
        
    
  }

}
