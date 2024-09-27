package mg.mgmap.activity.mgmap.features.routing.profile;

/**
 * computes natural cubic spline. Algorithm: https://en.wikipedia.org/wiki/Spline_(mathematics)
 */
public class CubicSplineDouble {

    private final double[][] polynominals;
    private final double[] x;
    public CubicSplineDouble(double x[], double y[]) throws Exception {
        if  ( x.length < 3 ) {
            throw new Exception("input array too short");
        } else if ( x.length != y.length ) {
            throw new Exception("x and y vector size don't match");
        }
        double min = - Double.MAX_VALUE;
        for ( int i = 0; i<x.length; i++){
            if (x[i] <= min) throw new Exception("x not sorted in ascending order");
            min = x[i];
        }
        this.x = x;
        int n = x.length - 1;

        double h[] = new double[n];
        for (int i = 0; i < n; i++) {
            h[i] = x[i+1] - x[i];
        }
        double mu[] = new double[n];
        double z[] = new double[n+1];
        mu[0] = 0.0;
        z[0] = 0.0;
        double l;
        for (int i = 1; i < n; i++) {
            l = 2.0 * (x[i+1] - x[i-1]) - h[i-1] * mu[i-1];
            mu[i] = h[i] / l;
            z[i] = (3.0 * (y[i+1] * h[i-1] - y[i] * (x[i+1] - x[i-1])+ y[i-1] * h[i]) /
                    (h[i-1] * h[i]) - h[i-1] * z[i-1])/ l;
        }
        double b[] = new double[n];
        double c[] = new double[n+1];
        double d[] = new double[n];
        z[n] = 0.0;
        c[n] = 0.0;
        for (int j = n -1; j >=0; j--) {
            c[j] = z[j] - mu[j] * c[j+1];
            b[j] = (y[j + 1] - y[j])/h[j] - h[j] * (c[j+1] + 2.0*c[j])/3.0;
            d[j] = (c[j+1] - c[j]) / (3.0* h[j]);
        }
        polynominals = new double[n+2][4];
        polynominals[0][0] = y[0];
        polynominals[0][1] = b[0];
        polynominals[0][2] = 0.0;
        polynominals[0][3] = 0.0;
        for ( int i = 0; i< n;i++){
            polynominals[i+1][0] = y[i];
            polynominals[i+1][1] = b[i];
            polynominals[i+1][2] = c[i];
            polynominals[i+1][3] = d[i];
        }
        double x1 = this.x[n] - this.x[n-1];
        double x2 = x1*x1;
        polynominals[n+1][0] = y[n] ;
        polynominals[n+1][1] = polynominals[n][1] + 2* polynominals[n][2]*x1 + 3*polynominals[n][3]*x2;
        polynominals[n+1][2] = 0.0;
        polynominals[n+1][3] = 0.0;
    }

    public double calc(double x){
        int i = 1;
        int in;
        if ( x < this.x[0])
            in = 0;
        else {
            while (x > this.x[i] && i < this.x.length - 1 ) i = i + 1;
            in = i;
        }
        double x1 = x - this.x[i-1];
        double x2 = x1*x1;
        double x3 = x2*x1;
        return polynominals[in][0] + polynominals[in][1]*x1 +polynominals[in][2]*x2 + polynominals[in][3]*x3;
    }

    public double calcSlope(double x){
        int i = 1;
        int in;
        if ( x < this.x[0])
            in = 0;
        else {
            while (x > this.x[i] && i < this.x.length - 1 ) i = i + 1;
            in = i;
        }
        double x1 = x - this.x[i-1];
        double x2 = x1*x1;
        return polynominals[in][1] + 2* polynominals[in][2]*x1 + 3*polynominals[in][3]*x2;
    }

    public double calcCurve(double x){
        int i = 1;
        int in;
        if ( x < this.x[0])
            in = 0;
        else {
            while (x > this.x[i] && i < this.x.length - 1 ) i = i + 1;
            in = i;
        }
        double x1 = x - this.x[i-1];
       return 2*polynominals[in][2] + 6*polynominals[in][3]*x1;
    }

}
