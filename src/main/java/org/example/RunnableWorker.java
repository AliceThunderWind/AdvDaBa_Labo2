package org.example;

import lombok.AllArgsConstructor;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.exceptions.TransientException;
import org.neo4j.kernel.DeadlockDetectedException;

import java.util.Map;
import java.util.logging.Logger;

@AllArgsConstructor
class RunnableWorker implements Runnable {

    private static final Logger log = Logger.getLogger(RunnableWorker.class.getName());

    private Driver driver;
    private Map<String, Object> map;

    @Override
    public void run() {
        try (Session session = driver.session(SessionConfig.defaultConfig())) {
            int maxRetries = 5;
            int retries = 0;

            while (retries < maxRetries) {
                try {
                    session.writeTransaction(tx -> {
                        Query articleQuery = new Query(
                                "UNWIND $articles as article " +
                                        "WITH article " +
                                        "MERGE (a:Article {_id: article._id}) " +
                                        "ON CREATE SET a.title = article.title",
                                map
                        );

                        tx.run(articleQuery);

                        Query referencesQuery = new Query(
                                "UNWIND $articles as article " +
                                        "WITH article " +
                                        "UNWIND article.references AS reference " +
                                        "WITH article, reference " +
                                        "MATCH (a1:Article {_id: article._id}) " +
                                        "WITH a1, reference " +
                                        "MERGE (a2:Article {_id: reference}) " +
                                        "WITH a1, a2 " +
                                        "MERGE (a1)-[:CITES]->(a2)",
                                map);

                        tx.run(referencesQuery);

                        Query authorsQuery = new Query(
                                "UNWIND $articles as article " +
                                        "WITH article " +
                                        "UNWIND article.authors as author " +
                                        "WITH article, author " +
                                        "MATCH (b:Article {_id: article._id}) " +
                                        "WITH b, author " +
                                        "MERGE (a:Author {_id: author._id}) " +
                                        "ON CREATE SET a.name = author.name " +
                                        "WITH a, b " +
                                        "MERGE (a)-[:AUTHORED]->(b)",
                                map);
                        tx.run(authorsQuery);

                        return 1;
                    });
                    break;
                } catch (TransientException | DeadlockDetectedException e) {
                    log.severe("[Java] Error during transaction : " + e.getMessage());
                    retries++;
                    try {
                        Thread.sleep(1000 * 2); // 2 seconds
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }
}
