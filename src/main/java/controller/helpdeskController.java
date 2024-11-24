package controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class helpdeskController {
    @GetMapping("")
    public String helpdesk() {
        return "Hello World";
    }
}
