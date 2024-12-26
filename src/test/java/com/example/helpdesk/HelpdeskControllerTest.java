package com.example.helpdesk;

import com.example.helpdesk.dto.UserRegistrationDTO;
import com.example.helpdesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.instanceOf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HelpdeskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("userDto", instanceOf(UserRegistrationDTO.class)));
    }

   @Test
    void testRegisterFailure() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "testuser")
                        .param("password", "abc123")
                        .param("confirmPassword", "xyz789")  // mismatch
                        .param("email", "test@example.com")
                        .param("name", "Test")
                        .param("surname", "User"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().hasErrors());
    }

    @Test
    void testLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void testDashboard() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"));
    }

    @Test
    @WithAnonymousUser
    void testUserEventsAnonymous() throws Exception {
        mockMvc.perform(get("/userEvents"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "john", roles = "USER")
    void testUserEventsAuthenticated() throws Exception {
        mockMvc.perform(get("/userEvents"))
                .andExpect(status().isOk())
                .andExpect(view().name("userEvents"))
                .andExpect(model().attributeExists("eventsPage"));
    }

    @Test
    void testDeleteEventUnauthenticated() throws Exception {
        mockMvc.perform(delete("/userEvents/999"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testDeleteEventAsAdmin() throws Exception {
        mockMvc.perform(delete("/userEvents/888"))
                .andExpect(status().isOk());
    }

    @Test
    void testAddEventPage() throws Exception {
        mockMvc.perform(get("/addEvent"))
                .andExpect(status().isOk())
                .andExpect(view().name("addEvent"));
    }

    @Test
    void testViewEvents() throws Exception {
        mockMvc.perform(get("/viewEvents"))
                .andExpect(status().isOk())
                .andExpect(view().name("viewEvents"))
                .andExpect(model().attributeExists("eventsPage", "currentPage", "totalPages", "sortBy", "sortDir"));
    }

}
