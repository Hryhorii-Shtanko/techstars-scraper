CREATE TABLE IF NOT EXISTS jobs (
                                    id BIGSERIAL PRIMARY KEY,
                                    job_page_url VARCHAR(255) NOT NULL UNIQUE,
    position_name VARCHAR(255),
    organization_url VARCHAR(255),
    logo_url VARCHAR(255),
    organization_title VARCHAR(255),
    labor_function VARCHAR(255),
    location VARCHAR(255),
    posted_date_timestamp BIGINT NOT NULL,
    description_html TEXT
    );

CREATE TABLE IF NOT EXISTS job_tags (
                                        job_id BIGINT NOT NULL,
                                        tag VARCHAR(255),
    CONSTRAINT fk_job FOREIGN KEY(job_id) REFERENCES jobs(id)
    );