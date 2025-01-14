/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.cts.verifier.telecom;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.VideoProfile;
import android.util.ArraySet;
import android.util.Log;

import com.android.compatibility.common.util.ApiTest;
import com.android.cts.verifier.R;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of the {@link android.telecom.Connection} class used by the
 * {@link CtsConnectionService}.
 */
@ApiTest(apis={"android.Telecom.Connection"})
public class CtsConnection extends Connection {
    public static final String TAG = "CtsConnection";

    /**
     * Listener used to inform the CtsVerifier app of changes to a connection.
     */
    public static abstract class Listener {
        void onDestroyed(CtsConnection connection) { };
        void onDisconnect(CtsConnection connection) { };
        void onHold(CtsConnection connection) { };
        void onUnhold(CtsConnection connection) { };
        void onAnswer(CtsConnection connection, int videoState) { };
        void onReject(CtsConnection connection) { };
        void onShowIncomingCallUi(CtsConnection connection) { };
    }

    public static final String EXTRA_PLAY_CS_AUDIO =
            "com.android.cts.verifier.telecom.PLAY_CS_AUDIO";

    private final boolean mIsIncomingCall;
    private final Set<Listener> mListener = new ArraySet<>();
    private final MediaPlayer mMediaPlayer;
    private final Context mContext;
    private CountDownLatch mWaitForCallAudioStateChanged = new CountDownLatch(1);

    public CtsConnection(Context context, boolean isIncomingCall,
            Listener listener, boolean hasAudio) {
        mContext = context;
        mIsIncomingCall = isIncomingCall;
        mListener.add(listener);
        if (hasAudio) {
            mMediaPlayer = createMediaPlayer();
        } else {
            mMediaPlayer = null;
        }
    }

    public boolean isIncomingCall() {
        return mIsIncomingCall;
    }

    public void addListener(Listener listener) {
        mListener.add(listener);
    }

    @Override
    public void onDisconnect() {
        setDisconnectedAndDestroy(new DisconnectCause(DisconnectCause.LOCAL));

        if (mListener != null) {
            mListener.forEach(l -> l.onDisconnect(CtsConnection.this));
        }
    }


    @Override
    public void onHold() {
        setOnHold();

        if (mListener != null) {
            mListener.forEach(l -> l.onHold(CtsConnection.this));
        }

        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            Log.d(TAG, "onHold: pausing media player");
            mMediaPlayer.pause();
        }
    }

    @Override
    public void onUnhold() {
        setActive();

        if (mListener != null) {
            mListener.forEach(l -> l.onUnhold(CtsConnection.this));
        }

        if (mMediaPlayer != null) {
            Log.d(TAG, "onUnhold: starting media player");
            mMediaPlayer.start();
        }
    }

    @Override
    public void onAnswer(int videoState) {
        setVideoState(videoState);
        setActive();

        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            Log.d(TAG, "onAnswer: playing media");
            mMediaPlayer.start();
        }

        if (mListener != null) {
            mListener.forEach(l -> l.onAnswer(CtsConnection.this, videoState));
        }
    }

    @Override
    public void onAnswer() {
        onAnswer(VideoProfile.STATE_AUDIO_ONLY);
    }

    @Override
    public void onReject() {
        setDisconnectedAndDestroy(new DisconnectCause(DisconnectCause.REJECTED));

        if (mListener != null) {
            mListener.forEach(l -> l.onReject(CtsConnection.this));
        }
    }

    @Override
    public void onShowIncomingCallUi() {
        if (mListener != null) {
            mListener.forEach(l -> l.onShowIncomingCallUi(CtsConnection.this));
        }
    }

    public void onCallAudioStateChanged(CallAudioState state) {
        mWaitForCallAudioStateChanged.countDown();
        mWaitForCallAudioStateChanged = new CountDownLatch(1);

    }

    public void waitForAudioStateChanged() {
        try {
            mWaitForCallAudioStateChanged.await(CtsConnectionService.TIMEOUT_MILLIS,
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
        }
    }

    private void setDisconnectedAndDestroy(DisconnectCause cause) {
        setDisconnected(cause);
        destroy();

        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
        }

        if (mListener != null) {
            mListener.forEach(l -> l.onDestroyed(CtsConnection.this));
        }
    }

    private MediaPlayer createMediaPlayer() {
        Log.d(TAG, "createMediaPlayer");
        AudioAttributes voiceCallAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .build();
        int audioSessionId = ((AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE))
                .generateAudioSessionId();
        // Prepare the media player to play a tone when there is a call.
        MediaPlayer mediaPlayer = MediaPlayer.create(mContext, R.raw.telecom_test_call_audio,
                voiceCallAttributes, audioSessionId);
        mediaPlayer.setLooping(true);
        return mediaPlayer;
    }
}
