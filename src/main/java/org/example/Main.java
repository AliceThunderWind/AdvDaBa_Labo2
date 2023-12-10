package org.example;

import org.neo4j.driver.Driver;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Main {

    private static final Logger log = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws InterruptedException {

        // STEP 1 : Connect to Neo4J and try connection
        log.info("[Neo4j] Connecting to database");

        String neo4jUrlEnv = System.getenv("NEO4J_IP");
        final String NEO4J_URL = (neo4jUrlEnv != null) ? neo4jUrlEnv : "localhost";
        Driver driver = new Neo4JDriver("bolt://" + NEO4J_URL + ":7687", "neo4j", "testtest").getDriver();

        // Check if database is up and running before continuing
        boolean isConnected = false;
        while (!isConnected) {
            try {
                Thread.sleep(3000);
                driver.verifyConnectivity();
                isConnected = true;
            } catch (Exception e) {
                log.severe("[Neo4j] Error connecting to the database: " + e.getMessage());
            }
        }
        log.info("[Neo4j] Successfully connected to database at " + "bolt://" + NEO4J_URL + ":7687");

        // STEP 2 : Get SYSTEM environments
        String maxNodeEnv = System.getenv("MAX_NODE");
        int MAX_NODE = (maxNodeEnv != null) ? Integer.parseInt(maxNodeEnv) : 10000;

        String batchSizeEnv = System.getenv("BATCH_SIZE");
        int BATCH_SIZE = (batchSizeEnv != null) ? Integer.parseInt(batchSizeEnv) : 1000;

        log.info("[Java] Parameters provided : Batch size is " + BATCH_SIZE + " and max node is " + MAX_NODE);

        // STEP 3 : Insert all nodes into database
        Processor processor = new Processor(BATCH_SIZE, MAX_NODE, driver);
        processor.run();

        log.info("[Java] Insertion to database done");

        driver.close();

        log.info("[Java] Program finished");
        log.info("[Java] Now sleeping endlessly...");

        while(true) {
            TimeUnit.SECONDS.sleep(10);
        }
    }
}
