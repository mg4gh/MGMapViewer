package mg.mgmap.activity.mgmap.features.routing.profile;

import java.lang.invoke.MethodHandles;

import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineProfileTreckingBike extends CostCalcSplineProfile {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private final CubicSpline[] SurfaceCatSpline = new CubicSpline[7];
    protected CostCalcSplineProfileTreckingBike() {
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
        float watt0 = 90.0f ;
        float watt = 130.0f;
        float ACw = 0.45f;
        float fdown = 8.5f;
        float m = 90f;
        float [] cr = new float[] {0.0035f,0.005f,0.0076f,0.015f,0.04f,0.075f,0.13f};
        float [] highdowndoffset = new float[] {0.15f,0.143f,0.13f,0.11f,0.1f,0.08f,-0.03f};
        float[] relSlope;
        float[] slopes;
        float[] durations;
        if (surfaceLevel <= 3) {
            slopes = new float[]{ -0.6f,-0.4f,-0.2f, -0.02f, 0.0f, 0.08f, 0.2f, 0.4f};
            relSlope = new float[]{2.2f,2.3f,1.15f,1.0f};
            durations = new float[slopes.length];
            durations[4] = 1f / getFrictionBasedVelocity(0.0f, watt0, cr[surfaceLevel], ACw, m);
            durations[3] = durations[4] + slopes[3]*relSlope[surfaceLevel];
        } else {
            slopes = new float[]{-0.6f,-0.4f,-0.2f, 0.0f, 0.08f,0.2f, 0.4f};
            durations = new float[slopes.length];
            durations[3] = 1 / getFrictionBasedVelocity(0.0, watt0, cr[surfaceLevel], ACw, m);
        }
        durations[0] = -(slopes[0]+highdowndoffset[surfaceLevel])*fdown*1.5f;
        durations[1] = -(slopes[1]+highdowndoffset[surfaceLevel])*fdown;
        durations[2] = -(slopes[2]+highdowndoffset[surfaceLevel])*fdown;
        durations[slopes.length-3] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-3], watt, cr[surfaceLevel], ACw, m)  ;
        durations[slopes.length-2] = 1.5f /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, cr[surfaceLevel], ACw, m)  ;
        durations[slopes.length-1] = 3.0f /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, cr[surfaceLevel], ACw, m)  ;
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

