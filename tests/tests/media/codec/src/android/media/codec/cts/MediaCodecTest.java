/*
 * Copyright (C) 2013 The Android Open Source Project
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

package android.media.codec.cts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.res.AssetFileDescriptor;
import android.hardware.HardwareBuffer;
import android.media.AudioFormat;
import android.media.AudioPresentation;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodec.CodecException;
import android.media.MediaCodec.CryptoException;
import android.media.MediaCodec.CryptoInfo;
import android.media.MediaCodec.CryptoInfo.Pattern;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.AudioCapabilities;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaCodecInfo.EncoderCapabilities;
import android.media.MediaCodecInfo.VideoCapabilities;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.cts.InputSurface;
import android.media.cts.OutputSurface;
import android.media.cts.StreamUtils;
import android.media.cts.TestUtils;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.Presubmit;
import android.platform.test.annotations.RequiresDevice;
import android.util.Log;
import android.util.Range;
import android.view.Surface;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;

import com.android.compatibility.common.util.ApiLevelUtil;
import com.android.compatibility.common.util.ApiTest;
import com.android.compatibility.common.util.MediaUtils;
import com.android.compatibility.common.util.Preconditions;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * General MediaCodec tests.
 *
 * In particular, check various API edge cases.
 */
@Presubmit
@SmallTest
@RequiresDevice
@AppModeFull(reason = "Instant apps cannot access the SD card")
@RunWith(AndroidJUnit4.class)
public class MediaCodecTest {
    private static final String TAG = "MediaCodecTest";
    private static final boolean VERBOSE = false;           // lots of logging

    static final String mInpPrefix = WorkDir.getMediaDirString();
    // parameters for the video encoder
                                                            // H.264 Advanced Video Coding
    private static final String MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC;
    private static final int BIT_RATE = 2000000;            // 2Mbps
    private static final int FRAME_RATE = 15;               // 15fps
    private static final int IFRAME_INTERVAL = 10;          // 10 seconds between I-frames
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    // parameters for the audio encoder
    private static final String MIME_TYPE_AUDIO = MediaFormat.MIMETYPE_AUDIO_AAC;
    private static final int AUDIO_SAMPLE_RATE = 44100;
    private static final int AUDIO_AAC_PROFILE = 2; /* OMX_AUDIO_AACObjectLC */
    private static final int AUDIO_CHANNEL_COUNT = 2; // mono
    private static final int AUDIO_BIT_RATE = 128000;

    private static final int TIMEOUT_USEC = 100000;
    private static final int TIMEOUT_USEC_SHORT = 100;

    private boolean mVideoEncoderHadError = false;
    private boolean mAudioEncoderHadError = false;
    private volatile boolean mVideoEncodingOngoing = false;

    private static final String INPUT_RESOURCE =
            "video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz.mp4";

    // The test should fail if the decoder never produces output frames for the input.
    // Time out decoding, as we have no way to query whether the decoder will produce output.
    private static final int DECODING_TIMEOUT_MS = 10000;

    private static boolean mIsAtLeastR = ApiLevelUtil.isAtLeast(Build.VERSION_CODES.R);
    private static boolean mIsAtLeastS = ApiLevelUtil.isAtLeast(Build.VERSION_CODES.S);
    private static boolean mIsAtLeastU = ApiLevelUtil.isAfter(Build.VERSION_CODES.TIRAMISU);

    /**
     * Tests:
     * <br> Exceptions for MediaCodec factory methods
     * <br> Exceptions for MediaCodec methods when called in the incorrect state.
     *
     * A selective test to ensure proper exceptions are thrown from MediaCodec
     * methods when called in incorrect operational states.
     */
    @ApiTest(apis = {"MediaCodec#createByCodecName", "MediaCodec#createDecoderByType",
            "MediaCodec#createEncoderByType", "MediaCodec#start", "MediaCodec#flush",
            "MediaCodec#configure", "MediaCodec#dequeueInputBuffer",
            "MediaCodec#dequeueOutputBuffer", "MediaCodec#createInputSurface",
            "MediaCodec#getInputBuffers", "MediaCodec#getQueueRequest",
            "MediaCodec#getOutputFrame", "MediaCodec#stop", "MediaCodec#release",
            "MediaCodec#getCodecInfo", "MediaCodec#getSupportedVendorParameters",
            "MediaCodec#getParameterDescriptor",
            "MediaCodec#subscribeToVendorParameters",
            "MediaCodec#unsubscribeFromVendorParameters",
            "MediaCodec#getInputBuffer", "MediaCodec#getOutputBuffer",
            "MediaCodec#setCallback", "MediaCodec#getName"})
    @Test
    public void testException() throws Exception {
        boolean tested = false;
        // audio decoder (MP3 should be present on all Android devices)
        MediaFormat format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_MPEG, 44100 /* sampleRate */, 2 /* channelCount */);
        tested = verifyException(format, false /* isEncoder */) || tested;

        // audio encoder (AMR-WB may not be present on some Android devices)
        format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_AMR_WB, 16000 /* sampleRate */, 1 /* channelCount */);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 19850);
        tested = verifyException(format, true /* isEncoder */) || tested;

        // video decoder (H.264/AVC may not be present on some Android devices)
        format = createMediaFormat();
        tested = verifyException(format, false /* isEncoder */) || tested;

        // video encoder (H.264/AVC may not be present on some Android devices)
        tested = verifyException(format, true /* isEncoder */) || tested;

        // signal test is skipped due to no device media codecs.
        if (!tested) {
            MediaUtils.skipTest(TAG, "cannot find any compatible device codecs");
        }
    }

    // wrap MediaCodec encoder and decoder creation
    private static MediaCodec createCodecByType(String type, boolean isEncoder)
            throws IOException {
        if (isEncoder) {
            return MediaCodec.createEncoderByType(type);
        }
        return MediaCodec.createDecoderByType(type);
    }

    private static void logMediaCodecException(MediaCodec.CodecException ex) {
        if (ex.isRecoverable()) {
            Log.w(TAG, "CodecException Recoverable: " + ex.getErrorCode());
        } else if (ex.isTransient()) {
            Log.w(TAG, "CodecException Transient: " + ex.getErrorCode());
        } else {
            Log.w(TAG, "CodecException Fatal: " + ex.getErrorCode());
        }
    }

    private static boolean verifyException(MediaFormat format, boolean isEncoder)
            throws IOException {
        String mimeType = format.getString(MediaFormat.KEY_MIME);
        if (!supportsCodec(mimeType, isEncoder)) {
            Log.i(TAG, "No " + (isEncoder ? "encoder" : "decoder")
                    + " found for mimeType= " + mimeType);
            return false;
        }

        final boolean isVideoEncoder = isEncoder && mimeType.startsWith("video/");

        if (isVideoEncoder) {
            format = new MediaFormat(format);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        }

        // create codec (enter Initialized State)
        MediaCodec codec;

        // create improperly
        final String methodName = isEncoder ? "createEncoderByType" : "createDecoderByType";
        try {
            codec = createCodecByType(null, isEncoder);
            fail(methodName + " should return NullPointerException on null");
        } catch (NullPointerException e) { // expected
        }
        try {
            codec = createCodecByType("foobarplan9", isEncoder); // invalid type
            fail(methodName + " should return IllegalArgumentException on invalid type");
        } catch (IllegalArgumentException e) { // expected
        }
        try {
            codec = MediaCodec.createByCodecName("foobarplan9"); // invalid name
            fail(methodName + " should return IllegalArgumentException on invalid name");
        } catch (IllegalArgumentException e) { // expected
        }
        // correct
        codec = createCodecByType(format.getString(MediaFormat.KEY_MIME), isEncoder);

        // test a few commands
        try {
            codec.start();
            fail("start should return IllegalStateException when in Initialized state");
        } catch (MediaCodec.CodecException e) {
            logMediaCodecException(e);
            fail("start should not return MediaCodec.CodecException on wrong state");
        } catch (IllegalStateException e) { // expected
        }
        try {
            codec.flush();
            fail("flush should return IllegalStateException when in Initialized state");
        } catch (MediaCodec.CodecException e) {
            logMediaCodecException(e);
            fail("flush should not return MediaCodec.CodecException on wrong state");
        } catch (IllegalStateException e) { // expected
        }

        MediaCodecInfo codecInfo = codec.getCodecInfo(); // obtaining the codec info now is fine.
        try {
            int bufIndex = codec.dequeueInputBuffer(0);
            fail("dequeueInputBuffer should return IllegalStateException"
                    + " when in the Initialized state");
        } catch (MediaCodec.CodecException e) {
            logMediaCodecException(e);
            fail("dequeueInputBuffer should not return MediaCodec.CodecException"
                    + " on wrong state");
        } catch (IllegalStateException e) { // expected
        }
        try {
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int bufIndex = codec.dequeueOutputBuffer(info, 0);
            fail("dequeueOutputBuffer should return IllegalStateException"
                    + " when in the Initialized state");
        } catch (MediaCodec.CodecException e) {
            logMediaCodecException(e);
            fail("dequeueOutputBuffer should not return MediaCodec.CodecException"
                    + " on wrong state");
        } catch (IllegalStateException e) { // expected
        }

        // configure (enter Configured State)

        // configure improperly
        try {
            codec.configure(format, null /* surface */, null /* crypto */,
                    isEncoder ? 0 : MediaCodec.CONFIGURE_FLAG_ENCODE /* flags */);
            fail("configure needs MediaCodec.CONFIGURE_FLAG_ENCODE for encoders only");
        } catch (MediaCodec.CodecException e) { // expected
            logMediaCodecException(e);
        } catch (IllegalStateException e) {
            fail("configure should not return IllegalStateException when improperly configured");
        }
        if (mIsAtLeastU) {
            try {
                int flags = MediaCodec.CONFIGURE_FLAG_USE_CRYPTO_ASYNC |
                        (isEncoder ? MediaCodec.CONFIGURE_FLAG_ENCODE : 0);
                codec.configure(format, null /* surface */, null /* crypto */, flags /* flags */);
                fail("At the minimum, CONFIGURE_FLAG_USE_CRYPTO_ASYNC requires setting callback");
            } catch(IllegalStateException e) { //expected
                // Need to set callbacks
            }
        }
        // correct
        codec.configure(format, null /* surface */, null /* crypto */,
                isEncoder ? MediaCodec.CONFIGURE_FLAG_ENCODE : 0 /* flags */);

        // test a few commands
        try {
            codec.flush();
            fail("flush should return IllegalStateException when in Configured state");
        } catch (MediaCodec.CodecException e) {
            logMediaCodecException(e);
            fail("flush should not return MediaCodec.CodecException on wrong state");
        } catch (IllegalStateException e) { // expected
        }
        try {
            Surface surface = codec.createInputSurface();
            if (!isEncoder) {
                fail("createInputSurface should not work on a decoder");
            }
        } catch (IllegalStateException |
                 IllegalArgumentException e) { // expected for decoder and audio encoder
            if (isVideoEncoder) {
                throw e;
            }
        }

        // test getInputBuffers before start()
        try {
            ByteBuffer[] buffers = codec.getInputBuffers();
            fail("getInputBuffers called before start() should throw exception");
        } catch (IllegalStateException e) { // expected
        }

        // start codec (enter Executing state)
        codec.start();

        // test getInputBuffers after start()
        try {
            ByteBuffer[] buffers = codec.getInputBuffers();
            if (buffers == null) {
                fail("getInputBuffers called after start() should not return null");
            }
            if (isVideoEncoder && buffers.length > 0) {
                fail("getInputBuffers returned non-zero length array with input surface");
            }
        } catch (IllegalStateException e) {
            fail("getInputBuffers called after start() shouldn't throw exception");
        }

        // test a few commands
        try {
            codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
            fail("configure should return IllegalStateException when in Executing state");
        } catch (MediaCodec.CodecException e) {
            logMediaCodecException(e);
            // TODO: consider configuring after a flush.
            fail("configure should not return MediaCodec.CodecException on wrong state");
        } catch (IllegalStateException e) { // expected
        }
        if (mIsAtLeastR) {
            try {
                codec.getQueueRequest(0);
                fail("getQueueRequest should throw IllegalStateException when not configured with " +
                        "CONFIGURE_FLAG_USE_BLOCK_MODEL");
            } catch (MediaCodec.CodecException e) {
                logMediaCodecException(e);
                fail("getQueueRequest should not return " +
                        "MediaCodec.CodecException on wrong configuration");
            } catch (IllegalStateException e) { // expected
            }
            try {
                codec.getOutputFrame(0);
                fail("getOutputFrame should throw IllegalStateException when not configured with " +
                        "CONFIGURE_FLAG_USE_BLOCK_MODEL");
            } catch (MediaCodec.CodecException e) {
                logMediaCodecException(e);
                fail("getOutputFrame should not return MediaCodec.CodecException on wrong " +
                        "configuration");
            } catch (IllegalStateException e) { // expected
            }
        }

        // two flushes should be fine.
        codec.flush();
        codec.flush();

        // stop codec (enter Initialized state)
        // two stops should be fine.
        codec.stop();
        codec.stop();

        // release codec (enter Uninitialized state)
        // two releases should be fine.
        codec.release();
        codec.release();

        try {
            codecInfo = codec.getCodecInfo();
            fail("getCodecInfo should should return IllegalStateException" +
                    " when in Uninitialized state");
        } catch (MediaCodec.CodecException e) {
            logMediaCodecException(e);
            fail("getCodecInfo should not return MediaCodec.CodecException on wrong state");
        } catch (IllegalStateException e) { // expected
        }
        try {
            codec.stop();
            fail("stop should return IllegalStateException when in Uninitialized state");
        } catch (MediaCodec.CodecException e) {
            logMediaCodecException(e);
            fail("stop should not return MediaCodec.CodecException on wrong state");
        } catch (IllegalStateException e) { // expected
        }

        if (mIsAtLeastS) {
            try {
                codec.getSupportedVendorParameters();
                fail("getSupportedVendorParameters should throw IllegalStateException" +
                        " when in Uninitialized state");
            } catch (IllegalStateException e) { // expected
            } catch (Exception e) {
                fail("unexpected exception: " + e.toString());
            }
            try {
                codec.getParameterDescriptor("");
                fail("getParameterDescriptor should throw IllegalStateException" +
                        " when in Uninitialized state");
            } catch (IllegalStateException e) { // expected
            } catch (Exception e) {
                fail("unexpected exception: " + e.toString());
            }
            try {
                codec.subscribeToVendorParameters(List.of(""));
                fail("subscribeToVendorParameters should throw IllegalStateException" +
                        " when in Uninitialized state");
            } catch (IllegalStateException e) { // expected
            } catch (Exception e) {
                fail("unexpected exception: " + e.toString());
            }
            try {
                codec.unsubscribeFromVendorParameters(List.of(""));
                fail("unsubscribeFromVendorParameters should throw IllegalStateException" +
                        " when in Uninitialized state");
            } catch (IllegalStateException e) { // expected
            } catch (Exception e) {
                fail("unexpected exception: " + e.toString());
            }
        }

        if (mIsAtLeastR) {
            // recreate
            codec = createCodecByType(format.getString(MediaFormat.KEY_MIME), isEncoder);

            if (isVideoEncoder) {
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            }

            // configure improperly
            try {
                codec.configure(format, null /* surface */, null /* crypto */,
                        MediaCodec.CONFIGURE_FLAG_USE_BLOCK_MODEL |
                        (isEncoder ? MediaCodec.CONFIGURE_FLAG_ENCODE : 0) /* flags */);
                fail("configure with detached buffer mode should be done after setCallback");
            } catch (MediaCodec.CodecException e) {
                logMediaCodecException(e);
                fail("configure should not return IllegalStateException when improperly configured");
            } catch (IllegalStateException e) { // expected
            }

            final LinkedBlockingQueue<Integer> inputQueue = new LinkedBlockingQueue<>();
            codec.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(MediaCodec codec, int index) {
                    inputQueue.offer(index);
                }
                @Override
                public void onOutputBufferAvailable(
                        MediaCodec codec, int index, MediaCodec.BufferInfo info) { }
                @Override
                public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) { }
                @Override
                public void onError(MediaCodec codec, CodecException e) { }
            });

            // configure with CONFIGURE_FLAG_USE_BLOCK_MODEL (enter Configured State)
            codec.configure(format, null /* surface */, null /* crypto */,
                    MediaCodec.CONFIGURE_FLAG_USE_BLOCK_MODEL |
                    (isEncoder ? MediaCodec.CONFIGURE_FLAG_ENCODE : 0) /* flags */);

            // start codec (enter Executing state)
            codec.start();

            // grab input index (this should happen immediately)
            Integer index = null;
            try {
                index = inputQueue.poll(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
            assertNotNull(index);

            // test a few commands
            try {
                codec.getInputBuffers();
                fail("getInputBuffers called in detached buffer mode should throw exception");
            } catch (MediaCodec.IncompatibleWithBlockModelException e) { // expected
            }
            try {
                codec.getOutputBuffers();
                fail("getOutputBuffers called in detached buffer mode should throw exception");
            } catch (MediaCodec.IncompatibleWithBlockModelException e) { // expected
            }
            try {
                codec.getInputBuffer(index);
                fail("getInputBuffer called in detached buffer mode should throw exception");
            } catch (MediaCodec.IncompatibleWithBlockModelException e) { // expected
            }
            try {
                codec.dequeueInputBuffer(0);
                fail("dequeueInputBuffer called in detached buffer mode should throw exception");
            } catch (MediaCodec.IncompatibleWithBlockModelException e) { // expected
            }
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            try {
                codec.dequeueOutputBuffer(info, 0);
                fail("dequeueOutputBuffer called in detached buffer mode should throw exception");
            } catch (MediaCodec.IncompatibleWithBlockModelException e) { // expected
            }

            // test getQueueRequest
            MediaCodec.QueueRequest request = codec.getQueueRequest(index);
            try {
                request.queue();
                fail("QueueRequest should throw IllegalStateException when no buffer is set");
            } catch (IllegalStateException e) { // expected
            }
            // setting a block
            String[] names = new String[]{ codec.getName() };
            request.setLinearBlock(MediaCodec.LinearBlock.obtain(1, names), 0, 0);
            // setting additional block should fail
            try (HardwareBuffer buffer = HardwareBuffer.create(
                    16 /* width */,
                    16 /* height */,
                    HardwareBuffer.YCBCR_420_888,
                    1 /* layers */,
                    HardwareBuffer.USAGE_CPU_READ_OFTEN | HardwareBuffer.USAGE_CPU_WRITE_OFTEN)) {
                request.setHardwareBuffer(buffer);
                fail("QueueRequest should throw IllegalStateException multiple blocks are set.");
            } catch (IllegalStateException e) { // expected
            }
        }

        // release codec
        codec.release();

        return true;
    }

    /**
     * Tests:
     * <br> calling createInputSurface() before configure() throws exception
     * <br> calling createInputSurface() after start() throws exception
     * <br> calling createInputSurface() with a non-Surface color format is not required to throw exception
     */
    @ApiTest(apis = "MediaCodec#createInputSurface")
    @Test
    public void testCreateInputSurfaceErrors() {
        if (!supportsCodec(MIME_TYPE, true)) {
            Log.i(TAG, "No encoder found for mimeType= " + MIME_TYPE);
            return;
        }

        MediaFormat format = createMediaFormat();
        MediaCodec encoder = null;
        Surface surface = null;

        // Replace color format with something that isn't COLOR_FormatSurface.
        MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
        int colorFormat = findNonSurfaceColorFormat(codecInfo, MIME_TYPE);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);

        try {
            try {
                encoder = MediaCodec.createByCodecName(codecInfo.getName());
            } catch (IOException e) {
                fail("failed to create codec " + codecInfo.getName());
            }
            try {
                surface = encoder.createInputSurface();
                fail("createInputSurface should not work pre-configure");
            } catch (IllegalStateException ise) {
                // good
            }
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();
            try {
                surface = encoder.createInputSurface();
                fail("createInputSurface should not work post-start");
            } catch (IllegalStateException ise) {
                // good
            }
        } finally {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
        }
        assertNull(surface);
    }

    /**
     * Tests:
     * <br> signaling end-of-stream before any data is sent works
     * <br> signaling EOS twice throws exception
     * <br> submitting a frame after EOS throws exception [TODO]
     */
    @ApiTest(apis = "MediaCodec#signalEndOfInputStream")
    @Test
    public void testSignalSurfaceEOS() {
        if (!supportsCodec(MIME_TYPE, true)) {
            Log.i(TAG, "No encoder found for mimeType= " + MIME_TYPE);
            return;
        }

        MediaFormat format = createMediaFormat();
        MediaCodec encoder = null;
        InputSurface inputSurface = null;

        try {
            try {
                encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            } catch (IOException e) {
                fail("failed to create " + MIME_TYPE + " encoder");
            }
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = new InputSurface(encoder.createInputSurface());
            inputSurface.makeCurrent();
            encoder.start();

            // send an immediate EOS
            encoder.signalEndOfInputStream();

            try {
                encoder.signalEndOfInputStream();
                fail("should not be able to signal EOS twice");
            } catch (IllegalStateException ise) {
                // good
            }

            // submit a frame post-EOS
            GLES20.glClearColor(0.0f, 0.5f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            try {
                inputSurface.swapBuffers();
                if (false) {    // TODO
                    fail("should not be able to submit frame after EOS");
                }
            } catch (Exception ex) {
                // good
            }
        } finally {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (inputSurface != null) {
                inputSurface.release();
            }
        }
    }

    /**
     * Tests:
     * <br> stopping with buffers in flight doesn't crash or hang
     */
    @ApiTest(apis = "MediaCodec#stop")
    @Test
    public void testAbruptStop() {
        if (!supportsCodec(MIME_TYPE, true)) {
            Log.i(TAG, "No encoder found for mimeType= " + MIME_TYPE);
            return;
        }

        // There appears to be a race, so run it several times with a short delay between runs
        // to allow any previous activity to shut down.
        for (int i = 0; i < 50; i++) {
            Log.d(TAG, "testAbruptStop " + i);
            doTestAbruptStop();
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
        }
    }
    private void doTestAbruptStop() {
        MediaFormat format = createMediaFormat();
        MediaCodec encoder = null;
        InputSurface inputSurface = null;

        try {
            try {
                encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            } catch (IOException e) {
                fail("failed to create " + MIME_TYPE + " encoder");
            }
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = new InputSurface(encoder.createInputSurface());
            inputSurface.makeCurrent();
            encoder.start();

            int totalBuffers = encoder.getOutputBuffers().length;
            if (VERBOSE) Log.d(TAG, "Total buffers: " + totalBuffers);

            // Submit several frames quickly, without draining the encoder output, to try to
            // ensure that we've got some queued up when we call stop().  If we do too many
            // we'll block in swapBuffers().
            for (int i = 0; i < totalBuffers; i++) {
                GLES20.glClearColor(0.0f, (i % 8) / 8.0f, 0.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                inputSurface.swapBuffers();
            }
            Log.d(TAG, "stopping");
            encoder.stop();
            Log.d(TAG, "stopped");
        } finally {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (inputSurface != null) {
                inputSurface.release();
            }
        }
    }

    @ApiTest(apis = {"MediaCodec#flush", "MediaCodec#release"})
    @Test
    public void testReleaseAfterFlush() throws IOException, InterruptedException {
        String mimes[] = new String[] { MIME_TYPE, MIME_TYPE_AUDIO};
        for (String mime : mimes) {
            if (!MediaUtils.checkEncoder(mime)) {
                continue;
            }
            testReleaseAfterFlush(mime);
        }
    }

    private void testReleaseAfterFlush(String mime) throws IOException, InterruptedException {
        CountDownLatch buffersExhausted = null;
        CountDownLatch codecFlushed = null;
        AtomicInteger numBuffers = null;

        // sync flush from same thread
        MediaCodec encoder = MediaCodec.createEncoderByType(mime);
        runReleaseAfterFlush(mime, encoder, buffersExhausted, codecFlushed, numBuffers);

        // sync flush from different thread
        encoder = MediaCodec.createEncoderByType(mime);
        buffersExhausted = new CountDownLatch(1);
        codecFlushed = new CountDownLatch(1);
        numBuffers = new AtomicInteger();
        Thread flushThread = new FlushThread(encoder, buffersExhausted, codecFlushed);
        flushThread.start();
        runReleaseAfterFlush(mime, encoder, buffersExhausted, codecFlushed, numBuffers);
        flushThread.join();

        // async
        // This value is calculated in getOutputBufferIndices by calling dequeueOutputBuffer
        // with a fixed timeout until buffers are exhausted; it is possible that random timing
        // in dequeueOutputBuffer can result in a smaller `nBuffs` than the max possible value.
        int nBuffs = numBuffers.get();
        HandlerThread callbackThread = new HandlerThread("ReleaseAfterFlushCallbackThread");
        callbackThread.start();
        Handler handler = new Handler(callbackThread.getLooper());

        // async flush from same thread
        encoder = MediaCodec.createEncoderByType(mime);
        buffersExhausted = null;
        codecFlushed = null;
        ReleaseAfterFlushCallback callback =
                new ReleaseAfterFlushCallback(mime, encoder, buffersExhausted, codecFlushed, nBuffs);
        encoder.setCallback(callback, handler); // setCallback before configure, which is called in run
        callback.run(); // drive input on main thread

        // async flush from different thread
        encoder = MediaCodec.createEncoderByType(mime);
        buffersExhausted = new CountDownLatch(1);
        codecFlushed = new CountDownLatch(1);
        callback = new ReleaseAfterFlushCallback(mime, encoder, buffersExhausted, codecFlushed, nBuffs);
        encoder.setCallback(callback, handler);
        flushThread = new FlushThread(encoder, buffersExhausted, codecFlushed);
        flushThread.start();
        callback.run();
        flushThread.join();

        callbackThread.quitSafely();
        callbackThread.join();
    }

    @ApiTest(apis = {"MediaCodec#setCallback", "MediaCodec#flush", "MediaCodec#reset"})
    @Test
    public void testAsyncFlushAndReset() throws Exception, InterruptedException {
        testAsyncReset(false /* testStop */);
    }

    @ApiTest(apis = {"MediaCodec#setCallback", "MediaCodec#stop", "MediaCodec#reset"})
    @Test
    public void testAsyncStopAndReset() throws Exception, InterruptedException {
        testAsyncReset(true /* testStop */);
    }

    private void testAsyncReset(boolean testStop) throws Exception, InterruptedException {
        // Test video and audio 10x each
        for (int i = 0; i < 10; i++) {
            testAsyncReset(false /* audio */, (i % 2) == 0 /* swap */, testStop);
        }
        for (int i = 0; i < 10; i++) {
            testAsyncReset(true /* audio */, (i % 2) == 0 /* swap */, testStop);
        }
    }

    /*
     * This method simulates a race between flush (or stop) and reset() called from
     * two threads. Neither call should get stuck. This should be run multiple rounds.
     */
    private void testAsyncReset(boolean audio, boolean swap, final boolean testStop)
            throws Exception, InterruptedException {
        String mimeTypePrefix  = audio ? "audio/" : "video/";
        final MediaExtractor mediaExtractor = getMediaExtractorForMimeType(
                INPUT_RESOURCE, mimeTypePrefix);
        MediaFormat mediaFormat = mediaExtractor.getTrackFormat(
                mediaExtractor.getSampleTrackIndex());
        if (!MediaUtils.checkDecoderForFormat(mediaFormat)) {
            return; // skip
        }

        OutputSurface outputSurface = audio ? null : new OutputSurface(1, 1);
        final Surface surface = outputSurface == null ? null : outputSurface.getSurface();

        String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
        final MediaCodec mediaCodec = MediaCodec.createDecoderByType(mimeType);

        try {
            mediaCodec.configure(mediaFormat, surface, null /* crypto */, 0 /* flags */);

            mediaCodec.start();

            assertTrue(runDecodeTillFirstOutput(mediaCodec, mediaExtractor));

            Thread flushingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (testStop) {
                            mediaCodec.stop();
                        } else {
                            mediaCodec.flush();
                        }
                    } catch (IllegalStateException e) {
                        // This is okay, since we're simulating a race between flush and reset.
                        // If reset executed first, flush could fail.
                    }
                }
            });

            Thread resettingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    mediaCodec.reset();
                }
            });

            // start flushing (or stopping) and resetting in two threads
            if (swap) {
                flushingThread.start();
                resettingThread.start();
            } else {
                resettingThread.start();
                flushingThread.start();
            }

            // wait for at most 5 sec, and check if the thread exits properly
            flushingThread.join(5000);
            assertFalse(flushingThread.isAlive());

            resettingThread.join(5000);
            assertFalse(resettingThread.isAlive());
        } finally {
            if (mediaCodec != null) {
                mediaCodec.release();
            }
            if (mediaExtractor != null) {
                mediaExtractor.release();
            }
            if (outputSurface != null) {
                outputSurface.release();
            }
        }
    }

    private static class FlushThread extends Thread {
        final MediaCodec mEncoder;
        final CountDownLatch mBuffersExhausted;
        final CountDownLatch mCodecFlushed;

        FlushThread(MediaCodec encoder, CountDownLatch buffersExhausted,
                CountDownLatch codecFlushed) {
            mEncoder = encoder;
            mBuffersExhausted = buffersExhausted;
            mCodecFlushed = codecFlushed;
        }

        @Override
        public void run() {
            try {
                mBuffersExhausted.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "buffersExhausted wait interrupted; flushing immediately.", e);
            }
            mEncoder.flush();
            mCodecFlushed.countDown();
        }
    }

    private static class ReleaseAfterFlushCallback extends MediaCodec.Callback implements Runnable {
        final String mMime;
        final MediaCodec mEncoder;
        final CountDownLatch mBuffersExhausted, mCodecFlushed;
        final int mNumBuffersBeforeFlush;

        CountDownLatch mStopInput = new CountDownLatch(1);
        List<Integer> mInputBufferIndices = new ArrayList<>();
        List<Integer> mOutputBufferIndices = new ArrayList<>();

        ReleaseAfterFlushCallback(String mime,
                MediaCodec encoder,
                CountDownLatch buffersExhausted,
                CountDownLatch codecFlushed,
                int numBuffersBeforeFlush) {
            mMime = mime;
            mEncoder = encoder;
            mBuffersExhausted = buffersExhausted;
            mCodecFlushed = codecFlushed;
            mNumBuffersBeforeFlush = numBuffersBeforeFlush;
        }

        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            assertTrue("video onInputBufferAvailable " + index, mMime.startsWith("audio/"));
            synchronized (mInputBufferIndices) {
                mInputBufferIndices.add(index);
            };
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, BufferInfo info) {
            mOutputBufferIndices.add(index);
            if (mOutputBufferIndices.size() == mNumBuffersBeforeFlush) {
                releaseAfterFlush(codec, mOutputBufferIndices, mBuffersExhausted, mCodecFlushed);
                mStopInput.countDown();
            }
        }

        @Override
        public void onError(MediaCodec codec, CodecException e) {
            Log.e(TAG, codec + " onError", e);
            fail(codec + " onError " + e.getMessage());
        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            Log.v(TAG, codec + " onOutputFormatChanged " + format);
        }

        @Override
        public void run() {
            InputSurface inputSurface = null;
            try {
                inputSurface = initCodecAndSurface(mMime, mEncoder);
                do {
                    int inputIndex = -1;
                    if (inputSurface == null) {
                        // asynchronous audio codec
                        synchronized (mInputBufferIndices) {
                            if (mInputBufferIndices.isEmpty()) {
                                continue;
                            } else {
                                inputIndex = mInputBufferIndices.remove(0);
                            }
                        }
                    }
                    feedEncoder(mEncoder, inputSurface, inputIndex);
                } while (!mStopInput.await(TIMEOUT_USEC, TimeUnit.MICROSECONDS));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "mEncoder input frames interrupted/stopped", e);
            } finally {
                cleanupCodecAndSurface(mEncoder, inputSurface);
            }
        }
    }

    private static void runReleaseAfterFlush(
            String mime,
            MediaCodec encoder,
            CountDownLatch buffersExhausted,
            CountDownLatch codecFlushed,
            AtomicInteger numBuffers) {
        InputSurface inputSurface = null;
        try {
            inputSurface = initCodecAndSurface(mime, encoder);
            List<Integer> outputBufferIndices = getOutputBufferIndices(encoder, inputSurface);
            if (numBuffers != null) {
                numBuffers.set(outputBufferIndices.size());
            }
            releaseAfterFlush(encoder, outputBufferIndices, buffersExhausted, codecFlushed);
        } finally {
            cleanupCodecAndSurface(encoder, inputSurface);
        }
    }

    private static InputSurface initCodecAndSurface(String mime, MediaCodec encoder) {
        MediaFormat format;
        InputSurface inputSurface = null;
        if (mime.startsWith("audio/")) {
            format = MediaFormat.createAudioFormat(mime, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_COUNT);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, AUDIO_AAC_PROFILE);
            format.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } else if (MIME_TYPE.equals(mime)) {
            CodecInfo info = getAvcSupportedFormatInfo();
            format = MediaFormat.createVideoFormat(mime, info.mMaxW, info.mMaxH);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, info.mBitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, info.mFps);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
            OutputSurface outputSurface = new OutputSurface(1, 1);
            encoder.configure(format, outputSurface.getSurface(), null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = new InputSurface(encoder.createInputSurface());
            inputSurface.makeCurrent();
        } else {
            throw new IllegalArgumentException("unsupported mime type: " + mime);
        }
        encoder.start();
        return inputSurface;
    }

    private static void cleanupCodecAndSurface(MediaCodec encoder, InputSurface inputSurface) {
        if (encoder != null) {
            encoder.stop();
            encoder.release();
        }

        if (inputSurface != null) {
            inputSurface.release();
        }
    }

    private static List<Integer> getOutputBufferIndices(MediaCodec encoder, InputSurface inputSurface) {
        boolean feedMoreFrames;
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        List<Integer> indices = new ArrayList<>();
        do {
            feedMoreFrames = indices.isEmpty();
            feedEncoder(encoder, inputSurface, -1);
            // dequeue buffers until not available
            int index = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            while (index >= 0) {
                indices.add(index);
                index = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC_SHORT);
            }
        } while (feedMoreFrames);
        assertFalse(indices.isEmpty());
        return indices;
    }

    /**
     * @param encoder audio/video encoder
     * @param inputSurface null for and only for audio encoders
     * @param inputIndex only used for audio; if -1 the function would attempt to dequeue from encoder;
     * do not use -1 for asynchronous encoders
     */
    private static void feedEncoder(MediaCodec encoder, InputSurface inputSurface, int inputIndex) {
        if (inputSurface == null) {
            // audio
            while (inputIndex == -1) {
                inputIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
            }
            ByteBuffer inputBuffer = encoder.getInputBuffer(inputIndex);;
            for (int i = 0; i < inputBuffer.capacity() / 2; i++) {
                inputBuffer.putShort((short)i);
            }
            encoder.queueInputBuffer(inputIndex, 0, inputBuffer.limit(), 0, 0);
        } else {
            // video
            GLES20.glClearColor(0.0f, 0.5f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            inputSurface.swapBuffers();
        }
    }

    private static void releaseAfterFlush(
            MediaCodec encoder,
            List<Integer> outputBufferIndices,
            CountDownLatch buffersExhausted,
            CountDownLatch codecFlushed) {
        if (buffersExhausted == null) {
            // flush from same thread
            encoder.flush();
        } else {
            assertNotNull(codecFlushed);
            buffersExhausted.countDown();
            try {
                codecFlushed.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.w(TAG, "codecFlushed wait interrupted; releasing buffers immediately.", e);
            }
        }

        for (int index : outputBufferIndices) {
            try {
                encoder.releaseOutputBuffer(index, true);
                fail("MediaCodec releaseOutputBuffer after flush() does not throw exception");
            } catch (MediaCodec.CodecException e) {
                // Expected
            }
        }
    }

    /**
     * Tests:
     * <br> dequeueInputBuffer() fails when encoder configured with an input Surface
     */
    @ApiTest(apis = {"MediaCodec#dequeueInputBuffer", "MediaCodec#getMetrics"})
    @Test
    public void testDequeueSurface() {
        if (!supportsCodec(MIME_TYPE, true)) {
            Log.i(TAG, "No encoder found for mimeType= " + MIME_TYPE);
            return;
        }

        MediaFormat format = createMediaFormat();
        MediaCodec encoder = null;
        Surface surface = null;

        try {
            try {
                encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            } catch (IOException e) {
                fail("failed to create " + MIME_TYPE + " encoder");
            }
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = encoder.createInputSurface();
            encoder.start();

            try {
                encoder.dequeueInputBuffer(-1);
                fail("dequeueInputBuffer should fail on encoder with input surface");
            } catch (IllegalStateException ise) {
                // good
            }

            PersistableBundle metrics = encoder.getMetrics();
            if (metrics == null) {
                fail("getMetrics() returns null");
            } else if (metrics.isEmpty()) {
                fail("getMetrics() returns empty results");
            }
            int encoding = metrics.getInt(MediaCodec.MetricsConstants.ENCODER, -1);
            if (encoding != 1) {
                fail("getMetrics() returns bad encoder value " + encoding);
            }
            String theCodec = metrics.getString(MediaCodec.MetricsConstants.CODEC, null);
            if (theCodec == null) {
                fail("getMetrics() returns null codec value ");
            }

        } finally {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (surface != null) {
                surface.release();
            }
        }
    }

    /**
     * Tests:
     * <br> configure() encoder with Surface, re-configure() without Surface works
     * <br> sending EOS with signalEndOfInputStream on non-Surface encoder fails
     */
    @ApiTest(apis = {"MediaCodec#configure", "MediaCodec#signalEndOfInputStream",
            "MediaCodec#getMetrics"})
    @Test
    public void testReconfigureWithoutSurface() {
        if (!supportsCodec(MIME_TYPE, true)) {
            Log.i(TAG, "No encoder found for mimeType= " + MIME_TYPE);
            return;
        }

        MediaFormat format = createMediaFormat();
        MediaCodec encoder = null;
        Surface surface = null;

        try {
            try {
                encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            } catch (IOException e) {
                fail("failed to create " + MIME_TYPE + " encoder");
            }
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = encoder.createInputSurface();
            encoder.start();

            encoder.getOutputBuffers();

            // re-configure, this time without an input surface
            if (VERBOSE) Log.d(TAG, "reconfiguring");
            encoder.stop();
            // Use non-opaque color format for byte buffer mode.
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();
            if (VERBOSE) Log.d(TAG, "reconfigured");

            encoder.getOutputBuffers();
            encoder.dequeueInputBuffer(-1);

            try {
                encoder.signalEndOfInputStream();
                fail("signalEndOfInputStream only works on surface input");
            } catch (IllegalStateException ise) {
                // good
            }

            PersistableBundle metrics = encoder.getMetrics();
            if (metrics == null) {
                fail("getMetrics() returns null");
            } else if (metrics.isEmpty()) {
                fail("getMetrics() returns empty results");
            }
            int encoding = metrics.getInt(MediaCodec.MetricsConstants.ENCODER, -1);
            if (encoding != 1) {
                fail("getMetrics() returns bad encoder value " + encoding);
            }
            String theCodec = metrics.getString(MediaCodec.MetricsConstants.CODEC, null);
            if (theCodec == null) {
                fail("getMetrics() returns null codec value ");
            }

        } finally {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (surface != null) {
                surface.release();
            }
        }
    }

    @ApiTest(apis = "MediaCodec#flush")
    @Test
    public void testDecodeAfterFlush() throws InterruptedException {
        testDecodeAfterFlush(true /* audio */);
        testDecodeAfterFlush(false /* audio */);
    }

    private void testDecodeAfterFlush(final boolean audio) throws InterruptedException {
        final AtomicBoolean completed = new AtomicBoolean(false);
        Thread decodingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                OutputSurface outputSurface = null;
                MediaExtractor mediaExtractor = null;
                MediaCodec mediaCodec = null;
                try {
                    String mimeTypePrefix  = audio ? "audio/" : "video/";
                    if (!audio) {
                        outputSurface = new OutputSurface(1, 1);
                    }
                    mediaExtractor = getMediaExtractorForMimeType(INPUT_RESOURCE, mimeTypePrefix);
                    MediaFormat mediaFormat =
                            mediaExtractor.getTrackFormat(mediaExtractor.getSampleTrackIndex());
                    if (!MediaUtils.checkDecoderForFormat(mediaFormat)) {
                        completed.set(true);
                        return; // skip
                    }
                    String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                    mediaCodec = MediaCodec.createDecoderByType(mimeType);
                    mediaCodec.configure(mediaFormat, outputSurface == null ? null : outputSurface.getSurface(),
                            null /* crypto */, 0 /* flags */);
                    mediaCodec.start();

                    if (!runDecodeTillFirstOutput(mediaCodec, mediaExtractor)) {
                        throw new RuntimeException("decoder does not generate non-empty output.");
                    }

                    PersistableBundle metrics = mediaCodec.getMetrics();
                    if (metrics == null) {
                        fail("getMetrics() returns null");
                    } else if (metrics.isEmpty()) {
                        fail("getMetrics() returns empty results");
                    }
                    int encoder = metrics.getInt(MediaCodec.MetricsConstants.ENCODER, -1);
                    if (encoder != 0) {
                        fail("getMetrics() returns bad encoder value " + encoder);
                    }
                    String theCodec = metrics.getString(MediaCodec.MetricsConstants.CODEC, null);
                    if (theCodec == null) {
                        fail("getMetrics() returns null codec value ");
                    }


                    // simulate application flush.
                    mediaExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                    mediaCodec.flush();

                    completed.set(runDecodeTillFirstOutput(mediaCodec, mediaExtractor));
                    metrics = mediaCodec.getMetrics();
                    if (metrics == null) {
                        fail("getMetrics() returns null");
                    } else if (metrics.isEmpty()) {
                        fail("getMetrics() returns empty results");
                    }
                    int encoding = metrics.getInt(MediaCodec.MetricsConstants.ENCODER, -1);
                    if (encoding != 0) {
                        fail("getMetrics() returns bad encoder value " + encoding);
                    }
                    String theCodec2 = metrics.getString(MediaCodec.MetricsConstants.CODEC, null);
                    if (theCodec2 == null) {
                        fail("getMetrics() returns null codec value ");
                    }

                } catch (IOException e) {
                    throw new RuntimeException("error setting up decoding", e);
                } finally {
                    if (mediaCodec != null) {
                        mediaCodec.stop();

                        PersistableBundle metrics = mediaCodec.getMetrics();
                        if (metrics == null) {
                            fail("getMetrics() returns null");
                        } else if (metrics.isEmpty()) {
                            fail("getMetrics() returns empty results");
                        }
                        int encoder = metrics.getInt(MediaCodec.MetricsConstants.ENCODER, -1);
                        if (encoder != 0) {
                            fail("getMetrics() returns bad encoder value " + encoder);
                        }
                        String theCodec = metrics.getString(MediaCodec.MetricsConstants.CODEC, null);
                        if (theCodec == null) {
                            fail("getMetrics() returns null codec value ");
                        }

                        mediaCodec.release();
                    }
                    if (mediaExtractor != null) {
                        mediaExtractor.release();
                    }
                    if (outputSurface != null) {
                        outputSurface.release();
                    }
                }
            }
        });
        decodingThread.start();
        decodingThread.join(DECODING_TIMEOUT_MS);
        // In case it's timed out, need to stop the thread and have all resources released.
        decodingThread.interrupt();
        if (!completed.get()) {
            throw new RuntimeException("timed out decoding to end-of-stream");
        }
    }

    // Run the decoder till it generates an output buffer.
    // Return true when that output buffer is not empty, false otherwise.
    private static boolean runDecodeTillFirstOutput(
            MediaCodec mediaCodec, MediaExtractor mediaExtractor) {
        final int TIME_OUT_US = 10000;

        assertTrue("Wrong test stream which has no data.",
                mediaExtractor.getSampleTrackIndex() != -1);
        boolean signaledEos = false;
        MediaCodec.BufferInfo outputBufferInfo = new MediaCodec.BufferInfo();
        while (!Thread.interrupted()) {
            // Try to feed more data into the codec.
            if (!signaledEos) {
                int bufferIndex = mediaCodec.dequeueInputBuffer(TIME_OUT_US /* timeoutUs */);
                if (bufferIndex != -1) {
                    ByteBuffer buffer = mediaCodec.getInputBuffer(bufferIndex);
                    int size = mediaExtractor.readSampleData(buffer, 0 /* offset */);
                    long timestampUs = mediaExtractor.getSampleTime();
                    mediaExtractor.advance();
                    signaledEos = mediaExtractor.getSampleTrackIndex() == -1;
                    mediaCodec.queueInputBuffer(bufferIndex,
                            0 /* offset */,
                            size,
                            timestampUs,
                            signaledEos ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    Log.i("DEBUG", "queue with " + signaledEos);
                }
            }

            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(
                    outputBufferInfo, TIME_OUT_US /* timeoutUs */);

            if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED
                    || outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
                    || outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                continue;
            }
            assertTrue("Wrong output buffer index", outputBufferIndex >= 0);

            PersistableBundle metrics = mediaCodec.getMetrics();
            Log.d(TAG, "getMetrics after first buffer metrics says: " + metrics);

            int encoder = metrics.getInt(MediaCodec.MetricsConstants.ENCODER, -1);
            if (encoder != 0) {
                fail("getMetrics() returns bad encoder value " + encoder);
            }
            String theCodec = metrics.getString(MediaCodec.MetricsConstants.CODEC, null);
            if (theCodec == null) {
                fail("getMetrics() returns null codec value ");
            }

            mediaCodec.releaseOutputBuffer(outputBufferIndex, false /* render */);
            boolean eos = (outputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
            Log.i("DEBUG", "Got a frame with eos=" + eos);
            if (eos && outputBufferInfo.size == 0) {
                return false;
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * Tests whether decoding a short group-of-pictures succeeds. The test queues a few video frames
     * then signals end-of-stream. The test fails if the decoder doesn't output the queued frames.
     */
    @ApiTest(apis = {"MediaCodecInfo.CodecCapabilities#COLOR_FormatSurface"})
    @Test
    public void testDecodeShortInput() throws InterruptedException {
        // Input buffers from this input video are queued up to and including the video frame with
        // timestamp LAST_BUFFER_TIMESTAMP_US.
        final String INPUT_RESOURCE =
                "video_480x360_mp4_h264_1350kbps_30fps_aac_stereo_192kbps_44100hz.mp4";
        final long LAST_BUFFER_TIMESTAMP_US = 166666;

        // The test should fail if the decoder never produces output frames for the truncated input.
        // Time out decoding, as we have no way to query whether the decoder will produce output.
        final int DECODING_TIMEOUT_MS = 2000;

        final AtomicBoolean completed = new AtomicBoolean();
        Thread videoDecodingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                completed.set(runDecodeShortInput(INPUT_RESOURCE, LAST_BUFFER_TIMESTAMP_US));
            }
        });
        videoDecodingThread.start();
        videoDecodingThread.join(DECODING_TIMEOUT_MS);
        if (!completed.get()) {
            throw new RuntimeException("timed out decoding to end-of-stream");
        }
    }

    private boolean runDecodeShortInput(final String inputResource, long lastBufferTimestampUs) {
        final int NO_BUFFER_INDEX = -1;

        OutputSurface outputSurface = null;
        MediaExtractor mediaExtractor = null;
        MediaCodec mediaCodec = null;
        try {
            outputSurface = new OutputSurface(1, 1);
            mediaExtractor = getMediaExtractorForMimeType(inputResource, "video/");
            MediaFormat mediaFormat =
                    mediaExtractor.getTrackFormat(mediaExtractor.getSampleTrackIndex());
            String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (!supportsCodec(mimeType, false)) {
                Log.i(TAG, "No decoder found for mimeType= " + MIME_TYPE);
                return true;
            }
            mediaCodec =
                    MediaCodec.createDecoderByType(mimeType);
            mediaCodec.configure(mediaFormat, outputSurface.getSurface(), null, 0);
            mediaCodec.start();
            boolean eos = false;
            boolean signaledEos = false;
            MediaCodec.BufferInfo outputBufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = NO_BUFFER_INDEX;
            while (!eos && !Thread.interrupted()) {
                // Try to feed more data into the codec.
                if (mediaExtractor.getSampleTrackIndex() != -1 && !signaledEos) {
                    int bufferIndex = mediaCodec.dequeueInputBuffer(0);
                    if (bufferIndex != NO_BUFFER_INDEX) {
                        ByteBuffer buffer = mediaCodec.getInputBuffers()[bufferIndex];
                        int size = mediaExtractor.readSampleData(buffer, 0);
                        long timestampUs = mediaExtractor.getSampleTime();
                        mediaExtractor.advance();
                        signaledEos = mediaExtractor.getSampleTrackIndex() == -1
                                || timestampUs == lastBufferTimestampUs;
                        mediaCodec.queueInputBuffer(bufferIndex,
                                0,
                                size,
                                timestampUs,
                                signaledEos ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                    }
                }

                // If we don't have an output buffer, try to get one now.
                if (outputBufferIndex == NO_BUFFER_INDEX) {
                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(outputBufferInfo, 0);
                }

                if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED
                        || outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED
                        || outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    outputBufferIndex = NO_BUFFER_INDEX;
                } else if (outputBufferIndex != NO_BUFFER_INDEX) {
                    eos = (outputBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;

                    boolean render = outputBufferInfo.size > 0;
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, render);
                    if (render) {
                        outputSurface.awaitNewImage();
                    }

                    outputBufferIndex = NO_BUFFER_INDEX;
                }
            }

            return eos;
        } catch (IOException e) {
            throw new RuntimeException("error reading input resource", e);
        } finally {
            if (mediaCodec != null) {
                mediaCodec.stop();
                mediaCodec.release();
            }
            if (mediaExtractor != null) {
                mediaExtractor.release();
            }
            if (outputSurface != null) {
                outputSurface.release();
            }
        }
    }

    /**
     * Tests creating two decoders for {@link #MIME_TYPE_AUDIO} at the same time.
     */
    @ApiTest(apis = {"MediaCodec#createDecoderByType",
            "android.media.MediaFormat#KEY_MIME",
            "android.media.MediaFormat#KEY_SAMPLE_RATE",
            "android.media.MediaFormat#KEY_CHANNEL_COUNT"})
    @Test
    public void testCreateTwoAudioDecoders() {
        final MediaFormat format = MediaFormat.createAudioFormat(
                MIME_TYPE_AUDIO, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_COUNT);

        MediaCodec audioDecoderA = null;
        MediaCodec audioDecoderB = null;
        try {
            try {
                audioDecoderA = MediaCodec.createDecoderByType(MIME_TYPE_AUDIO);
            } catch (IOException e) {
                fail("failed to create first " + MIME_TYPE_AUDIO + " decoder");
            }
            audioDecoderA.configure(format, null, null, 0);
            audioDecoderA.start();

            try {
                audioDecoderB = MediaCodec.createDecoderByType(MIME_TYPE_AUDIO);
            } catch (IOException e) {
                fail("failed to create second " + MIME_TYPE_AUDIO + " decoder");
            }
            audioDecoderB.configure(format, null, null, 0);
            audioDecoderB.start();
        } finally {
            if (audioDecoderB != null) {
                try {
                    audioDecoderB.stop();
                    audioDecoderB.release();
                } catch (RuntimeException e) {
                    Log.w(TAG, "exception stopping/releasing codec", e);
                }
            }

            if (audioDecoderA != null) {
                try {
                    audioDecoderA.stop();
                    audioDecoderA.release();
                } catch (RuntimeException e) {
                    Log.w(TAG, "exception stopping/releasing codec", e);
                }
            }
        }
    }

    /**
     * Tests creating an encoder and decoder for {@link #MIME_TYPE_AUDIO} at the same time.
     */
    @ApiTest(apis = {"MediaCodec#createDecoderByType", "MediaCodec#createEncoderByType",
            "android.media.MediaFormat#KEY_MIME",
            "android.media.MediaFormat#KEY_SAMPLE_RATE",
            "android.media.MediaFormat#KEY_CHANNEL_COUNT"})
    @Test
    public void testCreateAudioDecoderAndEncoder() {
        if (!supportsCodec(MIME_TYPE_AUDIO, true)) {
            Log.i(TAG, "No encoder found for mimeType= " + MIME_TYPE_AUDIO);
            return;
        }

        if (!supportsCodec(MIME_TYPE_AUDIO, false)) {
            Log.i(TAG, "No decoder found for mimeType= " + MIME_TYPE_AUDIO);
            return;
        }

        final MediaFormat encoderFormat = MediaFormat.createAudioFormat(
                MIME_TYPE_AUDIO, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_COUNT);
        encoderFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, AUDIO_AAC_PROFILE);
        encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE);
        final MediaFormat decoderFormat = MediaFormat.createAudioFormat(
                MIME_TYPE_AUDIO, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL_COUNT);

        MediaCodec audioEncoder = null;
        MediaCodec audioDecoder = null;
        try {
            try {
                audioEncoder = MediaCodec.createEncoderByType(MIME_TYPE_AUDIO);
            } catch (IOException e) {
                fail("failed to create " + MIME_TYPE_AUDIO + " encoder");
            }
            audioEncoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            audioEncoder.start();

            try {
                audioDecoder = MediaCodec.createDecoderByType(MIME_TYPE_AUDIO);
            } catch (IOException e) {
                fail("failed to create " + MIME_TYPE_AUDIO + " decoder");
            }
            audioDecoder.configure(decoderFormat, null, null, 0);
            audioDecoder.start();
        } finally {
            if (audioDecoder != null) {
                try {
                    audioDecoder.stop();
                    audioDecoder.release();
                } catch (RuntimeException e) {
                    Log.w(TAG, "exception stopping/releasing codec", e);
                }
            }

            if (audioEncoder != null) {
                try {
                    audioEncoder.stop();
                    audioEncoder.release();
                } catch (RuntimeException e) {
                    Log.w(TAG, "exception stopping/releasing codec", e);
                }
            }
        }
    }

    @ApiTest(apis = {"MediaCodec#createEncoderByType",
            "android.media.MediaFormat#KEY_MIME",
            "android.media.MediaFormat#KEY_SAMPLE_RATE",
            "android.media.MediaFormat#KEY_CHANNEL_COUNT",
            "android.media.MediaFormat#KEY_WIDTH",
            "android.media.MediaFormat#KEY_HEIGHT"})
    @Test
    public void testConcurrentAudioVideoEncodings() throws InterruptedException {
        if (!supportsCodec(MIME_TYPE_AUDIO, true)) {
            Log.i(TAG, "No encoder found for mimeType= " + MIME_TYPE_AUDIO);
            return;
        }

        if (!supportsCodec(MIME_TYPE, true)) {
            Log.i(TAG, "No decoder found for mimeType= " + MIME_TYPE);
            return;
        }

        final int VIDEO_NUM_SWAPS = 100;
        // audio only checks this and stop
        mVideoEncodingOngoing = true;
        final CodecInfo info = getAvcSupportedFormatInfo();
        long start = System.currentTimeMillis();
        Thread videoEncodingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                runVideoEncoding(VIDEO_NUM_SWAPS, info);
            }
        });
        Thread audioEncodingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                runAudioEncoding();
            }
        });
        videoEncodingThread.start();
        audioEncodingThread.start();
        videoEncodingThread.join();
        mVideoEncodingOngoing = false;
        audioEncodingThread.join();
        assertFalse("Video encoding error. Chekc logcat", mVideoEncoderHadError);
        assertFalse("Audio encoding error. Chekc logcat", mAudioEncoderHadError);
        long end = System.currentTimeMillis();
        Log.w(TAG, "Concurrent AV encoding took " + (end - start) + " ms for " + VIDEO_NUM_SWAPS +
                " video frames");
    }

    private static class CodecInfo {
        public int mMaxW;
        public int mMaxH;
        public int mFps;
        public int mBitRate;
    }

    @ApiTest(apis = {"MediaCodec#CryptoInfo", "MediaCodec#CryptoInfo#Pattern"})
    @Test
    public void testCryptoInfoPattern() {
        CryptoInfo info = new CryptoInfo();
        Pattern pattern = new Pattern(1 /*blocksToEncrypt*/, 2 /*blocksToSkip*/);
        assertEquals(1, pattern.getEncryptBlocks());
        assertEquals(2, pattern.getSkipBlocks());
        pattern.set(3 /*blocksToEncrypt*/, 4 /*blocksToSkip*/);
        assertEquals(3, pattern.getEncryptBlocks());
        assertEquals(4, pattern.getSkipBlocks());
        info.setPattern(pattern);
        // Check that CryptoInfo does not leak access to the underlying pattern.
        if (mIsAtLeastS) {
            // getPattern() availability SDK>=S
            pattern.set(10, 10);
            info.getPattern().set(10, 10);
            assertSame(3, info.getPattern().getEncryptBlocks());
            assertSame(4, info.getPattern().getSkipBlocks());
        }
    }

    private static CodecInfo getAvcSupportedFormatInfo() {
        MediaCodecInfo mediaCodecInfo = selectCodec(MIME_TYPE);
        CodecCapabilities cap = mediaCodecInfo.getCapabilitiesForType(MIME_TYPE);
        if (cap == null) { // not supported
            return null;
        }
        CodecInfo info = new CodecInfo();
        int highestLevel = 0;
        for (CodecProfileLevel lvl : cap.profileLevels) {
            if (lvl.level > highestLevel) {
                highestLevel = lvl.level;
            }
        }
        int maxW = 0;
        int maxH = 0;
        int bitRate = 0;
        int fps = 0; // frame rate for the max resolution
        switch(highestLevel) {
            // Do not support Level 1 to 2.
            case CodecProfileLevel.AVCLevel1:
            case CodecProfileLevel.AVCLevel11:
            case CodecProfileLevel.AVCLevel12:
            case CodecProfileLevel.AVCLevel13:
            case CodecProfileLevel.AVCLevel1b:
            case CodecProfileLevel.AVCLevel2:
                return null;
            case CodecProfileLevel.AVCLevel21:
                maxW = 352;
                maxH = 576;
                bitRate = 4000000;
                fps = 25;
                break;
            case CodecProfileLevel.AVCLevel22:
                maxW = 720;
                maxH = 480;
                bitRate = 4000000;
                fps = 15;
                break;
            case CodecProfileLevel.AVCLevel3:
                maxW = 720;
                maxH = 480;
                bitRate = 10000000;
                fps = 30;
                break;
            case CodecProfileLevel.AVCLevel31:
                maxW = 1280;
                maxH = 720;
                bitRate = 14000000;
                fps = 30;
                break;
            case CodecProfileLevel.AVCLevel32:
                maxW = 1280;
                maxH = 720;
                bitRate = 20000000;
                fps = 60;
                break;
            case CodecProfileLevel.AVCLevel4: // only try up to 1080p
            default:
                maxW = 1920;
                maxH = 1080;
                bitRate = 20000000;
                fps = 30;
                break;
        }
        info.mMaxW = maxW;
        info.mMaxH = maxH;
        info.mFps = fps;
        info.mBitRate = bitRate;
        Log.i(TAG, "AVC Level 0x" + Integer.toHexString(highestLevel) + " bit rate " + bitRate +
                " fps " + info.mFps + " w " + maxW + " h " + maxH);

        return info;
    }

    private void runVideoEncoding(int numSwap, CodecInfo info) {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, info.mMaxW, info.mMaxH);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, info.mBitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, info.mFps);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        MediaCodec encoder = null;
        InputSurface inputSurface = null;
        mVideoEncoderHadError = false;
        try {
            encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = new InputSurface(encoder.createInputSurface());
            inputSurface.makeCurrent();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            encoder.start();
            for (int i = 0; i < numSwap; i++) {
                GLES20.glClearColor(0.0f, 0.5f, 0.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
                inputSurface.swapBuffers();
                // dequeue buffers until not available
                int index = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
                while (index >= 0) {
                    encoder.releaseOutputBuffer(index, false);
                    // just throw away output
                    // allow shorter wait for 2nd round to move on quickly.
                    index = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC_SHORT);
                }
            }
            encoder.signalEndOfInputStream();
        } catch (Throwable e) {
            Log.w(TAG, "runVideoEncoding got error: " + e);
            mVideoEncoderHadError = true;
        } finally {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
            if (inputSurface != null) {
                inputSurface.release();
            }
        }
    }

    private void runAudioEncoding() {
        MediaFormat format = MediaFormat.createAudioFormat(MIME_TYPE_AUDIO, AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL_COUNT);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, AUDIO_AAC_PROFILE);
        format.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE);
        MediaCodec encoder = null;
        mAudioEncoderHadError = false;
        try {
            encoder = MediaCodec.createEncoderByType(MIME_TYPE_AUDIO);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            encoder.start();
            ByteBuffer[] inputBuffers = encoder.getInputBuffers();
            ByteBuffer source = ByteBuffer.allocate(inputBuffers[0].capacity());
            for (int i = 0; i < source.capacity()/2; i++) {
                source.putShort((short)i);
            }
            source.rewind();
            int currentInputBufferIndex = 0;
            long encodingLatencySum = 0;
            int totalEncoded = 0;
            int numRepeat = 0;
            while (mVideoEncodingOngoing) {
                numRepeat++;
                int inputIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                while (inputIndex == -1) {
                    inputIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC);
                }
                ByteBuffer inputBuffer = inputBuffers[inputIndex];
                inputBuffer.rewind();
                inputBuffer.put(source);
                long start = System.currentTimeMillis();
                totalEncoded += inputBuffers[inputIndex].limit();
                encoder.queueInputBuffer(inputIndex, 0, inputBuffer.limit(), 0, 0);
                source.rewind();
                int index = encoder.dequeueOutputBuffer(info, TIMEOUT_USEC);
                long end = System.currentTimeMillis();
                encodingLatencySum += (end - start);
                while (index >= 0) {
                    encoder.releaseOutputBuffer(index, false);
                    // just throw away output
                    // allow shorter wait for 2nd round to move on quickly.
                    index = encoder.dequeueOutputBuffer(info, TIMEOUT_USEC_SHORT);
                }
            }
            Log.w(TAG, "Audio encoding average latency " + encodingLatencySum / numRepeat +
                    " ms for average write size " + totalEncoded / numRepeat +
                    " total latency " + encodingLatencySum + " ms for total bytes " + totalEncoded);
        } catch (Throwable e) {
            Log.w(TAG, "runAudioEncoding got error: " + e);
            mAudioEncoderHadError = true;
        } finally {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
            }
        }
    }

    /**
     * Creates a MediaFormat with the basic set of values.
     */
    private static MediaFormat createMediaFormat() {
        MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, WIDTH, HEIGHT);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
        return format;
    }

    /**
     * Returns the first codec capable of encoding the specified MIME type, or null if no
     * match was found.
     */
    private static MediaCodecInfo selectCodec(String mimeType) {
        // FIXME: select codecs based on the complete use-case, not just the mime
        MediaCodecList mcl = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (MediaCodecInfo info : mcl.getCodecInfos()) {
            if (!info.isEncoder()) {
                continue;
            }

            String[] types = info.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    return info;
                }
            }
        }
        return null;
    }

    /**
     * Returns a color format that is supported by the codec and isn't COLOR_FormatSurface.  Throws
     * an exception if none found.
     */
    private static int findNonSurfaceColorFormat(MediaCodecInfo codecInfo, String mimeType) {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (colorFormat != MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface) {
                return colorFormat;
            }
        }
        fail("couldn't find a good color format for " + codecInfo.getName() + " / " + MIME_TYPE);
        return 0;   // not reached
    }

    private MediaExtractor getMediaExtractorForMimeType(final String resource,
            String mimeTypePrefix) throws IOException {
        Preconditions.assertTestFileExists(mInpPrefix + resource);
        MediaExtractor mediaExtractor = new MediaExtractor();
        File inpFile = new File(mInpPrefix + resource);
        ParcelFileDescriptor parcelFD =
                ParcelFileDescriptor.open(inpFile, ParcelFileDescriptor.MODE_READ_ONLY);
        AssetFileDescriptor afd = new AssetFileDescriptor(parcelFD, 0, parcelFD.getStatSize());
        try {
            mediaExtractor.setDataSource(
                    afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
        } finally {
            afd.close();
        }
        int trackIndex;
        for (trackIndex = 0; trackIndex < mediaExtractor.getTrackCount(); trackIndex++) {
            MediaFormat trackMediaFormat = mediaExtractor.getTrackFormat(trackIndex);
            if (trackMediaFormat.getString(MediaFormat.KEY_MIME).startsWith(mimeTypePrefix)) {
                mediaExtractor.selectTrack(trackIndex);
                break;
            }
        }
        if (trackIndex == mediaExtractor.getTrackCount()) {
            throw new IllegalStateException("couldn't get a video track");
        }

        return mediaExtractor;
    }

    private static boolean supportsCodec(String mimeType, boolean encoder) {
        MediaCodecList list = new MediaCodecList(MediaCodecList.ALL_CODECS);
        for (MediaCodecInfo info : list.getCodecInfos()) {
            if (encoder != info.isEncoder()) {
                continue;
            }

            for (String type : info.getSupportedTypes()) {
                if (type.equalsIgnoreCase(mimeType)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final UUID CLEARKEY_SCHEME_UUID =
            new UUID(0x1077efecc0b24d02L, 0xace33c1e52e2fb4bL);

    /**
     * Tests MediaCodec.CryptoException
     */
    @ApiTest(apis = "MediaCodec#CryptoException")
    @Test
    public void testCryptoException() {
        int errorCode = CryptoException.ERROR_KEY_EXPIRED;
        String errorMessage = "key_expired";
        CryptoException exception = new CryptoException(errorCode, errorMessage);

        assertEquals(errorCode, exception.getErrorCode());
        assertEquals(errorMessage, exception.getMessage());
    }

    /**
     * PCM encoding configuration test.
     *
     * If not specified in configure(), PCM encoding if it exists must be 16 bit.
     * If specified float in configure(), PCM encoding if it exists must be 16 bit, or float.
     *
     * As of Q, any codec of type "audio/raw" must support PCM encoding float.
     */
    @ApiTest(apis = {"android.media.AudioFormat#ENCODING_PCM_16BIT",
            "android.media.AudioFormat#ENCODING_PCM_FLOAT"})
    @MediumTest
    @Test
    public void testPCMEncoding() throws Exception {
        final MediaCodecList mcl = new MediaCodecList(MediaCodecList.ALL_CODECS);
        for (MediaCodecInfo codecInfo : mcl.getCodecInfos()) {
            final boolean isEncoder = codecInfo.isEncoder();
            final String name = codecInfo.getName();

            for (String type : codecInfo.getSupportedTypes()) {
                final MediaCodecInfo.CodecCapabilities ccaps =
                        codecInfo.getCapabilitiesForType(type);
                final MediaCodecInfo.AudioCapabilities acaps =
                        ccaps.getAudioCapabilities();
                if (acaps == null) {
                    break; // not an audio codec
                }

                // Deduce the minimum channel count (though prefer stereo over mono).
                final int channelCount = Math.min(acaps.getMaxInputChannelCount(), 2);

                // Deduce the minimum sample rate.
                final int[] sampleRates = acaps.getSupportedSampleRates();
                final Range<Integer>[] sampleRateRanges = acaps.getSupportedSampleRateRanges();
                assertNotNull("supported sample rate ranges must be non-null", sampleRateRanges);
                final int sampleRate = sampleRateRanges[0].getLower();

                // If sample rate array exists (it may not),
                // the minimum value must be equal with the minimum from the sample rate range.
                if (sampleRates != null) {
                    assertEquals("sample rate range and array should have equal minimum",
                            sampleRate, sampleRates[0]);
                    Log.d(TAG, "codec: " + name + " type: " + type
                            + " has both sampleRate array and ranges");
                } else {
                    Log.d(TAG, "codec: " + name + " type: " + type
                            + " returns null getSupportedSampleRates()");
                }

                // We create one format here for both tests below.
                final MediaFormat format = MediaFormat.createAudioFormat(
                    type, sampleRate, channelCount);

                // Bitrate field is mandatory for encoders (except FLAC).
                if (isEncoder) {
                    final int bitRate = acaps.getBitrateRange().getLower();
                    format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
                }

                // First test: An audio codec must be createable from a format
                // with the minimum sample rate and channel count.
                // The PCM encoding must be null (doesn't exist) or 16 bit.
                {
                    // Check encoding of codec.
                    final Integer actualEncoding = encodingOfAudioCodec(name, format, isEncoder);
                    if (actualEncoding != null) {
                        assertEquals("returned audio encoding must be 16 bit for codec: "
                                + name + " type: " + type + " encoding: " + actualEncoding,
                                AudioFormat.ENCODING_PCM_16BIT, actualEncoding.intValue());
                    }
                }

                // Second test: An audio codec configured with PCM encoding float must return
                // either an encoding of null (doesn't exist), 16 bit, or float.
                {
                    // Reuse the original format, and add float specifier.
                    format.setInteger(
                            MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_FLOAT);

                    // Check encoding of codec.
                    // The KEY_PCM_ENCODING key is advisory, so should not cause configuration
                    // failure.  The actual PCM encoding is returned from
                    // the input format (encoder) or output format (decoder).
                    final Integer actualEncoding = encodingOfAudioCodec(name, format, isEncoder);
                    if (actualEncoding != null) {
                        assertTrue(
                                "returned audio encoding must be 16 bit or float for codec: "
                                + name + " type: " + type + " encoding: " + actualEncoding,
                                actualEncoding == AudioFormat.ENCODING_PCM_16BIT
                                || actualEncoding == AudioFormat.ENCODING_PCM_FLOAT);
                        if (actualEncoding == AudioFormat.ENCODING_PCM_FLOAT) {
                            Log.d(TAG, "codec: " + name + " type: " + type + " supports float");
                        }
                    }

                    // As of Q, all codecs of type "audio/raw" must support float.
                    if (type.equals("audio/raw")) {
                        assertTrue(type + " must support float",
                                actualEncoding != null &&
                                actualEncoding.intValue() == AudioFormat.ENCODING_PCM_FLOAT);
                    }
                }
            }
        }
    }

    /**
     * Returns the PCM encoding of an audio codec, or null if codec doesn't exist,
     * or not an audio codec, or PCM encoding key doesn't exist.
     */
    private Integer encodingOfAudioCodec(String name, MediaFormat format, boolean encode)
            throws IOException {
        final int flagEncoder = encode ? MediaCodec.CONFIGURE_FLAG_ENCODE : 0;
        final MediaCodec codec = MediaCodec.createByCodecName(name);
        Integer actualEncoding = null;

        try {
            codec.configure(format, null /* surface */, null /* crypto */, flagEncoder);

            // Check input/output format - this must exist.
            final MediaFormat actualFormat =
                    encode ? codec.getInputFormat() : codec.getOutputFormat();
            assertNotNull("cannot get format for " + name, actualFormat);

            // Check actual encoding - this may or may not exist
            try {
                actualEncoding = actualFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
            } catch (Exception e) {
                ; // trying to get a non-existent key throws exception
            }
        } finally {
            codec.release();
        }
        return actualEncoding;
    }

    @ApiTest(apis = "android.media.AudioFormat#KEY_FLAC_COMPRESSION_LEVEL")
    @SmallTest
    @Test
    public void testFlacIdentity() throws Exception {
        final int PCM_FRAMES = 1152 * 4; // FIXME: requires 4 flac frames to work with OMX codecs.
        final int SAMPLES = PCM_FRAMES * AUDIO_CHANNEL_COUNT;
        final int[] SAMPLE_RATES = {AUDIO_SAMPLE_RATE, 192000}; // ensure 192kHz supported.

        for (int sampleRate : SAMPLE_RATES) {
            final MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_FLAC);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, AUDIO_CHANNEL_COUNT);

            Log.d(TAG, "Trying sample rate: " + sampleRate
                    + " channel count: " + AUDIO_CHANNEL_COUNT);
            // this key is only needed for encode, ignored for decode
            format.setInteger(MediaFormat.KEY_FLAC_COMPRESSION_LEVEL, 5);

            for (int i = 0; i < 2; ++i) {
                final boolean useFloat = (i == 1);
                final StreamUtils.PcmAudioBufferStream audioStream =
                    new StreamUtils.PcmAudioBufferStream(SAMPLES, sampleRate, 1000 /* frequency */,
                    100 /* sweep */, useFloat);

                if (useFloat) {
                    format.setInteger(
                        MediaFormat.KEY_PCM_ENCODING, AudioFormat.ENCODING_PCM_FLOAT);
                }

                final StreamUtils.MediaCodecStream rawToFlac = new StreamUtils.MediaCodecStream(
                    new StreamUtils.ByteBufferInputStream(audioStream), format, true /* encode */);
                final StreamUtils.MediaCodecStream flacToRaw = new StreamUtils.MediaCodecStream(
                    rawToFlac, format, false /* encode */);

                if (useFloat) { // ensure float precision supported at the sample rate.
                    assertTrue("No float FLAC encoder at " + sampleRate,
                            rawToFlac.mIsFloat);
                    assertTrue("No float FLAC decoder at " + sampleRate,
                            flacToRaw.mIsFloat);
                }

                // Note: the existence of signed zero (as well as NAN) may make byte
                // comparisons invalid for floating point output. In our case, since the
                // floats come through integer to float conversion, it does not matter.
                assertEquals("Audio data not identical after compression",
                    audioStream.sizeInBytes(),
                    StreamUtils.compareStreams(new StreamUtils.ByteBufferInputStream(flacToRaw),
                        new StreamUtils.ByteBufferInputStream(
                        new StreamUtils.PcmAudioBufferStream(audioStream))));
            }
        }
    }

    @ApiTest(apis = "MediaCodec#release")
    @Test
    public void testAsyncRelease() throws Exception {
        OutputSurface outputSurface = new OutputSurface(1, 1);
        MediaExtractor mediaExtractor = getMediaExtractorForMimeType(INPUT_RESOURCE, "video/");
        MediaFormat mediaFormat =
                mediaExtractor.getTrackFormat(mediaExtractor.getSampleTrackIndex());
        String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
        for (int i = 0; i < 100; ++i) {
            final MediaCodec codec = MediaCodec.createDecoderByType(mimeType);

            try {
                final ConditionVariable cv = new ConditionVariable();
                Runnable first = null;
                switch (i % 5) {
                    case 0:  // release
                        codec.configure(mediaFormat, outputSurface.getSurface(), null, 0);
                        codec.start();
                        first = () -> { cv.block(); codec.release(); };
                        break;
                    case 1:  // start
                        codec.configure(mediaFormat, outputSurface.getSurface(), null, 0);
                        first = () -> {
                            cv.block();
                            try {
                                codec.start();
                            } catch (Exception e) {
                                Log.i(TAG, "start failed", e);
                            }
                        };
                        break;
                    case 2:  // configure
                        first = () -> {
                            cv.block();
                            try {
                                codec.configure(mediaFormat, outputSurface.getSurface(), null, 0);
                            } catch (Exception e) {
                                Log.i(TAG, "configure failed", e);
                            }
                        };
                        break;
                    case 3:  // stop
                        codec.configure(mediaFormat, outputSurface.getSurface(), null, 0);
                        codec.start();
                        first = () -> {
                            cv.block();
                            try {
                                codec.stop();
                            } catch (Exception e) {
                                Log.i(TAG, "stop failed", e);
                            }
                        };
                        break;
                    case 4:  // flush
                        codec.configure(mediaFormat, outputSurface.getSurface(), null, 0);
                        codec.start();
                        codec.dequeueInputBuffer(0);
                        first = () -> {
                            cv.block();
                            try {
                                codec.flush();
                            } catch (Exception e) {
                                Log.i(TAG, "flush failed", e);
                            }
                        };
                        break;
                }

                Thread[] threads = new Thread[10];
                threads[0] = new Thread(first);
                for (int j = 1; j < threads.length; ++j) {
                    threads[j] = new Thread(() -> { cv.block(); codec.release(); });
                }
                for (Thread thread : threads) {
                    thread.start();
                }
                // Wait a little bit so that threads may reach block() call.
                Thread.sleep(50);
                cv.open();
                for (Thread thread : threads) {
                    thread.join();
                }
            } finally {
                codec.release();
            }
        }
    }

    @ApiTest(apis = "MediaCodec#setAudioPresentation")
    @Test
    public void testSetAudioPresentation() throws Exception {
        MediaFormat format = MediaFormat.createAudioFormat(
                MediaFormat.MIMETYPE_AUDIO_MPEG, 44100 /* sampleRate */, 2 /* channelCount */);
        String mimeType = format.getString(MediaFormat.KEY_MIME);
        MediaCodec codec = createCodecByType(
                format.getString(MediaFormat.KEY_MIME), false /* isEncoder */);
        assertNotNull(codec);
        assertThrows(NullPointerException.class, () -> {
            codec.setAudioPresentation(null);
        });
        codec.setAudioPresentation(
                (new AudioPresentation.Builder(42 /* presentationId */)).build());
    }

    @ApiTest(apis = "android.media.MediaFormat#KEY_PREPEND_HEADER_TO_SYNC_FRAMES")
    @Test
    public void testPrependHeadersToSyncFrames() throws IOException {
        MediaCodecList mcl = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (MediaCodecInfo info : mcl.getCodecInfos()) {
            boolean isEncoder = info.isEncoder();
            for (String mime: info.getSupportedTypes()) {
                CodecCapabilities caps = info.getCapabilitiesForType(mime);
                boolean isVideo = (caps.getVideoCapabilities() != null);
                boolean isAudio = (caps.getAudioCapabilities() != null);

                MediaCodec codec = null;
                MediaFormat format = null;
                try {
                    codec = MediaCodec.createByCodecName(info.getName());
                    if (isVideo) {
                        VideoCapabilities vcaps = caps.getVideoCapabilities();
                        int minWidth = vcaps.getSupportedWidths().getLower();
                        int minHeight = vcaps.getSupportedHeightsFor(minWidth).getLower();
                        int minBitrate = vcaps.getBitrateRange().getLower();
                        int minFrameRate = Math.max(vcaps.getSupportedFrameRatesFor(
                                minWidth, minHeight) .getLower().intValue(), 1);
                        format = MediaFormat.createVideoFormat(mime, minWidth, minHeight);
                        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, caps.colorFormats[0]);
                        format.setInteger(MediaFormat.KEY_BIT_RATE, minBitrate);
                        format.setInteger(MediaFormat.KEY_FRAME_RATE, minFrameRate);
                        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
                    } else if(isAudio){
                        AudioCapabilities acaps = caps.getAudioCapabilities();
                        int minSampleRate = acaps.getSupportedSampleRateRanges()[0].getLower();
                        int minChannelCount = 1;
                        if (mIsAtLeastS) {
                            minChannelCount = acaps.getMinInputChannelCount();
                        }
                        int minBitrate = acaps.getBitrateRange().getLower();
                        format = MediaFormat.createAudioFormat(mime, minSampleRate, minChannelCount);
                        format.setInteger(MediaFormat.KEY_BIT_RATE, minBitrate);
                    }

                    if (isVideo || isAudio) {
                        format.setInteger(MediaFormat.KEY_PREPEND_HEADER_TO_SYNC_FRAMES, 1);

                        codec.configure(format, null /* surface */, null /* crypto */,
                            isEncoder ? codec.CONFIGURE_FLAG_ENCODE : 0);
                    }
                    if (isVideo && isEncoder) {
                        Log.i(TAG, info.getName() + " supports KEY_PREPEND_HEADER_TO_SYNC_FRAMES");
                    } else {
                        Log.i(TAG, info.getName() + " is not a video encoder, so" +
                                " KEY_PREPEND_HEADER_TO_SYNC_FRAMES is no-op, as expected");
                    }
                    // TODO: actually test encoders prepend the headers to sync frames.
                } catch (IllegalArgumentException | CodecException e) {
                    if (isVideo && isEncoder) {
                        Log.i(TAG, info.getName() + " does not support" +
                                " KEY_PREPEND_HEADER_TO_SYNC_FRAMES");
                    } else {
                        fail(info.getName() + " is not a video encoder," +
                                " so it should not fail to configure.\n" + e.toString());
                    }
                } finally {
                    if (codec != null) {
                        codec.release();
                    }
                }
            }
        }
    }

    /**
     * Test if flushing early in the playback does not prevent client from getting the
     * latest configuration. Empirically, this happens most often when the
     * codec is flushed after the first buffer is queued, so this test walks
     * through the scenario.
     */
    @ApiTest(apis = "MediaCodec#flush")
    @Test
    public void testFlushAfterFirstBuffer() throws Exception {
        if (MediaUtils.check(mIsAtLeastR, "test needs Android 11")) {
            for (int i = 0; i < 100; ++i) {
                doFlushAfterFirstBuffer();
            }
        }
    }

    private void doFlushAfterFirstBuffer() throws Exception {
        MediaExtractor extractor = null;
        MediaCodec codec = null;

        try {
            MediaFormat newFormat = null;
            extractor = getMediaExtractorForMimeType(
                    "noise_2ch_48khz_aot29_dr_sbr_sig2_mp4.m4a", "audio/");
            int trackIndex = extractor.getSampleTrackIndex();
            MediaFormat format = extractor.getTrackFormat(trackIndex);
            codec = createCodecByType(
                    format.getString(MediaFormat.KEY_MIME), false /* isEncoder */);
            codec.configure(format, null, null, 0);
            codec.start();
            int firstInputIndex = codec.dequeueInputBuffer(0);
            while (firstInputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                firstInputIndex = codec.dequeueInputBuffer(5000);
            }
            assertTrue(firstInputIndex >= 0);
            extractor.readSampleData(codec.getInputBuffer(firstInputIndex), 0);
            codec.queueInputBuffer(
                    firstInputIndex, 0, Math.toIntExact(extractor.getSampleSize()),
                    extractor.getSampleTime(), extractor.getSampleFlags());
            // Don't advance, so the first buffer will be read again after flush
            codec.flush();
            ByteBuffer csd = format.getByteBuffer("csd-0");
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            // We don't need to decode many frames
            int numFrames = 10;
            boolean eos = false;
            while (!eos) {
                if (numFrames > 0) {
                    int inputIndex = codec.dequeueInputBuffer(0);
                    if (inputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        inputIndex = codec.dequeueInputBuffer(5000);
                    }
                    if (inputIndex >= 0) {
                        ByteBuffer inputBuffer = codec.getInputBuffer(inputIndex);
                        if (csd != null) {
                            inputBuffer.clear();
                            inputBuffer.put(csd);
                            codec.queueInputBuffer(
                                    inputIndex, 0, inputBuffer.position(), 0,
                                    MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
                            csd = null;
                        } else {
                            int size = extractor.readSampleData(inputBuffer, 0);
                            if (size <= 0) {
                                break;
                            }
                            int flags = extractor.getSampleFlags();
                            --numFrames;
                            if (numFrames <= 0) {
                                flags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                            }
                            codec.queueInputBuffer(
                                    inputIndex, 0, size, extractor.getSampleTime(), flags);
                            extractor.advance();
                        }
                    }
                }

                int outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0);
                if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    outputIndex = codec.dequeueOutputBuffer(bufferInfo, 5000);
                }
                if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    newFormat = codec.getOutputFormat();
                } else if (outputIndex >= 0) {
                    if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        eos = true;
                    }
                    codec.releaseOutputBuffer(outputIndex, false);
                }
            }
            assertNotNull(newFormat);
            assertEquals(48000, newFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
        } finally {
            if (extractor != null) {
                extractor.release();
            }
            if (codec != null) {
                codec.stop();
                codec.release();
            }
        }
    }

    @ApiTest(apis = {"MediaCodec#getSupportedVendorParameters",
            "MediaCodec#getParameterDescriptor",
            "MediaCodec#subscribeToVendorParameters",
            "MediaCodec#unsubscribeFromVendorParameters"})
    @Test
    public void testVendorParameters() {
        if (!MediaUtils.check(mIsAtLeastS, "test needs Android 12")) {
            return;
        }
        MediaCodecList mcl = new MediaCodecList(MediaCodecList.REGULAR_CODECS);
        for (MediaCodecInfo info : mcl.getCodecInfos()) {
            if (info.isAlias()) {
                continue;
            }
            MediaCodec codec = null;
            if (!TestUtils.isTestableCodecInCurrentMode(info.getName())) {
                Log.d(TAG, "skip testing codec " + info.getName() + " in current mode:"
                                + (TestUtils.isMtsMode() ? " MTS" : " CTS"));
                continue;
            }
            try {
                codec = MediaCodec.createByCodecName(info.getName());
                List<String> vendorParams = codec.getSupportedVendorParameters();
                if (VERBOSE) {
                    Log.d(TAG, "vendor params supported by " + info.getName() + ": " +
                            vendorParams.toString());
                }
                for (String name : vendorParams) {
                    MediaCodec.ParameterDescriptor desc = codec.getParameterDescriptor(name);
                    assertNotNull(name + " is in the list of supported parameters, so the codec" +
                            " should be able to describe it.", desc);
                    assertEquals("name differs from the name in the descriptor",
                            name, desc.getName());
                    assertTrue("type in the descriptor cannot be TYPE_NULL",
                            MediaFormat.TYPE_NULL != desc.getType());
                    if (VERBOSE) {
                        Log.d(TAG, name + " is of type " + desc.getType());
                    }
                }
                codec.subscribeToVendorParameters(vendorParams);

                // Build a MediaFormat that makes sense to the codec.
                String type = info.getSupportedTypes()[0];
                MediaFormat format = null;
                CodecCapabilities caps = info.getCapabilitiesForType(type);
                AudioCapabilities audioCaps = caps.getAudioCapabilities();
                VideoCapabilities videoCaps = caps.getVideoCapabilities();
                if (audioCaps != null) {
                    format = MediaFormat.createAudioFormat(
                            type,
                            audioCaps.getSupportedSampleRateRanges()[0].getLower(),
                            audioCaps.getMaxInputChannelCount());
                    if (info.isEncoder()) {
                        format.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE);
                    }
                } else if (videoCaps != null) {
                    int width = videoCaps.getSupportedWidths().getLower();
                    int height = videoCaps.getSupportedHeightsFor(width).getLower();
                    format = MediaFormat.createVideoFormat(type, width, height);
                    if (info.isEncoder()) {
                        EncoderCapabilities encCaps = caps.getEncoderCapabilities();
                        if (encCaps != null) {
                            int bitrateMode = -1;
                            List<Integer> candidates = Arrays.asList(
                                    EncoderCapabilities.BITRATE_MODE_VBR,
                                    EncoderCapabilities.BITRATE_MODE_CBR,
                                    EncoderCapabilities.BITRATE_MODE_CQ,
                                    EncoderCapabilities.BITRATE_MODE_CBR_FD);
                            for (int candidate : candidates) {
                                if (encCaps.isBitrateModeSupported(candidate)) {
                                    bitrateMode = candidate;
                                    break;
                                }
                            }
                            if (VERBOSE) {
                                Log.d(TAG, "video encoder: bitrate mode = " + bitrateMode);
                            }
                            format.setInteger(MediaFormat.KEY_BITRATE_MODE, bitrateMode);
                            switch (bitrateMode) {
                            case EncoderCapabilities.BITRATE_MODE_VBR:
                            case EncoderCapabilities.BITRATE_MODE_CBR:
                            case EncoderCapabilities.BITRATE_MODE_CBR_FD:
                                format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
                                break;
                            case EncoderCapabilities.BITRATE_MODE_CQ:
                                format.setInteger(
                                        MediaFormat.KEY_QUALITY,
                                        encCaps.getQualityRange().getLower());
                                if (VERBOSE) {
                                    Log.d(TAG, "video encoder: quality = " +
                                            encCaps.getQualityRange().getLower());
                                }
                                break;
                            default:
                                format.removeKey(MediaFormat.KEY_BITRATE_MODE);
                            }
                        }
                        format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
                        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
                        format.setInteger(
                                MediaFormat.KEY_COLOR_FORMAT,
                                CodecCapabilities.COLOR_FormatSurface);
                    }
                } else {
                    Log.i(TAG, info.getName() + " is in neither audio nor video domain; skipped");
                    codec.release();
                    continue;
                }
                codec.configure(
                        format, null, null,
                        info.isEncoder() ? MediaCodec.CONFIGURE_FLAG_ENCODE : 0);
                Surface inputSurface = null;
                if (videoCaps != null && info.isEncoder()) {
                    inputSurface = codec.createInputSurface();
                }
                codec.start();
                codec.unsubscribeFromVendorParameters(vendorParams);
                codec.stop();
            } catch (Exception e) {
                throw new RuntimeException("codec name: " + info.getName(), e);
            } finally {
                if (codec != null) {
                    codec.release();
                }
            }
        }
    }
}
