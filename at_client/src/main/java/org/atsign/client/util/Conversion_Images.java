// importing necessary packages

package org.atsign.client.util;

import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Conversion_Images
{
	/*
	The main method does the following:
	1. Takes an image file as input
	2. Converts the image to a byte array
	3. Reconstructs the image from the byte array and saves it as the output
	*/
	
	public static void main(String args[])throws IOException
	{
		BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
		String filename;
		System.out.println("Enter the name of the input image file: ");
		filename=br.readLine();
		File f=new File(filename);
		if(f.exists())
		{
			byte[] data=imageToByteArray(f);
			BufferedImage image=byteArrayToImage(data);
			File file=new File("Output.jpg");
			ImageIO.write(image,"jpg",file);
			System.out.println("Reconstructed image saved as "+file.getName());
		}
		else
		{
			System.out.println("No such file exists!");
		}
	}
	
	// This method converts an image to a byte array and returns the byte array
	public static byte[] imageToByteArray(File file)throws IOException
	{
		BufferedImage image=ImageIO.read(file);
		ByteArrayOutputStream bos=new ByteArrayOutputStream();
		ImageIO.write(image,"jpg",bos);
		byte[] data=bos.toByteArray();
		return data;
	}
	
	// This method converts a byte array to an image and returns the image file
	public static BufferedImage byteArrayToImage(byte data[])throws IOException
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		BufferedImage image=ImageIO.read(bis);
		return image;
	}
}
