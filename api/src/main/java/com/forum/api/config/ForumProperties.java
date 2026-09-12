package com.forum.api.config;

import java.nio.file.Path;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "forum")
public record ForumProperties(Path dataDir, int defaultMaxReplyDepth, List<String> corsAllowedOrigins) {
}
