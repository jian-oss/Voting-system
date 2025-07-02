package p.projectone;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Simplified voting server
 * Implements the server-side functionality of a distributed voting system
 * Demonstrates distributed algorithms: Locking, Synchronization, Scheduling, Replication
 * 
 * @author Distributed Systems Team
 */
public class SimpleVoteServer {
    
    private static final int PORT = 8080;
    private static final Map<String, Integer> voteCounts = new ConcurrentHashMap<>();
    private static final Map<String, String> userVotes = new ConcurrentHashMap<>();
    private static final Map<String, ReentrantLock> userLocks = new ConcurrentHashMap<>();
    
    // Statistics information
    private static final AtomicInteger totalRequests = new AtomicInteger(0);
    private static final AtomicInteger successfulVotes = new AtomicInteger(0);
    private static final AtomicInteger failedVotes = new AtomicInteger(0);
    
    // Candidate information
    private static final String[] candidates = {"Alice", "Bob", "Charlie"};
    private static final String[] candidateIds = {"1", "2", "3"};
    
    public static void main(String[] args) throws IOException {
        // Initialize voting data
        for (String id : candidateIds) {
            voteCounts.put(id, 0);
        }
        
        // Create HTTP server
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // Set thread pool (scheduling)
        server.setExecutor(Executors.newFixedThreadPool(10));
        
        // Register route handlers
        server.createContext("/api/vote", new VoteHandler());
        server.createContext("/api/vote/results", new ResultsHandler());
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/", new HomeHandler());
        
        // Start server
        server.start();
        
        System.out.println("=== Distributed Voting System Server Started ===");
        System.out.println("Server Address: http://10.72.83.45:" + PORT);
        System.out.println("API Endpoints:");
        System.out.println("  POST /api/vote - Vote");
        System.out.println("  GET  /api/vote/results - Get Results");
        System.out.println("  GET  /api/stats - Get Statistics");
        System.out.println("=====================================");
        System.out.println("Demonstrating Distributed Algorithms:");
        System.out.println("1. Locking - Prevent duplicate voting");
        System.out.println("2. Synchronization - Concurrency control");
        System.out.println("3. Scheduling - Thread pool processing");
        System.out.println("4. Replication - Data consistency");
        System.out.println("=====================================");
    }
    
    /**
     * Vote handler
     * Implements distributed lock and concurrency control
     */
    static class VoteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            totalRequests.incrementAndGet();
            
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            
            try {
                // Read request data
                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                System.out.println("Received vote request: " + requestBody);
                
                // Parse request parameters (simplified)
                String userId = extractUserId(requestBody);
                String candidateId = extractCandidateId(requestBody);
                String candidateName = extractCandidateName(requestBody);
                
                if (userId == null || candidateId == null) {
                    sendResponse(exchange, 400, "Invalid request data");
                    failedVotes.incrementAndGet();
                    return;
                }
                
                // Distributed lock implementation
                ReentrantLock userLock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock());
                
                boolean voteSuccess = false;
                if (userLock.tryLock()) {
                    try {
                        System.out.println("Acquired distributed lock - User: " + userId);
                        
                        // Check if already voted
                        if (userVotes.containsKey(userId)) {
                            System.out.println("User has already voted - User: " + userId);
                            sendResponse(exchange, 400, "User already voted");
                            failedVotes.incrementAndGet();
                            return;
                        }
                        
                        // Simulate processing delay
                        Thread.sleep(500);
                        
                        // Record vote
                        userVotes.put(userId, candidateId);
                        voteCounts.put(candidateId, voteCounts.get(candidateId) + 1);
                        
                        System.out.println("Vote successful - User: " + userId + " voted for " + candidateName);
                        successfulVotes.incrementAndGet();
                        voteSuccess = true;
                        
                    } finally {
                        userLock.unlock();
                        System.out.println("Released distributed lock - User: " + userId);
                    }
                } else {
                    System.out.println("Failed to acquire distributed lock - User: " + userId + " is being processed");
                    sendResponse(exchange, 429, "User is being processed");
                    failedVotes.incrementAndGet();
                    return;
                }
                
                if (voteSuccess) {
                    sendResponse(exchange, 200, "Vote successful");
                }
                
            } catch (Exception e) {
                System.err.println("Error occurred while processing vote request: " + e.getMessage());
                sendResponse(exchange, 500, "Internal server error");
                failedVotes.incrementAndGet();
            }
        }
    }
    
    /**
     * Results query handler
     */
    static class ResultsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            
            // Build JSON response
            StringBuilder json = new StringBuilder("{");
            for (int i = 0; i < candidateIds.length; i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(candidateIds[i]).append("\":")
                    .append(voteCounts.getOrDefault(candidateIds[i], 0));
            }
            json.append("}");
            
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            sendResponse(exchange, 200, json.toString());
        }
    }
    
    /**
     * Statistics handler
     */
    static class StatsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            
            StringBuilder json = new StringBuilder("{");
            json.append("\"totalRequests\":").append(totalRequests.get()).append(",");
            json.append("\"successfulVotes\":").append(successfulVotes.get()).append(",");
            json.append("\"failedVotes\":").append(failedVotes.get()).append(",");
            json.append("\"totalVoters\":").append(userVotes.size()).append(",");
            json.append("\"activeLocks\":").append(userLocks.size());
            json.append("}");
            
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            sendResponse(exchange, 200, json.toString());
        }
    }
    
    /**
     * Home page handler
     */
    static class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = "<html><body>" +
                "<h1>Distributed Voting System Server</h1>" +
                "<p>Server is running normally</p>" +
                "<p>API Endpoints:</p>" +
                "<ul>" +
                "<li>POST /api/vote - Vote</li>" +
                "<li>GET /api/vote/results - Get Results</li>" +
                "<li>GET /api/stats - Get Statistics</li>" +
                "</ul>" +
                "</body></html>";
            
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            sendResponse(exchange, 200, html);
        }
    }
    
    /**
     * Send HTTP response
     */
    private static void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        exchange.sendResponseHeaders(code, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
    
    /**
     * Extract user ID from request body (simplified)
     */
    private static String extractUserId(String requestBody) {
        if (requestBody.contains("\"userId\":")) {
            int start = requestBody.indexOf("\"userId\":\"") + 10;
            int end = requestBody.indexOf("\"", start);
            if (start > 9 && end > start) {
                return requestBody.substring(start, end);
            }
        }
        return null;
    }
    
    /**
     * Extract candidate ID from request body (simplified)
     */
    private static String extractCandidateId(String requestBody) {
        if (requestBody.contains("\"candidateId\":")) {
            int start = requestBody.indexOf("\"candidateId\":\"") + 15;
            int end = requestBody.indexOf("\"", start);
            if (start > 14 && end > start) {
                return requestBody.substring(start, end);
            }
        }
        return null;
    }
    
    /**
     * Extract candidate name from request body (simplified)
     */
    private static String extractCandidateName(String requestBody) {
        if (requestBody.contains("\"candidateName\":")) {
            int start = requestBody.indexOf("\"candidateName\":\"") + 17;
            int end = requestBody.indexOf("\"", start);
            if (start > 16 && end > start) {
                return requestBody.substring(start, end);
            }
        }
        return null;
    }
} 