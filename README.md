# AdvDaBa - Laboratory 2

**Student :** Dalia Maillefer

**Date :** December 14th, 2023

## Introduction

The goal of this laboratory is to process a 18Go JSON file and to create nodes and relationships in Neo4j, without scaling-up too much the environment loading the data and in a decent short amount of time.

## Content

The language used for the application is Java (version 11) and is also working with Maven, in order to work with dependencies / librairies.

Since the goal of this laboratory is to process with little memory, it is obvious that it will not be possible to keep 18Go in memory. Therefore, we need to work with Streaming.

This program is also working by batches, you will be able to set the value in the `docker-compose.yaml` as an environment variable

In order to increase the performance, we can use constraints to speed up the process, especially when IDs are supposed to be unique. This also allow to remove any duplicates while merging nodes :

```java
tx.run("CREATE CONSTRAINT FOR (a:Article) REQUIRE a._id IS UNIQUE");
tx.run("CREATE CONSTRAINT FOR (b:Author) REQUIRE b._id IS UNIQUE");
```

Source : [StackOverFlow](https://stackoverflow.com/questions/29657461/big-data-import-into-neo4j)

// TODO: COMPLETE THIS SECTION

## Results

Results -> { "team": "MailleAdvDaBa", "N": 10000, "RAM": 3000, "seconds": 11 } (Batch = 500)

Results -> { "team": "MailleAdvDaBa", "N": 10000, "RAM": 3000, "seconds": 12 } (Batch = 1000)

Results -> { "team": "MailleAdvDaBa", "N": 20000, "RAM": 3000, "seconds": 13 } (Batch = 2000)

Results -> { "team": "MailleAdvDaBa", "N": 30000, "RAM": 3000, "seconds": 15 } (Batch = 3000)

Results -> { "team": "MailleAdvDaBa", "N": 50000, "RAM": 3000, "seconds": 37 } (Batch = 5000)

Results -> { "team": "MailleAdvDaBa", "N": 100000, "RAM": 3000, "seconds": 74 } (Batch = 5000)

Results -> { "team": "MailleAdvDaBa", "N": 100000, "RAM": 3000, "seconds": 70 } (Batch = 10000)

Results -> { "team": "MailleAdvDaBa", "N": 500000, "RAM": 3000, "seconds": 355 } (Batch = 10000)

Results -> { "team": "MailleAdvDaBa", "N": 1000000, "RAM": 3000, "seconds": 823 } (Batch = 10000)

Results -> { "team": "MailleAdvDaBa", "N": 10000000, "RAM": 3000, "seconds": 5111 } (Batch = 10000)

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