package pl.use.auction.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.use.auction.model.*;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.AuctionService;

import java.util.*;
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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/feature-auction")
    public ResponseEntity<?> featureAuction(@RequestParam("auctionId") Long auctionId,
                                            @RequestParam("featuredType") String featuredType) {
        Optional<Auction> optionalAuction = auctionRepository.findById(auctionId);
        if (!optionalAuction.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", "Auction not found."));
        }

        Auction auction = optionalAuction.get();
        if (!featuredType.isEmpty()) {
            auction.setFeaturedType(FeaturedType.valueOf(featuredType));
        } else {
            auction.setFeaturedType(FeaturedType.NONE);
        }

        auctionRepository.save(auction);

        String imagePath = "/auctionSectionImages/" + auction.getFeaturedType().getImagePath();

        Map<String, String> response = new HashMap<>();
        response.put("imagePath", imagePath);
        return ResponseEntity.ok(response);
    }
}
