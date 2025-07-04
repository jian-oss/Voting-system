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
 * Simplified voting client
 * Connects to the server API to vote
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

    // Candidate list
    private String[] candidates = {"Alice", "Bob", "Charlie"};
    private String[] candidateIds = {"1", "2", "3"};
    
    // Support multiple server nodes
    private static final String[] SERVER_NODES = {
        "http://10.72.214.22:8080",
        "http://10.72.214.22:8081"
        
        // More nodes can be added, e.g. "http://192.168.1.101:8080"
    };
    private static final String LOAD_BALANCE_MODE = "roundrobin"; // Can be changed to "random"
    private static final AtomicInteger nodeIndex = new AtomicInteger(0);
    private static final Random random = new Random();

    // Get the current server node to use
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
        setTitle("Distributed Voting System Client - Server Connected");
        setSize(500, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Create input panel
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Voting Information"));
        
        inputPanel.add(new JLabel("User ID:"));
        userIdField = new JTextField();
        inputPanel.add(userIdField);
        
        inputPanel.add(new JLabel("Candidate:"));
        candidateBox = new JComboBox<>(candidates);
        inputPanel.add(candidateBox);
        
        voteButton = new JButton("Vote");
        inputPanel.add(voteButton);
        
        refreshButton = new JButton("Refresh Results");
        inputPanel.add(refreshButton);

        add(inputPanel, BorderLayout.NORTH);

        // Create result display panel
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setBorder(BorderFactory.createTitledBorder("Voting Results"));
        resultPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        
        // Create log panel
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setBorder(BorderFactory.createTitledBorder("System Log"));
        logArea.setPreferredSize(new Dimension(400, 150));
        resultPanel.add(new JScrollPane(logArea), BorderLayout.SOUTH);

        add(resultPanel, BorderLayout.CENTER);

        // Vote button event
        voteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userId = userIdField.getText().trim();
                int idx = candidateBox.getSelectedIndex();
                String candidateId = candidateIds[idx];
                String candidateName = candidates[idx];
                
                if (userId.isEmpty()) {
                    JOptionPane.showMessageDialog(SimpleVoteClient.this, "Please enter User ID!");
                    return;
                }
                
                logMessage("Voting started - User: " + userId + " voted for " + candidateName);
                
                // Send vote request to server
                boolean success = sendVoteToServer(userId, candidateId, candidateName);
                
                if (success) {
                    logMessage("Vote successful!");
                    JOptionPane.showMessageDialog(SimpleVoteClient.this, "Vote successful!");
                    refreshResultsFromServer();
                } else {
                    logMessage("Vote failed!");
                    JOptionPane.showMessageDialog(SimpleVoteClient.this, "Vote failed or already voted!");
                }
            }
        });

        // Refresh button event
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshResultsFromServer();
                logMessage("Refreshed voting results");
            }
        });

        // Show initial results on startup
        refreshResultsFromServer();
        logMessage("Distributed Voting System Client started");
        logMessage("Connected to server: " + getServerUrl());
        logMessage("Demonstrating distributed algorithms: Locking, Synchronization, Scheduling, Replication");
    }

    // Send vote request to server
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
            
            logMessage("Sending request to node: " + serverUrl + " Content: " + json);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes());
            }
            
            int responseCode = conn.getResponseCode();
            logMessage("Server response code: " + responseCode);
            
            if (responseCode == 200) {
                return true;
            } else {
                // Read error message
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    logMessage("Error message: " + response.toString());
                }
                // Failover: try next node
                for (int i = 0; i < SERVER_NODES.length - 1; i++) {
                    String nextUrl = getServerUrl();
                    if (!nextUrl.equals(serverUrl)) {
                        logMessage("Trying to switch to next node: " + nextUrl);
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
                                logMessage("Vote successful after switching node: " + nextUrl);
                                return true;
                            }
                        } catch (Exception ignore) {}
                    }
                }
                return false;
            }
            
        } catch (Exception ex) {
            logMessage("Failed to connect to server: " + ex.getMessage());
            return false;
        }
    }

    // Get voting results from server
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
                
                // Parse JSON results
                String jsonResponse = sb.toString();
                logMessage("Fetched results: " + jsonResponse);
                
                // Simple JSON parsing (should use a JSON library in real projects)
                Map<String, Integer> results = parseJsonResults(jsonResponse);
                
                StringBuilder resultText = new StringBuilder();
                resultText.append("=== Voting Results ===\n");
                for (int i = 0; i < candidates.length; i++) {
                    String name = candidates[i];
                    String cid = candidateIds[i];
                    int count = results.getOrDefault(cid, 0);
                    resultText.append(name).append(": ").append(count).append(" votes\n");
                }
                
                // Get statistics
                getServerStats(resultText);
                
                resultArea.setText(resultText.toString());
                
            } else {
                resultArea.setText("Failed to get voting results - Response code: " + responseCode);
                logMessage("Failed to fetch results - Response code: " + responseCode);
            }
            
        } catch (Exception ex) {
            resultArea.setText("Failed to connect to server\n" + ex.getMessage());
            logMessage("Failed to connect to server: " + ex.getMessage());
        }
    }
    
    // Get server statistics
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
                
                resultText.append("\n=== Server Statistics ===\n");
                resultText.append("Total Requests: ").append(stats.getOrDefault("totalRequests", 0)).append("\n");
                resultText.append("Successful Votes: ").append(stats.getOrDefault("successfulVotes", 0)).append("\n");
                resultText.append("Failed Votes: ").append(stats.getOrDefault("failedVotes", 0)).append("\n");
                resultText.append("Total Voters: ").append(stats.getOrDefault("totalVoters", 0)).append("\n");
                resultText.append("Active Locks: ").append(stats.getOrDefault("activeLocks", 0)).append("\n");
            }
        } catch (Exception ex) {
            logMessage("Failed to fetch statistics: " + ex.getMessage());
        }
    }
    
    // Simple JSON parsing (should use a JSON library in real projects)
    private Map<String, Integer> parseJsonResults(String json) {
        Map<String, Integer> results = new HashMap<>();
        try {
            // Remove curly braces
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
            logMessage("Failed to parse JSON: " + ex.getMessage());
        }
        return results;
    }

    // Add log message
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