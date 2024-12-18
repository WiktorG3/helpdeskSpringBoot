package com.example.helpdesk.repository;

import com.example.helpdesk.model.Event;
import com.example.helpdesk.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByUser(User user);

    @Query("SELECT DISTINCT e FROM Event e LEFT JOIN FETCH e.comments c LEFT JOIN FETCH c.user")
    List<Event> findAllWithComments();
}

