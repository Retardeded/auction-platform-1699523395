package pl.use.auction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Category;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pl.use.auction.service.AuctionService;
import pl.use.auction.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AuctionController {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuctionService auctionService;

    @PostMapping("/auctions/bid")
    public String placeBid(@RequestParam("auctionId") Long auctionId,
                           @RequestParam("bidAmount") BigDecimal bidAmount,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes,
                           Model model) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction Id:" + auctionId));
        AuctionUser bidder = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean bidPlaced = auctionService.placeBid(auction, bidder, bidAmount);
        if (bidPlaced) {
            redirectAttributes.addFlashAttribute("successMessage", "Bid placed successfully!");
            return "redirect:/auctions/all";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Bid not high enough!");
            return "redirect:/auctions/" + auctionId;
        }
    }

    @GetMapping("/auctions/create")
    public String createAuctionForm(Model model) {
        model.addAttribute("auction", new Auction());
        return "auctions/create-auction";
    }

    @PostMapping("/auctions/create")
    public String createAuction(@ModelAttribute Auction auction, BindingResult bindingResult, Authentication authentication) {
        if (bindingResult.hasErrors()) {
            return "auctions/create-auction";
        }

        AuctionUser user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        auction.setAuctionCreator(user);
        auction.setStartTime(LocalDateTime.now());
        auctionRepository.save(auction);

        return "redirect:/profile/auctions";
    }

    @GetMapping("/auctions/all")
    public String viewAllOngoingAuctions(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();
        AuctionUser currentUser = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Auction> ongoingAuctions = auctionRepository.findByEndTimeAfter(LocalDateTime.now())
                .stream()
                .filter(auction -> !auction.getAuctionCreator().equals(currentUser))
                .collect(Collectors.toList());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("ongoingAuctions", ongoingAuctions);
        return "auctions/all-auctions";
    }

    @GetMapping("/auctions/{categoryName}")
    public String viewCategory(@PathVariable String categoryName, Model model, Authentication authentication) {
        Category category = categoryRepository.findByNameIgnoreCase(StringUtils.slugToCategoryName(categoryName))
                .orElseThrow(() -> new IllegalArgumentException("Invalid category name:" + categoryName));

        String currentUserName = authentication.getName();
        AuctionUser currentUser = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Auction> categoryAuctions = auctionRepository.findByCategoryAndEndTimeAfter(category, LocalDateTime.now())
                .stream()
                .filter(auction -> !auction.getAuctionCreator().equals(currentUser))
                .collect(Collectors.toList());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("category", category);
        model.addAttribute("categoryAuctions", categoryAuctions);

        return "auctions/category";
    }

    @GetMapping("/auction/{slug}")
    public String viewAuctionDetail(@PathVariable("slug") String auctionSlug, Model model, Authentication authentication) {
        AuctionUser user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Auction auction = auctionRepository.findBySlug(auctionSlug)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction slug: " + auctionSlug));
        model.addAttribute("auction", auction);
        model.addAttribute("currentUser", user);
        return "auctions/auction-detail";
    }

    @GetMapping("/add-to-watchlist/{auctionId}")
    public ResponseEntity<?> addToWatchlist(@PathVariable Long auctionId, Authentication authentication, RedirectAttributes redirectAttributes) {
        AuctionUser currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction Id:" + auctionId));
        auctionService.addToWatchlist(currentUser, auction);
        redirectAttributes.addFlashAttribute("watchMessage", "Added to watchlist.");
        return ResponseEntity.ok("Added to watchlist");
    }

    @GetMapping("/remove-from-watchlist/{auctionId}")
    public ResponseEntity<?> removeFromWatchlist(@PathVariable Long auctionId, Authentication authentication, RedirectAttributes redirectAttributes) {
        AuctionUser currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction Id:" + auctionId));
        auctionService.removeFromWatchlist(currentUser, auction);
        redirectAttributes.addFlashAttribute("watchMessage", "Removed from watchlist.");
        return ResponseEntity.ok("Removed from watchlist");
    }
}
