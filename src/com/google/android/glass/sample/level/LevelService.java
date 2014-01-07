/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.glass.sample.level;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.TimelineManager;

/**
 * The main application service that manages the lifetime of the live card.
 */
public class LevelService extends Service implements SensorEventListener {

    private static final String LIVE_CARD_ID = "level";

    private TimelineManager mTimelineManager;
    private LiveCard mLiveCard;
    private LevelRenderer mRenderer;
    private SensorManager mSensorManager;

    private LevelView mLevelView;

    @Override
    public void onCreate() {
        super.onCreate();

        mTimelineManager = TimelineManager.from(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO: is there a better way to do this?
        LayoutInflater inflater = LayoutInflater.from(this);
        FrameLayout layout = (FrameLayout) inflater.inflate(R.layout.level_live_card, null);
        mLevelView = (LevelView) layout.findViewById(R.id.level);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        registerListeners();

        if (mLiveCard == null) {
            mLiveCard = mTimelineManager.createLiveCard(LIVE_CARD_ID);
            mRenderer = new LevelRenderer(layout);

            mLiveCard.setDirectRenderingEnabled(true);
            mLiveCard.getSurfaceHolder().addCallback(mRenderer);

            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, LevelMenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

            mLiveCard.publish(LiveCard.PublishMode.REVEAL);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterListeners();

        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard.getSurfaceHolder().removeCallback(mRenderer);
            mLiveCard = null;
        }
        super.onDestroy();
    }

    /**
     * Register this activity as a listener for gravity sensor changes.
     */
    private void registerListeners() {
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_NORMAL);
    }
    /**
     * Unregister this activity as a listener.
     */
    private void unregisterListeners() {
        mSensorManager.unregisterListener(this);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Nothing to do here.
    }

    //TODO: should I disable this when the card is paused? If so, how do I restart it?
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            computeOrientation(event);
        }
    }

    /**
     * Compute the orientation angle.
     *
     * @param event Gravity values.
     */
    private void computeOrientation(SensorEvent event) {
        float angle = (float) -Math.atan(event.values[0]
                / Math.sqrt(event.values[1] * event.values[1] + event.values[2] * event.values[2]));
        Log.v("compute", "Computed new angle to " + angle);
        mLevelView.setAngle(angle);
    }
}