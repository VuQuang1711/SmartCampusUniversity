package com.example.sis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@SpringBootApplication
public class SisApplication {

    public static void main(String[] args) {
        int port = resolveServerPort(args, 8081);
        freePortIfOccupied(port);

        SpringApplication.run(SisApplication.class, args);
    }

    private static int resolveServerPort(String[] args, int defaultPort) {
        if (args != null) {
            for (String arg : args) {
                if (arg != null && arg.startsWith("--server.port=")) {
                    try {
                        return Integer.parseInt(arg.substring("--server.port=".length()).trim());
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        return defaultPort;
    }

    /**
     * Tự động phát hiện và tắt tiến trình đang chiếm giữ port trên Windows/Linux/Mac
     */
    private static void freePortIfOccupied(int port) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            long currentPid = ProcessHandle.current().pid();

            if (os.contains("win")) {
                // Lệnh tìm PID đang lắng nghe trên cổng: netstat -ano | findstr :PORT
                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "netstat -ano | findstr LISTENING | findstr :" + port);
                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        String[] parts = line.split("\\s+");
                        if (parts.length > 0) {
                            String pidStr = parts[parts.length - 1];
                            try {
                                long pid = Long.parseLong(pidStr);
                                if (pid > 0 && pid != currentPid) {
                                    System.out.println(">>> [SisApplication] Port " + port + " occupied by PID " + pid + ". Terminating stale process...");
                                    new ProcessBuilder("taskkill", "/F", "/PID", String.valueOf(pid)).start().waitFor();
                                    Thread.sleep(600);
                                    System.out.println(">>> [SisApplication] Port " + port + " freed successfully.");
                                }
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            } else {
                new ProcessBuilder("sh", "-c", "fuser -k " + port + "/tcp").start().waitFor();
            }
        } catch (Exception e) {
            System.err.println(">>> [SisApplication] Port check skipped: " + e.getMessage());
        }
    }
}
