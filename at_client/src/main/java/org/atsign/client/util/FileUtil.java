import java.nio.file.Files;
import java.io.File;

public class FileUtil {

    /**
     * Util function used to convert file object to byte array
     * 
     * @param filePath: the path of the file that need to be converted 
     *                  to an byte array.
     * @return byte[]
     * 
     */
    public static byte[] toArrayByte(String filePath) {
        try {
            File file = new File(filePath);
            return Files.readAllBytes(file.toPath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Util function used to convert byte array data to a file,
     * then store file object to a specified path.
     * 
     * @param destPath: where the image will be stored.
     * @param data: the actual data of the file.
     * @return void
     * 
     */
    public static void toFile(String destPath, byte[] data) {
        try {
            File dest = new File(destPath);
            Files.write(dest.toPath(), data);
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
