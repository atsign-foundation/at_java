package org.atsign.client.util;

import java.io.*;

public class FileUtil {

	// This method converts a file to a byte array and returns the byte array
	public static byte[] convert(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		byte[] data = new byte[(int) file.length()];
		fis.read(data);
		fis.close();
		return data;
	}

	// This method converts a byte array to a file and returns the file
	public static File convert(byte data[], String fileName) throws IOException {
		File file = new File(fileName);
		OutputStream os = new FileOutputStream(file);
		os.write(data);
		os.close();
		return file;
	}
}
