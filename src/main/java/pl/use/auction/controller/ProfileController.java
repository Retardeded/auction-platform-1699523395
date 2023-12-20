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
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.UserRepository;

@Controller
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

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
}