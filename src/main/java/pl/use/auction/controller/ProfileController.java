package pl.use.auction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.use.auction.model.Auction;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.model.PasswordChangeResult;
import pl.use.auction.repository.AuctionRepository;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private AuctionRepository auctionRepository;

    @GetMapping("/profile/auctions")
    public String viewUserAuctions(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();
        AuctionUser user = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Auction> ongoingAuctions = auctionRepository.findByUserAndEndTimeAfter(user, LocalDateTime.now());
        List<Auction> pastAuctions = auctionRepository.findByUserAndEndTimeBefore(user, LocalDateTime.now());

        model.addAttribute("ongoingAuctions", ongoingAuctions);
        model.addAttribute("pastAuctions", pastAuctions);

        return "user-auctions";
    }

    @GetMapping("/profile")
    public String viewProfile(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();

        AuctionUser user = userRepository.findByEmail(currentUserName).orElse(null);
        model.addAttribute("user", user);
        return "profile";
    }

    @GetMapping("/profile/edit")
    public String editProfile(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();
        AuctionUser user = userRepository.findByEmail(currentUserName).orElse(null);
        model.addAttribute("user", user);
        return "profile-edit";
    }

    @GetMapping("/profile/change-password")
    public String showChangePasswordForm(Model model, Authentication authentication) {
        String currentUserName = authentication.getName();
        AuctionUser user = userRepository.findByEmail(currentUserName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        model.addAttribute("user", user);
        return "profile-change-password";
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
                                 RedirectAttributes redirectAttributes) {
        AuctionUser user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        PasswordChangeResult result = userService.changeUserPassword(oldPassword, newPassword, confirmNewPassword, user);

        switch (result) {
            case INVALID_OLD_PASSWORD:
                redirectAttributes.addFlashAttribute("error", "The current password is incorrect.");
                break;
            case PASSWORD_MISMATCH:
                redirectAttributes.addFlashAttribute("error", "The new passwords do not match.");
                break;
            case SUCCESS:
                redirectAttributes.addFlashAttribute("message", "Password changed successfully!");
                break;
            default:
                redirectAttributes.addFlashAttribute("error", "Password change failed.");
        }

        return "redirect:/profile";
    }
}