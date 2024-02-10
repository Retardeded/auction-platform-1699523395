package pl.use.auction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
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
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionStatus;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.PasswordChangeResult;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.service.UserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(MockitoExtension.class)
public class ProfileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private AuctionRepository auctionRepository;

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

        String email = "user@example.com";
        AuctionUser user = new AuctionUser();
        user.setEmail(email);
        when(authentication.getName()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        String viewName = profileController.viewProfile(model, authentication);

        verify(userRepository).findByEmail(email);
        assertEquals("profile/profile", viewName);
        assertEquals(user, model.getAttribute("currentUser"), "The model should contain the currentUser attribute");
    }

    @Test
    public void editProfile_ShouldReturnEditProfileView() throws Exception {
        String email = "user@example.com";
        AuctionUser mockUser = new AuctionUser();
        mockUser.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn(email);

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        mockMvc.perform(get("/profile/edit").principal(auth))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/profile-edit"))
                .andExpect(model().attributeExists("user"));

        SecurityContextHolder.clearContext();
    }

    @Test
    void testShowChangePasswordForm() {
        AuctionUser user = new AuctionUser();
        user.setEmail("user@example.com");

        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        String viewName = profileController.showChangePasswordForm(model, authentication);

        verify(userRepository).findByEmail("user@example.com");
        verify(model).addAttribute("user", user);

        assertEquals("profile/change-password", viewName);
    }

    @Test
    void testUpdateProfile() {
        AuctionUser existingUser = new AuctionUser();
        existingUser.setEmail("existing@example.com");

        AuctionUser updatedUser = new AuctionUser();
        updatedUser.setUsername("NewUsername");
        updatedUser.setFirstName("NewFirstName");
        updatedUser.setLastName("NewLastName");
        updatedUser.setLocation("NewLocation");
        updatedUser.setPhoneNumber("NewPhoneNumber");

        when(authentication.getName()).thenReturn("existing@example.com");
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existingUser));

        String viewName = profileController.updateProfile(updatedUser, authentication);

        verify(userRepository).findByEmail("existing@example.com");
        verify(userRepository).save(existingUser);
        assertEquals("NewUsername", existingUser.getUsername());
        assertEquals("NewFirstName", existingUser.getFirstName());
        assertEquals("NewLastName", existingUser.getLastName());
        assertEquals("NewLocation", existingUser.getLocation());
        assertEquals("NewPhoneNumber", existingUser.getPhoneNumber());

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
        user.setEmail("user@example.com");

        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
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
        user.setEmail("user@example.com");

        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
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
        user.setEmail("user@example.com");

        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
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
        user.setEmail("user@example.com");
        List<Auction> createdAuctions = createSampleAuctions();
        user.setCreatedAuctions(createdAuctions);

        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        String viewName = profileController.viewUserAuctions(model, authentication);

        verify(userRepository).findByEmail("user@example.com");

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
        String email = "user@example.com";
        AuctionUser currentUser = new AuctionUser();
        currentUser.setEmail(email);
        List<Auction> boughtAuctions = List.of(new Auction(), new Auction()); // Mock some bought auctions

        when(authentication.getName()).thenReturn(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(currentUser));
        when(auctionRepository.findByBuyer(currentUser)).thenReturn(boughtAuctions);

        String viewName = profileController.viewBoughtAuctions(model, authentication);

        verify(userRepository).findByEmail(email);
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
        user.setEmail("user@example.com");
        Auction specificAuction = createSpecificAuction("Unique Title", new BigDecimal("100.00"));
        List<Auction> highestBidAuctions = List.of(specificAuction);

        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(auctionRepository.findByHighestBidder(user)).thenReturn(highestBidAuctions);

        String viewName = profileController.viewUserHighestBids(model, authentication);

        verify(userRepository).findByEmail("user@example.com");
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
        currentUser.setEmail("user@example.com");
        Set<Auction> observedAuctions = Set.of(createSpecificAuction("Observed Auction", new BigDecimal("50.00")));
        currentUser.setObservedAuctions(observedAuctions);

        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(currentUser));

        String viewName = profileController.showObservedAuctions(model, authentication);

        verify(userRepository).findByEmail("user@example.com");
        verify(model).addAttribute("currentUser", currentUser);
        verify(model).addAttribute("observedAuctions", observedAuctions);

        assertEquals("profile/observed-auctions", viewName);

        assertTrue(observedAuctions.containsAll(currentUser.getObservedAuctions()));
    }

    @Test
    void testViewMyBidsAndWatches() {
        AuctionUser currentUser = new AuctionUser();
        currentUser.setEmail("user@example.com");

        Auction highestBidAuction = createSpecificAuction("Highest Bid Auction", new BigDecimal("100.00"));
        Auction observedAuction = createSpecificAuction("Observed Auction", new BigDecimal("50.00"));

        List<Auction> highestBidAuctions = List.of(highestBidAuction);
        Set<Auction> observedAuctions = new HashSet<>();
        observedAuctions.add(observedAuction);

        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(currentUser));
        when(auctionRepository.findByHighestBidderAndStatusIn(eq(currentUser), anyList())).thenReturn(highestBidAuctions);

        currentUser.setObservedAuctions(observedAuctions);

        String viewName = profileController.viewMyBidsAndWatches(model, authentication);

        verify(userRepository).findByEmail("user@example.com");
        verify(auctionRepository).findByHighestBidderAndStatusIn(eq(currentUser), anyList());

        verify(model).addAttribute("watchedAuctions", observedAuctions);
        verify(model).addAttribute("bidAuctions", highestBidAuctions);
        verify(model).addAttribute("currentUser", currentUser);

        assertEquals("profile/my-bids-and-watches", viewName);
    }

}
