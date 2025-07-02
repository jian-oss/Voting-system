package p.projectone;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class VoteStressTest {
    // 支持多个服务器节点
    private static final String[] SERVER_NODES = {
        "http://10.72.83.45:8080",
        "http://10.72.83.45:8081"
        // 可添加更多节点
    };
    private static final String[] CANDIDATES = {"1", "2", "3"};
    private static final int USER_COUNT = 100; // 并发用户数
    private static final int THREAD_POOL_SIZE = 20;
    private static final Random random = new Random();
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failCount = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        System.out.println("=== 分布式投票系统压力测试 ===");
        System.out.println("模拟 " + USER_COUNT + " 个用户并发投票...");
        CountDownLatch latch = new CountDownLatch(USER_COUNT);
        long start = System.currentTimeMillis();

        for (int i = 0; i < USER_COUNT; i++) {
            final int userNum = i;
            new Thread(() -> {
                String userId = "stress_user_" + userNum;
                String candidateId = CANDIDATES[random.nextInt(CANDIDATES.length)];
                String candidateName = candidateId.equals("1") ? "Alice" : candidateId.equals("2") ? "Bob" : "Charlie";
                String serverUrl = SERVER_NODES[random.nextInt(SERVER_NODES.length)];
                boolean result = sendVote(serverUrl, userId, candidateId, candidateName);
                if (result) {
                    successCount.incrementAndGet();
                } else {
                    failCount.incrementAndGet();
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("=== 测试完成 ===");
        System.out.println("成功投票: " + successCount.get());
        System.out.println("失败投票: " + failCount.get());
        System.out.println("总耗时: " + (end - start) + " ms");
        System.out.println("成功率: " + (successCount.get() * 100.0 / USER_COUNT) + "%");
    }

    private static boolean sendVote(String serverUrl, String userId, String candidateId, String candidateName) {
        try {
            URL url = new URL(serverUrl + "/api/vote");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            String json = String.format("{\"userId\":\"%s\",\"candidateId\":\"%s\",\"candidateName\":\"%s\"}",
                    userId, candidateId, candidateName);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
            }
            int code = conn.getResponseCode();
            return code == 200;
        } catch (Exception ex) {
            return false;
        }
    }
} 