package com.msaggik.musicplayer.model;

import android.net.Uri;

public class MusicTrack {

    // поля
    private long id; // идентификатор трека
    private long albumId; // идентификатор альбома
    private String title; // описание трека
    private Uri uri; // путь к треку
    private Uri imageUri; // путь к обложке трека
    private int size; // размер трека
    private int duration; // длительность трека
    private boolean likeTrack; // добавление трека в избранное

    // конструктор
    public MusicTrack(long id, long albumId, String title, Uri uri, Uri imageUri, int size, int duration, boolean likeTrack) {
        this.id = id;
        this.albumId = albumId;
        this.title = title;
        this.uri = uri;
        this.imageUri = imageUri;
        this.size = size;
        this.duration = duration;
        this.likeTrack = likeTrack;
    }

    // геттеры и сеттеры
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(long albumId) {
        this.albumId = albumId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public boolean isLikeTrack() {
        return likeTrack;
    }

    public void setLikeTrack(boolean likeTrack) {
        this.likeTrack = likeTrack;
    }
}
