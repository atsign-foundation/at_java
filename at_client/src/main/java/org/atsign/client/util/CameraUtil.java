package org.atsign.client.util;

import com.github.sarxos.webcam.Webcam;
import org.apache.commons.lang3.StringUtils;
import org.atsign.common.exceptions.AtClientConfigException;
import org.atsign.common.AtException;


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 5/10/2022
 * Author: Kumar Aggarrwal
 * <p>
 * CameraUtil
 * <p>
 * This Utility class can be used to capture a stream from the camera and get the result in list of byte[] OR capture a single image from camera into byte[]
 * Camera name can be provided to chose which camera would be used to capture.
 * <p>
 * usage:
 * CameraUtil.getCameraStream(); //For DefaultCamera
 * CameraUtil.getCameraStream(String cameraName); for a specific camera
 * CameraUtil.getCaptureSingleImage(); //For DefaultCamera
 * CameraUtil.getCaptureSingleImage(String cameraName); //For a specific camera
 * <p>
 * //example for camera Name: "Webcam HD Webcam: HD Webcam /dev/video0"
 * //Check you system drivers to identify camera name or use getAllCams method here
 */

@SuppressWarnings("unused")
public class CameraUtil {

    static Webcam webcam = null;

    /**
     * captures a single image from default camera
     */
    public static BufferedImage getSingleImage() {
        webcam = getWebcam(StringUtils.EMPTY);
        if (!webcam.isOpen()) {
            webcam.open();
        }
        return webcam.getImage();
    }

    /**
     * captures a single image from default camera
     */
    public static BufferedImage getSingleImage(String cameraName) {
        webcam = getWebcam(cameraName);
        if (!webcam.isOpen()) {
            webcam.open();
        }
        return webcam.getImage();
    }


    /**
     * This return all webcams connected to the system
     */
    public static List<String> getAllCams() {
        //noinspection Convert2MethodRef
        return Webcam.getWebcams().stream().map(webcam1 -> webcam1.getName()).collect(Collectors.toList());
    }

    /**
     * closes the camera if opened
     */
    public static void closeCamera() {
        if (webcam.isOpen()) webcam.close();
    }

    /**
     * return the camera stream as list of byte[] from a specific camera
     */
    public static List<byte[]> getCameraStream(String cameraName) throws AtException, IOException {
        webcam = getWebcam(cameraName);
        validateCamera(webcam);
        webcam.open();
        List<byte[]> stream = captureStream(webcam);
        closeCamera();
        return stream;
    }


    private static List<byte[]> captureStream(Webcam webcam) throws IOException {
        List<byte[]> resultList = new ArrayList<>();

        //This message could be configured via properties later
        System.out.println("Stream capture starting...\n Press Enter to stop the capture and return the result in byte array");
        while (System.in.available() == 0) {
            resultList.add(ImageUtil.toByteArray(getSingleImage()));

        }
        return resultList;
    }

    /**
     * return the camera stream into list of byte[] from a default camera
     */
    public static List<byte[]> getCameraStream() throws AtException, IOException {
        webcam = getWebcam(StringUtils.EMPTY);
        validateCamera(webcam);
        webcam.open();
        List<byte[]> stream = captureStream(webcam);
        closeCamera();
        return stream;
    }

    private static void validateCamera(Webcam webcam) throws AtException {
        if (webcam == null) {
            throw new AtClientConfigException("WebCam is not detected, Please unlock your camera");
        }
    }

    private static Webcam getWebcam(String name) {
        Webcam webcam;
        if (StringUtils.isEmpty(name)) {
            webcam = Webcam.getDefault();
        } else {
            webcam = Webcam.getWebcamByName(name);
        }
        return webcam;
    }
}
