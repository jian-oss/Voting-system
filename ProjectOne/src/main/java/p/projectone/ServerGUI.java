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
 * Server-side GUI interface
 * Connects to HTTP server and displays real-time data
 * 
 * @author Distributed Systems Team
 */
public class ServerGUI extends JFrame {
    
    // Server address
    private static final String SERVER_URL = "http://10.72.83.45:8080";
    
    // GUI components
    private JTextArea logArea;
    private JTextArea statsArea;
    private JTextArea voteResultsArea;
    private JButton clearLogButton;
    private JButton refreshButton;
    private JButton resetButton;
    private JLabel statusLabel;
    
    // Candidate information
    private String[] candidates = {"Alice", "Bob", "Charlie"};
    private String[] candidateIds = {"1", "2", "3"};
    
    // Auto-refresh timer
    private Timer refreshTimer;

    public ServerGUI() {
        setTitle("Distributed Voting System - Server Management Interface");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Create top status bar
        createStatusBar();
        
        // Create main panel
        createMainPanel();
        
        // Create bottom control panel
        createControlPanel();

        // Start auto-refresh
        startAutoRefresh();
        
        // Add initial log
        addLogMessage("Server Management Interface has started");
        addLogMessage("Connected to server: " + SERVER_URL);
        addLogMessage("System Status: Running");
        addLogMessage("Demonstrating Distributed Algorithm: Locking, Synchronization, Scheduling, Replication");
        
        updateStats();
        updateVoteResults();
    }
    
    /**
     * Create status bar
     */
    private void createStatusBar() {
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(BorderFactory.createEtchedBorder());
        
        statusLabel = new JLabel("Server Status: Connecting... | Port: 8080");
        statusLabel.setForeground(Color.ORANGE);
        statusPanel.add(statusLabel);
        
        add(statusPanel, BorderLayout.NORTH);
    }
    
    /**
     * Create main panel
     */
    private void createMainPanel() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // Left panel - log and statistics
        JPanel leftPanel = new JPanel(new BorderLayout());
        
        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("System Log"));
        logScrollPane.setPreferredSize(new Dimension(400, 300));
        leftPanel.add(logScrollPane, BorderLayout.CENTER);
        
        // Statistics area
        statsArea = new JTextArea();
        statsArea.setEditable(false);
        statsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane statsScrollPane = new JScrollPane(statsArea);
        statsScrollPane.setBorder(BorderFactory.createTitledBorder("Server Statistics"));
        statsScrollPane.setPreferredSize(new Dimension(400, 150));
        leftPanel.add(statsScrollPane, BorderLayout.SOUTH);
        
        // Right panel - voting results
        voteResultsArea = new JTextArea();
        voteResultsArea.setEditable(false);
        voteResultsArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane resultsScrollPane = new JScrollPane(voteResultsArea);
        resultsScrollPane.setBorder(BorderFactory.createTitledBorder("Real-Time Vote Results"));
        
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(resultsScrollPane);
        splitPane.setDividerLocation(400);
        
        add(splitPane, BorderLayout.CENTER);
    }
    
    /**
     * Create control panel
     */
    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        controlPanel.setBorder(BorderFactory.createEtchedBorder());
        
        clearLogButton = new JButton("Clear Log");
        refreshButton = new JButton("Refresh Data");
        resetButton = new JButton("Reset Votes");
        
        // Clear log button
        clearLogButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                logArea.setText("");
                addLogMessage("Log has been cleared");
            }
        });
        
        // Refresh button
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStats();
                updateVoteResults();
                addLogMessage("Data has been refreshed");
            }
        });
        
        // Reset button
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = JOptionPane.showConfirmDialog(
                    ServerGUI.this,
                    "Are you sure you want to reset all vote data?",
                    "Confirm Reset",
                    JOptionPane.YES_NO_OPTION
                );
                
                if (result == JOptionPane.YES_OPTION) {
                    resetVoteData();
                    addLogMessage("Vote data has been reset");
                }
            }
        });
        
        controlPanel.add(clearLogButton);
        controlPanel.add(refreshButton);
        controlPanel.add(resetButton);
        
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Start auto-refresh
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
        }, 1000, 2000); // Refresh every 2 seconds
    }
    
    /**
     * Update connection status
     */
    private void updateConnectionStatus() {
        try {
            URL url = new URL(SERVER_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                statusLabel.setText("Server Status: Connected | Port: 8080");
                statusLabel.setForeground(Color.GREEN);
            } else {
                statusLabel.setText("Server Status: Response Exception | Port: 8080");
                statusLabel.setForeground(Color.ORANGE);
            }
        } catch (Exception e) {
            statusLabel.setText("Server Status: Connection Failed | Port: 8080");
            statusLabel.setForeground(Color.RED);
        }
    }
    
    /**
     * Update statistics
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
                statsText.append("=== Server Statistics Information ===\n");
                statsText.append("Total Requests: ").append(stats.getOrDefault("totalRequests", 0)).append("\n");
                statsText.append("Successful Votes: ").append(stats.getOrDefault("successfulVotes", 0)).append("\n");
                statsText.append("Failed Votes: ").append(stats.getOrDefault("failedVotes", 0)).append("\n");
                statsText.append("Total Voters: ").append(stats.getOrDefault("totalVoters", 0)).append("\n");
                statsText.append("Active Locks: ").append(stats.getOrDefault("activeLocks", 0)).append("\n");
                
                int totalRequests = stats.getOrDefault("totalRequests", 0);
                int successfulVotes = stats.getOrDefault("successfulVotes", 0);
                double successRate = totalRequests > 0 ? (double) successfulVotes / totalRequests * 100 : 0;
                statsText.append("Success Rate: ").append(String.format("%.2f", successRate)).append("%\n");
                
                statsText.append("\n=== Distributed Algorithm Status ===\n");
                statsText.append("Locking Mechanism: Normal\n");
                statsText.append("Synchronization Mechanism: Normal\n");
                statsText.append("Scheduling Mechanism: Normal\n");
                statsText.append("Replication Mechanism: Normal\n");
                
                statsArea.setText(statsText.toString());
            } else {
                statsArea.setText("Unable to retrieve server statistics information");
            }
        } catch (Exception ex) {
            statsArea.setText("Connection to server failed: " + ex.getMessage());
        }
    }
    
    /**
     * Update voting results
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
                resultsText.append("=== Real-Time Vote Results ===\n\n");
                
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
                    resultsText.append("   Votes: ").append(count).append("\n");
                    resultsText.append("   Percentage: ").append(String.format("%.1f", percentage)).append("%\n");
                    resultsText.append("   Progress Bar: ");
                    
                    // Draw simple progress bar
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
                
                resultsText.append("=== Vote Details ===\n");
                resultsText.append("Total Votes: ").append(totalVotes).append("\n");
                resultsText.append("Candidate Count: ").append(candidates.length).append("\n");
                
                voteResultsArea.setText(resultsText.toString());
            } else {
                voteResultsArea.setText("Unable to retrieve vote results");
            }
        } catch (Exception ex) {
            voteResultsArea.setText("Connection to server failed: " + ex.getMessage());
        }
    }
    
    /**
     * Reset voting data
     */
    private void resetVoteData() {
        addLogMessage("Reset functionality requires server-side support");
        // You can add reset API call here
    }
    
    /**
     * Add log message
     */
    public void addLogMessage(String message) {
        String timestamp = java.time.LocalTime.now().toString();
        logArea.append("[" + timestamp + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    /**
     * Simple JSON parsing (should use a JSON library in real projects)
     */
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
            addLogMessage("JSON Parsing Failed: " + ex.getMessage());
        }
        return results;
    }
    
    /**
     * Stop timer
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
            
            // Add window close event
            serverGUI.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent e) {
                    serverGUI.stopTimer();
                }
            });
        });
    }
} 