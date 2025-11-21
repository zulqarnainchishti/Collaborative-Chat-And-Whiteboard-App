package controller;

import java.awt.*;
import java.awt.event.*;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.*;

import model.DrawAction;
import model.Message;
import model.WhiteboardModel;
import view.WhiteboardView;

public class WhiteboardController {
    private final WhiteboardModel model;
    private final WhiteboardView view;
    private final String username;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private DrawAction.ActionType currentTool = DrawAction.ActionType.FREEHAND;
    private Color currentColor = Color.BLACK;
    private float currentStroke = 2.0f;
    private ArrayList<Point> tempPoints = new ArrayList<>();
    private Point startPoint = null;

    public WhiteboardController(WhiteboardModel model, WhiteboardView view,
            String serverHost, int serverPort, String username) throws Exception {
        this.model = model;
        this.view = view;
        this.username = username;
        setupNetwork(serverHost, serverPort);
        wireView();
        startNetworkReader();
        refreshView();
    }

    private void setupNetwork(String host, int port) throws Exception {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        // handshake: send initial message with username
        Message init = new Message(Message.MessageType.SYNC_REQUEST);
        init.senderId = username;
        out.writeObject(init);
        out.reset();
    }

    private void startNetworkReader() {
        Thread reader = new Thread(() -> {
            try {
                while (true) {
                    Message m = (Message) in.readObject();
                    if (m == null)
                        break;
                    switch (m.type) {
                        case SYNC_RESPONSE:
                            model.setSnapshot(m.canvasSnapshot != null ? m.canvasSnapshot : new ArrayList<>());
                            refreshView();
                            if (m.text != null)
                                appendChat("[server] " + m.text);
                            break;
                        case DRAW:
                            model.addAction(m.drawAction);
                            refreshView();
                            break;
                        case CHAT:
                            appendChat(m.senderId + ": " + m.text);
                            break;
                        case CONTROL:
                            if (m.control == Message.ControlType.CLEAR)
                                model.clear();
                            else if (m.control == Message.ControlType.UNDO)
                                model.undo();
                            else if (m.control == Message.ControlType.REDO)
                                model.redo();
                            refreshView();
                            break;
                        case NOTIFICATION:
                            appendChat("[notify] " + m.text);
                            break;
                        default:
                            break;
                    }
                }
            } catch (Exception ex) {
                appendChat("[error] connection lost: " + ex.getMessage());
            }
        });
        reader.setDaemon(true);
        reader.start();
    }

    private void wireView() {
        view.sendBtn.addActionListener(e -> sendChat());
        view.chatInput.addActionListener(e -> sendChat());

        view.pencilBtn.addActionListener(e -> currentTool = DrawAction.ActionType.FREEHAND);
        view.lineBtn.addActionListener(e -> currentTool = DrawAction.ActionType.LINE);
        view.rectBtn.addActionListener(e -> currentTool = DrawAction.ActionType.RECT);
        view.circleBtn.addActionListener(e -> currentTool = DrawAction.ActionType.CIRCLE);
        view.eraserBtn.addActionListener(e -> currentTool = DrawAction.ActionType.ERASER);
        view.colorBtn.addActionListener(e -> {
            Color c = JColorChooser.showDialog(view, "Choose Color", currentColor);
            if (c != null)
                currentColor = c;
        });
        view.clearBtn.addActionListener(e -> sendControl(Message.ControlType.CLEAR));
        view.undoBtn.addActionListener(e -> sendControl(Message.ControlType.UNDO));
        view.redoBtn.addActionListener(e -> sendControl(Message.ControlType.REDO));

        // Mouse handling for canvas
        WhiteboardView.DrawCanvas canvas = view.canvas;
        canvas.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
                tempPoints.clear();
                tempPoints.add(startPoint);
            }

            public void mouseReleased(MouseEvent e) {
                Point end = e.getPoint();
                DrawAction action = null;
                if (currentTool == DrawAction.ActionType.FREEHAND || currentTool == DrawAction.ActionType.ERASER) {
                    tempPoints.add(end);
                    if (currentTool == DrawAction.ActionType.ERASER)
                        action = DrawAction.eraser(tempPoints, currentStroke * 4);
                    else
                        action = DrawAction.freehand(tempPoints, currentColor, currentStroke);
                } else {
                    if (currentTool == DrawAction.ActionType.LINE)
                        action = DrawAction.line(startPoint.x, startPoint.y, end.x, end.y, currentColor, currentStroke);
                    else if (currentTool == DrawAction.ActionType.RECT)
                        action = DrawAction.rect(startPoint.x, startPoint.y, end.x, end.y, currentColor, currentStroke);
                    else if (currentTool == DrawAction.ActionType.CIRCLE)
                        action = DrawAction.circle(startPoint.x, startPoint.y, end.x, end.y, currentColor,
                                currentStroke);
                }
                if (action != null) {
                    model.addAction(action);
                    refreshView();
                    sendDraw(action);
                }
                tempPoints = new ArrayList<>();
                startPoint = null;
            }
        });

        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (currentTool == DrawAction.ActionType.FREEHAND || currentTool == DrawAction.ActionType.ERASER) {
                    Point p = e.getPoint();
                    tempPoints.add(p);
                    // show intermediate line locally for immediate feedback
                    model.addAction(new DrawAction() {
                        {
                            actionType = DrawAction.ActionType.FREEHAND;
                            points = new ArrayList<>(tempPoints);
                            color = currentTool == DrawAction.ActionType.ERASER ? Color.WHITE : currentColor;
                            strokeWidth = currentStroke;
                        }
                    });
                    refreshView();
                    model.undo();
                }
            }
        });
    }

    private void appendChat(String s) {
        SwingUtilities.invokeLater(() -> {
            view.chatArea.append(s + "\n");
        });
    }

    private void refreshView() {
        SwingUtilities.invokeLater(() -> {
            view.canvas.setActions(model.getActionsCopy());
        });
    }

    private void sendChat() {
        String text = view.chatInput.getText().trim();
        if (text.isEmpty())
            return;
        Message m = new Message(Message.MessageType.CHAT);
        m.senderId = username;
        m.text = text;
        try {
            out.writeObject(m);
            out.reset();
            view.chatInput.setText("");
        } catch (Exception ex) {
            appendChat("[error] sendChat: " + ex.getMessage());
        }
    }

    private void sendDraw(DrawAction a) {
        try {
            Message m = new Message(Message.MessageType.DRAW);
            m.senderId = username;
            m.drawAction = a;
            out.writeObject(m);
            out.reset();
        } catch (Exception ex) {
            appendChat("[error] sendDraw: " + ex.getMessage());
        }
    }

    private void sendControl(Message.ControlType ct) {
        try {
            Message m = new Message(Message.MessageType.CONTROL);
            m.senderId = username;
            m.control = ct;
            out.writeObject(m);
            out.reset();
        } catch (Exception ex) {
            appendChat("[error] sendControl: " + ex.getMessage());
        }
    }
}
