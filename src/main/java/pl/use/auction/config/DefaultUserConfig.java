package pl.use.auction.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.use.auction.model.*;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import pl.use.auction.service.AuctionService;

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
                                                    AuctionService auctionService,
                                                    PasswordEncoder passwordEncoder) {
        return args -> {
            AuctionUser defaultUser = createUserIfNotFound(userRepository, passwordEncoder, "default@gmail.com", "default", "default", "Krakow", "USER");

            AuctionUser anotherUser = createUserIfNotFound(userRepository, passwordEncoder, "another@gmail.com", "another", "another", "Warsaw", "USER");

            AuctionUser basicUser = createUserIfNotFound(userRepository, passwordEncoder, "basic@gmail.com", "basic", "basic", "Krakow", "USER");

            AuctionUser testUser = createUserIfNotFound(userRepository, passwordEncoder, "test@gmail.com", "test", "test", "Warsaw", "USER");

            AuctionUser bottomUser = createUserIfNotFound(userRepository, passwordEncoder, "bottom@gmail.com", "bottom", "bottom", "Wroclaw", "USER");

            AuctionUser adminUser = createUserIfNotFound(userRepository, passwordEncoder, "admin@gmail.com", "admin", "admin", "Wroclaw", "ADMIN");

            if (auctionRepository.findByAuctionCreator(defaultUser).isEmpty()) {
                createSampleAuctions(testUser, auctionRepository, defaultUser, categoryRepository, userRepository);
            }
            if (auctionRepository.findByAuctionCreator(anotherUser).isEmpty()) {
                createSampleAuctions(testUser, auctionRepository, anotherUser, categoryRepository, userRepository);
            }
            if (auctionRepository.findByAuctionCreator(basicUser).isEmpty()) {
                createSampleAuctions(testUser, auctionRepository, basicUser, categoryRepository, userRepository);
            }
            if (auctionRepository.findByAuctionCreator(testUser).isEmpty()) {
                createSampleAuctions(defaultUser, auctionRepository, testUser, categoryRepository, userRepository);
            }

            if (auctionRepository.findByAuctionCreator(testUser).isEmpty()) {
                createSampleAuctions(defaultUser, auctionRepository, bottomUser, categoryRepository, userRepository);
            }

            auctionService.setCheapestAuctions(6);
            auctionService.setExpensiveAuctions(6);
        };
    }

    private AuctionUser createUserIfNotFound(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                             String email, String username, String password, String location, String role) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            AuctionUser user = new AuctionUser();
            user.setRole(role);
            user.setEmail(email);
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setVerified(true);
            user.setLocation(location);
            return userRepository.save(user);
        });
    }

    private void createSampleAuctions(AuctionUser auctionBuyer, AuctionRepository auctionRepository, AuctionUser user, CategoryRepository categoryRepository, UserRepository userRepository) throws Exception {
        String userIdentifier = user.getUsername().toUpperCase();
        Random random = new Random();

        // Example data for auctions
        Object[][] auctionData = {
                {"Vintage Camera", "A classic vintage camera in excellent condition.", "Cameras", "Electronics", "ACTIVE"},
                {"Designer Dress", "A stunning dress from a renowned fashion designer.", "Dresses", "Fashion", "ACTIVE"},
                {"Garden Shovel", "A durable shovel for gardening.", "Gardening Tools", "Home & Garden", "SOLD",},
                {"Antique Vase", "A beautiful antique vase, perfect for collectors.", "Antiques", "Collectibles & Art", "SOLD"},
                {"Signed Book", "A book signed by its famous author.", "Books", "Collectibles & Art", "ACTIVE"},
                {"Handmade Jewelry", "Exquisite handmade jewelry with unique design.", "Accessories", "Fashion", "ACTIVE"},
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

            long randomStartDays = ThreadLocalRandom.current().nextLong(-3, 0);
            LocalDateTime randomStartTime = now.plusDays(randomStartDays);

            LocalDateTime nowPlusShortMoment = LocalDateTime.now().plusHours(1).plusSeconds(30);
            long randomHours = ThreadLocalRandom.current().nextLong(-2, 4);
            LocalDateTime randomEndTime = nowPlusShortMoment.plusHours(randomHours);

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
                auction.setHighestBidder(auctionBuyer);
            }
            auction.setBuyNowPrice(startingPrice.add(BigDecimal.valueOf(50)));
            auction.setHighestBid(highestBid);

            String statusString = (String) auctionInfo[4];
            AuctionStatus statusEnum = AuctionStatus.valueOf(statusString);
            if (statusEnum == AuctionStatus.SOLD) {
                auction.setBuyer(auctionBuyer);
            }
            auction.setStatus(statusEnum);
            auction.setAuctionCreator(user);
            auction.setLocation(user.getLocation());
            if (!user.getObservedAuctions().contains(auction)) {
                user.getObservedAuctions().add(auction);
            }

            if (!auction.getObservers().contains(user)) {
                auction.getObservers().add(user);
            }
            String baseImageName = ((String) auctionInfo[0]).replaceAll("\\s+", "_");
            String originalImagePath = "auctionImages/" + baseImageName + ".png";
            String mirroredImagePath = "auctionImages/" + baseImageName + "_mirrored.png";
            String rotatedImagePath = "auctionImages/" + baseImageName + "_rotated.png";
            auction.setImageUrls(List.of(originalImagePath, mirroredImagePath, rotatedImagePath));

            auctionRepository.save(auction);
        }

        userRepository.save(user);
    }
}
