package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Processor {

    private static final Logger log = Logger.getLogger(Processor.class.getName());

    private final int BATCH_SIZE;
    private final int MAX_NODE;
    private final Driver driver;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public Processor(int batchSize, int maxNode, Driver driver) {
        this.BATCH_SIZE = batchSize;
        this.MAX_NODE = maxNode;
        this.driver = driver;
    }

    public void run() throws InterruptedException {

        try(Session session = driver.session(SessionConfig.defaultConfig())) {

            // https://stackoverflow.com/questions/29657461/big-data-import-into-neo4j
            session.writeTransaction(tx -> {
                try {
                    tx.run("CREATE CONSTRAINT FOR (a:Article) REQUIRE a._id IS UNIQUE");
                    tx.run("CREATE CONSTRAINT FOR (b:Author) REQUIRE b._id IS UNIQUE");
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
            try (JsonReader jsonReader = new JsonReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                jsonReader.setLenient(true);
                Gson gson = new GsonBuilder().create();
                jsonReader.beginArray();

                List<Article> articlesBatch = new ArrayList<>();

                while (jsonReader.hasNext() && totalArticles < MAX_NODE) {
                    try {
                        Article article = gson.fromJson(jsonReader, Article.class);
                        //Article article = ArticleFactory.createArticle(jsonReader); // Uncomment to use the factory (needed for NumberInt parsing)

                        articlesBatch.add(article);
                        ++totalArticles;

                        if (articlesBatch.size() == BATCH_SIZE) {
                            executorService.submit(new RunnableWorker(driver, convertListToMap(articlesBatch)));
                            articlesBatch.clear();
                        }
                    } catch (JsonSyntaxException e) {
                        log.severe("[Java] Error parsing JSON :" + e.getMessage());
                        jsonReader.skipValue();
                    } catch (Exception e) {
                        jsonReader.skipValue();
                        log.severe("[Java] Error occurred :" + e.getMessage());
                    }
                }

                // In case, there are Articles left in the list
                if (!articlesBatch.isEmpty()) {
                    executorService.submit(new RunnableWorker(driver, convertListToMap(articlesBatch)));
                }

            } catch (IOException e) {
                log.severe("[Java] Error occured with JSONReader: " + e.getMessage());
            } finally {
                executorService.shutdown();
                executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                long endTime = System.nanoTime();
                long elapsedTime = (endTime - startTime);
                long elapsedTimeInSeconds = TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
                String ramEnv = System.getenv("RAM");
                int RAM = (ramEnv != null) ? Integer.parseInt(ramEnv) : -1;
                log.info("[Java] { \"team\": \"MailleAdvDaBa\", \"N\": " + MAX_NODE +
                        ", \"RAM\": " + RAM + ", \"seconds\": " + elapsedTimeInSeconds + " }");
                log.info("[Java] Created " + totalArticles + " articles nodes");
            }
        } catch (IOException e) {
            log.severe(e.getMessage());
        }

    }

    private Map<String, Object> convertListToMap(List<Article> batch) {
        List<Map<String, Object>> articlesMap = batch.stream()
                .map(Article::toMap)
                .collect(Collectors.toList());

        Map<String, Object> map = new HashMap<>();
        map.put("articles", articlesMap);

        return new HashMap<>(map);
    }

}
