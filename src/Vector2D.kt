import kotlin.String
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.require

class Vector2D(x: Double, y: Double) {
    val x: Double
    val y: Double

    init {
        require(!x.isNaN()) { "x value is Nan" }
        require(!y.isNaN()) { "y value is Nan" }
        this.x = x
        this.y = y
    }

    fun add(other: Vector2D): Vector2D {
        return Vector2D(x + other.x, y + other.y)
    }

    fun add(x: Double, y: Double): Vector2D {
        return this.add(Vector2D(x, y))
    }

    fun mult(s: Double): Vector2D {
        return Vector2D(x * s, y * s)
    }

    fun mag(): Double {
        return sqrt(x.pow(2.0) + y.pow(2.0))
    }

    fun normal(): Vector2D {
        return mult(1 / mag())
    }

    fun clip(s: Double): Vector2D {
        return if (mag() > s) normal().mult(s) else Vector2D(x, y)
    }

    fun wrap(w: Double, h: Double): Vector2D {
        val x = (x + w) % w
        val y = (y + h) % h
        return Vector2D(x, y)
    }

    fun bound(w: Double, h: Double, buffer: Double =  10.0) : Vector2D {
        val x = max(min(x,w - buffer ), buffer)
        val y = max(min(y,h - buffer ), buffer)
        return Vector2D(x, y)
    }

    fun dot(other: Vector2D):Double {
        return x * other.x + y * other.y
    }

    fun toPolar(): Vector2D {
        val r = mag()
        val theta = atan2(x, -y)
        return Vector2D(r, theta)
    }

    fun toCartesian(): Vector2D {
        return Vector2D(
            (x * cos(y)),
            (x * sin(y))
        )
    }

    override fun toString(): String {
        return "Vector2D($x,$y)"
    }
}
