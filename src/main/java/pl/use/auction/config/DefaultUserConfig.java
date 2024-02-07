package pl.use.auction.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionStatus;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Category;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static pl.use.auction.util.StringUtils.createSlugFromTitle;

@Configuration
public class DefaultUserConfig {

    @Bean
    CommandLineRunner createDefaultCategories(CategoryRepository categoryRepository) {
        return args -> {
            Category electronics = createOrFindCategory("Electronics", null, categoryRepository);
            Category fashion = createOrFindCategory("Fashion", null, categoryRepository);
            Category homeGarden = createOrFindCategory("Home & Garden", null, categoryRepository);
            Category collectiblesArt = createOrFindCategory("Collectibles & Art", null, categoryRepository);

            // Subcategories for Electronics
            createOrFindCategory("Cameras", electronics, categoryRepository);
            createOrFindCategory("Smartphones", electronics, categoryRepository);
            createOrFindCategory("Laptops", electronics, categoryRepository);
            createOrFindCategory("Audio", electronics, categoryRepository);
            // ... create other subcategories for Electronics ...

            // Subcategories for Fashion
            createOrFindCategory("Dresses", fashion, categoryRepository);
            createOrFindCategory("Shoes", fashion, categoryRepository);
            createOrFindCategory("Accessories", fashion, categoryRepository);
            createOrFindCategory("Menswear", fashion, categoryRepository);

            // Subcategories for Home & Garden
            createOrFindCategory("Gardening Tools", homeGarden, categoryRepository);

            // Subcategories for Collectibles & Art
            createOrFindCategory("Antiques", collectiblesArt, categoryRepository);
            createOrFindCategory("Books", collectiblesArt, categoryRepository);
        };
    }

    private Category createOrFindCategory(String name, Category parent, CategoryRepository repository) {
        return repository.findByNameAndParentCategory(name, parent)
                .orElseGet(() -> {
                    Category newCategory = new Category(name, parent);
                    return repository.save(newCategory);
                });
    }

    @Bean
    CommandLineRunner createDefaultUsersAndAuctions(UserRepository userRepository,
                                                    AuctionRepository auctionRepository,
                                                    CategoryRepository categoryRepository,
                                                    PasswordEncoder passwordEncoder) {
        return args -> {
            AuctionUser defaultUser = createUserIfNotFound(userRepository, passwordEncoder, "default@gmail.com", "default", "default", "Krakow");

            AuctionUser anotherUser = createUserIfNotFound(userRepository, passwordEncoder, "another@gmail.com", "another", "another", "Warsaw");

            AuctionUser basicUser = createUserIfNotFound(userRepository, passwordEncoder, "basic@gmail.com", "basic", "basic", "Krakow");

            AuctionUser testUser = createUserIfNotFound(userRepository, passwordEncoder, "test@gmail.com", "test", "test", "Warsaw");

            if (auctionRepository.findByAuctionCreator(defaultUser).isEmpty()) {
                createSampleAuctions(auctionRepository, defaultUser, categoryRepository);
            }
            if (auctionRepository.findByAuctionCreator(anotherUser).isEmpty()) {
                createSampleAuctions(auctionRepository, anotherUser, categoryRepository);
            }
            if (auctionRepository.findByAuctionCreator(basicUser).isEmpty()) {
                createSampleAuctions(auctionRepository, basicUser, categoryRepository);
            }
            if (auctionRepository.findByAuctionCreator(testUser).isEmpty()) {
                createSampleAuctions(auctionRepository, testUser, categoryRepository);
            }
        };
    }

    private AuctionUser createUserIfNotFound(UserRepository userRepository, PasswordEncoder passwordEncoder, String email, String username, String password, String location) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            AuctionUser user = new AuctionUser();
            user.setEmail(email);
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setVerified(true);
            user.setLocation(location);
            return userRepository.save(user);
        });
    }

    private void createSampleAuctions(AuctionRepository auctionRepository, AuctionUser user, CategoryRepository categoryRepository) throws Exception {
        String userIdentifier = user.getUsername().toUpperCase();
        Random random = new Random();

        // Example data for auctions
        Object[][] auctionData = {
                {"Vintage Camera", "A classic vintage camera in excellent condition.", "Cameras", "Electronics", "ACTIVE"},
                {"Designer Dress", "A stunning dress from a renowned fashion designer.", "Dresses", "Fashion", "ACTIVE"},
                {"Garden Shovel", "A durable shovel for gardening.", "Gardening Tools", "Home & Garden", "ACTIVE",},
                {"Antique Vase", "A beautiful antique vase, perfect for collectors.", "Antiques", "Collectibles & Art", "SOLD"},
                {"Signed Book", "A book signed by its famous author.", "Books", "Collectibles & Art", "ACTIVE"},
                {"Handmade Jewelry", "Exquisite handmade jewelry with unique design.", "Accessories", "Fashion", "SOLD"},
        };

        boolean isFirstAuction = true;
        LocalDateTime now = LocalDateTime.now();

        for (Object[] auctionInfo : auctionData) {
            Auction auction = new Auction();
            auction.setTitle(userIdentifier + " User's " + auctionInfo[0]);
            auction.setSlug(createSlugFromTitle(auction.getTitle()));
            auction.setDescription((String) auctionInfo[1]);

            String subCategoryName = (String) auctionInfo[2];
            String parentCategoryName = (String) auctionInfo[3];
            Category parentCategory = categoryRepository.findByName(parentCategoryName)
                    .orElseThrow(() -> new Exception("Parent category not found: " + parentCategoryName));

            Category itemCategory = categoryRepository.findByNameAndParentCategory(subCategoryName, parentCategory)
                    .orElseThrow(() -> new Exception("Subcategory not found: " + subCategoryName + " under " + parentCategoryName));

            auction.setCategory(itemCategory);

            long randomStartDays = ThreadLocalRandom.current().nextLong(-3, 4);
            LocalDateTime randomStartTime = now.plusDays(randomStartDays);

            long randomEndDays = ThreadLocalRandom.current().nextLong(-2, 6);
            LocalDateTime randomEndTime = now.plusDays(randomEndDays);

            auction.setStartTime(randomStartTime);
            auction.setEndTime(randomEndTime);

            BigDecimal startingPrice = BigDecimal.valueOf(50 + random.nextInt(101));
            auction.setStartingPrice(startingPrice);
            BigDecimal highestBid;
            if (isFirstAuction) {
                highestBid = BigDecimal.ZERO;
                isFirstAuction = false; // Only for the first auction
            } else {
                highestBid = startingPrice.add(BigDecimal.valueOf(random.nextInt(50) + 1));
            }
            auction.setBuyNowPrice(highestBid.add(BigDecimal.valueOf(50)));
            auction.setHighestBid(highestBid);
            String statusString = (String) auctionInfo[4];
            AuctionStatus statusEnum = AuctionStatus.valueOf(statusString);
            auction.setStatus(statusEnum);
            auction.setAuctionCreator(user);
            auction.setLocation(user.getLocation());

            String baseImageName = ((String) auctionInfo[0]).replaceAll("\\s+", "_");
            String originalImagePath = "auctionImages/" + baseImageName + ".png";
            String mirroredImagePath = "auctionImages/" + baseImageName + "_mirrored.png";
            String rotatedImagePath = "auctionImages/" + baseImageName + "_rotated.png";
            auction.setImageUrls(List.of(originalImagePath, mirroredImagePath, rotatedImagePath));

            auctionRepository.save(auction);
        }
    }
}
