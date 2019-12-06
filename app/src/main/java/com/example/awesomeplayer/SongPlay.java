package com.example.awesomeplayer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class SongPlay extends Activity implements AudioManager.OnAudioFocusChangeListener {

    ContentResolver contentResolver;
    Cursor cursor;
    Uri uri;
    TextView txtSongTitle,txtSongArtist;
    private TextView txtTimestamp;
    Handler mHandler;
    ImageView imgAlbumArt, btnPlayPause;
    MediaPlayer mPlayer;
    SeekBar mSeekBar;
    private Runnable mRunnable;
    AudioManager mAudioManager;
    String SongTitle, SongArtist, str ;
    DataBaseHelper db;
    private SeekBar volumeSeekbar = null;
    private AudioManager audioManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_play);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        initControls();

        if (Intent.ACTION_VIEW.equals(getIntent().getAction()))
        {
            str = (getIntent().getData().getPath());
        }else {
            Bundle extras = getIntent().getExtras();
            str = extras.getString("SongData");
        }
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        txtSongTitle = findViewById(R.id.txtSongTitle);
        txtSongArtist = findViewById(R.id.txtSongArtist);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        txtTimestamp = findViewById(R.id.txtTimestamp);
        mSeekBar = findViewById(R.id.seekBarTimestamp);
        imgAlbumArt = findViewById(R.id.imgAlbumArt);
        mHandler = new Handler();
        mPlayer = new MediaPlayer();
        db = new DataBaseHelper(this);
        contentResolver = SongPlay.this.getContentResolver();

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
            Toast.makeText(SongPlay.this, "Something Went Wrong.", Toast.LENGTH_SHORT);
        } else if (!cursor.moveToFirst()) {
            Toast.makeText(SongPlay.this, "No Music Found on SD Card.", Toast.LENGTH_SHORT);
        } else {
            int Title = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int Data = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int Artist = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int ID = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int Duration = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int AlbumId = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            do {
                String SongData = cursor.getString(Data);
                if(SongData.equals(str))
                {
                    SongTitle = cursor.getString(Title);
                    SongArtist = cursor.getString(Artist);
                    int SongDuration = Integer.parseInt(cursor.getString(Duration));
                    long SongAlbumID = Long.parseLong(cursor.getString(AlbumId));
                    SongDuration=SongDuration/1000;
                    txtTimestamp.setText("0:0 | "+Integer.toString(SongDuration/60)+":"+Integer.toString(SongDuration%60));
                    Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
                    Uri albumArtUri = ContentUris.withAppendedId(sArtworkUri, SongAlbumID);
                    Bitmap bitmap= BitmapFactory.decodeResource(getResources(), R.drawable.newpic);
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(SongPlay.this.getContentResolver(), albumArtUri);
                    }catch(Exception e){}
                    txtSongTitle.setText(SongTitle);
                    txtSongArtist.setText(SongArtist);
                    imgAlbumArt.setImageBitmap(bitmap);
                    break;
                }
            } while (cursor.moveToNext());
        }
        try{
            mPlayer.setDataSource(str);
            mPlayer.prepareAsync();
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    mPlayer.seekTo(db.ReadPos(str));
                    int duration  = mPlayer.getDuration()/1000; // In milliseconds
                    int pass =  mPlayer.getCurrentPosition()/1000;
                    txtTimestamp.setText(Integer.toString(pass/60)+":"+Integer.toString(pass%60)+" | "+Integer.toString(duration/60)+":"+Integer.toString(duration%60));
                    mSeekBar.setMax(mPlayer.getDuration());
                    SongPlay.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(mPlayer != null){
                                int mcurpos=mPlayer.getCurrentPosition();
                                mSeekBar.setProgress(mcurpos);
                                int duration  = mPlayer.getDuration()/1000; // In milliseconds
                                int pass =  mPlayer.getCurrentPosition()/1000;
                                txtTimestamp.setText(Integer.toString(pass/60)+":"+Integer.toString(pass%60)+" | "+Integer.toString(duration/60)+":"+Integer.toString(duration%60));
                            }
                            mHandler.postDelayed(this, 1);
                        }
                    });
                }
            });
        }catch (Exception e){
            try{
                mPlayer.start();
            }catch (Exception a){}
        }
        btnPlayPause.setImageResource(R.drawable.pauseim);

        btnPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mPlayer.isPlaying())
                {
                    btnPlayPause.setImageResource(R.drawable.playim);
                    mPlayer.pause();
                }else
                {
                    mPlayer.start();
                    btnPlayPause.setImageResource(R.drawable.pauseim);
                }
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(mPlayer != null && fromUser){
                    mPlayer.seekTo(progress);
                }
            }
        });
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                mPlayer.start();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mPlayer.isPlaying()) mPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mPlayer.isPlaying()) mPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mPlayer.isPlaying()) mPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if(mPlayer.isPlaying()) {
            if(mPlayer!=null){
                mPlayer.stop();
                mPlayer.release();
                mPlayer = null;
                if(mHandler!=null){
                    mHandler.removeCallbacks(mRunnable);
                }
            }
        }
        mAudioManager.abandonAudioFocus(this);
    }
    private void initControls()
    {
        try
        {
            volumeSeekbar = findViewById(R.id.seekBarVolume);
            audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            volumeSeekbar.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
            volumeSeekbar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));


            volumeSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
            {
                @Override
                public void onStopTrackingTouch(SeekBar arg0)
                {
                }

                @Override
                public void onStartTrackingTouch(SeekBar arg0)
                {
                }

                @Override
                public void onProgressChanged(SeekBar arg0, int progress, boolean arg2)
                {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                            progress, 0);
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}