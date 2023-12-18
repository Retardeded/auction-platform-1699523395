package pl.use.auction.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pl.use.auction.dto.UserLoginDto;
import pl.use.auction.dto.UserRegistrationDto;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(AuctionUser user, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Verify your email");
        message.setText("Thank you for registering. Please click the link below to verify your email: \n"
                + "http://localhost:8080/verify?token=" + token);
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

}