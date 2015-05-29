// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.

package org.thaliproject.p2p.btinsecuresync;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;

import org.thaliproject.p2p.btpollingsynclib.BtPollingSyncService;

/**
 * Created by juksilve on 21.5.2015.
 */
public class MyDataHandlerApp extends Application {

    BtPollingSyncService myService;
    boolean isBound = false;

    PeerManagerDb db = null;


    private ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i("MyDataHandlerApp", "onServiceConnected.\n");
            BtPollingSyncService.MyLocalBinder binder = (BtPollingSyncService.MyLocalBinder) service;
            myService = binder.getService();
            isBound = true;
        }

        public void onServiceDisconnected(ComponentName arg0) {
            Log.i("MyDataHandlerApp", "onServiceDisconnected.\n");
            isBound = false;
            myService = null;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        db = new PeerManagerDb(this);
        Intent i = new Intent(this, BtPollingSyncService.class);
        bindService(i, myConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if(db != null){
            db.closeDataBase();
            db = null;
        }
        if (isBound) {
            Log.i("MyDataHandlerApp","onDestroy -- unbindService");
            unbindService(myConnection);
            isBound = false;
        }
    }

    public PeerManagerDb GetDb(){
        return db;
    }
    public BtPollingSyncService GetService(){
        return myService;
    }
    public boolean isServiceBound(){ return isBound;}


}

