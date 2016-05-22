/**
 * NotificationHelper.java
 * Implements the NotificationHelper class
 * A NotificationHelper creates and configures a notification
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-16 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.helpers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.y20k.transistor.MainActivity;
import org.y20k.transistor.PlayerService;
import org.y20k.transistor.R;
import org.y20k.transistor.core.Collection;
import org.y20k.transistor.core.Station;


/**
 * NotificationHelper class
 */
public final class NotificationHelper {

    /* Define log tag */
    private static final String LOG_TAG = NotificationHelper.class.getSimpleName();


    /* Main class variables */
    private static MediaSessionCompat mSession;
    private static String mStationMetadata;
    private static int mStationID;
    private static String mStationName;
    private static Service mLastUsedService;
    private static Collection mCollection;

    public static void initialize(Collection collection) {
        mCollection = collection;
    }

    public static void updateNotification() {
        if (mLastUsedService == null) {
            Log.i(LOG_TAG, "PlayerService not started yet, cannot create notification");
            return;
        }
        createNotification(mLastUsedService);
    }

    /* Create and put up notification */
    public static void createNotification(final Service context) {
        NotificationCompat.Builder builder;
        Notification notification;
        NotificationManager notificationManager;
        String notificationText;
        String notificationTitle;
        // int notificationColor;

        // retrieve notification system service
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // create content of notification
        // notificationColor = ContextCompat.getColor(context, R.color.transistor_grey_dark);
        notificationTitle = context.getString(R.string.notification_playing) + ": " + mStationName;
        if (mStationMetadata != null) {
            notificationText = mStationMetadata;
        } else {
            notificationText = mStationName;
        }

        // explicit intent for notification tap
        Intent tapIntent = new Intent(context, MainActivity.class);
        tapIntent.setAction(TransistorKeys.ACTION_SHOW_PLAYER);
        tapIntent.putExtra(TransistorKeys.EXTRA_STATION_ID, mStationID);

        // explicit intent for notification swipe
        Intent stopActionIntent = new Intent(context, PlayerService.class);
        stopActionIntent.setAction(TransistorKeys.ACTION_STOP);


        // artificial back stack for started Activity.
        // -> navigating backward from the Activity leads to Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // backstack: adds back stack for Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // backstack: add explicit intent for notification tap
        stackBuilder.addNextIntent(tapIntent);


        // pending intent wrapper for notification tap
        PendingIntent tapPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        // pending intent wrapper for notification stop action
        PendingIntent stopActionPendingIntent = PendingIntent.getService(context, 0, stopActionIntent, 0);

        // construct notification in builder
        builder = new NotificationCompat.Builder(context);
        builder.setSmallIcon(R.drawable.ic_notification_small_24dp);
        builder.setLargeIcon(getStationIcon(context, mStationID));
        builder.setContentTitle(notificationTitle);
        builder.setContentText(notificationText);
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText));
        builder.addAction (R.drawable.ic_stop_white_36dp, context.getString(R.string.notification_stop), stopActionPendingIntent);
        builder.setOngoing(true);
        // builder.setColor(notificationColor);
        builder.setContentIntent(tapPendingIntent);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if (mSession != null) {
            NotificationCompat.MediaStyle style = new NotificationCompat.MediaStyle();
            style.setMediaSession(mSession.getSessionToken());
            style.setShowActionsInCompactView(0);
            builder.setStyle(style);

        } else {
            Log.e(LOG_TAG, "MediaSession not initialized");
        }

        // build notification
        notification = builder.build();

        // display notification
        // System will never kill a service which has a foreground notification,
        // but it will kill a service without notification, so you open few other apps and get a notification and no music
        context.startForeground(TransistorKeys.PLAYER_SERVICE_NOTIFICATION_ID, notification);

        if (mLastUsedService != context) {
            mLastUsedService = context;
        }
    }


    /* Get station image for notification's large icon */
    private static Bitmap getStationIcon(Context context, int stationID) {
        if (mCollection == null) {
            return null;
        }

        Station station = mCollection.getStations().get(stationID);
        // create station image icon
        ImageHelper imageHelper;
        Bitmap stationImage;
        Bitmap stationIcon;

        if (station.getStationImageFile().exists()) {
            // use station image
            stationImage = BitmapFactory.decodeFile(station.getStationImageFile().toString());
        } else {
            stationImage = null;
        }
        imageHelper = new ImageHelper(stationImage, context);
        stationIcon = imageHelper.createStationIcon(512);

        return stationIcon;
    }


    /* Setter for current media session */
    public static void setMediaSession(MediaSessionCompat session) {
        mSession = session;
    }

    /* Setter for name of station */
    public static void setStationName(String stationName) {
        mStationName = stationName;
    }

    /* Setter for name of station */
    public static void setStationID(int stationID) {
        mStationID = stationID;
    }

    /* Setter for metadata of station */
    public static void setStationMetadata(String stationMetadata) {
        mStationMetadata = stationMetadata;
    }

}
