# AdvDaBa - Laboratory 2

**Student :** Dalia Maillefer

**Date :** December 14th, 2023

## Introduction

The goal of this laboratory is to process a 18Go JSON file and to create nodes and relationships in Neo4j, without scaling-up too much the environment loading the data and in a decent short amount of time.

## Content

The language used for the application is Java (version 11) and is also working with Maven, in order to work with dependencies / librairies. Therefore, you will need to run the command `mvn clean install` in order to get those dependencies.

Since the goal of this laboratory is to process with little memory, it is obvious that it will not be possible to keep 18Go in memory. Therefore, we need to work with streaming. This is possible with GSON

This program is also working by batches, you will be able to set the value in the `docker-compose.yaml` as an environment variable. Because of the restriction of the memory, we parse JSON objects into Article objects and insert them into the database.
When the number of Article has reached the value of the batch size, threads will take over and the list will be cleared in order to store the next articles.

In order to increase the performance, as briefly mentionned, threads allowed to parallelize the queries but also this come with issues, concurrency and deadlocks. 

we can use constraints to speed up the process, especially when IDs are supposed to be unique. This also allow to remove any duplicates while merging nodes :

```java
tx.run("CREATE CONSTRAINT FOR (a:Article) REQUIRE a._id IS UNIQUE");
tx.run("CREATE CONSTRAINT FOR (b:Author) REQUIRE b._id IS UNIQUE");
```

Source : [StackOverFlow](https://stackoverflow.com/questions/29657461/big-data-import-into-neo4j)

// TODO: COMPLETE THIS SECTION

## Results

{ "team": "MailleAdvDaBa", "N": 1000, "RAM": 3000, "seconds": 4 } (Batch = 100)

{ "team": "MailleAdvDaBa", "N": 10000, "RAM": 3000, "seconds": 11 } (Batch = 500)

{ "team": "MailleAdvDaBa", "N": 10000, "RAM": 3000, "seconds": 11 } (Batch = 1000)

{ "team": "MailleAdvDaBa", "N": 20000, "RAM": 3000, "seconds": 14 } (Batch = 2000)

{ "team": "MailleAdvDaBa", "N": 30000, "RAM": 3000, "seconds": 21 } (Batch = 3000)

{ "team": "MailleAdvDaBa", "N": 50000, "RAM": 3000, "seconds": 52 } (Batch = 5000)

{ "team": "MailleAdvDaBa", "N": 100000, "RAM": 3000, "seconds": 79 } (Batch = 10000)

{ "team": "MailleAdvDaBa", "N": 1000000, "RAM": 3000, "seconds": 867 } (Batch = 10000)


## Run with Docker

If you are using the image in Docker Hub, you can simply run the following command and go to the link `localhost:7474` for accessing the Neo4j browser.

```bash
docker compose up -d
```

If you want to build the image using the `Dockerfile` instead of the image from Docker Hub, simply replace `image: alicethunderwind/advdaba_app:latest` with `build: .` and run :

```bash
docker compose build
docker compose up -d
```

## Run with Kubernetes / Rancher

Before deploying to Rancher, we need to push our image related to the Java app to Docker Hub, so that

```bash
docker build . -t advdaba_app # build the image
docker tag advdaba_app:latest alicethunderwind/advdaba_app:latest # set a tag to the image
docker push alicethunderwind/advdaba_app:latest # publish the image to Docker Hub
```

In the root folder, you will find 2 files `service.yaml` and `pod.yaml`. There are 2 pods, one dedicated to Neo4j and the other to the Java app. There is 1 service allowing the

```bash
kubectl create -f service.yaml -n adv-da-ba23-1
kubectl create -f pod.yaml -n adv-da-ba23-1
```

In order to access to the browser of Neo4j, you will need to do a port-forward using this command :

```bash
kubectl port-forward neo4j 7474:7474 7687:7687 --address='0.0.0.0' -n adv-da-ba23-1
```

Then, you'll be able to connect to the browser using the following link `localhost:7474` and connect to the database with `neo4j` as user and `testtest` as password.