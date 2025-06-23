package p.projectone.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 复制服务
 * 实现分布式算法中的复制机制，确保数据一致性
 * 
 * 复杂度：O(n) - n为副本数量
 * 实现方式：异步复制 + 一致性检查 + 故障恢复
 * 
 * @author Distributed Systems Team
 */
@Service
public class ReplicationService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Value("${distributed.replication.replica-count:3}")
    private int replicaCount;
    
    @Value("${distributed.replication.sync-timeout:5000}")
    private long syncTimeout;
    
    // 复制状态跟踪
    private final ConcurrentHashMap<String, ReplicationStatus> replicationStatus = new ConcurrentHashMap<>();
    private final AtomicInteger successfulReplications = new AtomicInteger(0);
    private final AtomicInteger failedReplications = new AtomicInteger(0);
    
    /**
     * 复制投票数据到多个副本
     * 实现分布式算法中的复制机制
     * 
     * @param voteId 投票ID
     * @param voteData 投票数据
     * @return 是否复制成功
     */
    public boolean replicateVoteData(String voteId, String voteData) {
        System.out.println("开始复制投票数据 - 投票ID: " + voteId + ", 副本数: " + replicaCount);
        
        ReplicationStatus status = new ReplicationStatus(voteId, replicaCount);
        replicationStatus.put(voteId, status);
        
        try {
            // 异步复制到多个副本
            List<CompletableFuture<Boolean>> replicationFutures = new java.util.ArrayList<>();
            
            for (int i = 0; i < replicaCount; i++) {
                final int replicaIndex = i;
                CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                    return replicateToReplica(voteId, voteData, replicaIndex);
                });
                replicationFutures.add(future);
            }
            
            // 等待所有副本复制完成
            CompletableFuture<Void> allReplications = CompletableFuture.allOf(
                replicationFutures.toArray(new CompletableFuture[0])
            );
            
            // 设置超时
            allReplications.get(syncTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
            
            // 检查复制结果
            int successCount = 0;
            for (CompletableFuture<Boolean> future : replicationFutures) {
                if (future.get()) {
                    successCount++;
                }
            }
            
            boolean replicationSuccess = successCount >= (replicaCount / 2 + 1); // 多数派原则
            
            if (replicationSuccess) {
                successfulReplications.incrementAndGet();
                status.setStatus(ReplicationStatus.Status.COMPLETED);
                System.out.println("投票数据复制成功 - 投票ID: " + voteId + 
                                 ", 成功副本数: " + successCount + "/" + replicaCount);
            } else {
                failedReplications.incrementAndGet();
                status.setStatus(ReplicationStatus.Status.FAILED);
                System.out.println("投票数据复制失败 - 投票ID: " + voteId + 
                                 ", 成功副本数: " + successCount + "/" + replicaCount);
            }
            
            return replicationSuccess;
            
        } catch (Exception e) {
            failedReplications.incrementAndGet();
            status.setStatus(ReplicationStatus.Status.FAILED);
            System.err.println("复制投票数据时发生异常 - 投票ID: " + voteId + ", 错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 复制数据到单个副本
     * 
     * @param voteId 投票ID
     * @param voteData 投票数据
     * @param replicaIndex 副本索引
     * @return 是否复制成功
     */
    private boolean replicateToReplica(String voteId, String voteData, int replicaIndex) {
        try {
            // 模拟复制到不同副本的延迟
            Thread.sleep((long) (Math.random() * 1000));
            
            // 在实际系统中，这里会复制到不同的数据库实例或服务器
            // 这里简化为在同一个MongoDB中创建不同的集合来模拟副本
            String replicaCollectionName = "votes_replica_" + replicaIndex;
            
            // 保存到副本集合
            ReplicaVote replicaVote = new ReplicaVote(voteId, voteData, replicaIndex);
            mongoTemplate.save(replicaVote, replicaCollectionName);
            
            System.out.println("数据复制到副本 " + replicaIndex + " 成功 - 投票ID: " + voteId);
            return true;
            
        } catch (Exception e) {
            System.err.println("复制到副本 " + replicaIndex + " 失败 - 投票ID: " + voteId + 
                             ", 错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查数据一致性
     * 
     * @param voteId 投票ID
     * @return 一致性检查结果
     */
    public ConsistencyCheckResult checkConsistency(String voteId) {
        System.out.println("开始检查数据一致性 - 投票ID: " + voteId);
        
        try {
            // 从主数据库获取数据
            String mainData = getVoteDataFromMain(voteId);
            
            // 从各个副本获取数据
            List<String> replicaData = new java.util.ArrayList<>();
            for (int i = 0; i < replicaCount; i++) {
                String data = getVoteDataFromReplica(voteId, i);
                if (data != null) {
                    replicaData.add(data);
                }
            }
            
            // 检查一致性
            boolean isConsistent = true;
            int consistentReplicas = 0;
            
            for (String replicaDataItem : replicaData) {
                if (mainData.equals(replicaDataItem)) {
                    consistentReplicas++;
                } else {
                    isConsistent = false;
                }
            }
            
            ConsistencyCheckResult result = new ConsistencyCheckResult(
                voteId, isConsistent, consistentReplicas, replicaData.size()
            );
            
            System.out.println("一致性检查完成 - 投票ID: " + voteId + 
                             ", 一致性: " + isConsistent + 
                             ", 一致副本数: " + consistentReplicas + "/" + replicaData.size());
            
            return result;
            
        } catch (Exception e) {
            System.err.println("一致性检查失败 - 投票ID: " + voteId + ", 错误: " + e.getMessage());
            return new ConsistencyCheckResult(voteId, false, 0, 0);
        }
    }
    
    /**
     * 从主数据库获取投票数据
     */
    private String getVoteDataFromMain(String voteId) {
        // 简化实现，返回模拟数据
        return "vote_data_" + voteId;
    }
    
    /**
     * 从副本获取投票数据
     */
    private String getVoteDataFromReplica(String voteId, int replicaIndex) {
        try {
            String replicaCollectionName = "votes_replica_" + replicaIndex;
            // 这里应该查询副本集合
            // 简化实现，返回模拟数据
            return "vote_data_" + voteId + "_replica_" + replicaIndex;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取复制统计信息
     */
    public ReplicationStats getReplicationStats() {
        return new ReplicationStats(
            successfulReplications.get(),
            failedReplications.get(),
            replicationStatus.size()
        );
    }
    
    /**
     * 复制状态内部类
     */
    public static class ReplicationStatus {
        private final String voteId;
        private final int totalReplicas;
        private Status status;
        private final long startTime;
        
        public enum Status {
            PENDING, COMPLETED, FAILED
        }
        
        public ReplicationStatus(String voteId, int totalReplicas) {
            this.voteId = voteId;
            this.totalReplicas = totalReplicas;
            this.status = Status.PENDING;
            this.startTime = System.currentTimeMillis();
        }
        
        // Getter和Setter方法
        public String getVoteId() { return voteId; }
        public int getTotalReplicas() { return totalReplicas; }
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
        public long getStartTime() { return startTime; }
    }
    
    /**
     * 一致性检查结果
     */
    public static class ConsistencyCheckResult {
        private final String voteId;
        private final boolean isConsistent;
        private final int consistentReplicas;
        private final int totalReplicas;
        
        public ConsistencyCheckResult(String voteId, boolean isConsistent, 
                                    int consistentReplicas, int totalReplicas) {
            this.voteId = voteId;
            this.isConsistent = isConsistent;
            this.consistentReplicas = consistentReplicas;
            this.totalReplicas = totalReplicas;
        }
        
        // Getter方法
        public String getVoteId() { return voteId; }
        public boolean isConsistent() { return isConsistent; }
        public int getConsistentReplicas() { return consistentReplicas; }
        public int getTotalReplicas() { return totalReplicas; }
        public double getConsistencyRate() { 
            return totalReplicas > 0 ? (double) consistentReplicas / totalReplicas * 100 : 0; 
        }
    }
    
    /**
     * 复制统计信息
     */
    public static class ReplicationStats {
        private final int successfulReplications;
        private final int failedReplications;
        private final int activeReplications;
        
        public ReplicationStats(int successfulReplications, int failedReplications, int activeReplications) {
            this.successfulReplications = successfulReplications;
            this.failedReplications = failedReplications;
            this.activeReplications = activeReplications;
        }
        
        // Getter方法
        public int getSuccessfulReplications() { return successfulReplications; }
        public int getFailedReplications() { return failedReplications; }
        public int getActiveReplications() { return activeReplications; }
        public int getTotalReplications() { return successfulReplications + failedReplications; }
        public double getSuccessRate() { 
            int total = getTotalReplications();
            return total > 0 ? (double) successfulReplications / total * 100 : 0; 
        }
    }
    
    /**
     * 副本投票数据模型
     */
    public static class ReplicaVote {
        private String id;
        private String voteId;
        private String voteData;
        private int replicaIndex;
        private long timestamp;
        
        public ReplicaVote(String voteId, String voteData, int replicaIndex) {
            this.id = java.util.UUID.randomUUID().toString();
            this.voteId = voteId;
            this.voteData = voteData;
            this.replicaIndex = replicaIndex;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getter和Setter方法
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getVoteId() { return voteId; }
        public void setVoteId(String voteId) { this.voteId = voteId; }
        public String getVoteData() { return voteData; }
        public void setVoteData(String voteData) { this.voteData = voteData; }
        public int getReplicaIndex() { return replicaIndex; }
        public void setReplicaIndex(int replicaIndex) { this.replicaIndex = replicaIndex; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
} 