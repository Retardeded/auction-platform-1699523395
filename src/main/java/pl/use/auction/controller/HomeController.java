package pl.use.auction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Category;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.AuctionService;
import pl.use.auction.service.UserService;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private UserService userService;


    @GetMapping("/home")
    public String home(Model model, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        model.addAttribute("username", username);

        AuctionUser currentUser = userService.findByUsernameOrThrow(username);


        model.addAttribute("currentUser", currentUser);

        List<Category> parentCategories = categoryRepository.findByParentCategoryIsNull();
        model.addAttribute("parentCategories", parentCategories);

        List<Auction> cheapestAuctions = auctionService.getCheapestAuctions(6);
        model.addAttribute("cheapestAuctions", cheapestAuctions);

        List<Auction> expensiveAuctions = auctionService.getExpensiveAuctions(6);
        model.addAttribute("expensiveAuctions", expensiveAuctions);

        List<Auction> goodDealAuctions = auctionService.getGoodDealAuctions(6);
        model.addAttribute("goodDealAuctions", goodDealAuctions);

        return "home";
    }

    @GetMapping("/search")
    public String search(
            @RequestParam("search") String query,
            @RequestParam("location") String location,
            @RequestParam("category") String category,
            @RequestParam(value = "sort", required = false, defaultValue = "date") String sort,
            Model model,
            Authentication authentication) {

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        model.addAttribute("username", username);

        AuctionUser currentUser = userService.findByUsernameOrThrow(username);
        model.addAttribute("currentUser", currentUser);

        List<Category> parentCategories = categoryRepository.findByParentCategoryIsNull();
        model.addAttribute("parentCategories", parentCategories);

        List<Auction> searchResults = auctionService.searchAuctions(query, location, category, sort);
        model.addAttribute("searchResults", searchResults);
        model.addAttribute("currentSortOrder", sort);

        return "auctions/search-results";
    }
}