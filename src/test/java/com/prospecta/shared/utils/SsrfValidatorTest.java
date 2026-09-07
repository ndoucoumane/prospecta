package com.prospecta.shared.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SsrfValidatorTest {

    @Test
    @DisplayName("Should block localhost and loopback URLs")
    void shouldBlockLocalhostAndLoopback() {
        assertThat(SsrfValidator.isSafeUrl("http://localhost:8080/actuator")).isFalse();
        assertThat(SsrfValidator.isSafeUrl("http://127.0.0.1:5432")).isFalse();
        assertThat(SsrfValidator.isSafeUrl("http://127.0.0.1/test")).isFalse();
    }

    @Test
    @DisplayName("Should block AWS and Cloud metadata IP 169.254.169.254")
    void shouldBlockCloudMetadata() {
        assertThat(SsrfValidator.isSafeUrl("http://169.254.169.254/latest/meta-data/")).isFalse();
    }

    @Test
    @DisplayName("Should block private subnet addresses")
    void shouldBlockPrivateSubnets() {
        assertThat(SsrfValidator.isSafeUrl("http://10.0.0.1/admin")).isFalse();
        assertThat(SsrfValidator.isSafeUrl("http://192.168.1.1/router")).isFalse();
        assertThat(SsrfValidator.isSafeUrl("http://172.16.0.5/")).isFalse();
    }

    @Test
    @DisplayName("Should reject non-HTTP schemes like file, ftp, gopher")
    void shouldRejectDangerousSchemes() {
        assertThat(SsrfValidator.isSafeUrl("file:///etc/passwd")).isFalse();
        assertThat(SsrfValidator.isSafeUrl("ftp://internal.server/file")).isFalse();
        assertThat(SsrfValidator.isSafeUrl("gopher://localhost:70/")).isFalse();
    }
}
