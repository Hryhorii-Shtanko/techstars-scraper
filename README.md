# Techstars Job Scraper

This is a Spring Boot application designed to scrape job listings from `jobs.techstars.com`. It filters jobs by a specific job function (e.g., "Software Engineering"), parses the data, and stores it in a PostgreSQL database.

## Features

- Scrapes jobs by a given job function.
- Extracts key details: position, company, location, date, description, and tags.
- Uses a multi-threaded approach for faster scraping.
- Stores data in a PostgreSQL database using Spring Data JPA.
- Can be easily run using Docker Compose.