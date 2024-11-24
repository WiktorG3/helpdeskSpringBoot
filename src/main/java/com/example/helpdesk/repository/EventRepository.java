package com.example.helpdesk.repository;

import com.example.helpdesk.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
