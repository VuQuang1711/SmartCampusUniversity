package com.example.sis.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CloudflareTunnelService {

    private static final Logger log = LoggerFactory.getLogger(CloudflareTunnelService.class);
    // Bắt đúng URL tunnel ngẫu nhiên (loại trừ api.trycloudflare.com)
    private static final Pattern URL_PATTERN = Pattern.compile("https://(?!api\\.)([a-zA-Z0-9-]+)\\.trycloudflare\\.com");

    @Value("${app.cloudflare.tunnel.enabled:true}")
    private boolean enabled;

    @Value("${server.port:8081}")
    private int serverPort;

    private Process tunnelProcess;
    private volatile String publicUrl = null;

    @EventListener(ApplicationReadyEvent.class)
    public void startTunnel() {
        if (!enabled) {
            log.info(">>> Cloudflare Tunnel is disabled via app.cloudflare.tunnel.enabled=false");
            return;
        }

        File exeFile = findCloudflaredExecutable();
        if (exeFile == null || !exeFile.exists()) {
            log.warn(">>> [Cloudflare Tunnel] cloudflared.exe not found in project or PATH. Skipping auto-tunnel.");
            return;
        }

        Thread tunnelThread = new Thread(() -> {
            try {
                log.info(">>> [Cloudflare Tunnel] Starting tunnel for http://localhost:{}...", serverPort);
                ProcessBuilder pb = new ProcessBuilder(
                        exeFile.getAbsolutePath(),
                        "tunnel",
                        "--url",
                        "http://localhost:" + serverPort
                );
                pb.redirectErrorStream(true);
                tunnelProcess = pb.start();

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(tunnelProcess.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (publicUrl == null) {
                            Matcher m = URL_PATTERN.matcher(line);
                            if (m.find()) {
                                publicUrl = m.group();
                                printBanner(publicUrl);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error(">>> [Cloudflare Tunnel] Error running tunnel: {}", e.getMessage());
            }
        }, "cloudflare-tunnel-worker");

        tunnelThread.setDaemon(true);
        tunnelThread.start();
    }

    @PreDestroy
    public void stopTunnel() {
        if (tunnelProcess != null && tunnelProcess.isAlive()) {
            log.info(">>> [Cloudflare Tunnel] Stopping tunnel, disconnecting public URL...");
            tunnelProcess.destroy();
            try {
                tunnelProcess.waitFor(java.util.concurrent.TimeUnit.SECONDS.toMillis(3), java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {}
            if (tunnelProcess.isAlive()) {
                tunnelProcess.destroyForcibly();
            }
            log.info(">>> [Cloudflare Tunnel] Tunnel closed. Public URL is now offline.");
        }
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    private File findCloudflaredExecutable() {
        // 1. Check in root project directory
        File local = new File("cloudflared.exe");
        if (local.exists()) return local;

        // 2. Check current user directory
        File userBin = new File(System.getProperty("user.home"), "cloudflared.exe");
        if (userBin.exists()) return userBin;

        return null;
    }

    private void printBanner(String url) {
        String border = "+--------------------------------------------------------------------------+";
        System.out.println();
        System.out.println(border);
        System.out.println("|  [ONLINE] HUIT SIS & E-LEARNING PUBLIC TUNNEL READY                      |");
        System.out.println(border);
        System.out.println("  * Public URL : " + url);
        System.out.println("  * E-Learning : " + url + "/ELearning");
        System.out.println("  * Local URL  : http://localhost:" + serverPort);
        System.out.println("  * Security   : Auto-closes immediately when application stops.");
        System.out.println(border);
        System.out.println();
    }
}
