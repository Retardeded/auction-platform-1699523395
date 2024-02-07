package pl.use.auction.controller;

import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.http.HttpServletRequest;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @PostMapping("/auction/{slug}/buy-now")
    @ResponseBody
    public ResponseEntity<?> buyNow(@PathVariable("slug") String auctionSlug,
                                    @RequestBody BigDecimal buyNowRequest,
                                    Authentication authentication) {

        try {
            String clientSecret = auctionService.createPaymentIntent(buyNowRequest, "pln");
            Map<String, String> response = new HashMap<>();
            response.put("clientSecret", clientSecret);

            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error processing payment: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "An error occurred during the purchase process."));
        }
    }

    @PostMapping("/auction/{slug}/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@PathVariable("slug") String auctionSlug,
                                                   @RequestParam("buyNowPrice") BigDecimal buyNowPrice,
                                                   HttpServletRequest request) {
        try {
            Stripe.apiKey = stripeApiKey;
            String successUrl = "http://localhost:8080/payment/success?session_id={CHECKOUT_SESSION_ID}";
            String cancelUrl = "http://localhost:8080/auction/" + auctionSlug;

            SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .putMetadata("auction_slug", auctionSlug)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("pln") // Use your currency here
                                    .setUnitAmount(buyNowPrice.longValue() * 100) // Stripe expects the amount in cents
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Auction: " + auctionSlug) // The name of your product
                                            .build())
                                    .build())
                            .build())
                    .build();

            Session session = Session.create(params);

            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(session.getUrl())).build();
        } catch (StripeException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Error creating Stripe Checkout session: " + e.getMessage()));
        }
    }

    @GetMapping("/payment/success")
    public String handlePaymentSuccess(@RequestParam("session_id") String sessionId, Authentication authentication, Model model) {
        try {
            Stripe.apiKey = stripeApiKey;

            Session session = Session.retrieve(sessionId);
            String auctionSlug = session.getMetadata().get("auction_slug");

            // Retrieve the corresponding auction and user from the database
            Auction auction = auctionRepository.findBySlug(auctionSlug)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid auction slug: " + auctionSlug));
            AuctionUser user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            Long amountPaid = session.getAmountTotal(); // This is in the smallest currency unit, e.g., cents
            BigDecimal finalPrice = BigDecimal.valueOf(amountPaid).divide(BigDecimal.valueOf(100)); // Convert to standard currency unit, e.g., dollars

            // Update auction status and buyer
            if (auction.getStatus() != AuctionStatus.SOLD) {
                auction.setStatus(AuctionStatus.SOLD);
                auction.setBuyer(user);
                auction.setHighestBid(finalPrice); // Set the highest bid to the final price paid
                auctionRepository.save(auction);
            } else {
                // Handle the case where the auction is already sold
                model.addAttribute("error", "This auction is already sold.");
                return "error"; // Show an error page or message
            }

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

        return "redirect:/profile/auctions";
    }

    @DeleteMapping("/auctions/delete/{auctionId}")
    public ResponseEntity<?> deleteAuction(@PathVariable Long auctionId, Authentication authentication) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);

        if (auction == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Auction not found");
        }

        if (auction.getHighestBid().compareTo(BigDecimal.ZERO) == 0
                && auction.getAuctionCreator().getEmail().equals(authentication.getName())) {
            auctionRepository.delete(auction);
            return ResponseEntity.ok("Auction deleted successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You cannot delete an auction with bids or that you did not create.");
        }
    }

    @GetMapping("/auction/{slug}")
    public String viewAuctionDetail(@PathVariable("slug") String auctionSlug, Model model, Authentication authentication) {
        AuctionUser user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Auction auction = auctionRepository.findBySlug(auctionSlug)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction slug: " + auctionSlug));
        if (auction.getStatus() == AuctionStatus.SOLD) {
            model.addAttribute("errorMessage", "This auction has ended and the item has been sold.");
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
        AuctionUser currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

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
        AuctionUser currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

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

        return "redirect:/profile/auctions";
    }

    @PostMapping("/add-to-watchlist/{auctionId}")
    public ResponseEntity<?> addToWatchlist(@PathVariable Long auctionId, Authentication authentication, RedirectAttributes redirectAttributes) {
        AuctionUser currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction Id:" + auctionId));
        auctionService.addToWatchlist(currentUser, auction);
        redirectAttributes.addFlashAttribute("watchMessage", "Added to watchlist.");
        return ResponseEntity.ok("Added to watchlist");
    }

    @PostMapping("/remove-from-watchlist/{auctionId}")
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
