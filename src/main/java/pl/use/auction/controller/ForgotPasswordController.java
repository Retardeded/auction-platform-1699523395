package pl.use.auction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.use.auction.service.UserService;

@Controller
public class ForgotPasswordController {

    @Autowired
    private UserService userService;

    @GetMapping("/forgot-password")
    public String displayForgotPasswordPage() {
        return "authentication/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPasswordForm(@RequestParam("email") String userEmail) {
        userService.processForgotPassword(userEmail);
        return "redirect:/password-reset-requested";
    }

    @GetMapping("/password-reset-requested")
    public String displayPasswordResetRequested() {
        return "authentication/password-reset-requested";
    }
}
