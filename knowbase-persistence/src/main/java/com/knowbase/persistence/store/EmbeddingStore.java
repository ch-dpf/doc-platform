package com.knowbase.persistence.store;

import com.knowbase.persistence.support.ChunkSearchRow;
import com.knowbase.persistence.support.VectorSupport;
import com.pgvector.PGvector;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public final class EmbeddingStore {

    private static final RowMapper<ChunkSearchRow> SEARCH_ROW_MAPPER = new RowMapper<>() {
        @Override
        public ChunkSearchRow mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ChunkSearchRow(
                    rs.getObject("chunk_id", UUID.class),
                    rs.getObject("document_id", UUID.class),
                    rs.getObject("library_id", UUID.class),
                    rs.getObject("index_version_id", UUID.class),
                    rs.getString("content"),
                    rs.getString("metadata_json"),
                    rs.getDouble("score")
            );
        }
    };

    private final JdbcTemplate jdbcTemplate;

    public EmbeddingStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertEmbedding(UUID embeddingId, UUID chunkId, String model, int dimension, float[] vector) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            PGvector.addVectorType(connection);
            try (var statement = connection.prepareStatement(
                    """
                            INSERT INTO kb_embedding (embedding_id, chunk_id, embedding_model, embedding_dimension, embedding)
                            VALUES (?, ?, ?, ?, ?)
                            """
            )) {
                statement.setObject(1, embeddingId);
                statement.setObject(2, chunkId);
                statement.setString(3, model);
                statement.setInt(4, dimension);
                statement.setObject(5, VectorSupport.toPgVector(vector));
                statement.executeUpdate();
            }
            return null;
        });
    }

    public List<ChunkSearchRow> searchSimilar(UUID indexVersionId, float[] queryVector, int limit) {
        PGvector vector = VectorSupport.toPgVector(queryVector);
        return jdbcTemplate.execute((ConnectionCallback<List<ChunkSearchRow>>) connection -> {
            PGvector.addVectorType(connection);
            try (var statement = connection.prepareStatement(
                    """
                            SELECT c.chunk_id,
                                   c.document_id,
                                   c.library_id,
                                   c.index_version_id,
                                   c.content,
                                   c.metadata_json,
                                   1 - (e.embedding <=> ?::vector) AS score
                            FROM kb_chunk c
                            INNER JOIN kb_embedding e ON c.chunk_id = e.chunk_id
                            WHERE c.index_version_id = ?
                            ORDER BY e.embedding <=> ?::vector
                            LIMIT ?
                            """
            )) {
                statement.setObject(1, vector);
                statement.setObject(2, indexVersionId);
                statement.setObject(3, vector);
                statement.setInt(4, limit);
                try (var resultSet = statement.executeQuery()) {
                    List<ChunkSearchRow> rows = new java.util.ArrayList<>();
                    int rowNum = 0;
                    while (resultSet.next()) {
                        rows.add(SEARCH_ROW_MAPPER.mapRow(resultSet, rowNum++));
                    }
                    return rows;
                }
            }
        });
    }
}
