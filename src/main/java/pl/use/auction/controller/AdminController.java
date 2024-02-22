package pl.use.auction.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pl.use.auction.model.*;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.AuctionService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    @Autowired
    private AuctionRepository auctionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private AuctionService auctionService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/home")
    public String adminHome(Model model, Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        model.addAttribute("username", email);

        AuctionUser currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        model.addAttribute("currentUser", currentUser);

        List<Category> parentCategories = categoryRepository.findByParentCategoryIsNull();
        model.addAttribute("parentCategories", parentCategories);

        List<Auction> cheapestAuctions = auctionService.findCheapestAuctions(6);
        model.addAttribute("cheapestAuctions", cheapestAuctions);

        List<Auction> expensiveAuctions = auctionService.findExpensiveAuctions(6);
        model.addAttribute("expensiveAuctions", expensiveAuctions);

        return "admin/home";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/profile")
    public String viewAdminProfile(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();

        AuctionUser currentUser = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        model.addAttribute("currentUser", currentUser);

        return "admin/profile";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all-auctions")
    public String viewAllAuctionsForAdmin(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();

        AuctionUser currentUser = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        model.addAttribute("currentUser", currentUser);

        List<Auction> allAuctions = auctionRepository.findAll();

        List<Auction> ongoingAuctions = allAuctions.stream()
                .filter(auction -> auction.getStatus() == AuctionStatus.ACTIVE)
                .collect(Collectors.toList());
        List<Auction> pastAuctions = allAuctions.stream()
                .filter(auction -> auction.getStatus() != AuctionStatus.ACTIVE)
                .collect(Collectors.toList());

        model.addAttribute("ongoingAuctions", ongoingAuctions);
        model.addAttribute("pastAuctions", pastAuctions);

        return "admin/all-auctions";
    }
}
