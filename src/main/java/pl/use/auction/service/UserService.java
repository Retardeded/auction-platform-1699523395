package pl.use.auction.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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
import pl.use.auction.model.PasswordChangeResult;
import pl.use.auction.model.UserStatus;
import pl.use.auction.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;

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
        auctionUser.setRole("USER");
        auctionUser.setVerified(false);
        auctionUser.setEmail(registrationDto.getEmail());
        auctionUser.setUsername(registrationDto.getUsername());
        auctionUser.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        auctionUser.setVerificationToken(token);
        return userRepository.save(auctionUser);
    }

    public List<AuctionUser> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<AuctionUser> userOptional = userRepository.findByEmail(email);

        AuctionUser auctionUser = userOptional.orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        if (!auctionUser.isVerified()) {
            throw new UsernameNotFoundException("User not verified");
        }

        if (auctionUser.getStatus() == UserStatus.BANNED) {
            throw new LockedException("User is banned");
        }

        if (auctionUser.getSuspensionEndDate() != null && auctionUser.getSuspensionEndDate().after(new Date())) {
            throw new LockedException("User is currently suspended.");
        }

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + auctionUser.getRole().toUpperCase()));

        return new User(auctionUser.getEmail(), auctionUser.getPassword(), authorities);
    }

    @Transactional
    public PasswordChangeResult changeUserPassword(String oldPassword, String newPassword, String confirmNewPassword, AuctionUser user) {
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return PasswordChangeResult.INVALID_OLD_PASSWORD;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            return PasswordChangeResult.PASSWORD_MISMATCH;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return PasswordChangeResult.SUCCESS;
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