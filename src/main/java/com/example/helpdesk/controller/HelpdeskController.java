package com.example.helpdesk.controller;

import com.example.helpdesk.model.User;
import com.example.helpdesk.repository.EventRepository;
import com.example.helpdesk.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.List;

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

    @GetMapping("/view_events")
    public String viewEvents() {
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
    public String register(@RequestParam String email, @RequestParam String username, @RequestParam String password, Model model) {
        if(userRepository.findByUsername(username).isPresent()){
            model.addAttribute("message", "Username already exists");
            return "register";
        }
        User user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
        model.addAttribute("message", "User registered successfully");
        return "redirect:/login";
    }
}
