package p.projectone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application for distributed online voting system
 * Demonstrates core concepts of distributed algorithms: Locking, Synchronization & Concurrency, Scheduling, Replication
 * 
 * @author Distributed Systems Team
 * @version 1.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DistributedVotingSystemApplication {

    public static void main(String[] args) {
        System.out.println("=== Distributed Online Voting System Starting ===");
        System.out.println("The system demonstrates the following distributed algorithm concepts:");
        System.out.println("1. Locking - Prevent duplicate voting");
        System.out.println("2. Synchronization & Concurrency - Handle concurrent voting requests");
        System.out.println("3. Scheduling - Queue voting requests");
        System.out.println("4. Replication - Data redundancy");
        System.out.println("=====================================");
        
        SpringApplication.run(DistributedVotingSystemApplication.class, args);
        
        System.out.println("=== System Startup Complete ===");
        System.out.println("Access URL: http://10.72.214.22:8080");
        System.out.println("WebSocket URL: ws://10.72.214.22:8080/ws");
    }
} 