package org.atsign.client.util;

public class StringUtil {
    
    /**
     * 
     * Convert raw response of the scan verb to a String[] of key names
     * 
     * See StringUtilTest.java for more examples
     * 
     * Example:
     * String rawResponse = "[\"public:publickey@farinataanxious\",\"public:signing_publickey@farinataanxious\", \"@farinataanxious:signing_privatekey@farinataanxious\"]"
     * String[] keyNames = getRawStringArrayFromScanRawReponseString(rawResponse);
     * System.out.println(keyNames); // {"public:publickey@farinataanxious", "public:signing_publickey@farinataanxious", "@farinataanxious:signing_privatekey@farinataanxious"}
     * 
     * @param rawResponse == raw response string of scan verb
     * @return String[] each with a keyname
     */
    public static String[] getRawStringArrayFromScanRawResponseString(String rawResponseString) {
        rawResponseString = rawResponseString.replace("[", "");
        rawResponseString = rawResponseString.replace("]", "");
        rawResponseString = rawResponseString.replace("\"", "");
        
        String[] rawArray = rawResponseString.split(","); // eg: {"public:publickey@farinataanxious","public:signing_publickey@farinataanxious", "@farinataanxious:signing_privatekey@farinataanxious"}
        return rawArray;
    }

}
