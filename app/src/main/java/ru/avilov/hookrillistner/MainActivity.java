package ru.avilov.hookrillistner;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private Listner listner = null;

    private final static String TAG = MainActivity.class.getName();

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder binder) {
            listner = ((Listner.LocalBinder)binder).getService();
            Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT)
                    .show();
        }

        public void onServiceDisconnected(ComponentName className) {
            listner = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startService(new Intent(this, Listner.class));
        try {
            installLib(false);
        } catch (IOException e) {
            Log.e(TAG, "Copying error", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        String libpath = System.getProperty("rild.libpath", null);
//        if(libpath != null && libpath.contains("libhookril")) {
            bindService(new Intent(this, Listner.class), mConnection, Context.BIND_AUTO_CREATE);
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(listner != null)
            unbindService(mConnection);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.my_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;

            case R.id.action_connect_button:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void installLib(boolean isMTK) throws IOException {
        String type = isMTK ? "mtk" : "clear";
        String libname = "libhookril.so";
        

        OutputStream myOutput = new FileOutputStream("/data/local/tmp/" + libname);
        byte[] buffer = new byte[1024];
        int length;
        InputStream myInput = getAssets().open(type + "/" + libname);
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }
        myInput.close();
        myOutput.flush();
        myOutput.close();
    }
}
