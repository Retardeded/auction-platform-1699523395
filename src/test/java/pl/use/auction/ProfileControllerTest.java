package pl.use.auction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import pl.use.auction.controller.ProfileController;
import pl.use.auction.dto.TransactionFeedbackDTO;
import pl.use.auction.model.*;
import pl.use.auction.repository.TransactionFeedbackRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.service.ProfileService;
import pl.use.auction.service.UserService;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(MockitoExtension.class)
public class ProfileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private ProfileService profileService;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private TransactionFeedbackRepository transactionFeedbackRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProfileController profileController;

    @Mock
    private Model model;
    @BeforeEach
    public void setup() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("templates/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setTemplateEngine(templateEngine);

        mockMvc = MockMvcBuilders.standaloneSetup(profileController)
                .setViewResolvers(viewResolver)
                .build();
    }

    @Test
    void viewProfile_ShouldReturnProfileView() {
        Model model = new ExtendedModelMap();

        String username = "user";
        AuctionUser user = new AuctionUser();
        user.setUsername(username);

        when(authentication.getName()).thenReturn(user.getUsername());
        when(userService.findByUsernameOrThrow(user.getUsername())).thenReturn(user);

        String viewName = profileController.viewProfile(model, authentication);

        verify(userService).findByUsernameOrThrow(username);
        assertEquals("profile/profile", viewName);
        assertEquals(user, model.getAttribute("currentUser"), "The model should contain the currentUser attribute");
    }

    @Test
    void editProfile_ShouldReturnEditProfileView() {
        Model model = new ExtendedModelMap();
        String username = "user";
        AuctionUser user = new AuctionUser();
        user.setUsername(username);

        when(authentication.getName()).thenReturn(username);
        when(userService.findByUsernameOrThrow(username)).thenReturn(user);

        String viewName = profileController.editProfile(model, authentication);

        verify(userService).findByUsernameOrThrow(username);
        assertEquals("profile/profile-edit", viewName, "The returned view name should be 'profile/profile-edit'");
        assertEquals(user, model.getAttribute("user"), "The model should contain the user attribute");
    }

    @Test
    void testShowChangePasswordForm() {
        String username = "user";
        AuctionUser user = new AuctionUser();
        user.setUsername(username);

        when(authentication.getName()).thenReturn(user.getUsername());
        when(userService.findByUsernameOrThrow(user.getUsername())).thenReturn(user);

        String viewName = profileController.showChangePasswordForm(model, authentication);

        verify(userService).findByUsernameOrThrow("user");
        verify(model).addAttribute("user", user);

        assertEquals("profile/change-password", viewName);
    }

    @Test
    void testUpdateProfile() {
        String username = "user";
        AuctionUser existingUser = new AuctionUser();
        existingUser.setUsername(username);

        AuctionUser updatedUser = new AuctionUser();
        updatedUser.setUsername("NewUsername");
        updatedUser.setFirstName("NewFirstName");
        updatedUser.setLastName("NewLastName");
        updatedUser.setLocation("NewLocation");
        updatedUser.setPhoneNumber("NewPhoneNumber");

        when(authentication.getName()).thenReturn(username);

        String viewName = profileController.updateProfile(updatedUser, authentication);

        ArgumentCaptor<AuctionUser> userCaptor = ArgumentCaptor.forClass(AuctionUser.class);
        verify(userService).updateProfile(eq(username), userCaptor.capture());
        AuctionUser capturedUser = userCaptor.getValue();

        assertEquals("NewUsername", capturedUser.getUsername());
        assertEquals("NewFirstName", capturedUser.getFirstName());
        assertEquals("NewLastName", capturedUser.getLastName());
        assertEquals("NewLocation", capturedUser.getLocation());
        assertEquals("NewPhoneNumber", capturedUser.getPhoneNumber());

        assertEquals("redirect:/profile", viewName);
    }

    @Mock
    private RedirectAttributes redirectAttributes;

    @Test
    void testChangePassword_Success() {
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";
        String confirmNewPassword = "newPassword";
        AuctionUser user = new AuctionUser();
        user.setUsername("user");

        when(authentication.getName()).thenReturn(user.getUsername());
        when(userService.findByUsernameOrThrow(user.getUsername())).thenReturn(user);
        when(userService.changeUserPassword(oldPassword, newPassword, confirmNewPassword, user))
                .thenReturn(PasswordChangeResult.SUCCESS);

        String viewName = profileController.changePassword(oldPassword, newPassword, confirmNewPassword,
                authentication, redirectAttributes, model);

        verify(userService).changeUserPassword(oldPassword, newPassword, confirmNewPassword, user);
        verify(redirectAttributes).addFlashAttribute("message", "Password changed successfully!");

        assertEquals("redirect:/profile", viewName);
    }

    @Test
    void testChangePassword_InvalidOldPassword() {
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";
        String confirmNewPassword = "newPassword";
        AuctionUser user = new AuctionUser();
        user.setUsername("user");

        when(authentication.getName()).thenReturn(user.getUsername());
        when(userService.findByUsernameOrThrow(user.getUsername())).thenReturn(user);
        when(userService.changeUserPassword(oldPassword, newPassword, confirmNewPassword, user))
                .thenReturn(PasswordChangeResult.INVALID_OLD_PASSWORD);

        String viewName = profileController.changePassword(oldPassword, newPassword, confirmNewPassword,
                authentication, redirectAttributes, model);

        verify(userService).changeUserPassword(oldPassword, newPassword, confirmNewPassword, user);
        verify(model).addAttribute("error", "The current password is incorrect.");
        assertEquals("profile/change-password", viewName);
    }

    @Test
    void testChangePassword_PasswordMismatch() {
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";
        String confirmNewPassword = "differentNewPassword";
        AuctionUser user = new AuctionUser();
        user.setUsername("user");

        when(authentication.getName()).thenReturn(user.getUsername());
        when(userService.findByUsernameOrThrow(user.getUsername())).thenReturn(user);
        when(userService.changeUserPassword(oldPassword, newPassword, confirmNewPassword, user))
                .thenReturn(PasswordChangeResult.PASSWORD_MISMATCH);

        String viewName = profileController.changePassword(oldPassword, newPassword, confirmNewPassword,
                authentication, redirectAttributes, model);

        verify(userService).changeUserPassword(oldPassword, newPassword, confirmNewPassword, user);
        verify(model).addAttribute("error", "The new passwords do not match.");
        assertEquals("profile/change-password", viewName);
    }

    @Captor
    private ArgumentCaptor<List<Auction>> ongoingAuctionListCaptor;

    @Captor
    private ArgumentCaptor<List<Auction>> pastAuctionListCaptor;

    @Test
    void testViewUserAuctions() {
        AuctionUser user = new AuctionUser();
        user.setUsername("user");
        List<Auction> createdAuctions = createSampleAuctions();
        user.setCreatedAuctions(createdAuctions);

        when(authentication.getName()).thenReturn(user.getUsername());
        when(userService.findByUsernameOrThrow(user.getUsername())).thenReturn(user);;

        String viewName = profileController.viewUserAuctions(model, authentication);

        verify(userService).findByUsernameOrThrow("user");

        verify(model).addAttribute(eq("ongoingAuctions"), ongoingAuctionListCaptor.capture());
        List<Auction> ongoingAuctions = ongoingAuctionListCaptor.getValue();
        assertTrue(ongoingAuctions.stream().allMatch(auction -> auction.getStatus() == AuctionStatus.ACTIVE));

        verify(model).addAttribute(eq("pastAuctions"), pastAuctionListCaptor.capture());
        List<Auction> pastAuctions = pastAuctionListCaptor.getValue();
        assertTrue(pastAuctions.stream().allMatch(auction -> auction.getStatus() != AuctionStatus.ACTIVE));

        assertEquals("profile/user-auctions", viewName);
    }

    @Test
    void testViewBoughtAuctions() {
        String username = "user";
        AuctionUser currentUser = new AuctionUser();
        currentUser.setEmail(username);
        List<Auction> boughtAuctions = List.of(new Auction(), new Auction());

        when(authentication.getName()).thenReturn(currentUser.getUsername());
        when(userService.findByUsernameOrThrow(currentUser.getUsername())).thenReturn(currentUser);
        when(auctionRepository.findByBuyer(currentUser)).thenReturn(boughtAuctions);

        String viewName = profileController.viewBoughtAuctions(model, authentication);

        verify(auctionRepository).findByBuyer(currentUser);
        verify(model).addAttribute("currentUser", currentUser);
        verify(model).addAttribute("boughtAuctions", boughtAuctions);
        assertEquals("profile/purchase-auctions", viewName);
    }

    private List<Auction> createSampleAuctions() {
        List<Auction> auctions = new ArrayList<>();

        Auction pastAuction = new Auction();
        pastAuction.setStatus(AuctionStatus.EXPIRED); // Set status to EXPIRED
        auctions.add(pastAuction);

        Auction ongoingAuction = new Auction();
        ongoingAuction.setStatus(AuctionStatus.ACTIVE); // Set status to ACTIVE
        auctions.add(ongoingAuction);

        return auctions;
    }

    @Test
    void testViewUserHighestBids() {
        AuctionUser user = new AuctionUser();
        user.setUsername("user");
        Auction specificAuction = createSpecificAuction("Unique Title", new BigDecimal("100.00"));
        List<Auction> highestBidAuctions = List.of(specificAuction);

        when(authentication.getName()).thenReturn(user.getUsername());
        when(userService.findByUsernameOrThrow(user.getUsername())).thenReturn(user);
        when(auctionRepository.findByHighestBidder(user)).thenReturn(highestBidAuctions);

        String viewName = profileController.viewUserHighestBids(model, authentication);

        verify(userService).findByUsernameOrThrow("user");
        verify(auctionRepository).findByHighestBidder(user);
        verify(model).addAttribute("highestBidAuctions", highestBidAuctions);

        assertEquals("profile/highest-bids", viewName);

        assertTrue(highestBidAuctions.contains(specificAuction));
        assertEquals("Unique Title", specificAuction.getTitle());
        assertEquals(new BigDecimal("100.00"), specificAuction.getHighestBid());
    }

    private Auction createSpecificAuction(String title, BigDecimal highestBid) {
        Auction auction = new Auction();
        auction.setTitle(title);
        auction.setHighestBid(highestBid);
        return auction;
    }

    @Test
    void testShowObservedAuctions() {
        AuctionUser currentUser = new AuctionUser();
        currentUser.setUsername("user");
        Set<Auction> observedAuctions = Set.of(createSpecificAuction("Observed Auction", new BigDecimal("50.00")));
        currentUser.setObservedAuctions(observedAuctions);

        when(authentication.getName()).thenReturn(currentUser.getUsername());
        when(userService.findByUsernameOrThrow(currentUser.getUsername())).thenReturn(currentUser);

        String viewName = profileController.showObservedAuctions(model, authentication);

        verify(userService).findByUsernameOrThrow("user");
        verify(model).addAttribute("currentUser", currentUser);
        verify(model).addAttribute("observedAuctions", observedAuctions);

        assertEquals("profile/observed-auctions", viewName);

        assertTrue(observedAuctions.containsAll(currentUser.getObservedAuctions()));
    }

    @Test
    void testViewMyBidsAndWatches() {
        AuctionUser currentUser = new AuctionUser();
        currentUser.setUsername("user");

        Auction highestBidAuction = createSpecificAuction("Highest Bid Auction", new BigDecimal("100.00"));
        Auction observedAuction = createSpecificAuction("Observed Auction", new BigDecimal("50.00"));

        List<Auction> highestBidAuctions = List.of(highestBidAuction);
        Set<Auction> observedAuctions = new HashSet<>();
        observedAuctions.add(observedAuction);

        when(authentication.getName()).thenReturn(currentUser.getUsername());
        when(userService.findByUsernameOrThrow(currentUser.getUsername())).thenReturn(currentUser);
        when(auctionRepository.findByHighestBidderAndStatusIn(eq(currentUser), anyList())).thenReturn(highestBidAuctions);

        currentUser.setObservedAuctions(observedAuctions);

        String viewName = profileController.viewMyBidsAndWatches(model, authentication);

        verify(userService).findByUsernameOrThrow("user");
        verify(auctionRepository).findByHighestBidderAndStatusIn(eq(currentUser), anyList());

        verify(model).addAttribute("watchedAuctions", observedAuctions);
        verify(model).addAttribute("bidAuctions", highestBidAuctions);
        verify(model).addAttribute("currentUser", currentUser);

        assertEquals("profile/my-bids-and-watches", viewName);
    }

    private AuctionUser createSpecificUser(String username) {
        AuctionUser user = new AuctionUser();
        user.setUsername(username);
        // Set other necessary fields
        return user;
    }

    @Test
    void testViewUserProfile() {
        String currentUserName = "user";
        String profileUsername = "profileUser";
        AuctionUser currentUser = createSpecificUser(currentUserName);
        AuctionUser profileUser = createSpecificUser(profileUsername);
        List<TransactionFeedback> feedbackList = List.of();
        String cumulativeRating = "80% Positive";

        when(authentication.getName()).thenReturn(currentUserName);

        when(userService.findByUsernameOrThrow(currentUserName)).thenReturn(currentUser);
        when(userService.findByUsernameOrThrow(profileUsername)).thenReturn(profileUser);
        when(transactionFeedbackRepository.findBySellerOrBuyer(profileUser, profileUser)).thenReturn(feedbackList);
        when(profileService.calculateCumulativeRating(feedbackList, feedbackList.size())).thenReturn(cumulativeRating);

        String viewName = profileController.viewUserProfile(profileUsername, authentication, model);

        verify(transactionFeedbackRepository).findBySellerOrBuyer(profileUser, profileUser);
        verify(model).addAttribute("currentUser", currentUser);
        verify(model).addAttribute("profileUser", profileUser);
        verify(model).addAttribute("feedbackList", feedbackList);
        verify(model).addAttribute("cumulativeRating", cumulativeRating);

        assertEquals("profile/user-profile", viewName);
    }

    @Test
    void testShowRatingPageAsSeller() {
        String auctionSlug = "unique-auction-slug";
        Auction auction = new Auction();
        auction.setSlug(auctionSlug);
        AuctionUser seller = new AuctionUser();
        auction.setAuctionCreator(seller);

        when(auctionRepository.findBySlug(auctionSlug)).thenReturn(Optional.of(auction));
        when(authentication.getName()).thenReturn(seller.getUsername());
        when(userService.findByUsernameOrThrow(seller.getUsername())).thenReturn(seller);

        String viewName = profileController.showRatingPage(auctionSlug, model, authentication);

        verify(auctionRepository).findBySlug(auctionSlug);
        verify(userService).findByUsernameOrThrow(seller.getUsername());
        verify(model).addAttribute("auction", auction);
        verify(model).addAttribute("currentUser", seller);
        verify(model).addAttribute("role", "seller");

        assertEquals("profile/auction-feedback", viewName);
    }

    @Test
    void testShowRatingPageAsBuyer() {
        String auctionSlug = "unique-auction-slug";
        Auction auction = new Auction();
        AuctionUser buyer = new AuctionUser();
        AuctionUser seller = new AuctionUser();
        auction.setAuctionCreator(seller);

        when(authentication.getName()).thenReturn(buyer.getUsername());
        when(userService.findByUsernameOrThrow(buyer.getUsername())).thenReturn(buyer);
        when(auctionRepository.findBySlug(auctionSlug)).thenReturn(Optional.of(auction));

        String viewName = profileController.showRatingPage(auctionSlug, model, authentication);

        verify(auctionRepository).findBySlug(auctionSlug);
        verify(userService).findByUsernameOrThrow(buyer.getUsername());
        verify(model).addAttribute("auction", auction);
        verify(model).addAttribute("currentUser", buyer);
        verify(model).addAttribute("role", "buyer");

        assertEquals("profile/auction-feedback", viewName);
    }

    @Test
    void testSubmitBuyerFeedback_Success() {
        String auctionSlug = "test-slug";
        Auction auction = new Auction();
        auction.setSlug(auctionSlug);
        AuctionUser buyer = new AuctionUser();
        buyer.setUsername("buyer");
        AuctionUser seller = new AuctionUser();
        auction.setAuctionCreator(seller);

        TransactionFeedbackDTO transactionFeedbackDTO = new TransactionFeedbackDTO();
        transactionFeedbackDTO.setComment("Great transaction!");
        transactionFeedbackDTO.setRating(Rating.POSITIVE);

        when(authentication.getName()).thenReturn(buyer.getUsername());
        when(userService.findByUsernameOrThrow(buyer.getUsername())).thenReturn(buyer);

        when(auctionRepository.findBySlug(auctionSlug)).thenReturn(Optional.of(auction));
        when(transactionFeedbackRepository.findByAuction(auction)).thenReturn(Optional.empty());

        ResponseEntity<?> response = profileController.submitBuyerFeedback(auctionSlug, transactionFeedbackDTO, authentication);

        verify(transactionFeedbackRepository).save(any(TransactionFeedback.class));
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void testSubmitBuyerFeedback_AuctionNotFound() {
        String auctionSlug = "test-slug";
        Auction auction = new Auction();
        auction.setSlug(auctionSlug);
        AuctionUser buyer = new AuctionUser();
        buyer.setUsername("buyer");
        AuctionUser seller = new AuctionUser();
        auction.setAuctionCreator(seller);

        TransactionFeedbackDTO transactionFeedbackDTO = new TransactionFeedbackDTO();
        transactionFeedbackDTO.setComment("Great transaction!");
        transactionFeedbackDTO.setRating(Rating.POSITIVE);

        when(auctionRepository.findBySlug(anyString())).thenThrow(new IllegalArgumentException("Invalid Auction slug: " + auction.getSlug()));

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                profileController.submitBuyerFeedback(auction.getSlug(), transactionFeedbackDTO, authentication));

        assertEquals("Invalid Auction slug: " + auction.getSlug(), exception.getMessage());
    }

    @Test
    void testSubmitSellerFeedback_Success() {
        String auctionSlug = "test-slug";
        Auction auction = new Auction();
        auction.setSlug(auctionSlug);
        AuctionUser seller = new AuctionUser();
        seller.setUsername("seller");
        auction.setAuctionCreator(seller);

        TransactionFeedbackDTO transactionFeedbackDTO = new TransactionFeedbackDTO();
        transactionFeedbackDTO.setComment("Excellent buyer!");
        transactionFeedbackDTO.setRating(Rating.POSITIVE);

        when(authentication.getName()).thenReturn(seller.getUsername());
        when(userService.findByUsernameOrThrow(seller.getUsername())).thenReturn(seller);
        when(auctionRepository.findBySlug(auctionSlug)).thenReturn(Optional.of(auction));
        when(transactionFeedbackRepository.findByAuction(auction)).thenReturn(Optional.empty());

        ResponseEntity<?> response = profileController.submitSellerFeedback(auctionSlug, transactionFeedbackDTO, authentication);

        verify(transactionFeedbackRepository).save(any(TransactionFeedback.class));
        assertEquals(200, response.getStatusCodeValue());
    }

}
