# AdvDaBa - Laboratory 2

**Student :** Dalia Maillefer

**Date :** December 14th, 2023

## Introduction

The goal of this laboratory is to process a JSON file of 18Go and create nodes and relationships in Neo4j

## Content

The language used for the application is Java (version 11) and is also working with Maven, in order to work with dependencies / librairies.

Since the goal of this laboratory is to process with little memory, it is obvious that it will not be possible to keep 18Go in memory. Therefore, we need to work with Streaming.

This program is also working by batches, you will be able to set the value in the `docker-compose.yaml` as an environment variable

## Run with Docker

```
docker compose up
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