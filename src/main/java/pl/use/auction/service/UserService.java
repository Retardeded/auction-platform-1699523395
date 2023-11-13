package pl.use.auction.service;

import org.springframework.beans.factory.annotation.Autowired;
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

    public AuctionUser registerNewUser(UserRegistrationDto registrationDto) {
        AuctionUser auctionUser = new AuctionUser();
        auctionUser.setEmail(registrationDto.getEmail());
        auctionUser.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        return userRepository.save(auctionUser);
    }

    public boolean authenticateUser(UserLoginDto userLoginDto) {
        Optional<AuctionUser> user = userRepository.findByEmail(userLoginDto.getEmail());
        var users = getAllUsers();
        System.out.println(users);
        if (user.isPresent() && user.get().getPassword().equals(userLoginDto.getPassword())) {
            return true;
        }
        return false;
    }

    public List<AuctionUser> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<AuctionUser> user = userRepository.findByEmail(email);

        if (!user.isPresent()) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        }

        AuctionUser auctionUser = user.get();
        return new User(auctionUser.getEmail(), auctionUser.getPassword(), new ArrayList<>());
    }
}