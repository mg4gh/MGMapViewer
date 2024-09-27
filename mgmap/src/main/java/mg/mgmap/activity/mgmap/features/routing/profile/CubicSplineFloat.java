package mg.mgmap.activity.mgmap.features.routing.profile;

public class CubicSplineFloat {
    private final float[][] polynominals;
    private final float[] x;

    public CubicSplineFloat(float x[], float y[]) throws Exception {
        if  ( x.length < 3 ) {
            throw new Exception("input array too short");
        } else if ( x.length != y.length ) {
            throw new Exception("x and y vector size don't match");
        }
        float min = - Float.MAX_VALUE;
        for ( int i = 0; i<x.length; i++){
            if (x[i] <= min) throw new Exception("x not sorted in ascending order");
            min = x[i];
        }
        this.x = x;
        int n = x.length - 1;

        float h[] = new float[n];
        for (int i = 0; i < n; i++) {
            h[i] = x[i+1] - x[i];
        }
        float mu[] = new float[n];
        float z[] = new float[n+1];
        mu[0] = 0.0f;
        z[0] = 0.0f;
        float l;
        for (int i = 1; i < n; i++) {
            l = 2.0f * (x[i+1] - x[i-1]) - h[i-1] * mu[i-1];
            mu[i] = h[i] / l;
            z[i] = (3.0f * (y[i+1] * h[i-1] - y[i] * (x[i+1] - x[i-1])+ y[i-1] * h[i]) /
                    (h[i-1] * h[i]) - h[i-1] * z[i-1])/ l;
        }
        float b[] = new float[n];
        float c[] = new float[n+1];
        float d[] = new float[n];
        z[n] = 0.0f;
        c[n] = 0.0f;
        for (int j = n -1; j >=0; j--) {
            c[j] = z[j] - mu[j] * c[j+1];
            b[j] = (y[j + 1] - y[j])/h[j] - h[j] * (c[j+1] + 2.0f*c[j])/3.0f;
            d[j] = (c[j+1] - c[j]) / (3.0f* h[j]);
        }
        polynominals = new float[n+2][4];
        polynominals[0][0] = y[0];
        polynominals[0][1] = b[0];
        polynominals[0][2] = 0.0f;
        polynominals[0][3] = 0.0f;
        for ( int i = 0; i< n;i++){
            polynominals[i+1][0] = y[i];
            polynominals[i+1][1] = b[i];
            polynominals[i+1][2] = c[i];
            polynominals[i+1][3] = d[i];
        }
        float x1 = this.x[n] - this.x[n-1];
        float x2 = x1*x1;
        polynominals[n+1][0] = y[n] ;
        polynominals[n+1][1] = polynominals[n][1] + 2* polynominals[n][2]*x1 + 3*polynominals[n][3]*x2;
        polynominals[n+1][2] = 0.0f;
        polynominals[n+1][3] = 0.0f;
    }

    public float calc(float x){
/*        int i = 1;
        int in;
        if ( x < this.x[0])
            in = 0;
        else {
            while (x > this.x[i] && i < this.x.length - 1 ) i = i + 1;
            in = i;
        }
        float x1 = x - this.x[i-1];
         */
        int in = geti(x);
        float x1 = x - this.x[(in == 0)?0:in-1];
        float x2 = x1*x1;
        float x3 = x2*x1;
        return polynominals[in][0] + polynominals[in][1]*x1 +polynominals[in][2]*x2 + polynominals[in][3]*x3;
    }

    public float calcSlope(float x){
        int in = geti(x);
        float x1 = x - this.x[(in == 0)?0:in-1];
        float x2 = x1*x1;
        return polynominals[in][1] + 2* polynominals[in][2]*x1 + 3*polynominals[in][3]*x2;
    }

    public float calcCurve(float x){
        int in = geti(x);
        float x1 = x - this.x[(in == 0)?0:in-1];
        return 2*polynominals[in][2] + 6*polynominals[in][3]*x1;
    }

    private int geti(float x){
        int i;
        if ( x < this.x[0])
            i = 0;
        else {
            i = 1;
            while (x > this.x[i] && i < this.x.length - 1 ) i = i + 1;
        }
        return i;
    }

    /* derive a Spline by cutting out a piece between min and max of an existing
    reference splin and continue as linear function. Used as heuristic */
    public CubicSplineFloat(CubicSplineFloat cubicSpline, float min, float max){
        int minInd = cubicSpline.geti(min);
        int maxInd = cubicSpline.geti(max);
        int n = maxInd - minInd + 1;
        polynominals = new float[n+2][4];
        x = new float[n+1];
        /*first linear section*/
        polynominals[0][0] = cubicSpline.calc(min);
        polynominals[0][1] = cubicSpline.calcSlope(min);
        polynominals[0][2] = 0f;
        polynominals[0][3] = 0f;
        x[0] = min;
        /* translation to new point min */
        polynominals[1][0] = cubicSpline.calc(min);
        polynominals[1][1] = cubicSpline.calcSlope(min);
        polynominals[1][2] = cubicSpline.calcCurve(min);
        polynominals[1][3] = cubicSpline.polynominals[minInd][4];
        /* using existing */
        for ( int i = 2; i<n+1;i++){
           x[i-1] = cubicSpline.x[minInd+i-1];
           for ( int j = 0; j<4;j++){
               polynominals[i][j] = cubicSpline.polynominals[minInd+i+1][j];
           }
        }
        /* last linear section*/
        x[n] = max;
        polynominals[n+1][0] = cubicSpline.calc(max);
        polynominals[n+1][1] = cubicSpline.calcSlope(max);
        polynominals[n+1][2] = 0f;
        polynominals[n+1][3] = 0f;
    };

}
