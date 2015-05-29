// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.

package org.thaliproject.p2p.btpollingsynclib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;

import java.util.List;

/**
 * Created by juksilve on 20.5.2015.
 */
public class MyDataSyncHandler implements BtPollingCallback, BTHandShaker.HandShakeCallback {

    MyDataSyncHandler that = this;

    Context context = null;
    PeerDatabaseHandler.PeerDataChangedCallback callback = null;
    BTConnectToThread mBTConnectToThread = null;
    BTHandShaker mBTHandShaker = null;

    BluetoothAdapter bluetooth = null;
    private Handler mHandler =  null;

    private List<PeerSynchStatus> synchList = null;
    private int pollingIndex = 0;

    final CountDownTimer nextRoundTimer = new CountDownTimer(5000, 500) {
        public void onTick(long millisUntilFinished) {
            // not using
        }
        public void onFinish() {
            DoNextPollingRound();
        }
    };

    public MyDataSyncHandler(Context Context,PeerDatabaseHandler.PeerDataChangedCallback Callback ){
        this.context = Context;
        this.callback = Callback;
        this.mHandler =  new Handler(this.context.getMainLooper());
        this.bluetooth = BluetoothAdapter.getDefaultAdapter();
    }

    public void Start(List<PeerSynchStatus> list){
        pollingIndex = 0;
        this.synchList = list;
        DoNextPollingRound();
    }

    public void Stop(){
        nextRoundTimer.cancel();

        if(mBTHandShaker != null){
            mBTHandShaker.tryCloseSocket();
            mBTHandShaker.Stop();
            mBTHandShaker = null;
        }

        if(mBTConnectToThread != null){
            mBTConnectToThread.Stop();
            mBTConnectToThread = null;
        }
    }

    private void DoNextPollingRound() {
        PeerSynchStatus nextDevice = getNextToSync();

        if(nextDevice != null){
            print_line("NR","Polling device : " + nextDevice.Name + ", at: " + nextDevice.Address + ", IsSynched : " + nextDevice.isSynched );
            BluetoothDevice device = this.bluetooth.getRemoteDevice(nextDevice.Address.trim());
            mBTConnectToThread = new BTConnectToThread(this,device,nextDevice.Data);
            mBTConnectToThread.start();
        }else{
            print_line("NR","we are done");
            // we have synched all, so we can now rest untill we have new data to sync
        }
    }

    private PeerSynchStatus getNextToSync(){

        PeerSynchStatus ret = null;
        if(synchList != null && synchList.size() > 0) {
            pollingIndex = pollingIndex + 1;
            if (pollingIndex > synchList.size()){
                pollingIndex = 0;
            }

            int tmpIndex = -1;

            for (int i = 0; i < synchList.size(); i++) {

                if(ret != null){
                    break;
                }else {
                    if (!synchList.get(i).isSynched && synchList.get(i).Address.length() > 0) {
                        if (i < pollingIndex) {
                            if (tmpIndex < 0) {
                                tmpIndex = i;
                            }
                        } else {
                            pollingIndex = i;
                            ret = synchList.get(i);
                        }
                    }
                }
            }

            print_line("NR","tmp index : " + tmpIndex + " p: " + pollingIndex + ", ret: " +ret + ", size: " + synchList.size());

            if(ret == null && tmpIndex >= 0 && tmpIndex < synchList.size()){
                pollingIndex = tmpIndex;
                ret = synchList.get(tmpIndex);
            }
        }



        return ret;
    }

    @Override
    public void Connected(BluetoothSocket socket, String syncData) {
        //make sure we do not close the socket,
        if(mBTHandShaker == null) {

            final BluetoothSocket tmp = socket;
            final String syncDataTmp = syncData;
            mBTConnectToThread = null;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBTHandShaker = new BTHandShaker(tmp, that, false);
                    mBTHandShaker.Start(syncDataTmp);
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
                print_line("conn", "CreateSocketFailed: " + tmp);
                if(mBTConnectToThread != null){
                    mBTConnectToThread.Stop();
                    mBTConnectToThread = null;
                }
                nextRoundTimer.cancel();
                nextRoundTimer.start();
            }
        });
    }

    @Override
    public void ConnectionFailed(String reason) {
        final String tmp = reason;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                print_line("conn", "ConnectionFailed: " + tmp);
                if(mBTConnectToThread != null){
                    mBTConnectToThread.Stop();
                    mBTConnectToThread = null;
                }
                nextRoundTimer.cancel();
                nextRoundTimer.start();
            }
        });
    }

    @Override
    public void HandShakeFailed(String reason, boolean isIncoming) {
        if(mBTHandShaker != null){
            mBTHandShaker.tryCloseSocket();
            mBTHandShaker.Stop();
            mBTHandShaker = null;
        }

        if(mBTConnectToThread != null){
            mBTConnectToThread.Stop();
            mBTConnectToThread = null;
        }
        nextRoundTimer.start();
    }

    @Override
    public void HandShakeOk(BluetoothSocket socket, String data, boolean isIncoming) {
        this.callback.SyncDataChanged(socket.getRemoteDevice().getAddress(), socket.getRemoteDevice().getName(),data, true);
        // we'll be re-freshed by the app after the database is updated, so we don't need to do anyting here
    }

    public void print_line(String who, String line) {
        //latsDbgString = who + " : " + line;
        Log.i("MyDataSyncHandler" + who, line);
    }
}
