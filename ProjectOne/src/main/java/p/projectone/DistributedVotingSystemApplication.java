package p.projectone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 分布式在线投票系统主应用程序
 * 展示分布式算法的核心概念：锁定、同步和并发、调度、复制
 * 
 * @author Distributed Systems Team
 * @version 1.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class DistributedVotingSystemApplication {

    public static void main(String[] args) {
        System.out.println("=== 分布式在线投票系统启动中 ===");
        System.out.println("系统将展示以下分布式算法概念：");
        System.out.println("1. 锁定 (Locking) - 防止重复投票");
        System.out.println("2. 同步和并发 (Synchronization & Concurrency) - 处理并发投票请求");
        System.out.println("3. 调度 (Scheduling) - 投票请求排队处理");
        System.out.println("4. 复制 (Replication) - 数据多副本存储");
        System.out.println("=====================================");
        
        SpringApplication.run(DistributedVotingSystemApplication.class, args);
        
        System.out.println("=== 系统启动完成 ===");
        System.out.println("访问地址: http://10.72.83.45:8080");
        System.out.println("WebSocket地址: ws://10.72.83.45:8080/ws");
    }
} 