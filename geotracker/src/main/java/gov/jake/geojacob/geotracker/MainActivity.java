package gov.jake.geojacob.geotracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient mFusedLocationClient;
    Toast toast;
    Button button;
    Button endbutton;
    TextView textView;

    String userID;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        Intent i = getIntent();
        userID = i.getStringExtra("ID");

        toast = Toast.makeText(getBaseContext(), "Welcome " + userID, Toast.LENGTH_SHORT);
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

    public void configure() {
        Intent intent = new Intent(this, NetworkGPSService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        final LocationCallback mCallBack = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                String lat = "" + locationResult.getLocations().get(0).getLatitude();
                String lon = "" + locationResult.getLocations().get(0).getLongitude();

                if (mService != null) {
                    mService.sendMessage("location", userID + " " + lat + " " + lon);
                }
            }
        };

        button.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onClick(View v) {
                button.setActivated(false);

                endbutton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mService != null) {
                            mService.sendMessage("end", "");
                            mService.unbindService(mConnection);
                        }
                        button.setActivated(true);
                    }
                });

                mFusedLocationClient.requestLocationUpdates(createLocationRequest(), mCallBack, null);
            }
        });
    }

    private LocationRequest createLocationRequest() {
        LocationRequest mLocReq = new LocationRequest();
        mLocReq.setFastestInterval(10);
        mLocReq.setInterval(100);
        mLocReq.setSmallestDisplacement(1);
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
        }

        public void onServiceDisconnected(ComponentName className) {
            Toast.makeText(getBaseContext(), "Lost connection with the server!", Toast.LENGTH_SHORT).show();
            button.setActivated(true);
            mService = null;
        }
    };

}
