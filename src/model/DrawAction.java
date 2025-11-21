package model;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.UUID;

public class DrawAction implements Serializable {

    public enum ActionType {
        FREEHAND, LINE, RECT, CIRCLE, ERASER
    }

    public ActionType actionType;
    public ArrayList<Point> points;
    public Color color;
    public float strokeWidth;

    public int x1, y1, x2, y2;

    public final UUID actionId = UUID.randomUUID();

    public DrawAction() {
        this.points = new ArrayList<>();
        this.color = Color.BLACK;
        this.strokeWidth = 2.0f;
    }

    public static DrawAction line(int x1, int y1, int x2, int y2, Color c, float w) {
        DrawAction a = new DrawAction();
        a.actionType = ActionType.LINE;
        a.x1 = x1;
        a.y1 = y1;
        a.x2 = x2;
        a.y2 = y2;
        a.color = c;
        a.strokeWidth = w;
        return a;
    }

    public static DrawAction freehand(ArrayList<Point> pts, Color c, float w) {
        DrawAction a = new DrawAction();
        a.actionType = ActionType.FREEHAND;
        a.points = new ArrayList<>(pts);
        a.color = c;
        a.strokeWidth = w;
        return a;
    }

    public static DrawAction rect(int x1, int y1, int x2, int y2, Color c, float w) {
        DrawAction a = new DrawAction();
        a.actionType = ActionType.RECT;
        a.x1 = x1;
        a.y1 = y1;
        a.x2 = x2;
        a.y2 = y2;
        a.color = c;
        a.strokeWidth = w;
        return a;
    }

    public static DrawAction circle(int x1, int y1, int x2, int y2, Color c, float w) {
        DrawAction a = new DrawAction();
        a.actionType = ActionType.CIRCLE;
        a.x1 = x1;
        a.y1 = y1;
        a.x2 = x2;
        a.y2 = y2;
        a.color = c;
        a.strokeWidth = w;
        return a;
    }

    public static DrawAction eraser(ArrayList<Point> pts, float w) {
        DrawAction a = new DrawAction();
        a.actionType = ActionType.ERASER;
        a.points = new ArrayList<>(pts);
        a.color = Color.WHITE;
        a.strokeWidth = w;
        return a;
    }
}
