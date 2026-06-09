package ru.timter.artbackendai.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "artworks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artwork {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String artist;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String style;

    @Column(name = "image_s3_url", nullable = false)
    private String imageS3Url;
}
