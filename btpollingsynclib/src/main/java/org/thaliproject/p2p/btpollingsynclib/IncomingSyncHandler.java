
// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btpollingsynclib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.List;

/**
 * Created by juksilve on 20.5.2015.
 */
public class IncomingSyncHandler implements BtListenCallback, BTHandShaker.HandShakeCallback {

    IncomingSyncHandler that = this;
    Context context = null;
    BTListenerThread mBTListenerThread = null;
    BTHandShaker mBTHandShaker = null;

    BluetoothAdapter bluetooth = null;
    private Handler mHandler =  null;
    PeerDatabaseHandler.PeerDataChangedCallback callback = null;


    public IncomingSyncHandler(Context Context, PeerDatabaseHandler.PeerDataChangedCallback Callback){
        this.context = Context;
        this.callback = Callback;
        this.mHandler =  new Handler(this.context.getMainLooper());
        this.bluetooth = BluetoothAdapter.getDefaultAdapter();
    }

    public void Start(){
        startListener();
    }

    public void Stop(){
        if(mBTHandShaker != null){
            mBTHandShaker.tryCloseSocket();
            mBTHandShaker.Stop();
            mBTHandShaker = null;
        }

        if (mBTListenerThread != null) {
            mBTListenerThread.Stop();
            mBTListenerThread = null;
        }
    }
    private  void startListener() {

        if (mBTListenerThread == null && this.bluetooth != null) {
            print_line("", "StartBluetooth listener");
            mBTListenerThread = new BTListenerThread(this,this.bluetooth);
            mBTListenerThread.start();
        }
    }

    @Override
    public void GotConnection(BluetoothSocket socket) {
        if(mBTHandShaker == null) {
            final BluetoothSocket tmp = socket;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBTHandShaker = new BTHandShaker(tmp, that, true);
                    mBTHandShaker.Start("not_needed_to_say_anyting");// the String is only used to send data, not used when receiving
                }
            });
        }
    }

    @Override
    public void CreateSocketFailed(String reason) {
        final String tmp = reason;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                print_line("LISTEN", "Error: " + tmp);
                //only care if we have not stoppeed & nulled the instance
                if (mBTListenerThread != null) {
                    mBTListenerThread.Stop();
                    mBTListenerThread = null;
                    startListener();
                }
            }
        });
    }

    @Override
    public void ListeningFailed(String reason) {
        final String tmp = reason;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                print_line("LISTEN", "Error: " + tmp);
                //only care if we have not stoppeed & nulled the instance
                if (mBTListenerThread != null) {
                    mBTListenerThread.Stop();
                    mBTListenerThread = null;
                    startListener();
                }
            }
        });
    }

    @Override
    public void HandShakeFailed(String reason, boolean isIncoming) {
        ListeningFailed("HandShake failed: " + reason);
    }

    @Override
    public void HandShakeOk(BluetoothSocket socket, String data, boolean isIncoming) {
        this.callback.GotSyncData(socket.getRemoteDevice().getAddress(), socket.getRemoteDevice().getName(),data);
        //we gotta cleanup & re-start here. The app is just updating the database with data.
        Stop();
        startListener();
    }

    public void print_line(String who, String line) {
        //latsDbgString = who + " : " + line;
        Log.i("IncomingSyncHandler" + who, line);
    }
}
