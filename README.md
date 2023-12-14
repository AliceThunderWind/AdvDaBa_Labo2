# AdvDaBa - Laboratory 2

**Student :** Dalia Maillefer

**Date :** December 14th, 2023

## Table of content

* [Introduction](#introduction)
* [Content](#content)
* [Results](#results)
* [How to use the program ?](#how-to-use-the-program-)
    + [Run the Java application](#run-the-java-application)
    + [Run with Docker](#run-with-docker)
    + [Run with Kubernetes / Rancher](#run-with-kubernetes-rancher)

## Introduction

The goal of this laboratory is to process a 18Go JSON file and to create nodes and relationships in Neo4j, without scaling-up too much the environment loading the data and in a decent short amount of time.

## Content

The language used for the application is Java (version 11) and is also working with Maven, in order to work with dependencies / librairies. Therefore, you will need to run the command `mvn clean install` in order to get those dependencies.

Since the goal of this laboratory is to process with little memory, it is obvious that it will not be possible to keep 18Go in memory. Therefore, we need to work with streaming. This is possible with GSON where JSONReader

Moreover, this program should be able to work on a Kubernetes environment, that means without any volume. In the first approach, the `dblpv13.json` was part of a volume within the Java container and read as InputStream. This was replaced by an URL as follow :

```java
URL jsonUrl = new URL("http://vmrum.isc.heia-fr.ch/dblpv13.json");
```

This program is also working by batches, you will be able to set the value in the `docker-compose.yaml` as an environment variable. Because of the restriction of the memory, we parse one by one JSON objects into Article objects and insert them into a list of size `BATCH_SIZE`. When the number of Article has reached the maximum size of the list, threads will take over and the list will be cleared in order to store the next articles.

Regarding the parsing, since it was asked in the laboratory to create Article nodes with `_id` and `title` attributes and Author nodes with `_id` and `name`, only those fields were used in the `ArticleFactory` class and any malformed field (like `NumberInt`) was ignored. However, if we were to add the other fields, we could easily add them in the switch case and perform any. Otherwise, if the file was correctly made, we could use the `Gson.fromJson()` method that replace the whole factory with one single line of code.

The main goal of the process was also to process the whole file while reducing the time complexity. That means that we create nodes and relationship in one go. That mostly concerns the relationship `[:CITES]` where references contain the ID of Article. Even if the article doesn't exist, we still create the node with only the ID and later, we add the title using `ON UPDATE SET article.name = 'name'`. That prevents to create all Article nodes and then all relashionship associated.

In order to increase the performance, as briefly mentionned, threads allowed to parallelize the queries but also come with issues, such as concurrency and deadlocks. In order to solve that problem, a retryer has been implemented. If a deadlock occurs, the thread will wait for a few second before retrying the transaction. A limit of retry was also set.

We can use also constraints to speed up the process because they also act as indexes. IDs are supposed to be unique, therefore these constraints prevent any duplicates while merging (creating) nodes :

```java
tx.run("CREATE CONSTRAINT FOR (a:Article) REQUIRE a._id IS UNIQUE");
tx.run("CREATE CONSTRAINT FOR (b:Author) REQUIRE b._id IS UNIQUE");
```

Source : [StackOverFlow](https://stackoverflow.com/questions/29657461/big-data-import-into-neo4j)


## Results

{ "team": "MailleAdvDaBa", "N": 1000, "RAM": 3000, "seconds": 4 } (Batch = 100)

{ "team": "MailleAdvDaBa", "N": 10000, "RAM": 3000, "seconds": 11 } (Batch = 500)

{ "team": "MailleAdvDaBa", "N": 10000, "RAM": 3000, "seconds": 11 } (Batch = 1000)

{ "team": "MailleAdvDaBa", "N": 20000, "RAM": 3000, "seconds": 14 } (Batch = 2000)

{ "team": "MailleAdvDaBa", "N": 30000, "RAM": 3000, "seconds": 21 } (Batch = 3000)

{ "team": "MailleAdvDaBa", "N": 50000, "RAM": 3000, "seconds": 52 } (Batch = 5000)

{ "team": "MailleAdvDaBa", "N": 100000, "RAM": 3000, "seconds": 79 } (Batch = 10000)

{ "team": "MailleAdvDaBa", "N": 1000000, "RAM": 3000, "seconds": 867 } (Batch = 10000)

{ "team": "MailleAdvDaBa", "N": 6000000, "RAM": 3000, "seconds": 5526 } (Batch = 20000)

{ "team": "MailleAdvDaBa", "N": 6000000, "RAM": 3000, "seconds": 5827 } (Batch = 5000)

## How to use the program ?

### Run the Java application

First of all, you will need Java 11 and Maven in order to run this project. Then use the command `mvn clean install` to get all dependencies from the `pom.xml` file. Then you can use your favorite IDE and run the program.

Prior to that, you will need to launch the Neo4J container, otherwise the application will try endlessly to connect to the database.

### Run with Docker

If you are using the image in Docker Hub, you can simply run the following command and go to the link `localhost:7474` for accessing the Neo4j browser.

```bash
docker compose up -d
```

If you want to build the image using the `Dockerfile` instead of the image from Docker Hub, simply replace `image: alicethunderwind/advdaba_app:latest` with `build: .` and run :

```bash
docker compose build
docker compose up -d
```

### Run with Kubernetes / Rancher

Before deploying to Rancher, we need to push our image related to the Java app to Docker Hub :

```bash
docker build . -t advdaba_app # build the image
docker tag advdaba_app:latest alicethunderwind/advdaba_app:latest # set a tag to the image
docker push alicethunderwind/advdaba_app:latest # publish the image to Docker Hub
```

You can replace the name of the image by the name of your account on Docker Hub.

In the root folder, you will find 2 files `service.yaml` and `pod.yaml`. There are 2 pods, one dedicated to Neo4j and the other to the Java app. There is 1 service allowing the application to connect to the database within the cluster.

```bash
kubectl create -f service.yaml -n adv-da-ba23-1
kubectl create -f pod.yaml -n adv-da-ba23-1
```

In order to access to the browser of Neo4j, you will need to do a port-forward using this command :

```bash
kubectl port-forward neo4j 7474:7474 7687:7687 --address='0.0.0.0' -n adv-da-ba23-1
```

Then, you'll be able to connect to the browser using the following link `localhost:7474` and connect to the database with `neo4j` as user and `testtest` as password.

A `deploy.yaml` is also present in this repository and you can use it with the command :

```bash
kubectl create -f deploy.yaml -n adv-da-ba23-1
```

