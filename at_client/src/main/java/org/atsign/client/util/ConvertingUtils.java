import java.io.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

// Main class

public class ConvertingUtils{

     // Converting file object to byte array
    public static byte[] convert(File file)
        throws IOException
    {
  
        //creating an instance of file
        FileInputStream filel = new FileInputStream(file);
  
        //creating a byte array with same length as file
        byte[] byteArr = new byte[(int)file.length()];
  
       //Reading file content 
        filel.read(byteArr);
  
        //Closing the file
        filel.close();
  
        // Returning byte array
        return byteArr;
    }

    public static File convert(byte[] bytes)
    {
        //paste yout destination path here
        String FILEPATH="";  
        File file = new File(FILEPATH);
       
        try {
 
            // InitializING a pointer in file
            OutputStream ostream = new FileOutputStream(file);
            
            // Starting writing the bytes in it
            ostream.write(bytes);
            System.out.println("Byte is successfully inserted");
 
            ostream.close();
         
        }
 
        // Catch block to handle the exceptions
        catch (Exception e) {
 
            // Display exception on console
            System.out.println("Exception: " + e);
        }
        return file;
    }
}
