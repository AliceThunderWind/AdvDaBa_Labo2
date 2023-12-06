package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.example.model.Article;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ArticleProcessor {

    private static final Logger log = Logger.getLogger(ArticleProcessor.class.getName());

    private final int BATCH_SIZE;
    private final int MAX_NODE;
    private final Driver driver;

    public ArticleProcessor(int batchSize, int maxNode, Driver driver) {
        this.BATCH_SIZE = batchSize;
        this.MAX_NODE = maxNode;
        this.driver = driver;
    }

    public void processArticles() throws InterruptedException {

        try(Session session = driver.session(SessionConfig.defaultConfig())) {

            // https://stackoverflow.com/questions/29657461/big-data-import-into-neo4j
            session.writeTransaction(tx -> {
                try {
                    tx.run("CREATE CONSTRAINT FOR (a:Article) REQUIRE a.id IS UNIQUE");
                    tx.run("CREATE CONSTRAINT FOR (b:Author) REQUIRE b.id IS UNIQUE");
                } catch (Exception e) {
                    tx.rollback();
                }
                return 1;
            });
        }

        log.info("[Java] Start timer");
        long startTime = System.nanoTime();
        int totalArticles = 0;

        try {
            // Open a connection to the URL
            URL jsonUrl = new URL("http://vmrum.isc.heia-fr.ch/dblpv13.json");
            //URL jsonUrl = new URL("http://vmrum.isc.heia-fr.ch/biggertest.json"); // Uncomment this line to use the lighter JSON
            URLConnection connection = jsonUrl.openConnection();
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            try (JsonReader jsonReader = new JsonReader(new InputStreamReader(connection.getInputStream() /*new FileInputStream(FILENAME)*/, StandardCharsets.UTF_8))) {
                Gson gson = new GsonBuilder().create();
                jsonReader.beginArray();

                List<Article> articlesBatch = new ArrayList<>();
                List<Thread> threads = new LinkedList<>();

                while (jsonReader.hasNext() && totalArticles < MAX_NODE) {
                    Article article = gson.fromJson(jsonReader, Article.class);

                    articlesBatch.add(article);
                    ++totalArticles;

                    if (articlesBatch.size() == BATCH_SIZE) {
                        Thread t = new Thread(new RunnableWorker(driver, processBatch(articlesBatch)));
                        t.start();
                        threads.add(t);
                        articlesBatch.clear();
                    }
                }

                if (!articlesBatch.isEmpty()) {
                    Thread t = new Thread(new RunnableWorker(driver, processBatch(articlesBatch)));
                    t.start();
                    threads.add(t);
                }

                for(Thread t : threads){
                    t.join();
                }

            } catch (IOException | InterruptedException e) {
                log.severe(e.getMessage());
            } finally {
                long endTime = System.nanoTime();
                long elapsedTime = (endTime - startTime);
                long convert = TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
                log.info("[Java] Elapsed time: " + convert + " seconds");
            }
        } catch (IOException e) {
            log.severe(e.getMessage());
        }
        log.info("[Java] Now sleeping endlessly...");

        while(true) {
            TimeUnit.SECONDS.sleep(10);
        }
    }

    private Map<String, Object> processBatch(List<Article> batch) {
        List<Map<String, Object>> articlesMap = batch.stream()
                .map(Article::toMap)
                .collect(Collectors.toList());

        Map<String, Object> map = new HashMap<>();
        map.put("articles", articlesMap);

        return new HashMap<>(map);
    }

}
