package com.whj.vlc.play;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.whj.vlc.player.VlcPlayActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this,VlcPlayActivity.class);
        intent.putExtra(VlcPlayActivity.URL, "rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov");
        startActivity(intent);
    }
}
