package com.prospecta.shared.utils;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

@Slf4j
public final class SsrfValidator {

    private SsrfValidator() {
    }

    /**
     * Validates that an external URL is safe to fetch (HTTP/HTTPS only, no private/link-local/loopback IP).
     *
     * @param urlString The candidate URL
     * @return true if the URL is safe, false otherwise
     */
    public static boolean isSafeUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return false;
        }

        try {
            URI uri = URI.create(urlString.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                log.warn("SSRF blocked: Invalid scheme '{}' in URL '{}'", scheme, urlString);
                return false;
            }

            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                log.warn("SSRF blocked: Missing host in URL '{}'", urlString);
                return false;
            }

            // Reject obvious localhost and link-local names
            if (host.equalsIgnoreCase("localhost") || host.equalsIgnoreCase("127.0.0.1") || host.equalsIgnoreCase("::1")) {
                log.warn("SSRF blocked: Localhost host '{}'", host);
                return false;
            }

            // Resolve DNS addresses and check each
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (isPrivateOrRestricted(addr)) {
                    log.warn("SSRF blocked: Host '{}' resolved to restricted IP '{}'", host, addr.getHostAddress());
                    return false;
                }
            }

            return true;
        } catch (IllegalArgumentException | UnknownHostException e) {
            log.warn("SSRF blocked: URL resolution error for '{}': {}", urlString, e.getMessage());
            return false;
        }
    }

    public static boolean isPrivateOrRestricted(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()) {
            return true;
        }

        if (address.isSiteLocalAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();
        if (bytes.length == 4) { // IPv4
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;

            // 10.0.0.0/8
            if (b0 == 10) return true;
            // 172.16.0.0/12
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return true;
            // 192.168.0.0/16
            if (b0 == 192 && b1 == 168) return true;
            // 169.254.0.0/16 (AWS/Cloud metadata)
            if (b0 == 169 && b1 == 254) return true;
            // 127.0.0.0/8
            if (b0 == 127) return true;
            // 0.0.0.0/8
            if (b0 == 0) return true;
        }

        return false;
    }
}
