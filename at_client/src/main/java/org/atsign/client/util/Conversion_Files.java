// importing necessary packages

package org.atsign.client.util;

import java.io.*;
import java.nio.*;
import java.util.*;

public class Conversion_Files
{
	public static String extension;
	
	/*
	The main method does the following:
	1. Takes a file as input
	2. Converts the file to a byte array
	3. Reconstructs the file from the byte array and saves it as the output
	*/
	
	public static void main(String args[])throws IOException
	{
		BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
		String filename;
		int index;
		System.out.println("Enter the name of the input file: ");
		filename=br.readLine();
		File f=new File(filename);
		if(f.exists())
		{
			index=filename.lastIndexOf('.');
			extension=filename.substring(index+1);
			byte[] data=fileToByteArray(f);
			File file=byteArrayToFile(data);
			System.out.println("Reconstructed file saved as "+file.getName());
		}
		else
		{
			System.out.println("No such file exists!");
		}
	}
	
	// This method converts a file to a byte array and returns the byte array
	public static byte[] fileToByteArray(File file)throws IOException
	{
		FileInputStream fis=new FileInputStream(file);
        byte[] data=new byte[(int)file.length()];
        fis.read(data);
        fis.close();
		return data;
	}
	
	// This method converts a byte array to a file and returns the file
	public static File byteArrayToFile(byte data[])throws IOException
	{
		String filename="Output."+extension;
		File file=new File(filename);
		OutputStream os=new FileOutputStream(file);
        os.write(data);
        os.close();
		return file;
	}
}
