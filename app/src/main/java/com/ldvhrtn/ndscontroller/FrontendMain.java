package com.ldvhrtn.ndscontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

public class FrontendMain extends Activity {

    static final String con_ok_string = ", connection compatible";
    static final String con_not_ok_string = ", NDS may <font color='#EE0000'>not</font> be able to connect";
    CheckForNetworkUpdates netcheck_task;
    boolean connectible = false;
    boolean wifi_connection = false;
    boolean currently_tethering = false;

    class CheckForNetworkUpdates extends AsyncTask<Void, String, Void> {
        protected Void doInBackground(Void... v) {
            while(!isCancelled()) {
                try {
                    ConnectionStateManager tetherMan = new ConnectionStateManager(getBaseContext());
                    if (tetherMan.get_tether_state()) {
                        currently_tethering = true;
                        if (tetherMan.connection_nds_compatible()) {
                            connectible = true;
                            publishProgress("192.168.43.1"+ con_ok_string);
                        } else {
                            publishProgress("192.168.43.1"+ con_not_ok_string);
                        }
                    } else if (tetherMan.wifi_is_connected()) {
                        wifi_connection = true;
                        if (tetherMan.connection_nds_compatible()) {
                            connectible = true;
                            publishProgress(tetherMan.wifi_ip()+ con_ok_string);
                        } else {
                            publishProgress(tetherMan.wifi_ip()+ con_not_ok_string);
                        }
                    }
                } catch (Throwable e){
                    Log.e("frontmain", "throwable in tetherMan creation", e);
                }
                SystemClock.sleep(1000);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... msgs) {
            super.onProgressUpdate(msgs);
            TextView m_textview = (TextView) findViewById(R.id.netinfo_tv);
            m_textview.setText(Html.fromHtml(msgs[0]));
        }
    }

    // Prompt the user to allow the keyboard to be selected
    public void input_enabler(View v) {
        new AlertDialog.Builder(v.getContext())
                .setTitle("Opening Language & Input Dialog")
                .setMessage("Android will give a warning that enabling an additional input method" +
                        " could be used to steal your data. This app doesn't do that - it's open " +
                        "source, so you can check for yourself if you want!")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK), 2000);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    // Prompt the user to select the keyboard
    public void input_selector(View v) {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showInputMethodPicker();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frontend_main);
        Button inputenable_button = (Button) findViewById(R.id.inputenablebutton);
    }

    @Override
    protected void onResume() {
        super.onResume();
        netcheck_task = new CheckForNetworkUpdates();
        netcheck_task.execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        netcheck_task.cancel(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_frontend_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
