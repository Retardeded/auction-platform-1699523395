package pl.use.auction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Category;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pl.use.auction.service.AuctionService;
import pl.use.auction.service.CategoryService;
import pl.use.auction.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

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

    @Autowired
    CategoryService categoryService;

    @PostMapping("/auction/{slug}/bid")
    public String placeBid(@PathVariable("slug") String auctionSlug,
                           @RequestParam("bidAmount") BigDecimal bidAmount,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {

        Auction auction = auctionRepository.findBySlug(auctionSlug)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction slug:" + auctionSlug));
        AuctionUser bidder = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        boolean bidPlaced = auctionService.placeBid(auction, bidder, bidAmount);
        if (bidPlaced) {
            redirectAttributes.addFlashAttribute("successMessage", "Bid placed successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Bid not high enough!");
        }
        return "redirect:/auction/" + auctionSlug;
    }

    @GetMapping("/auctions/create")
    public String createAuctionForm(Model model) {
        model.addAttribute("auction", new Auction());
        List<Category> categories = categoryService.findAllMainCategoriesWithSubcategories();
        model.addAttribute("categories", categories);
        return "auctions/create-auction";
    }

    @PostMapping("/auctions/create")
    public String createAuction(@ModelAttribute Auction auction,
                                @RequestParam("images") MultipartFile[] files,
                                @RequestParam("category") Long categoryId, // This is to capture the category ID from the form
                                BindingResult bindingResult,
                                Authentication authentication) {
        if (bindingResult.hasErrors()) {
            return "auctions/create-auction";
        }

        try {
            Auction createdAuction = auctionService.createAndSaveAuction(auction, categoryId, files, authentication.getName());
        } catch (IOException e) {
            e.printStackTrace();
            return "auctions/create-auction";
        }

        return "redirect:/profile/auctions";
    }

    @GetMapping("/auctions/{categoryName}")
    public String viewCategory(@PathVariable String categoryName, Model model, Authentication authentication) {
        Category parentCategory = categoryRepository.findByNameIgnoreCase(StringUtils.slugToCategoryName(categoryName))
                .orElseThrow(() -> new IllegalArgumentException("Invalid category name:" + categoryName));

        String currentUserName = authentication.getName();
        AuctionUser currentUser = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Auction> aggregatedAuctions = auctionService.getAggregatedAuctionsForCategory(parentCategory, currentUser);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("category", parentCategory);
        model.addAttribute("categoryAuctions", aggregatedAuctions);

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
