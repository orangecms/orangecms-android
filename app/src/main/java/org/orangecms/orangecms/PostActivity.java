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
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by dan on 4/7/15.
 */
public class PostActivity extends Activity {
    /**
     * Keep track of the posting task to ensure we can cancel it if requested.
     */
    private PostingTask mPostTask = null;

    private String mEndpointUrl;

    // UI references.
    private EditText mTitleView;
    private EditText mTextView;
    private View mProgressView;
    private View mPostFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        Intent intent = getIntent();
        mEndpointUrl = intent.getStringExtra("endpointUrl");

        // Set up the login form.
        mTitleView = (EditText) findViewById(R.id.title);
        mTextView  = (EditText) findViewById(R.id.text);
        mTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.title || id == EditorInfo.IME_NULL) {
                    attemptPost();
                    return true;
                }
                return false;
            }
        });

        Button mCameraButton = (Button) findViewById(R.id.camera_button);
        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();
            }
        });

        Button mPostButton = (Button) findViewById(R.id.post_button);
        mPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptPost();
            }
        });

        mPostFormView = findViewById(R.id.post_form);
        mProgressView = findViewById(R.id.progress);
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

            mPostFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mPostFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mPostFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mPostFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    static final int REQUEST_IMAGE_CAPTURE = 1;

    /**
     *
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(getApplication(), CameraDemoActivity.class);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /**
     * Attempts to post the data specified by the login form.
     * If there are form errors, the errors are presented.
     */
    public void attemptPost() {
        if (mPostTask != null) {
            return;
        }

        // Reset errors.
        mTitleView.setError(null);
        mTextView.setError(null);

        // Store values at the time of the login attempt.
        String title = mTitleView.getText().toString();
        String text  = mTextView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for empty fields
        if (TextUtils.isEmpty(title)) {
            mTitleView.setError(getString(R.string.error_field_required));
            focusView = mTitleView;
            cancel = true;
        }
        if (TextUtils.isEmpty(text)) {
            mTextView.setError(getString(R.string.error_field_required));
            focusView = mTextView;
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
            mPostTask = new PostingTask(title, text);
            mPostTask.execute((Void) null);
        }
    }

    /**
     * Represents an asynchronous login task used to authenticate the user.
     */
    public class PostingTask extends AsyncTask<Void, Void, String> {

        private final String mTitle;
        private final String mText;

        PostingTask(String username, String password) {
            mTitle = username;
            mText = password;
        }

        @Override
        protected String doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.
            String result = "";

            if (isNetworkAvailable() && mEndpointUrl != null) {
                JSONObject post = new JSONObject();
                try {
                    post.put("title", mTitle);
                    post.put("text", mText);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                String requestBody = post.toString();
                try {
                    URL url = new URL(mEndpointUrl + "/posts");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();

                    con.setRequestMethod("POST");
                    con.setRequestProperty(
                            "Content-Type",
                            "application/json"
                    );
//                    con.setRequestProperty(
//                            "Content-Length",
//                            Integer.toString(requestBody.getBytes().length)
//                    );
                    con.setRequestProperty("Content-Language", "en-US");

                    con.setUseCaches(false);
                    con.setDoInput(true);
                    con.setDoOutput(true);

                    Log.d(LoginActivity.LOG_TAG, requestBody);

                    //Send request
                    DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                    wr.writeBytes(requestBody);
                    wr.flush();
                    wr.close();

                    result = readStream(con.getInputStream());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(final String result) {
            mPostTask = null;
            showProgress(false);

            if (result != null) {
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
//                finish();
            } else {
                Toast.makeText(getApplicationContext(), "Error :(", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            mPostTask = null;
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
