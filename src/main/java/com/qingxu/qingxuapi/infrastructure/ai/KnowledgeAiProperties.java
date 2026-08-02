package com.qingxu.qingxuapi.infrastructure.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "qingxu.knowledge.ai")
public class KnowledgeAiProperties {
    private boolean enabled = true;
    private String baseUrl = "http://127.0.0.1:8090";
    private String startPath = "/rag/index";
    private String graphStartPath = "/rag/graph/build";
    private String graphStatusPath = "/rag/graph/status/{graphJobId}";
    private String cancelPath = "/rag/index/cancel";
    private String callbackToken = "dev-internal-token";
}
