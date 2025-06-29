package mg.mgmap.generic.graph.implbb;

import java.nio.ByteBuffer;
import java.util.Locale;

public abstract class BRedBlackEntries extends BBufferedEntries{

    final static int RED_BLACK_SIZE =
            1       // color: 0 - black, 1 - red
            + 4     // parent idx
            + 4     // left child idx
            + 4;     // right child idx

    final static int RB_COLOR_OFFSET = 0;
    final static int RB_PARENT_OFFSET = 1;
    final static int RB_LEFT_OFFSET = 5;
    final static int RB_RIGHT_OFFSET = 9;

    final static byte RB_COLOR_BLACK = 0;
    final static byte RB_COLOR_RED = 1;


    public final static int NIL = -1;

    final private int entrySize;
    int rootIdx = NIL;

    public BRedBlackEntries(int entrySize, int entriesInBuffer){
        super(entrySize+RED_BLACK_SIZE, entriesInBuffer);
        this.entrySize = entrySize;
    }



    byte getColor(int node){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+entrySize+RB_COLOR_OFFSET);
        return bb.get();
    }
    void setColor(int node, byte color){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+entrySize+RB_COLOR_OFFSET);
        bb.put(color);
    }

    int getParent(int node){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+entrySize+RB_PARENT_OFFSET);
        return bb.getInt();
    }
    void setParent(int node, int parent){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+entrySize+RB_PARENT_OFFSET);
        bb.putInt(parent);
    }

    int getLeftChild(int node){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+entrySize+RB_LEFT_OFFSET);
        return bb.getInt();
    }
    void setLeftChild(int node, int leftChild){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+entrySize+RB_LEFT_OFFSET);
        bb.putInt(leftChild);
    }

    int getRightChild(int node){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+entrySize+RB_RIGHT_OFFSET);
        return bb.getInt();
    }
    void setRightChild(int node, int rightChild){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+entrySize+RB_RIGHT_OFFSET);
        bb.putInt(rightChild);
    }
    String toString(int node){
        return String.format(Locale.ENGLISH, "node="+node+" parent="+getParent(node)+" left="+getLeftChild(node)+" right="+getRightChild(node)+" color="+(getColor(node)==RB_COLOR_BLACK?"BLACK":"RED"));
    }


    int getUncle(int parent) {
        int grandparent = getParent(parent);
        int grandparentLeft = getLeftChild(grandparent);
        int grandparentRight = getRightChild(grandparent);
        if (grandparentLeft == parent) {
            return grandparentRight;
        } else if (grandparentRight == parent) {
            return grandparentLeft;
        } else {
            assert (false):("Parent is not a child of its grandparent");
        }
        return NIL;
    }

    public int getFirstNode(){
        int res = NIL;
        int left = rootIdx;
        while (left != NIL){
            res = left;
            left = getLeftChild(left);
        }
        return res;
    }

    public int getNextNode(int node){
        int res = NIL;
        int right = getRightChild(node);
        if (right != NIL){
            int left = right;
            while (left != NIL){
                res = left;
                left = getLeftChild(left);
            }
        } else {
            while (true){
                int parent = getParent(node);
                if (parent == NIL) break; // root reached
                int parentLeft = getLeftChild(parent);
                if ( parentLeft == node){
                    res = parent;
                    break;
                }
                node = parent;
            }
        }
        return res;
    }



    protected void insert(int idx){
        if (rootIdx == NIL){
            rootIdx = idx;
            setNode(idx, NIL);
        } else {
            insert(idx, rootIdx);
        }
    }
    protected void insert(int idx, int treeIdx){
        int cRes = compareTo(idx, treeIdx);
        assert cRes != 0;
        ByteBuffer bb = getBuf4Idx(treeIdx);
        int pos = bb.position() + entrySize + ((cRes < 0)?RB_LEFT_OFFSET:RB_RIGHT_OFFSET);
        bb.position(pos);
        int subTreeIdx = bb.getInt();
        if (subTreeIdx == NIL){
            bb.position(pos);
            bb.putInt(idx); // set child in parent
            setNode(idx, treeIdx);
        } else {
            insert(idx, subTreeIdx);
        }
    }

    protected void setNode(int node, int parent) {
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+entrySize);
        bb.put( RB_COLOR_RED );
        bb.putInt(parent);
        bb.putInt(NIL); // left child
        bb.putInt(NIL); // right child

        recolor(node);
    }

    protected void recolor(int node){
        int parent = getParent(node);

        // Case 1: Parent is null, we've reached the root, the end of the recursion
        if (parent == NIL) {
            // Uncomment the following line if you want to enforce black roots (rule 2):
            setColor(node, RB_COLOR_BLACK);
            return;
        }

        // Parent is black --> nothing to do
        if (getColor(parent) == RB_COLOR_BLACK) {
            return;
        }

        // From here on, parent is red
        int grandparent = getParent(parent);

        // Case 2:
        // Not having a grandparent means that parent is the root. If we enforce black roots
        // (rule 2), grandparent will never be null, and the following if-then block can be
        // removed.
        if (grandparent == NIL) {
            // As this method is only called on red nodes (either on newly inserted ones - or -
            // recursively on red grandparents), all we have to do is to recolor the root black.
            setColor(parent, RB_COLOR_BLACK);
            return;
        }

        // Get the uncle (may be null/nil, in which case its color is BLACK)
        int uncle = getUncle(parent);

        // Case 3: Uncle is red -> recolor parent, grandparent and uncle
        if ((uncle != NIL) && (getColor(uncle) == RB_COLOR_RED)) {
            setColor(parent, RB_COLOR_BLACK);
            setColor(grandparent, RB_COLOR_RED);
            setColor(uncle, RB_COLOR_BLACK);

            // Call recursively for grandparent, which is now red.
            // It might be root or have a red parent, in which case we need to fix more...
            recolor(grandparent);
        }

        // Parent is left child of grandparent
        else if (parent == getLeftChild(grandparent)) {
            // Case 4a: Uncle is black and node is left->right "inner child" of its grandparent
            if (node == getRightChild(parent)) {
                rotateLeft(parent);

                // Let "parent" point to the new root node of the rotated sub-tree.
                // It will be recolored in the next step, which we're going to fall-through to.
                parent = node;
            }

            // Case 5a: Uncle is black and node is left->left "outer child" of its grandparent
            rotateRight(grandparent);

            // Recolor original parent and grandparent
            setColor(parent, RB_COLOR_BLACK);
            setColor(grandparent, RB_COLOR_RED);
        }

        // Parent is right child of grandparent
        else {
            // Case 4b: Uncle is black and node is right->left "inner child" of its grandparent
            if (node == getLeftChild(parent)) {
                rotateRight(parent);

                // Let "parent" point to the new root node of the rotated sub-tree.
                // It will be recolored in the next step, which we're going to fall-through to.
                parent = node;
            }

            // Case 5b: Uncle is black and node is right->right "outer child" of its grandparent
            rotateLeft(grandparent);

            // Recolor original parent and grandparent
            setColor(parent, RB_COLOR_BLACK);
            setColor(grandparent, RB_COLOR_RED);
        }

    }



    private void replaceParentsChild(int parent, int oldChild, int newChild) {
        if (parent == NIL) {
            rootIdx = newChild;
        } else {
            ByteBuffer bb  = getBuf4Idx(parent);
            int posRB = bb.position()+entrySize;
            bb.position( posRB + RB_LEFT_OFFSET );
            int left = bb.getInt();
            int right = bb.getInt();
            if (left == oldChild){
                bb.position( posRB + RB_LEFT_OFFSET );
            } else if (right == oldChild){
                bb.position( posRB + RB_RIGHT_OFFSET );
            } else {
                assert false:"illegal usage: parent="+parent+" oldChild="+oldChild+" newChild="+newChild;
            }
            bb.putInt(newChild);
        }
        if (newChild != NIL) {
            setParent(newChild, parent);
        }
    }

    private void rotateRight(int node) {
        int parent = getParent(node);
        int leftChild = getLeftChild(node);
        int leftRightChild = getRightChild(leftChild);
        if (leftRightChild != NIL){
            setParent(leftRightChild, node);
        }
        setParent(node, leftChild); // set node.parent = leftChild
        setLeftChild(node, leftRightChild); // set node.left = leftChild.right
        setRightChild(leftChild, node); // leftChild.right = node;

        replaceParentsChild(parent, node, leftChild);
    }

    private void rotateLeft(int node) {
        int parent = getParent(node);
        int rightChild = getRightChild(node);
        int rightLeftChild = getLeftChild(rightChild);
        if (rightLeftChild != NIL){
            setParent(rightLeftChild, node);
        }
        setParent(node, rightChild); // set node.parent = rightChild
        setRightChild(node, rightLeftChild); // set node.right = rightChild.left
        setLeftChild(rightChild, node); // rightChild.left = node;

        replaceParentsChild(parent, node, rightChild);
    }


    abstract int compareTo(int idx1, int idx2);

}
