/*
 * Copyright 2013, Edmodo, Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file, or at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License. 
 */

package com.smartmux.photocutter.cropper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.smartmux.photocutter.cropper.cropwindow.handle.Handle;
import com.smartmux.photocutter.cropper.util.AspectRatioUtil;

/**
 * A custom View representing the crop window and the shaded background outside the crop window.
 */
@SuppressLint("NewApi") public class CropOverlayView extends View {

    //region: Fields and Consts

    /**
     * The Paint used to draw the white rectangle around the crop area.
     */
    private Paint mBorderPaint;

    /**
     * The Paint used to draw the corners of the Border
     */
    private Paint mBorderCornerPaint;

    /**
     * The Paint used to draw the guidelines within the crop area when pressed.
     */
    private Paint mGuidelinePaint;

    /**
     * The Paint used to darken the surrounding areas outside the crop area.
     */
    private Paint mBackgroundPaint;

    /**
     * The bounding box around the Bitmap that we are cropping.
     */
    private Rect mBitmapRect;

    /**
     * The offset to draw the border corener from the border
     */
    private float mBorderCornerOffset;

    /**
     * the length of the border corner to draw
     */
    private float mBorderCornerLength;

    /**
     * The radius of the touch zone (in pixels) around a given Handle.
     */
    private float mHandleRadius;

    /**
     * An edge of the crop window will snap to the corresponding edge of a specified bounding box
     * when the crop window edge is less than or equal to this distance (in pixels) away from the bounding box edge.
     */
    private float mSnapRadius;

    /**
     * Holds the x and y offset between the exact touch location and the exact handle location that is activated.
     * There may be an offset because we allow for some leeway (specified by mHandleRadius) in activating a handle.
     * However, we want to maintain these offset values while the handle is being dragged so that the handle
     * doesn't jump.
     */
    private Pair<Float, Float> mTouchOffset;

    /**
     * The Handle that is currently pressed; null if no Handle is pressed.
     */
    private Handle mPressedHandle;

    /**
     * Flag indicating if the crop area should always be a certain aspect ratio (indicated by mTargetAspectRatio).
     */
    private boolean mFixAspectRatio = Defaults.DEFAULT_FIXED_ASPECT_RATIO;

    /**
     * Floats to save the current aspect ratio of the image
     */
    private int mAspectRatioX = Defaults.DEFAULT_ASPECT_RATIO_X;

    private int mAspectRatioY = Defaults.DEFAULT_ASPECT_RATIO_Y;

    /**
     * The aspect ratio that the crop area should maintain;
     * this variable is only used when mMaintainAspectRatio is true.
     */
    private float mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;

    /**
     * Instance variables for customizable attributes
     */
    private int mGuidelines = Defaults.DEFAULT_GUIDELINES;

    /**
     * The shape of the cropping area - rectangle/circular.
     */
    private CropImageView.CropShape mCropShape;

    /**
     * Whether the Crop View has been initialized for the first time
     */
    private boolean initializedCropWindow = false;

    /**
     * Used to set back LayerType after changing to software.
     */
    private Integer mOriginalLayerType;
    
//    boolean hexa = false;
    //endregion

    public CropOverlayView(Context context) {
        this(context, null);
    }

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Informs the CropOverlayView of the image's position relative to the
     * ImageView. This is necessary to call in order to draw the crop window.
     *
     * @param bitmapRect the image's bounding box
     */
    public void setBitmapRect(Rect bitmapRect) {
        mBitmapRect = bitmapRect;
        initCropWindow(mBitmapRect);
    }

    /**
     * Resets the crop overlay view.
     */
    public void resetCropOverlayView() {

        if (initializedCropWindow) {
            initCropWindow(mBitmapRect);
            invalidate();
        }
    }

    /**
     * The shape of the cropping area - rectangle/circular.
     */
    public CropImageView.CropShape getCropShape() {
        return mCropShape;
    }

    /**
     * The shape of the cropping area - rectangle/circular.
     */
   public void setCropShape(CropImageView.CropShape cropShape) {
        if (mCropShape != cropShape) {
            mCropShape = cropShape;
            if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT <= 17) {
                if (mCropShape == CropImageView.CropShape.OVAL) {
                    mOriginalLayerType = getLayerType();
                    if (mOriginalLayerType != View.LAYER_TYPE_SOFTWARE) {
                        // TURN off hardware acceleration
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                    } else {
                        mOriginalLayerType = null;
                    }
                } else if (mOriginalLayerType != null) {
                    // return hardware acceleration back
                    setLayerType(mOriginalLayerType, null);
                    mOriginalLayerType = null;
                }
            }
            invalidate();
        }
    }
   
   /**
    * for hexagon shape
    * @param hexaFrame
    */
//   public void setHexaFrame(boolean hexaFrame){
//	   hexa = hexaFrame;
//	   invalidate();
//   }

    /**
     * Sets the guidelines for the CropOverlayView to be either on, off, or to
     * show when resizing the application.
     *
     * @param guidelines Integer that signals whether the guidelines should be
     * on, off, or only showing when resizing.
     */
    public void setGuidelines(int guidelines) {
        if (guidelines < 0 || guidelines > 2)
            throw new IllegalArgumentException("Guideline value must be set between 0 and 2. See documentation.");
        else {
            mGuidelines = guidelines;
            if (initializedCropWindow) {
                initCropWindow(mBitmapRect);
                invalidate();
            }
        }
    }

    /**
     * Sets whether the aspect ratio is fixed or not; true fixes the aspect
     * ratio, while false allows it to be changed.
     *
     * @param fixAspectRatio Boolean that signals whether the aspect ratio
     * should be maintained.
     */
    public void setFixedAspectRatio(boolean fixAspectRatio) {
        mFixAspectRatio = fixAspectRatio;

        if (initializedCropWindow) {
            initCropWindow(mBitmapRect);
            invalidate();
        }
    }

    /**
     * Sets the X value of the aspect ratio; is defaulted to 1.
     *
     * @param aspectRatioX int that specifies the new X value of the aspect
     * ratio
     */
    public void setAspectRatioX(int aspectRatioX) {
        if (aspectRatioX <= 0)
            throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0.");
        else {
            mAspectRatioX = aspectRatioX;
            mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;

            if (initializedCropWindow) {
                initCropWindow(mBitmapRect);
                invalidate();
            }
        }
    }

    /**
     * Sets the Y value of the aspect ratio; is defaulted to 1.
     *
     * @param aspectRatioY int that specifies the new Y value of the aspect
     * ratio
     */
    public void setAspectRatioY(int aspectRatioY) {
        if (aspectRatioY <= 0)
            throw new IllegalArgumentException("Cannot set aspect ratio value to a number less than or equal to 0.");
        else {
            mAspectRatioY = aspectRatioY;
            mTargetAspectRatio = ((float) mAspectRatioX) / mAspectRatioY;

            if (initializedCropWindow) {
                initCropWindow(mBitmapRect);
                invalidate();
            }
        }
    }

    /**
     * An edge of the crop window will snap to the corresponding edge of a
     * specified bounding box when the crop window edge is less than or equal to
     * this distance (in pixels) away from the bounding box edge. (default: 3)
     */
    public void setSnapRadius(float snapRadius) {
        mSnapRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, snapRadius, getResources().getDisplayMetrics());
    }

    /**
     * Sets all initial values, but does not call initCropWindow to reset the
     * views. Used once at the very start to initialize the attributes.
     *
     * @param cropShape
     * @param snapRadius
     * @param guidelines Integer that signals whether the guidelines should be
     * on, off, or only showing when resizing.
     * @param fixAspectRatio Boolean that signals whether the aspect ratio
     * should be maintained.
     * @param aspectRatioX float that specifies the new X value of the aspect
     * ratio
     * @param aspectRatioY float that specifies the new Y value of the aspect
     * @param guidelinesThickness
     * @param guidelinesColor
     */
    public void setInitialAttributeValues(CropImageView.CropShape cropShape,
                                          float snapRadius,
                                          int guidelines,
                                          boolean fixAspectRatio,
                                          int aspectRatioX,
                                          int aspectRatioY,
                                          float borderLineThickness,
                                          int borderLineColor,
                                          float borderCornerThickness,
                                          float borderCornerOffset,
                                          float borderCornerLength,
                                          int borderCornerColor,
                                          float guidelinesThickness,
                                          int guidelinesColor,
                                          int backgroundColor) {

        DisplayMetrics dm = getResources().getDisplayMetrics();

        setCropShape(cropShape);

        setSnapRadius(snapRadius);

        setGuidelines(guidelines);

        setFixedAspectRatio(fixAspectRatio);

        setAspectRatioX(aspectRatioX);

        setAspectRatioY(aspectRatioY);

        if (borderLineThickness < 0) {
            throw new IllegalArgumentException("Cannot set line thickness value to a number less than 0.");
        }
        mBorderPaint = HandleUtil.getNewPaintOrNull(dm, borderLineThickness, borderLineColor);

        if (borderCornerThickness < 0) {
            throw new IllegalArgumentException("Cannot set corner thickness value to a number less than 0.");
        }
        mBorderCornerOffset = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, borderCornerOffset, dm);
        mBorderCornerLength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, borderCornerLength, dm);
        mBorderCornerPaint = HandleUtil.getNewPaintOrNull(dm, borderCornerThickness, borderCornerColor);

        if (guidelinesThickness < 0) {
            throw new IllegalArgumentException("Cannot set guidelines thickness value to a number less than 0.");
        }
        mGuidelinePaint = HandleUtil.getNewPaintOrNull(dm, guidelinesThickness, guidelinesColor);

        mBackgroundPaint = HandleUtil.getNewPaint(backgroundColor);

        mHandleRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, Defaults.TARGET_RADIUS, dm);
    }

    /**
     * Indicates whether the crop window is small enough that the guidelines
     * should be shown. Public because this function is also used to determine
     * if the center handle should be focused.
     *
     * @return boolean Whether the guidelines should be shown or not
     */
    public static boolean showGuidelines() {
        if ((Math.abs(Edge.LEFT.getCoordinate() - Edge.RIGHT.getCoordinate()) < Defaults.DEFAULT_SHOW_GUIDELINES_LIMIT)
                || (Math.abs(Edge.TOP.getCoordinate() - Edge.BOTTOM.getCoordinate()) < Defaults.DEFAULT_SHOW_GUIDELINES_LIMIT)) {
            return false;
        } else {
            return true;
        }
    }

    //region: Private methods

    /**
     * Set the initial crop window size and position. This is dependent on the
     * size and position of the image being cropped.
     *
     * @param bitmapRect the bounding box around the image being cropped
     */
    private void initCropWindow(Rect bitmapRect) {

        if (bitmapRect.width() == 0 || bitmapRect.height() == 0) {
            return;
        }

        // Tells the attribute functions the crop window has already been initialized
        initializedCropWindow = true;

        if (mFixAspectRatio
                && (bitmapRect.left != 0 || bitmapRect.right != 0
                || bitmapRect.top != 0 || bitmapRect.bottom != 0)) {

            // If the image aspect ratio is wider than the crop aspect ratio,
            // then the image height is the determining initial length. Else, vice-versa.
            float bitmapAspectRatio = (float) bitmapRect.width() / (float) bitmapRect.height();
            if (bitmapAspectRatio > mTargetAspectRatio) {

                Edge.TOP.setCoordinate(bitmapRect.top);
                Edge.BOTTOM.setCoordinate(bitmapRect.bottom);

                float centerX = getWidth() / 2f;

                //dirty fix for wrong crop overlay aspect ratio when using fixed aspect ratio
                mTargetAspectRatio = (float) mAspectRatioX / mAspectRatioY;

                // Limits the aspect ratio to no less than 40 wide or 40 tall
                float cropWidth = Math.max(Edge.MIN_CROP_HORIZONTAL_LENGTH,
                        AspectRatioUtil.calculateWidth(Edge.TOP.getCoordinate(),
                                Edge.BOTTOM.getCoordinate(),
                                mTargetAspectRatio));

                // Create new TargetAspectRatio if the original one does not fit the screen
                if (cropWidth == Edge.MIN_CROP_HORIZONTAL_LENGTH) {
                    mTargetAspectRatio = (Edge.MIN_CROP_HORIZONTAL_LENGTH) / (Edge.BOTTOM.getCoordinate() - Edge.TOP.getCoordinate());
                }

                float halfCropWidth = cropWidth / 2f;
                Edge.LEFT.setCoordinate(centerX - halfCropWidth);
                Edge.RIGHT.setCoordinate(centerX + halfCropWidth);

            } else {

                Edge.LEFT.setCoordinate(bitmapRect.left);
                Edge.RIGHT.setCoordinate(bitmapRect.right);

                float centerY = getHeight() / 2f;

                // Limits the aspect ratio to no less than 40 wide or 40 tall
                float cropHeight = Math.max(Edge.MIN_CROP_VERTICAL_LENGTH,
                        AspectRatioUtil.calculateHeight(Edge.LEFT.getCoordinate(),
                                Edge.RIGHT.getCoordinate(),
                                mTargetAspectRatio));

                // Create new TargetAspectRatio if the original one does not fit the screen
                if (cropHeight == Edge.MIN_CROP_VERTICAL_LENGTH) {
                    mTargetAspectRatio = (Edge.RIGHT.getCoordinate() - Edge.LEFT.getCoordinate()) / Edge.MIN_CROP_VERTICAL_LENGTH;
                }

                float halfCropHeight = cropHeight / 2f;
                Edge.TOP.setCoordinate(centerY - halfCropHeight);
                Edge.BOTTOM.setCoordinate(centerY + halfCropHeight);
            }
        } else {
            // ... do not fix aspect ratio...

            // Initialize crop window to have 10% padding w/ respect to image.
            float horizontalPadding = 0.1f * bitmapRect.width();
            float verticalPadding = 0.1f * bitmapRect.height();

            Edge.LEFT.setCoordinate(bitmapRect.left + horizontalPadding);
            Edge.TOP.setCoordinate(bitmapRect.top + verticalPadding);
            Edge.RIGHT.setCoordinate(bitmapRect.right - horizontalPadding);
            Edge.BOTTOM.setCoordinate(bitmapRect.bottom - verticalPadding);
        }
    }

    /**
     * Initialize the crop window here because we need the size of the view to have been determined.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        initCropWindow(mBitmapRect);
    }

    /**
     * Draw crop overview by drawing background over image not in the cripping area, then borders and guidelines.
     */
    @Override
    protected void onDraw(Canvas canvas) {

        super.onDraw(canvas);

        // Draw translucent background for the cropped area.
        drawBackground(canvas, mBitmapRect);

        if (showGuidelines()) {
            // Determines whether guidelines should be drawn or not
            if (mGuidelines == Defaults.GUIDELINES_ON) {
                drawGuidelines(canvas);
            } else if (mGuidelines == Defaults.GUIDELINES_ON_TOUCH) {
                // Draw only when resizing
                if (mPressedHandle != null) {
                    drawGuidelines(canvas);
                }
            }
        }

        drawBorders(canvas);

        if (mCropShape == CropImageView.CropShape.RECTANGLE
                || mCropShape == CropImageView.CropShape.DIAMOND
                || mCropShape == CropImageView.CropShape.PENTAGON
                || mCropShape == CropImageView.CropShape.HEXAGON
                || mCropShape == CropImageView.CropShape.OCTAGON) {
            drawCorners(canvas);
        }

    }

    /**
     * Draw shadow background over the image not including the crop area.
     */
    private void drawBackground(Canvas canvas, Rect bitmapRect) {

        float l = Edge.LEFT.getCoordinate();
        float t = Edge.TOP.getCoordinate();
        float r = Edge.RIGHT.getCoordinate();
        float b = Edge.BOTTOM.getCoordinate();

        if (mCropShape == CropImageView.CropShape.RECTANGLE
                || mCropShape == CropImageView.CropShape.DIAMOND
                || mCropShape == CropImageView.CropShape.PENTAGON
                || mCropShape == CropImageView.CropShape.HEXAGON
                || mCropShape == CropImageView.CropShape.OCTAGON) {
            canvas.drawRect(bitmapRect.left, bitmapRect.top, bitmapRect.right, t, mBackgroundPaint);
            canvas.drawRect(bitmapRect.left, b, bitmapRect.right, bitmapRect.bottom, mBackgroundPaint);
            canvas.drawRect(bitmapRect.left, t, l, b, mBackgroundPaint);
            canvas.drawRect(r, t, bitmapRect.right, b, mBackgroundPaint);
        }
//        else if (mCropShape == CropImageView.CropShape.HEXAGON) {
//            canvas.drawRect(bitmapRect.left, bitmapRect.top, bitmapRect.right, t, mBackgroundPaint);
//            canvas.drawRect(bitmapRect.left, b, bitmapRect.right, bitmapRect.bottom, mBackgroundPaint);
//            canvas.drawRect(bitmapRect.left, t, l, b, mBackgroundPaint);
//            canvas.drawRect(r, t, bitmapRect.right, b, mBackgroundPaint);
//        }
        else {
            Path circleSelectionPath = new Path();
            if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT <= 17 && mCropShape == CropImageView.CropShape.OVAL) {
                Defaults.EMPTY_RECT_F.set(l + 2, t + 2, r - 2, b - 2);
            } else {
                Defaults.EMPTY_RECT_F.set(l, t, r, b);
            }
            circleSelectionPath.addOval(Defaults.EMPTY_RECT_F, Path.Direction.CW);
            canvas.save();
            canvas.clipPath(circleSelectionPath, Region.Op.XOR);
            canvas.drawRect(bitmapRect.left, bitmapRect.top, bitmapRect.right, bitmapRect.bottom, mBackgroundPaint);
            canvas.restore();
        }
    }

    /**
     * Draw 2 veritcal and 2 horizontal guidelines inside the cropping area to split it into 9 equal parts.
     */
    private void drawGuidelines(Canvas canvas) {
        if (mGuidelinePaint != null) {
            float sw = mBorderPaint != null ? mBorderPaint.getStrokeWidth() : 0;
            float l = Edge.LEFT.getCoordinate() + sw;
            float t = Edge.TOP.getCoordinate() + sw;
            float r = Edge.RIGHT.getCoordinate() - sw;
            float b = Edge.BOTTOM.getCoordinate() - sw;

            float oneThirdCropWidth = Edge.getWidth() / 3;
            float oneThirdCropHeight = Edge.getHeight() / 3;

            if (mCropShape == CropImageView.CropShape.OVAL) {

                float w = Edge.getWidth() / 2 - sw;
                float h = Edge.getHeight() / 2 - sw;

                // Draw vertical guidelines.
                float x1 = l + oneThirdCropWidth;
                float x2 = r - oneThirdCropWidth;
                float yv = (float) (h * Math.sin(Math.acos((w - oneThirdCropWidth) / w)));
                canvas.drawLine(x1, t + h - yv, x1, b - h + yv, mGuidelinePaint);
                canvas.drawLine(x2, t + h - yv, x2, b - h + yv, mGuidelinePaint);

                // Draw horizontal guidelines.
                float y1 = t + oneThirdCropHeight;
                float y2 = b - oneThirdCropHeight;
                float xv = (float) (w * Math.cos(Math.asin((h - oneThirdCropHeight) / h)));
                canvas.drawLine(l + w - xv, y1, r - w + xv, y1, mGuidelinePaint);
                canvas.drawLine(l + w - xv, y2, r - w + xv, y2, mGuidelinePaint);
            } else {

                // Draw vertical guidelines.
                float x1 = l + oneThirdCropWidth;
                float x2 = r - oneThirdCropWidth;
                canvas.drawLine(x1, t, x1, b, mGuidelinePaint);
                canvas.drawLine(x2, t, x2, b, mGuidelinePaint);

                // Draw horizontal guidelines.
                float y1 = t + oneThirdCropHeight;
                float y2 = b - oneThirdCropHeight;
                canvas.drawLine(l, y1, r, y1, mGuidelinePaint);
                canvas.drawLine(l, y2, r, y2, mGuidelinePaint);
            }
        }
    }

    /**
     * Draw borders of the crop area.
     */
    private void drawBorders(Canvas canvas) {
        if (mBorderPaint != null) {
            float w = mBorderPaint.getStrokeWidth();
            float l = Edge.LEFT.getCoordinate() + w / 2;
            float t = Edge.TOP.getCoordinate() + w / 2;
            float r = Edge.RIGHT.getCoordinate() - w / 2;
            float b = Edge.BOTTOM.getCoordinate() - w / 2;
            if (mCropShape == CropImageView.CropShape.RECTANGLE) {
                // Draw rectangle crop window border.
                canvas.drawRect(l, t, r, b, mBorderPaint);
                
            } else  if (mCropShape == CropImageView.CropShape.HEXAGON) {
                canvas.drawRect(l, t, r, b, mBorderPaint);

                    int wt = (int) (r-l);
                    int ht = (int) (b-t);

                    int x1 = (int) (wt/2 + l);
                    int y1 = (int)t;

                    int x2 =(int)l;
                    int y2 =  (int) (ht/4)+y1;

                    int x3 =(int)l;
                    int y3 = (int) (y2+y2+y2 - y1-y1);

                    int x4 = (int) (wt/2 + l);
                    int y4 =  (int) (ht + y1);

                    int x6 = (int) (wt+l);
                    int y6 = (int) (ht/4)+y1;

                    int x5 =(int) (wt+l);
                    int y5 =  (int) (y2+y2+y2 - y1-y1);



                    Point point1_draw = new Point(x1, y1);
                    Point point2_draw = new Point(x2, y2);
                    Point point3_draw = new Point(x3, y3);
                    Point point4_draw = new Point(x4, y4);
                    Point point5_draw = new Point(x5, y5);
                    Point point6_draw = new Point(x6, y6);

                    Path path = new Path();
                    path.moveTo(point1_draw.x, point1_draw.y);
                    path.lineTo(point2_draw.x, point2_draw.y);
                    path.lineTo(point3_draw.x, point3_draw.y);
                    path.lineTo(point4_draw.x, point4_draw.y);
                    path.lineTo(point5_draw.x, point5_draw.y);
                    path.lineTo(point6_draw.x, point6_draw.y);

                    path.close();
                    canvas.drawPath(path, mBorderPaint);

            }
            else  if (mCropShape == CropImageView.CropShape.OCTAGON) {
                canvas.drawRect(l, t, r, b, mBorderPaint);

                int wt = (int) (r-l);
                int ht = (int) (b-t);

                int x1 = (int) (wt/4 + l);
                int y1 = (int)t;

                int x2 =(int)l;
                int y2 =  (int) (ht/4)+y1;

                int x3 =(int)l;
                int y3 = (int) (y2+y2+y2 - y1-y1);

                int x4 = (int) (wt/4 + l);
                int y4 =  (int) (ht + y1);

                int x5 = (int) (x1+x1+x1-l-l);
                int y5 =  (int) (ht + y1);

                int x6 =(int) (wt+l);
                int y6 =  (int) (y2+y2+y2 - y1-y1);

                int x7 = (int) (wt+l);
                int y7 = (int) (ht/4)+y1;

                int x8 = (int) (x1+x1+x1-l-l);
                int y8 = (int)t;



                Point point1_draw = new Point(x1, y1);
                Point point2_draw = new Point(x2, y2);
                Point point3_draw = new Point(x3, y3);
                Point point4_draw = new Point(x4, y4);
                Point point5_draw = new Point(x5, y5);
                Point point6_draw = new Point(x6, y6);
                Point point7_draw = new Point(x7, y7);
                Point point8_draw = new Point(x8, y8);

                Path path = new Path();
                path.moveTo(point1_draw.x, point1_draw.y);
                path.lineTo(point2_draw.x, point2_draw.y);
                path.lineTo(point3_draw.x, point3_draw.y);
                path.lineTo(point4_draw.x, point4_draw.y);
                path.lineTo(point5_draw.x, point5_draw.y);
                path.lineTo(point6_draw.x, point6_draw.y);
                path.lineTo(point7_draw.x, point7_draw.y);
                path.lineTo(point8_draw.x, point8_draw.y);

                path.close();
                canvas.drawPath(path, mBorderPaint);

            }
            else  if (mCropShape == CropImageView.CropShape.PENTAGON) {
                canvas.drawRect(l, t, r, b, mBorderPaint);

                int wt = (int) (r-l);
                int ht = (int) (b-t);

                int x1 = (int) (wt/2 + l);
                int y1 = (int)t;

                int x2 =(int)l;
                int y2 =  (int) (ht/3)+y1;

                int x3 =(int) (wt/5 + l);
                int y3 = (int) (ht + y1);

                int x4 = (int) (x3+x3+x3+x3-l-l-l);
                int y4 =  (int) (ht + y1);

                int x5 =(int) (wt+l);
                int y5 =  (int) (ht/3)+y1;



                Point point1_draw = new Point(x1, y1);
                Point point2_draw = new Point(x2, y2);
                Point point3_draw = new Point(x3, y3);
                Point point4_draw = new Point(x4, y4);
                Point point5_draw = new Point(x5, y5);

                Path path = new Path();
                path.moveTo(point1_draw.x, point1_draw.y);
                path.lineTo(point2_draw.x, point2_draw.y);
                path.lineTo(point3_draw.x, point3_draw.y);
                path.lineTo(point4_draw.x, point4_draw.y);
                path.lineTo(point5_draw.x, point5_draw.y);

                path.close();
                canvas.drawPath(path, mBorderPaint);

            }
            else  if (mCropShape == CropImageView.CropShape.DIAMOND) {
                canvas.drawRect(l, t, r, b, mBorderPaint);

                int wt = (int) (r-l);
                int ht = (int) (b-t);

                int x1 = (int) (wt/4 + l);
                int y1 = (int)t;


                int x2 =(int)l;
                int y2 =  (int) (ht/5)+y1;

                int x3 =(int)l;
                int y3 = (int) (y2+y2-y1);

                int x4 = (int) (wt/2 + l);
                int y4 =  (int) (ht + y1);

                int x5 =(int) (wt+l);
                int y5 =  (int) (y2+y2-y1);

                int x6 = (int) (wt+l);
                int y6 = (int) (ht/5)+y1;

                int x7 = (int) (x1+x1+x1-l-l);
                int y7 = (int)t;


                Point point1_draw = new Point(x1, y1);
                Point point2_draw = new Point(x2, y2);
                Point point3_draw = new Point(x3, y3);
                Point point4_draw = new Point(x4, y4);
                Point point5_draw = new Point(x5, y5);
                Point point6_draw = new Point(x6, y6);
                Point point7_draw = new Point(x7, y7);

                Path path = new Path();
                path.moveTo(point1_draw.x, point1_draw.y);
                path.lineTo(point2_draw.x, point2_draw.y);
                path.lineTo(point3_draw.x, point3_draw.y);
                path.lineTo(point4_draw.x, point4_draw.y);
                path.lineTo(point5_draw.x, point5_draw.y);
                path.lineTo(point6_draw.x, point6_draw.y);
                path.lineTo(point7_draw.x, point7_draw.y);

                path.close();
                canvas.drawPath(path, mBorderPaint);

            }
            else {
                // Draw circular crop window border
                Defaults.EMPTY_RECT_F.set(l, t, r, b);
                canvas.drawOval(Defaults.EMPTY_RECT_F, mBorderPaint);
            }
        }
    }

    /**
     * Draw the corner of crop overlay.
     */
    private void drawCorners(Canvas canvas) {
        if (mBorderCornerPaint != null) {

            float lineWidth = mBorderPaint != null ? mBorderPaint.getStrokeWidth() : 0;
            float cornerWidth = mBorderCornerPaint.getStrokeWidth();
            float w = cornerWidth / 2 + mBorderCornerOffset;
            float l = Edge.LEFT.getCoordinate() + w;
            float t = Edge.TOP.getCoordinate() + w;
            float r = Edge.RIGHT.getCoordinate() - w;
            float b = Edge.BOTTOM.getCoordinate() - w;

            float cornerOffset = (cornerWidth - lineWidth) / 2;
            float cornerExtension = cornerWidth / 2 + cornerOffset;

            // Top left
            canvas.drawLine(l - cornerOffset, t - cornerExtension, l - cornerOffset, t + mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(l - cornerExtension, t - cornerOffset, l + mBorderCornerLength, t - cornerOffset, mBorderCornerPaint);

            // Top right
            canvas.drawLine(r + cornerOffset, t - cornerExtension, r + cornerOffset, t + mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(r + cornerExtension, t - cornerOffset, r - mBorderCornerLength, t - cornerOffset, mBorderCornerPaint);

            // Bottom left
            canvas.drawLine(l - cornerOffset, b + cornerExtension, l - cornerOffset, b - mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(l - cornerExtension, b + cornerOffset, l + mBorderCornerLength, b + cornerOffset, mBorderCornerPaint);

            // Bottom left
            canvas.drawLine(r + cornerOffset, b + cornerExtension, r + cornerOffset, b - mBorderCornerLength, mBorderCornerPaint);
            canvas.drawLine(r + cornerExtension, b + cornerOffset, r - mBorderCornerLength, b + cornerOffset, mBorderCornerPaint);
        }
    }

    @Override
    public boolean onTouchEvent(@SuppressWarnings("NullableProblems") MotionEvent event) {

        // If this View is not enabled, don't allow for touch interactions.
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                onActionDown(event.getX(), event.getY());
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                onActionUp();
                return true;

            case MotionEvent.ACTION_MOVE:
                onActionMove(event.getX(), event.getY());
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            default:
                return false;
        }
    }

    /**
     * Handles a {@link android.view.MotionEvent#ACTION_DOWN} event.
     *
     * @param x the x-coordinate of the down action
     * @param y the y-coordinate of the down action
     */
    private void onActionDown(float x, float y) {

        float left = Edge.LEFT.getCoordinate();
        float top = Edge.TOP.getCoordinate();
        float right = Edge.RIGHT.getCoordinate();
        float bottom = Edge.BOTTOM.getCoordinate();

        mPressedHandle = HandleUtil.getPressedHandle(x, y, left, top, right, bottom, mHandleRadius, mCropShape);

        if (mPressedHandle == null) {
            return;
        }

        // Calculate the offset of the touch point from the precise location
        // of the handle. Save these values in a member variable since we want
        // to maintain this offset as we drag the handle.
        mTouchOffset = HandleUtil.getOffset(mPressedHandle, x, y, left, top, right, bottom);

        invalidate();
    }

    /**
     * Handles a {@link android.view.MotionEvent#ACTION_UP} or
     * {@link android.view.MotionEvent#ACTION_CANCEL} event.
     */
    private void onActionUp() {
        if (mPressedHandle == null) {
            return;
        }
        mPressedHandle = null;
        invalidate();
    }

    /**
     * Handles a {@link android.view.MotionEvent#ACTION_MOVE} event.
     *
     * @param x the x-coordinate of the move event
     * @param y the y-coordinate of the move event
     */
    private void onActionMove(float x, float y) {

        if (mPressedHandle == null) {
            return;
        }

        // Adjust the coordinates for the finger position's offset (i.e. the
        // distance from the initial touch to the precise handle location).
        // We want to maintain the initial touch's distance to the pressed
        // handle so that the crop window size does not "jump".
        x += mTouchOffset.first;
        y += mTouchOffset.second;

        // Calculate the new crop window size/position.
        if (mFixAspectRatio) {
            mPressedHandle.updateCropWindow(x, y, mTargetAspectRatio, mBitmapRect, mSnapRadius);
        } else {
            mPressedHandle.updateCropWindow(x, y, mBitmapRect, mSnapRadius);
        }
        invalidate();
    }
    //endregion
}
