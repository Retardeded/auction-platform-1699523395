package pl.use.auction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.stripe.exception.AuthenticationException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import pl.use.auction.model.*;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.NotificationRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.AuctionService;
import pl.use.auction.service.FileSystemStorageService;
import pl.use.auction.service.NotificationService;
import pl.use.auction.service.StripeServiceWrapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FileSystemStorageService fileSystemStorageService;

    @Mock
    private StripeServiceWrapper stripeServiceWrapper;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AuctionService auctionService;

    @Test
    public void updateStatusOfEndedAuctions() throws JsonProcessingException {
        Auction auctionWithBidder = new Auction();
        auctionWithBidder.setEndTime(LocalDateTime.now().minusDays(1));
        auctionWithBidder.setStatus(AuctionStatus.ACTIVE);
        auctionWithBidder.setTitle("Auction with bidder");
        auctionWithBidder.setHighestBidder(new AuctionUser());

        Auction auctionWithoutBidder = new Auction();
        auctionWithoutBidder.setEndTime(LocalDateTime.now().minusDays(1));
        auctionWithoutBidder.setStatus(AuctionStatus.ACTIVE);
        auctionWithoutBidder.setTitle("Auction without bidder");

        List<Auction> endedAuctions = Arrays.asList(auctionWithBidder, auctionWithoutBidder);

        when(auctionRepository.findByEndTimeBeforeAndStatus(any(LocalDateTime.class), any(AuctionStatus.class)))
                .thenReturn(endedAuctions);

        auctionService.updateStatusOfEndedAuctions();

        verify(auctionRepository).saveAll(endedAuctions);
        verify(notificationService, times(1)).createAndSendNotificationForEndedAuction(any(Auction.class));

        assertEquals(AuctionStatus.AWAITING_PAYMENT, auctionWithBidder.getStatus());
        assertEquals(AuctionStatus.EXPIRED, auctionWithoutBidder.getStatus());
    }

    @Test
    void proceedToPayment_Successful() throws StripeException {
        String auctionSlug = "test-slug";
        BigDecimal auctionPrice = new BigDecimal("100.00");

        Auction auction = new Auction();
        auction.setSlug(auctionSlug);

        Session session = mock(Session.class);
        when(session.getUrl()).thenReturn("http://example.com/payment");

        when(auctionRepository.findBySlug(auctionSlug)).thenReturn(Optional.of(auction));

        when(stripeServiceWrapper.createCheckoutSession(any(SessionCreateParams.class))).thenReturn(session);

        ResponseEntity<?> response = auctionService.proceedToPayment(auctionSlug, auctionPrice);

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals("http://example.com/payment", response.getHeaders().getLocation().toString());
    }

    @Test
    void proceedToPayment_StripeException() throws Exception {
        String auctionSlug = "test-slug";
        BigDecimal auctionPrice = new BigDecimal("100.00");
        Auction auction = new Auction();
        auction.setSlug(auctionSlug);

        AuthenticationException stripeException = new AuthenticationException("Stripe error", null, null, null);

        when(auctionRepository.findBySlug(auctionSlug)).thenReturn(Optional.of(auction));
        when(stripeServiceWrapper.createCheckoutSession(any(SessionCreateParams.class)))
                .thenThrow(stripeException);

        ResponseEntity<?> response = auctionService.proceedToPayment(auctionSlug, auctionPrice);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Error creating Stripe Checkout session"));
    }

    @Test
    void proceedToPayment_IllegalArgumentException() {
        String auctionSlug = "invalid-slug";
        BigDecimal auctionPrice = new BigDecimal("100.00");

        when(auctionRepository.findBySlug(auctionSlug)).thenThrow(new IllegalArgumentException("Invalid auction slug: " + auctionSlug));

        ResponseEntity<?> response = auctionService.proceedToPayment(auctionSlug, auctionPrice);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Invalid auction slug: " + auctionSlug));
    }

    @Test
    void createCheckoutSession_Successful() throws Exception {
        String auctionSlug = "test-auction";
        BigDecimal auctionPrice = new BigDecimal("100.00");
        Auction auction = new Auction();
        auction.setSlug(auctionSlug);
        auction.setCurrencyCode(CurrencyCode.USD); // Assuming CurrencyCode is an enum or similar

        Session mockSession = mock(Session.class);
        when(mockSession.getUrl()).thenReturn("http://example.com/checkout");

        when(stripeServiceWrapper.createCheckoutSession(any(SessionCreateParams.class))).thenReturn(mockSession);

        Session resultSession = auctionService.createCheckoutSession(auction, auctionPrice);

        assertNotNull(resultSession);
        assertEquals("http://example.com/checkout", resultSession.getUrl());
    }

    @Test
    void handleCheckoutSessionCreation_AuctionNotFound() {
        String auctionSlug = "nonexistent-auction";
        BigDecimal auctionPrice = new BigDecimal("100.00");

        when(auctionRepository.findBySlug(auctionSlug)).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            Auction auction = auctionRepository.findBySlug(auctionSlug)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid auction slug: " + auctionSlug));
            auctionService.createCheckoutSession(auction, auctionPrice);
        });

        assertEquals("Invalid auction slug: " + auctionSlug, exception.getMessage());
    }

    @Test
    public void testCreatePaymentIntent() throws StripeException {
        BigDecimal buyNowPrice = new BigDecimal("100.00");
        String currency = "USD";
        String expectedClientSecret = "some_client_secret";

        when(stripeServiceWrapper.createPaymentIntent(buyNowPrice, currency)).thenReturn(expectedClientSecret);

        String clientSecret = auctionService.createPaymentIntent(buyNowPrice, currency);

        assertEquals(expectedClientSecret, clientSecret);
        verify(stripeServiceWrapper).createPaymentIntent(buyNowPrice, currency);
    }

    @Test
    void finalizeAuctionSale_Successful() {
        String auctionSlug = "valid-auction";
        AuctionUser buyer = new AuctionUser();
        BigDecimal finalPrice = new BigDecimal("100.00");
        Auction auction = new Auction();
        auction.setStatus(AuctionStatus.ACTIVE); // Ensure the auction is not already sold

        when(auctionRepository.findBySlug(auctionSlug)).thenReturn(Optional.of(auction));

        auctionService.finalizeAuctionSale(auctionSlug, buyer, finalPrice);

        assertEquals(AuctionStatus.SOLD, auction.getStatus());
        assertEquals(buyer, auction.getBuyer());
        assertEquals(finalPrice, auction.getHighestBid());
        verify(auctionRepository).save(auction);
    }

    @Test
    void finalizeAuctionSale_AuctionNotFound() {
        String auctionSlug = "nonexistent-auction";
        AuctionUser buyer = new AuctionUser(); // Assume initialized
        BigDecimal finalPrice = new BigDecimal("100.00");

        when(auctionRepository.findBySlug(auctionSlug)).thenReturn(Optional.empty());

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            auctionService.finalizeAuctionSale(auctionSlug, buyer, finalPrice);
        });

        assertEquals("Invalid auction slug: " + auctionSlug, exception.getMessage());
    }

    @Test
    void finalizeAuctionSale_AuctionAlreadySold() {
        String auctionSlug = "sold-auction";
        AuctionUser buyer = new AuctionUser(); // Assume initialized
        BigDecimal finalPrice = new BigDecimal("100.00");
        Auction auction = new Auction();
        auction.setStatus(AuctionStatus.SOLD);

        when(auctionRepository.findBySlug(auctionSlug)).thenReturn(Optional.of(auction));

        Exception exception = assertThrows(IllegalStateException.class, () -> {
            auctionService.finalizeAuctionSale(auctionSlug, buyer, finalPrice);
        });

        assertEquals("This auction is already sold.", exception.getMessage());
    }

    @Test
    void testPlaceBid() {
        Auction auction = mock(Auction.class);
        AuctionUser bidder = mock(AuctionUser.class);
        BigDecimal newBidAmount = new BigDecimal("100.00");
        BigDecimal currentHighestBid = new BigDecimal("90.00");

        when(auction.getHighestBid()).thenReturn(currentHighestBid);
        boolean result = auctionService.placeBid(auction, bidder, newBidAmount);

        assertTrue(result);
        verify(auction).setHighestBid(newBidAmount);
        verify(auction).setHighestBidder(bidder);
        verify(auctionRepository).save(auction);

        if (!bidder.getObservedAuctions().contains(auction)) {
            verify(userRepository).save(bidder);
        }
    }

    @Test
    void testPlaceBid_UnsuccessfulDueToLowAmount() {
        Auction auction = mock(Auction.class);
        AuctionUser bidder = mock(AuctionUser.class);
        BigDecimal newBidAmount = new BigDecimal("80.00");
        BigDecimal currentHighestBid = new BigDecimal("90.00");

        when(auction.getHighestBid()).thenReturn(currentHighestBid);
        boolean result = auctionService.placeBid(auction, bidder, newBidAmount);

        assertFalse(result);
        verify(auction, never()).setHighestBid(newBidAmount);
        verify(auction, never()).setHighestBidder(bidder);
        verify(auctionRepository, never()).save(auction);
        verify(userRepository, never()).save(bidder);
    }

    @Test
    void testAddToWatchlist() {
        AuctionUser user = mock(AuctionUser.class);
        Auction auction = mock(Auction.class);
        Set<Auction> observedAuctions = new HashSet<>();

        when(user.getObservedAuctions()).thenReturn(observedAuctions);

        auctionService.addToWatchlist(user, auction);

        assertTrue(observedAuctions.contains(auction));
        verify(userRepository).save(user);
    }

    @Test
    void testRemoveFromWatchlist() {
        AuctionUser user = mock(AuctionUser.class);
        Auction auction = mock(Auction.class);
        Set<Auction> observedAuctions = new HashSet<>();
        observedAuctions.add(auction);

        when(user.getObservedAuctions()).thenReturn(observedAuctions);

        auctionService.removeFromWatchlist(user, auction);

        assertFalse(observedAuctions.contains(auction));
        verify(userRepository).save(user);
    }

    @Test
    void testFindCheapestAuctions() {
        // Arrange
        AuctionUser auctionCreator1 = new AuctionUser();
        AuctionUser auctionCreator2 = new AuctionUser();
        AuctionUser auctionCreator3 = new AuctionUser();

        Auction auction1 = new Auction();
        auction1.setEndTime(LocalDateTime.now().plusDays(1));
        auction1.setStatus(AuctionStatus.ACTIVE);
        auction1.setHighestBid(new BigDecimal("100.00"));
        auction1.setAuctionCreator(auctionCreator1);

        Auction auction2 = new Auction();
        auction2.setEndTime(LocalDateTime.now().plusDays(1));
        auction2.setStatus(AuctionStatus.ACTIVE);
        auction2.setHighestBid(new BigDecimal("50.00"));
        auction2.setAuctionCreator(auctionCreator2);

        Auction auction3 = new Auction();
        auction3.setEndTime(LocalDateTime.now().plusDays(1));
        auction3.setStatus(AuctionStatus.ACTIVE);
        auction3.setHighestBid(new BigDecimal("75.00"));
        auction3.setAuctionCreator(auctionCreator3);

        List<Auction> auctions = Arrays.asList(auction1, auction2, auction3);

        when(auctionRepository.findByEndTimeAfterAndStatusNot(any(LocalDateTime.class), any(AuctionStatus.class)))
                .thenReturn(auctions);

        List<Auction> result = auctionService.setCheapestAuctions(2);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("50.00"), result.get(0).getHighestBid());
        assertEquals(new BigDecimal("75.00"), result.get(1).getHighestBid());
    }


    @Test
    void testFindExpensiveAuctions() {
        AuctionUser auctionCreator1 = new AuctionUser();
        AuctionUser auctionCreator2 = new AuctionUser();
        AuctionUser auctionCreator3 = new AuctionUser();

        Auction auction1 = new Auction();
        auction1.setHighestBid(new BigDecimal("100.00"));
        auction1.setAuctionCreator(auctionCreator1);
        auction1.setEndTime(LocalDateTime.now().plusDays(1));
        auction1.setStatus(AuctionStatus.ACTIVE);

        Auction auction2 = new Auction();
        auction2.setHighestBid(new BigDecimal("50.00"));
        auction2.setAuctionCreator(auctionCreator2);
        auction2.setEndTime(LocalDateTime.now().plusDays(1));
        auction2.setStatus(AuctionStatus.ACTIVE);

        Auction auction3 = new Auction();
        auction3.setHighestBid(new BigDecimal("75.00"));
        auction3.setAuctionCreator(auctionCreator3);
        auction3.setEndTime(LocalDateTime.now().plusDays(1));
        auction3.setStatus(AuctionStatus.ACTIVE);

        List<Auction> auctions = Arrays.asList(auction1, auction2, auction3);

        when(auctionRepository.findByEndTimeAfterAndStatusNot(any(LocalDateTime.class), eq(AuctionStatus.SOLD)))
                .thenReturn(auctions);

        AuctionUser currentUser = new AuctionUser();

        List<Auction> result = auctionService.setExpensiveAuctions(2);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("100.00"), result.get(0).getHighestBid());
        assertEquals(new BigDecimal("75.00"), result.get(1).getHighestBid());
    }

    @Test
    void testCreateAndSaveAuction() throws IOException {
        Auction auction = new Auction();
        auction.setTitle("Vintage Camera");
        auction.setDescription("A classic camera in good condition.");
        auction.setStartingPrice(new BigDecimal("50.00"));
        auction.setEndTime(LocalDateTime.now().plusDays(7));

        Long categoryId = 1L;
        Category category = new Category();
        category.setId(categoryId);

        AuctionUser user = new AuctionUser();
        user.setEmail("user@example.com");

        MultipartFile[] files = {mock(MultipartFile.class)};
        when(files[0].isEmpty()).thenReturn(false);
        String uploadDir = "src/main/resources/static/auctionImages/";
        String expectedFilePath = "auctionImages/image.jpg";

        when(files[0].isEmpty()).thenReturn(false);
        when(fileSystemStorageService.save(any(MultipartFile.class), eq(uploadDir))).thenReturn(expectedFilePath);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Auction savedAuction = auctionService.createAndSaveAuction(auction, categoryId, files, "user@example.com");

        assertNotNull(savedAuction);
        assertTrue(savedAuction.getImageUrls().contains(expectedFilePath));
        assertEquals(category, savedAuction.getCategory());
        assertEquals(user, savedAuction.getAuctionCreator());
        assertFalse(savedAuction.getImageUrls().isEmpty());
        assertEquals("Vintage Camera", savedAuction.getTitle());
        assertEquals("A classic camera in good condition.", savedAuction.getDescription());
        assertTrue(savedAuction.getStartTime().isBefore(savedAuction.getEndTime()));
        assertEquals(new BigDecimal("50.00"), savedAuction.getStartingPrice());
        assertEquals(BigDecimal.ZERO, savedAuction.getHighestBid());
        assertEquals(AuctionStatus.ACTIVE, savedAuction.getStatus());
        assertNotNull(savedAuction.getSlug());

        String expectedImagePath = "auctionImages/image.jpg";
        assertTrue(savedAuction.getImageUrls().contains(expectedImagePath));

        verify(auctionRepository).save(savedAuction);
        verify(fileSystemStorageService).save(any(MultipartFile.class), eq(uploadDir));
    }

    @Test
    void testUpdateAuction() throws IOException {
        String slug = "vintage-camera";
        Auction existingAuction = new Auction();
        existingAuction.setSlug(slug);
        existingAuction.setImageUrls(new ArrayList<>(List.of("existingImage.jpg")));

        Auction auctionDetails = new Auction();
        auctionDetails.setDescription("Updated description");
        auctionDetails.setStartingPrice(new BigDecimal("75.00"));
        auctionDetails.setEndTime(LocalDateTime.now().plusDays(10));

        Category category = new Category();
        category.setId(2L);
        auctionDetails.setCategory(category);

        MultipartFile newImage = mock(MultipartFile.class);
        when(newImage.isEmpty()).thenReturn(false);

        MultipartFile[] newImages = {newImage};
        List<String> imagesToDelete = List.of("existingImage.jpg");

        when(auctionRepository.findBySlug(slug)).thenReturn(Optional.of(existingAuction));
        when(fileSystemStorageService.save(newImage, "src/main/resources/static/auctionImages/")).thenReturn("auctionImages/newImage.jpg");
        when(auctionRepository.save(existingAuction)).thenAnswer(invocation -> invocation.getArgument(0));

        Auction updatedAuction = auctionService.updateAuction(slug, auctionDetails, newImages, imagesToDelete);

        assertNotNull(updatedAuction);
        assertEquals("Updated description", updatedAuction.getDescription());
        assertEquals(new BigDecimal("75.00"), updatedAuction.getStartingPrice());
        assertTrue(updatedAuction.getEndTime().isAfter(LocalDateTime.now()));
        assertTrue(updatedAuction.getImageUrls().contains("auctionImages/newImage.jpg"));
        assertFalse(updatedAuction.getImageUrls().contains("existingImage.jpg"));
        assertEquals(category, updatedAuction.getCategory());

        verify(auctionRepository).findBySlug(slug);
        verify(fileSystemStorageService).save(newImage, "src/main/resources/static/auctionImages/");
        verify(auctionRepository).save(updatedAuction);
    }

    @Test
    void searchAuctions_WithoutCategory() {
        String query = "camera";
        String location = "";
        AuctionStatus activeStatus = AuctionStatus.ACTIVE;
        List<Auction> expectedAuctions = List.of(new Auction());

        when(auctionRepository.findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndStatus(
                eq(query), eq(location), eq(activeStatus)))
                .thenReturn(expectedAuctions);

        List<Auction> result = auctionService.searchAuctions(query, location, "", "date");

        assertEquals(expectedAuctions, result);
        verify(auctionRepository).findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndStatus(
                eq(query), eq(location), eq(activeStatus));
    }

    @Test
    void searchAuctions_WithValidCategory() {
        String query = "camera";
        String location = "";
        String categoryName = "Electronics";
        AuctionStatus activeStatus = AuctionStatus.ACTIVE;
        Category category = new Category();
        category.setId(1L);
        category.setName(categoryName);
        category.setChildCategories(new HashSet<>());
        List<Auction> expectedAuctions = List.of(new Auction());

        when(categoryRepository.findByName(categoryName)).thenReturn(Optional.of(category));
        when(auctionRepository.findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndCategoryIdInAndStatus(
                eq(query), eq(location), anyList(), eq(activeStatus)))
                .thenReturn(expectedAuctions);

        List<Auction> result = auctionService.searchAuctions(query, location, categoryName, "date");

        assertEquals(expectedAuctions, result);
        verify(categoryRepository).findByName(categoryName);
        verify(auctionRepository).findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndCategoryIdInAndStatus(
                eq(query), eq(location), anyList(), eq(activeStatus));
    }

    @Test
    void searchAuctions_SortByDate() {
        AuctionStatus activeStatus = AuctionStatus.ACTIVE;
        List<Auction> auctions = IntStream.range(0, 3)
                .mapToObj(i -> {
                    Auction auction = new Auction();
                    auction.setStartTime(LocalDateTime.now().minusDays(i));
                    auction.setStatus(activeStatus);
                    return auction;
                })
                .collect(Collectors.toList());

        when(auctionRepository.findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndStatus(
                anyString(), anyString(), eq(activeStatus)))
                .thenReturn(auctions);

        List<Auction> result = auctionService.searchAuctions("", "", "", "date");

        assertTrue(result.get(0).getStartTime().isAfter(result.get(1).getStartTime()));
        assertTrue(result.get(1).getStartTime().isAfter(result.get(2).getStartTime()));
    }

    @Test
    void searchAuctions_FilterByQuery() {
        AuctionStatus activeStatus = AuctionStatus.ACTIVE;
        Category electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");

        Auction matchingAuction = new Auction();
        matchingAuction.setTitle("Camera");
        matchingAuction.setLocation("New York");
        matchingAuction.setCategory(electronics);
        matchingAuction.setStartTime(LocalDateTime.now().plusDays(5));
        matchingAuction.setEndTime(LocalDateTime.now().plusDays(5));
        matchingAuction.setStartingPrice(new BigDecimal("100.00"));
        matchingAuction.setHighestBid(new BigDecimal("150.00"));

        Auction nonMatchingAuction = new Auction();
        nonMatchingAuction.setTitle("Laptop");
        nonMatchingAuction.setLocation("San Francisco");
        nonMatchingAuction.setCategory(electronics);
        nonMatchingAuction.setStartTime(LocalDateTime.now().plusDays(10));
        nonMatchingAuction.setEndTime(LocalDateTime.now().plusDays(10));
        nonMatchingAuction.setStartingPrice(new BigDecimal("500.00"));
        nonMatchingAuction.setHighestBid(new BigDecimal("700.00"));

        when(auctionRepository.findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndStatus(
                anyString(), anyString(), eq(activeStatus)))
                .thenReturn(List.of(matchingAuction));

        String query = "Camera";
        List<Auction> result = auctionService.searchAuctions(query, "", "", "date");

        assertEquals(1, result.size(), "Only one auction should match the query.");
        assertTrue(result.contains(matchingAuction), "The result should contain the matching auction.");
        assertFalse(result.contains(nonMatchingAuction), "The result should not contain the non-matching auction.");

        verify(auctionRepository).findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndStatus(
                eq(query), eq(""), eq(activeStatus));
    }

    @Test
    void searchAuctions_FilterByQueryAndCategory() {
        AuctionStatus activeStatus = AuctionStatus.ACTIVE;
        Category electronics = new Category();
        electronics.setId(1L);
        electronics.setName("Electronics");
        electronics.setChildCategories(Collections.emptySet()); // Assuming no child categories for simplicity

        Auction matchingAuction = new Auction();
        matchingAuction.setTitle("Camera");
        matchingAuction.setLocation("New York");
        matchingAuction.setCategory(electronics);
        matchingAuction.setStartTime(LocalDateTime.now().plusDays(5));
        matchingAuction.setEndTime(LocalDateTime.now().plusDays(5));
        matchingAuction.setStartingPrice(new BigDecimal("100.00"));
        matchingAuction.setHighestBid(new BigDecimal("150.00"));

        when(categoryRepository.findByName("Electronics")).thenReturn(Optional.of(electronics));
        when(auctionRepository.findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndCategoryIdInAndStatus(
                eq("Camera"), eq(""), anyList(), eq(activeStatus)))
                .thenReturn(List.of(matchingAuction));

        List<Auction> result = auctionService.searchAuctions("Camera", "", "Electronics", "date");

        assertEquals(1, result.size());
        assertTrue(result.contains(matchingAuction));
        verify(categoryRepository).findByName("Electronics");
        verify(auctionRepository).findByTitleContainingIgnoreCaseAndLocationContainingIgnoreCaseAndCategoryIdInAndStatus(
                eq("Camera"), eq(""), anyList(), eq(activeStatus));
    }
}