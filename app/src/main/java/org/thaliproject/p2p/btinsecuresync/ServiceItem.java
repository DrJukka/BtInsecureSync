// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.
package org.thaliproject.p2p.btinsecuresync;

/**
 * Created by juksilve on 12.3.2015.
 */
public class ServiceItem {

    public ServiceItem(String instanceName,String Address,String type, String address, String name){
        this.instanceName = instanceName;
        this.instanceAddress = Address;
        this.serviceType = type;
        this.deviceAddress = address;
        this.deviceName =  name;
    }
    public String instanceName;
    public String instanceAddress;
    public String serviceType;
    public String deviceAddress;
    public String deviceName;
}
