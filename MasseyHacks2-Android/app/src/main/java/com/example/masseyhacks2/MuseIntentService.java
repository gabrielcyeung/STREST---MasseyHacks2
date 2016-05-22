package com.example.masseyhacks2;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseDataPacketType;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Created by Bruno on 2016-05-22.
 */
public class MuseIntentService extends IntentService {

    private static final String TAG = "MuseIntentService";

    public static final String EEG_RANGE_LOW_INTENT_KEY = "eegRangeLow";
    public static final String EEG_RANGE_HIGH_INTENT_KEY = "eegRangeHigh";

    private boolean runService = true;

    private MuseManagerAndroid manager;
    private Muse muse = null;

    private ConnectionListener connectionListener;
    private DataListener dataListener;

    /**
     * Data comes in from the headband at a very fast rate; 220Hz, 256Hz or 500Hz,
     * depending on the type of headband and the preset configuration.  We buffer the
     * data that is read until we can update the UI.
     *
     * The stale flags indicate whether or not new data has been received and the buffers
     * hold the values of the last data packet received.  We are displaying the EEG, ALPHA_RELATIVE
     * and ACCELEROMETER values in this example.
     *
     * Note: the array lengths of the buffers are taken from the comments in
     * MuseDataPacketType, which specify 3 values for accelerometer and 6
     * values for EEG and EEG-derived packets.
     */
    private final double[] eegBuffer = new double[6];
    Range eegRange = new Range(100, 800);

    private boolean isStressCurrentlyDetected = false;


    public MuseIntentService() {
        this("MuseIntentService");
    }

    public MuseIntentService(String name) {
        super(name);

        Log.v(TAG, "MuseIntentService()");

        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);
        manager.startListening();

        WeakReference<MuseIntentService> weakIntentService =
                new WeakReference<MuseIntentService>(this);

        manager.setMuseListener(new Listener(weakIntentService));
        connectionListener = new ConnectionListener(weakIntentService);
        dataListener = new DataListener(weakIntentService);
    }

    @Override
    public void onCreate() {
        Log.v(TAG, "onCreate()");
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.v(TAG, "onHandleIntent()");
        eegRange = new Range(   intent.getDoubleExtra(EEG_RANGE_LOW_INTENT_KEY, 100),
                intent.getDoubleExtra(EEG_RANGE_HIGH_INTENT_KEY, 800)   );

        while (runService) {}
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy()");

        super.onDestroy();

        manager.stopListening();
    }

    private void museListChanged() {
        Log.v(TAG, "mustListChanged()");

        List<Muse> availableMuses = manager.getMuses();

        // Check that we actually have something to connect to.
        if (availableMuses.size() < 1) {
            Log.w(TAG, "There is nothing to connect to");
        } else {
            muse = availableMuses.get(0);

            muse.unregisterAllListeners();
            muse.registerConnectionListener(connectionListener);
            muse.registerDataListener(dataListener, MuseDataPacketType.EEG);

            // Initiate a connection to the headband and stream the data asynchronously.
            muse.runAsynchronously();
        }
    }

    private void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
        Log.v(TAG, "receiveMusicConnectionPacket()");

        final ConnectionState current = p.getCurrentConnectionState();

        // Format a message to show the change of connection state in the UI.
        //final String status = p.getPreviousConnectionState() + " -> " + current;
        final String status = current.toString();
        Log.i(TAG, status);

        if (current == ConnectionState.DISCONNECTED) {
            Log.i(TAG, "Muse disconnected:" + muse.getName());
            // We have disconnected from the headband, so set our cached copy to null.
            this.muse = null;

            runService = false;
        }
    }

    private void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
        Log.v(TAG, "receiveMuseDataPacket()");

        // valuesSize returns the number of data values contained in the packet.
        final long n = p.valuesSize();
        switch (p.packetType()) {
            case EEG:
                assert(eegBuffer.length >= n);
                getEegChannelValues(eegBuffer, p);
                break;

            default:
                break;
        }
    }

    private void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
        Log.v(TAG, "receiveMuseArtifactPacket()");
    }


    private void getEegChannelValues(double[] buffer, MuseDataPacket p) {
        Log.v(TAG, "getEegChannelValues()");

        buffer[0] = p.getEegChannelValue(Eeg.EEG1);
        buffer[1] = p.getEegChannelValue(Eeg.EEG2);
        buffer[2] = p.getEegChannelValue(Eeg.EEG3);
        buffer[3] = p.getEegChannelValue(Eeg.EEG4);
        buffer[4] = p.getEegChannelValue(Eeg.AUX_LEFT);
        buffer[5] = p.getEegChannelValue(Eeg.AUX_RIGHT);

        double sum = 0;
        for (int a = 0; a <= 5; a++) {
            sum += buffer[a];
        }
        final double average = sum / 6;

        if (average > eegRange.high && !isStressCurrentlyDetected) {
            isStressCurrentlyDetected = true;
            Log.v(TAG, "getEegChannelValues(): isStressCurrentlyDetected = true");

            sendStressDetectedNotification();

            new CountDownTimer(30000, 1000) {
                public void onTick(long millisUntilFinished) {
                    if (average <= eegRange.high && isStressCurrentlyDetected) {
                        isStressCurrentlyDetected = false;
                        Log.v(TAG, "getEegChannelValues(): isStressCurrentlyDetected = false");

                        cancelStressDetectedNotification();
                    }
                }

                public void onFinish() {
                    if (isStressCurrentlyDetected) {
                        isStressCurrentlyDetected = false;
                        Log.v(TAG, "getEegChannelValues(): isStressCurrentlyDetected = false");

                        cancelStressDetectedNotification();
                    }
                }
            }.start();

        }
    }


    private void sendStressDetectedNotification() {
        Log.v(TAG, "sendStressDetectedNotification()");

        android.support.v4.app.NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Stress detected")
                        .setContentText("Take a break, stand up, and walk around!")
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(Notification.CATEGORY_STATUS)
                        .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                        .setOngoing(true);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(0, mBuilder.build());
    }

    private void cancelStressDetectedNotification() {
        Log.v(TAG, "cancelStressDetectedNotification()");

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0);
    }


    class Listener extends MuseListener {
        final WeakReference<MuseIntentService> intentServiceRef;

        Listener(final WeakReference<MuseIntentService> intentServiceRef) {
            this.intentServiceRef = intentServiceRef;
        }

        @Override
        public void museListChanged() {
            intentServiceRef.get().museListChanged();
        }
    }

    class ConnectionListener extends MuseConnectionListener {
        final WeakReference<MuseIntentService> intentServiceRef;

        ConnectionListener(final WeakReference<MuseIntentService> intentServiceRef) {
            this.intentServiceRef = intentServiceRef;
        }

        @Override
        public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
            intentServiceRef.get().receiveMuseConnectionPacket(p, muse);
        }
    }

    class DataListener extends MuseDataListener {
        final WeakReference<MuseIntentService> intentServiceRef;

        DataListener(final WeakReference<MuseIntentService> intentServiceRef) {
            this.intentServiceRef = intentServiceRef;
        }

        @Override
        public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
            intentServiceRef.get().receiveMuseDataPacket(p, muse);
        }

        @Override
        public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
            intentServiceRef.get().receiveMuseArtifactPacket(p, muse);
        }
    }

}
