package ru.timter.artbackendai.repository;

public interface ArtworkMatchProjection {
    Long getId();
    String getArtist();
    String getTitle();
    String getStyle();
    String getImageS3Url();
    Double getScore();
}
