// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.

package org.thaliproject.p2p.btpollingsynclib;

import android.bluetooth.BluetoothSocket;

/**
 * Created by juksilve on 20.5.2015.
 */
public interface  BtListenCallback {
    void GotConnection(BluetoothSocket socket);
    void CreateSocketFailed(String reason);
    void ListeningFailed(String reason);
}
