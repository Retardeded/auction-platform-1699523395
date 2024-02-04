package pl.use.auction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import pl.use.auction.model.Auction;
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
}