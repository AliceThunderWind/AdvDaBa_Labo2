package org.example;

import lombok.Getter;
import lombok.Setter;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

@Getter
@Setter
public class Neo4JDriver implements AutoCloseable {

    private final Driver driver;

    public Neo4JDriver(String uri, String user, String password) {
        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    @Override
    public void close() {
        driver.close();
    }

}
