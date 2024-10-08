import java.util.Arrays
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * A context map stores a set of direction and how good or bad to move in those directions,
 * directions radiate out of the agent like spokes of a wheel
 * each different context map is sinonimous with a behavior.
 *
 *
 * Context maps use cartesian coordinates for directions, however unless otherwise noted, the
 * orientation of the context map does not matter (ie works the same if (0,0) is on the top left
 * or bottom left)
 */
abstract class ContextMap {

    companion object {
        const val NUMBINS = 16
        const val NUMLINBINS = 12
        val bindir = Array(NUMBINS) {
                i ->
            val theta = (2.0 * Math.PI / NUMBINS * i)
            Vector2D(1.0, theta).toCartesian()
        }

        fun scaled(map: ContextMap, `val`: Double): ContextMap {
            val output: ContextMap = object : ContextMap(map) {}
            output.multScalar(`val`)
            return output
        }
    }

    val bins = DoubleArray(NUMBINS)
    var lincontext : LinearContext? = null


    constructor(linearbins : Boolean = false) {
        Arrays.fill(bins, 0.0)
        if (linearbins) {
            lincontext = LinearContext()
        }
    }

    constructor(other: ContextMap, linearbins : Boolean = false) {
        for (i in 0 until NUMBINS) {
            bins[i] = other.bins[i]
        }
        if (linearbins && other.lincontext !=null) {
            lincontext = LinearContext(other.lincontext!!)
        }
    }

    open fun populateContext() {}

    /**
     * Sets all bins to 0.0
     */
    fun clearContext() {
        Arrays.fill(bins, 0.0)
        lincontext?.clearContext()
    }

    /**
     * Takes the dot product of a input direction, and the different directional bins of the
     * context map, the directional bin closest to the input direction will have the highest
     * magnitude.
     *
     *
     * Taking the dot product is ideal for giving an agent more options to choose from in making
     * a decision
     * @param direction to check against
     * @param shift shift the dot product from [-1,1] with + value
     * @param scale scales the dot product after shifting, ie to provide more magnitude to the bins.
     * @param power exponent to ally after shifting and before scaling
     */
    fun dotContext(direction: Vector2D, shift: Double, scale: Double,power: Double = 1.0, clipZero: Boolean = true) {
        for (i in 0 until NUMBINS) {
            val value = (bindir[i].dot(direction) + shift).pow(power) * scale
            bins[i] += if (clipZero) max(0.0,value) else value
        }
    }

    fun addContext(other: ContextMap, scale: Double = 1.0) {
        for (i in 0 until NUMBINS) {
            bins[i] += scale * other.bins[i]
        }
        other.lincontext?.let { lincontext?.addContext(it)}
    }

    fun addScalar(`val`: Double) {
        require(!`val`.isNaN()) { "scalar value is Nan" }
        for (i in 0 until NUMBINS) {
            bins[i] += `val`
        }
    }

    fun multScalar(`val`: Double) {
        require(!`val`.isNaN()) { "scalar value is Nan" }
        for (i in 0 until NUMBINS) {
            bins[i] *= `val`
        }
    }

    fun clipZero() {
        for (i in 0 until NUMBINS) {
            if (bins[i] < 0.0) bins[i] = 0.0
        }
    }

    /**
     * Masks a context using a danger, masking is essential for obstical avoidance and making sure
     * the agent does not make incorrect decisions.
     * @param other the danger context map
     * @param threshold
     */
    fun maskContext(other: ContextMap, threshold: Double) {
        for (i in 0 until NUMBINS) {
            if (other.bins[i] >= threshold) {
                bins[i] = -1000.0
            }
        }
    }

    fun softMaskContext(other: ContextMap, threshold: Double) {
        for (i in 0 until NUMBINS) {
            val sigmoid : Double
            val x = (threshold - other.bins[i])*10.0
            sigmoid = if (x < 0) {
                exp(x) / (1.0 + exp(x))
            } else {
                1 / (1 + exp(-x))
            }
            bins[i] *= sigmoid
        }
    }

    /**
     * Finds the argmax and adjacent bins, because context maps are circular, indices wrap around
     */
    fun maxAdjacent(): IntArray {
        val indices = IntArray(3)
        indices[0] = 0
        indices[1] = 1
        indices[2] = 2
        var maxVal = bins[1]
        var i = 2
        while (i != 1) {
            val `val` = bins[i]
            if (`val` > maxVal) {
                indices[0] = (i - 1 + NUMBINS) % NUMBINS
                indices[1] = i
                indices[2] = (i + 1) % NUMBINS
                maxVal = `val`
            }
            i = (i + 1) % NUMBINS
        }
        return indices
    }

    /**
     * Using the bin information, outputs a proposed direction (a decision) by finding the
     * direction with the maximum weight, and interpolating with adjacent directions
     * negative weights are ignored
     * @return The proposed interpolated decision (in cartesian cords)
     */
    fun interpolatedMaxDir(): Vector2D {
        val indices = maxAdjacent()
        val vals = doubleArrayOf(bins[indices[0]], bins[indices[1]], bins[indices[2]])
        val dirs = arrayOf(bindir[indices[0]], bindir[indices[1]], bindir[indices[2]])

        //soft max
        var sum = 1e-5
        for (i in 0..2) {
            //vals[i] = (float) Math.exp(vals[i]);
            vals[i] = if (vals[i] < 0) 0.0 else vals[i]
            sum += vals[i]
        }
        for (i in 0..2) {
            vals[i] /= sum
        }

        //interpolate
        var output = Vector2D(0.0, 0.0)
        for (i in 0..2) {
            output = output.add(dirs[i].mult(vals[i]))
        }
        //if no decision was made then default to random direction
        if (output.mag() < 1e-5) output = Vector2D(1.0, Math.random() * Math.PI * 2).toCartesian()
        return output
    }

    inner class LinearContext {
        val bins = DoubleArray(NUMLINBINS)
        val binloc = Array(NUMLINBINS) { i ->
            val spacing = 1.0 /(NUMLINBINS - 3).toDouble()
            (i - 1) * spacing
        }
        constructor() {
            bins.fill(0.0)
        }

        constructor(other: LinearContext) {
            for (i in 0 until NUMLINBINS) {
                bins[i] = other.bins[i]
            }
        }

        /**
         * Sets all bins to 0.0
         */
        fun clearContext() {
            Arrays.fill(bins, 0.0)
        }

        fun addContext(other: LinearContext) {
            for (i in 0 until NUMLINBINS) {
                bins[i] += other.bins[i]
            }
        }

        inline fun apply(func : (i : Int) -> Double, clear : Boolean = true) {
            if (clear) clearContext()
            for (i in 0 until NUMLINBINS) {
                bins[i] += func(i)
            }
        }

        fun populatePeak(scalar : Double, weight : Double, edgepoints: Boolean = true, spread : Double = 0.25, power : Double = 2.0): (Int) -> Double {
            var scaled = max(0.0, min(scalar, 1.0))
            if (edgepoints) {
                val range = binloc.last() - binloc.first()
                scaled = scaled * range + binloc.first()
            }
            val lamb  = {i : Int ->
                val loc = binloc[i]
                val dist = abs(scaled - loc)
                max(0.0, 1.0 - dist/spread).pow(power)*weight
            }
            return lamb
        }

        fun interpolotedMax(): Double {
            val i = bins.withIndex().maxBy { it.value }.index
            var j = i-1
            var maxval = 0.0
            for (k in i-1 until i+2){
                if (k == i || k < 0 || k == NUMLINBINS) {
                    continue
                }
                if (bins[k] > maxval) {
                    maxval = bins[k]
                    j = k
                }
            }
            val interpolated = (bins[i]*binloc[i] + bins[j]*binloc[j])/(bins[i]+bins[j])
            return max(0.0, min(interpolated, 1.0))
        }
    }
}
