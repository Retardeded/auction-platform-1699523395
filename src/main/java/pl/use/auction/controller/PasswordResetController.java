package pl.use.auction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.use.auction.service.UserService;

@Controller
public class PasswordResetController {

    @Autowired
    private UserService userService;

    @GetMapping("/reset-password")
    public String displayResetPasswordPage(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("password") String password) {
        userService.resetPassword(token, password);
        return "redirect:/password-reset-success";
    }

    @GetMapping("/password-reset-success")
    public String displayPasswordResetSuccess() {
        return "password-reset-success";
    }
}