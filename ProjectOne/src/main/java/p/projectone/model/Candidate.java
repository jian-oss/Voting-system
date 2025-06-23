package p.projectone.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 候选人数据模型
 * 
 * @author Distributed Systems Team
 */
@Document(collection = "candidates")
public class Candidate {
    
    @Id
    private String id;
    
    private String name;
    
    private String description;
    
    private String party;
    
    private LocalDateTime createdAt;
    
    private boolean active;
    
    // 构造函数
    public Candidate() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.active = true;
    }
    
    public Candidate(String name, String description, String party) {
        this();
        this.name = name;
        this.description = description;
        this.party = party;
    }
    
    // Getter和Setter方法
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getParty() {
        return party;
    }
    
    public void setParty(String party) {
        this.party = party;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    @Override
    public String toString() {
        return "Candidate{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", party='" + party + '\'' +
                ", active=" + active +
                '}';
    }
} 