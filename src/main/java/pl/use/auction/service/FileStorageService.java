package pl.use.auction.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    String save(MultipartFile file, String uploadDir) throws IOException;
}