package pl.use.auction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import pl.use.auction.controller.AuctionController;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Category;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.AuctionService;
import pl.use.auction.service.CategoryService;
import pl.use.auction.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

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
    private CategoryService categoryService;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private Authentication authentication;

    @Test
    void testCreateAuctionForm() {
        List<Category> categories = new ArrayList<>();
        // add categories to the list as needed for your test

        when(categoryService.findAllMainCategoriesWithSubcategories()).thenReturn(categories);

        String viewName = auctionController.createAuctionForm(model);

        verify(model).addAttribute(eq("auction"), any(Auction.class));
        verify(model).addAttribute("categories", categories);
        assertEquals("auctions/create-auction", viewName);
    }

    @Test
    void testCreateAuction_WithErrors() {
        when(bindingResult.hasErrors()).thenReturn(true);

        String viewName = auctionController.createAuction(new Auction(), new MultipartFile[]{}, 1L, bindingResult, authentication);

        assertEquals("auctions/create-auction", viewName);
    }

    @Test
    void testCreateAuction_Successful() throws IOException {
        Auction auction = new Auction();
        AuctionUser user = new AuctionUser();
        user.setEmail("test@example.com");
        Long categoryId = 1L;
        MultipartFile[] files = new MultipartFile[]{};

        when(bindingResult.hasErrors()).thenReturn(false);
        when(authentication.getName()).thenReturn("test@example.com");
        when(auctionService.createAndSaveAuction(auction, categoryId, files, "test@example.com")).thenReturn(auction);

        String viewName = auctionController.createAuction(auction, files, categoryId, bindingResult, authentication);

        verify(auctionService).createAndSaveAuction(auction, categoryId, files, "test@example.com");
        assertEquals("redirect:/profile/auctions", viewName);
    }

    @Test
    void deleteAuction_NotFound() {
        Long auctionId = 1L;
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());

        ResponseEntity<?> response = auctionController.deleteAuction(auctionId, authentication);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Auction not found", response.getBody());
    }

    @Test
    void deleteAuction_Successful() {
        Long auctionId = 1L;
        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setHighestBid(BigDecimal.ZERO);
        AuctionUser user = new AuctionUser();
        user.setEmail("user@example.com");
        auction.setAuctionCreator(user);

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(authentication.getName()).thenReturn("user@example.com");

        ResponseEntity<?> response = auctionController.deleteAuction(auctionId, authentication);

        verify(auctionRepository).delete(auction);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Auction deleted successfully.", response.getBody());
    }

    @Test
    void deleteAuction_Forbidden() {
        Long auctionId = 1L;
        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setHighestBid(new BigDecimal("100.00"));
        AuctionUser user = new AuctionUser();
        user.setEmail("user@example.com");
        auction.setAuctionCreator(user);

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        ResponseEntity<?> response = auctionController.deleteAuction(auctionId, authentication);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You cannot delete an auction with bids or that you did not create.", response.getBody());
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

    @Test
    void editAuction_ReturnsCorrectViewAndModel_WhenCurrentUserIsCreatorAndNoBids() {
        String slug = "some-slug";
        Auction auction = new Auction();
        auction.setSlug(slug);
        auction.setHighestBid(BigDecimal.ZERO);
        AuctionUser creator = new AuctionUser();
        creator.setId(1L);
        auction.setAuctionCreator(creator);
        List<Category> categories = new ArrayList<>();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("creator@example.com");
        when(userRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(creator));
        when(auctionRepository.findBySlug(slug)).thenReturn(Optional.of(auction));
        when(categoryService.findAllMainCategoriesWithSubcategories()).thenReturn(categories);

        String viewName = auctionController.editAuction(slug, model, authentication);

        verify(model).addAttribute("auction", auction);
        verify(model).addAttribute("categories", categories);
        assertEquals("auctions/auction-edit", viewName);
    }

    @Test
    void editAuction_ThrowsResponseStatusException_WhenAuctionNotFound() {
        String slug = "non-existent-slug";
        Authentication authentication = mock(Authentication.class);

        when(auctionRepository.findBySlug(slug)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            auctionController.editAuction(slug, model, authentication);
        });
    }

    @Test
    void editAuction_ThrowsResponseStatusException_WhenCurrentUserIsNotCreator() {
        String slug = "some-slug";
        Auction auction = new Auction();
        auction.setSlug(slug);
        auction.setHighestBid(BigDecimal.ZERO);
        AuctionUser creator = new AuctionUser();
        creator.setId(1L);
        AuctionUser currentUser = new AuctionUser();
        currentUser.setId(2L);
        auction.setAuctionCreator(creator);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("notcreator@example.com");
        when(userRepository.findByEmail("notcreator@example.com")).thenReturn(Optional.of(currentUser));
        when(auctionRepository.findBySlug(slug)).thenReturn(Optional.of(auction));

        assertThrows(ResponseStatusException.class, () -> {
            auctionController.editAuction(slug, model, authentication);
        });
    }

    @Test
    void editAuction_ThrowsResponseStatusException_WhenAuctionHasBids() {
        String slug = "some-slug";
        Auction auction = new Auction();
        auction.setSlug(slug);
        auction.setHighestBid(new BigDecimal("100.00"));
        AuctionUser creator = new AuctionUser();
        creator.setId(1L);
        auction.setAuctionCreator(creator);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("creator@example.com");
        when(userRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(creator));
        when(auctionRepository.findBySlug(slug)).thenReturn(Optional.of(auction));

        assertThrows(ResponseStatusException.class, () -> {
            auctionController.editAuction(slug, model, authentication);
        });
    }
    @Test
    void updateAuction_Successful_WhenUserIsCreatorAndNoBids() throws IOException {
        String slug = "some-slug";
        Auction auction = new Auction();
        auction.setId(1L);
        auction.setHighestBid(BigDecimal.ZERO);
        AuctionUser creator = new AuctionUser();
        creator.setId(1L);
        auction.setAuctionCreator(creator);

        Auction auctionDetails = new Auction();
        MultipartFile[] newImages = {};
        List<String> imagesToDelete = new ArrayList<>();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("creator@example.com");
        when(userRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(creator));
        when(auctionRepository.findBySlug(slug)).thenReturn(Optional.of(auction));
        when(auctionService.updateAuction(slug, auctionDetails, newImages, imagesToDelete)).thenReturn(auction);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = auctionController.updateAuction(slug, auctionDetails, newImages, imagesToDelete, authentication, redirectAttributes);

        verify(auctionService).updateAuction(slug, auctionDetails, newImages, imagesToDelete);
        assertEquals("redirect:/profile/auctions", viewName);
    }

    @Test
    void updateAuction_ThrowsResponseStatusException_WhenUserIsNotCreator() {
        String slug = "some-slug";
        Auction auction = new Auction();
        auction.setId(1L);
        auction.setHighestBid(BigDecimal.ZERO);
        AuctionUser creator = new AuctionUser();
        creator.setId(1L);
        AuctionUser nonCreator = new AuctionUser();
        nonCreator.setId(2L);
        auction.setAuctionCreator(creator);

        Auction auctionDetails = new Auction();
        MultipartFile[] newImages = {}; // Mock MultipartFile array
        List<String> imagesToDelete = new ArrayList<>();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("noncreator@example.com");
        when(userRepository.findByEmail("noncreator@example.com")).thenReturn(Optional.of(nonCreator));
        when(auctionRepository.findBySlug(slug)).thenReturn(Optional.of(auction));

        assertThrows(ResponseStatusException.class, () -> {
            auctionController.updateAuction(slug, auctionDetails, newImages, imagesToDelete, authentication, new RedirectAttributesModelMap());
        });
    }

    @Test
    void updateAuction_RedirectsWithError_WhenAuctionHasBids() {
        String slug = "some-slug";
        Auction auction = new Auction();
        auction.setId(1L);
        auction.setHighestBid(new BigDecimal("100.00"));
        AuctionUser creator = new AuctionUser();
        creator.setId(1L);
        auction.setAuctionCreator(creator);

        Auction auctionDetails = new Auction();
        MultipartFile[] newImages = {};
        List<String> imagesToDelete = new ArrayList<>();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("creator@example.com");
        when(userRepository.findByEmail("creator@example.com")).thenReturn(Optional.of(creator));
        when(auctionRepository.findBySlug(slug)).thenReturn(Optional.of(auction));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String viewName = auctionController.updateAuction(slug, auctionDetails, newImages, imagesToDelete, authentication, redirectAttributes);

        assertEquals("redirect:/auction/" + slug + "/edit", viewName);
        assertEquals("Cannot edit auction as bidding has already started.", redirectAttributes.getFlashAttributes().get("error"));
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
        String auctionSlug = "some-auction-slug";
        BigDecimal bidAmount = new BigDecimal("100.00");
        AuctionUser bidder = new AuctionUser();
        bidder.setEmail("bidder@example.com");
        Auction auction = new Auction();
        auction.setSlug(auctionSlug);

        when(authentication.getName()).thenReturn("bidder@example.com");
        when(userRepository.findByEmail("bidder@example.com")).thenReturn(Optional.of(bidder));
        when(auctionRepository.findBySlug(auctionSlug)).thenReturn(Optional.of(auction));
        when(auctionService.placeBid(auction, bidder, bidAmount)).thenReturn(true);

        String viewName = auctionController.placeBid(auctionSlug, bidAmount, authentication, redirectAttributes);

        verify(auctionRepository).findBySlug(auctionSlug);
        verify(userRepository).findByEmail("bidder@example.com");
        verify(auctionService).placeBid(auction, bidder, bidAmount);
        verify(redirectAttributes).addFlashAttribute("successMessage", "Bid placed successfully!");

        assertEquals("redirect:/auction/" + auctionSlug, viewName);
    }

    @Test
    void testPlaceBid_Unsuccessful() {
        String auctionSlug = "another-auction-slug";
        BigDecimal bidAmount = new BigDecimal("100.00");
        AuctionUser bidder = new AuctionUser();
        bidder.setEmail("bidder@example.com");
        Auction auction = new Auction();
        auction.setSlug(auctionSlug);

        when(authentication.getName()).thenReturn("bidder@example.com");
        when(userRepository.findByEmail("bidder@example.com")).thenReturn(Optional.of(bidder));
        when(auctionRepository.findBySlug(auctionSlug)).thenReturn(Optional.of(auction));
        when(auctionService.placeBid(auction, bidder, bidAmount)).thenReturn(false);

        String viewName = auctionController.placeBid(auctionSlug, bidAmount, authentication, redirectAttributes);

        verify(redirectAttributes).addFlashAttribute("errorMessage", "Bid not high enough!");
        assertEquals("redirect:/auction/" + auctionSlug, viewName);
    }
}