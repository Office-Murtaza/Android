package com.smartmux.filevaultfree.lockscreen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.smartmux.filevaultfree.R;
//import com.andexert.library.RippleAnimationListener;

/**
 * Created by stoyan and oliviergoutay on 1/13/15.
 */
public class KeyboardButtonView extends RelativeLayout  {

    private KeyboardButtonClickedListener mKeyboardButtonClickedListener;

    private Context mContext;

    public KeyboardButtonView(Context context) {
        this(context, null);
    }

    public KeyboardButtonView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public KeyboardButtonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.mContext = context;
        initializeView(attrs, defStyleAttr);
    }

    @SuppressLint("NewApi")
	private void initializeView(AttributeSet attrs, int defStyleAttr) {
        if (attrs != null && !isInEditMode()) {
            final TypedArray attributes = mContext.getTheme().obtainStyledAttributes(attrs, R.styleable.KeyboardButtonView,
                    defStyleAttr, 0);
            String text = attributes.getString(R.styleable.KeyboardButtonView_lp_keyboard_button_text);
            Drawable image = attributes.getDrawable(R.styleable.KeyboardButtonView_lp_keyboard_button_image);
            boolean rippleEnabled = attributes.getBoolean(R.styleable.KeyboardButtonView_lp_keyboard_button_ripple_enabled, true);

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            KeyboardButtonView view = (KeyboardButtonView) inflater.inflate(R.layout.view_keyboard_button, this);

            if (text != null) {
                TextView textView = (TextView) view.findViewById(R.id.keyboard_button_textview);
                textView.setTextColor(Color.parseColor("#ffffff"));
                if (textView != null) {
                    textView.setText(text);
                }
            }
            if (image != null) {
                ImageView imageView = (ImageView) view.findViewById(R.id.keyboard_button_imageview);
                if (imageView != null) {
                    imageView.setImageDrawable(image);
                    imageView.setVisibility(View.VISIBLE);
                }
            }

    
        }
    }

    /**
     * Set by {@link KeyboardView} to returns events to
     * {@link com.github.com.smartmux.filevaultfree.managers.AppLockActivity}
     */
    public void setOnRippleAnimationEndListener(KeyboardButtonClickedListener keyboardButtonClickedListener) {
        mKeyboardButtonClickedListener = keyboardButtonClickedListener;
    }

//    @Override
//    public void onRippleAnimationEnd() {
//        if (mKeyboardButtonClickedListener != null) {
//            mKeyboardButtonClickedListener.onRippleAnimationEnd();
//        }
//    }

    /**
     * Retain touches for {@link com.andexert.library.RippleView}.
     * Otherwise views above will not have the event.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        onTouchEvent(event);
        return false;
    }
}
