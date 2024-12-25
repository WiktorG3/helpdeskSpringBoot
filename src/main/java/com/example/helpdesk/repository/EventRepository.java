package com.example.helpdesk.repository;

import com.example.helpdesk.model.Event;
import com.example.helpdesk.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    long countByEmergencyTrue();
    long countByStatus(Event.EventStatus status);
    Page<Event> findByUser(User user, Pageable pageable);
    List<Event> findByDetectionDateBetween(LocalDate fromDate, LocalDate toDate);
}

