package com.mall;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 一次性工具测试: 生成种子数据要用的 BCrypt 密码哈希。
 * 运行: mvn test -Dtest=SeedHashGeneratorTest
 */
class SeedHashGeneratorTest {

    @Test
    void generateSeedHashes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String[] passwords = {"123456", "admin123", "merchant123"};
        for (String pwd : passwords) {
            String hash = encoder.encode(pwd);
            boolean verified = encoder.matches(pwd, hash);
            System.out.println("SEED_HASH|" + pwd + "|" + hash + "|verify=" + verified);
        }
    }
}
