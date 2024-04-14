public class Vector2D {
    final float x;
    final float y;

    public Vector2D(float x, float y) {
        if (Float.isNaN(x)) throw new IllegalArgumentException("x value is Nan");
        if (Float.isNaN(y)) throw new IllegalArgumentException("y value is Nan");
        this.x = x;
        this.y = y;
    }

    public Vector2D add(Vector2D other) {
        return new Vector2D(this.x + other.x, this.y + other.y);
    }

    public Vector2D add(float x, float y) {
        return this.add(new Vector2D(x,y));
    }

    public Vector2D mult(float s) {
        return new Vector2D(this.x * s, this.y * s);
    }

    public float mag() {
        return (float) Math.sqrt(Math.pow(this.x,2)+Math.pow(this.y,2));
    }

    public  Vector2D normal() {
        return mult(1 /mag());
    }

    public Vector2D clip(float s) {
        return (mag() > s) ? normal().mult(s) : new Vector2D(this.x, this.y);
    }

    public Vector2D wrap(float w, float h) {
        float x  = (this.x + w) % w;
        float y = (this.y + h) % h;
        return new Vector2D(x, y);
    }

    public float dot(Vector2D other) {
        return this.x * other.x + this.y * other.y;
    }

    public Vector2D toPolar() {
        float r = mag();
        float theta = (float) Math.atan2(this.x, -this.y);
        return new Vector2D(r,theta);
    }

    public Vector2D toCartesian() {
        return new Vector2D((float) (this.x * Math.cos(this.y)), (float) (this.x * Math.sin(this.y)));
    }

    @Override
    public String toString() {
        return "Vector2D(" + x + "," + y + ")";
    }
}
