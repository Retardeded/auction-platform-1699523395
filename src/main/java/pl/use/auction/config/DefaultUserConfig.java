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

            AuctionUser another3User = createUserIfNotFound(userRepository, passwordEncoder, "basic@gmail.com", "basic", "basic");

            if (auctionRepository.findByAuctionCreator(defaultUser).isEmpty()) {
                createSampleAuctions(auctionRepository, defaultUser);
            }
            if (auctionRepository.findByAuctionCreator(anotherUser).isEmpty()) {
                createSampleAuctions(auctionRepository, anotherUser);
            }
            if (auctionRepository.findByAuctionCreator(another3User).isEmpty()) {
                createSampleAuctions(auctionRepository, another3User);
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
        String userIdentifier = user.getUsername().toUpperCase();

        // Array of sample auction titles, descriptions, and status (ongoing or expired)
        String[][] auctionData = {
                {"Vintage Camera", "A classic vintage camera in excellent condition.", "ONGOING"},
                {"Antique Vase", "A beautiful antique vase, perfect for collectors.", "EXPIRED"},
                {"Signed Book", "A book signed by its famous author.", "ONGOING"},
                {"Handmade Jewelry", "Exquisite handmade jewelry with unique design.", "EXPIRED"},
                {"Rare Vinyl Record", "A rare vinyl record for music enthusiasts.", "ONGOING"}
        };

        // Create multiple auctions with different characteristics
        for (int i = 0; i < auctionData.length; i++) {
            Auction auction = new Auction();
            auction.setTitle(userIdentifier + " User's " + auctionData[i][0]);
            auction.setDescription(auctionData[i][1]);
            if ("ONGOING".equals(auctionData[i][2])) {
                auction.setStartTime(LocalDateTime.now().minusDays(i));
                auction.setEndTime(LocalDateTime.now().plusDays(i + 1));
            } else { // EXPIRED
                auction.setStartTime(LocalDateTime.now().minusDays(i + 5));
                auction.setEndTime(LocalDateTime.now().minusDays(i + 1));
            }
            auction.setStartingPrice(BigDecimal.valueOf(50 + i * 10));
            auction.setHighestBid(BigDecimal.valueOf(75 + i * 10));
            auction.setStatus(auctionData[i][2]);
            auction.setAuctionCreator(user);
            auctionRepository.save(auction);
        }
    }
}
