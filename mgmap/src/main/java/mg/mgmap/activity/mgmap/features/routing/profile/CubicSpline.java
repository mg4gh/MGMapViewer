package mg.mgmap.activity.mgmap.features.routing.profile;
/**
 * computes natural cubic spline. Algorithm: <a href="https://en.wikipedia.org/wiki/Spline_(mathematics)">...</a>
 */
public class CubicSpline {
    private final float[][] polynominals;
    private final float[] x;

    public CubicSpline(float[] x, float[] y) throws Exception {
        if  ( x.length < 3 ) {
            throw new Exception("input array too short");
        } else if ( x.length != y.length ) {
            throw new Exception("x and y vector size don't match");
        }
        float min = - Float.MAX_VALUE;
        for (float v : x) {
            if (v <= min) throw new Exception("x not sorted in ascending order");
            min = v;
        }
        this.x = x;
        int n = x.length - 1;

        float[] h = new float[n];
        for (int i = 0; i < n; i++) {
            h[i] = x[i+1] - x[i];
        }
        float[] mu = new float[n];
        float[] z = new float[n+1];
        mu[0] = 0.0f;
        z[0] = 0.0f;
        float l;
        for (int i = 1; i < n; i++) {
            l = 2.0f * (x[i+1] - x[i-1]) - h[i-1] * mu[i-1];
            mu[i] = h[i] / l;
            z[i] = (3.0f * (y[i+1] * h[i-1] - y[i] * (x[i+1] - x[i-1])+ y[i-1] * h[i]) /
                    (h[i-1] * h[i]) - h[i-1] * z[i-1])/ l;
        }
        float[] b = new float[n];
        float[] c = new float[n+1];
        float[] d = new float[n];
        z[n] = 0.0f;
        c[n] = 0.0f;
        for (int j = n -1; j >=0; j--) {
            c[j] = z[j] - mu[j] * c[j+1];
            b[j] = (y[j + 1] - y[j])/h[j] - h[j] * (c[j+1] + 2.0f*c[j])/3.0f;
            d[j] = (c[j+1] - c[j]) / (3.0f* h[j]);
        }
        polynominals = new float[n+2][];
        polynominals[0] = new float[] {y[0],b[0]}; // initial linear section
        for ( int i = 0; i< n;i++){
            polynominals[i+1] = new float[] {y[i],b[i],c[i],d[i]};
        }
        float x1 = this.x[n] - this.x[n-1];
        float x2 = x1*x1;
        polynominals[n+1] = new float[] {y[n],polynominals[n][1] + 2* polynominals[n][2]*x1 + 3*polynominals[n][3]*x2}; // final linear section
    }

    public float calc(float x){
        int in = geti(x);
        float x1 = x - this.x[(in == 0)?0:in-1];
        if (in==0 || in==this.x.length)
            return polynominals[in][0] + polynominals[in][1]*x1;
        else {
            float x2 = x1*x1;
            float x3 = x2*x1;
            return polynominals[in][0] + polynominals[in][1] * x1 + polynominals[in][2] * x2 + polynominals[in][3] * x3;
        }
    }

    public float calcSlope(float x){
        int in = geti(x);
        if (in==0 || in==this.x.length)
            return polynominals[in][1];
        else {
            float x1 = x - this.x[in-1];
            float x2 = x1*x1;
            return polynominals[in][1] + 2f*polynominals[in][2]*x1 + 3f*polynominals[in][3]*x2;
        }
    }

    public float calcCurve(float x){
        int in = geti(x);
        if (in==0 || in==this.x.length)
            return 0f;
        else {
            float x1 = x - this.x[in - 1];
            return 2f * polynominals[in][2] + 6f * polynominals[in][3] * x1;
        }
    }

    private int geti(float x){
        int i;
        if ( x < this.x[0])
            i = 0;
        else {
            i = 1;
            while (i < this.x.length && x > this.x[i] ) i = i + 1;
        }
        return i;
    }

    public CubicSpline getCutCubicSpline(float min, float max){
        return new CubicSpline(this,min,max);
    }

    /* derive a Spline by cutting out a piece between min and max of an existing
    reference splin and continue as linear function. Used as heuristic */
    private CubicSpline(CubicSpline cubicSpline, float min, float max){
        int minInd = cubicSpline.geti(min);
        if (minInd < 1 )
            throw new RuntimeException("Slope of lower tangent too small for heuristic Spline:" + min);
        int maxInd = cubicSpline.geti(max);
        if (maxInd >= cubicSpline.x.length )
            throw new RuntimeException("Slope of upper tangent too large for heuristic Spline:" + max );
        int n = maxInd - minInd + 1;
        polynominals = new float[n+2][4];
        x = new float[n+1];
        /*first linear section */
        polynominals[0] = new float[] {cubicSpline.calc(min),cubicSpline.calcSlope(min)};
        x[0] = min;
        /* translation to new point min */
        polynominals[1][0] = cubicSpline.calc(min);
        polynominals[1][1] = cubicSpline.calcSlope(min);
        polynominals[1][2] = cubicSpline.calcCurve(min)/2f; // translation requires 2nd derivative / 2
        polynominals[1][3] = cubicSpline.polynominals[minInd][3]; // 3rd derivative does not vary
        /* using existing */
        for ( int i = 2; i<n+1;i++){
           x[i-1] = cubicSpline.x[minInd+i-2];
//           for ( int j = 0; j<4;j++){
//               polynominals[i][j] = cubicSpline.polynominals[minInd+i-1][j];
//           }
           System.arraycopy(cubicSpline.polynominals[minInd + i - 1], 0, polynominals[i], 0, 4);
        }
        /* last linear section*/
        x[n] = max;
        polynominals[n+1] = new float[] {cubicSpline.calc(max),cubicSpline.calcSlope(max)};
    }

}
