package pl.use.auction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.use.auction.dto.UserRegistrationDto;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.UserService;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private UserService userService;


    @Test
    public void whenRegisterNewUser_thenSaveUser() {
        when(userRepository.save(any(AuctionUser.class))).thenAnswer(i -> i.getArguments()[0]);

        UserRegistrationDto registrationDto = new UserRegistrationDto();
        registrationDto.setEmail("test@example.com");
        registrationDto.setPassword("password");
        String token = "token123";

        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        AuctionUser savedUser = userService.registerNewUser(registrationDto, token);

        verify(userRepository).save(any(AuctionUser.class));
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals(token, savedUser.getVerificationToken());
    }

    @Test
    public void whenLoadUserByUsernameAndUserExistsAndVerified_thenReturnsUserDetails() {
        String email = "test@example.com";
        AuctionUser auctionUser = new AuctionUser();
        auctionUser.setEmail(email);
        auctionUser.setPassword("password");
        auctionUser.setVerified(true);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(auctionUser));

        UserDetails userDetails = userService.loadUserByUsername(email);

        assertNotNull(userDetails);
        assertEquals(email, userDetails.getUsername());
    }

    @Test
    public void whenLoadUserByUsernameAndUserNotExists_thenThrowsUsernameNotFoundException() {
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername(email);
        });
    }

    @Test
    public void whenLoadUserByUsernameAndUserNotVerified_thenThrowsUsernameNotFoundException() {
        String email = "unverified@example.com";
        AuctionUser auctionUser = new AuctionUser();
        auctionUser.setEmail(email);
        auctionUser.setPassword("password");
        auctionUser.setVerified(false);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(auctionUser));

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername(email);
        });
    }

    @Test
    public void whenSendVerificationEmail_thenEmailSentWithCorrectDetails() {
        AuctionUser user = new AuctionUser();
        user.setEmail("test@example.com");
        String token = "verificationToken";

        userService.sendVerificationEmail(user, token);

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());

        SimpleMailMessage sentEmail = mailCaptor.getValue();
        assertEquals("test@example.com", sentEmail.getTo()[0]);
        assertEquals("Verify your email", sentEmail.getSubject());
        assertTrue(sentEmail.getText().contains("http://localhost:8080/verify?token=" + token));
    }
}