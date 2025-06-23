package p.projectone.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 投票调度服务
 * 实现分布式算法中的调度机制，处理投票请求的排队和调度
 * 
 * 复杂度：O(log n) - 优先队列插入和删除
 * 实现方式：线程池 + 优先队列 + 异步处理
 * 
 * @author Distributed Systems Team
 */
@Service
public class VoteSchedulingService {
    
    @Autowired
    private VoteProcessingService voteProcessingService;
    
    // 线程池配置
    private final ExecutorService executorService;
    
    // 投票请求队列（优先队列，按优先级排序）
    private final PriorityBlockingQueue<VoteRequest> voteQueue;
    
    // 统计信息
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger processedRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    
    // 调度器状态
    private volatile boolean isRunning = false;
    private Thread schedulerThread;
    
    public VoteSchedulingService() {
        // 创建线程池，用于处理投票请求
        this.executorService = new ThreadPoolExecutor(
            5,  // 核心线程数
            10, // 最大线程数
            60L, // 空闲线程存活时间
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100), // 工作队列
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
        
        // 创建优先队列，按请求时间排序
        this.voteQueue = new PriorityBlockingQueue<>(1000, 
            (r1, r2) -> Long.compare(r1.getTimestamp(), r2.getTimestamp()));
        
        // 启动调度器
        startScheduler();
    }
    
    /**
     * 提交投票请求到调度队列
     * 实现分布式算法中的调度机制
     * 
     * @param userId 用户ID
     * @param candidateId 候选人ID
     * @param candidateName 候选人姓名
     * @param sessionId 会话ID
     * @return 请求ID
     */
    public String submitVoteRequest(String userId, String candidateId, String candidateName, String sessionId) {
        VoteRequest request = new VoteRequest(userId, candidateId, candidateName, sessionId);
        
        try {
            // 添加到优先队列
            voteQueue.offer(request);
            totalRequests.incrementAndGet();
            
            System.out.println("投票请求已提交到调度队列 - 用户: " + userId + 
                             ", 候选人: " + candidateName + 
                             ", 队列大小: " + voteQueue.size());
            
            return request.getRequestId();
        } catch (Exception e) {
            failedRequests.incrementAndGet();
            System.err.println("提交投票请求失败: " + e.getMessage());
            throw new RuntimeException("提交投票请求失败", e);
        }
    }
    
    /**
     * 启动调度器
     */
    private void startScheduler() {
        if (isRunning) {
            return;
        }
        
        isRunning = true;
        schedulerThread = new Thread(this::schedulerLoop, "VoteScheduler");
        schedulerThread.setDaemon(true);
        schedulerThread.start();
        
        System.out.println("投票调度器已启动");
    }
    
    /**
     * 调度器主循环
     * 从队列中取出请求并分配给线程池处理
     */
    private void schedulerLoop() {
        while (isRunning) {
            try {
                // 从队列中取出请求（阻塞等待）
                VoteRequest request = voteQueue.take();
                
                // 提交给线程池异步处理
                executorService.submit(() -> processVoteRequest(request));
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("调度器被中断: " + e.getMessage());
                break;
            } catch (Exception e) {
                System.err.println("调度器处理请求时发生错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理投票请求
     * 
     * @param request 投票请求
     */
    @Async
    private void processVoteRequest(VoteRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            System.out.println("开始处理投票请求 - 用户: " + request.getUserId() + 
                             ", 候选人: " + request.getCandidateName());
            
            // 调用投票处理服务
            boolean success = voteProcessingService.processVote(
                request.getUserId(), 
                request.getCandidateId(), 
                request.getCandidateName(), 
                request.getSessionId()
            );
            
            if (success) {
                processedRequests.incrementAndGet();
                System.out.println("投票请求处理成功 - 用户: " + request.getUserId());
            } else {
                failedRequests.incrementAndGet();
                System.out.println("投票请求处理失败 - 用户: " + request.getUserId());
            }
            
        } catch (Exception e) {
            failedRequests.incrementAndGet();
            System.err.println("处理投票请求时发生异常: " + e.getMessage());
        } finally {
            long processingTime = System.currentTimeMillis() - startTime;
            totalProcessingTime.addAndGet(processingTime);
        }
    }
    
    /**
     * 获取调度器统计信息
     * 
     * @return 统计信息
     */
    public SchedulingStats getStats() {
        return new SchedulingStats(
            totalRequests.get(),
            processedRequests.get(),
            failedRequests.get(),
            voteQueue.size(),
            totalProcessingTime.get(),
            isRunning
        );
    }
    
    /**
     * 停止调度器
     */
    public void shutdown() {
        isRunning = false;
        if (schedulerThread != null) {
            schedulerThread.interrupt();
        }
        executorService.shutdown();
        System.out.println("投票调度器已停止");
    }
    
    /**
     * 投票请求内部类
     */
    private static class VoteRequest {
        private final String requestId;
        private final String userId;
        private final String candidateId;
        private final String candidateName;
        private final String sessionId;
        private final long timestamp;
        
        public VoteRequest(String userId, String candidateId, String candidateName, String sessionId) {
            this.requestId = java.util.UUID.randomUUID().toString();
            this.userId = userId;
            this.candidateId = candidateId;
            this.candidateName = candidateName;
            this.sessionId = sessionId;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getter方法
        public String getRequestId() { return requestId; }
        public String getUserId() { return userId; }
        public String getCandidateId() { return candidateId; }
        public String getCandidateName() { return candidateName; }
        public String getSessionId() { return sessionId; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * 调度统计信息
     */
    public static class SchedulingStats {
        private final int totalRequests;
        private final int processedRequests;
        private final int failedRequests;
        private final int queueSize;
        private final long totalProcessingTime;
        private final boolean isRunning;
        
        public SchedulingStats(int totalRequests, int processedRequests, int failedRequests, 
                             int queueSize, long totalProcessingTime, boolean isRunning) {
            this.totalRequests = totalRequests;
            this.processedRequests = processedRequests;
            this.failedRequests = failedRequests;
            this.queueSize = queueSize;
            this.totalProcessingTime = totalProcessingTime;
            this.isRunning = isRunning;
        }
        
        // Getter方法
        public int getTotalRequests() { return totalRequests; }
        public int getProcessedRequests() { return processedRequests; }
        public int getFailedRequests() { return failedRequests; }
        public int getQueueSize() { return queueSize; }
        public long getTotalProcessingTime() { return totalProcessingTime; }
        public boolean isRunning() { return isRunning; }
        public double getAverageProcessingTime() { 
            return processedRequests > 0 ? (double) totalProcessingTime / processedRequests : 0; 
        }
    }
} 