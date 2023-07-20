package com.msaggik.musicplayer.view.activities;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chibde.visualizer.BarVisualizer;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.jgabrielfreitas.core.BlurImageView;
import com.msaggik.musicplayer.R;
import com.msaggik.musicplayer.model.MusicTrack;
import com.msaggik.musicplayer.services.PlayerService;
import com.msaggik.musicplayer.viewmodel.adapters.MusicAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;

public class MainActivity extends AppCompatActivity {

    // поля
    // 1) разрешения на хранение данных
    private ActivityResultLauncher<String[]> storagePermissionLauncher; // для нескольких разрешений
    private final String [] PERMISSION = new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}; // разрешения

    // 2) логика воспроизведения
    private ExoPlayer exoPlayer; // плеер
    private List<MusicTrack> allMusicTrack = new ArrayList<>(); // список треков
    private int defaultStatusColor; // буферный цвет статус-бара
    private int repeatMode = 1; // флаг повтора: repeat all = 1, repeat one = 2, shuffle all = 3
    private boolean isBound = false; // флаг привязки к сервису

    // 3.1) представление
    // список треков
    private RecyclerView musicRecyclerview; // список треков
    private MusicAdapter musicAdapter; // адаптер для списка треков
    // нижняя панель управления
    private ConstraintLayout homeControlsWrapper; // нижняя панель управления
    private TextView homePlayingSongNameView; // вывод названия трека
    private ImageButton homePrevBtn, homePlayPauseBtn, homeNextBtn; // кнопки нижней панели управления
    // 3.2) разметка воспроизведения одного трека
    private ConstraintLayout playerView; // контейнер разметки воспроизведения одного трека
    private BlurImageView blurImageView; // задний размытый фон под тон обложки
    // верхняя панель управления
    private ImageButton playerCloseBtn; // кнопка перехода в главную активность из воспроизведения одного трека
    private TextView songNameView; // вывод названия трека
    // круглая обложка
    private CircleImageView artworkView; // контейнер
    // сикбар
    private SeekBar seekBar;
    private TextView progressView, durationView;
    // средняя панель управления
    private ImageButton repeatModeBtn, skipPreviousBtn, playPauseBtn, skipNextBtn, playlistBtn;
    // эквалайзер
    private BarVisualizer visualizer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // работа со статус-баром (самый верхний)
        defaultStatusColor = getWindow().getStatusBarColor(); // сохранение цвета статус бара
        getWindow().setNavigationBarColor(ColorUtils
                .setAlphaComponent(defaultStatusColor, 199)); // задание цвета и прозрачности

        // работа с тул-баром (верхний)
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getResources().getString(R.string.app_name)); // вывод названия приложения в тул-бар

        // связь с разметкой
        musicRecyclerview = findViewById(R.id.music_recyclerview);
        playerView = findViewById(R.id.playerView);
        playerCloseBtn = findViewById(R.id.playerCloseBtn);
        songNameView = findViewById(R.id.songNameView);
        skipPreviousBtn = findViewById(R.id.skipPreviousBtn);
        skipNextBtn = findViewById(R.id.skipNextBtn);
        playPauseBtn = findViewById(R.id.playPauseBtn);
        repeatModeBtn = findViewById(R.id.repeatModeBtn);
        playlistBtn = findViewById(R.id.playlistBtn);
        homePlayingSongNameView = findViewById(R.id.homePlayingSongNameView);
        homePrevBtn = findViewById(R.id.homePrevBtn);
        homePlayPauseBtn = findViewById(R.id.homePlayPauseBtn);
        homeNextBtn = findViewById(R.id.homeNextBtn);
        homeControlsWrapper = findViewById(R.id.homeControlsWrapper);
        artworkView = findViewById(R.id.artworkView);
        seekBar = findViewById(R.id.seekBar);
        progressView = findViewById(R.id.progressView);
        durationView = findViewById(R.id.durationView);
        visualizer = findViewById(R.id.visualizer);
        blurImageView = findViewById(R.id.blurImageView);

        // получение доступа к памяти
        registerPermission(); // регистрация разрешений и запуск методов извлечения треков и эквалайзера
        checkPermission(); // проверка и получение разрешения
        // метод привязки сервиса
        doBindService();
    }

    // метод проверки разрешений
    private void checkPermission() {
        // проверка разрешений
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            //Toast.makeText(this, "Данны разрешения на чтение аудио-файлов и демонстрацию эквалайзера", Toast.LENGTH_SHORT).show();
        } else { // запрос нескольких разрешений
            storagePermissionLauncher.launch(PERMISSION);
        }
    }

    // регистрация разрешений
    private void registerPermission() {
        storagePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                result -> result.forEach(
                        (permission, granted) -> {
                            switch (permission) {
                                case Manifest.permission.READ_EXTERNAL_STORAGE:
                                    if (granted) {
                                        //Toast.makeText(this, "Разрешение дано к аудиофайлам", Toast.LENGTH_SHORT).show();
                                        // извлечение треков из памяти
                                        fetchMusicTrack();
                                    } else {
                                        Toast.makeText(this, "Доступ к аудиофайлам запрещён", Toast.LENGTH_SHORT).show();
                                    } break;
                                case Manifest.permission.RECORD_AUDIO:
                                    if (granted) {
                                        //Toast.makeText(this, "Разрешение дано к демонстрации эквалайзера", Toast.LENGTH_SHORT).show();
                                        // активация эквалайзера
                                        activateAudioVisualizer();
                                    } else {
                                        Toast.makeText(this, "Доступ к демонстрации эквалайзера запрещён", Toast.LENGTH_SHORT).show();
                                    } break;
                            }
                        }));
    }

    private void doBindService() {
        Intent playerServiceIntent = new Intent(this, PlayerService.class);
        // подключение к сервису плеера
        bindService(playerServiceIntent, playerServiceConnection, Context.BIND_AUTO_CREATE);
    }

    // новое сервисное соединение
    ServiceConnection playerServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // получить экземпляр сервиса
            PlayerService.ServiceBinder binder = (PlayerService.ServiceBinder) iBinder;
            // создание экземпляра плеера
            exoPlayer = binder.getPlayerService().getPlayer();
            isBound = true;
            // демонстрация песни
            storagePermissionLauncher.launch(PERMISSION);
            // вызов модуля управления плеером
            playerControls();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    private void playerControls() {
        // song name marquee
        songNameView.setSelected(true);
        homePlayingSongNameView.setSelected(true);

        // выход из представления плеера
        playerCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitPlayerView();
            }
        });
        playlistBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitPlayerView();
            }
        });
        // открыть представление плеера
        homeControlsWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerView.setVisibility(View.VISIBLE);
            }
        });
        // добавление слушателя в плеер
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                // демонстрация воспроизведения песни
                assert mediaItem != null;
                songNameView.setText(mediaItem.mediaMetadata.title);
                homePlayingSongNameView.setText(mediaItem.mediaMetadata.title);

                // данные для сикбара
                progressView.setText(getReadableTime((int) exoPlayer.getCurrentPosition()));
                seekBar.setProgress((int) exoPlayer.getCurrentPosition());
                seekBar.setMax((int) exoPlayer.getDuration());
                durationView.setText(getReadableTime((int) exoPlayer.getDuration()));
                playPauseBtn.setImageResource(R.drawable.btn_pause);
                homePlayPauseBtn.setImageResource(R.drawable.btn_pause);

                // демонстрация обложки
                artworkView.setImageURI(Objects.requireNonNull(exoPlayer.getCurrentMediaItem()).mediaMetadata.artworkUri);
                if (artworkView.getDrawable() == null) {
                    artworkView.setImageResource(R.drawable.icon_logo);
                }
                // обновление сикбара
                updatePlayerPositionProgress();

                // загрузка анимации поворота обложки
                artworkView.setAnimation(loadRotation());

                // установка визуализатора
                activateAudioVisualizer();
                // обновление цветов в плеере
                //updatePlayerColor();

                if (!exoPlayer.isPlaying()) {
                    exoPlayer.play();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);

                // проверка плеера к воспроизведению
                if (playbackState == ExoPlayer.STATE_READY) {
                    // установка значений для представления плеера
                    songNameView.setText(Objects.requireNonNull(exoPlayer.getCurrentMediaItem()).mediaMetadata.title);
                    homePlayingSongNameView.setText(exoPlayer.getCurrentMediaItem().mediaMetadata.title);
                    progressView.setText(getReadableTime((int) exoPlayer.getCurrentPosition()));
                    durationView.setText(getReadableTime((int) exoPlayer.getDuration()));
                    seekBar.setMax((int) exoPlayer.getDuration());
                    seekBar.setProgress((int) exoPlayer.getCurrentPosition());
                    playPauseBtn.setImageResource(R.drawable.btn_pause);
                    homePlayPauseBtn.setImageResource(R.drawable.btn_pause);

                    // демонстрация обложки
                    artworkView.setImageURI(Objects.requireNonNull(exoPlayer.getCurrentMediaItem()).mediaMetadata.artworkUri);
                    if (artworkView.getDrawable() == null) {
                        artworkView.setImageResource(R.drawable.icon_logo);
                    }
                    // обновление сикбара
                    updatePlayerPositionProgress();

                    // загрузка анимации поворота обложки
                    artworkView.setAnimation(loadRotation());

                    // установка визуализатора
                    activateAudioVisualizer();
                    // обновление цветов в плеере
                    updatePlayerColors();

                    if (!exoPlayer.isPlaying()) {
                        exoPlayer.play();
                    }
                } else {
                    playPauseBtn.setImageResource(R.drawable.btn_play);
                    homePlayPauseBtn.setImageResource(R.drawable.btn_play);
                }

            }
        });

        // переход к следующему треку
        skipNextBtn.setOnClickListener(view -> {
            if (exoPlayer.hasNextMediaItem()) {
                exoPlayer.seekToNext();
            }
        });
        homeNextBtn.setOnClickListener(view -> {
            if (exoPlayer.hasNextMediaItem()) {
                exoPlayer.seekToNext();
            }
        });
        // переход к предыдущему треку
        skipPreviousBtn.setOnClickListener(view -> {
            if (exoPlayer.hasPreviousMediaItem()) {
                exoPlayer.seekToPrevious();
            }
        });
        homePrevBtn.setOnClickListener(view -> {
            if (exoPlayer.hasPreviousMediaItem()) {
                exoPlayer.seekToPrevious();
            }
        });
        // кнопка плей/пауза
        playPauseBtn.setOnClickListener(view -> {
            playPausePlayer();
        });
        homePlayPauseBtn.setOnClickListener(view -> {
            playPausePlayer();
        });
        // слушатель для сикбара
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressValue = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progressValue = seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (exoPlayer.getPlaybackState() == ExoPlayer.STATE_READY) {
                    seekBar.setProgress(progressValue);
                    progressView.setText(getReadableTime(progressValue));
                    exoPlayer.seekTo(progressValue);
                }
            }
        });

        // режим повтора
        repeatModeBtn.setOnClickListener(view -> {
            if (repeatMode == 1) { // один повтор
                exoPlayer.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
                repeatMode++;
                repeatModeBtn.setImageResource(R.drawable.btn_repeat);
            } else if (repeatMode == 2) { // воспроизведение песен в случайном порядке
                exoPlayer.setShuffleModeEnabled(true);
                exoPlayer.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                repeatMode++;
                repeatModeBtn.setImageResource(R.drawable.btn_shuffle);
            } else if (repeatMode == 3) { // воспроизведение песен в случайном порядке
                exoPlayer.setShuffleModeEnabled(false);
                exoPlayer.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                repeatMode = 1;
                repeatModeBtn.setImageResource(R.drawable.btn_repeat_one);
            }
            // обновление цветов плеера
            updatePlayerColors();
        });
    }

    private void playPausePlayer(){
        if (exoPlayer.isPlaying()) {
            exoPlayer.pause();
            playPauseBtn.setImageResource(R.drawable.btn_play);
            homePlayPauseBtn.setImageResource(R.drawable.btn_play);
            // очистка анимации
            artworkView.clearAnimation();
        } else {
            exoPlayer.play();
            playPauseBtn.setImageResource(R.drawable.btn_pause);
            homePlayPauseBtn.setImageResource(R.drawable.btn_pause);
            // запуск анимации
            artworkView.startAnimation(loadRotation());
        }
        // обновление цветов плеера
        updatePlayerColors();
    }

    private void updatePlayerColors() {
        // видимость представления плеера
        if (playerView.getVisibility() == View.GONE) {
            return;
        }

        BitmapDrawable bitmapDrawable = (BitmapDrawable) artworkView.getDrawable();

        if (bitmapDrawable == null) {
            bitmapDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.icon_logo);
        }
        assert bitmapDrawable != null;
        Bitmap bitmap = bitmapDrawable.getBitmap();
        // размытие растрового изображения для фона плеера
        blurImageView.setImageBitmap(bitmap);
        blurImageView.setBlur(5);
        // цвет панели управления плеера
        Palette.from(bitmap).generate(palette -> {
            if (palette != null ) {
                Palette.Swatch swatch = palette.getDarkVibrantSwatch();
                if (swatch == null) {
                    swatch = palette.getMutedSwatch();
                    if (swatch == null) {
                        swatch = palette.getDominantSwatch();
                    }
                }
                // изменение цвета текста
                assert swatch != null;
                int titleTextColor = swatch.getTitleTextColor();
                int bodyTextColor = swatch.getBodyTextColor();
                int rgbColor = swatch.getRgb();

                // установка цветов
                // цвет статуса и навигации бара
                getWindow().setStatusBarColor(rgbColor);
                getWindow().setNavigationBarColor(rgbColor);
                // цвет текста
                songNameView.setTextColor(titleTextColor);
                progressView.setTextColor(bodyTextColor);
                durationView.setTextColor(bodyTextColor);


            }
        });
    }

    private void updatePlayerPositionProgress() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (exoPlayer.isPlaying()){
                    progressView.setText(getReadableTime((int) exoPlayer.getCurrentPosition()));
                    seekBar.setProgress((int) exoPlayer.getCurrentPosition());
                }

                //repeat calling method
                updatePlayerPositionProgress();
            }
        }, 1000);
    }
    private Animation loadRotation() {
        RotateAnimation rotateAnimation = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(100000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        return rotateAnimation;
    }

    private String getReadableTime(int currentPosition) {
        int second = (int) (currentPosition / 1000); // определение количества секунд
        int minute = second / 60; // определение количества минут
        int hour = minute / 60; // определение количества часов
        int day = hour / 24; // определение количества дней

        second = second % 60; // ограничение количества секунд 60 секундами
        minute = minute % 60; // ограничение количества минут 60 минутами
        hour = hour % 24; // ограничение количества часов 24 часами

        // запись времени
        if (hour < 1) {
            return String.format("%02d:%02d", minute, second);
        } else {
            return String.format("%01d:%02d:%02d", hour, minute, second);
        }
    }

    // аудио визуализация
    private void activateAudioVisualizer() {
        // проверка наличия разрешения на показ аудиовизуализатора
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // установка цвета для аудиовизаулизатора
        visualizer.setColor(ContextCompat.getColor(this, R.color.colorAccentDarkTheme));
        // установка номера визаулизатора от 10 до 256
        visualizer.setDensity(10);
        // запуск визаулизатора вместе с плеером
        visualizer.setPlayer(exoPlayer.getAudioSessionId());


    }


    // метод извлечения всех треков из памяти
    private void fetchMusicTrack() {
        List<MusicTrack> musicTracks = new ArrayList<>();
        Uri mediaStoreUri;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaStoreUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            mediaStoreUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = new String[] {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM_ID,
        };

        String sortedOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        // получение треков
        try (Cursor cursor = getApplicationContext().getContentResolver().query(mediaStoreUri, projection, null, null, sortedOrder)) {
            // кэш курсора индекса
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int idAlbum = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
            boolean likeTrack = false; // значение трека в избранное

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                long idAlb = cursor.getLong(idAlbum);
                String name = cursor.getString(nameColumn);
                int duration = cursor.getInt(durationColumn);
                int size = cursor.getInt(sizeColumn);
                long albumId = cursor.getLong(albumIdColumn);

                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                Uri albumImageUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);

                name = name.substring(0, name.lastIndexOf("."));

                MusicTrack musicTrack = new MusicTrack(id, idAlb, name, uri, albumImageUri, size, duration, likeTrack);

                musicTracks.add(musicTrack);
            }

            // отображение треков на экране
            showSongs(musicTracks);
        } catch (Exception e) {
            Toast.makeText(this, "Файлы не прочитаны", Toast.LENGTH_LONG).show();
        }
    }

    // метод отображения треков на экране
    private void showSongs(List<MusicTrack> musicTracks) {
        if (musicTracks.size() == 0) {
            Toast.makeText(this, "На устройстве нет музыкальных треков", Toast.LENGTH_LONG).show();
            return;
        }
        // обновление списка треков
        allMusicTrack.clear();
        allMusicTrack.addAll(musicTracks);

        // обновление названия в экшен-баре
        String title = getResources().getString(R.string.app_name) + " - " + musicTracks.size();
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);

        // лайаут мэнеджер
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        musicRecyclerview.setLayoutManager(layoutManager);

        // адаптер
        musicAdapter = new MusicAdapter(this, musicTracks, exoPlayer, playerView);
        // передача адаптера на ресайклервью
        //recyclerView.setAdapter(musicTrackAdapter);

        // анимированный ресайклервью
        ScaleInAnimationAdapter scaleInAnimationAdapter = new ScaleInAnimationAdapter(musicAdapter);
        scaleInAnimationAdapter.setDuration(1000);
        scaleInAnimationAdapter.setInterpolator(new OvershootInterpolator());
        scaleInAnimationAdapter.setFirstOnly(false);
        musicRecyclerview.setAdapter(scaleInAnimationAdapter);

    }

    // метод поиска трека
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    // остановка и очистка плеера
    @Override
    protected void onDestroy() {
        super.onDestroy();

//        if (exoPlayer.isPlaying()) {
//            exoPlayer.stop();
//        }
//        exoPlayer.release();

        // отмена привязки к сервису
        doUnbindService();
    }

    private void doUnbindService() {
        if (isBound) {
            unbindService(playerServiceConnection);
            isBound = false;
        }
    }

    // геттер для разрешения
    public ActivityResultLauncher<String[]> getStoragePermissionLauncher() {
        return storagePermissionLauncher;
    }

    // переопределение кнопки назад
    @Override
    public void onBackPressed() {
        if (playerView.getVisibility() == View.VISIBLE) {
            exitPlayerView();
        } else {
            super.onBackPressed();
        }
    }

    private void exitPlayerView() {
        playerView.setVisibility(View.GONE); // установление невидимости плеера
        getWindow().setStatusBarColor(defaultStatusColor);
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor, 199));
    }
}