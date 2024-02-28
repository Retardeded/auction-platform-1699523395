package pl.use.auction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.use.auction.dto.TransactionFeedbackDTO;
import pl.use.auction.model.*;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.TransactionFeedbackRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.ProfileService;
import pl.use.auction.service.UserService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private ProfileService profileService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private TransactionFeedbackRepository transactionFeedbackRepository;

    @GetMapping("/profile/user-auctions")
    public String viewUserAuctions(Model model, Authentication authentication) {
        AuctionUser currentUser = userService.findByUsernameOrThrow(authentication.getName());
        model.addAttribute("currentUser", currentUser);

        List<Auction> allUserAuctions = currentUser.getCreatedAuctions();
        List<Auction> ongoingAuctions = allUserAuctions.stream()
                .filter(auction -> auction.getStatus() == AuctionStatus.ACTIVE)
                .collect(Collectors.toList());
        List<Auction> pastAuctions = allUserAuctions.stream()
                .filter(auction -> auction.getStatus() != AuctionStatus.ACTIVE)
                .collect(Collectors.toList());

        Map<Long, TransactionFeedback> feedbackMap = new HashMap<>();

        for (Auction auction : pastAuctions) {
            transactionFeedbackRepository.findByAuction(auction).ifPresent(feedback ->
                    feedbackMap.put(auction.getId(), feedback));
        }

        model.addAttribute("ongoingAuctions", ongoingAuctions);
        model.addAttribute("pastAuctions", pastAuctions);
        model.addAttribute("feedbackMap", feedbackMap);

        return "profile/user-auctions";
    }

    @GetMapping("/profile/purchase-auctions")
    public String viewBoughtAuctions(Model model, Authentication authentication) {
        AuctionUser currentUser = userService.findByUsernameOrThrow(authentication.getName());
        model.addAttribute("currentUser", currentUser);

        List<Auction> boughtAuctions = auctionRepository.findByBuyer(currentUser);
        Map<Long, TransactionFeedback> feedbackMap = new HashMap<>();

        for (Auction auction : boughtAuctions) {
            transactionFeedbackRepository.findByAuction(auction).ifPresent(feedback ->
                    feedbackMap.put(auction.getId(), feedback));
        }

        model.addAttribute("boughtAuctions", boughtAuctions);
        model.addAttribute("feedbackMap", feedbackMap);

        return "profile/purchase-auctions";
    }

    @GetMapping("/profile/highest-bids")
    public String viewUserHighestBids(Model model, Authentication authentication) {
        AuctionUser user = userService.findByUsernameOrThrow(authentication.getName());
        List<Auction> highestBidAuctions = auctionRepository.findByHighestBidder(user);
        model.addAttribute("highestBidAuctions", highestBidAuctions);

        return "profile/highest-bids";
    }

    @GetMapping("/profile/observed-auctions")
    public String showObservedAuctions(Model model, Authentication authentication) {
        AuctionUser currentUser = userService.findByUsernameOrThrow(authentication.getName());

        Set<Auction> observedAuctions = currentUser.getObservedAuctions();
        model.addAttribute("currentUser", currentUser); // Pass the current user to the model
        model.addAttribute("observedAuctions", observedAuctions);

        return "profile/observed-auctions";
    }

    @GetMapping("/profile/my-bids-and-watches")
    public String viewMyBidsAndWatches(Model model, Authentication authentication) {
        AuctionUser currentUser = userService.findByUsernameOrThrow(authentication.getName());

        List<AuctionStatus> statuses = List.of(AuctionStatus.ACTIVE, AuctionStatus.AWAITING_PAYMENT);
        List<Auction> highestBidAuctions = auctionRepository.findByHighestBidderAndStatusIn(currentUser, statuses);
        Set<Auction> observedAuctions = currentUser.getObservedAuctions();

        model.addAttribute("watchedAuctions", observedAuctions);
        model.addAttribute("bidAuctions", highestBidAuctions);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("zero", BigDecimal.ZERO);

        return "profile/my-bids-and-watches";
    }

    @GetMapping("/user/{username}")
    public String viewUserProfile(@PathVariable("username") String username, Authentication authentication, Model model) {
        AuctionUser currentUser = userService.findByUsernameOrThrow(authentication.getName());
        AuctionUser user = userService.findByUsernameOrThrow(username);

        List<TransactionFeedback> feedbackList = transactionFeedbackRepository.findBySellerOrBuyer(user, user);
        long totalFeedback = feedbackList.size();

        String cumulativeRating = profileService.calculateCumulativeRating(feedbackList, totalFeedback);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("profileUser", user);
        model.addAttribute("feedbackList", feedbackList);
        model.addAttribute("cumulativeRating", cumulativeRating);

        return "profile/user-profile";
    }

    @GetMapping("/profile")
    public String viewProfile(Model model, Authentication authentication) {
        AuctionUser currentUser = userService.findByUsernameOrThrow(authentication.getName());
        model.addAttribute("currentUser", currentUser);
        return "profile/profile";
    }

    @GetMapping("/profile/edit")
    public String editProfile(Model model, Authentication authentication) {
        AuctionUser user = userService.findByUsernameOrThrow(authentication.getName());
        model.addAttribute("user", user);
        return "profile/profile-edit";
    }

    @GetMapping("/profile/change-password")
    public String showChangePasswordForm(Model model, Authentication authentication) {
        AuctionUser user = userService.findByUsernameOrThrow(authentication.getName());
        model.addAttribute("user", user);
        return "profile/change-password";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute AuctionUser updatedUser, Authentication authentication) {
        userService.updateProfile(authentication.getName(), updatedUser);
        return "redirect:/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmNewPassword") String confirmNewPassword,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        AuctionUser user = userService.findByUsernameOrThrow(authentication.getName());

        PasswordChangeResult result = userService.changeUserPassword(oldPassword, newPassword, confirmNewPassword, user);

        switch (result) {
            case INVALID_OLD_PASSWORD:
                model.addAttribute("error", "The current password is incorrect.");
                break;
            case PASSWORD_MISMATCH:
                model.addAttribute("error", "The new passwords do not match.");
                break;
            default:
                model.addAttribute("error", "Password change failed.");
                break;
            case SUCCESS:
                redirectAttributes.addFlashAttribute("message", "Password changed successfully!");
                return "redirect:/profile";
        }
        model.addAttribute("user", user);
        return "profile/change-password";
    }

    @GetMapping("/rate-auction/{slug}")
    public String showRatingPage(@PathVariable("slug") String auctionSlug, Model model, Authentication authentication) {
        Auction auction = auctionRepository.findBySlug(auctionSlug)
                .orElseThrow(() -> new IllegalArgumentException("Invalid auction slug: " + auctionSlug));

        AuctionUser currentUser = userService.findByUsernameOrThrow(authentication.getName());

        String role = auction.getAuctionCreator().equals(currentUser) ? "seller" : "buyer";

        model.addAttribute("auction", auction);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("role", role);

        return "profile/auction-feedback";
    }

    @PostMapping("/rate-auction/{slug}/buyer")
    @ResponseBody
    public ResponseEntity<?> submitBuyerFeedback(@PathVariable("slug") String auctionSlug,
                                                 @ModelAttribute TransactionFeedbackDTO transactionFeedbackDTO,
                                                 Authentication authentication) {
        Auction auction = auctionRepository.findBySlug(auctionSlug)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Auction slug: " + auctionSlug));
        AuctionUser buyer = userService.findByUsernameOrThrow(authentication.getName());

        TransactionFeedback feedback = transactionFeedbackRepository.findByAuction(auction)
                .orElseGet(() -> {
                    TransactionFeedback newFeedback = new TransactionFeedback();
                    newFeedback.setAuction(auction);
                    newFeedback.setSeller(auction.getAuctionCreator());
                    newFeedback.setBuyer(buyer);
                    return newFeedback;
                });

        if (feedback.getBuyer().equals(buyer)) {
            if (feedback.getRatingByBuyer() != null) {
                return ResponseEntity.badRequest().body("Feedback already submitted for this auction by the buyer.");
            }
            feedback.setCommentByBuyer(transactionFeedbackDTO.getComment());
            feedback.setRatingByBuyer(transactionFeedbackDTO.getRating());
            feedback.setDateOfBuyerFeedback(LocalDateTime.now());
        } else {
            return ResponseEntity.badRequest().body("You are not authorized to submit feedback as the buyer for this auction.");
        }

        transactionFeedbackRepository.save(feedback);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/rate-auction/{slug}/seller")
    @ResponseBody
    public ResponseEntity<?> submitSellerFeedback(@PathVariable("slug") String auctionSlug,
                                                  @ModelAttribute TransactionFeedbackDTO transactionFeedbackDTO,
                                                  Authentication authentication) {
        Auction auction = auctionRepository.findBySlug(auctionSlug)
                .orElseThrow(() -> new IllegalArgumentException("Invalid Auction slug: " + auctionSlug));
        AuctionUser seller = userService.findByUsernameOrThrow(authentication.getName());

        if (!auction.getAuctionCreator().equals(seller)) {
            return ResponseEntity.badRequest().body("You are not authorized to submit feedback as the seller for this auction.");
        }

        TransactionFeedback feedback = transactionFeedbackRepository.findByAuction(auction)
                .orElseGet(() -> {
                    TransactionFeedback newFeedback = new TransactionFeedback();
                    newFeedback.setAuction(auction);
                    newFeedback.setSeller(seller);
                    newFeedback.setBuyer(auction.getBuyer());
                    return newFeedback;
                });

        if (feedback.getRatingBySeller() != null) {
            return ResponseEntity.badRequest().body("Feedback already submitted for this auction by the seller.");
        }

        feedback.setCommentBySeller(transactionFeedbackDTO.getComment());
        feedback.setRatingBySeller(transactionFeedbackDTO.getRating());
        feedback.setDateOfSellerFeedback(LocalDateTime.now());

        transactionFeedbackRepository.save(feedback);
        return ResponseEntity.ok().build();
    }
}