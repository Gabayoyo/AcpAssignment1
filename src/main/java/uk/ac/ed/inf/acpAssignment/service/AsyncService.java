package uk.ac.ed.inf.acpAssignment.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service

// the magic annotation to enable async
@EnableAsync

public class AsyncService {

    @Async
    public CompletableFuture<String> asyncMethod(){
        System.out.println("Async method init at: " + LocalDateTime.now());
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            System.err.println("Error sleeping: " + e.getMessage());
        }
        System.out.println("Async method terminated at: " + LocalDateTime.now());
        return CompletableFuture.completedFuture("Done for " + UUID.randomUUID());
    }
}
