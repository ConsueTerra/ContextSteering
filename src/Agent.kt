abstract class Agent(var ship: Ship) {
    abstract fun steer() : SteeringOutput
    data class SteeringOutput(val newHeading : Vector2D, val newThrust : Vector2D)
}
