package pl.use.auction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import pl.use.auction.controller.RegistrationController;
import pl.use.auction.dto.UserRegistrationDto;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.UserService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class RegistrationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private RegistrationController registrationController;

    @BeforeEach
    public void setUp() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);

        mockMvc = MockMvcBuilders.standaloneSetup(registrationController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    public void whenGetRegister_thenReturnsRegistrationView() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("authentication/register")); // Updated view name
    }

    @Test
    public void whenRegisterUserAccount_thenRegisterAndSendEmail() throws Exception {
        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setEmail("test@example.com");
        registrationDto.setPassword("password");

        AuctionUser mockUser = new AuctionUser();
        mockUser.setEmail("test@example.com");

        when(userService.registerNewUser(any(UserRegistrationDto.class), anyString()))
                .thenReturn(mockUser);

        mockMvc.perform(post("/register")
                        .param("email", registrationDto.getEmail())
                        .param("password", registrationDto.getPassword()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/authentication/thank-you"));

        ArgumentCaptor<UserRegistrationDto> userCaptor = ArgumentCaptor.forClass(UserRegistrationDto.class);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);

        verify(userService).registerNewUser(userCaptor.capture(), tokenCaptor.capture());
        verify(userService).sendVerificationEmail(any(AuctionUser.class), eq(tokenCaptor.getValue()));

        assertEquals(registrationDto.getEmail(), userCaptor.getValue().getEmail());
        assertEquals(registrationDto.getPassword(), userCaptor.getValue().getPassword());
    }

    @Test
    public void whenVerifyUserWithValidToken_thenRedirectToLogin() throws Exception {
        String token = "validToken";
        AuctionUser mockUser = new AuctionUser();
        mockUser.setEmail("test@example.com");
        mockUser.setVerificationToken(token);

        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/verify").param("token", token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/authentication/login"));

        verify(userRepository).save(mockUser);
        assertTrue(mockUser.isVerified());
    }

    @Test
    public void whenVerifyUserWithInvalidToken_thenReturnErrorView() throws Exception {
        String token = "invalidToken";
        when(userRepository.findByVerificationToken(token)).thenReturn(Optional.empty());

        mockMvc.perform(get("/verify").param("token", token))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));
    }
}