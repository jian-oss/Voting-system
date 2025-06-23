package p.projectone.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 投票数据模型
 * 展示分布式系统中的数据复制和一致性
 * 
 * @author Distributed Systems Team
 */
@Document(collection = "votes")
public class Vote {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String userId;
    
    @Indexed
    private String candidateId;
    
    private String candidateName;
    
    private LocalDateTime voteTime;
    
    private String serverNode; // 记录处理该投票的服务器节点
    
    private String sessionId; // 用于分布式锁验证
    
    private VoteStatus status;
    
    public enum VoteStatus {
        PENDING,    // 待处理
        PROCESSING, // 处理中
        CONFIRMED,  // 已确认
        REJECTED    // 被拒绝
    }
    
    // 构造函数
    public Vote() {
        this.id = UUID.randomUUID().toString();
        this.voteTime = LocalDateTime.now();
        this.status = VoteStatus.PENDING;
    }
    
    public Vote(String userId, String candidateId, String candidateName) {
        this();
        this.userId = userId;
        this.candidateId = candidateId;
        this.candidateName = candidateName;
    }
    
    // Getter和Setter方法
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getCandidateId() {
        return candidateId;
    }
    
    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }
    
    public String getCandidateName() {
        return candidateName;
    }
    
    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }
    
    public LocalDateTime getVoteTime() {
        return voteTime;
    }
    
    public void setVoteTime(LocalDateTime voteTime) {
        this.voteTime = voteTime;
    }
    
    public String getServerNode() {
        return serverNode;
    }
    
    public void setServerNode(String serverNode) {
        this.serverNode = serverNode;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public VoteStatus getStatus() {
        return status;
    }
    
    public void setStatus(VoteStatus status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "Vote{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", candidateId='" + candidateId + '\'' +
                ", candidateName='" + candidateName + '\'' +
                ", voteTime=" + voteTime +
                ", serverNode='" + serverNode + '\'' +
                ", status=" + status +
                '}';
    }
} 