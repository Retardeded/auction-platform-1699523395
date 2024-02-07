package pl.use.auction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionStatus;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Category;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.AuctionService;
import pl.use.auction.service.FileSystemStorageService;

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

    @InjectMocks
    private AuctionService auctionService;

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
        AuctionUser auctionCreator1 = new AuctionUser();
        AuctionUser auctionCreator2 = new AuctionUser();
        AuctionUser auctionCreator3 = new AuctionUser();

        Auction auction1 = new Auction();
        auction1.setHighestBid(new BigDecimal("100.00"));
        auction1.setAuctionCreator(auctionCreator1);

        Auction auction2 = new Auction();
        auction2.setHighestBid(new BigDecimal("50.00"));
        auction2.setAuctionCreator(auctionCreator2);

        Auction auction3 = new Auction();
        auction3.setHighestBid(new BigDecimal("75.00"));
        auction3.setAuctionCreator(auctionCreator3);

        List<Auction> auctions = Arrays.asList(auction1, auction2, auction3);

        when(auctionRepository.findByEndTimeAfter(any(LocalDateTime.class))).thenReturn(auctions);

        AuctionUser currentUser = new AuctionUser();

        List<Auction> result = auctionService.findCheapestAuctions(2);

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

        Auction auction2 = new Auction();
        auction2.setHighestBid(new BigDecimal("50.00"));
        auction2.setAuctionCreator(auctionCreator2);

        Auction auction3 = new Auction();
        auction3.setHighestBid(new BigDecimal("75.00"));
        auction3.setAuctionCreator(auctionCreator3);

        List<Auction> auctions = Arrays.asList(auction1, auction2, auction3);

        when(auctionRepository.findByEndTimeAfter(any(LocalDateTime.class))).thenReturn(auctions);

        AuctionUser currentUser = new AuctionUser();

        List<Auction> result = auctionService.findExpensiveAuctions(2);

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
        assertEquals("ONGOING", savedAuction.getStatus());
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