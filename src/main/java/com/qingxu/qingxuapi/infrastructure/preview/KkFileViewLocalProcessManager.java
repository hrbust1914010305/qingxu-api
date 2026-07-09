package com.qingxu.qingxuapi.infrastructure.preview;

import com.qingxu.qingxuapi.common.config.QingxuPreviewProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class KkFileViewLocalProcessManager {

    private static final int CONNECT_TIMEOUT_MILLIS = 1000;
    private static final int READ_TIMEOUT_MILLIS = 1000;
    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);

    private final QingxuPreviewProperties previewProperties;
    private Process managedProcess;

    @EventListener(ApplicationReadyEvent.class)
    public void startIfNecessary() {
        QingxuPreviewProperties.LocalService localService = previewProperties.getLocalService();
        if (!previewProperties.isEnabled() || localService == null || !localService.isAutoStart()) {
            return;
        }
        if (isKkFileViewReachable()) {
            log.info("kkFileView local service is already reachable at {}", previewProperties.getKkFileViewBaseUrl());
            return;
        }
        Path jarPath = Path.of(localService.getJarPath());
        if (!Files.isRegularFile(jarPath)) {
            log.warn("kkFileView jar not found, skip local auto start. jarPath={}", jarPath);
            return;
        }
        try {
            managedProcess = startProcess(localService, jarPath);
            if (waitUntilReachable(localService.getStartupTimeout())) {
                log.info("kkFileView local service started at {}", previewProperties.getKkFileViewBaseUrl());
            } else {
                log.warn("kkFileView local service was started but is not reachable within {}. logFile={}",
                        localService.getStartupTimeout(), localService.getLogFile());
            }
        } catch (IOException ex) {
            log.warn("Failed to start kkFileView local service. jarPath={}, logFile={}",
                    jarPath, localService.getLogFile(), ex);
        }
    }

    @PreDestroy
    public void stopManagedProcess() {
        if (managedProcess == null || !managedProcess.isAlive()) {
            return;
        }
        log.info("Stopping kkFileView local service started by qingxu-api");
        managedProcess.destroy();
        try {
            if (!managedProcess.waitFor(5, TimeUnit.SECONDS)) {
                managedProcess.destroyForcibly();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            managedProcess.destroyForcibly();
        }
    }

    private Process startProcess(QingxuPreviewProperties.LocalService localService, Path jarPath) throws IOException {
        List<String> command = new ArrayList<>();
        command.add(localService.getJavaCommand());
        command.add("-jar");
        command.add(jarPath.toAbsolutePath().toString());
        ProcessBuilder builder = new ProcessBuilder(command);
        if (localService.getWorkingDirectory() != null && !localService.getWorkingDirectory().isBlank()) {
            builder.directory(Path.of(localService.getWorkingDirectory()).toFile());
        }
        if (localService.getBinFolder() != null && !localService.getBinFolder().isBlank()) {
            builder.environment().put("KKFILEVIEW_BIN_FOLDER", localService.getBinFolder());
        }
        if (localService.getTrustHost() != null && !localService.getTrustHost().isBlank()) {
            builder.environment().put("KK_TRUST_HOST", localService.getTrustHost());
        }
        if (localService.getNotTrustHost() != null && !localService.getNotTrustHost().isBlank()) {
            builder.environment().put("KK_NOT_TRUST_HOST", localService.getNotTrustHost());
        }
        builder.redirectErrorStream(true);
        if (localService.getLogFile() != null && !localService.getLogFile().isBlank()) {
            Path logPath = Path.of(localService.getLogFile());
            Path parent = logPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logPath.toFile()));
        } else {
            builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        }
        log.info("Starting kkFileView local service. command={}, directory={}, logFile={}",
                command, builder.directory(), localService.getLogFile());
        return builder.start();
    }

    private boolean waitUntilReachable(Duration timeout) {
        Instant deadline = Instant.now().plus(timeout == null ? Duration.ZERO : timeout);
        while (Instant.now().isBefore(deadline)) {
            if (isKkFileViewReachable()) {
                return true;
            }
            try {
                Thread.sleep(POLL_INTERVAL.toMillis());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return isKkFileViewReachable();
    }

    private boolean isKkFileViewReachable() {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(previewProperties.getKkFileViewBaseUrl())
                    .toURL()
                    .openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestMethod("GET");
            int status = connection.getResponseCode();
            return status >= 200 && status < 500;
        } catch (Exception ex) {
            return false;
        }
    }
}
