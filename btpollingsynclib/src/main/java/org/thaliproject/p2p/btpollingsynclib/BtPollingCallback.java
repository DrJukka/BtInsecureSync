// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.

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