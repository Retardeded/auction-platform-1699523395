package pl.use.auction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import pl.use.auction.dto.UserRegistrationDto;
import pl.use.auction.service.UserService;

import java.util.UUID;

@Controller
public class RegistrationController {

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
        var newUser = (userService.registerNewUser(registrationDto));
        System.out.println(newUser);

        return "redirect:/login";
    }
}