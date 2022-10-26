package org.atsign.client.util;
import java.nio.charset.StandardCharsets;   //Importing UTF-8 Character Encoding

public class ByteUtil {
    public static String convert(byte[] data){ // Method to convert byte[] array to string
        try {
            String st = new String(data, StandardCharsets.UTF_8);   //Trying to parse the byte[] array 'data' to string
            return st;
        }catch (Exception e){   // In case if an error occurs while parsing the array
            System.out.println("Error occured while parsing the data to string ");
            e.printStackTrace();    // Printing the stack trace if any error occurs
            return null;
        }
    }
    public static byte[] convert(String data){ // Method to convert String to byte[] array
        try{
            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);   // Gets the byte value of the string passed , iterating through character by character and stores their byte value into byte array
            return bytes;
        }catch (Exception e){
            System.out.println("Error occured while parsing the string to byte array data");
            e.printStackTrace();
            return null;
        }
    }
}
