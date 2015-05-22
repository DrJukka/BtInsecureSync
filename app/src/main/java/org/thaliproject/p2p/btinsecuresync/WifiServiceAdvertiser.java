// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btinsecuresync;

import android.bluetooth.BluetoothAdapter;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by juksilve on 28.2.2015.
 */
public class WifiServiceAdvertiser {

    private WifiP2pManager p2p;
    private WifiP2pManager.Channel channel;
    private final WifiBase.WifiStatusCallBack callback;

    public WifiServiceAdvertiser(WifiP2pManager Manager, WifiP2pManager.Channel Channel, WifiBase.WifiStatusCallBack handler) {
        this.p2p = Manager;
        this.channel = Channel;
        this.callback = handler;
    }

    public void Start() {

        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if(bluetooth != null) {
            debug_print("My BT: " + bluetooth.getAddress() + " : " + bluetooth.getName() + " , state: " + bluetooth.getState());
            String instance = bluetooth.getAddress() + " ; " + bluetooth.getName();

            Map<String, String> record = new HashMap<String, String>();
            record.put("available", "visible");

            WifiP2pDnsSdServiceInfo service = WifiP2pDnsSdServiceInfo.newInstance(instance, WifiBase.SERVICE_TYPE, record);

            debug_print("Add local service :" + instance);
            p2p.addLocalService(channel, service, new WifiP2pManager.ActionListener() {
                public void onSuccess() {
                    debug_print("Added local service");
                }

                public void onFailure(int reason) {
                    callback.LocalServiceStartError("Adding local service failed, error code " + reason);
                }
            });
        }else{
            callback.LocalServiceStartError("This device does not support Bluetooth");
        }
    }

    public void Stop() {
        p2p.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                debug_print("Cleared local services");
            }

            public void onFailure(int reason) {
                debug_print("Clearing local services failed, error code " + reason);
            }
        });
    }

    private void debug_print(String buffer) {
        Log.i("ACCESS point", buffer);
    }
}
