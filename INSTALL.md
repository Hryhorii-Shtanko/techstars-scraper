# Installation and Setup Guide

There are two ways to run this project: using Docker (recommended) or manually.

## 1. Running with Docker (Recommended)

This is the easiest way to get started.

### Prerequisites

- [Docker](https://docs.docker.com/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/install/)

### Steps

1.  **Clone the repository:**
    ```bash
    git clone <your-repo-url>
    cd techstars-scraper
    ```

2.  **Build and run the application:**
    ```bash
    docker-compose up --build
    ```
    This command will:
    - Build the Java application using a multi-stage Dockerfile.
    - Start a PostgreSQL container.
    - Start the scraper application container.
    - The application will automatically start scraping "Software Engineering" jobs.

## 2. Running Manually

### Prerequisites

- Java 17 or later
- Maven
- PostgreSQL server running locally

### Steps

1.  **Clone the repository:**
    ```bash
    git clone <your-repo-url>
    cd techstars-scraper
    ```
2.  **Create a PostgreSQL database:**
    - Create a user (e.g., `postgres`).
    - Create a database named `techstars_jobs`.

3.  **Configure the database connection:**
    - Open `src/main/resources/application.properties`.
    - Update `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password` with your PostgreSQL credentials.

4.  **Build and run the application:**
    ```bash
    mvn spring-boot:run
    ```
    The application will start and begin the scraping process.