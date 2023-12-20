package pl.use.auction.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.use.auction.exception.InvalidTokenException;
import pl.use.auction.exception.TokenExpiredException;
import pl.use.auction.service.UserService;

@Controller
public class PasswordResetController {

    @Autowired
    private UserService userService;

    @GetMapping("/reset-password")
    public String displayResetPasswordPage(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "authentication/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                       @RequestParam("password") String password,
                                       RedirectAttributes redirectAttributes) {
        try {
            userService.resetPassword(token, password);
            redirectAttributes.addFlashAttribute("message", "Your password has been reset successfully.");
            return "redirect:/password-reset-success";
        } catch (TokenExpiredException ex) {
            redirectAttributes.addFlashAttribute("error", "The reset token has expired.");
        } catch (InvalidTokenException ex) {
            redirectAttributes.addFlashAttribute("error", "The reset token is invalid.");
        }
        return "redirect:/authentication/reset-password?token=" + token;
    }

    @GetMapping("/password-reset-success")
    public String displayPasswordResetSuccess() {
        return "authentication/password-reset-success";
    }
}