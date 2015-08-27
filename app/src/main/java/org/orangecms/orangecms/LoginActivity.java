package org.orangecms.orangecms;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;


/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity {

    // Constants
    public static final String LOG_TAG = "ORN";
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private LoginTask mAuthTask = null;

    // UI references.
    private EditText mEndpointUrlView;
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Cookie handling
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);

        // Set up the login form.
        mEndpointUrlView = (EditText) findViewById(R.id.endpoint_url);
        mUsernameView    = (EditText) findViewById(R.id.username);
        mPasswordView    = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.integer.loginImeActionId || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mLoginButton = (Button) findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid username, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEndpointUrlView.setError(null);
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String endpointUrl = mEndpointUrlView.getText().toString();
        String username    = mUsernameView.getText().toString();
        String password    = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;


        // Check for a valid endpoint.
        if (TextUtils.isEmpty(endpointUrl) || !isEndpointValid(endpointUrl)) {
            mEndpointUrlView.setError(getString(R.string.error_invalid_endpoint));
            focusView = mEndpointUrlView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        } else if (!isUsernameValid(username)) {
            mUsernameView.setError(getString(R.string.error_invalid_username));
            focusView = mUsernameView;
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
            mAuthTask = new LoginTask(endpointUrl, username, password);
            mAuthTask.execute((Void) null);
        }
    }

    private boolean isEndpointValid(String endpointUrl) {
        // only takes URLs which will not throw a MalformulatedUrlException
        // TODO is this enough for a check?
        return URLUtil.isHttpUrl(endpointUrl) && Patterns.WEB_URL.matcher(endpointUrl).matches();
    }

    private boolean isUsernameValid(String username) {
        //TODO: Replace this with your own logic
        return username.length() > 0;
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
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
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login task used to authenticate the user.
     */
    public class LoginTask extends AsyncTask<Void, Void, String> {

        private final String mEndpointUrl;
        private final String mUsername;
        private final String mPassword;

        private int mResponseCode;
        private Exception mException;

        LoginTask(String endpointUrl, String username, String password) {
            mEndpointUrl = endpointUrl;
            mUsername    = username;
            mPassword    = password;
        }

        @Override
        protected String doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            String result = "";
            // could also set this to null and adapt onPostExecute
            if (isNetworkAvailable()) {
                JSONObject requestJSON = new JSONObject();
                try {
                    requestJSON.put("username", mUsername);
                    requestJSON.put("password", mPassword);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String requestBody = requestJSON.toString();

                Log.d(LOG_TAG, mEndpointUrl);
                Log.d(LOG_TAG, requestBody);
                try {
                    URL url = new URL(mEndpointUrl + "/login");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();

                    con.setRequestMethod("POST");
                    con.setRequestProperty("Content-Type", "application/json");
                    con.setRequestProperty(
                            "Content-Length",
                            Integer.toString(requestBody.getBytes().length)
                    );

                    con.setUseCaches(false);
                    con.setDoInput(true);
                    con.setDoOutput(true);

                    //Send request
                    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                    wr.writeBytes(requestBody);
                    wr.flush();
                    wr.close();

                    Log.d(LOG_TAG, "HTTP status: " + con.getResponseCode());
                    mResponseCode = con.getResponseCode();
                    result = readStream(con.getInputStream());
                } catch (Exception e) {
                    mException = e;
                    e.printStackTrace();
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(final String result) {
            mAuthTask = null;
            showProgress(false);

            if (!TextUtils.isEmpty(result)) {
                // result is set to "" in doInBackground and therefore will never be null
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();

                Intent intent = new Intent(getApplication(), PostActivity.class);
                intent.putExtra("endpointUrl", mEndpointUrl);
                startActivity(intent);
            } else {
                // try to find out the error type by server response code
                if (mResponseCode == 401){
                    mPasswordView.setError(getString(R.string.error_wrong_user_or_password));
                    mPasswordView.requestFocus();
                } else if (mResponseCode == 404 || mException instanceof UnknownHostException) {
                    mEndpointUrlView.setError(getString(R.string.error_unknown_endpoint));
                    mEndpointUrlView.requestFocus();
                } else { // other error code or exception
                    mEndpointUrlView.setError(getString(R.string.error_connection));
                    mEndpointUrlView.requestFocus();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }

        private String readStream(InputStream in) {
            BufferedReader reader = null;
            String result = "";
            try {
                reader = new BufferedReader(new InputStreamReader(in));
                String line;
                while ((line = reader.readLine()) != null) {
                    result += line;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return result;
        }

        public boolean isNetworkAvailable() {
            ConnectivityManager cm = (ConnectivityManager)
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            // if no network is available networkInfo will be null
            // otherwise check if we are connected
            return networkInfo != null && networkInfo.isConnected();
        }
    }
}
