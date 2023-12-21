package pl.use.auction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.use.auction.dto.UserRegistrationDto;
import pl.use.auction.exception.InvalidTokenException;
import pl.use.auction.exception.TokenExpiredException;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.PasswordChangeResult;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.UserService;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
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

    @Value("${app.url}")
    private String appUrl;

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
        assertTrue(sentEmail.getText().contains( appUrl + "/verify?token=" + token));
    }

    @Test
    public void whenProcessForgotPassword_thenGenerateTokenAndSendEmail() {
        String userEmail = "test@example.com";
        AuctionUser user = new AuctionUser();
        user.setEmail(userEmail);
        user.setVerified(true);

        when(userRepository.findByEmail(userEmail)).thenReturn(Optional.of(user));

        ArgumentCaptor<AuctionUser> userCaptor = ArgumentCaptor.forClass(AuctionUser.class);

        userService.processForgotPassword(userEmail);

        verify(userRepository).save(userCaptor.capture());
        AuctionUser savedUser = userCaptor.getValue();
        assertNotNull(savedUser.getResetToken());
        assertTrue(savedUser.getResetTokenExpiryTime().isAfter(LocalDateTime.now()));

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());

        SimpleMailMessage sentEmail = mailCaptor.getValue();
        assertEquals(userEmail, sentEmail.getTo()[0]);
        assertTrue(sentEmail.getText().contains(appUrl + "/reset-password?token=" + savedUser.getResetToken()));
    }

    @Test
    public void whenResetPasswordWithValidToken_thenResetPassword() {
        String token = "validToken";
        String newPassword = "newPassword";
        AuctionUser user = new AuctionUser();
        user.setEmail("user@example.com");
        user.setResetToken(token);
        user.setResetTokenExpiryTime(LocalDateTime.now().plusHours(1));

        when(userRepository.findByResetToken(token)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn("encodedNewPassword");

        ArgumentCaptor<AuctionUser> userCaptor = ArgumentCaptor.forClass(AuctionUser.class);

        userService.resetPassword(token, newPassword);

        verify(userRepository).save(userCaptor.capture());
        AuctionUser updatedUser = userCaptor.getValue();
        assertEquals("encodedNewPassword", updatedUser.getPassword());
        assertNull(updatedUser.getResetToken());
        assertNull(updatedUser.getResetTokenExpiryTime());
    }

    @Test
    public void whenResetPasswordWithExpiredToken_thenThrowTokenExpiredException() {
        String token = "expiredToken";
        String newPassword = "newPassword";
        AuctionUser user = new AuctionUser();
        user.setEmail("user@example.com");
        user.setResetToken(token);
        user.setResetTokenExpiryTime(LocalDateTime.now().minusHours(1)); // Token has expired

        when(userRepository.findByResetToken(token)).thenReturn(Optional.of(user));

        assertThrows(TokenExpiredException.class, () -> {
            userService.resetPassword(token, newPassword);
        });
    }

    @Test
    public void whenResetPasswordWithInvalidToken_thenThrowInvalidTokenException() {
        String token = "invalidToken";
        String newPassword = "newPassword";

        when(userRepository.findByResetToken(token)).thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class, () -> {
            userService.resetPassword(token, newPassword);
        });
    }

    @Test
    public void whenChangePasswordWithValidOldPassword_thenSuccess() {
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";
        AuctionUser user = new AuctionUser();
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode(oldPassword));

        when(passwordEncoder.matches(oldPassword, user.getPassword())).thenReturn(true);

        PasswordChangeResult result = userService.changeUserPassword(oldPassword, newPassword, newPassword, user);

        verify(userRepository).save(any(AuctionUser.class));
        assertEquals(PasswordChangeResult.SUCCESS, result);
    }

    @Test
    public void whenChangePasswordWithInvalidOldPassword_thenFail() {
        String oldPassword = "oldPassword";
        String invalidOldPassword = "invalidOldPassword";
        String newPassword = "newPassword";
        AuctionUser user = new AuctionUser();
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode(oldPassword));

        when(passwordEncoder.matches(invalidOldPassword, user.getPassword())).thenReturn(false);

        PasswordChangeResult result = userService.changeUserPassword(invalidOldPassword, newPassword, newPassword, user);

        assertEquals(PasswordChangeResult.INVALID_OLD_PASSWORD, result);
    }

    @Test
    public void whenChangePasswordWithMismatchedNewPasswords_thenFail() {
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";
        String confirmPassword = "confirmPassword";
        AuctionUser user = new AuctionUser();
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode(oldPassword));

        when(passwordEncoder.matches(oldPassword, user.getPassword())).thenReturn(true);

        PasswordChangeResult result = userService.changeUserPassword(oldPassword, newPassword, confirmPassword, user);

        assertEquals(PasswordChangeResult.PASSWORD_MISMATCH, result);
    }
}