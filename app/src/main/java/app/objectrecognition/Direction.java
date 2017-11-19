package app.objectrecognition;

public class Direction {
    private  float x;
    private float y;
    private float z;
    private String description;

    Direction(){};

    Direction(float x, float y, float z, String description){
        this.x = x;
        this.y = y;
        this.z = z;
        this.description = description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public String getDescription() {
        return description;
    }
}
