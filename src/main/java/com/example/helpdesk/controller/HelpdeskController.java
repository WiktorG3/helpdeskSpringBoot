package com.example.helpdesk.controller;

import com.example.helpdesk.model.Event;
import com.example.helpdesk.model.User;
import com.example.helpdesk.repository.EventRepository;
import com.example.helpdesk.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class HelpdeskController {
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;


    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/addEvent")
    public String addEvent() {
        return "addEvent";
    }

    @GetMapping("/viewEvents")
    public String viewEvents(Model model) {
        List<Event> events = eventRepository.findAll();
        model.addAttribute("events", events);
        return "viewEvents";
    }

    @GetMapping("/edit_event")
    public String editEvent() {
        return "editEvent";
    }

    @GetMapping("/viewUsers")
    public String users(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "viewUsers";
    }

    @PostMapping("/register")
    public String register(@RequestParam String email, @RequestParam String username, @RequestParam String name, @RequestParam String surname, @RequestParam String password, Model model) {
        if(userRepository.findByUsername(username).isPresent()){
            model.addAttribute("message", "Username already exists");
            return "register";
        }
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setName(name);
        user.setSurname(surname);
        user.setRole("user");
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        model.addAttribute("message", "User registered successfully");
        return "redirect:/login";
    }

    @PostMapping("/addEvent")
    public String addEvent(@RequestParam String category, @RequestParam String title, @RequestParam String description,
                           @RequestParam(required = false) boolean emergency,
                           @RequestParam String detectionDate, @RequestParam int downtime, Model model
                           ){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        Optional<User> optionalUser = userRepository.findByUsername(currentUsername);

        if(optionalUser.isEmpty()) {
            model.addAttribute("message", "User not found");
            return "redirect:/dashboard";
        }

        User currentUser = optionalUser.get();

        Event event = new Event();
        event.setCategory(category);
        event.setTitle(title);
        event.setDescription(description);
        event.setEmergency(emergency);
        LocalDate date = LocalDate.parse(detectionDate);
        event.setDetectionDate(date);
        event.setDowntime(downtime);
        event.setUser(currentUser);
        event.setCompleted(false);

        eventRepository.save(event);
        model.addAttribute("message", "Event added successfully");
        return "redirect:/viewEvents";
    }
}
