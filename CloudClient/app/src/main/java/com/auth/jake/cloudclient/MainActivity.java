package com.auth.jake.cloudclient;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.microsoft.windowsazure.mobileservices.*;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import java.net.MalformedURLException;
import java.util.List;

public class MainActivity extends Activity {

    // Create an object to connect to your mobile app service
    private MobileServiceClient mClient;

    // Create an object for  a table on your mobile app service
    private MobileServiceTable<ToDoItem> mToDoTable;

    // global variable to update a TextView control text
    TextView display;

    // simple stringbulder to store textual data retrieved from mobile app service table
    StringBuilder sb = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

       try {

           // using the MobileServiceClient global object, create a reference to YOUR service
           mClient = new MobileServiceClient(
                   "https://mobcompjake.azurewebsites.net",
                   this
           );

           // using the MobileServiceTable object created earlier, create a reference to YOUR table
           mToDoTable = mClient.getTable(ToDoItem.class);


           display = (TextView) findViewById(R.id.displayData);


       } catch (MalformedURLException e) {
            e.printStackTrace();
       }
    }

    // method to add data to mobile service table
    public void addData(View view) {


        // create reference to TextView input widgets
        TextView data1 = (TextView) findViewById(R.id.insertText1);
        // the below textview widget isn't used (yet!)
        TextView data2 = (TextView) findViewById(R.id.insertText2);

        // Create a new data item from the text input
        final ToDoItem item = new ToDoItem();
        item.text = data1.getText().toString();

        // This is an async task to call the mobile service and insert the data
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    //
                    final ToDoItem entity = mToDoTable.insert(item).get();  //addItemInTable(item);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                        // code inserted here can update UI elements, if required

                        }
                    });
                } catch (Exception exception) {

                }
                return null;
            }
        }.execute();
    }

    // method to view data from mobile service table
    public void viewData(View view) {

        display.setText("Loading...");

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    final List<ToDoItem> result = mToDoTable.select("id", "text").execute().get();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // get all data from column 'text' only add add to the stringbuilder
                            for (ToDoItem item : result) {
                                sb.append(item.text + " ");
                            }

                            // display stringbuilder text using scrolling method
                            display.setText(sb.toString());
                            display.setMovementMethod(new ScrollingMovementMethod());
                            sb.setLength(0);
                        }
                    });
                } catch (Exception exception) {
                }
                return null;
            }
        }.execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // class used to work with ToDoItem table in mobile service, this needs to be edited if you wish to use with another table
    public class ToDoItem {
        private String id;
        private String text;
    }




}
