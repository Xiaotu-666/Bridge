package com.bridgeinspection.service;

import com.bridgeinspection.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path uploadDir;

    public FileStorageService(@Value("${app.files.upload-dir}") String uploadDir) throws IOException {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    public String store(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String suffix = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            suffix = original.substring(dot).toLowerCase(Locale.ROOT);
        }
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String safeCategory = category == null || category.isBlank() ? "common" : category.replaceAll("[^A-Za-z0-9_-]", "");
        Path dir = uploadDir.resolve(safeCategory).resolve(date).normalize();
        try {
            Files.createDirectories(dir);
            String name = UUID.randomUUID().toString().replace("-", "") + suffix;
            Path target = dir.resolve(name).normalize();
            if (!target.startsWith(uploadDir)) {
                throw new BusinessException("非法文件路径");
            }
            file.transferTo(target);
            return uploadDir.relativize(target).toString().replace("\\", "/");
        } catch (IOException ex) {
            throw new BusinessException("文件保存失败：" + ex.getMessage());
        }
    }

    public Resource load(String relativePath) {
        try {
            Path target = uploadDir.resolve(relativePath).normalize();
            if (!target.startsWith(uploadDir) || !Files.exists(target)) {
                throw new BusinessException("文件不存在");
            }
            return new UrlResource(target.toUri());
        } catch (MalformedURLException ex) {
            throw new BusinessException("文件路径无效");
        }
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Path target = uploadDir.resolve(relativePath).normalize();
            if (!target.startsWith(uploadDir)) {
                throw new BusinessException("非法文件路径");
            }
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            throw new BusinessException("文件删除失败：" + ex.getMessage());
        }
    }
}
