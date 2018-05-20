package gov.jake.geojacob.geotracker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A login screen that offers login via ID/password.
 */
public class LoginActivity extends AppCompatActivity {

    // UI references.
    private EditText mIDView;
    private EditText mPasswordView;
    private EditText mIPView;
    private View mProgressView;
    private View mLoginFormView;
    private NetworkGPSService mService;
    private IBinder mBinder;
    private boolean mShouldUnbind;
    private String ipAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Set up the login form.
        mPasswordView = (EditText) findViewById(R.id.password);
        mIDView = (EditText) findViewById(R.id.email);
        mIPView = (EditText) findViewById(R.id.ip_address);

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    Toast.makeText(getBaseContext(), "Logging in", Toast.LENGTH_SHORT).show();
                    attemptLogin();
                    return true;
                }
                Toast.makeText(getBaseContext(), "Returning false", Toast.LENGTH_SHORT).show();
                return false;
            }
        });

        Button mIDSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mIDSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.email_login_form);
        mProgressView = findViewById(R.id.login_progress);

        doBindService(ipAddress);
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid ID, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mIDView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String ID = mIDView.getText().toString();
        String password = mPasswordView.getText().toString();
        ipAddress = mIPView.getText().toString();
        Toast.makeText(getBaseContext(), "The IP Address: " + ipAddress, Toast.LENGTH_SHORT).show();
        doBindService(ipAddress);

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (password.isEmpty() && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid ID.
        if (ID.isEmpty()) {
            mIDView.setError(getString(R.string.error_field_required));
            focusView = mIDView;
            cancel = true;
        } else if (!isIDValid(ID)) {
            mIDView.setError(getString(R.string.error_invalid_email));
            focusView = mIDView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);

            while (mService == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (ID.contains("@")) {
                ID = ID.split("@")[0];
            }

            mService.connect(ipAddress);
            mService.sendMessage("login", ID + " " + password);
            String welcome = mService.receiveMessage();

            if (welcome.split(" ").length != 2 || !welcome.split(" ")[1].equals("true")) {
                mIDView.setError(getString(R.string.general_badness));
                focusView = mIDView;
                focusView.requestFocus();
                showProgress(false);
                return;
            }

            Intent mainActivity = new Intent(getApplicationContext(), MainActivity.class);
            mainActivity.putExtra("ID", ID);
            mainActivity.putExtra("IP", ipAddress);
            startActivity(mainActivity);
            doUnbindService();
            finish();
        }
    }

    private boolean isIDValid(String id) {
        return id.length() > 3;
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 4 && !password.contains(" ");
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private boolean mBound;
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
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mBound = false;
        }
    };

    void doBindService(String ipAddress) {
        // Attempts to establish a connection with the service.  We use an
        // explicit class name because we want a specific service
        // implementation that we know will be running in our own process
        // (and thus won't be supporting component replacement by other
        // applications).

        Intent intent = new Intent(this, NetworkGPSService.class);
        intent.putExtra("IP", ipAddress);
        Toast.makeText(getBaseContext(), "The IP Address in doBind: " + ipAddress, Toast.LENGTH_SHORT).show();
        if (bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            mShouldUnbind = true;
        }
    }

    void doUnbindService() {
        if (mShouldUnbind) {
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
}

