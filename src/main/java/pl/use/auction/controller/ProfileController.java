package pl.use.auction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.use.auction.model.*;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.UserService;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuctionRepository auctionRepository;

    @GetMapping("/profile/user-auctions")
    public String viewUserAuctions(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();
        AuctionUser currentUser = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        model.addAttribute("currentUser", currentUser);

        List<Auction> allUserAuctions = currentUser.getCreatedAuctions();
        List<Auction> ongoingAuctions = allUserAuctions.stream()
                .filter(auction -> auction.getStatus() == AuctionStatus.ACTIVE)
                .collect(Collectors.toList());
        List<Auction> pastAuctions = allUserAuctions.stream()
                .filter(auction -> auction.getStatus() != AuctionStatus.ACTIVE)
                .collect(Collectors.toList());

        model.addAttribute("ongoingAuctions", ongoingAuctions);
        model.addAttribute("pastAuctions", pastAuctions);

        return "profile/user-auctions";
    }

    @GetMapping("/profile/purchase-auctions")
    public String viewBoughtAuctions(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();
        AuctionUser currentUser = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        model.addAttribute("currentUser", currentUser);

        List<Auction> boughtAuctions = auctionRepository.findByBuyer(currentUser);

        model.addAttribute("boughtAuctions", boughtAuctions);

        return "profile/purchase-auctions";
    }

    @GetMapping("/profile/highest-bids")
    public String viewUserHighestBids(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();
        AuctionUser user = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Auction> highestBidAuctions = auctionRepository.findByHighestBidder(user);
        model.addAttribute("highestBidAuctions", highestBidAuctions);

        return "profile/highest-bids";
    }

    @GetMapping("/profile/observed-auctions")
    public String showObservedAuctions(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();
        AuctionUser currentUser = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Set<Auction> observedAuctions = currentUser.getObservedAuctions();
        model.addAttribute("currentUser", currentUser); // Pass the current user to the model
        model.addAttribute("observedAuctions", observedAuctions);

        return "profile/observed-auctions";
    }

    @GetMapping("/profile/my-bids-and-watches")
    public String viewMyBidsAndWatches(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();
        AuctionUser currentUser = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<AuctionStatus> statuses = List.of(AuctionStatus.ACTIVE, AuctionStatus.AWAITING_PAYMENT);
        List<Auction> highestBidAuctions = auctionRepository.findByHighestBidderAndStatusIn(currentUser, statuses);
        Set<Auction> observedAuctions = currentUser.getObservedAuctions();

        model.addAttribute("watchedAuctions", observedAuctions);
        model.addAttribute("bidAuctions", highestBidAuctions);
        model.addAttribute("currentUser", currentUser);

        return "profile/my-bids-and-watches";
    }

    @GetMapping("/profile")
    public String viewProfile(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();

        AuctionUser currentUser = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        model.addAttribute("currentUser", currentUser);
        return "profile/profile";
    }

    @GetMapping("/profile/edit")
    public String editProfile(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();
        AuctionUser user = userRepository.findByEmail(currentUserName).orElse(null);
        model.addAttribute("user", user);
        return "profile/profile-edit";
    }

    @GetMapping("/profile/change-password")
    public String showChangePasswordForm(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();
        AuctionUser user = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        model.addAttribute("user", user);
        return "profile/change-password";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute AuctionUser updatedUser, Authentication authentication) {
        String email = authentication.getName();
        AuctionUser existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setFirstName(updatedUser.getFirstName());
        existingUser.setLastName(updatedUser.getLastName());
        existingUser.setLocation(updatedUser.getLocation());
        existingUser.setPhoneNumber(updatedUser.getPhoneNumber());

        userRepository.save(existingUser);

        return "redirect:/profile";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmNewPassword") String confirmNewPassword,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        AuctionUser user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

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
}