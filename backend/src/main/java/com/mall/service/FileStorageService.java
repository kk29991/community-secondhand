package com.mall.service;

import com.mall.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 本地文件存储服务: 保存上传图片并返回可访问 URL
 */
@Slf4j
@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    private final Path rootDir;
    private final String urlPrefix;

    public FileStorageService(@Value("${mall.upload.path:./uploads}") String uploadPath,
                              @Value("${mall.upload.url-prefix:http://localhost:9090/api/uploads}") String urlPrefix) {
        this.rootDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        this.urlPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            log.error("创建上传目录失败: {}", rootDir, e);
        }
    }

    /**
     * 保存图片文件
     *
     * @return 可访问的完整 URL
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BusinessException("仅支持 jpg/jpeg/png/gif/webp/bmp 格式的图片");
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Path targetDir = rootDir.resolve(datePath);
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return urlPrefix + "/" + datePath + "/" + filename;
        } catch (IOException e) {
            log.error("保存上传文件失败", e);
            throw new BusinessException("文件上传失败, 请重试");
        }
    }
}
