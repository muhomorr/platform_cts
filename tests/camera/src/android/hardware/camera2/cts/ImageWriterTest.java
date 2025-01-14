/*
 * Copyright 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.hardware.camera2.cts;

import static android.hardware.camera2.cts.CameraTestUtils.*;

import static junit.framework.Assert.*;

import static org.junit.Assume.assumeNotNull;
import static org.junit.Assume.assumeTrue;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.DataSpace;
import android.hardware.HardwareBuffer;
import android.hardware.SyncFence;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.cts.CameraTestUtils.SimpleCaptureCallback;
import android.hardware.camera2.cts.testcases.Camera2AndroidTestCase;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageWriter;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.android.cts.hardware.SyncFenceUtil;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Basic test for ImageWriter APIs. ImageWriter takes the images produced by
 * camera (via ImageReader), then the data is consumed by either camera input
 * interface or ImageReader.
 * </p>
 */
@RunWith(Parameterized.class)
public class ImageWriterTest extends Camera2AndroidTestCase {
    private static final String TAG = "ImageWriterTest";
    private static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    // Max number of images can be accessed simultaneously from ImageReader.
    private static final int MAX_NUM_IMAGES = 3;
    private static final int CAMERA_PRIVATE_FORMAT = ImageFormat.PRIVATE;
    private static final int BUFFER_WIDTH = 640;
    private static final int BUFFER_HEIGHT = 480;
    private ImageReader mReaderForWriter;
    private ImageWriter mWriter;

    @Override
    public void tearDown() throws Exception {
        try {
            closeImageReader(mReaderForWriter);
        } finally {
            mReaderForWriter = null;
            if (mWriter != null) {
                mWriter.close();
                mWriter = null;
            }
        }

        super.tearDown();
    }

    /**
     * <p>
     * Basic YUV420_888 format ImageWriter ImageReader test that checks the
     * images produced by camera can be passed correctly by ImageWriter.
     * </p>
     * <p>
     * {@link ImageReader} reads the images produced by {@link CameraDevice}.
     * The images are then passed to ImageWriter, which produces new images that
     * are consumed by the second image reader. The images from first
     * ImageReader should be identical with the images from the second
     * ImageReader. This validates the basic image input interface of the
     * ImageWriter. Below is the data path tested:
     * <li>Explicit data copy: Dequeue an image from ImageWriter, copy the image
     * data from first ImageReader into this image, then queue this image back
     * to ImageWriter. This validates the ImageWriter explicit buffer copy
     * interface.</li>
     * </p>
     */
    @Test
    public void testYuvImageWriterReaderOperation() throws Exception {
        for (String id : mCameraIdsUnderTest) {
            try {
                Log.i(TAG, "Testing Camera " + id);
                if (!mAllStaticInfo.get(id).isColorOutputSupported()) {
                    Log.i(TAG, "Camera " + id + " does not support color outputs, skipping");
                    continue;
                }
                openDevice(id);
                readerWriterFormatTestByCamera(ImageFormat.YUV_420_888, false);
            } finally {
                closeDevice(id);
            }
        }
    }

    /**
     * <p>
     * Similar to testYuvImageWriterReaderOperation, but use the alternative
     * factory method of ImageReader and ImageWriter.
     * </p>
     */
    @Test
    public void testYuvImageWriterReaderOperationAlt() throws Exception {
        for (String id : mCameraIdsUnderTest) {
            try {
                Log.i(TAG, "Testing Camera " + id);
                if (!mAllStaticInfo.get(id).isColorOutputSupported()) {
                    Log.i(TAG, "Camera " + id + " does not support color outputs, skipping");
                    continue;
                }
                openDevice(id);
                readerWriterFormatTestByCamera(ImageFormat.YUV_420_888, true);
            } finally {
                closeDevice(id);
            }
        }
    }

    @Test
    public void testAbandonedSurfaceExceptions() throws Exception {
        final int READER_WIDTH = 1920;
        final int READER_HEIGHT = 1080;
        final int READER_FORMAT = ImageFormat.YUV_420_888;

        // Verify that if the image writer's input surface is abandoned, dequeueing an image
        // throws IllegalStateException
        ImageReader reader = ImageReader.newInstance(READER_WIDTH, READER_HEIGHT, READER_FORMAT,
                MAX_NUM_IMAGES);
        ImageWriter writer = ImageWriter.newInstance(reader.getSurface(), MAX_NUM_IMAGES);

        // Close image reader to abandon the input surface.
        reader.close();

        Image image;
        try {
            image = writer.dequeueInputImage();
            fail("Should get an IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        } finally {
            writer.close();
        }

        // Verify that if the image writer's input surface is abandoned, queueing an image
        // throws IllegalStateException
        reader = ImageReader.newInstance(READER_WIDTH, READER_HEIGHT, READER_FORMAT,
                MAX_NUM_IMAGES);
        writer = ImageWriter.newInstance(reader.getSurface(), MAX_NUM_IMAGES);
        image = writer.dequeueInputImage();

        // Close image reader to abandon the input surface.
        reader.close();

        try {
            writer.queueInputImage(image);
            fail("Should get an IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        } finally {
            writer.close();
        }
    }

    @Test
    public void testWriterReaderBlobFormats() throws Exception {
        int[] READER_TEST_FORMATS = {ImageFormat.JPEG, ImageFormat.DEPTH_JPEG,
                                     ImageFormat.HEIC, ImageFormat.DEPTH_POINT_CLOUD};

        for (int format : READER_TEST_FORMATS) {
            ImageReader reader = ImageReader.newInstance(640, 480, format, 1 /*maxImages*/);
            ImageWriter writer = ImageWriter.newInstance(reader.getSurface(), 1 /*maxImages*/);
            writer.close();
            reader.close();
        }
    }

    @Test
    public void testWriterFormatOverride() throws Exception {
        int[] TEXTURE_TEST_FORMATS = {ImageFormat.YV12, ImageFormat.YUV_420_888};
        SurfaceTexture texture = new SurfaceTexture(false);
        texture.setDefaultBufferSize(BUFFER_WIDTH, BUFFER_HEIGHT);
        Surface surface = new Surface(texture);

        // Make sure that the default newInstance is still valid.
        ImageWriter defaultWriter = ImageWriter.newInstance(surface, MAX_NUM_IMAGES);
        Image defaultImage = defaultWriter.dequeueInputImage();
        defaultWriter.close();

        for (int format : TEXTURE_TEST_FORMATS) {
            // Override default buffer format of Surface texture to test format
            ImageWriter writer = ImageWriter.newInstance(surface, MAX_NUM_IMAGES, format);
            Image image = writer.dequeueInputImage();
            Log.i(TAG, "testing format " + format + ", got input image format " +
                    image.getFormat());
            assertTrue(image.getFormat() == format);
            writer.close();
        }
    }

    @Test
    public void testWriterWithImageFormatOverride() throws Exception {
        final int imageReaderFormat = ImageFormat.YUV_420_888;
        final int imageWriterFormat = ImageFormat.YV12;
        final int dataSpace = DataSpace.DATASPACE_JFIF;
        final int hardwareBufferFormat = ImageFormat.YV12;
        try (
            ImageReader reader = new ImageReader
                .Builder(BUFFER_WIDTH, BUFFER_HEIGHT)
                .setImageFormat(imageReaderFormat)
                .build();
            ImageWriter writer = new ImageWriter
                .Builder(reader.getSurface())
                .setImageFormat(imageWriterFormat)
                .build();
            Image outputImage = writer.dequeueInputImage()
        ) {
            assertEquals(1, reader.getMaxImages());
            assertEquals(imageReaderFormat, reader.getImageFormat());
            assertEquals(dataSpace, reader.getDataSpace());

            assertEquals(HardwareBuffer.USAGE_CPU_READ_OFTEN, reader.getUsage());
            assertEquals(dataSpace, writer.getDataSpace());
            assertEquals(imageWriterFormat, writer.getFormat());

            assertEquals(BUFFER_WIDTH, outputImage.getWidth());
            assertEquals(BUFFER_HEIGHT, outputImage.getHeight());
            assertEquals(imageWriterFormat, outputImage.getFormat());
            assertEquals(dataSpace, outputImage.getDataSpace());
        }
    }

    @Test
    public void testWriterBuilderDefault() throws Exception {
        try (
            ImageReader reader = new ImageReader
                .Builder(BUFFER_WIDTH, BUFFER_HEIGHT)
                .setImageFormat(ImageFormat.HEIC)
                .setDefaultHardwareBufferFormat(HardwareBuffer.RGBA_8888)
                .setDefaultDataSpace(DataSpace.DATASPACE_BT709)
                .build();
            ImageWriter writer = new ImageWriter
                .Builder(reader.getSurface())
                .build();
            Image outputImage = writer.dequeueInputImage()
        ) {
            assertEquals(1, reader.getMaxImages()); // default maxImages
            assertEquals(HardwareBuffer.USAGE_CPU_READ_OFTEN, reader.getUsage()); // default usage
            assertEquals(HardwareBuffer.RGBA_8888, reader.getHardwareBufferFormat());
            assertEquals(DataSpace.DATASPACE_BT709, reader.getDataSpace());

            assertEquals(BUFFER_WIDTH, outputImage.getWidth());
            assertEquals(BUFFER_HEIGHT, outputImage.getHeight());
            assertEquals(HardwareBuffer.RGBA_8888, outputImage.getFormat());
        }
    }

    @Test
    public void testWriterBuilderSetImageFormatAndSize() throws Exception {
        SurfaceTexture texture = new SurfaceTexture(false);
        texture.setDefaultBufferSize(BUFFER_WIDTH, BUFFER_HEIGHT);
        Surface surface = new Surface(texture);
        final int imageWriterWidth = 20;
        final int imageWriterHeight = 50;

        long usage = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE;
        try (
            ImageWriter writer = new ImageWriter
                .Builder(surface)
                .setWidthAndHeight(imageWriterWidth, imageWriterHeight)
                .setMaxImages(MAX_NUM_IMAGES)
                .setImageFormat(ImageFormat.YV12)
                .setUsage(usage)
                .build();
            Image image = writer.dequeueInputImage()
        ) {
            // ImageFormat.YV12 HAL dataspace is DataSpace.DATASPACE_JFIF
            assertEquals(imageWriterWidth, writer.getWidth());
            assertEquals(imageWriterHeight, writer.getHeight());
            assertEquals(MAX_NUM_IMAGES, writer.getMaxImages());
            assertEquals(DataSpace.DATASPACE_JFIF, writer.getDataSpace());
            assertEquals(usage, writer.getUsage());

            assertEquals(DataSpace.DATASPACE_JFIF, image.getDataSpace());
            assertEquals(ImageFormat.YV12, image.getFormat());
            assertEquals(imageWriterWidth, image.getWidth());
        }
    }

    @Test
    public void testWriterBuilderSetHardwareBufferFormatAndDataSpace() throws Exception {
        SurfaceTexture texture = new SurfaceTexture(false);
        texture.setDefaultBufferSize(BUFFER_WIDTH, BUFFER_HEIGHT);
        Surface surface = new Surface(texture);

        long usage = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE | HardwareBuffer.USAGE_GPU_COLOR_OUTPUT;
        try (
            ImageWriter writer = new ImageWriter
                .Builder(surface)
                .setImageFormat(ImageFormat.YV12)
                .setHardwareBufferFormat(HardwareBuffer.RGBA_8888)
                .setDataSpace(DataSpace.DATASPACE_BT709)
                .setUsage(usage)
                .build();
            Image image = writer.dequeueInputImage()
        ) {
            assertEquals(BUFFER_WIDTH, writer.getWidth());
            assertEquals(BUFFER_HEIGHT, writer.getHeight());
            assertEquals(DataSpace.DATASPACE_BT709, writer.getDataSpace());
            assertEquals(HardwareBuffer.RGBA_8888, writer.getHardwareBufferFormat());
            assertEquals(usage, writer.getUsage());

            assertEquals(DataSpace.DATASPACE_BT709, image.getDataSpace());
            assertEquals(BUFFER_WIDTH, image.getWidth());
            assertEquals(BUFFER_HEIGHT, image.getHeight());
        }
    }

    @Test
    public void testWriterBuilderWithBLOB() throws Exception {
        SurfaceTexture texture = new SurfaceTexture(false);
        texture.setDefaultBufferSize(BUFFER_WIDTH, BUFFER_HEIGHT);
        Surface surface = new Surface(texture);

        long usage = HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE | HardwareBuffer.USAGE_GPU_COLOR_OUTPUT;
        try (
            ImageWriter writer = new ImageWriter
                .Builder(surface)
                .setHardwareBufferFormat(HardwareBuffer.BLOB)
                .setDataSpace(DataSpace.DATASPACE_JFIF)
                .setUsage(usage)
                .build();
        ) {
            assertEquals(BUFFER_WIDTH, writer.getWidth());
            assertEquals(BUFFER_HEIGHT, writer.getHeight());
            assertEquals(DataSpace.DATASPACE_JFIF, writer.getDataSpace());
            assertEquals(HardwareBuffer.BLOB, writer.getHardwareBufferFormat());
            assertEquals(ImageFormat.JPEG, writer.getFormat());
        }
    }

    @Test
    public void testGetFence() throws Exception {
        try (
            ImageReader reader = new ImageReader
                .Builder(20, 45)
                .setMaxImages(2)
                .setImageFormat(ImageFormat.YUV_420_888)
                .build();
            ImageWriter writer = new ImageWriter
                .Builder(reader.getSurface())
                .build();
            Image outputImage = writer.dequeueInputImage()
        ) {
            assertEquals(false, outputImage.getFence().isValid());
        }
    }

    @Test
    public void testSetFence() throws Exception {
        SyncFence fence = SyncFenceUtil.createUselessFence();
        assumeNotNull(fence);

        SurfaceTexture texture = new SurfaceTexture(false);
        texture.setDefaultBufferSize(BUFFER_WIDTH, BUFFER_HEIGHT);
        Surface surface = new Surface(texture);
        // fence may not be valid on cuttlefish using swiftshader
        assumeTrue(fence.isValid());

        try (
            ImageWriter writer = new ImageWriter
                    .Builder(surface)
                    .build();
            Image outputImage = writer.dequeueInputImage()
        ) {
            outputImage.setFence(fence);
            assertEquals(fence.getSignalTime(), outputImage.getFence().getSignalTime());
        }
    }

    @Test
    public void testGetPlanesAndFence() throws Exception {
        try (
            ImageReader reader = new ImageReader
                    .Builder(BUFFER_WIDTH, BUFFER_HEIGHT)
                    .build();
            ImageWriter writer = new ImageWriter
                    .Builder(reader.getSurface())
                    .build();
            Image outputImage = writer.dequeueInputImage();
        ) {
            outputImage.getPlanes();
            assertEquals(false, outputImage.getFence().isValid());
        }
    }

    private void readerWriterFormatTestByCamera(int format, boolean altFactoryMethod)
            throws Exception {
        List<Size> sizes = getSortedSizesForFormat(mCamera.getId(), mCameraManager, format, null);
        Size maxSize = sizes.get(0);
        if (VERBOSE) {
            Log.v(TAG, "Testing size " + maxSize);
        }

        // Create ImageReader for camera output.
        SimpleImageReaderListener listenerForCamera  = new SimpleImageReaderListener();
        if (altFactoryMethod) {
            createDefaultImageReader(maxSize, format, MAX_NUM_IMAGES,
                    HardwareBuffer.USAGE_CPU_READ_OFTEN, listenerForCamera);
        } else {
            createDefaultImageReader(maxSize, format, MAX_NUM_IMAGES, listenerForCamera);
        }

        if (VERBOSE) {
            Log.v(TAG, "Created camera output ImageReader");
        }

        // Create ImageReader for ImageWriter output
        SimpleImageReaderListener listenerForWriter  = new SimpleImageReaderListener();
        if (altFactoryMethod) {
            mReaderForWriter = createImageReader(
                    maxSize, format, MAX_NUM_IMAGES,
                    HardwareBuffer.USAGE_CPU_READ_OFTEN, listenerForWriter);
        } else {
            mReaderForWriter = createImageReader(
                    maxSize, format, MAX_NUM_IMAGES, listenerForWriter);
        }

        if (VERBOSE) {
            Log.v(TAG, "Created ImageWriter output ImageReader");
        }

        // Create ImageWriter
        Surface surface = mReaderForWriter.getSurface();
        assertNotNull("Surface from ImageReader shouldn't be null", surface);
        if (altFactoryMethod) {
            mWriter = ImageWriter.newInstance(surface, MAX_NUM_IMAGES, format);
        } else {
            mWriter = ImageWriter.newInstance(surface, MAX_NUM_IMAGES);
        }
        SimpleImageWriterListener writerImageListener = new SimpleImageWriterListener(mWriter);
        mWriter.setOnImageReleasedListener(writerImageListener, mHandler);

        // Start capture: capture 2 images.
        List<Surface> outputSurfaces = new ArrayList<Surface>();
        outputSurfaces.add(mReader.getSurface());
        CaptureRequest.Builder requestBuilder = prepareCaptureRequestForSurfaces(outputSurfaces,
                CameraDevice.TEMPLATE_PREVIEW);
        SimpleCaptureCallback captureListener = new SimpleCaptureCallback();
        // Capture 1st image.
        startCapture(requestBuilder.build(), /*repeating*/false, captureListener, mHandler);
        // Capture 2nd image.
        startCapture(requestBuilder.build(), /*repeating*/false, captureListener, mHandler);
        if (VERBOSE) {
            Log.v(TAG, "Submitted 2 captures");
        }

        // Image from the first ImageReader.
        Image cameraImage = null;
        // ImageWriter input image.
        Image inputImage = null;
        // Image from the second ImageReader.
        Image outputImage = null;
        assertTrue("ImageWriter max images should be " + MAX_NUM_IMAGES,
                mWriter.getMaxImages() == MAX_NUM_IMAGES);
        if (format == CAMERA_PRIVATE_FORMAT) {
            assertTrue("First ImageReader format should be PRIVATE",
                    mReader.getImageFormat() == CAMERA_PRIVATE_FORMAT);
            assertTrue("Second ImageReader should be PRIVATE",
                    mReaderForWriter.getImageFormat() == CAMERA_PRIVATE_FORMAT);
            assertTrue("Format of first ImageReader should be PRIVATE",
                    mReader.getImageFormat() == CAMERA_PRIVATE_FORMAT);
            assertTrue(" Format of second ImageReader should be PRIVATE",
                    mReaderForWriter.getImageFormat() == CAMERA_PRIVATE_FORMAT);
            assertTrue(" Format of ImageWriter should be PRIVATE",
                    mWriter.getFormat() == CAMERA_PRIVATE_FORMAT);

            // Validate 2 images
            validateOpaqueImages(maxSize, listenerForCamera, listenerForWriter, captureListener,
                    /*numImages*/2, writerImageListener);
        } else {
            // Test case 1: Explicit data copy, only applicable for explicit formats.

            // Get 1st image from first ImageReader, and copy the data to ImageWrtier input image
            cameraImage = listenerForCamera.getImage(CAPTURE_IMAGE_TIMEOUT_MS);
            inputImage = mWriter.dequeueInputImage();
            inputImage.setTimestamp(cameraImage.getTimestamp());
            if (VERBOSE) {
                Log.v(TAG, "Image is being copied");
            }
            imageCopy(cameraImage, inputImage);
            if (VERBOSE) {
                Log.v(TAG, "Image copy is done");
            }
            mCollector.expectTrue(
                    "ImageWriter 1st input image should match camera 1st output image",
                    isImageStronglyEqual(inputImage, cameraImage));

            if (DEBUG) {
                String inputFileName = mDebugFileNameBase + "/" + maxSize + "_image1_input.yuv";
                dumpFile(inputFileName, getDataFromImage(inputImage));
            }

            // Image should be closed after queueInputImage call
            Plane closedPlane = inputImage.getPlanes()[0];
            ByteBuffer closedBuffer = closedPlane.getBuffer();
            mWriter.queueInputImage(inputImage);
            imageInvalidAccessTestAfterClose(inputImage, closedPlane, closedBuffer);

            outputImage = listenerForWriter.getImage(CAPTURE_IMAGE_TIMEOUT_MS);
            mCollector.expectTrue("ImageWriter 1st output image should match 1st camera image",
                    isImageStronglyEqual(cameraImage, outputImage));
            if (DEBUG) {
                String img1FileName = mDebugFileNameBase + "/" + maxSize + "_image1_camera.yuv";
                String outputImg1FileName = mDebugFileNameBase + "/" + maxSize
                        + "_image1_output.yuv";
                dumpFile(img1FileName, getDataFromImage(cameraImage));
                dumpFile(outputImg1FileName, getDataFromImage(outputImage));
            }
            // No need to close inputImage, as it is sent to the surface after queueInputImage;
            cameraImage.close();
            outputImage.close();

            // Make sure ImageWriter listener callback is fired.
            writerImageListener.waitForImageReleased(CAPTURE_IMAGE_TIMEOUT_MS);

            // Test case 2: Directly inject the image into ImageWriter: works for all formats.

            // Get 2nd image and queue it directly to ImageWrier
            cameraImage = listenerForCamera.getImage(CAPTURE_IMAGE_TIMEOUT_MS);
            // make a copy of image1 data, as it will be closed after queueInputImage;
            byte[] img1Data = getDataFromImage(cameraImage);
            if (DEBUG) {
                String img2FileName = mDebugFileNameBase + "/" + maxSize + "_image2_camera.yuv";
                dumpFile(img2FileName, img1Data);
            }

            // Image should be closed after queueInputImage call
            closedPlane = cameraImage.getPlanes()[0];
            closedBuffer = closedPlane.getBuffer();
            mWriter.queueInputImage(cameraImage);
            imageInvalidAccessTestAfterClose(cameraImage, closedPlane, closedBuffer);

            outputImage = listenerForWriter.getImage(CAPTURE_IMAGE_TIMEOUT_MS);
            byte[] outputImageData = getDataFromImage(outputImage);

            mCollector.expectTrue("ImageWriter 2nd output image should match camera "
                    + "2nd output image", Arrays.equals(img1Data, outputImageData));

            if (DEBUG) {
                String outputImgFileName = mDebugFileNameBase + "/" + maxSize +
                        "_image2_output.yuv";
                dumpFile(outputImgFileName, outputImageData);
            }
            // No need to close inputImage, as it is sent to the surface after queueInputImage;
            outputImage.close();

            // Make sure ImageWriter listener callback is fired.
            writerImageListener.waitForImageReleased(CAPTURE_IMAGE_TIMEOUT_MS);
        }

        stopCapture(/*fast*/false);
        mReader.close();
        mReader = null;
        mReaderForWriter.close();
        mReaderForWriter = null;
        mWriter.close();
        mWriter = null;
    }

    private void validateOpaqueImages(Size maxSize, SimpleImageReaderListener listenerForCamera,
            SimpleImageReaderListener listenerForWriter, SimpleCaptureCallback captureListener,
            int numImages, SimpleImageWriterListener writerListener) throws Exception {
        Image cameraImage;
        Image outputImage;
        for (int i = 0; i < numImages; i++) {
            cameraImage = listenerForCamera.getImage(CAPTURE_IMAGE_TIMEOUT_MS);
            CaptureResult result = captureListener.getCaptureResult(CAPTURE_IMAGE_TIMEOUT_MS);
            validateOpaqueImage(cameraImage, "Opaque image " + i + "from camera: ", maxSize,
                    result);
            mWriter.queueInputImage(cameraImage);
            // Image should be closed after queueInputImage
            imageInvalidAccessTestAfterClose(cameraImage,
                    /*closedPlane*/null, /*closedBuffer*/null);
            outputImage = listenerForWriter.getImage(CAPTURE_IMAGE_TIMEOUT_MS);
            validateOpaqueImage(outputImage, "First Opaque image output by ImageWriter: ",
                    maxSize, result);
            outputImage.close();
            writerListener.waitForImageReleased(CAPTURE_IMAGE_TIMEOUT_MS);
        }
    }

    private void validateOpaqueImage(Image image, String msg, Size imageSize,
            CaptureResult result) {
        assertNotNull("Opaque image Capture result should not be null", result != null);
        mCollector.expectImageProperties(msg + "Opaque ", image, CAMERA_PRIVATE_FORMAT,
                imageSize, result.get(CaptureResult.SENSOR_TIMESTAMP));
        mCollector.expectTrue(msg + "Opaque image number planes should be zero",
                image.getPlanes().length == 0);
    }
}
