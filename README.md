# SparkJava Web Crawler
 
A simple, asynchronous web crawler built with Java and the SparkJava micro-framework. It exposes a RESTful API to start, monitor, and retrieve the results of crawl jobs.

The application is designed to be thread-safe, handling concurrent requests and performing crawl operations in the background without blocking the main server thread.

## Features

*   **RESTful API**: Simple endpoints to create and query crawl jobs.
*   **Asynchronous Crawling**: Uses a cached thread pool to handle crawl jobs in the background.
*   **Real-time Status**: Each job has a status (`active`, `done`, `error`) that can be queried at any time.
*   **In-Memory Storage**: Utilizes a `ConcurrentHashMap` for thread-safe, in-memory storage of job data.
*   **Graceful Shutdown**: Implements a shutdown hook to ensure background tasks are terminated properly.
*   **Dependency Injection**: Clear separation of concerns with dependencies wired manually in the `Main` class.

## Technology Stack

*   **Language**: Java 17
*   **Framework**: SparkJava 2.9.4
*   **Build Tool**: Apache Maven
*   **JSON Serialization**: Google Gson 2.13.1
*   **Logging**: SLF4J 2.0.17
*   **Testing**: JUnit 5, Mockito, Hamcrest

## Prerequisites

*   JDK 17 or higher
*   Apache Maven 3.6.0 or higher
*   Docker (for containerized deployment)

## Getting Started

Follow these instructions to get the project up and running on your local machine.

1.  **Clone the repository:**
    ```sh
    git clone <your-repository-url>
    cd crawler-sparkjava
    ```

2.  **Build the project:**
    Use Maven to compile the source code and install dependencies.
    ```sh
    mvn clean install
    ```

3.  **Run the application:**
    The `exec-maven-plugin` is configured to run the main class.
    ```sh
    mvn exec:java
    ```
    The server will start on `http://localhost:8081` by default. You can set the `PORT` environment variable to use a different port.

## Running with Docker

The project includes a `Dockerfile` for easy containerization.

1.  **Build the Docker image:**
    From the project root directory, run:
    ```sh
    docker build -t crawler-sparkjava .
    ```

2.  **Run the Docker container:**
    ```sh
    docker run -p 8081:8081 crawler-sparkjava
    ```
    The application will be accessible at `http://localhost:8081`.

## API Endpoints

The API provides three endpoints to manage the crawler.

### 1. Create a Crawl Job

Initiates a new crawl job for a given keyword. The crawling process starts immediately in the background.

*   **Endpoint**: `POST /crawl`
*   **Request Body**: A JSON object with a `keyword`. The keyword must be between 4 and 32 characters.
    ```json
    {
      "keyword": "sparkjava"
    }
    ```
*   **Success Response (201 Created)**: Returns the initial state of the newly created job.
    ```json
    {
      "id": "a1b2c3d4",
      "status": "active",
      "urls": []
    }
    ```

### 2. Get Crawl Status

Retrieves the current status and results for a specific crawl job using its unique ID.

*   **Endpoint**: `GET /crawl/:id`
*   **URL Parameter**:
    *   `id` (string, 8 characters): The unique ID of the crawl job.
*   **Success Response (200 OK)**:
    ```json
    {
      "id": "a1b2c3d4",
      "status": "done",
      "urls": [
        "https://sparkjava.com/",
        "https://sparkjava.com/documentation"
      ]
    }
    ```
*   **Error Response (404 Not Found)**: If the ID does not exist.

### 3. List All Crawl Jobs

Returns a list of all crawl jobs that have been created.

*   **Endpoint**: `GET /crawl`
*   **Success Response (200 OK)**:
    ```json
    [
      {
        "id": "a1b2c3d4",
        "status": "done",
        "urls": ["..."]
      },
      {
        "id": "e5f6g7h8",
        "status": "active",
        "urls": []
      }
    ]
    ```

## Project Structure

*   `controller`: Handles HTTP requests, validates input, and orchestrates responses.
*   `dao`: Data Access Object for in-memory storage of `Crawler` entities.
*   `entity`: Domain model classes (e.g., `Crawler`, `Status`).
*   `handler`: Contains the core business logic for the crawling process.
*   `helper`: Utility classes, such as the `CrawlerMapper` for DTO conversion.
*   `route`: Defines the API routes and connects them to the controller methods.
*   `Main.java`: The application entry point, responsible for dependency injection and server setup.