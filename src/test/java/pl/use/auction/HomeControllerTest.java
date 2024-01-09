package pl.use.auction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import pl.use.auction.controller.HomeController;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Category;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.AuctionService;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @InjectMocks
    private HomeController homeController;

    @Mock
    private Model model;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AuctionService auctionService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @Test
    void testHome() {
        AuctionUser auctionCreator = new AuctionUser();
        Auction cheapAuction = createTestAuctionWithCreator(auctionCreator);
        Auction expensiveAuction = createTestAuctionWithCreator(auctionCreator);

        AuctionUser currentUser = new AuctionUser();
        currentUser.setEmail("currentUser@example.com");

        List<Auction> cheapestAuctions = List.of(cheapAuction);
        List<Auction> expensiveAuctions = List.of(expensiveAuction);

        List<Category> parentCategories = List.of(new Category());
        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(parentCategories);

        when(auctionService.findCheapestAuctions(any(AuctionUser.class), eq(6))).thenReturn(cheapestAuctions);
        when(auctionService.findExpensiveAuctions(any(AuctionUser.class), eq(6))).thenReturn(expensiveAuctions);

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(currentUser));
        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(parentCategories);

        String viewName = homeController.home(model, authentication);

        verify(userDetails).getUsername();
        verify(userRepository).findByEmail("test@example.com");
        verify(categoryRepository).findByParentCategoryIsNull();
        verify(auctionService).findCheapestAuctions(currentUser, 6);
        verify(auctionService).findExpensiveAuctions(currentUser, 6);

        assertEquals("home", viewName);
        verify(model).addAttribute(eq("username"), anyString());
        verify(model).addAttribute(eq("parentCategories"), anyList());
        verify(model).addAttribute(eq("cheapestAuctions"), anyList());
        verify(model).addAttribute(eq("expensiveAuctions"), anyList());
    }

    private Auction createTestAuctionWithCreator(AuctionUser creator) {
        Auction auction = new Auction();
        auction.setAuctionCreator(creator);
        return auction;
    }
}