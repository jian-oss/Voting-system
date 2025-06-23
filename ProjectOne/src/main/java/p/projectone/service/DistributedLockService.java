package p.projectone.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 分布式锁服务
 * 实现分布式算法中的锁定机制，防止重复投票
 * 
 * 复杂度：O(1) - 常量时间复杂度
 * 实现方式：Redis分布式锁 + 本地锁
 * 
 * @author Distributed Systems Team
 */
@Service
public class DistributedLockService {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    // 本地锁，用于单机并发控制
    private final Lock localLock = new ReentrantLock();
    
    private static final String LOCK_PREFIX = "vote_lock:";
    private static final String SESSION_PREFIX = "vote_session:";
    private static final long LOCK_TIMEOUT = 10; // 锁超时时间（秒）
    private static final long SESSION_TIMEOUT = 300; // 会话超时时间（秒）
    
    /**
     * 尝试获取分布式锁
     * 实现分布式算法中的锁定机制
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 是否成功获取锁
     */
    public boolean tryLock(String userId, String sessionId) {
        String lockKey = LOCK_PREFIX + userId;
        String sessionKey = SESSION_PREFIX + userId;
        
        try {
            // 使用本地锁确保原子性
            if (localLock.tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    // 检查Redis分布式锁
                    Boolean lockAcquired = redisTemplate.opsForValue()
                            .setIfAbsent(lockKey, sessionId, LOCK_TIMEOUT, TimeUnit.SECONDS);
                    
                    if (Boolean.TRUE.equals(lockAcquired)) {
                        // 设置会话信息
                        redisTemplate.opsForValue().set(sessionKey, sessionId, SESSION_TIMEOUT, TimeUnit.SECONDS);
                        System.out.println("分布式锁获取成功 - 用户: " + userId + ", 会话: " + sessionId);
                        return true;
                    }
                    
                    System.out.println("分布式锁获取失败 - 用户: " + userId + " 已被锁定");
                    return false;
                } finally {
                    localLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("获取本地锁时被中断: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 释放分布式锁
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @return 是否成功释放锁
     */
    public boolean releaseLock(String userId, String sessionId) {
        String lockKey = LOCK_PREFIX + userId;
        String sessionKey = SESSION_PREFIX + userId;
        
        try {
            // 验证锁的所有者
            String currentSessionId = redisTemplate.opsForValue().get(lockKey);
            if (sessionId.equals(currentSessionId)) {
                // 删除锁和会话
                redisTemplate.delete(lockKey);
                redisTemplate.delete(sessionKey);
                System.out.println("分布式锁释放成功 - 用户: " + userId + ", 会话: " + sessionId);
                return true;
            }
            
            System.out.println("分布式锁释放失败 - 会话不匹配 - 用户: " + userId);
            return false;
        } catch (Exception e) {
            System.err.println("释放分布式锁时发生错误: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 检查用户是否已被锁定
     * 
     * @param userId 用户ID
     * @return 是否被锁定
     */
    public boolean isLocked(String userId) {
        String lockKey = LOCK_PREFIX + userId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(lockKey));
    }
    
    /**
     * 获取锁的剩余时间
     * 
     * @param userId 用户ID
     * @return 剩余时间（秒）
     */
    public long getLockRemainingTime(String userId) {
        String lockKey = LOCK_PREFIX + userId;
        Long ttl = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0;
    }
    
    /**
     * 强制释放锁（管理员功能）
     * 
     * @param userId 用户ID
     * @return 是否成功释放
     */
    public boolean forceReleaseLock(String userId) {
        String lockKey = LOCK_PREFIX + userId;
        String sessionKey = SESSION_PREFIX + userId;
        
        try {
            redisTemplate.delete(lockKey);
            redisTemplate.delete(sessionKey);
            System.out.println("强制释放分布式锁 - 用户: " + userId);
            return true;
        } catch (Exception e) {
            System.err.println("强制释放分布式锁时发生错误: " + e.getMessage());
            return false;
        }
    }
} 