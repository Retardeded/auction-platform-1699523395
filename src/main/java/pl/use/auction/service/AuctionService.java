package pl.use.auction.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pl.use.auction.model.*;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.NotificationRepository;
import pl.use.auction.repository.UserRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pl.use.auction.util.StringUtils.createSlugFromTitle;

@Service
public class AuctionService {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FileSystemStorageService fileSystemStorageService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Value("${stripe.api.secretkey}")
    private String stripeApiKey;

    public String createPaymentIntent(BigDecimal buyNowPrice, String currency) throws StripeException {
        Stripe.apiKey = stripeApiKey;

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(buyNowPrice.multiply(new BigDecimal(100)).longValue()) // convert price to the smallest currency unit, e.g., cents
                .setCurrency(currency)
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);

        return paymentIntent.getClientSecret();
    }

    public boolean placeBid(Auction auction, AuctionUser bidder, BigDecimal bidAmount) {
        boolean bidPlaced = false;
        if (bidAmount.compareTo(auction.getHighestBid()) > 0) {
            auction.setHighestBid(bidAmount);
            auction.setHighestBidder(bidder);
            addToWatchlist(bidder, auction);

            auctionRepository.save(auction);
            bidPlaced = true;
        }
        return bidPlaced;
    }

    public void addToWatchlist(AuctionUser user, Auction auction) {
        user.getObservedAuctions().add(auction);
        userRepository.save(user);
    }

    public void removeFromWatchlist(AuctionUser user, Auction auction) {
        user.getObservedAuctions().remove(auction);
        userRepository.save(user);
    }

    public List<Auction> findCheapestAuctions(int limit) {
        List<Auction> auctions = auctionRepository.findByEndTimeAfterAndStatusNot(LocalDateTime.now(), AuctionStatus.SOLD).stream()
                .sorted(Comparator.comparing(Auction::getHighestBid))
                .limit(limit)
                .collect(Collectors.toList());

        auctions.forEach(auction -> auction.setFeaturedType(FeaturedType.CHEAP));
        auctionRepository.saveAll(auctions);
        return auctions;
    }

    public List<Auction> findExpensiveAuctions(int limit) {
        List<Auction> auctions = auctionRepository.findByEndTimeAfterAndStatusNot(LocalDateTime.now(), AuctionStatus.SOLD).stream()
                .sorted(Comparator.comparing(Auction::getHighestBid).reversed())
                .limit(limit)
                .collect(Collectors.toList());

        auctions.forEach(auction -> auction.setFeaturedType(FeaturedType.EXPENSIVE));
        auctionRepository.saveAll(auctions);
        return auctions;
    }

    @Scheduled(fixedRate = 60000) // This will run the method every 60 seconds.
    public void updateStatusOfEndedAuctions() {
        List<Auction> endedAuctions = auctionRepository.findByEndTimeBeforeAndStatus(LocalDateTime.now(), AuctionStatus.ACTIVE);
        for (Auction auction : endedAuctions) {
            if (auction.getHighestBidder() != null) {
                auction.setStatus(AuctionStatus.AWAITING_PAYMENT);

                Notification notification = new Notification();
                notification.setUser(auction.getHighestBidder());
                notification.setDescription("Congratulations! You are the highest bidder for the auction: " + auction.getTitle());
                notification.setAction("Please see your bids to confirm your transaction.");
                notification.setRead(false);
                notificationRepository.save(notification);
            } else {
                auction.setStatus(AuctionStatus.EXPIRED);
            }
        }
        auctionRepository.saveAll(endedAuctions);
    }

    public Auction createAndSaveAuction(Auction auction,
                                        Long categoryId,
                                        MultipartFile[] files,
                                        String username) throws IOException {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
        AuctionUser user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        auction.setCategory(category);
        auction.setAuctionCreator(user);
        auction.setStartTime(LocalDateTime.now());
        auction.setEndTime(auction.getEndTime());
        auction.setStartingPrice(auction.getStartingPrice());
        auction.setHighestBid(BigDecimal.valueOf(0));
        auction.setStatus(AuctionStatus.valueOf("ACTIVE"));
        auction.setSlug(createSlugFromTitle(auction.getTitle()));
        auction.setImageUrls(new ArrayList<>()); // Make sure the list is initialized

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String imageUrl = saveImage(file);
                auction.getImageUrls().add(imageUrl); // Add the image URL to the list
            }
        }

        return auctionRepository.save(auction);
    }

    public Auction updateAuction(String slug,
                                 Auction auctionDetails,
                                 MultipartFile[] newImages,
                                 List<String> imagesToDelete) throws IOException {
        Auction existingAuction = auctionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));

        existingAuction.setDescription(auctionDetails.getDescription());
        existingAuction.setCategory(auctionDetails.getCategory());
        existingAuction.setStartingPrice(auctionDetails.getStartingPrice());
        existingAuction.setEndTime(auctionDetails.getEndTime());

        for (MultipartFile file : newImages) {
            if (!file.isEmpty()) {
                String imageUrl = saveImage(file);
                existingAuction.getImageUrls().add(imageUrl);
            }
        }

        if (imagesToDelete != null) {
            existingAuction.getImageUrls().removeAll(imagesToDelete);
        }

        return auctionRepository.save(existingAuction);
    }

    public String saveImage(MultipartFile file) throws IOException {
        return fileSystemStorageService.save(file, "src/main/resources/static/auctionImages/");
    }

    public List<Auction> searchAuctions(String query, String location, String categoryName, String sort) {
        List<Auction> auctions = new ArrayList<>();
        Category category = null;
        AuctionStatus activeStatus = AuctionStatus.ACTIVE;

        if (categoryName != null && !categoryName.trim().isEmpty()) {
            Optional<Category> optionalCategory = categoryRepository.findByName(categoryName);
            if (optionalCategory.isPresent()) {
                category = optionalCategory.get();
                List<Long> categoryIds = Stream.concat(
                        Stream.of(category.getId()),
                        category.getChildCategories().stream().map(Category::getId)
                ).collect(Collectors.toList());

                auctions = auctionRepository.findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndCategoryIdInAndStatus
                        (query, location, categoryIds, activeStatus);
            }
        } else {
            auctions = auctionRepository.findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndStatus
                    (query, location, activeStatus);
        }

        return switch (sort) {
            case "date" -> auctions.stream()
                    .sorted(Comparator.comparing(Auction::getStartTime).reversed())
                    .collect(Collectors.toList());
            case "currentBid" -> auctions.stream()
                    .sorted(Comparator.comparing(Auction::getHighestBid))
                    .collect(Collectors.toList());
            case "endingSoon" -> auctions.stream()
                    .sorted(Comparator.comparing(Auction::getEndTime))
                    .collect(Collectors.toList());
            default -> auctions;
        };
    }
}
