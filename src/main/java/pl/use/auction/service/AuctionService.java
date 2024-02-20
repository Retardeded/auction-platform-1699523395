package pl.use.auction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    @Autowired NotificationService notificationService;

    @Value("${stripe.api.publishablekey}")
    private String stripePublishableKey;

    @Value("${app.url}")
    private String appUrl;

    @Autowired
    private StripeServiceWrapper stripeServiceWrapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final Map<String, LocalDateTime> notificationSentTimestamps = new ConcurrentHashMap<>();

    public ResponseEntity<?> proceedToPayment(String auctionSlug, BigDecimal auctionPrice) {
        try {
            Auction auction = auctionRepository.findBySlug(auctionSlug)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid auction slug: " + auctionSlug));

            Session session = createCheckoutSession(auction, auctionPrice);
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(session.getUrl())).build();
        } catch (StripeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error creating Stripe Checkout session: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    public Session createCheckoutSession(Auction auction, BigDecimal auctionPrice) throws StripeException {

        String successUrl = appUrl + "/payment/success?session_id={CHECKOUT_SESSION_ID}";
        String cancelUrl = appUrl + "/auction/" + auction.getSlug();

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("auction_slug", auction.getSlug())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(auction.getCurrencyCode().toString().toLowerCase())
                                .setUnitAmount(auctionPrice.multiply(new BigDecimal(100)).longValue()) // Convert to cents
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Auction: " + auction.getSlug())
                                        .build())
                                .build())
                        .build())
                .build();

        return stripeServiceWrapper.createCheckoutSession(params);
    }

    public String createPaymentIntent(BigDecimal buyNowPrice, String currency) throws StripeException {
        return stripeServiceWrapper.createPaymentIntent(buyNowPrice, currency);
    }

    public void finalizeAuctionSale(String auctionSlug, AuctionUser buyer, BigDecimal finalPrice) {
        Auction auction = auctionRepository.findBySlug(auctionSlug)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction slug: " + auctionSlug));

        if (auction.getStatus() != AuctionStatus.SOLD) {
            auction.setStatus(AuctionStatus.SOLD);
            auction.setBuyer(buyer);
            auction.setHighestBid(finalPrice);
            auctionRepository.save(auction);
        } else {
            throw new IllegalStateException("This auction is already sold.");
        }
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

    @Scheduled(fixedRate = 3 * 60 * 60 * 1000)
    public void clearSentNotifications() {
        notificationSentTimestamps.clear();
    }

    @Scheduled(fixedRate = 60000) // This will run the method every 60 seconds.
    @Transactional
    public void updateStatusOfEndedAuctions() throws JsonProcessingException {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> endedAuctions = auctionRepository.findByEndTimeBeforeAndStatus(now, AuctionStatus.ACTIVE);
        List<Auction> endingSoonAuctions = auctionRepository.findByEndTimeBetweenAndStatus(now, now.plusHours(5), AuctionStatus.ACTIVE);

        for (Auction auction : endedAuctions) {
            if (auction.getHighestBidder() != null) {
                auction.setStatus(AuctionStatus.AWAITING_PAYMENT);
                notificationService.createAndSendNotificationForEndedAuction(auction);
            } else {
                auction.setStatus(AuctionStatus.EXPIRED);
            }
        }

        for (Auction auction : endingSoonAuctions) {
            for (AuctionUser observer : auction.getObservers()) {
                String key = auction.getId() + ":" + observer.getId();
                LocalDateTime lastSent = notificationSentTimestamps.get(key);

                if (lastSent == null || now.isAfter(lastSent.plusHours(3))) {
                    notificationService.createAndSendEndingSoonNotification(auction, observer);
                    notificationSentTimestamps.put(key, now);
                }
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
        auction.setImageUrls(new ArrayList<>());

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String imageUrl = saveImage(file);
                auction.getImageUrls().add(imageUrl);
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
