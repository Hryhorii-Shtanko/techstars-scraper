package com.github.hryhoriishtanko.techstarsscraper.repository;

import com.github.hryhoriishtanko.techstarsscraper.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    // This method allows checking if a job with a specific URL already exists
    Optional<Job> findByJobPageUrl(String jobPageUrl);
}