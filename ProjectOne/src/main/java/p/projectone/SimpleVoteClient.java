package p.projectone;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 简化的投票客户端
 * 连接到服务器端API进行投票
 * 
 * @author Distributed Systems Team
 */
public class SimpleVoteClient extends JFrame {
    private JTextField userIdField;
    private JComboBox<String> candidateBox;
    private JButton voteButton;
    private JButton refreshButton;
    private JTextArea resultArea;
    private JTextArea logArea;

    // 候选人列表
    private String[] candidates = {"Alice", "Bob", "Charlie"};
    private String[] candidateIds = {"1", "2", "3"};
    
    // 支持多个服务器节点
    private static final String[] SERVER_NODES = {
        "http://localhost:8080",
        "http://localhost:8081",
        // 可以添加更多节点，如 "http://192.168.1.101:8080"
    };
    // 负载均衡模式："roundrobin" 或 "random"
    private static final String LOAD_BALANCE_MODE = "roundrobin"; // 可改为"random"
    private static final AtomicInteger nodeIndex = new AtomicInteger(0);
    private static final Random random = new Random();

    // 获取当前要用的服务器节点
    private String getServerUrl() {
        if (LOAD_BALANCE_MODE.equals("random")) {
            int idx = random.nextInt(SERVER_NODES.length);
            return SERVER_NODES[idx];
        } else { // roundrobin
            int idx = nodeIndex.getAndIncrement() % SERVER_NODES.length;
            return SERVER_NODES[idx];
        }
    }

    public SimpleVoteClient() {
        setTitle("分布式投票系统客户端 - 连接服务器版");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 创建输入面板
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("投票信息"));
        
        inputPanel.add(new JLabel("用户ID:"));
        userIdField = new JTextField();
        inputPanel.add(userIdField);
        
        inputPanel.add(new JLabel("候选人:"));
        candidateBox = new JComboBox<>(candidates);
        inputPanel.add(candidateBox);
        
        voteButton = new JButton("投票");
        inputPanel.add(voteButton);
        
        refreshButton = new JButton("刷新结果");
        inputPanel.add(refreshButton);

        add(inputPanel, BorderLayout.NORTH);

        // 创建结果显示面板
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setBorder(BorderFactory.createTitledBorder("投票结果"));
        resultPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        
        // 创建日志面板
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setBorder(BorderFactory.createTitledBorder("系统日志"));
        logArea.setPreferredSize(new Dimension(400, 150));
        resultPanel.add(new JScrollPane(logArea), BorderLayout.SOUTH);

        add(resultPanel, BorderLayout.CENTER);

        // 投票按钮事件
        voteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userId = userIdField.getText().trim();
                int idx = candidateBox.getSelectedIndex();
                String candidateId = candidateIds[idx];
                String candidateName = candidates[idx];
                
                if (userId.isEmpty()) {
                    JOptionPane.showMessageDialog(SimpleVoteClient.this, "请输入用户ID！");
                    return;
                }
                
                logMessage("开始投票 - 用户: " + userId + " 投给 " + candidateName);
                
                // 发送投票请求到服务器
                boolean success = sendVoteToServer(userId, candidateId, candidateName);
                
                if (success) {
                    logMessage("投票成功！");
                    JOptionPane.showMessageDialog(SimpleVoteClient.this, "投票成功！");
                    refreshResultsFromServer();
                } else {
                    logMessage("投票失败！");
                    JOptionPane.showMessageDialog(SimpleVoteClient.this, "投票失败或已投票！");
                }
            }
        });

        // 刷新按钮事件
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshResultsFromServer();
                logMessage("刷新投票结果");
            }
        });

        // 启动时显示初始结果
        refreshResultsFromServer();
        logMessage("分布式投票系统客户端已启动");
        logMessage("连接到服务器: " + getServerUrl());
        logMessage("演示分布式算法：锁定、同步、调度、复制");
    }

    // 发送投票请求到服务器
    private boolean sendVoteToServer(String userId, String candidateId, String candidateName) {
        try {
            String serverUrl = getServerUrl();
            URL url = new URL(serverUrl + "/api/vote");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            String json = String.format("{\"userId\":\"%s\",\"candidateId\":\"%s\",\"candidateName\":\"%s\"}",
                    userId, candidateId, candidateName);
            
            logMessage("发送请求到节点: " + serverUrl + " 内容: " + json);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
            }
            
            int responseCode = conn.getResponseCode();
            logMessage("服务器响应码: " + responseCode);
            
            if (responseCode == 200) {
                return true;
            } else {
                // 读取错误信息
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    logMessage("错误信息: " + response.toString());
                }
                // 故障转移：尝试下一个节点
                for (int i = 0; i < SERVER_NODES.length - 1; i++) {
                    String nextUrl = getServerUrl();
                    if (!nextUrl.equals(serverUrl)) {
                        logMessage("尝试切换到下一个节点: " + nextUrl);
                        try {
                            URL retryUrl = new URL(nextUrl + "/api/vote");
                            HttpURLConnection retryConn = (HttpURLConnection) retryUrl.openConnection();
                            retryConn.setRequestMethod("POST");
                            retryConn.setRequestProperty("Content-Type", "application/json");
                            retryConn.setDoOutput(true);
                            try (OutputStream os = retryConn.getOutputStream()) {
                                os.write(json.getBytes());
                            }
                            int retryCode = retryConn.getResponseCode();
                            if (retryCode == 200) {
                                logMessage("切换节点投票成功: " + nextUrl);
                                return true;
                            }
                        } catch (Exception ignore) {}
                    }
                }
                return false;
            }
            
        } catch (Exception ex) {
            logMessage("连接服务器失败: " + ex.getMessage());
            return false;
        }
    }

    // 从服务器获取投票结果
    private void refreshResultsFromServer() {
        try {
            URL url = new URL(getServerUrl() + "/api/vote/results");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                
                // 解析JSON结果
                String jsonResponse = sb.toString();
                logMessage("获取结果: " + jsonResponse);
                
                // 简单解析JSON（实际项目中应该使用JSON库）
                Map<String, Integer> results = parseJsonResults(jsonResponse);
                
                StringBuilder resultText = new StringBuilder();
                resultText.append("=== 投票结果 ===\n");
                for (int i = 0; i < candidates.length; i++) {
                    String name = candidates[i];
                    String cid = candidateIds[i];
                    int count = results.getOrDefault(cid, 0);
                    resultText.append(name).append(": ").append(count).append(" 票\n");
                }
                
                // 获取统计信息
                getServerStats(resultText);
                
                resultArea.setText(resultText.toString());
                
            } else {
                resultArea.setText("无法获取投票结果 - 响应码: " + responseCode);
                logMessage("获取结果失败 - 响应码: " + responseCode);
            }
            
        } catch (Exception ex) {
            resultArea.setText("无法连接到服务器\n" + ex.getMessage());
            logMessage("连接服务器失败: " + ex.getMessage());
        }
    }
    
    // 获取服务器统计信息
    private void getServerStats(StringBuilder resultText) {
        try {
            URL url = new URL(getServerUrl() + "/api/stats");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                
                Map<String, Integer> stats = parseJsonResults(sb.toString());
                
                resultText.append("\n=== 服务器统计 ===\n");
                resultText.append("总请求数: ").append(stats.getOrDefault("totalRequests", 0)).append("\n");
                resultText.append("成功投票: ").append(stats.getOrDefault("successfulVotes", 0)).append("\n");
                resultText.append("失败投票: ").append(stats.getOrDefault("failedVotes", 0)).append("\n");
                resultText.append("总投票人数: ").append(stats.getOrDefault("totalVoters", 0)).append("\n");
                resultText.append("活跃锁数: ").append(stats.getOrDefault("activeLocks", 0)).append("\n");
            }
        } catch (Exception ex) {
            logMessage("获取统计信息失败: " + ex.getMessage());
        }
    }
    
    // 简单解析JSON（实际项目中应该使用JSON库）
    private Map<String, Integer> parseJsonResults(String json) {
        Map<String, Integer> results = new HashMap<>();
        try {
            // 移除花括号
            json = json.substring(1, json.length() - 1);
            String[] pairs = json.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].replace("\"", "").trim();
                    int value = Integer.parseInt(keyValue[1].trim());
                    results.put(key, value);
                }
            }
        } catch (Exception ex) {
            logMessage("解析JSON失败: " + ex.getMessage());
        }
        return results;
    }

    // 添加日志消息
    private void logMessage(String message) {
        String timestamp = java.time.LocalTime.now().toString();
        logArea.append("[" + timestamp + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SimpleVoteClient().setVisible(true);
        });
    }
} 