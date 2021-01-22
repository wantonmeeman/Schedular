package com.example.ca1;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.settings);
        SharedPreferences pref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        Switch switchTheme = (Switch)findViewById(R.id.simpleSwitch);
        Log.i("1",Boolean.toString(pref.getBoolean("UIMode",false)));
        switchTheme.setChecked(pref.getBoolean("UIMode",false));

        switchTheme.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = pref.edit();
                int mode = 0;
                Log.i("isChecked",Boolean.toString(isChecked));
                if (isChecked) {
                    editor.putBoolean("UIMode",true);
                    Log.i("Mode","Dark");
                    mode = AppCompatDelegate.MODE_NIGHT_YES;
                } else {
                    editor.putBoolean("UIMode",false);
                    Log.i("Mode","Light");
                    mode = AppCompatDelegate.MODE_NIGHT_NO;
                }
                editor.commit();
                getDelegate().setLocalNightMode(mode);
                getDelegate().applyDayNight();
                recreate();
                AppCompatDelegate.setDefaultNightMode(mode);
            }

        });

        MaterialButton button = findViewById(R.id.logoutBtn);
        button.setPaintFlags(button.getPaintFlags() |   Paint.UNDERLINE_TEXT_FLAG);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        //.requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
                GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(getApplication(), gso);
                mGoogleSignInClient.signOut();
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("firebaseUserId",null);
                editor.commit();
                Intent intent = new Intent(getApplication(),LoginActivity.class);
                startActivity(intent);
            }

        });


        BottomNavigationView botNavView = (BottomNavigationView) findViewById(R.id.bottomNavigation);
        botNavView.getMenu().getItem(4).setChecked(true);//Set Middle(Home) to checked
        botNavView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener(){
            public boolean onNavigationItemSelected(@NonNull MenuItem item){
                switch(item.getItemId()){
                    case R.id.location:
                        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.calendar:
                        intent = new Intent(getApplicationContext(), ScheduleActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.home:
                        intent = new Intent(getApplicationContext(),HomeActivity.class);
                        startActivity(intent);
                        return true;
                    case R.id.qr:
                        intent = new Intent(getApplicationContext(), QRActivity.class);
                        startActivity(intent);
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        return true;
                    case R.id.settings:
                        return true;

                }
                return false;
            };
        });

        this.getSupportActionBar().hide();//Remove Title, probably not very good



    }

    public void onBackPressed() {
        SharedPreferences pref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        if (GoogleSignIn.getLastSignedInAccount(this) == null && pref.getString("firebaseUserId","1") == "1") {//If there is no google account detected, and if there is no account detected
            super.onBackPressed();
        }else{

        }
    }

}