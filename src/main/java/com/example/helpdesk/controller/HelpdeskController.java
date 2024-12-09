package com.example.helpdesk.controller;

import com.example.helpdesk.dto.UserRegistrationDTO;
import com.example.helpdesk.model.Event;
import com.example.helpdesk.model.User;
import com.example.helpdesk.repository.EventRepository;
import com.example.helpdesk.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    public String register(Model model) {
        model.addAttribute("userDto", new UserRegistrationDTO());
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

    @GetMapping("/userEvents")
    public String userEvents(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            List<Event> userEvents = eventRepository.findByUser(user);
            model.addAttribute("events", userEvents);
        }
        return "userEvents";
    }

    @GetMapping("/editEvent/{id}")
    public String editEvent(@PathVariable Long id, Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            Event event = eventRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

            if (event.getUser().getId().equals(user.getId())) {
                model.addAttribute("event", event);
                model.addAttribute("formattedDate", event.getDetectionDate().format(DateTimeFormatter.ISO_DATE));
                return "editEvent";
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to edit this event");
    }
    @PostMapping("/editEvent/{id}")
    public String updateEvent(@PathVariable("id") Long id,
                              @ModelAttribute Event updatedEvent,
                              @RequestParam("detectionDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate detectionDate,
                              Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            Event event = eventRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

            if (event.getUser().getId().equals(user.getId())) {
                event.setCategory(updatedEvent.getCategory());
                event.setTitle(updatedEvent.getTitle());
                event.setDescription(updatedEvent.getDescription());
                event.setEmergency(updatedEvent.isEmergency());
                event.setDetectionDate(detectionDate);
                event.setDowntime(updatedEvent.getDowntime());

                eventRepository.save(event);
                return "redirect:/userEvents";
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to edit this event");
    }

    @GetMapping("/viewUsers")
    public String users(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "viewUsers";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("userDto") UserRegistrationDTO userDto,
                           BindingResult result,
                           Model model) {
        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.userDto", "Passwords do not match");
        }

        if (userRepository.findByUsername(userDto.getUsername()).isPresent()) {
            result.rejectValue("username", "error.userDto", "Username already exists");
        }
        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            result.rejectValue("email", "error.userDto", "Email already exists");
        }
        if (result.hasErrors()) {
            return "register";
        }

        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setUsername(userDto.getUsername());
        user.setName(userDto.getName());
        user.setSurname(userDto.getSurname());
        user.setRole("user");
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
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
