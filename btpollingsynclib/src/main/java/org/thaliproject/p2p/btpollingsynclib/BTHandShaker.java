// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btpollingsynclib;

import android.bluetooth.BluetoothSocket;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by juksilve on 11.3.2015.
 */

public class BTHandShaker {

    private final BluetoothSocket mmSocket;
    private final HandShakeCallback callback;
    private final boolean isIncoming;

    String handShakeBuf = "";
    String shakeBackBuf = "shakehand";

    BTHandShakeSocketTread mBTHandShakeSocketTread = null;

    public interface HandShakeCallback{
        public void HandShakeFailed(String reason,boolean isIncoming);
        public void HandShakeOk(BluetoothSocket socket, String data,boolean isIncoming);
    }


    final CountDownTimer HandShakeTimeOutTimer = new CountDownTimer(4000, 1000) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {
            callback.HandShakeFailed("TimeOut",isIncoming);
        }
    };

    public BTHandShaker(BluetoothSocket socket, HandShakeCallback Callback, boolean incoming) {
        print_line("Creating BTHandShaker");
        callback = Callback;
        mmSocket = socket;
        isIncoming = incoming;
    }

    public void Start(String syncData) {
        print_line("Start");
        HandShakeTimeOutTimer.start();

        mBTHandShakeSocketTread = new BTHandShakeSocketTread(mmSocket,mHandler);
        mBTHandShakeSocketTread.start();

        if(!isIncoming) {
            handShakeBuf = syncData;
            mBTHandShakeSocketTread.write(handShakeBuf.getBytes());
        }
    }

    public void tryCloseSocket() {
        if(mBTHandShakeSocketTread != null){
            mBTHandShakeSocketTread.CloseSocket();
        }
    }

    public void Stop() {
        print_line("Stop");
        HandShakeTimeOutTimer.cancel();
        if(mBTHandShakeSocketTread != null){
            mBTHandShakeSocketTread = null;
        }
    }

    private void print_line(String message){
           Log.d("BTHandShaker", "BTHandShaker: " + message);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mBTHandShakeSocketTread != null) {
                switch (msg.what) {
                    case BTHandShakeSocketTread.MESSAGE_WRITE: {
                        byte[] writeBuf = (byte[]) msg.obj;// construct a string from the buffer
                        String writeMessage = new String(writeBuf);
                        print_line("MESSAGE_WRITE: " +writeMessage + ", is " + msg.arg1 + " bytes.");
                    }
                    break;
                    case BTHandShakeSocketTread.MESSAGE_READ: {
                        byte[] readBuf = (byte[]) msg.obj;// construct a string from the valid bytes in the buffer
                        String readMessage = new String(readBuf, 0, msg.arg1);
                        print_line("got MESSAGE_READ: " + readMessage + " , is  " + msg.arg1 + " bytes.");

                        if (isIncoming) {
                           mBTHandShakeSocketTread.write(shakeBackBuf.getBytes());
                           callback.HandShakeOk(mmSocket,readMessage, isIncoming);
                        }else{
                            callback.HandShakeOk(mmSocket,handShakeBuf, isIncoming);
                        }
                    }
                    break;
                    case BTHandShakeSocketTread.SOCKET_DISCONNEDTED: {

                        callback.HandShakeFailed("SOCKET_DISCONNEDTED", isIncoming);
                    }
                    break;
                }
            } else {
                print_line("handleMessage called for NULL thread handler");
            }
        }
    };
}
