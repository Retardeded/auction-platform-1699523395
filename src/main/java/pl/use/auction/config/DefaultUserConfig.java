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

    //class purely used for easier developing/testing stuff
    @Bean
    CommandLineRunner createDefaultUserAndAuctions(UserRepository userRepository,
                                                   AuctionRepository auctionRepository,
                                                   PasswordEncoder passwordEncoder) {
        return args -> {
            AuctionUser defaultUser;
            if (userRepository.findByEmail("default@gmail.com").isEmpty()) {
                defaultUser = new AuctionUser();
                defaultUser.setEmail("default@gmail.com");
                defaultUser.setUsername("default");
                defaultUser.setPassword(passwordEncoder.encode("default"));
                defaultUser.setVerified(true);
                userRepository.save(defaultUser);
            } else {
                defaultUser = userRepository.findByEmail("default@gmail.com").get();
            }

            if (auctionRepository.findByUser(defaultUser).isEmpty()) {
                // Create some sample ongoing and past auctions
                Auction ongoingAuction = new Auction();
                ongoingAuction.setTitle("Ongoing Auction 1");
                ongoingAuction.setDescription("Description of ongoing auction");
                ongoingAuction.setStartTime(LocalDateTime.now().minusDays(1));
                ongoingAuction.setEndTime(LocalDateTime.now().plusDays(1));
                ongoingAuction.setStartingPrice(BigDecimal.valueOf(100));
                ongoingAuction.setCurrentBid(BigDecimal.valueOf(150));
                ongoingAuction.setStatus("ONGOING");
                ongoingAuction.setUser(defaultUser);
                auctionRepository.save(ongoingAuction);

                Auction pastAuction = new Auction();
                pastAuction.setTitle("Past Auction 1");
                pastAuction.setDescription("Description of past auction");
                pastAuction.setStartTime(LocalDateTime.now().minusDays(10));
                pastAuction.setEndTime(LocalDateTime.now().minusDays(5));
                pastAuction.setStartingPrice(BigDecimal.valueOf(50));
                pastAuction.setCurrentBid(BigDecimal.valueOf(75));
                pastAuction.setStatus("ENDED");
                pastAuction.setUser(defaultUser);
                auctionRepository.save(pastAuction);
            }
        };
    }
}
