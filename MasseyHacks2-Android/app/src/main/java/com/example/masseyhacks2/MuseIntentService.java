package com.example.masseyhacks2;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by Bruno on 2016-05-22.
 */
public class MuseIntentService extends IntentService {

    public static String museIntentKey = "muse";

    public MuseIntentService() {
        this("MuseIntentService");
    }

    public MuseIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {}
}
