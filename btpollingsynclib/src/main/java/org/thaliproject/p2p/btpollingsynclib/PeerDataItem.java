// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.

package org.thaliproject.p2p.btpollingsynclib;

/**
 * Created by juksilve on 20.5.2015.
 */
public class PeerDataItem {

    public PeerDataItem(String DeviceName,String DeviceAddress,String DeviceData){
        this.Name = DeviceName;
        this.Address = DeviceAddress;
        this.Data = DeviceData;
    }
    public String Name;
    public String Address;
    public String Data;
}
