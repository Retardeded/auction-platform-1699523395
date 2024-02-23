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
        String username = "test@example.com";
        AuctionUser auctionUser = new AuctionUser();
        auctionUser.setEmail(username);
        List<Category> parentCategories = List.of(new Category());
        List<Auction> cheapestAuctions = List.of(new Auction());
        List<Auction> expensiveAuctions = List.of(new Auction());

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(username);
        when(userRepository.findByEmail(username)).thenReturn(Optional.of(auctionUser));
        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(parentCategories);
        when(auctionService.setCheapestAuctions(6)).thenReturn(cheapestAuctions);
        when(auctionService.setExpensiveAuctions(6)).thenReturn(expensiveAuctions);

        String viewName = homeController.home(model, authentication);

        verify(userDetails).getUsername();
        verify(userRepository).findByEmail("test@example.com");
        verify(categoryRepository).findByParentCategoryIsNull();
        verify(auctionService).setCheapestAuctions(6);
        verify(auctionService).setExpensiveAuctions(6);

        assertEquals("home", viewName);
        verify(model).addAttribute(eq("username"), anyString());
        verify(model).addAttribute(eq("parentCategories"), anyList());
        verify(model).addAttribute(eq("cheapestAuctions"), anyList());
        verify(model).addAttribute(eq("expensiveAuctions"), anyList());
    }

    @Test
    void testSearch() {
        String username = "user@example.com";
        String searchQuery = "example";
        String location = "location1";
        String category = "category1";
        String sort = "date";

        AuctionUser auctionUser = new AuctionUser();
        auctionUser.setEmail(username);

        List<Category> parentCategories = List.of(new Category());
        List<Auction> searchResults = List.of(new Auction());

        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn(username);
        when(userRepository.findByEmail(username)).thenReturn(Optional.of(auctionUser));
        when(categoryRepository.findByParentCategoryIsNull()).thenReturn(parentCategories);
        when(auctionService.searchAuctions(searchQuery, location, category, sort)).thenReturn(searchResults);

        String viewName = homeController.search(searchQuery, location, category, sort, model, authentication);

        verify(userRepository).findByEmail(username);
        verify(categoryRepository).findByParentCategoryIsNull();
        verify(auctionService).searchAuctions(searchQuery, location, category, sort);

        assertEquals("auctions/search-results", viewName);
        verify(model).addAttribute("username", username);
        verify(model).addAttribute("currentUser", auctionUser);
        verify(model).addAttribute("parentCategories", parentCategories);
        verify(model).addAttribute("searchResults", searchResults);
        verify(model).addAttribute("currentSortOrder", sort);
    }

    private Auction createTestAuctionWithCreator(AuctionUser creator) {
        Auction auction = new Auction();
        auction.setAuctionCreator(creator);
        return auction;
    }
}