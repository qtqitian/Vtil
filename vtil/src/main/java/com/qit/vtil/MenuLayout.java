package com.qit.vtil;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;

import static com.qit.vtil.DimenUtil.px2dip;
import static com.qit.vtil.Util.getApplicationContext;

/**
 * author: Qit .
 * date:   On 2019/5/28
 */
public class MenuLayout extends ViewGroup implements OnClickListener {
    private WindowManager windowManager;
    private WindowManager.LayoutParams params = new WindowManager.LayoutParams();

    private int mRadius;
    /**
     * 菜单的状态
     */
    private Status mCurrentStatus = Status.CLOSE;
    /**
     * 菜单的主按钮
     */
    private View mMenuBtn;
    /**
     * 菜单主按钮内容控件
     */
    private ImageView mMenuContentView;
    /**
     * 内容图
     */
    private Drawable mMenuContentDrawable;
    /**
     * 选中的功能项
     */
    private int position = 0;

    private OnMenuItemClickListener mMenuItemClickListener;
    private OnMenuClickListener mMenuClickListener;

    public void show() {
        mMenuBtn = getChildAt(0);
        mMenuContentView = mMenuBtn.findViewById(R.id.id_button);
        windowManager.addView(this, getWindowLayoutParams());
    }

    public enum Status {
        OPEN, CLOSE
    }

    private boolean isSelect = false;

    /**
     * 点击子菜单项的回调接口
     */
    public interface OnMenuItemClickListener {
        void onClick(View view, int pos);
    }

    /**
     * 点击菜单的回调接口
     */
    public interface OnMenuClickListener {
        void reset(int pos);
    }

    public void setOnMenuItemClickListener(
            OnMenuItemClickListener mMenuItemClickListener) {
        this.mMenuItemClickListener = mMenuItemClickListener;
    }

    public void setOnMenuClickListener(
            OnMenuClickListener mMenuClickListener) {
        this.mMenuClickListener = mMenuClickListener;
    }

    public MenuLayout(Context context) {
        this(context, null);
    }

    public MenuLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MenuLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        windowManager = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);

        mRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                50, getResources().getDisplayMetrics());


    }


    private int touchSlop;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);


        int width = mMenuBtn.getWidth() * 2 + mRadius;
        setMeasuredDimension(width, width);
        requestLayout();

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            // 测量child
            measureChild(getChildAt(i), widthMeasureSpec, heightMeasureSpec);
        }

    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            layoutCButton();

            int count = getChildCount();

            for (int i = 0; i < count - 1; i++) {
                View child = getChildAt(i + 1);

                child.setVisibility(View.GONE);

                int cl = (int) (mRadius * Math.sin(Math.PI / 2 / (count - 2)
                        * i));
                int ct = (int) (mRadius * Math.cos(Math.PI / 2 / (count - 2)
                        * i));

                int cWidth = child.getMeasuredWidth();
                int cHeight = child.getMeasuredHeight();

                ct = getMeasuredHeight() - cHeight - ct;
                cl = getMeasuredWidth() - cWidth - cl;
                child.layout(cl, ct, cl + cWidth, ct + cHeight);

            }

        }

    }

    /**
     * 定位主菜单按钮
     */
    private void layoutCButton() {
//        mMenuBtn = getChildAt(0);
        mMenuBtn.setOnClickListener(this);
        mMenuBtn.setOnTouchListener(new View.OnTouchListener() {
            private float downX, downY;
            private float lastX, lastY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getRawX();
                        downY = event.getRawY();
                        lastY = downY;
                        lastX = downX;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        params.y -= event.getRawY() - lastY;
                        params.x -= event.getRawX() - lastX;
                        params.y = Math.max(0, params.y);
                        params.x = Math.max(0, params.x);
                        windowManager.updateViewLayout(MenuLayout.this, params);
                        lastY = event.getRawY();
                        lastX = event.getRawX();
                        break;
                    case MotionEvent.ACTION_UP:
                        if (Math.abs(event.getRawX() - downX) < touchSlop && Math.abs(event.getRawY() - downY) < touchSlop) {
                            mMenuBtn.performClick();
                        }
                        break;
                }
                return true;
            }
        });

        int width = mMenuBtn.getMeasuredWidth();
        int height = mMenuBtn.getMeasuredHeight();

        int l = getMeasuredWidth() - width;
        int t = getMeasuredHeight() - height;
        mMenuBtn.layout(l, t, l + width, t + width);
    }

    @Override
    public void onClick(View v) {
        if (isSelect) {
            mMenuContentDrawable = null;
            mMenuContentView.setImageDrawable(null);
            mMenuClickListener.reset(position);
            position = 0;
            isSelect = false;
        }
        changeStatus();
        toggleMenu(300);
    }

    /**
     * 切换菜单
     */
    public void toggleMenu(int duration) {
        // 为menuItem添加平移动画和旋转动画
        int count = getChildCount();

        for (int i = 0; i < count - 1; i++) {
            final View childView = getChildAt(i + 1);
            childView.setVisibility(View.VISIBLE);

            int mBtnPadding = px2dip(mMenuBtn.getPaddingBottom());
            // start
            int cl = (int) (mRadius * Math.sin(Math.PI / 2 / (count - 2) * i)) - mBtnPadding;
            int ct = (int) (mRadius * Math.cos(Math.PI / 2 / (count - 2) * i)) - mBtnPadding;
            // end 0 , 0


            AnimationSet animset = new AnimationSet(true);
            Animation tranAnim;

            // to open
            if (mCurrentStatus == Status.OPEN) {
                tranAnim = new TranslateAnimation(cl, 0, ct, 0);
                childView.setClickable(true);
                childView.setFocusable(true);

            } else
            // to close
            {
                tranAnim = new TranslateAnimation(0, cl, 0, ct);
                childView.setClickable(false);
                childView.setFocusable(false);
            }
            tranAnim.setFillAfter(true);
            tranAnim.setDuration(duration);
            tranAnim.setStartOffset((i * 100) / count);

            tranAnim.setAnimationListener(new AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (mCurrentStatus == Status.CLOSE) {
                        childView.setVisibility(View.GONE);
                        mMenuContentView.setImageDrawable(mMenuContentDrawable);
                    }
                }
            });
            // 旋转动画
            RotateAnimation rotateAnim = new RotateAnimation(0, 720,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnim.setDuration(duration);
            rotateAnim.setFillAfter(true);

            animset.addAnimation(rotateAnim);
            animset.addAnimation(tranAnim);
            childView.startAnimation(animset);

            final int pos = i + 1;
            childView.setOnClickListener(v -> {
                position = pos;
                if (mMenuItemClickListener != null)
                    mMenuItemClickListener.onClick(childView, pos);
                mMenuContentDrawable = ((ImageView) getChildAt(pos)).getDrawable();
                isSelect = true;
                menuItemAnim(pos - 1);
                changeStatus();
                toggleMenu(300);

            });
        }
        // 切换菜单状态
    }

    /**
     * 添加menuItem的点击动画
     */
    private void menuItemAnim(int pos) {
        for (int i = 0; i < getChildCount() - 1; i++) {

            View childView = getChildAt(i + 1);
            if (i == pos) {
                childView.startAnimation(scaleBigAnim());
            } else {

                childView.startAnimation(scaleSmallAnim());
            }

            childView.setClickable(false);
            childView.setFocusable(false);

        }

    }

    private Animation scaleSmallAnim() {

        AnimationSet animationSet = new AnimationSet(true);

        ScaleAnimation scaleAnim = new ScaleAnimation(1.0f, 0.0f, 1.0f, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        AlphaAnimation alphaAnim = new AlphaAnimation(1f, 0.0f);
        animationSet.addAnimation(scaleAnim);
        animationSet.addAnimation(alphaAnim);
        animationSet.setDuration(300);
        animationSet.setFillAfter(true);
        return animationSet;

    }

    /**
     * 为当前点击的Item设置变大和透明度降低的动画
     */
    private Animation scaleBigAnim() {
        AnimationSet animationSet = new AnimationSet(true);

        ScaleAnimation scaleAnim = new ScaleAnimation(1.0f, 4.0f, 1.0f, 4.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        AlphaAnimation alphaAnim = new AlphaAnimation(1f, 0.0f);

        animationSet.addAnimation(scaleAnim);
        animationSet.addAnimation(alphaAnim);

        animationSet.setDuration(300);
        animationSet.setFillAfter(true);
        return animationSet;

    }

    /**
     * 切换菜单状态
     */
    private void changeStatus() {
        mCurrentStatus = (mCurrentStatus == Status.CLOSE ? Status.OPEN
                : Status.CLOSE);
    }

    private WindowManager.LayoutParams getWindowLayoutParams() {
        params.width = FrameLayout.LayoutParams.WRAP_CONTENT;
        params.height = FrameLayout.LayoutParams.WRAP_CONTENT;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.format = PixelFormat.TRANSLUCENT;
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.x = 50;
        params.y = 50;
        return params;
    }


}

