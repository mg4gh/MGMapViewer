package mg.mgmap.activity.mgmap.features.routing.profile;

import java.lang.invoke.MethodHandles;

import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineProfileMTB extends CostCalcSplineProfile {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private final CubicSpline[] SurfaceCatSpline = new CubicSpline[7];
    protected CostCalcSplineProfileMTB() {
        super((short) 1);
        SurfaceCatSpline[1] = super.getProfileSpline();
    }

    protected CubicSpline getRefSpline(Object context) {
        try {
            return calcSpline((short) context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CubicSpline calcSpline(short surfaceLevel) throws Exception {
        float watt0 = 160f ;
        float watt = 160f;
        float ACw = 0.45f;
        float[] fdown = {3f,3f,3.5f,3.5f,3.8f,5f,5f};
        float[] highdowndoffset = {0.11f,0.11f,0.11f,0.11f,0.11f,0.08f,0.05f};
        float m = 90f;
        float[] cr =  {0.005f,0.0055f,0.007f,0.01f,0.02f,0.075f,0.15f};
        float[] f1u =  {1.15f,1.15f,1.15f,1.2f,1.2f,1.6f,1.7f};
        float[] f2u =  {3f,3f,3f,3f,3.6f,3.3f,3.5f};
        float[] relSlope;
        float[] slopes;
        float[] durations;
        if (surfaceLevel <= 4) {
            slopes = new float[] { -2f,-0.4f,-0.2f, -0.02f, 0.0f, 0.1f, 0.3f,2f};
            relSlope = new float[]{1.6f,1.4f,1.2f,1.2f,1.5f};
            durations = new float[slopes.length];
            durations[4] = 1f / getFrictionBasedVelocity(0.0f, watt0, cr[surfaceLevel], ACw, m);
            durations[3] = durations[4] + slopes[3]*relSlope[surfaceLevel];
        } else {
            slopes = new float[]{ -2f,-0.4f,-0.2f, 0.0f, 0.1f, 0.3f,2f};
            durations = new float[slopes.length];
            durations[3] = 1f / getFrictionBasedVelocity(0.0, watt0, cr[surfaceLevel], ACw, m);
        }
        durations[0] = -(slopes[0]+highdowndoffset[surfaceLevel])*12f;
        durations[1] = -(slopes[1]+highdowndoffset[surfaceLevel])*fdown[surfaceLevel];
        durations[2] = -(slopes[2]+highdowndoffset[surfaceLevel])*fdown[surfaceLevel];
        durations[slopes.length-3] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-3], watt, cr[surfaceLevel], ACw, m)  ;
        durations[slopes.length-2] = f1u[surfaceLevel] /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, cr[surfaceLevel], ACw, m)  ;
        durations[slopes.length-1] = f2u[surfaceLevel] /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, cr[surfaceLevel], ACw, m)  ;
        return getCheckSpline(slopes, durations,surfaceLevel);
    }

    protected CubicSpline getSpline(short surfaceLevel){
        CubicSpline cubicSpline = SurfaceCatSpline[surfaceLevel];
        if (cubicSpline == null) {
            try {
                cubicSpline = calcSpline(surfaceLevel);
                SurfaceCatSpline[surfaceLevel] = cubicSpline;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return cubicSpline;
    }
}
