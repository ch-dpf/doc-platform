package com.knowbase.persistence.jdbc;

import com.knowbase.persistence.support.KnowbaseSchemaSupport;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.lang.Nullable;

import javax.sql.DataSource;
import java.util.List;

/**
 * Qualifies raw {@code kb_*} SQL and temporarily sets {@code search_path} for connection callbacks.
 */
public final class SchemaAwareJdbcTemplate extends JdbcTemplate {

    private final KnowbaseSchemaSupport schemaSupport;

    public SchemaAwareJdbcTemplate(DataSource dataSource, KnowbaseSchemaSupport schemaSupport) {
        super(dataSource);
        this.schemaSupport = schemaSupport;
    }

    private String qualify(String sql) {
        return schemaSupport.qualifySql(sql);
    }

    @Override
    public int update(String sql) throws DataAccessException {
        return super.update(qualify(sql));
    }

    @Override
    public int update(String sql, @Nullable Object... args) throws DataAccessException {
        return super.update(qualify(sql), args);
    }

    @Override
    public int update(String sql, PreparedStatementSetter pss) throws DataAccessException {
        return super.update(qualify(sql), pss);
    }

    @Override
    public <T> T queryForObject(String sql, RowMapper<T> rowMapper, @Nullable Object... args) throws DataAccessException {
        return super.queryForObject(qualify(sql), rowMapper, args);
    }

    @Override
    public <T> T queryForObject(String sql, Class<T> requiredType, @Nullable Object... args) throws DataAccessException {
        return super.queryForObject(qualify(sql), requiredType, args);
    }

    @Override
    public <T> List<T> query(String sql, RowMapper<T> rowMapper, @Nullable Object... args) throws DataAccessException {
        return super.query(qualify(sql), rowMapper, args);
    }

    @Override
    public <T> List<T> query(String sql, @Nullable Object[] args, RowMapper<T> rowMapper) throws DataAccessException {
        return super.query(qualify(sql), args, rowMapper);
    }

    @Override
    public void query(String sql, RowCallbackHandler rch, @Nullable Object... args) throws DataAccessException {
        super.query(qualify(sql), rch, args);
    }

    @Override
    public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
        if (!schemaSupport.hasSchema()) {
            return super.execute(action);
        }
        ConnectionCallback<T> scopedAction = connection -> {
            String previousSearchPath = SearchPathSupport.read(connection);
            try {
                SearchPathSupport.set(connection, schemaSupport.schema() + ",public");
                return action.doInConnection(connection);
            } finally {
                SearchPathSupport.restore(connection, previousSearchPath);
            }
        };
        return super.execute(scopedAction);
    }
}
