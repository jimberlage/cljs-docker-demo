# cljs-docker-demo

## Problem

We want to run 1..N docker containers from our local filesystem, and get a GUI display of their status.

## Dependencies

* [docker](https://www.docker.com/products/overview)
* Java
* [Boot](http://boot-clj.com/)

## Getting Started

To build and run the server:

```bash
# Build a JAR file containing the server
boot backend
# Run the server
(cd target && java -jar backend/project.jar)
```

To build the Clojurescript:

```bash
# In development
boot frontend-dev
```

OR

```bash
# In production
boot frontend-prod
```

## Overview

This app consists of a simple interface.  A user can specify an app that exists on their filesystem and run it in a docker container on a given port.

So if there is an app at `/Users/me/my-app`, and `/Users/me/my-app` contains a [Dockerfile](https://docs.docker.com/engine/reference/builder/) in its root, then the user can visit localhost:3000, and run the app on whatever port they choose by clicking through the interface.

There is a small server component, with three api endpoints:

* `/api/apps/build` (builds a docker image for the given app)
* `/api/apps/run` (runs a docker container for the given app)
* `/api/apps/stop` (stops a docker container for the given app)

The client interface is a Clojurescript + Reagent app, using those endpoints.
