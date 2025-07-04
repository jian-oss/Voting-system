package p.projectone;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class VoteStressTest {
    // Support multiple server nodes
    private static final String[] SERVER_NODES = {
        "http://10.72.214.22:8080",
        "http://10.72.214.22:8081"
        // More nodes can be added
    };
    private static final String[] CANDIDATES = {"1", "2", "3"};
    private static final int USER_COUNT = 100; // Number of concurrent users
    private static final int THREAD_POOL_SIZE = 20;
    private static final Random random = new Random();
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failCount = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        System.out.println("=== Distributed Voting System Stress Test ===");
        System.out.println("Simulating concurrent voting for " + USER_COUNT + " users...");
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
        System.out.println("=== Test Completed ===");
        System.out.println("Successful votes: " + successCount.get());
        System.out.println("Failed votes: " + failCount.get());
        System.out.println("Total time: " + (end - start) + " ms");
        System.out.println("Success rate: " + (successCount.get() * 100.0 / USER_COUNT) + "%");
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