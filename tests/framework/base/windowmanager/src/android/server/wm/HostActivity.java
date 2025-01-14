/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.server.wm;

import static android.server.wm.app.Components.RenderService.BROADCAST_EMBED_CONTENT;
import static android.server.wm.app.Components.RenderService.EXTRAS_BUNDLE;
import static android.server.wm.app.Components.RenderService.EXTRAS_DISPLAY_ID;
import static android.server.wm.app.Components.RenderService.EXTRAS_HOST_TOKEN;
import static android.server.wm.app.Components.RenderService.EXTRAS_SURFACE_PACKAGE;
import static android.server.wm.app.Components.UnresponsiveActivity.EXTRA_ON_MOTIONEVENT_DELAY_MS;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.SurfaceControlViewHost;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.concurrent.CountDownLatch;


public class HostActivity extends Activity implements SurfaceHolder.Callback{
    private SurfaceView mSurfaceView;
    public CountDownLatch mEmbeddedViewAttachedLatch =  new CountDownLatch(1);
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {}

        @Override
        public void onServiceDisconnected(ComponentName className) {}
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SurfaceControlViewHost.SurfacePackage surfacePackage =
                    intent.getParcelableExtra(EXTRAS_SURFACE_PACKAGE);
            if (surfacePackage != null) {
                mSurfaceView.setChildSurfacePackage(surfacePackage);
                mEmbeddedViewAttachedLatch.countDown();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter(BROADCAST_EMBED_CONTENT);
        registerReceiver(mReceiver, filter, Context.RECEIVER_EXPORTED);

        final RelativeLayout content = new RelativeLayout(this);
        mSurfaceView = new SurfaceView(this);
        mSurfaceView.setZOrderOnTop(true);
        content.addView(mSurfaceView,
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT));
        setContentView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mSurfaceView.getHolder().addCallback(this);
    }

    @Override
    protected void onPause() {
        unregisterReceiver(mReceiver);
        super.onPause();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Intent mIntent = new Intent();
        mIntent.setComponent(new ComponentName(
                "android.server.wm.app", "android.server.wm.app.RenderService"));
        Bundle bundle = new Bundle();
        bundle.putBinder(EXTRAS_HOST_TOKEN, mSurfaceView.getHostToken());
        bundle.putInt(EXTRAS_DISPLAY_ID, getDisplay().getDisplayId());
        bundle.putInt(EXTRA_ON_MOTIONEVENT_DELAY_MS, 10000);
        mIntent.putExtra(EXTRAS_BUNDLE, bundle);
        bindService(mIntent, mConnection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}
}
