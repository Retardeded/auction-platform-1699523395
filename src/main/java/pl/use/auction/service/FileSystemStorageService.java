package pl.use.auction.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileSystemStorageService implements FileStorageService {

    @Override
    public String save(MultipartFile file, String uploadDir) throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
        Path filePath = Paths.get(uploadDir, file.getOriginalFilename());
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "auctionImages/" + file.getOriginalFilename();
    }
}
