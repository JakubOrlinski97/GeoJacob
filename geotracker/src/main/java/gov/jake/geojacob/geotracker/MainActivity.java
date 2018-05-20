package gov.jake.geojacob.geotracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity {

    public static final int startUpdates = 0;

    private FusedLocationProviderClient mFusedLocationClient;
    Toast toast;
    Button button;
    Button endbutton;
    TextView textView;

    String userID;
    String ipAddress;
    private boolean mShouldUnbind;
    private NetworkChangeReceiver receiver;
    private boolean stopped = false;
    private boolean iBound = false;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Intent i = getIntent();
        userID = i.getStringExtra("ID");
        ipAddress = i.getStringExtra("IP");
        toast = Toast.makeText(getBaseContext(), "Welcome " + userID + " And we have IP: " + ipAddress, Toast.LENGTH_SHORT);
        toast.show();

        button = findViewById(R.id.button3);
        endbutton = findViewById(R.id.button2);
        textView = findViewById(R.id.textView2);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 10);
        } else {
            configure();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 10:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    configure();
                }
        }
    }

    @SuppressLint("MissingPermission")
    public void configure() {
        doBindService();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        button.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                button.setActivated(false);
                stopped = false;
                mService.connect(ipAddress);

                endbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mService != null) {
                            mService.sendMessage("end", "");
                            mService.disconnect();

                            doUnbindService();
                        }
                        button.setActivated(true);
                        mFusedLocationClient.removeLocationUpdates(mCallBack);
                        stopped = true;
                    }
                });
                if (!stopped) {
                    mFusedLocationClient.requestLocationUpdates(createLocationRequest(), mCallBack, null);
                }
            }
        });

        if (!stopped) {
            mFusedLocationClient.requestLocationUpdates(createLocationRequest(), mCallBack, null);
        }
    }

    final LocationCallback mCallBack = new LocationCallback(){
        @SuppressLint("MissingPermission")
        @Override
        public void onLocationResult(LocationResult locationResult) {
            String lat = "" + locationResult.getLocations().get(0).getLatitude();
            String lon = "" + locationResult.getLocations().get(0).getLongitude();

            textView.setText("Sent location!: " + lat + " " + lon + " " + System.currentTimeMillis());

            if (iBound) {
                mService.sendMessage("location", userID + " " + lat + " " + lon + " " + System.currentTimeMillis());
            } else {
                doBindService();
                mFusedLocationClient.requestLocationUpdates(createLocationRequest(), mCallBack, null);
            }
        }
    };

    private LocationRequest createLocationRequest() {
        LocationRequest mLocReq = new LocationRequest();
        mLocReq.setFastestInterval(250);
        mLocReq.setInterval(500);
        mLocReq.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocReq;
    }


    private NetworkGPSService mService;
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service.  We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            NetworkGPSService.LocalBinder binder = (NetworkGPSService.LocalBinder) service;
            mService = binder.getService();
            mService.connect(ipAddress);
            iBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            Toast.makeText(getBaseContext(), "Lost connection with the server!", Toast.LENGTH_SHORT).show();
            button.setActivated(true);
            mService = null;
            iBound = false;
        }
    };

    void doBindService() {
        // Attempts to establish a connection with the service.  We use an
        // explicit class name because we want a specific service
        // implementation that we know will be running in our own process
        // (and thus won't be supporting component replacement by other
        // applications).
        Intent intent = new Intent(this, NetworkGPSService.class);
        intent.putExtra("IP", ipAddress);
        if (bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            mShouldUnbind = true;

            receiver = new NetworkChangeReceiver();
            IntentFilter intentFilter = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
            intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
            intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
            registerReceiver(receiver, intentFilter);
        }
    }

    void doUnbindService() {
        if (mShouldUnbind) {
            unregisterReceiver(receiver);

            // Release information about the service's state.
            unbindService(mConnection);
            mShouldUnbind = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    public class NetworkChangeReceiver extends BroadcastReceiver {
        NetworkGPSService service;
        NetworkGPSService.LocalBinder iBinder;

        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(final Context context, final Intent intent) {
            iBinder = (NetworkGPSService.LocalBinder) peekService(context, new Intent(context, NetworkGPSService.class));
            if (iBinder == null)
                return;

            service = iBinder.getService();

            int status = NetworkUtil.getConnectivityStatusString(context);

            if (!"android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
                Toast.makeText(getBaseContext(), "Network status changed", Toast.LENGTH_SHORT).show();
                if (status==NetworkUtil.NETWORK_STATUS_NOT_CONNECTED) {
                    if (service != null) {
                        service.disconnect();
                    }
                }
                if (status == NetworkUtil.NETWORK_STATUS_MOBILE || status == NetworkUtil.NETWORK_STAUS_WIFI) {
                    if (service != null) {
                        service.connect(ipAddress);
                    }
                }
            }
        }
    }
}
