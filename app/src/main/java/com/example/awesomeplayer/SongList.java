package com.example.awesomeplayer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SongList extends AppCompatActivity {

    Context context;
    String[] ListElements = new String[]{};
    ListView listView;
    List<String> ListElementsArrayList;
    ArrayAdapter<String> adapter;
    ContentResolver contentResolver;
    Cursor cursor;
    Uri uri;
    public static final int RUNTIME_PERMISSION_CODE = 7;
    HashMap<String, String> SongD;
    DataBaseHelper db;
    float temperaturebms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_list);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        SongD = new HashMap<>();
        listView = findViewById(R.id.listView);
        context = getApplicationContext();
        ListElementsArrayList = new ArrayList<>(Arrays.asList(ListElements));
        Collections.sort(ListElementsArrayList);
        adapter = new ArrayAdapter<>(SongList.this, android.R.layout.simple_list_item_1, ListElementsArrayList);
        AndroidRuntimePermission();
        GetAllMediaMp3Files();
        listView.setAdapter(adapter);
        db = new DataBaseHelper(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String tempo = db.cEntry(SongD.get(parent.getAdapter().getItem(position).toString()),temperaturebms);
                if(SongD.get(parent.getAdapter().getItem(position).toString()).equals(db.cEntry(SongD.get(parent.getAdapter().getItem(position).toString()),temperaturebms)))
                {
                    db.EntryUpdate(SongD.get(parent.getAdapter().getItem(position).toString()),temperaturebms);
                }else
                {
                    db.SongEntry(SongD.get(parent.getAdapter().getItem(position).toString()),parent.getAdapter().getItem(position).toString(),temperaturebms);
                }
                String SongMessage = SongD.get(parent.getAdapter().getItem(position).toString());
                Intent intent = new Intent(SongList.this, SongPlay.class);
                intent.putExtra("SongData", SongMessage);
                startActivity(intent);
            }
        });
    }
    public void GetAllMediaMp3Files() {
        contentResolver = context.getContentResolver();
        uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String[] STAR={"*"};
        cursor = contentResolver.query(
                uri,
                STAR,
                selection,
                null,
                null
        );
        if (cursor == null) {
            Toast.makeText(SongList.this, "Something Went Wrong.", Toast.LENGTH_SHORT);
        } else if (!cursor.moveToFirst()) {
            Toast.makeText(SongList.this, "No Music Found on SD Card.", Toast.LENGTH_SHORT);
        } else {
            int Title = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int Data = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            do {
                String SongTitle = cursor.getString(Title);
                String SongData = cursor.getString(Data);
                ListElementsArrayList.add(SongTitle);
                SongD.put(SongTitle , SongData);
            } while (cursor.moveToNext());
        }
    }
    public void AndroidRuntimePermission(){

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){

            if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                if(shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)){
                    AlertDialog.Builder alert_builder = new AlertDialog.Builder(SongList.this);
                    alert_builder.setMessage("External Storage Permission is Required.");
                    alert_builder.setTitle("Please Grant Permission.");
                    alert_builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(
                                    SongList.this,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    RUNTIME_PERMISSION_CODE
                            );
                        }
                    });
                    alert_builder.setNeutralButton("Cancel",null);
                    AlertDialog dialog = alert_builder.create();
                    dialog.show();
                }
                else {
                    ActivityCompat.requestPermissions(
                            SongList.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            RUNTIME_PERMISSION_CODE
                    );
                }
            }else {
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case RUNTIME_PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                }
            }
        }
    }
}
