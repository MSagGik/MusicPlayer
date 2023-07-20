package com.msaggik.musicplayer.view.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.msaggik.musicplayer.R;

public class StartActivity extends AppCompatActivity implements Runnable{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        new Thread(this).start(); // запуск потока
        findViewById(R.id.imageNote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // переход на новую активность
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
            }
        });
    }

    @Override
    public void run() {
        // засыпание потока на 3 секунды
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // переход на новую активность
        startActivity(new Intent(getApplicationContext(), MainActivity.class));
    }
}