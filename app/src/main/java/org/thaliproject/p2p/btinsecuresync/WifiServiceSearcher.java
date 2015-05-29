// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.

package org.thaliproject.p2p.btinsecuresync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION;


/**
 * Created by juksilve on 28.2.2015.
 */


public class WifiServiceSearcher {

    private Context context;
    private BroadcastReceiver receiver;
    private IntentFilter filter;


    private final WifiBase.WifiStatusCallBack callback;
    private WifiP2pManager p2p;
    private WifiP2pManager.Channel channel;
    private WifiP2pManager.DnsSdServiceResponseListener serviceListener;
    private WifiP2pManager.PeerListListener peerListListener;

    enum ServiceState{
        NONE,
        DiscoverPeer,
        DiscoverService
    }
    ServiceState myServiceState = ServiceState.NONE;
    List<ServiceItem> myServiceList = new ArrayList<ServiceItem>();


    public WifiServiceSearcher(Context Context, WifiP2pManager Manager, WifiP2pManager.Channel Channel, WifiBase.WifiStatusCallBack handler) {
        this.context = Context;
        this.p2p = Manager;
        this.channel = Channel;
        this.callback = handler;
    }

    public List<ServiceItem> GetServiceList(){
        return  myServiceList;
    }

    public void Start() {

        receiver = new ServiceSearcherReceiver();
        filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION);
        filter.addAction(WIFI_P2P_PEERS_CHANGED_ACTION);

        this.context.registerReceiver(receiver, filter);

        peerListListener = new WifiP2pManager.PeerListListener() {

            public void onPeersAvailable(WifiP2pDeviceList peers) {

                final WifiP2pDeviceList pers = peers;
                if (pers.getDeviceList().size() > 0) {
                    // this is called still multiple time time-to-time
                    // so need to make sure we only make one service discovery call
                    if(myServiceState != ServiceState.DiscoverService){
                        startServiceDiscovery();
                    }
                }
            }
        };

        serviceListener = new WifiP2pManager.DnsSdServiceResponseListener() {

            public void onDnsSdServiceAvailable(String instanceName, String serviceType, WifiP2pDevice device) {

                debug_print("Found Service, :" + instanceName  + ", length : " + instanceName.length() + ", type" + serviceType + ":");

                if (serviceType.startsWith(WifiBase.SERVICE_TYPE)) {
                    boolean addService = true;
                    for (int i=0; i<myServiceList.size(); i++) {
                        if(myServiceList.get(i).deviceAddress.equals(device.deviceAddress)){
                            addService = false;
                        }
                    }
                    if(addService) {
                        debug_print("instance: " + instanceName);
                        String[] separated = instanceName.split(";");
                        if(separated.length > 1) {
                            debug_print("found address:" + separated[0] + ", name:" + separated[1]);

                            ServiceItem tmp = new ServiceItem(separated[1],separated[0], serviceType, device.deviceAddress, device.deviceName);

                            myServiceList.add(tmp);

                            if (callback != null) {
                                callback.gotService(tmp);
                            }
                        }
                    }

                } else {
                    debug_print("Not our Service, :" + WifiBase.SERVICE_TYPE + "!=" + serviceType + ":");
                }
            }
        };

        p2p.setDnsSdResponseListeners(channel, serviceListener, null);
    }

    public void Stop() {
        this.context.unregisterReceiver(receiver);
        stopDiscovery();
        stopPeerDiscovery();
    }

    public void startPeerDiscovery() {

        p2p.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                myServiceState = ServiceState.DiscoverPeer;
                debug_print("Started peer discovery");
            }
            public void onFailure(int reason) {
                myServiceState = ServiceState.NONE;
                if(callback != null) {
                    callback.PeerStartError("Starting peer discovery failed, error code " + reason);
                }
            }
        });
    }

    private void stopPeerDiscovery() {
        p2p.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {
                debug_print("Stopped peer discovery");
            }

            public void onFailure(int reason) {
                debug_print("Stopping peer discovery failed, error code " + reason);
            }
        });
    }

    private void startServiceDiscovery() {

        myServiceState = ServiceState.DiscoverService;

        WifiP2pDnsSdServiceRequest request = WifiP2pDnsSdServiceRequest.newInstance(WifiBase.SERVICE_TYPE);
        final Handler handler = new Handler();
        p2p.addServiceRequest(channel, request, new WifiP2pManager.ActionListener() {

            public void onSuccess() {
                debug_print("Added service request");
                handler.postDelayed(new Runnable() {
                    //There are supposedly a possible race-condition bug with the service discovery
                    // thus to avoid it, we are delaying the service discovery start here
                    public void run() {
                        p2p.discoverServices(channel, new WifiP2pManager.ActionListener() {
                            public void onSuccess() {
                                myServiceList.clear();
                                debug_print("Started service discovery");
                                myServiceState = ServiceState.DiscoverService;
                            }
                            public void onFailure(int reason) {
                                stopDiscovery();
                                myServiceState = ServiceState.NONE;
                                if(callback != null) {
                                    callback.ServiceStartError("Starting service discovery failed, error code " + reason);
                                }
                            }
                        });
                    }
                }, 1000);
            }

            public void onFailure(int reason) {
                myServiceState = ServiceState.NONE;
                if(callback != null) {
                    callback.AddReqStartError("Adding service request failed, error code " + reason);
                }
            }
        });

    }

    private void stopDiscovery() {
        p2p.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            public void onSuccess() {debug_print("Cleared service requests");}
            public void onFailure(int reason) {debug_print("Clearing service requests failed, error code " + reason);}
        });
    }
    private void debug_print(String buffer) {
        Log.i("Service searcher", buffer);
    }
    private class ServiceSearcherReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                if(myServiceState != ServiceState.DiscoverService) {
                    //&& myServiceState != ServiceState.WaitingTimer) {
                    p2p.requestPeers(channel, peerListListener);
                }
            } else if(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED);
                String persTatu = "Discovery state changed to ";

                if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED) {
                    persTatu = persTatu + "Stopped.";
                    startPeerDiscovery();
                } else if (state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED) {
                    persTatu = persTatu + "Started.";
                } else {
                    persTatu = persTatu + "unknown  " + state;
                }
                debug_print(persTatu);

            }
        }
    }
}
