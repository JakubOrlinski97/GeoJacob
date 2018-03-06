package gov.jake.geojacob.geotracker;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NetworkGPSService extends Service {
    private static final String IP = "130.89.95.67";
    private static final String LOCALHOST = "127.0.0.1";
    private static final String IP2 = "192.168.1.34";
    private static final int PORT = 3000;


    private List<String> commands = Arrays.asList("welcome", "exit");

    Socket sock;
    BufferedWriter out;
    BufferedReader in;


    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

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

        Toast.makeText(getBaseContext(), "Starting service", Toast.LENGTH_SHORT).show();

        try {
            out.write(command + " " + body + "\n");
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String receiveMessage() {
        String input = null;
        try {
            input = in.readLine();

            while (input != null) {
                String[] parts = input.split(" ");
                if (commands.contains(parts[0])) {
                    return input;
                }
                input = in.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public IBinder onBind(Intent intent) {
        try {
            sock = new Socket(LOCALHOST, PORT);
            out = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Couldn't connect!", Toast.LENGTH_LONG).show();
            stopSelf();
        }

        return mBinder;
    }


    @Override
    public void onDestroy(){
        Toast.makeText(getBaseContext(), "Stopping service", Toast.LENGTH_SHORT).show();
    }


}
