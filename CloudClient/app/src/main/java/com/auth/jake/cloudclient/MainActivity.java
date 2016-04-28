package com.auth.jake.cloudclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import com.facebook.FacebookSdk;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceAuthenticationProvider;
import com.microsoft.windowsazure.mobileservices.authentication.MobileServiceUser;
import com.microsoft.windowsazure.mobileservices.*;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import org.json.JSONObject;
import java.net.MalformedURLException;

public class MainActivity extends Activity {



    public static final String SHAREDPREFFILE = "temp";
    public static final String USERIDPREF = "uid";
    public static final String TOKENPREF = "tkn";
    public static final byte[] key = "sjdnvjfndnfhdjqwerfdscvfghyujkui".getBytes();
    public static String accessToken;
    public static String authToken;
    public static Boolean hasAuthenticated = false;
    public static Boolean pressed = false;
    private MobileServiceClient mClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        // Initialize the SDK before executing any other operations,
        // especially, if you're using Facebook UI elements.
        setContentView(R.layout.activity_main);
        try {
            // using the MobileServiceClient global object, create a reference to YOUR service
            mClient = new MobileServiceClient(
                    "https://mobcompjake.azurewebsites.net/.auth/me",
                    this
            );

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (isConnectedToInternet()) {
            try {
                authenticate();
            }catch (Exception e) {
            }
        } else {
            //otherwise, display a dialog box that can direct the user to the settings page or ignore the warning
            final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(("Connection Failed"));
            alertDialog.setMessage("Please go to settings to check your connection status");
            alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Settings", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
                    startActivity(intent);
                }
            });
            alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Ignore", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    alertDialog.hide();
                }
            });
            alertDialog.show();
        }
    } //small changes

    public void onMoreInfo(View view) {
        if (!hasAuthenticated) {
            createAndShowDialog("You are not authenticated yet, Please Wait to recover the authentication token", "Error!");
        } else {
            if (pressed) {
                createAndShowDialog("This Information is being recovered, Please Wait", "Error!");
            } else {
                pressed = true;
                new getFBWithToken().execute();
            }
        }
    } //sound

    private void cacheUserToken(MobileServiceUser user) throws Exception{
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putString(USERIDPREF, user.getUserId());
        editor.putString(TOKENPREF, SecurityClass.encrypt(user.getAuthenticationToken(), key));
        editor.apply();
    } //sound

    private boolean loadUserTokenCache(MobileServiceClient client) throws Exception {
        SharedPreferences prefs = getSharedPreferences(SHAREDPREFFILE, Context.MODE_PRIVATE);
        String userId = prefs.getString(USERIDPREF, "undefined");
        if (userId == "undefined")
            return false;
        String token = prefs.getString(TOKENPREF, "undefined");
        if (token == "undefined")
            return false;
        MobileServiceUser user = new MobileServiceUser(userId);
        user.setAuthenticationToken(token);
        authToken = SecurityClass.decrypt(user.getAuthenticationToken(), key);
        client.setCurrentUser(user);
        return true;
    } //sound

    private void ObtainAuth() {
        new getAuthToken().execute();
    } //sound

    private void authenticate() throws Exception{
        // We first try to load a token cache if one exists.
        if (loadUserTokenCache(mClient)) {
            createAndShowDialog("Your Cached Authentication Token has been used to authorise you", "Success!");
            ObtainAuth();
        }
        // If we failed to load a token cache, login and create a token cache
        else {
            // Login using the FB provider.
            ListenableFuture<MobileServiceUser> mLogin = mClient.login(MobileServiceAuthenticationProvider.Facebook);
            Futures.addCallback(mLogin, new FutureCallback<MobileServiceUser>() {
                @Override
                public void onFailure(Throwable exc) {
                    createAndShowDialog("You Failed To Authenticate, Please Restart and Login", "Error");
                }

                @Override
                public void onSuccess(MobileServiceUser user){
                    try {
                        createAndShowDialog("Your Auth Token has been successfully cached", "Success!");
                        cacheUserToken(mClient.getCurrentUser());
                        authToken = user.getAuthenticationToken();
                        ObtainAuth();
                    }catch(Exception e) {

                    }
                }
            });
        }
    } //sound

    public void createAndShowDialog(String message, String title) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    } //reference maybe?

    public class getAuthToken extends AsyncTask<String, String, String> {
        private static final String API = "https://mobcompjake.azurewebsites.net/.auth/me";

        @Override
        protected void onPreExecute() {
        }

        @Override
        // this method is used for...................
        protected String doInBackground(String... arg0) {

            try {
                String accessResult;
                getAzure jParser = new getAzure();
                accessResult = jParser.getTokenFromAzure(MainActivity.this, API, authToken);
                accessToken = accessResult;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("ERROR", e.toString());
            }
            return null;
        }

        //List View is created and parsed JSON data form web service is appended to a new item of the list
        @Override
        protected void onPostExecute(String strFromDoInBg) {
            hasAuthenticated = true;
            TextView t = (TextView) findViewById(R.id.textView);
            String authmsg = "Status: Authenticated";
            t.setText(authmsg);
        }
    }

    public class getFBWithToken extends AsyncTask<String, String, String> {

        private final String FB_API = ("https://graph.facebook.com/me?fields=name,gender,picture,id&access_token=" + accessToken);
        JSONObject FBJSON = new JSONObject();
        String yourServiceUrl = (FB_API);

        @Override
        protected void onPreExecute() {
        }

        @Override
        // this method is used for...................
        protected String doInBackground(String... arg0) {
            try {
                getFacebook jParser = new getFacebook();
                FBJSON = jParser.getJSONFromUrl(MainActivity.this, yourServiceUrl, authToken);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("ERROR", e.toString());
            }
            return null;
        }

        //List View is created and parsed JSON data form web service is appended to a new item of the list
        @Override
        protected void onPostExecute(String strFromDoInBg) {
            try {
                String[] results = new String[3];
                results[0] = FBJSON.getString("name");
                results[1] = FBJSON.getString("gender");
                results[2] = FBJSON.getString("id");
                String id = FBJSON.getString("id");
                new DownloadImageTask((ImageView) findViewById(R.id.imageView)).execute("https://graph.facebook.com/"+id+"/picture?type=large");
                ArrayList<String> items = new ArrayList<String>(Arrays.asList(results));
                ListView list = (ListView) findViewById(R.id.dataView);
                ArrayAdapter<String> facebookAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_expandable_list_item_1, items);
                list.setAdapter(facebookAdapter);
                pressed = false;
            } catch (Exception e) {

            }
        }
    }

    public class getAzure {
        final String TAG = "JsonParser.java";
        String json = "";

        public String getTokenFromAzure(Context context, String url, String Auth_Token) {
            try {
                URL u = new URL(url);

                HttpURLConnection restConnection = (HttpURLConnection) u.openConnection();

                //request data from azure
                restConnection.setRequestMethod("GET");
                restConnection.setRequestProperty("X-ZUMO-AUTH", Auth_Token);
                restConnection.addRequestProperty("content-length", "0");
                restConnection.setUseCaches(false);
                restConnection.setAllowUserInteraction(false);
                restConnection.setConnectTimeout(10000);
                restConnection.setReadTimeout(10000);
                restConnection.connect();

                int status = restConnection.getResponseCode();

                switch (status) {
                    case 200:
                    case 201:
                        // live connection to  REST service is established here using getInputStream() method
                        BufferedReader br = new BufferedReader(new InputStreamReader(restConnection.getInputStream()));

                        // create a new string builder to store json data returned from the REST service
                        StringBuilder sb = new StringBuilder();
                        String line = "";

                        // loop through returned data line by line and append to stringbuffer 'sb' variable
                        while ((line = br.readLine()) != null) {
                            sb.append(line + "\n");
                        }
                        br.close();
                        //JSON returned as a JSONObject
                        try {
                            json = sb.toString();
                            json = json.substring(json.indexOf(":") + 1);
                            json = json.substring(0, json.indexOf(","));
                            json = json.replace("\"", "");
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing data " + e.toString());

                            return null;
                        }

                        return json;
                }
                // HTTP 200 and 201 error handling from switch statement
            } catch (MalformedURLException ex) {
                Log.e(TAG, "Malformed URL ");
            } catch (IOException ex) {
                Log.e(TAG, "IO Exception ");
            }
            return json = "";
        }
    } //slight changes

    public class getFacebook {
        final String TAG = "JsonParser.java";
        String json = "";
        JSONObject dataFb = new JSONObject();

        public JSONObject getJSONFromUrl(Context context, String url, String Auth_Token) {
            try {
                URL u = new URL(url);

                HttpURLConnection restConnection = (HttpURLConnection) u.openConnection();

                //request data from azure
                restConnection.setRequestMethod("GET");
                restConnection.setRequestProperty("X-ZUMO-AUTH", Auth_Token);
                restConnection.addRequestProperty("content-length", "0");
                restConnection.setUseCaches(false);
                restConnection.setAllowUserInteraction(false);
                restConnection.setConnectTimeout(10000);
                restConnection.setReadTimeout(10000);
                restConnection.connect();

                int status = restConnection.getResponseCode();

                switch (status) {
                    case 200:
                    case 201:
                        // live connection to  REST service is established here using getInputStream() method
                        BufferedReader br = new BufferedReader(new InputStreamReader(restConnection.getInputStream()));
                        // create a new string builder to store json data returned from the REST service
                        StringBuilder sb = new StringBuilder();
                        String line = "";
                        // loop through returned data line by line and append to stringbuffer 'sb' variable
                        while ((line = br.readLine()) != null) {
                            sb.append(line + "\n");
                        }
                        br.close();
                        //JSON returned as a JSONObject
                        try {
                            dataFb = new JSONObject(sb.toString());
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing data " + e.toString());

                            return null;
                        }

                        return dataFb;
                }
                // HTTP 200 and 201 error handling from switch statement
            } catch (MalformedURLException ex) {
                Log.e(TAG, "Malformed URL ");
            } catch (IOException ex) {
                Log.e(TAG, "IO Exception ");
            }
            return dataFb;
        }
    } //slight changes

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        //From http://web.archive.org/web/20120802025411/http://developer.aiwgame.com/imageview-show-image-from-url-on-android-4-0.html
        //How to download an Image through a url via asynctask (archived from Android Developer)
        ImageView bmImage;
        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    public boolean isConnectedToInternet() {
        ConnectivityManager internetConn = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (internetConn != null) {
            NetworkInfo[] info = internetConn.getAllNetworkInfo();
            if (info != null) //If there is a connection present
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true; //return the state of the connection
                    }
        }
        return false; //otherwise, return nothing
    } //needs changing
}