package pl.use.auction.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class DefaultUserConfig {

    @Bean
    CommandLineRunner createDefaultUsersAndAuctions(UserRepository userRepository,
                                                    AuctionRepository auctionRepository,
                                                    PasswordEncoder passwordEncoder) {
        return args -> {
            AuctionUser defaultUser = createUserIfNotFound(userRepository, passwordEncoder, "default@gmail.com", "default", "default");

            AuctionUser anotherUser = createUserIfNotFound(userRepository, passwordEncoder, "another@gmail.com", "another", "another");

            if (auctionRepository.findByUser(defaultUser).isEmpty()) {
                createSampleAuctions(auctionRepository, defaultUser);
            }

            if (auctionRepository.findByUser(anotherUser).isEmpty()) {
                createSampleAuctions(auctionRepository, anotherUser);
            }
        };
    }

    private AuctionUser createUserIfNotFound(UserRepository userRepository, PasswordEncoder passwordEncoder, String email, String username, String password) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            AuctionUser user = new AuctionUser();
            user.setEmail(email);
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setVerified(true);
            return userRepository.save(user);
        });
    }

    private void createSampleAuctions(AuctionRepository auctionRepository, AuctionUser user) {
        String userIdentifier = user.getUsername().equals("default") ? "Default" : "Another";

        Auction ongoingAuction = new Auction();
        ongoingAuction.setTitle(userIdentifier + " User's Ongoing Auction");
        ongoingAuction.setDescription("This is an ongoing auction created by " + userIdentifier + " user.");
        ongoingAuction.setStartTime(LocalDateTime.now().minusDays(1));
        ongoingAuction.setEndTime(LocalDateTime.now().plusDays(1));
        ongoingAuction.setStartingPrice(BigDecimal.valueOf(100));
        ongoingAuction.setCurrentBid(BigDecimal.valueOf(150));
        ongoingAuction.setStatus("ONGOING");
        ongoingAuction.setUser(user);
        auctionRepository.save(ongoingAuction);

        Auction pastAuction = new Auction();
        pastAuction.setTitle(userIdentifier + " User's Past Auction");
        pastAuction.setDescription("This is a past auction created by " + userIdentifier + " user.");
        pastAuction.setStartTime(LocalDateTime.now().minusDays(10));
        pastAuction.setEndTime(LocalDateTime.now().minusDays(5));
        pastAuction.setStartingPrice(BigDecimal.valueOf(50));
        pastAuction.setCurrentBid(BigDecimal.valueOf(75));
        pastAuction.setStatus("ENDED");
        pastAuction.setUser(user);
        auctionRepository.save(pastAuction);
    }
}
