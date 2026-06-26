package com.knowbase.persistence.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

final class SearchPathSupport {

    private SearchPathSupport() {
    }

    static String read(Connection connection) throws SQLException {
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SHOW search_path")) {
            return resultSet.next() ? resultSet.getString(1) : null;
        }
    }

    static void set(Connection connection, String searchPath) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET search_path TO " + searchPath);
        }
    }

    static void restore(Connection connection, String previousSearchPath) throws SQLException {
        if (previousSearchPath == null || previousSearchPath.isBlank()) {
            set(connection, "\"$user\", public");
            return;
        }
        set(connection, previousSearchPath);
    }
}
