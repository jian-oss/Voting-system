package p.projectone.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import p.projectone.model.Vote;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 投票处理服务
 * 实现分布式算法中的同步和并发机制
 * 
 * 复杂度：O(1) - 数据库查询和插入
 * 实现方式：事务 + 原子操作 + 并发控制
 * 
 * @author Distributed Systems Team
 */
@Service
public class VoteProcessingService {
    
    @Autowired
    private DistributedLockService lockService;
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    // 统计信息
    private final AtomicInteger totalVotes = new AtomicInteger(0);
    private final AtomicInteger successfulVotes = new AtomicInteger(0);
    private final AtomicInteger failedVotes = new AtomicInteger(0);
    
    /**
     * 处理投票请求
     * 实现分布式算法中的同步和并发机制
     * 
     * @param userId 用户ID
     * @param candidateId 候选人ID
     * @param candidateName 候选人姓名
     * @param sessionId 会话ID
     * @return 是否处理成功
     */
    @Transactional
    public boolean processVote(String userId, String candidateId, String candidateName, String sessionId) {
        totalVotes.incrementAndGet();
        
        try {
            System.out.println("开始处理投票 - 用户: " + userId + ", 候选人: " + candidateName);
            
            // 1. 获取分布式锁（防止重复投票）
            if (!lockService.tryLock(userId, sessionId)) {
                System.out.println("获取分布式锁失败 - 用户: " + userId + " 可能正在投票中");
                failedVotes.incrementAndGet();
                return false;
            }
            
            try {
                // 2. 检查用户是否已经投票
                if (hasUserVoted(userId)) {
                    System.out.println("用户已投票 - 用户: " + userId);
                    failedVotes.incrementAndGet();
                    return false;
                }
                
                // 3. 创建投票记录
                Vote vote = new Vote(userId, candidateId, candidateName);
                vote.setSessionId(sessionId);
                vote.setServerNode(getServerNodeId());
                vote.setStatus(Vote.VoteStatus.PROCESSING);
                
                // 4. 保存投票记录（事务保证原子性）
                mongoTemplate.save(vote);
                
                // 5. 更新投票状态为已确认
                vote.setStatus(Vote.VoteStatus.CONFIRMED);
                mongoTemplate.save(vote);
                
                successfulVotes.incrementAndGet();
                System.out.println("投票处理成功 - 用户: " + userId + ", 候选人: " + candidateName);
                
                return true;
                
            } finally {
                // 6. 释放分布式锁
                lockService.releaseLock(userId, sessionId);
            }
            
        } catch (Exception e) {
            failedVotes.incrementAndGet();
            System.err.println("处理投票时发生异常 - 用户: " + userId + ", 错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否已经投票
     * 
     * @param userId 用户ID
     * @return 是否已投票
     */
    private boolean hasUserVoted(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId));
        return mongoTemplate.exists(query, Vote.class);
    }
    
    /**
     * 获取服务器节点ID
     * 
     * @return 服务器节点ID
     */
    private String getServerNodeId() {
        return "server-" + System.getProperty("server.port", "8080") + "-" + 
               Thread.currentThread().getId();
    }
    
    /**
     * 获取投票统计信息
     * 
     * @return 统计信息
     */
    public VoteStats getVoteStats() {
        return new VoteStats(
            totalVotes.get(),
            successfulVotes.get(),
            failedVotes.get()
        );
    }
    
    /**
     * 获取候选人投票结果
     * 
     * @param candidateId 候选人ID
     * @return 投票数量
     */
    public long getCandidateVoteCount(String candidateId) {
        Query query = new Query(Criteria.where("candidateId").is(candidateId)
                .and("status").is(Vote.VoteStatus.CONFIRMED));
        return mongoTemplate.count(query, Vote.class);
    }
    
    /**
     * 获取所有候选人投票结果
     * 
     * @return 投票结果映射
     */
    public java.util.Map<String, Long> getAllCandidateVoteCounts() {
        java.util.Map<String, Long> results = new java.util.HashMap<>();
        
        // 这里应该从候选人集合中获取所有候选人
        // 简化实现，直接查询投票记录
        Query query = new Query(Criteria.where("status").is(Vote.VoteStatus.CONFIRMED));
        java.util.List<Vote> votes = mongoTemplate.find(query, Vote.class);
        
        for (Vote vote : votes) {
            String candidateId = vote.getCandidateId();
            results.put(candidateId, results.getOrDefault(candidateId, 0L) + 1);
        }
        
        return results;
    }
    
    /**
     * 投票统计信息
     */
    public static class VoteStats {
        private final int totalVotes;
        private final int successfulVotes;
        private final int failedVotes;
        
        public VoteStats(int totalVotes, int successfulVotes, int failedVotes) {
            this.totalVotes = totalVotes;
            this.successfulVotes = successfulVotes;
            this.failedVotes = failedVotes;
        }
        
        // Getter方法
        public int getTotalVotes() { return totalVotes; }
        public int getSuccessfulVotes() { return successfulVotes; }
        public int getFailedVotes() { return failedVotes; }
        public double getSuccessRate() { 
            return totalVotes > 0 ? (double) successfulVotes / totalVotes * 100 : 0; 
        }
    }
} 