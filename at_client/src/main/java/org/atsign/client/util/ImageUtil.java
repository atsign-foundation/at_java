package org.atsign.client.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Optional;

/**
 *
 * 5/10/2022
 * Author- Kumar Aggarrwal
 *
 * ImageUtil
 *
 * This utility class can be used to convert buffered image to byte[] and vice versa, it can also be used to save image to memory
 * Image format and file location can be provided, default format is 'jpg' and default location would be classpath
 *
 */

public class ImageUtil {


    private static final String DEFAULT_FORMAT = "jpg";
    private static final String IMAGE = "image";

    /**
     * Converts BufferedIamge to byte[] with a given format
     * @param image
     * @param format
     * @return
     * @throws IOException
     */
    public static byte[] toByteArray(BufferedImage image, String format)
            throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        byte[] bytes = baos.toByteArray();
        return bytes;

    }

    /**
     * Converts BufferedIamge to byte[] with a given format
     * @param image
     * @return
     * @throws IOException
     */
    public static byte[] toByteArray(BufferedImage image)
            throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, DEFAULT_FORMAT, baos);
        byte[] bytes = baos.toByteArray();
        return bytes;

    }

    /**
     * Convert byte[] to bufferedImage
     * @param bytes
     * @return
     * @throws IOException
     */
    public static BufferedImage toBufferedImage(byte[] bytes)
            throws IOException {

        InputStream is = new ByteArrayInputStream(bytes);
        BufferedImage image = ImageIO.read(is);
        return image;

    }


    /**
     * Saves the image to the disk with image format
     * @param image
     * @param format
     * @param file
     * @throws IOException
     */
    public static void saveImage(BufferedImage image, String format, Optional<File> file) throws IOException {

        File fileToCreate = file.isPresent() ? file.get() : new File(IMAGE.concat(".").concat(format));
        ImageIO.write(image, format, fileToCreate);

    }

    /**
     * Saves the images to the disk with default format
     * @param image
     * @param file
     * @throws IOException
     */
    public static void saveImage(BufferedImage image, Optional<File> file) throws IOException {

        File fileToCreate = file.isPresent() ? file.get() : new File(IMAGE.concat(".").concat(DEFAULT_FORMAT));
        ImageIO.write(image, DEFAULT_FORMAT, fileToCreate);

    }



}
