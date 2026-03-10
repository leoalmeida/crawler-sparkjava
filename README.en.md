# crawler-sparkjava

[Português](README.md) | [English](README.en.md)

![Java](https://img.shields.io/badge/java-17-orange)
![SparkJava](https://img.shields.io/badge/sparkjava-2.9.4-red)
![Status](https://img.shields.io/badge/status-active-brightgreen)

Asynchronous web crawler built with Java + SparkJava, exposing a REST API to create, monitor, and list crawl jobs.

## Table of Contents

- [Overview](#overview)
- [Requirements](#requirements)
- [Configuration](#configuration)
- [How to Run](#how-to-run)
- [API](#api)
- [Tests and Quality](#tests-and-quality)
- [Docker](#docker)
- [Structure](#structure)
- [Troubleshooting](#troubleshooting)

## Overview

- REST API endpoints under `/crawl`
- background processing using `ExecutorService`
- thread-safe in-memory storage (`ConcurrentHashMap`)
- graceful shutdown with JVM shutdown hook

## Requirements

- JDK 17+
- Maven 3.6+
- Docker (optional)

## Configuration

Runtime environment variables:

- `BASE_URL`: target base URL to crawl (required)
- `PORT`: API HTTP port (optional, default `8081`)

PowerShell example:

```powershell
$env:BASE_URL="https://example.com"
$env:PORT="8081"
```

## How to Run

```powershell
Set-Location "c:\Users\leo_a\projetos\crawler-sparkjava"
mvn clean install
mvn exec:java
```

API available at `http://localhost:8081`.

## API

### `POST /crawl`

Creates a new asynchronous crawl job.

Request:

```json
{
  "keyword": "sparkjava"
}
```

Response `201`:

```json
{
  "id": "a1b2c3d4"
}
```

### `GET /crawl/:id`

Returns status and discovered URLs for one job.

### `GET /crawl`

Lists all submitted jobs.

## Tests and Quality

Run all tests:

```powershell
mvn test
```

Run full validation pipeline (tests + static analysis):

```powershell
mvn verify
```

Format code:

```powershell
mvn spotless:apply
```

## Docker

Build image:

```powershell
docker build -t crawler-sparkjava .
```

Run container:

```powershell
docker run --rm -p 8081:8081 -e BASE_URL="https://example.com" crawler-sparkjava
```

## Structure

```text
crawler-sparkjava/
  src/main/java/space/lasf/sparkjava/
    controller/
    dao/
    dto/
    entity/
    exception/
    handler/
    helper/
    route/
    Main.java
  src/test/java/space/lasf/sparkjava/
  pom.xml
  Dockerfile
```

## Troubleshooting

- `BASE_URL environment variable not set`: define `BASE_URL` before startup.
- Port already in use: set a different `PORT`.
- Integration test instability: run `mvn clean test` to clear previous state.
