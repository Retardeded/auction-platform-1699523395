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
import org.springframework.ui.Model;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import pl.use.auction.controller.ProfileController;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.service.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @InjectMocks
    private ProfileController profileController;

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
    public void viewProfile_ShouldReturnProfileView() throws Exception {
        String email = "user@example.com";
        AuctionUser mockUser = new AuctionUser();
        mockUser.setEmail(email);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        Authentication auth = Mockito.mock(Authentication.class);
        when(auth.getName()).thenReturn(email);

        SecurityContext securityContext = Mockito.mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        mockMvc.perform(get("/profile").principal(auth))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/profile"))
                .andExpect(model().attributeExists("user"));

        SecurityContextHolder.clearContext();
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

    @Mock
    private Model model;

    @Mock
    private Authentication authentication;

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
        assertTrue(ongoingAuctions.stream().allMatch(auction -> auction.getEndTime().isAfter(LocalDateTime.now())));

        verify(model).addAttribute(eq("pastAuctions"), pastAuctionListCaptor.capture());
        List<Auction> pastAuctions = pastAuctionListCaptor.getValue();
        assertTrue(pastAuctions.stream().allMatch(auction -> auction.getEndTime().isBefore(LocalDateTime.now())));

        assertEquals("profile/user-auctions", viewName);
    }

    private List<Auction> createSampleAuctions() {
        List<Auction> auctions = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        Auction pastAuction = new Auction();
        pastAuction.setEndTime(now.minusDays(1)); // End time in the past
        auctions.add(pastAuction);

        Auction ongoingAuction = new Auction();
        ongoingAuction.setEndTime(now.plusDays(1)); // End time in the future
        auctions.add(ongoingAuction);

        return auctions;
    }
}
