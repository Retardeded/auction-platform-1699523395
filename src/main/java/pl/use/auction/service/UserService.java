package pl.use.auction.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.use.auction.dto.UserRegistrationDto;
import pl.use.auction.exception.InvalidTokenException;
import pl.use.auction.exception.TokenExpiredException;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmailAddress;

    @Value("${app.url}")
    private String appUrl;

    public void sendVerificationEmail(AuctionUser user, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Verify your email");
        message.setText("Thank you for registering. Please click the link below to verify your email: \n"
                + appUrl + "/verify?token=" + token);
        mailSender.send(message);
    }

    public AuctionUser registerNewUser(UserRegistrationDto registrationDto, String token) {
        AuctionUser auctionUser = new AuctionUser();
        auctionUser.setEmail(registrationDto.getEmail());
        auctionUser.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        auctionUser.setVerificationToken(token);
        return userRepository.save(auctionUser);
    }

    public List<AuctionUser> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<AuctionUser> user = userRepository.findByEmail(email);

        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        AuctionUser auctionUser = user.get();
        if (!auctionUser.isVerified()) {
            throw new UsernameNotFoundException("User not verified");
        }

        return new User(auctionUser.getEmail(), auctionUser.getPassword(), new ArrayList<>());
    }

    @Transactional
    public void processForgotPassword(String userEmail) {
        Optional<AuctionUser> userOptional = userRepository.findByEmail(userEmail);
        if (userOptional.isPresent()) {
            AuctionUser user = userOptional.get();

            String token = UUID.randomUUID().toString();
            user.setResetToken(token);
            user.setResetTokenExpiryTime(LocalDateTime.now().plusHours(1)); // Token valid for 1 hour
            userRepository.save(user);

            String resetUrl = appUrl + "/reset-password?token=" + token;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(userEmail);
            message.setSubject("Password Reset Request");
            message.setText("To reset your password, click the link below:\n" + resetUrl);
            mailSender.send(message);
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        Optional<AuctionUser> userOptional = userRepository.findByResetToken(token);

        if (userOptional.isPresent()) {
            AuctionUser user = userOptional.get();

            if (user.getResetTokenExpiryTime().isAfter(LocalDateTime.now())) {

                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetToken(null);
                user.setResetTokenExpiryTime(null);

                userRepository.save(user);
            } else {
                throw new TokenExpiredException("The token has expired.");
            }
        } else {
            throw new InvalidTokenException("The token is invalid.");
        }
    }

}