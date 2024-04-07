import java.util.Arrays;
import java.util.List;

/**
 * A context map stores a set of direction and how good or bad to move in those directions,
 * directions radiate out of the agent like spokes of a wheel
 * each different context map is sinonimous with a behavior.<p>
 * Context maps use cartesian coordinates for directions, however unless otherwise noted, the
 * orientation of the context map does not matter (ie works the same if (0,0) is on the top left
 * or bottom left)
 */
public abstract class ContextMap {
    static int numbins = 16;
    float[] bins = new float[numbins];
    static final Vector2D[] bindir = new Vector2D[numbins];
    static {
        for (int i = 0; i < numbins;i++) {
            float theta = (float) (2.0* Math.PI / numbins * i);
            bindir[i] = new Vector2D(1.0F, theta).toCartesian();
        }
    }

    public ContextMap() {
        Arrays.fill(bins, 0.0F);
    }
    public ContextMap(ContextMap other) {
        for (int i = 0; i < numbins; i++) {
            bins[i] = other.bins[i];
        }
    }

    public void populateContext(Agent agent, List<ContextMap> otherContexts) {};

    /**
     * Sets all bins to 0.0
     */
    public final void clearContext() {
        Arrays.fill(bins, 0.0F);
    }

    /**
     * Takes the dot product of a input direction, and the different directional bins of the
     * context map, the directional bin closest to the input direction will have the highest
     * magnitude. <p>
     * Taking the dot product is ideal for giving an agent more options to choose from in making
     * a decision
     * @param direction to check against
     * @param shift shift the dot product from [-1,1] with + value
     * @param scale scales the dot product after shifting, ie to provide more magnitude to the bins.
     */
    public final void dotContext(Vector2D direction, float shift, float scale) {
        for (int i = 0; i < numbins;i++) {
            bins[i] += (bindir[i].dot(direction) + shift) * scale;
        }
    }

    public final void addContext(ContextMap other) {
        for (int i = 0; i < numbins; i++) {
            this.bins[i] += other.bins[i];
        }
    }

    public final void addScalar(float val) {
        for (int i = 0; i < numbins; i++) {
            this.bins[i] += val;
        }
    }

    public  final void multScalar(float val) {
        for (int i = 0; i < numbins; i++) {
            this.bins[i] *= val;
        }
    }

    public static ContextMap scaled(ContextMap map,float val) {
        ContextMap output = new ContextMap(map) { };
        output.multScalar(val);
        return output;
    }

    /**
     * Masks a context using a danger, masking is essential for obstical avoidance and making sure
     * the agent does not make incorrect decisions.
     * @param other the danger context map
     * @param threshold
     */
    public final void maskContext(ContextMap other, float threshold) {
        for (int i = 0; i < numbins; i++) {
            if (other.bins[i] >= threshold) {
                this.bins[i] = -1000.0f;
            }
        }
    }

    /**
     * Finds the argmax and adjacent bins, because context maps are circular, indices wrap around
     */
    public final int[] maxAdjacent() {
        int[] indices = new int[3];
        indices[0] = 0; indices[1] = 1; indices[2] = 2;
        float maxVal = bins[1];
        for (int i = 2; i != 1; i = (i + 1) % numbins) {
            float val = bins[i];
            if (val > maxVal) {
                indices[0] = (i - 1 + numbins) % numbins;
                indices[1] = i;
                indices[2] = (i + 1) % numbins;
                maxVal = val;
            }
        }
        return indices;
    }

    /**
     * Using the bin information, outputs a proposed direction (a decision) by finding the
     * direction with the maximum weight, and interpolating with adjacent directions
     * negative weights are ignored
     * @return The proposed interpolated decision (in cartesian cords)
     */
    public final Vector2D interpolatedMaxDir() {
        int[] indices = maxAdjacent();
        float[] vals = {bins[indices[0]], bins[indices[1]], bins[indices[2]]};
        Vector2D[] dirs = {bindir[indices[0]], bindir[indices[1]], bindir[indices[2]]};

        //soft max
        float sum = 1e-5f;
        for (int i = 0; i < 3; i++) {
            //vals[i] = (float) Math.exp(vals[i]);
            vals[i] = vals[i] < 0 ? 0.0F: vals[i];
            sum += vals[i];
        }
        for (int i = 0; i < 3; i++) {
            vals[i] /= sum;
        }

        //interpolate
        Vector2D output = new Vector2D(0.0F,0.0F);
        for (int i = 0; i < 3; i++) {
            output = output.add(dirs[i].mult(vals[i]));
        }
        //if no decision was made then default to face east
        if (output.mag() < 1e-5f) output = new Vector2D(1,0);
        output = output.normal();
        return output;
    }
}
