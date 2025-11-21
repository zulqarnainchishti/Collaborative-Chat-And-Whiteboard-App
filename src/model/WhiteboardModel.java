package model;

import java.util.*;

public class WhiteboardModel {
    private final ArrayList<DrawAction> actions = new ArrayList<>();
    private final Stack<DrawAction> undoStack = new Stack<>();
    private final Stack<DrawAction> redoStack = new Stack<>();

    public synchronized void addAction(DrawAction a) {
        actions.add(a);
        undoStack.push(a);
        redoStack.clear();
    }

    public synchronized void setSnapshot(ArrayList<DrawAction> snap) {
        actions.clear();
        actions.addAll(snap);
        undoStack.clear();
        for (DrawAction da : snap)
            undoStack.push(da);
        redoStack.clear();
    }

    public synchronized ArrayList<DrawAction> getActionsCopy() {
        return new ArrayList<>(actions);
    }

    public synchronized void clear() {
        actions.clear();
        undoStack.clear();
        redoStack.clear();
    }

    public synchronized void undo() {
        if (!undoStack.isEmpty()) {
            DrawAction a = undoStack.pop();
            redoStack.push(a);
            actions.remove(a);
        }
    }

    public synchronized void redo() {
        if (!redoStack.isEmpty()) {
            DrawAction a = redoStack.pop();
            actions.add(a);
            undoStack.push(a);
        }
    }
}
