package pl.use.auction.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig {
    @Bean
    public Path fileStorageLocation() {
        return Paths.get("src/main/resources/static/auctionImages");
    }
}
