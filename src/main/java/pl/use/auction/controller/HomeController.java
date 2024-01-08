package pl.use.auction.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Category;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final CategoryRepository categoryRepository;

    private final AuctionRepository auctionRepository;

    private final UserRepository userRepository;

    public HomeController(CategoryRepository categoryRepository, AuctionRepository auctionRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.auctionRepository = auctionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/home")
    public String home(Model model, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        model.addAttribute("username", userDetails.getUsername());

        AuctionUser currentUser = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Category> orderedCategories = categoryRepository.findCategoriesOrderedByAuctionCount();
        model.addAttribute("orderedCategories", orderedCategories);

        List<Auction> cheapestAuctions = auctionRepository.findByEndTimeAfter(LocalDateTime.now())
                .stream()
                .filter(auction -> !auction.getAuctionCreator().equals(currentUser))
                .sorted(Comparator.comparing(Auction::getHighestBid))
                .limit(6)
                .collect(Collectors.toList());
        model.addAttribute("cheapestAuctions", cheapestAuctions);

        List<Auction> expensiveAuctions = auctionRepository.findByEndTimeAfter(LocalDateTime.now())
                .stream()
                .filter(auction -> !auction.getAuctionCreator().equals(currentUser))
                .sorted(Comparator.comparing(Auction::getHighestBid).reversed())
                .limit(6)
                .collect(Collectors.toList());
        model.addAttribute("expensiveAuctions", expensiveAuctions);

        return "home";
    }
}