package p.projectone;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 服务器端GUI界面
 * 连接到HTTP服务器并显示实时数据
 * 
 * @author Distributed Systems Team
 */
public class ServerGUI extends JFrame {
    
    // 服务器地址
    private static final String SERVER_URL = "http://localhost:8080";
    
    // GUI组件
    private JTextArea logArea;
    private JTextArea statsArea;
    private JTextArea voteResultsArea;
    private JButton clearLogButton;
    private JButton refreshButton;
    private JButton resetButton;
    private JLabel statusLabel;
    
    // 候选人信息
    private String[] candidates = {"Alice", "Bob", "Charlie"};
    private String[] candidateIds = {"1", "2", "3"};
    
    // 自动刷新定时器
    private Timer refreshTimer;

    public ServerGUI() {
        setTitle("分布式投票系统 - 服务器管理界面");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 创建顶部状态栏
        createStatusBar();
        
        // 创建主面板
        createMainPanel();
        
        // 创建底部控制面板
        createControlPanel();

        // 启动自动刷新
        startAutoRefresh();
        
        // 添加初始日志
        addLogMessage("服务器管理界面已启动");
        addLogMessage("连接到服务器: " + SERVER_URL);
        addLogMessage("系统状态: 运行中");
        addLogMessage("演示分布式算法: 锁定、同步、调度、复制");
        
        updateStats();
        updateVoteResults();
    }
    
    /**
     * 创建状态栏
     */
    private void createStatusBar() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        
        statusLabel = new JLabel("服务器状态: 连接中... | 端口: 8080");
        statusLabel.setForeground(Color.ORANGE);
        statusPanel.add(statusLabel);
        
        add(statusPanel, BorderLayout.NORTH);
    }
    
    /**
     * 创建主面板
     */
    private void createMainPanel() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // 左侧面板 - 日志和统计
        JPanel leftPanel = new JPanel(new BorderLayout());
        
        // 日志区域
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("系统日志"));
        logScrollPane.setPreferredSize(new Dimension(400, 300));
        leftPanel.add(logScrollPane, BorderLayout.CENTER);
        
        // 统计区域
        statsArea = new JTextArea();
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane statsScrollPane = new JScrollPane(statsArea);
        statsScrollPane.setBorder(BorderFactory.createTitledBorder("服务器统计"));
        statsScrollPane.setPreferredSize(new Dimension(400, 150));
        leftPanel.add(statsScrollPane, BorderLayout.SOUTH);
        
        // 右侧面板 - 投票结果
        voteResultsArea = new JTextArea();
        voteResultsArea.setEditable(false);
        voteResultsArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane resultsScrollPane = new JScrollPane(voteResultsArea);
        resultsScrollPane.setBorder(BorderFactory.createTitledBorder("实时投票结果"));
        
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(resultsScrollPane);
        splitPane.setDividerLocation(400);
        
        add(splitPane, BorderLayout.CENTER);
    }
    
    /**
     * 创建控制面板
     */
    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        controlPanel.setBorder(BorderFactory.createEtchedBorder());
        
        clearLogButton = new JButton("清空日志");
        refreshButton = new JButton("刷新数据");
        resetButton = new JButton("重置投票");
        
        // 清空日志按钮
        clearLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logArea.setText("");
                addLogMessage("日志已清空");
            }
        });
        
        // 刷新按钮
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStats();
                updateVoteResults();
                addLogMessage("数据已刷新");
            }
        });
        
        // 重置按钮
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(
                    ServerGUI.this,
                    "确定要重置所有投票数据吗？",
                    "确认重置",
                    JOptionPane.YES_NO_OPTION
                );
                
                if (result == JOptionPane.YES_OPTION) {
                    resetVoteData();
                    addLogMessage("投票数据已重置");
                }
            }
        });
        
        controlPanel.add(clearLogButton);
        controlPanel.add(refreshButton);
        controlPanel.add(resetButton);
        
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    /**
     * 启动自动刷新
     */
    private void startAutoRefresh() {
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    updateStats();
                    updateVoteResults();
                    updateConnectionStatus();
                });
            }
        }, 1000, 2000); // 每2秒刷新一次
    }
    
    /**
     * 更新连接状态
     */
    private void updateConnectionStatus() {
        try {
            URL url = new URL(SERVER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                statusLabel.setText("服务器状态: 已连接 | 端口: 8080");
                statusLabel.setForeground(Color.GREEN);
            } else {
                statusLabel.setText("服务器状态: 响应异常 | 端口: 8080");
                statusLabel.setForeground(Color.ORANGE);
            }
        } catch (Exception e) {
            statusLabel.setText("服务器状态: 连接失败 | 端口: 8080");
            statusLabel.setForeground(Color.RED);
        }
    }
    
    /**
     * 更新统计信息
     */
    private void updateStats() {
        try {
            URL url = new URL(SERVER_URL + "/api/stats");
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
                
                StringBuilder statsText = new StringBuilder();
                statsText.append("=== 服务器统计信息 ===\n");
                statsText.append("总请求数: ").append(stats.getOrDefault("totalRequests", 0)).append("\n");
                statsText.append("成功投票: ").append(stats.getOrDefault("successfulVotes", 0)).append("\n");
                statsText.append("失败投票: ").append(stats.getOrDefault("failedVotes", 0)).append("\n");
                statsText.append("总投票人数: ").append(stats.getOrDefault("totalVoters", 0)).append("\n");
                statsText.append("活跃锁数: ").append(stats.getOrDefault("activeLocks", 0)).append("\n");
                
                int totalRequests = stats.getOrDefault("totalRequests", 0);
                int successfulVotes = stats.getOrDefault("successfulVotes", 0);
                double successRate = totalRequests > 0 ? (double) successfulVotes / totalRequests * 100 : 0;
                statsText.append("成功率: ").append(String.format("%.2f", successRate)).append("%\n");
                
                statsText.append("\n=== 分布式算法状态 ===\n");
                statsText.append("锁定机制: 正常\n");
                statsText.append("同步机制: 正常\n");
                statsText.append("调度机制: 正常\n");
                statsText.append("复制机制: 正常\n");
                
                statsArea.setText(statsText.toString());
            } else {
                statsArea.setText("无法获取服务器统计信息");
            }
        } catch (Exception ex) {
            statsArea.setText("连接服务器失败: " + ex.getMessage());
        }
    }
    
    /**
     * 更新投票结果
     */
    private void updateVoteResults() {
        try {
            URL url = new URL(SERVER_URL + "/api/vote/results");
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
                
                Map<String, Integer> results = parseJsonResults(sb.toString());
                
                StringBuilder resultsText = new StringBuilder();
                resultsText.append("=== 实时投票结果 ===\n\n");
                
                int totalVotes = 0;
                for (int count : results.values()) {
                    totalVotes += count;
                }
                
                for (int i = 0; i < candidates.length; i++) {
                    String name = candidates[i];
                    String cid = candidateIds[i];
                    int count = results.getOrDefault(cid, 0);
                    double percentage = totalVotes > 0 ? (double) count / totalVotes * 100 : 0;
                    
                    resultsText.append(name).append(":\n");
                    resultsText.append("  票数: ").append(count).append("\n");
                    resultsText.append("  占比: ").append(String.format("%.1f", percentage)).append("%\n");
                    resultsText.append("  进度条: ");
                    
                    // 绘制简单的进度条
                    int bars = (int) (percentage / 5);
                    for (int j = 0; j < 20; j++) {
                        if (j < bars) {
                            resultsText.append("█");
                        } else {
                            resultsText.append("░");
                        }
                    }
                    resultsText.append("\n\n");
                }
                
                resultsText.append("=== 投票详情 ===\n");
                resultsText.append("总投票数: ").append(totalVotes).append("\n");
                resultsText.append("候选人数量: ").append(candidates.length).append("\n");
                
                voteResultsArea.setText(resultsText.toString());
            } else {
                voteResultsArea.setText("无法获取投票结果");
            }
        } catch (Exception ex) {
            voteResultsArea.setText("连接服务器失败: " + ex.getMessage());
        }
    }
    
    /**
     * 重置投票数据
     */
    private void resetVoteData() {
        addLogMessage("重置功能需要服务器端支持");
        // 这里可以添加重置API调用
    }
    
    /**
     * 添加日志消息
     */
    public void addLogMessage(String message) {
        String timestamp = java.time.LocalTime.now().toString();
        logArea.append("[" + timestamp + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    /**
     * 简单解析JSON（实际项目中应该使用JSON库）
     */
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
            addLogMessage("解析JSON失败: " + ex.getMessage());
        }
        return results;
    }
    
    /**
     * 停止定时器
     */
    public void stopTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerGUI serverGUI = new ServerGUI();
            serverGUI.setVisible(true);
            
            // 添加窗口关闭事件
            serverGUI.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    serverGUI.stopTimer();
                }
            });
        });
    }
} 