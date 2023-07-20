package com.msaggik.musicplayer.viewmodel.adapters;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.MediaMetadata;
import com.msaggik.musicplayer.R;
import com.msaggik.musicplayer.model.MusicTrack;
import com.msaggik.musicplayer.services.PlayerService;
import com.msaggik.musicplayer.view.activities.MainActivity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{
    // поля
    private Context context;
    private List<MusicTrack> musicTrackList;
    private ExoPlayer exoPlayer;
    private ConstraintLayout playerView;

    // конструктор
    public MusicAdapter(Context context, List<MusicTrack> musicTrackList, ExoPlayer exoPlayer, ConstraintLayout playerView) {
        this.context = context;
        this.musicTrackList = musicTrackList;
        this.exoPlayer = exoPlayer;
        this.playerView = playerView;
    }

    @Override
    public int getItemCount() {
        return musicTrackList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        private ImageView imageAlbumHolder;
        private TextView numberHolder, titleHolder, durationHolder, sizeHolder;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            imageAlbumHolder = itemView.findViewById(R.id.imageAlbumItem);
            numberHolder = itemView.findViewById(R.id.numberItem);
            titleHolder = itemView.findViewById(R.id.titleItem);
            durationHolder = itemView.findViewById(R.id.durationItem);
            sizeHolder = itemView.findViewById(R.id.sizeItem);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        MusicTrack musicTrack = musicTrackList.get(position);
        ViewHolder viewHolder = (ViewHolder) holder;

        // загрузка информации о треке
        viewHolder.numberHolder.setText(String.valueOf(position+1)); // номер трека
        viewHolder.titleHolder.setText(musicTrack.getTitle()); // название трека
        viewHolder.durationHolder.setText(getDuration(musicTrack.getDuration())); // длительность трека
        viewHolder.sizeHolder.setText(getSize(musicTrack.getSize())); // размер трека

        // загрузка обложки трека
        Uri imageMusicTrackUri = musicTrack.getImageUri();
        if (imageMusicTrackUri != null) { // если обложка есть, то
            viewHolder.imageAlbumHolder.setImageURI(imageMusicTrackUri);
        } else if (viewHolder.imageAlbumHolder.getDrawable() == null) { // иначе загрузка обложки по умолчанию
            viewHolder.imageAlbumHolder.setImageResource(R.drawable.btn_play);
        }

        // при нажатии на трек (item) произойдёт воспроизведение трека и переключение на активность/фрагмент и сервис
        viewHolder.itemView.setOnClickListener(view -> {

            // запуск сервиса плеера
            context.startService(new Intent(context.getApplicationContext(), PlayerService.class));

            // показать представление плеера
            playerView.setVisibility(View.VISIBLE);

            // воспроизведение плеера
            if (!exoPlayer.isPlaying()) {
                exoPlayer.setMediaItems(getMediaItems(), position, 0);
            } else {
                exoPlayer.pause();
                exoPlayer.seekTo(position,0);
            }
            // воспроизведение трека
            exoPlayer.prepare();
            exoPlayer.play();

            Toast.makeText(context, musicTrack.getTitle(), Toast.LENGTH_LONG).show();

            // проверка разрешения на включение записи
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // запрос разрешения
                ((MainActivity)context).getStoragePermissionLauncher().launch(new String[]{Manifest.permission.RECORD_AUDIO});

            }
        });
    }

    // формирование списка треков
    private List<MediaItem> getMediaItems() {
        // список треков
        List<MediaItem> mediaItems = new ArrayList<>();

        for (MusicTrack mTrack : musicTrackList) {
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(mTrack.getUri())
                    .setMediaMetadata(getMetaData(mTrack))
                    .build();
            mediaItems.add(mediaItem);
        }
        return mediaItems;
    }

    // формирования набора метаданных
    private MediaMetadata getMetaData(MusicTrack mTrack) {
        return new MediaMetadata.Builder()
                .setTitle(mTrack.getTitle())
                .setArtworkUri(mTrack.getImageUri())
                .build();
    }

    // фильтр треков
    @SuppressLint("NotifyDataSetChanged")
    public void filterMusicTrack(List<MusicTrack> filteredMusicList) {
        musicTrackList = filteredMusicList;
        notifyDataSetChanged();
    }

    //  метод конвертирования длительности трека
    @SuppressLint("DefaultLocale")
    private String getDuration(int totalDuration) {

        int second = totalDuration / 1000; // определение количества секунд
        int minute = second / 60; // определение количества минут
        int hour = minute / 60; // определение количества часов

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

    //  метод конвертирования размера трека
    private String getSize(long totalSize) {

        double kilobyte = totalSize / 1024.0; // количество килобайт
        double megabyte = kilobyte / 1024.0; // количество мегабайт
        double gigabyte = megabyte / 1024.0; // количество гигабайт
        double terabyte = gigabyte / 1024.0; // количество терабайт

        DecimalFormat decimalFormat = new DecimalFormat("0.00");

        if (terabyte > 1) {
            return decimalFormat.format(terabyte).concat(" TB");
        } else if (gigabyte > 1) {
            return decimalFormat.format(gigabyte).concat(" GB");
        } else if (megabyte > 1) {
            return decimalFormat.format(megabyte).concat(" MB");
        } else if (kilobyte > 1) {
            return decimalFormat.format(kilobyte).concat(" KB");
        } else {
            return "";
        }
    }
}
