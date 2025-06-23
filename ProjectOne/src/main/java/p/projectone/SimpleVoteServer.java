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
 * 简化的投票服务器
 * 实现分布式投票系统的服务器端功能
 * 展示分布式算法：锁定、同步、调度、复制
 * 
 * @author Distributed Systems Team
 */
public class SimpleVoteServer {
    
    private static final int PORT = 8080;
    private static final Map<String, Integer> voteCounts = new ConcurrentHashMap<>();
    private static final Map<String, String> userVotes = new ConcurrentHashMap<>();
    private static final Map<String, ReentrantLock> userLocks = new ConcurrentHashMap<>();
    
    // 统计信息
    private static final AtomicInteger totalRequests = new AtomicInteger(0);
    private static final AtomicInteger successfulVotes = new AtomicInteger(0);
    private static final AtomicInteger failedVotes = new AtomicInteger(0);
    
    // 候选人信息
    private static final String[] candidates = {"Alice", "Bob", "Charlie"};
    private static final String[] candidateIds = {"1", "2", "3"};
    
    public static void main(String[] args) throws IOException {
        // 初始化投票数据
        for (String id : candidateIds) {
            voteCounts.put(id, 0);
        }
        
        // 创建HTTP服务器
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // 设置线程池（调度）
        server.setExecutor(Executors.newFixedThreadPool(10));
        
        // 注册路由处理器
        server.createContext("/api/vote", new VoteHandler());
        server.createContext("/api/vote/results", new ResultsHandler());
        server.createContext("/api/stats", new StatsHandler());
        server.createContext("/", new HomeHandler());
        
        // 启动服务器
        server.start();
        
        System.out.println("=== 分布式投票系统服务器已启动 ===");
        System.out.println("服务器地址: http://localhost:" + PORT);
        System.out.println("API接口:");
        System.out.println("  POST /api/vote - 投票");
        System.out.println("  GET  /api/vote/results - 获取结果");
        System.out.println("  GET  /api/stats - 获取统计");
        System.out.println("=====================================");
        System.out.println("演示分布式算法:");
        System.out.println("1. 锁定 (Locking) - 防止重复投票");
        System.out.println("2. 同步 (Synchronization) - 并发控制");
        System.out.println("3. 调度 (Scheduling) - 线程池处理");
        System.out.println("4. 复制 (Replication) - 数据一致性");
        System.out.println("=====================================");
    }
    
    /**
     * 投票处理器
     * 实现分布式锁和并发控制
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
                // 读取请求数据
                String requestBody = new String(exchange.getRequestBody().readAllBytes());
                System.out.println("收到投票请求: " + requestBody);
                
                // 解析请求参数（简化版）
                String userId = extractUserId(requestBody);
                String candidateId = extractCandidateId(requestBody);
                String candidateName = extractCandidateName(requestBody);
                
                if (userId == null || candidateId == null) {
                    sendResponse(exchange, 400, "Invalid request data");
                    failedVotes.incrementAndGet();
                    return;
                }
                
                // 分布式锁实现
                ReentrantLock userLock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock());
                
                boolean voteSuccess = false;
                if (userLock.tryLock()) {
                    try {
                        System.out.println("获取分布式锁成功 - 用户: " + userId);
                        
                        // 检查是否已投票
                        if (userVotes.containsKey(userId)) {
                            System.out.println("用户已投票 - 用户: " + userId);
                            sendResponse(exchange, 400, "User already voted");
                            failedVotes.incrementAndGet();
                            return;
                        }
                        
                        // 模拟处理延迟
                        Thread.sleep(500);
                        
                        // 记录投票
                        userVotes.put(userId, candidateId);
                        voteCounts.put(candidateId, voteCounts.get(candidateId) + 1);
                        
                        System.out.println("投票成功 - 用户: " + userId + " 投给 " + candidateName);
                        successfulVotes.incrementAndGet();
                        voteSuccess = true;
                        
                    } finally {
                        userLock.unlock();
                        System.out.println("释放分布式锁 - 用户: " + userId);
                    }
                } else {
                    System.out.println("获取分布式锁失败 - 用户: " + userId + " 正在处理中");
                    sendResponse(exchange, 429, "User is being processed");
                    failedVotes.incrementAndGet();
                    return;
                }
                
                if (voteSuccess) {
                    sendResponse(exchange, 200, "Vote successful");
                }
                
            } catch (Exception e) {
                System.err.println("处理投票请求时发生错误: " + e.getMessage());
                sendResponse(exchange, 500, "Internal server error");
                failedVotes.incrementAndGet();
            }
        }
    }
    
    /**
     * 结果查询处理器
     */
    static class ResultsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed");
                return;
            }
            
            // 构建JSON响应
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
     * 统计信息处理器
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
     * 首页处理器
     */
    static class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = "<html><body>" +
                "<h1>分布式投票系统服务器</h1>" +
                "<p>服务器运行正常</p>" +
                "<p>API接口:</p>" +
                "<ul>" +
                "<li>POST /api/vote - 投票</li>" +
                "<li>GET /api/vote/results - 获取结果</li>" +
                "<li>GET /api/stats - 获取统计</li>" +
                "</ul>" +
                "</body></html>";
            
            exchange.getResponseHeaders().add("Content-Type", "text/html");
            sendResponse(exchange, 200, html);
        }
    }
    
    /**
     * 发送HTTP响应
     */
    private static void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        exchange.sendResponseHeaders(code, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
    
    /**
     * 从请求体中提取用户ID（简化实现）
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
     * 从请求体中提取候选人ID（简化实现）
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
     * 从请求体中提取候选人姓名（简化实现）
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