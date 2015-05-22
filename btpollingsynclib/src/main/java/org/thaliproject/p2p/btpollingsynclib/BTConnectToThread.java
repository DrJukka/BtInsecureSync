// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btpollingsynclib;


import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by juksilve on 12.3.2015.
 */
public class BTConnectToThread extends Thread {

    private final BtPollingCallback callback;
    private final BluetoothSocket mSocket;
    String tmpData = "";
    boolean mStopped = false;


    public BTConnectToThread(BtPollingCallback Callback, BluetoothDevice device,String syncData) {
        tmpData = syncData;
        callback = Callback;
        BluetoothSocket tmp = null;
        try {
            tmp = device.createInsecureRfcommSocketToServiceRecord(BluetoothBase.BtUUID);
        } catch (IOException e) {
            print_line("createInsecure.. failed: " + e.toString());
            callback.CreateSocketFailed(e.toString());
        }
        mSocket = tmp;
    }

    public void run() {
        print_line("Starting to connect");
        if(mSocket != null && callback != null) {
            try {
                mSocket.connect();
                //return success
                callback.Connected(mSocket,tmpData);
            } catch (IOException e) {
                print_line("socket connect failed: " + e.toString());
                try {
                    mSocket.close();
                } catch (IOException ee) {
                    print_line("closing socket 2 failed: " + ee.toString());
                }

                if(!mStopped) {
                    mStopped = true;
                    callback.ConnectionFailed(e.toString());
                }
            }
        }
    }

    private void print_line(String message) {
       //    Log.d("BTConnectToThread", "BTConnectToThread: " + message);
    }

    public void Stop() {
        try {
            mStopped = true;
            if(mSocket != null) {
                mSocket.close();
            }
        } catch (IOException e) {
            print_line("closing socket failed: " + e.toString());
        }
    }
}
