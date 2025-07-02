package p.projectone.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class VoteClientGUI extends JFrame {
    private JTextField userIdField;
    private JComboBox<String> candidateBox;
    private JButton voteButton;
    private JButton refreshButton;
    private JTextArea resultArea;

    // 假设候选人列表
    private String[] candidates = {"Alice", "Bob", "Charlie"};
    private String[] candidateIds = {"1", "2", "3"};

    public VoteClientGUI() {
        setTitle("Distributed Voting System Client");
        setSize(400, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
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

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setBorder(BorderFactory.createTitledBorder("Voting Results"));
        add(new JScrollPane(resultArea), BorderLayout.CENTER);

        // Vote button event
        voteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userId = userIdField.getText().trim();
                int idx = candidateBox.getSelectedIndex();
                String candidateId = candidateIds[idx];
                String candidateName = candidates[idx];
                if (userId.isEmpty()) {
                    JOptionPane.showMessageDialog(VoteClientGUI.this, "Please enter User ID!");
                    return;
                }
                boolean success = sendVote(userId, candidateId, candidateName);
                if (success) {
                    JOptionPane.showMessageDialog(VoteClientGUI.this, "Vote successful!");
                    refreshResults();
                } else {
                    JOptionPane.showMessageDialog(VoteClientGUI.this, "Vote failed or already voted!");
                }
            }
        });

        // Refresh button event
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshResults();
            }
        });

        // Auto refresh on startup
        refreshResults();
    }

    // 发送投票请求
    private boolean sendVote(String userId, String candidateId, String candidateName) {
        try {
            URL url = new URL("http://10.72.83.45:8080/api/vote");
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
            if (code == 200) {
                return true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    // 查询投票结果
    private void refreshResults() {
        try {
            URL url = new URL("http://10.72.83.45:8080/api/vote/results");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                br.close();
                // 解析JSON
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Integer> resultMap = mapper.readValue(sb.toString(), new TypeReference<Map<String, Integer>>(){});
                StringBuilder resultText = new StringBuilder();
                for (int i = 0; i < candidates.length; i++) {
                    String name = candidates[i];
                    String cid = candidateIds[i];
                    int count = resultMap.getOrDefault(cid, 0);
                    resultText.append(name).append(": ").append(count).append(" 票\n");
                }
                resultArea.setText(resultText.toString());
            } else {
                resultArea.setText("无法获取投票结果");
            }
        } catch (Exception ex) {
            resultArea.setText("无法获取投票结果\n" + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VoteClientGUI().setVisible(true);
        });
    }
} 