package com.example.ca1;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class QRActivity extends AppCompatActivity {

    SurfaceView surfaceView;
    CameraSource cameraSource;
    TextView textView;
    BarcodeDetector barcodeDetector;
    Geocoder geocoder;
    String locationName;

    SimpleDateFormat dateFormat = new SimpleDateFormat("kk:mm dd/MM/yyyy");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        String userid = "";
        SharedPreferences pref = getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        GoogleSignInAccount gAcc = GoogleSignIn.getLastSignedInAccount(this);

        if (gAcc != null) {
            userid = gAcc.getId();
        } else {
            userid = pref.getString("firebaseUserId", "1");
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://schedulardb-default-rtdb.firebaseio.com");

        DatabaseReference myDbRef = database.getReference("usersInformation").child(userid).child("UserAlarms");

        setContentView(R.layout.qr_code);
        surfaceView = ((SurfaceView) findViewById(R.id.cameraPreview));
        surfaceView.setVisibility(View.INVISIBLE);
        textView = (TextView) findViewById(R.id.textView);

        barcodeDetector = new BarcodeDetector
                .Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        surfaceView.setVisibility(View.VISIBLE);
        cameraSource = new CameraSource.Builder(this, barcodeDetector).setRequestedPreviewSize(900, 480).setAutoFocusEnabled(true).build();


        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, PackageManager.PERMISSION_GRANTED);
                    return;
                } else {
                    try {
                        cameraSource.start(surfaceView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }


            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {

            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                geocoder = new Geocoder(getApplication(), Locale.getDefault());
                SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                Calendar cal = Calendar.getInstance();
                if (qrCodes.size() != 0) {
                    for (int i = 0; i < qrCodes.size(); ++i) {
                        int id = qrCodes.keyAt(i);
                        Barcode qrCode = qrCodes.get(id);
                        Log.e("Bounding Box", qrCode.getBoundingBox().toString());
                        Log.e("Center Y", String.valueOf(qrCode.getBoundingBox().centerY()));
                        Log.e("Center X", String.valueOf(qrCode.getBoundingBox().centerX()));

                        if (qrCode.getBoundingBox().centerY() > 325 && qrCode.getBoundingBox().centerY() < 575) {
                            textView.post(new Runnable() {
                                public void run() {
                                    cameraSource.stop();
                                    Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                                    vibrator.vibrate(250);
                                    String qrcode = qrCodes.valueAt(0).displayValue;
                                    JSONObject jObject = null;
                                    try {
                                        if (textView.getText() != "Place the QR code in the middle of the screen.") {
                                            textView.setText("Place the QR code in the middle of the screen.");
                                        };

                                        Alarm alarm = new Alarm();
                                        jObject = new JSONObject(qrcode);

                                        alarm.setTitle(jObject.getString("title"));
                                        alarm.setDescription(jObject.getString("description"));
                                        alarm.setLongitude(jObject.getDouble("longitude"));
                                        alarm.setLatitude(jObject.getDouble("latitude"));
                                        alarm.setUnixTime(jObject.getLong("unixTime"));
                                        cal.setTimeInMillis(alarm.getUnixTime());

                                        String alphabet = "123456789";
                                        StringBuilder sb = new StringBuilder();
                                        Random random = new Random();

                                        locationName = geocoder.getFromLocation(alarm.getLongitude(),alarm.getLatitude(),1).get(0).getAddressLine(0);

                                        int length = 21;

                                        for (int i = 0; i < length; i++) {
                                            int index = random.nextInt(alphabet.length());
                                            char randomChar = alphabet.charAt(index);
                                            sb.append(randomChar);
                                        }
                                        String randomString = sb.toString();
                                        alarm.setUid(randomString);

                                        new Handler().postDelayed(new Runnable() {//Using a handler works, for some reason.....
                                            @Override
                                            public void run() {
                                                AlertDialog.Builder builder = new AlertDialog.Builder(QRActivity.this);
                                                builder.setMessage(
                                                        "Add this task to your calendar?\n" +
                                                        "\nTitle: "+alarm.getTitle()+
                                                        "\nDescription: "+alarm.getDescription()+
                                                        "\nDate and Time: "+dateFormat.format(cal)+
                                                        "\nLocation: "+locationName);
                                                builder.setCancelable(true);

                                                builder.setPositiveButton(
                                                        "Yes",
                                                        new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int id) {
                                                            try{
                                                                cameraSource.start(surfaceView.getHolder());//This isnt really an error, its a warning, we know what we are doing
                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                            }
                                                                myDbRef.child(randomString).setValue(alarm);
                                                                dialog.cancel();
                                                            }
                                                        });

                                                builder.setNegativeButton(
                                                        "No",
                                                        new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int id) {
                                                                try{
                                                                    cameraSource.start(surfaceView.getHolder());//This isnt really an error, its a warning, we know what we are doing
                                                                } catch (IOException e) {
                                                                    e.printStackTrace();
                                                                }
                                                                dialog.cancel();
                                                            }
                                                        });

                                                AlertDialog alert11 = builder.create();
                                                alert11.show();
                                            }
                                        },100);

                                        //Add some code to stop it to prevent spamming firebase
                                    }catch(JSONException | IOException e){
                                        //Add text
                                        textView.setText("Invalid JSON!");
                                        Log.e("Error",e.toString());
                                    }

                                }
                            });
                        }

                    }

                }
            }
        });

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults) {//Handles the permission response from user
        if (requestCode == PackageManager.PERMISSION_GRANTED) {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("Accepted","Accepted");
                    try {
                        cameraSource.start(surfaceView.getHolder());//This error can be ignored
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.i("Denied","Denied");
                }
                return;
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }
}



