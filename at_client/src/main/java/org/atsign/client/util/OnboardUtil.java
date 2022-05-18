package org.atsign.client.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class OnboardUtil {

	private static final String currAtSignFile = System.getProperty("user.dir") + "/.currAtSign";

	public static String getCurruntAtSign() {

		File file = new File(currAtSignFile);

		try (FileInputStream fileInputStream = new FileInputStream(file)) {

			byte byteArray[] = fileInputStream.readAllBytes();
			String currAtSign = new String(byteArray);
			System.out.println(currAtSign);
			return currAtSign;
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	public static void setCurruntAtSign(String atSign) throws IOException {

		File file = new File(currAtSignFile);
		if(!file.exists()) {
			 file.createNewFile();
		}
		
		try(FileOutputStream fileOutputStream = new FileOutputStream(file)){      
			
			byte byteArray[] = atSign.toString().getBytes(); //converting string into byte array      
			fileOutputStream.write(byteArray);  
		}    
	}
}
