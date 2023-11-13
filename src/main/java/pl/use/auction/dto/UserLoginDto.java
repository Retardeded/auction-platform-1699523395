package pl.use.auction.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserLoginDto {
    @Email
    private String email;
    @NotEmpty
    private String password;

    // Getters and Setters
}