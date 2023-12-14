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
            int maxRetries = 30;
            int retries = 1;

            while (retries <= maxRetries) {
                try {
                    session.writeTransaction(tx -> {

                        // Doing in three separate queries
                        // Performance tests : { "team": "MailleAdvDaBa", "N": 10000, "RAM": 3000, "seconds": 11 } (Batch = 1000)
                        tx.run(new Query(
                                "UNWIND $articles as article " +
                                        "WITH article " +
                                        "MERGE (arti:Article {_id: article._id}) " +
                                        "ON CREATE SET arti.title = article.title " +
                                        "ON MATCH SET arti.title = COALESCE(article.title, arti.title)",
                                map
                        ));

                        tx.run(new Query(
                                "UNWIND $articles as article " +
                                        "WITH article " +
                                        "UNWIND article.authors as author " +
                                        "WITH article, author " +
                                        "MATCH (arti:Article {_id: article._id}) " +
                                        "WITH arti, author " +
                                        "MERGE (auth:Author {_id: author._id}) " +
                                        "ON CREATE SET auth.name = author.name " +
                                        "WITH auth, arti " +
                                        "MERGE (auth)-[:AUTHORED]->(arti)",
                                map));

                        tx.run(new Query(
                                "UNWIND $articles as article " +
                                        "WITH article " +
                                        "UNWIND article.references AS reference " +
                                        "WITH article, reference " +
                                        "MATCH (arti1:Article {_id: article._id}) " +
                                        "WITH arti1, reference " +
                                        "MERGE (arti2:Article {_id: reference}) " +
                                        "WITH arti1, arti2 " +
                                        "MERGE (arti1)-[:CITES]->(arti2)",
                                map)
                        );

                        // Doing in one query seems slower
                        // Performance tests showed : { "team": "MailleAdvDaBa", "N": 10000, "RAM": 3000, "seconds": 60 } (Batch = 1000)
                        /*
                        tx.run(new Query(
                                "UNWIND $articles as article " +
                                        "WITH article " +
                                        "MERGE (arti:Article {_id: article._id}) " +
                                        "ON CREATE SET arti.title = article.title " +
                                        "ON MATCH SET arti.title = COALESCE(article.title, arti.title) " +
                                        "WITH arti, article.authors as authors, article.references as references " +
                                        "UNWIND authors as author " +
                                        "MERGE (auth:Author {_id: author._id}) " +
                                        "ON CREATE SET auth.name = author.name " +
                                        "ON MATCH SET auth.name = author.name " +
                                        "MERGE (auth)-[:AUTHORED]->(arti) " +
                                        "WITH arti, references " +
                                        "UNWIND references AS reference " +
                                        "MERGE (arti2:Article {_id: reference}) " +
                                        "MERGE (arti)-[:CITES]->(arti2)",
                                map
                        ));*/

                        // Based from https://neo4j.com/developer/graph-data-science/link-prediction/graph-data-science-library/#citation-graph
                        // but seems slower than queries above
                        /*
                        tx.run(new Query(
                                "UNWIND $articles AS article " +
                                        "MERGE (a:Article {_id: article._id}) " +
                                        "ON CREATE SET a.title = article.title " +
                                        "ON MATCH SET a.title = COALESCE(article.title, a.title) " +
                                        "WITH a, article.authors as authors, article.references AS references " +
                                        "FOREACH(author in authors | " +
                                        "     MERGE (b:Author {_id:author._id}) " +
                                        "     ON CREATE SET b.name = author.name " +
                                        "     MERGE (a)-[:AUTHORED]->(b)) " +
                                        "FOREACH(reference in references | " +
                                        "     MERGE (b:Article {_id: reference}) " +
                                        "     MERGE (a)-[:CITED]->(b))",
                                map
                        ));*/

                        return 1;
                    });
                    break;
                } catch (TransientException | DeadlockDetectedException e) {
                    log.severe("[Java] Error during transaction : " + e.getMessage());
                    log.info("Retry number " + retries);
                    retries++;
                    try {
                        Thread.sleep(1000 * 10); // 10 seconds
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        }
    }
}
