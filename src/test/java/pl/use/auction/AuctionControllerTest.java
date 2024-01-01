package pl.use.auction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.use.auction.controller.AuctionController;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.AuctionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
class AuctionControllerTest {

    @InjectMocks
    private AuctionController auctionController;

    @Mock
    private Model model;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private Authentication authentication;

    @Test
    void testCreateAuctionForm() {
        String viewName = auctionController.createAuctionForm(model);

        verify(model).addAttribute(eq("auction"), any(Auction.class));
        assertEquals("auctions/create-auction", viewName);
    }

    @Test
    void testCreateAuction_WithErrors() {
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = auctionController.createAuction(new Auction(), bindingResult, authentication);

        assertEquals("auctions/create-auction", viewName);
    }

    @Test
    void testCreateAuction_Successful() {
        Auction auction = new Auction();
        AuctionUser user = new AuctionUser();
        user.setEmail("test@example.com");

        when(bindingResult.hasErrors()).thenReturn(false);
        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        String viewName = auctionController.createAuction(auction, bindingResult, authentication);

        verify(userRepository).findByEmail("test@example.com");
        verify(auctionRepository).save(auction);
        assertEquals("redirect:/profile/auctions", viewName);
    }

    @Captor
    private ArgumentCaptor<List<Auction>> auctionListCaptor;

    @Test
    void testViewAllOngoingAuctions() {

        AuctionUser currentUser = new AuctionUser();
        currentUser.setEmail("user@example.com");
        Auction auction1 = createAuctionWithCreator("user@example.com");
        Auction auction2 = createAuctionWithCreator("other@example.com");

        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(currentUser));
        when(auctionRepository.findByEndTimeAfter(any(LocalDateTime.class)))
                .thenReturn(List.of(auction2));

        String viewName = auctionController.viewAllOngoingAuctions(model, authentication);

        verify(userRepository).findByEmail("user@example.com");
        verify(auctionRepository).findByEndTimeAfter(any(LocalDateTime.class));
        verify(model).addAttribute(eq("currentUser"), any(AuctionUser.class));
        assertEquals("auctions/all-auctions", viewName);

        verify(model).addAttribute(eq("ongoingAuctions"), auctionListCaptor.capture());
        List<Auction> capturedAuctions = auctionListCaptor.getValue();

        assertTrue(capturedAuctions.contains(auction2) && capturedAuctions.size() == 1);
        assertEquals("auctions/all-auctions", viewName);
    }

    private Auction createAuctionWithCreator(String creatorEmail) {
        Auction auction = new Auction();
        AuctionUser creator = new AuctionUser();
        creator.setEmail(creatorEmail);
        auction.setAuctionCreator(creator);
        return auction;
    }

    @Captor
    private ArgumentCaptor<Auction> auctionCaptor;

    @Captor
    private ArgumentCaptor<AuctionUser> userCaptor;
    @Test
    void testViewAuctionDetail() {
        Long auctionId = 1L;
        Auction auction = new Auction();
        auction.setId(auctionId);
        AuctionUser user = new AuctionUser();
        user.setEmail("test@example.com");

        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        String viewName = auctionController.viewAuctionDetail(auctionId, model, authentication);

        verify(userRepository).findByEmail("test@example.com");
        verify(auctionRepository).findById(auctionId);

        verify(model).addAttribute(eq("auction"), auctionCaptor.capture());
        assertEquals(auction, auctionCaptor.getValue());

        verify(model).addAttribute(eq("currentUser"), userCaptor.capture());
        assertEquals(user, userCaptor.getValue());

        assertEquals("auctions/auction-detail", viewName);
    }

    @Mock
    private AuctionService auctionService;

    @Mock
    private RedirectAttributes redirectAttributes;
    @Test
    void testAddToWatchlist() {
        Long auctionId = 1L;
        AuctionUser currentUser = new AuctionUser();
        currentUser.setEmail("test@example.com");
        Auction auction = new Auction();
        auction.setId(auctionId);

        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        ResponseEntity<?> response = auctionController.addToWatchlist(auctionId, authentication, redirectAttributes);

        verify(userRepository).findByEmail("test@example.com");
        verify(auctionRepository).findById(auctionId);
        verify(auctionService).addToWatchlist(currentUser, auction);
        verify(redirectAttributes).addFlashAttribute("watchMessage", "Added to watchlist.");

        assertEquals("Added to watchlist", response.getBody());
    }

    @Test
    void testRemoveFromWatchlist() {
        Long auctionId = 1L;
        AuctionUser currentUser = new AuctionUser();
        currentUser.setEmail("test@example.com");
        Auction auction = new Auction();
        auction.setId(auctionId);

        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        ResponseEntity<?> response = auctionController.removeFromWatchlist(auctionId, authentication, redirectAttributes);

        verify(userRepository).findByEmail("test@example.com");
        verify(auctionRepository).findById(auctionId);
        verify(auctionService).removeFromWatchlist(currentUser, auction);
        verify(redirectAttributes).addFlashAttribute("watchMessage", "Removed from watchlist.");

        assertEquals("Removed from watchlist", response.getBody());
    }

    @Test
    void testPlaceBid_Successful() {
        Long auctionId = 1L;
        BigDecimal bidAmount = new BigDecimal("100.00");
        AuctionUser bidder = new AuctionUser();
        bidder.setEmail("bidder@example.com");
        Auction auction = new Auction();
        auction.setId(auctionId);

        when(authentication.getName()).thenReturn("bidder@example.com");
        when(userRepository.findByEmail("bidder@example.com")).thenReturn(Optional.of(bidder));
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(auctionService.placeBid(auction, bidder, bidAmount)).thenReturn(true);

        String viewName = auctionController.placeBid(auctionId, bidAmount, authentication, redirectAttributes, null);

        verify(auctionRepository).findById(auctionId);
        verify(userRepository).findByEmail("bidder@example.com");
        verify(auctionService).placeBid(auction, bidder, bidAmount);
        verify(redirectAttributes).addFlashAttribute("successMessage", "Bid placed successfully!");

        assertEquals("redirect:/auctions/all", viewName);
    }

    @Test
    void testPlaceBid_Unsuccessful() {
        Long auctionId = 2L;
        BigDecimal bidAmount = new BigDecimal("100.00");
        AuctionUser bidder = new AuctionUser();
        bidder.setEmail("bidder@example.com");
        Auction auction = new Auction();
        auction.setId(auctionId);

        when(authentication.getName()).thenReturn("bidder@example.com");
        when(userRepository.findByEmail("bidder@example.com")).thenReturn(Optional.of(bidder));
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(auctionService.placeBid(auction, bidder, bidAmount)).thenReturn(false);

        String viewName = auctionController.placeBid(auctionId, bidAmount, authentication, redirectAttributes, null);

        verify(redirectAttributes).addFlashAttribute("errorMessage", "Bid not high enough!");
        assertEquals("redirect:/auctions/" + auctionId, viewName);
    }
}