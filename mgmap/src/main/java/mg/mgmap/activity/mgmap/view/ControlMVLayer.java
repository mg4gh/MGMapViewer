/*
 * Copyright 2017 - 2022 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.activity.mgmap.view;

import android.os.Handler;

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;

public abstract class ControlMVLayer<T> extends MVLayer {

    private float dragX;
    private float dragY;

    /** 0 - no drag origin found
     *  1 - drag origin found
     *  2 - drag aborted          */
    private int dragOriginFound = 0;
    private T dragObject = null;

    private Handler timer = null;
    private final Runnable ttDrag = () -> abortDrag(dragX, dragY);
    protected final long timeoutDrag = 1000;

    public ControlMVLayer(){
        reset();
    }

    public ControlMVLayer(Handler timer){
        this.timer = timer;
        reset();
    }

    private void reset(){
        dragX = Float.MIN_VALUE;
        dragY = Float.MIN_VALUE;
        dragOriginFound = 0;
        dragObject = null;
    }


    @Override
    public boolean onScroll(float scrollX1, float scrollY1, float scrollX2, float scrollY2) {
        if (topLeftPoint == null) return false; // not yet initialized, cannot check for scroll
        if (!checkDragXY(scrollX1,scrollY1)){
            reset();
            setDragXY(scrollX1,scrollY1);
            if (checkDrag(scrollX1, scrollY1)){
                dragOriginFound = 1;
            }
        } else {
            if (dragOriginFound == 2){
                return true;
            }
        }

        if (dragOriginFound == 1){
            handleDrag(scrollX1, scrollY1, scrollX2, scrollY2);
            if (timer != null){
                timer.removeCallbacks(ttDrag);
                timer.postDelayed(ttDrag,timeoutDrag);
            }
            return true;
        }

        return false;
    }



    private boolean checkDragXY(float scrollX, float scrollY){
        return ((dragX == scrollX) && (dragY == scrollY));
    }
    private void setDragXY(float scrollX, float scrollY){
        dragX = scrollX;
        dragY = scrollY;
    }

    public void setDragObject(T object){
        dragObject = object;
    }
    public T getDragObject(){
        return dragObject;
    }


    protected boolean checkDrag(float scrollX, float scrollY){
        PointModel pmStartScroll = new WriteablePointModelImpl(y2lat(scrollY), x2lon(scrollX));
        return checkDrag(pmStartScroll);
    }
    protected boolean checkDrag(PointModel pmStart){
        return false;
    }

    protected void handleDrag(float scrollX1, float scrollY1, float scrollX2, float scrollY2){
        WriteablePointModel pmCurrent = new WriteablePointModelImpl(y2lat(scrollY2), x2lon(scrollX2));
        handleDrag(pmCurrent);
    }

    protected void handleDrag(WriteablePointModel pmCurrent){}

    protected void abortDrag(float scrollX, float scrollY){
//        reset();
        dragOriginFound = 2;
    }
}
