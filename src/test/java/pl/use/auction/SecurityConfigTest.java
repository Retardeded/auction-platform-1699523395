package pl.use.auction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.UserService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
public class SecurityConfigTest {
    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
    }

    @Test
    public void whenGetRegister_thenStatusOk() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk());
    }

    @Test
    public void whenGetLogin_thenStatusOk() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    public void whenGetCssFile_thenStatusOk() throws Exception {
        mockMvc.perform(get("/css/styles.css"))
                .andExpect(status().isOk());
    }

    @Test
    public void whenAccessProtectedUrlWithoutAuth_thenRedirectToLogin() throws Exception {
        mockMvc.perform(get("/protected-url"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    public void whenLoginWithValidUser_thenRedirectToHome() throws Exception {

        AuctionUser testUser = new AuctionUser();
        testUser.setEmail("testUser@example.com");
        testUser.setPassword(passwordEncoder.encode("userPassword"));
        testUser.setVerified(true);
        userRepository.save(testUser);

        mockMvc.perform(formLogin("/login").user("email", "testUser@example.com").password("password", "userPassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));
    }

}