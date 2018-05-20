package gov.jake.geojacob.geotracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class NetworkGPSService extends Service {
    private static final String IP2 = "130.89.94.37";
    private static final String LOCALHOST = "127.0.0.1";
    private static final String IP = "130.89.93.234";
    private static final int PORT = 3000;


    private List<String> commands = Arrays.asList("welcome", "exit");

    SSLSocket sock;
    BufferedWriter out;
    BufferedReader in;
    String ipAddress;


    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    public void disconnect() {
        if (sock != null && in != null && out != null) {
            try {
                in.close();
                out.close();
                sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            sock = null;
        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public NetworkGPSService getService() {
            return NetworkGPSService.this;
        }
    }

    public void sendMessage(String command, String body) {
        try {
            if (out != null) {
            out.write(command + " " + body + "\n");
            out.flush();
            } else {
                Toast.makeText(getBaseContext(), "The service isn't connected!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            if (isNetworkConnected()) {
                connect(ipAddress);
            }
        }
    }

    private boolean isNetworkConnected(){
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public String receiveMessage() {
        String input = null;
        try {
            if (in != null) {
                input = in.readLine();

                while (input != null) {
                    String[] parts = input.split(" ");
                    if (commands.contains(parts[0])) {
                        return input;
                    }
                    input = in.readLine();
                }
            } else {
                Toast.makeText(getBaseContext(), "The service isn't connected!", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void connect(String ipAddress) {
        try {
            Toast.makeText(getBaseContext(), "The IP Address in connect: " + ipAddress, Toast.LENGTH_SHORT).show();
            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            sock = (SSLSocket) sslsocketfactory.createSocket(ipAddress, PORT);

            //sock = new Socket(IP, PORT);
            out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Got an IOException", Toast.LENGTH_SHORT).show();
            stopSelf();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        ipAddress = intent.getStringExtra("IP");
        Toast.makeText(getBaseContext(), "The IP Address in onBind: " + intent.getStringExtra("IP"), Toast.LENGTH_SHORT).show();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }
}
