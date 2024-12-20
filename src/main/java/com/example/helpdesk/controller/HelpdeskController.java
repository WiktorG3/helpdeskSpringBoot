package com.example.helpdesk.controller;

import com.example.helpdesk.dto.UserRegistrationDTO;
import com.example.helpdesk.model.Comment;
import com.example.helpdesk.model.Event;
import com.example.helpdesk.model.User;
import com.example.helpdesk.repository.EventRepository;
import com.example.helpdesk.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        List<Event> events = eventRepository.findAllWithComments();
        model.addAttribute("events", events);
        return "viewEvents";
    }

    @GetMapping("/viewComment/{eventId}")
    @ResponseBody
    public String viewComment(@PathVariable Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getStatus() == Event.EventStatus.RESOLVED && !event.getComments().isEmpty()) {
            return event.getComments().get(event.getComments().size() - 1).getContent();
        }

        return "No comment available";
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


        eventRepository.save(event);
        model.addAttribute("message", "Event added successfully");
        return "redirect:/viewEvents";
    }

    @GetMapping("/editUsers")
    public String editUsers(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "editUsers";
    }

    @GetMapping("/editUsers/{username}")
    public String editUser(@PathVariable String username, Model model) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        model.addAttribute("user", user);
        return "editUser";
    }

    @PostMapping("/editUsers")
    public String updateUser(@RequestParam String originalUsername,
                             @RequestParam String username,
                             @RequestParam String name,
                             @RequestParam String surname,
                             @RequestParam String email,
                             @RequestParam String role,
                             @RequestParam(required = false) String newPassword,
                             RedirectAttributes redirectAttributes) {
        User user = userRepository.findByUsername(originalUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        user.setUsername(username);
        user.setName(name);
        user.setSurname(surname);
        user.setEmail(email);
        user.setRole(role);

        if (newPassword != null && !newPassword.isEmpty()) {
            if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "New password must be at least 6 characters long");
                return "redirect:/editUsers/" + originalUsername;
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "User updated successfully");
        return "redirect:/editUsers";
    }


    @GetMapping("/resolveEvents")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String resolveEvents(Model model,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(defaultValue = "id") String sortBy,
                                @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<Event> eventsPage = eventRepository.findAll(pageRequest);

        model.addAttribute("eventsPage", eventsPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", eventsPage.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        return "resolveEvents";
    }

    @PostMapping("/resolveEvents/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String updateEventStatus(@PathVariable Long id,
                                    @RequestParam Event.EventStatus status,
                                    @RequestParam String comment,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            Event event = eventRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

            event.setStatus(status);

            User admin = userRepository.findByUsername(authentication.getName())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));

            Comment newComment = new Comment(comment, event, admin);
            event.addComment(newComment);

            eventRepository.save(event);
            redirectAttributes.addFlashAttribute("success", "Event status updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update event status: " + e.getMessage());
        }
        return "redirect:/resolveEvents";
    }


}