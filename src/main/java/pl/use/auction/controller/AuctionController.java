package pl.use.auction.controller;

import com.stripe.model.checkout.Session;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionStatus;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.Category;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.CategoryRepository;
import pl.use.auction.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import pl.use.auction.service.AuctionService;
import pl.use.auction.service.CategoryService;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import pl.use.auction.service.UserService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Controller
public class AuctionController {

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Value("${stripe.api.publishablekey}")
    private String stripePublishableKey;

    @Value("${stripe.api.secretkey}")
    private String stripeApiKey;

    @PostMapping("/auction/{slug}/bid")
    public String placeBid(@PathVariable("slug") String auctionSlug,
                           @RequestParam("bidAmount") BigDecimal bidAmount,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {

        Auction auction = auctionRepository.findBySlug(auctionSlug)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction slug:" + auctionSlug));
        AuctionUser bidder = userService.findByUsernameOrThrow(authentication.getName());

        if (!auction.getStatus().equals(AuctionStatus.ACTIVE)) {
            redirectAttributes.addFlashAttribute("errorMessage", "This auction is no longer active and cannot accept bids.");
            return "auctions/auction-expired";
        }

        boolean bidPlaced = auctionService.placeBid(auction, bidder, bidAmount);
        if (bidPlaced) {
            redirectAttributes.addFlashAttribute("successMessage", "Bid placed successfully!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Bid not high enough!");
        }
        return "redirect:/auction/" + auctionSlug;
    }

    @PostMapping("/auction/{slug}/buy-now")
    public ResponseEntity<?> buyNow(@PathVariable("slug") String auctionSlug,
                                    @RequestParam("auctionPrice") BigDecimal buyNowPrice) {

        Auction auction = auctionRepository.findBySlug(auctionSlug)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction slug: " + auctionSlug));

        if (auction.getBuyNowPrice() == null || auction.getBuyNowPrice().compareTo(buyNowPrice) != 0) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "The buy now price does not match."));
        }

        return auctionService.proceedToPayment(auctionSlug, buyNowPrice);
    }

    @PostMapping("/auction/{slug}/finalize-auction-payment")
    public ResponseEntity<?> finalizeAuctionPayment(@PathVariable("slug") String auctionSlug,
                                    @RequestParam("auctionPrice") BigDecimal highestBidPrice,
                                    Authentication authentication) {
            Auction auction = auctionRepository.findBySlug(auctionSlug)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid auction slug: " + auctionSlug));
            AuctionUser currentUser = userService.findByUsernameOrThrow(authentication.getName());

            if (!(auction.getStatus() == AuctionStatus.AWAITING_PAYMENT &&
                    auction.getHighestBidder().equals(currentUser))) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("error", "You are not authorized to perform this operation."));
            }

        return auctionService.proceedToPayment(auctionSlug, highestBidPrice);
    }

    @GetMapping("/payment/success")
    public String handlePaymentSuccess(@RequestParam("session_id") String sessionId, Authentication authentication, Model model) {
        try {
            Stripe.apiKey = stripeApiKey;

            Session session = Session.retrieve(sessionId);
            String auctionSlug = session.getMetadata().get("auction_slug");

            AuctionUser user = userService.findByUsernameOrThrow(authentication.getName());

            Long amountPaid = session.getAmountTotal();
            BigDecimal finalPrice = BigDecimal.valueOf(amountPaid).divide(BigDecimal.valueOf(100));

            auctionService.finalizeAuctionSale(auctionSlug, user, finalPrice);

            model.addAttribute("session", session);

            return "auctions/success";
        } catch (StripeException e) {
            e.printStackTrace();
            model.addAttribute("error", "An error occurred while processing your payment.");
            return "error";
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "An error occurred while retrieving auction details.");
            return "error";
        }
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
                                @RequestParam("category") Long categoryId,
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

        return "redirect:/profile/user-auctions";
    }

    @Transactional
    @DeleteMapping("/auctions/delete/{auctionId}")
    public ResponseEntity<?> deleteAuction(@PathVariable Long auctionId, Authentication authentication) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);

        if (auction == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Auction not found");
        }

        if (auction.getHighestBid().compareTo(BigDecimal.ZERO) == 0
                && auction.getAuctionCreator().getEmail().equals(authentication.getName())) {

            auctionService.removeAuctionFromObservers(auction);
            auctionRepository.delete(auction);

            return ResponseEntity.ok("Auction deleted successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You cannot delete an auction with bids or that you did not create.");
        }
    }

    @GetMapping("/auction/{slug}")
    public String viewAuctionDetail(@PathVariable("slug") String auctionSlug, Model model, Authentication authentication) {
        AuctionUser user = userService.findByUsernameOrThrow(authentication.getName());
        Auction auction = auctionRepository.findBySlug(auctionSlug)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction slug: " + auctionSlug));


        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            model.addAttribute("errorMessage", "This auction is no longer active.");
            return "auctions/auction-expired"; // Assuming you have a view for expired auctions
        }

        model.addAttribute("auction", auction);
        model.addAttribute("currentUser", user);
        model.addAttribute("stripePublishableKey", stripePublishableKey);
        return "auctions/auction-detail";
    }

    @GetMapping("/auction/{slug}/edit")
    public String editAuction(@PathVariable("slug") String slug, Model model, Authentication authentication) {
        Auction auction = auctionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));
        AuctionUser currentUser = userService.findByUsernameOrThrow(authentication.getName());

        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            model.addAttribute("errorMessage", "This auction is no longer active.");
            return "auctions/auction-expired";
        }

        if (!currentUser.getId().equals(auction.getAuctionCreator().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (auction.getHighestBid().compareTo(BigDecimal.ZERO) > 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Editing is not allowed once bidding has started");
        }

        List<Category> categories = categoryService.findAllMainCategoriesWithSubcategories();
        model.addAttribute("auction", auction);
        model.addAttribute("categories", categories);
        return "auctions/auction-edit";
    }

    @PostMapping("/auction/{slug}/edit")
    public String updateAuction(@PathVariable("slug") String slug,
                                @ModelAttribute Auction auctionDetails,
                                @RequestParam("images") MultipartFile[] newImages,
                                @RequestParam(value = "imagesToDelete", required = false) List<String> imagesToDelete,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {

        Auction auction = auctionRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Auction not found"));
        AuctionUser currentUser = userService.findByUsernameOrThrow(authentication.getName());

        if (!currentUser.getId().equals(auction.getAuctionCreator().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (auction.getHighestBid().compareTo(BigDecimal.ZERO) > 0) {
            redirectAttributes.addFlashAttribute("error", "Cannot edit auction as bidding has already started.");
            return "redirect:/auction/" + slug + "/edit";
        }

        try {
            Auction updatedAuction = auctionService.updateAuction(slug, auctionDetails, newImages, imagesToDelete);
        } catch (IOException e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Error saving images.");
            return "redirect:/auction/" + slug + "/edit";
        }

        return "redirect:/auction/" + slug;
    }

    @PostMapping("/add-to-watchlist/{auctionId}")
    public ResponseEntity<?> addToWatchlist(@PathVariable Long auctionId, Authentication authentication, RedirectAttributes redirectAttributes) {
        AuctionUser currentUser = userService.findByUsernameOrThrow(authentication.getName());
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction Id:" + auctionId));
        auctionService.addToWatchlist(currentUser, auction);
        redirectAttributes.addFlashAttribute("watchMessage", "Added to watchlist.");
        return ResponseEntity.ok("Added to watchlist");
    }

    @PostMapping("/remove-from-watchlist/{auctionId}")
    public ResponseEntity<?> removeFromWatchlist(@PathVariable Long auctionId, Authentication authentication, RedirectAttributes redirectAttributes) {
        AuctionUser currentUser = userService.findByUsernameOrThrow(authentication.getName());
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction Id:" + auctionId));
        auctionService.removeFromWatchlist(currentUser, auction);
        redirectAttributes.addFlashAttribute("watchMessage", "Removed from watchlist.");
        return ResponseEntity.ok("Removed from watchlist");
    }
}
