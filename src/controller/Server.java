package controller;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import model.DrawAction;
import model.Message;

public class Server {
    private final int port;
    private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final ArrayList<DrawAction> canvas = new ArrayList<>();

    public Server(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        int port = 6000;
        Server server = new Server(port);
        server.start();
    }

    public void start() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            while (true) {
                Socket s = serverSocket.accept();
                ClientHandler h = new ClientHandler(s, this);
                clients.add(h);
                new Thread(h).start();
            }
        }
    }

    public synchronized void broadcast(Message m) {
        for (ClientHandler ch : clients) {
            try {
                ch.send(m);
            } catch (Exception ex) {
                /* client will be removed on error */ }
        }
    }

    public synchronized void addDrawAction(DrawAction a) {
        canvas.add(a);
    }

    public synchronized ArrayList<DrawAction> getCanvasSnapshot() {
        return new ArrayList<>(canvas);
    }

    public synchronized void clearCanvas() {
        canvas.clear();
    }

    public synchronized void undoLast() {
        if (!canvas.isEmpty())
            canvas.remove(canvas.size() - 1);
    }

    public synchronized void removeClient(ClientHandler ch) {
        clients.remove(ch);
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final Server server;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private String clientId = "unknown";

        ClientHandler(Socket s, Server server) {
            this.socket = s;
            this.server = server;
        }

        public void send(Message m) throws IOException {
            out.writeObject(m);
            out.reset();
        }

        public void run() {
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());

                // Expect initial handshake: a Message with senderId and a SYNC_REQUEST or CHAT
                Message init = (Message) in.readObject();
                if (init != null) {
                    clientId = init.senderId != null ? init.senderId : ("client-" + socket.getPort());
                    System.out.println("Client connected: " + clientId);
                    // respond with canvas snapshot
                    Message snap = new Message(Message.MessageType.SYNC_RESPONSE);
                    snap.canvasSnapshot = server.getCanvasSnapshot();
                    snap.senderId = "server";
                    send(snap);

                    // notify others
                    Message notif = new Message(Message.MessageType.NOTIFICATION);
                    notif.text = clientId + " joined.";
                    notif.senderId = "server";
                    server.broadcast(notif);
                }

                // Main loop: read messages and process
                while (true) {
                    Message m = (Message) in.readObject();
                    if (m == null)
                        break;
                    switch (m.type) {
                        case CHAT:
                            System.out.println("CHAT from " + m.senderId + ": " + m.text);
                            server.broadcast(m);
                            break;
                        case DRAW:
                            if (m.drawAction != null) {
                                server.addDrawAction(m.drawAction);
                                server.broadcast(m);
                            }
                            break;
                        case CONTROL:
                            if (m.control == Message.ControlType.CLEAR) {
                                server.clearCanvas();
                                server.broadcast(m);
                            } else if (m.control == Message.ControlType.UNDO) {
                                server.undoLast();
                                server.broadcast(m);
                            } else if (m.control == Message.ControlType.REDO) {
                                // REDO support would require a redo stack; for brevity we broadcast the request
                                server.broadcast(m);
                            }
                            break;
                        case SYNC_REQUEST:
                            Message resp = new Message(Message.MessageType.SYNC_RESPONSE);
                            resp.canvasSnapshot = server.getCanvasSnapshot();
                            resp.senderId = "server";
                            send(resp);
                            break;
                        default:
                            break;
                    }
                }
            } catch (Exception e) {
                System.out.println("Client " + clientId + " disconnected or error: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (Exception ignored) {
                }
                server.removeClient(this);
                Message notif = new Message(Message.MessageType.NOTIFICATION);
                notif.text = clientId + " left.";
                notif.senderId = "server";
                server.broadcast(notif);
            }
        }
    }
}
