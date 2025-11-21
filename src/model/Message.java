package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class Message implements Serializable {

    public enum MessageType {
        CHAT, DRAW, CONTROL, SYNC_REQUEST, SYNC_RESPONSE, NOTIFICATION
    }

    public enum ControlType {
        UNDO, REDO, CLEAR
    }

    public MessageType type;
    public String senderId;
    public String text;
    public DrawAction drawAction;
    public ArrayList<DrawAction> canvasSnapshot;
    public ControlType control;
    public UUID targetActionId;

    public Message(MessageType t) {
        this.type = t;
    }
}
