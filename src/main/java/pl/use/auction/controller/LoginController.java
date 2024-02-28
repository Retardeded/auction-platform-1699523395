package pl.use.auction.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.use.auction.dto.UserLoginDto;
import pl.use.auction.service.UserService;

import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    UserService userService;

    @GetMapping("/login")
    public String showLoginForm(@RequestParam Optional<String> error, HttpServletRequest request, Model model) {
        if (error.isPresent()) {
            String authError = (String) request.getSession().getAttribute("authError");
            model.addAttribute("errorMessage", authError != null ? authError : "Invalid username or password.");
            request.getSession().removeAttribute("authError");
        }
        model.addAttribute("userLogin", new UserLoginDto());
        return "authentication/login";
    }

}