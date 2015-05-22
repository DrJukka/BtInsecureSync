package org.thaliproject.p2p.btpollingsynclib;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by juksilve on 20.5.2015.
 */
public class BtPollingSyncService extends Service implements BluetoothBase.BluetoothStatusChanged, PeerDatabaseHandler.PeerDataChangedCallback {

    final BtPollingSyncService that = this;

    static final public String DSS_DEBUG_VALUES = "test.microsoft.com.wifidirecttest.DSS_DEBUG_VALUES";
    static final public String DSS_DEBUG_MESSAGE = "test.microsoft.com.wifidirecttest.DSS_DEBUG_MESSAGE";

    BluetoothBase mBluetoothBase = null;

    IncomingSyncHandler mIncomingSyncHandler = null;
    MyDataSyncHandler mMyDataSyncHandler = null;
    PeerDatabaseHandler mPeerItemsHandler = null;

    private final IBinder mBinder = new MyLocalBinder();

    public class MyLocalBinder extends Binder {
        public BtPollingSyncService getService() {
            return BtPollingSyncService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        print_line("","onStartCommand rounds so far :");
        Start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        print_line("","onDestroy");
        Stop();
    }

    public boolean isRunnuing(){
        boolean ret = false;
        //check if we are running
        if(mBluetoothBase != null){
            ret = true;
        }
        return ret;
    }

    public void Start() {
        Stop();

        mPeerItemsHandler = new PeerDatabaseHandler(this);

        mBluetoothBase = new BluetoothBase(this,this);
        mBluetoothBase.Start();

        startAll();
    }

    public void Stop() {
        stopAll();

        if(mBluetoothBase != null){
            mBluetoothBase.Stop();
            mBluetoothBase = null;
        }

        if(mPeerItemsHandler != null) {
            mPeerItemsHandler.closeDataBase();
            mPeerItemsHandler = null;
        }
    }

    public void startAll(){
        if(mPeerItemsHandler != null){
            print_line("", "startAll");
            mIncomingSyncHandler = new IncomingSyncHandler(this,this);
            mIncomingSyncHandler.Start();

            mMyDataSyncHandler = new MyDataSyncHandler(this,this);
            mMyDataSyncHandler.Start(mPeerItemsHandler.GetPeerSynchStatusList());
        }
    }

    public void stopAll(){
        print_line("", "stopAll");
        if (mIncomingSyncHandler != null) {
            mIncomingSyncHandler.Stop();
            mIncomingSyncHandler = null;
        }

        if(mMyDataSyncHandler != null){
            mMyDataSyncHandler.Stop();
            mMyDataSyncHandler = null;
        }
    }

    @Override
    public void SyncDataChanged(String Address, String Name, String data, boolean isSynched) {

        if(mPeerItemsHandler != null && mMyDataSyncHandler != null) {

            if(isSynched) {
                print_line("- Sent", Name + ":\"" + data + "\"");
            }else{

            }
            mPeerItemsHandler.addOrUpdateSyncItem(Address, Name, data, isSynched);

            //Re-start with updated list
            mMyDataSyncHandler.Stop();
            mMyDataSyncHandler.Start(mPeerItemsHandler.GetPeerSynchStatusList());
        }
    }

    @Override
    public void GotSyncData(String Address, String Name, String data) {
        if(mPeerItemsHandler != null ) {
            print_line("+ Got", Name + ":\"" + data + "\"");
            mPeerItemsHandler.addOrUpdateItemData(Address, Name, data);
        }

        ShowToast(Name + " : " + data);
    }

    private void ShowToast(String message) {
        Toast toast = Toast.makeText(this, message,Toast.LENGTH_LONG);
        toast.show();
    }


    @Override
    public void BluetoothStateChanged(int state) {
        if (state == BluetoothAdapter.SCAN_MODE_NONE) {
            print_line("BT", "Bluetooth DISABLED, stopping");
            stopAll();
        } else if (state == BluetoothAdapter.SCAN_MODE_CONNECTABLE
                || state == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            print_line("BT", "Bluetooth enabled, re-starting");
            startAll();
        }
    }
    public void print_line(String who, String line) {
        //latsDbgString = who + " : " + line;
        Log.i("BtPollingSyncService" + who, line);

        Intent intent = new Intent(DSS_DEBUG_VALUES);
        intent.putExtra(DSS_DEBUG_MESSAGE, who + line);
        sendBroadcast(intent);

    }
}
