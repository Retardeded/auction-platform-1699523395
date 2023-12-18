package pl.use.auction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.use.auction.dto.UserRegistrationDto;
import pl.use.auction.model.AuctionUser;
import pl.use.auction.repository.UserRepository;
import pl.use.auction.service.UserService;

import java.util.Optional;
import java.util.UUID;

@Controller
public class RegistrationController {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    UserService userService;

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("userRegister", new UserRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String registerUserAccount(@ModelAttribute("userRegister") UserRegistrationDto registrationDto) {
        String verificationToken = UUID.randomUUID().toString();
        var newUser = userService.registerNewUser(registrationDto, verificationToken);

        System.out.println(newUser);

        userService.sendVerificationEmail(newUser, verificationToken);
        return "redirect:/thank-you";
    }
    @GetMapping("/thank-you")
    public String thankYou() {
        return "thank-you"; // name of the HTML file without the extension
    }

    @GetMapping("/verify")
    public String verifyUser(@RequestParam String token) {
        Optional<AuctionUser> user = userRepository.findByVerificationToken(token);
        if (user.isPresent()) {
            AuctionUser auctionUser = user.get();
            auctionUser.setVerified(true);
            userRepository.save(auctionUser);

            System.out.println("User Verified: " + auctionUser.getEmail());

            return "redirect:/login";
        } else {
            return "error";
        }
    }
}