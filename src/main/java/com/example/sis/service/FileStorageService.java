package com.example.sis.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final long MAX_PDF_SIZE = 10L * 1024 * 1024; // 10MB
    private final Path certificateDir;

    public FileStorageService() {
        this.certificateDir = Paths.get("uploads", "certificates").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.certificateDir);
        } catch (IOException e) {
            throw new RuntimeException("Không thể khởi tạo thư mục lưu chứng chỉ PDF", e);
        }
    }

    public String storeCertificatePdf(MultipartFile file, String mssv) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng đính kèm file minh chứng định dạng PDF!");
        }
        if (file.getSize() > MAX_PDF_SIZE) {
            throw new IllegalArgumentException("Dung lượng file PDF vượt quá giới hạn 10MB!");
        }
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "certificate.pdf";
        if (!originalName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Hệ thống chỉ chấp nhận file minh chứng có định dạng .PDF!");
        }

        // Kiểm tra magic bytes %PDF-
        byte[] header = new byte[5];
        try (InputStream is = file.getInputStream()) {
            int read = is.read(header);
            String sig = read >= 4 ? new String(header, 0, 4) : "";
            if (!sig.equals("%PDF")) {
                throw new IllegalArgumentException("File tải lên không phải định dạng PDF hợp lệ!");
            }
        }

        String safeMssv = mssv.replaceAll("[^a-zA-Z0-9_-]", "");
        String storedName = safeMssv + "_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
        Path targetPath = this.certificateDir.resolve(storedName).normalize();
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        return storedName;
    }

    public Resource loadCertificatePdf(String storedFilename) throws MalformedURLException {
        if (storedFilename == null || storedFilename.contains("..")) {
            return null;
        }
        Path filePath = this.certificateDir.resolve(storedFilename).normalize();
        if (!Files.exists(filePath)) return null;
        return new UrlResource(filePath.toUri());
    }
}
