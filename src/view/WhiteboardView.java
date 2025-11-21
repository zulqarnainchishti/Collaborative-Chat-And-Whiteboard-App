package view;

import javax.swing.*;

import model.DrawAction;

import java.awt.*;
import java.util.ArrayList;

public class WhiteboardView extends JFrame {

    public final DrawCanvas canvas;
    public final JTextArea chatArea = new JTextArea();
    public final JTextField chatInput = new JTextField();
    public final JButton sendBtn = new JButton("Send");
    public final JButton pencilBtn = new JButton("Pencil");
    public final JButton lineBtn = new JButton("Line");
    public final JButton rectBtn = new JButton("Rect");
    public final JButton circleBtn = new JButton("Circle");
    public final JButton eraserBtn = new JButton("Eraser");
    public final JButton undoBtn = new JButton("Undo");
    public final JButton redoBtn = new JButton("Redo");
    public final JButton clearBtn = new JButton("Clear");
    public final JButton colorBtn = new JButton("Color");

    public WhiteboardView() {
        super("Collaborative Chat & Drawing Board");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(pencilBtn);
        top.add(lineBtn);
        top.add(rectBtn);
        top.add(circleBtn);
        top.add(eraserBtn);
        top.add(colorBtn);
        top.add(undoBtn);
        top.add(redoBtn);
        top.add(clearBtn);

        canvas = new DrawCanvas();
        canvas.setPreferredSize(new Dimension(700, 500));

        JPanel right = new JPanel(new BorderLayout());
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setPreferredSize(new Dimension(280, 450));

        JPanel chatInputPanel = new JPanel(new BorderLayout());
        chatInputPanel.add(chatInput, BorderLayout.CENTER);
        chatInputPanel.add(sendBtn, BorderLayout.EAST);

        right.add(chatScroll, BorderLayout.CENTER);
        right.add(chatInputPanel, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(canvas, BorderLayout.CENTER);
        getContentPane().add(right, BorderLayout.EAST);

        setVisible(true);
    }

    public static class DrawCanvas extends JPanel {
        private ArrayList<DrawAction> actions = new ArrayList<>();

        public DrawCanvas() {
            setBackground(Color.WHITE);
        }

        public void setActions(ArrayList<DrawAction> a) {
            this.actions = a;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            for (DrawAction a : actions) {
                g.setStroke(new BasicStroke(a.strokeWidth));
                g.setColor(a.color);
                switch (a.actionType) {
                    case FREEHAND:
                    case ERASER:
                        for (int i = 1; i < a.points.size(); i++) {
                            Point p1 = a.points.get(i - 1);
                            Point p2 = a.points.get(i);
                            g.drawLine(p1.x, p1.y, p2.x, p2.y);
                        }
                        break;
                    case LINE:
                        g.drawLine(a.x1, a.y1, a.x2, a.y2);
                        break;
                    case RECT:
                        int rx = Math.min(a.x1, a.x2), ry = Math.min(a.y1, a.y2);
                        int rw = Math.abs(a.x2 - a.x1), rh = Math.abs(a.y2 - a.y1);
                        g.drawRect(rx, ry, rw, rh);
                        break;
                    case CIRCLE:
                        int cx = Math.min(a.x1, a.x2), cy = Math.min(a.y1, a.y2);
                        int cw = Math.abs(a.x2 - a.x1), ch = Math.abs(a.y2 - a.y1);
                        g.drawOval(cx, cy, cw, ch);
                        break;
                }
            }
        }
    }
}
