package com.example.helpdesk.controller;

import com.example.helpdesk.dto.UserRegistrationDTO;
import com.example.helpdesk.model.Comment;
import com.example.helpdesk.model.Event;
import com.example.helpdesk.model.User;
import com.example.helpdesk.repository.EventRepository;
import com.example.helpdesk.repository.UserRepository;
import com.example.helpdesk.service.PdfGeneratorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import java.io.ByteArrayInputStream;
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

    @Autowired
    private PdfGeneratorService pdfGeneratorService;

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("userDto", new UserRegistrationDTO());
        return "register";
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
        user.setRole("ROLE_USER");
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        userRepository.save(user);

        model.addAttribute("message", "User registered successfully");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long emergencyCount = eventRepository.countByEmergencyTrue();
        long openCount = eventRepository.countByStatus(Event.EventStatus.OPEN);
        long inProgressCount = eventRepository.countByStatus(Event.EventStatus.IN_PROGRESS);
        long resolvedCount = eventRepository.countByStatus(Event.EventStatus.RESOLVED);
        long closedCount = eventRepository.countByStatus(Event.EventStatus.CLOSED);
        long reopenedCount = eventRepository.countByStatus(Event.EventStatus.REOPENED);

        model.addAttribute("emergencyCount", emergencyCount);
        model.addAttribute("openCount", openCount);
        model.addAttribute("inProgressCount", inProgressCount);
        model.addAttribute("resolvedCount", resolvedCount);
        model.addAttribute("closedCount", closedCount);
        model.addAttribute("reopenedCount", reopenedCount);

        return "dashboard";
    }

    @GetMapping("/addEvent")
    public String addEvent() {
        return "addEvent";
    }

    @GetMapping("/viewEvents")
    public String viewEvents(Model model,
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
    public String userEvents(Model model,
                             Authentication authentication,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(defaultValue = "id") String sortBy,
                             @RequestParam(defaultValue = "asc") String sortDir) {

        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
            PageRequest pageRequest = PageRequest.of(page, size, sort);

            Page<Event> eventsPage = eventRepository.findByUser(user, pageRequest);

            model.addAttribute("eventsPage", eventsPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", eventsPage.getTotalPages());
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("sortDir", sortDir);
        }

        return "userEvents";
    }

    @PostMapping("/userEvents/{id}")
    public String updateUserEvent(@PathVariable("id") Long id,
                                  @ModelAttribute Event updatedEvent,
                                  @RequestParam("detectionDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate detectionDate,
                                  Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getUser().getId().equals(user.getId()) || user.getRole().equalsIgnoreCase("admin")) {
            event.setDescription(updatedEvent.getDescription());
            event.setEmergency(updatedEvent.isEmergency());
            event.setDetectionDate(detectionDate);
            event.setDowntime(updatedEvent.getDowntime());

            eventRepository.save(event);
            return "redirect:/userEvents";
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to edit this event");
        }
    }

    @DeleteMapping("/userEvents/{id}")
    public ResponseEntity<String> deleteUserEvent(@PathVariable("id") Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getUser().getId().equals(user.getId()) || user.getRole().equalsIgnoreCase("admin")) {
            eventRepository.delete(event);
            return ResponseEntity.ok("Event deleted successfully");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You don't have permission to delete this event");
        }
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
    public String viewUsers(Model model,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(defaultValue = "id") String sortBy,
                             @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<User> usersPage = userRepository.findAll(pageRequest);

        model.addAttribute("eventsPage", usersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        return "viewUsers";
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
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String editUsers(Model model,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(defaultValue = "username") String sortBy,
                           @RequestParam(defaultValue = "asc") String sortDir) {
        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<User> usersPage = userRepository.findAll(pageRequest);

        model.addAttribute("usersPage", usersPage);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", usersPage.getTotalPages());
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        return "editUsers";
    }

    @PostMapping("/editUsers")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String updateUser(@RequestParam Long id,
                             @RequestParam String username,
                             @RequestParam String name,
                             @RequestParam String surname,
                             @RequestParam String email,
                             @RequestParam String role,
                             @RequestParam(required = false) String newPassword,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

            user.setUsername(username);
            user.setName(name);
            user.setSurname(surname);
            user.setEmail(email);
            user.setRole(role);

            if (newPassword != null && !newPassword.isEmpty()) {
                if (newPassword.length() < 6) {
                    redirectAttributes.addFlashAttribute("error", "New password must be at least 6 characters long");
                    return "redirect:/editUsers";
                }
                user.setPassword(passwordEncoder.encode(newPassword));
            }

            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "User updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update user: " + e.getMessage());
        }
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

    @GetMapping("/printResults")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public String printResults() {
        return "printResults";
    }

    @GetMapping("/printUsers")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InputStreamResource> printUsers(@RequestParam(required = false) String role) {
        List<User> users;
        if (role != null && !role.equals("everyone")) {
            users = userRepository.findByRole(role);
        } else {
            users = userRepository.findAll();
        }
        ByteArrayInputStream bis = pdfGeneratorService.generateUsersPdf(users);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=users.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }

    @GetMapping("/printEvents")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<InputStreamResource> printEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        List<Event> events;
        if (fromDate != null && toDate != null) {
            events = eventRepository.findByDetectionDateBetween(fromDate, toDate);
        } else {
            events = eventRepository.findAll();
        }

        ByteArrayInputStream bis = pdfGeneratorService.generateEventsPdf(events);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=events.pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }
}