package com.prospecta.enrichment.service;

import com.prospecta.shared.utils.SsrfValidator;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Service
public class WebsiteResearchService {

    private static final int TIMEOUT_MS = 5000;
    private static final int MAX_BODY_SIZE = 2 * 1024 * 1024; // 2 MB
    private static final int MAX_CLEAN_TEXT_LENGTH = 8000;
    private static final String USER_AGENT = "ProspectaBot/1.0 (+https://prospecta.sn; Commercial Intelligence)";

    public Optional<String> fetchCleanWebsiteContent(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }

        String targetUrl = url.trim();
        if (!targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
            targetUrl = "https://" + targetUrl;
        }

        // 1. Strict SSRF Validation
        if (!SsrfValidator.isSafeUrl(targetUrl)) {
            log.warn("Website research aborted: SSRF check failed for URL: {}", targetUrl);
            return Optional.empty();
        }

        try {
            log.info("Fetching company website: {}", targetUrl);
            Document doc = Jsoup.connect(targetUrl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .maxBodySize(MAX_BODY_SIZE)
                    .followRedirects(true)
                    .ignoreHttpErrors(false)
                    .get();

            // Strip irrelevant elements for LLM context
            doc.select("script, style, noscript, nav, footer, header, svg, iframe").remove();

            String title = doc.title();
            String metaDesc = doc.select("meta[name=description]").attr("content");
            String bodyText = doc.body().text();

            StringBuilder cleanContent = new StringBuilder();
            if (title != null && !title.isBlank()) {
                cleanContent.append("Titre du site: ").append(title).append("\n");
            }
            if (metaDesc != null && !metaDesc.isBlank()) {
                cleanContent.append("Description: ").append(metaDesc).append("\n");
            }
            cleanContent.append("\nContenu principal:\n").append(bodyText);

            String result = cleanContent.toString();
            if (result.length() > MAX_CLEAN_TEXT_LENGTH) {
                result = result.substring(0, MAX_CLEAN_TEXT_LENGTH) + "... [tronqué]";
            }

            return Optional.of(result);

        } catch (IOException e) {
            log.warn("Failed to fetch website content for URL '{}': {}", targetUrl, e.getMessage());
            return Optional.empty();
        }
    }
}
