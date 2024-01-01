package pl.use.auction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.AuctionService;

import java.math.BigDecimal;
import java.util.HashSet;
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
}