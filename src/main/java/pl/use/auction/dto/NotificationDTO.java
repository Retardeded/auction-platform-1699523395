package pl.use.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private String email;
    private String description;
    private String action;
    private boolean read;
}
