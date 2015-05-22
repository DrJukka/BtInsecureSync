package org.thaliproject.p2p.btpollingsynclib;

import android.bluetooth.BluetoothSocket;

/**
 * Created by juksilve on 20.5.2015.
 */
public interface  BtPollingCallback{
    void Connected(BluetoothSocket socket, String syncData);
    void CreateSocketFailed(String reason);
    void ConnectionFailed(String reason);
}