package ru.timter.artbackendai.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.timter.artbackendai.entity.Artwork;

import java.util.List;

public interface ArtworkRepository extends JpaRepository<Artwork, Long> {

    @Query(value = "SELECT id, artist, title, style, image_s3_url as imageS3Url, (1 - (embedding <=> cast(:query_embedding as vector))) as score " +
                   "FROM artworks ORDER BY embedding <=> cast(:query_embedding as vector) ASC LIMIT 6", nativeQuery = true)
    List<ArtworkMatchProjection> findTop6SimilarWithScore(@Param("query_embedding") String queryEmbedding);

    @Query(value = "SELECT id, artist, title, style, image_s3_url as imageS3Url, (1 - (embedding <=> cast(:query_embedding as vector))) as score " +
                   "FROM artworks ORDER BY embedding <=> cast(:query_embedding as vector) ASC LIMIT :limit OFFSET :offset", nativeQuery = true)
    List<ArtworkMatchProjection> findSimilarPaginatedWithScore(@Param("query_embedding") String queryEmbedding, @Param("limit") int limit, @Param("offset") int offset);
}
