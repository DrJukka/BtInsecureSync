// Copyright (c) Microsoft. All Rights Reserved. Licensed under the MIT License. See license.txt in the project root for further information.

package org.thaliproject.p2p.btinsecuresync;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.thaliproject.p2p.btpollingsynclib.BtPollingSyncService;
import org.thaliproject.p2p.btpollingsynclib.PeerDataItem;

import java.util.List;


public class MainActivity extends Activity {

    final MainActivity that = this;

    MainBCReceiver mBRReceiver;
    private IntentFilter filter;

    PowerManager.WakeLock mWakeLock = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBRReceiver = new MainBCReceiver();
        filter = new IntentFilter();
        filter.addAction(BtPollingSyncService.DSS_DEBUG_VALUES);

        this.registerReceiver((mBRReceiver), filter);

        Button clButton = (Button) findViewById(R.id.button2);
        clButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((TextView)findViewById(R.id.debugdataBox)).setText("cleared.\n");
            }
        });

        Button cuntButton = (Button) findViewById(R.id.cuntToggle);
        cuntButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getBaseContext(), PeersActivity.class);
                startActivity(intent);
            }
        });

        Button toggleButton = (Button) findViewById(R.id.buttonToggle);
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BtPollingSyncService  service = ((MyDataHandlerApp)getApplicationContext()).GetService();

                if(service != null){
                    if(service.isRunnuing()){
                        service.Stop();
                        stopService(new Intent(that, BtPollingSyncService.class));
                        ((TextView)findViewById(R.id.debugdataBox)).append("Service stopped\n");
                    }else {
                        service.Start();
                        Intent i = new Intent(that, BtPollingSyncService.class);
                        startService(i);
                        ((TextView) findViewById(R.id.debugdataBox)).append("Service running\n");
                    }
                }else {
                    ((TextView) findViewById(R.id.debugdataBox)).append("myService is null\n");
                }
            }
        });

    /*    PeerManagerDb db = ((MyDataHandlerApp)getApplicationContext()).GetDb();
        if(db != null) {
            List<PeerDataItem> pers = db.GetPeerList();
            if (pers != null) {
                Toast toast = Toast.makeText(this, "we have " + pers.size() + " items in database", Toast.LENGTH_LONG);
                toast.show();
            }
        }*/

        //for demo & testing to keep lights on
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(mBRReceiver);
    }

    private class MainBCReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BtPollingSyncService.DSS_DEBUG_VALUES.equals(action)) {
                String s = intent.getStringExtra(BtPollingSyncService.DSS_DEBUG_MESSAGE);
                String addText = s + "\n" + ((TextView) findViewById(R.id.debugdataBox)).getText().toString();
                ((TextView) findViewById(R.id.debugdataBox)).setText(addText);
            }
        }
    }
}
