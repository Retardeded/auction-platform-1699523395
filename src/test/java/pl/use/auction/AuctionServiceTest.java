package pl.use.auction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Category;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.AuctionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private UserRepository userRepository;

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
    void testGetAggregatedAuctionsForCategory() {
        AuctionUser auctionCreator = new AuctionUser();
        auctionCreator.setEmail("creator@example.com");
        Category category = new Category();
        Category childCategory = new Category();
        category.setChildCategories(Set.of(childCategory));

        AuctionUser currentUser = new AuctionUser();
        Auction auctionInCategory = new Auction();
        auctionInCategory.setAuctionCreator(auctionCreator);
        auctionInCategory.setCategory(category);

        Auction auctionInChildCategory = new Auction();
        auctionInChildCategory.setAuctionCreator(auctionCreator);
        auctionInChildCategory.setCategory(childCategory);


        when(auctionRepository.findByCategoryAndEndTimeAfter(eq(category), any(LocalDateTime.class)))
                .thenReturn(List.of(auctionInCategory));
        when(auctionRepository.findByCategoryAndEndTimeAfter(eq(childCategory), any(LocalDateTime.class)))
                .thenReturn(List.of(auctionInChildCategory));

        List<Auction> result = auctionService.getAggregatedAuctionsForCategory(category, currentUser);

        assertTrue(result.contains(auctionInCategory) && result.contains(auctionInChildCategory));
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

        List<Auction> result = auctionService.findCheapestAuctions(currentUser, 2);

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

        List<Auction> result = auctionService.findExpensiveAuctions(currentUser, 2);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("100.00"), result.get(0).getHighestBid());
        assertEquals(new BigDecimal("75.00"), result.get(1).getHighestBid());
    }

}