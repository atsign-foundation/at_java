import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImageConversion {

	
	public byte[] extractBytes (String ImageName) throws IOException {
			 File path = new File(ImageName);
			 BufferedImage image = ImageIO.read(path);
	
			 WritableRaster raster = image .getRaster();
			 DataBufferByte bytes   = (DataBufferByte) raster.getDataBuffer();
	
			 return ( bytes.getData() );
	}

	public BufferedImage byteArrayToImage(byte[] byteArr) throws IOException {
		
		return ImageIO.read(new ByteArrayInputStream(byteArr));
		
		
	}

}
