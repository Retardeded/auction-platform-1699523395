package pl.use.auction.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DefaultUserConfig {

    //class purely used for easier developing/testing stuff
    @Bean
    CommandLineRunner createDefaultUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.findByEmail("default@gmail.com").isEmpty()) {
                AuctionUser defaultUser = new AuctionUser();
                defaultUser.setEmail("default@gmail.com");
                defaultUser.setPassword(passwordEncoder.encode("default"));
                defaultUser.setVerified(true);
                userRepository.save(defaultUser);
            }
        };
    }
}
