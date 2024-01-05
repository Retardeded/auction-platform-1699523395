package pl.use.auction.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Category;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Configuration
public class DefaultUserConfig {

    @Bean
    CommandLineRunner createDefaultCategories(CategoryRepository categoryRepository) {
        return args -> {
            String[] categoryNames = {
                    "Electronics", "Fashion", "Home & Garden", "Automotive",
                    "Baby", "Health", "Beauty", "Culture & Entertainment",
                    "Sports & Tourism", "Collectibles & Art"
            };
            for (String name : categoryNames) {
                categoryRepository.findByName(name).orElseGet(() -> {
                    Category category = new Category();
                    category.setName(name);
                    return categoryRepository.save(category);
                });
            }
        };
    }
    @Bean
    CommandLineRunner createDefaultUsersAndAuctions(UserRepository userRepository,
                                                    AuctionRepository auctionRepository,
                                                    CategoryRepository categoryRepository,
                                                    PasswordEncoder passwordEncoder) {
        return args -> {
            AuctionUser defaultUser = createUserIfNotFound(userRepository, passwordEncoder, "default@gmail.com", "default", "default");

            AuctionUser anotherUser = createUserIfNotFound(userRepository, passwordEncoder, "another@gmail.com", "another", "another");

            AuctionUser another3User = createUserIfNotFound(userRepository, passwordEncoder, "basic@gmail.com", "basic", "basic");

            if (auctionRepository.findByAuctionCreator(defaultUser).isEmpty()) {
                createSampleAuctions(auctionRepository, defaultUser, categoryRepository);
            }
            if (auctionRepository.findByAuctionCreator(anotherUser).isEmpty()) {
                createSampleAuctions(auctionRepository, anotherUser, categoryRepository);
            }
            if (auctionRepository.findByAuctionCreator(another3User).isEmpty()) {
                createSampleAuctions(auctionRepository, another3User, categoryRepository);
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

    private void createSampleAuctions(AuctionRepository auctionRepository, AuctionUser user, CategoryRepository categoryRepository) throws Exception {
        String userIdentifier = user.getUsername().toUpperCase();

        String[][] auctionData = {
                {"Vintage Camera", "A classic vintage camera in excellent condition.", "Electronics", "ONGOING"},
                {"Designer Dress", "A stunning dress from a renowned fashion designer.", "Fashion", "ONGOING"},
                {"Garden Shovel", "A durable shovel for gardening.", "Home & Garden", "ONGOING"},
                {"Antique Vase", "A beautiful antique vase, perfect for collectors.", "Collectibles & Art", "EXPIRED"},
                {"Signed Book", "A book signed by its famous author.", "Collectibles & Art", "ONGOING"},
                {"Handmade Jewelry", "Exquisite handmade jewelry with unique design.", "Collectibles & Art", "EXPIRED"},
        };

        for (String[] auctionInfo : auctionData) {
            Auction auction = new Auction();
            auction.setTitle(userIdentifier + " User's " + auctionInfo[0]);
            auction.setDescription(auctionInfo[1]);

            Category itemCategory = categoryRepository.findByName(auctionInfo[2])
                    .orElseThrow(() -> new Exception("Category not found: " + auctionInfo[2]));

            auction.setCategory(itemCategory);

            auction.setStartTime(LocalDateTime.now());
            auction.setEndTime(LocalDateTime.now().plusDays(10));
            auction.setStartingPrice(BigDecimal.valueOf(100));
            auction.setHighestBid(BigDecimal.valueOf(100));
            auction.setStatus(auctionInfo[3]);
            auction.setAuctionCreator(user);

            auctionRepository.save(auction);
        }
    }
}
