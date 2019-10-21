package butter.droid.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import butter.droid.base.utils.VersionUtils;

public class WrappingViewPager extends ViewPager {

    private Boolean mAnimStarted = false;

    public WrappingViewPager(Context context) {
        super(context);
    }

    public WrappingViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (!mAnimStarted && null != getAdapter()) {
            int height = 0;
            View child = ((FragmentPagerAdapter) getAdapter()).getItem(getCurrentItem()).getView();
            if (child != null) {
                child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                height = child.getMeasuredHeight();
                if (VersionUtils.isJellyBean() && height < getMinimumHeight()) {
                    height = getMinimumHeight();
                }
            }

            // Not the best place to put this animation, but it works pretty good.
            int newHeight = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            if (getLayoutParams().height != 0 && heightMeasureSpec != newHeight) {
                final int targetHeight = height;
                final int currentHeight = getLayoutParams().height;
                final int heightChange = targetHeight - currentHeight;

                Animation a = new Animation() {
                    @Override
                    protected void applyTransformation(float interpolatedTime, Transformation t) {
                        if (interpolatedTime >= 1) {
                            getLayoutParams().height = targetHeight;
                        } else {
                            int stepHeight = (int) (heightChange * interpolatedTime);
                            getLayoutParams().height = currentHeight + stepHeight;
                        }
                        requestLayout();
                    }

                    @Override
                    public boolean willChangeBounds() {
                        return true;
                    }
                };

                a.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        mAnimStarted = true;
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mAnimStarted = false;
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });

                if (heightChange > 0) {
                    a.setDuration(50);
                } else {
                    a.setDuration(500);
                }
                startAnimation(a);
                mAnimStarted = true;
            } else {
                heightMeasureSpec = newHeight;
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onPageScrolled(int position, float offset, int offsetPixels) {
        super.onPageScrolled(position, offset, offsetPixels);

        invalidate();
        requestLayout();
    }
}