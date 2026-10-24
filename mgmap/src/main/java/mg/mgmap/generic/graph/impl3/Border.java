package mg.mgmap.generic.graph.impl3;

public class Border {

    public static final byte BORDER_NODE_WEST  = 0x08;
    public static final byte BORDER_NODE_NORTH = 0x04;
    public static final byte BORDER_NODE_EAST  = 0x02;
    public static final byte BORDER_NODE_SOUTH = 0x01;

    public static int deltaX(byte border){
        return switch (border) {
            case BORDER_NODE_WEST -> -1;
            case BORDER_NODE_EAST -> 1;
            default -> 0;
        };
    }
    public static int deltaY(byte border){
        return switch (border) {
            case BORDER_NODE_NORTH -> -1;
            case BORDER_NODE_SOUTH -> 1;
            default -> 0;
        };
    }
    public static byte oppositeBorder(byte border){
        return switch (border) {
            case BORDER_NODE_WEST -> BORDER_NODE_EAST;
            case BORDER_NODE_EAST -> BORDER_NODE_WEST;
            case BORDER_NODE_NORTH -> BORDER_NODE_SOUTH;
            case BORDER_NODE_SOUTH -> BORDER_NODE_NORTH;
            default -> 0;
        };
    }


}
