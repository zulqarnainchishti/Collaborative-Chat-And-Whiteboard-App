package controller;

import javax.swing.*;

import model.WhiteboardModel;
import view.WhiteboardView;

public class Client {
    public static void main(String[] args) throws Exception {
        String serverHost = "localhost";
        int serverPort = 6000;
        String inputUsername = JOptionPane.showInputDialog("Enter username:");
        final String username = (inputUsername == null || inputUsername.trim().isEmpty())
                ? "guest-" + System.currentTimeMillis() % 1000
                : inputUsername;

        SwingUtilities.invokeLater(() -> {
            WhiteboardModel model = new WhiteboardModel();
            WhiteboardView view = new WhiteboardView();
            try {
                new WhiteboardController(model, view, serverHost, serverPort, username);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(view, "Failed to connect: " + e.getMessage());
                System.exit(1);
            }
        });
    }
}
