package ovgu.creasy.geom;

import java.util.Objects;

public class Vertex {
    private Point point;
    private Type type;

    public enum Type {
        INTERNAL,

        BORDER,

        VIRTUAL;
    }

    /**
     * @param point is a point in the x-y plane representing the position of the vertex after the model is folded
     * @param type  is the type of the vertex
     */
    public Vertex(Point point, Type type) {
        this.point = point;
        this.type = type;
    }

    public Point getPoint() {
        return point;
    }

    public Type getType() {
        return type;
    }

    public void setPoint(Point point) {
        this.point = point;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Vertex{" +
                "Point=" + point.getX() + point.getY() +
                ", type=" + type +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(point, type);
    }
}
