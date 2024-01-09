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
import pl.use.auction.model.Category;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.AuctionService;
import pl.use.auction.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private CategoryRepository categoryRepository;

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

    @Test
    void testViewCategory() {
        String categoryName = "electronics";
        Category parentCategory = new Category(); // set necessary properties
        AuctionUser currentUser = new AuctionUser();
        currentUser.setEmail("user@example.com");
        List<Auction> aggregatedAuctions = List.of(new Auction()); // create mock auctions

        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(currentUser));
        when(categoryRepository.findByNameIgnoreCase(StringUtils.slugToCategoryName(categoryName)))
                .thenReturn(Optional.of(parentCategory));
        when(auctionService.getAggregatedAuctionsForCategory(parentCategory, currentUser))
                .thenReturn(aggregatedAuctions);

        String viewName = auctionController.viewCategory(categoryName, model, authentication);

        verify(categoryRepository).findByNameIgnoreCase(StringUtils.slugToCategoryName(categoryName));
        verify(userRepository).findByEmail("user@example.com");
        verify(auctionService).getAggregatedAuctionsForCategory(parentCategory, currentUser);

        assertEquals("auctions/category", viewName);
        verify(model).addAttribute(eq("currentUser"), eq(currentUser));
        verify(model).addAttribute(eq("category"), eq(parentCategory));
        verify(model).addAttribute(eq("categoryAuctions"), eq(aggregatedAuctions));
    }

    @Captor
    private ArgumentCaptor<Auction> auctionCaptor;

    @Captor
    private ArgumentCaptor<AuctionUser> userCaptor;
    @Test
    void testViewAuctionDetail() {
        String auctionSlug = "some-auction-slug";
        Auction auction = new Auction();
        auction.setSlug(auctionSlug);
        Category category = new Category();
        category.setName("Category Name");
        Category parentCategory = new Category();
        parentCategory.setName("Parent Category Name");
        category.setParentCategory(parentCategory);
        auction.setCategory(category);

        AuctionUser user = new AuctionUser();
        user.setEmail("test@example.com");

        when(authentication.getName()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(auctionRepository.findBySlug(auctionSlug)).thenReturn(Optional.of(auction));

        String viewName = auctionController.viewAuctionDetail(auctionSlug, model, authentication);

        verify(userRepository).findByEmail("test@example.com");
        verify(auctionRepository).findBySlug(auctionSlug);

        verify(model).addAttribute(eq("auction"), auctionCaptor.capture());
        assertEquals(auction, auctionCaptor.getValue());

        verify(model).addAttribute(eq("currentUser"), userCaptor.capture());
        assertEquals(user, userCaptor.getValue());

        ArgumentCaptor<Auction> auctionCaptor = ArgumentCaptor.forClass(Auction.class);
        verify(model).addAttribute(eq("auction"), auctionCaptor.capture());
        Auction capturedAuction = auctionCaptor.getValue();
        assertNotNull(capturedAuction.getCategory());
        assertEquals("Category Name", capturedAuction.getCategory().getName());
        assertNotNull(capturedAuction.getCategory().getParentCategory());
        assertEquals("Parent Category Name", capturedAuction.getCategory().getParentCategory().getName());

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