package com.example.helpdesk.repository;

import com.example.helpdesk.model.Event;
import com.example.helpdesk.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByUser(User user);
}
