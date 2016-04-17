package com.ldvhrtn.ndscontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.Html;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.util.Log;

public class FrontendMain extends Activity {

    static final String con_ok_string = ", connection <font color='#007700'>compatible</font>.";
    static final String con_not_ok_string = ", NDS may <font color='#EE0000'>not</font> be able to connect.";
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
                            publishProgress("192.168.43.1" + con_ok_string);
                        } else {
                            publishProgress("192.168.43.1" + con_not_ok_string);
                        }
                    } else if (tetherMan.wifi_is_connected()) {
                        wifi_connection = true;
                        if (tetherMan.connection_nds_compatible()) {
                            connectible = true;
                            publishProgress(tetherMan.wifi_ip() + con_ok_string);
                        } else {
                            publishProgress(tetherMan.wifi_ip() + con_not_ok_string);
                        }
                    } else {
                        publishProgress("0.0.0.0" + ", <font color='#EE0000'>no connection.</font>");
                    }
                    Thread.sleep(3000);
                } catch (InterruptedException ei) {
                    Log.d("frontmain", "networkupdates check interrupted");
                } catch (Throwable e){
                    Log.e("frontmain", "throwable in tetherMan creation", e);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... msgs) {
            super.onProgressUpdate(msgs);
            TextView m_textview = (TextView) findViewById(R.id.netinfo_tv);
            m_textview.setText(Html.fromHtml(msgs[0]));
            update_button_state(); //Update the buttons every second as well
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

    public void update_button_state() {
        Button inputenable_button = (Button) findViewById(R.id.inputenablebutton);
        Button inputselect_button = (Button) findViewById(R.id.inputselectbutton);
        Context ctx = inputenable_button.getContext();

        String id = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        ComponentName default_input_method = ComponentName.unflattenFromString(id);
        ComponentName m_service_name = new ComponentName(ctx, NDSControllerService.class);

        if(m_service_name.equals(default_input_method)) {
            String selected = "NDS Input Method is <font color='#00ee00'>Selected</font>";
            inputselect_button.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.checkbox_on_background, 0);
            inputenable_button.setEnabled(false);
            inputselect_button.setText(Html.fromHtml(selected));
        } else {
            String selected = "NDS Input Method is <font color='#ee0000'>Not Selected</font>";
            inputselect_button.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.checkbox_off_background, 0);
            inputenable_button.setEnabled(true);
            inputselect_button.setText(Html.fromHtml(selected));
        }
    }

    @Override
    public boolean onKeyDown( int keyCode, KeyEvent event ) {
        TextView show_input = (TextView) findViewById(R.id.inputcheck_tv);
        if (Build.VERSION.SDK_INT >= 12) {
            show_input.setText(KeyEvent.keyCodeToString(keyCode)+", "+event.toString());
        } else {
            show_input.setText("keycode: "+Integer.toString(keyCode)+", "+event.toString());
        }
        return super.onKeyDown( keyCode, event );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frontend_main);
        update_button_state();
    }

    @Override
    protected void onResume() {
        super.onResume();
        netcheck_task = new CheckForNetworkUpdates();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            netcheck_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            netcheck_task.execute();
        }
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
