package io.github.codefalling.recyclerviewswipedismiss;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.os.SystemClock;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by falling on 15-5-1.
 */


public class SwipeDismissRecyclerViewTouchListener implements View.OnTouchListener {
    // Cached ViewConfiguration and system-wide constant values
    private int mSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long mAnimationTime;

    // Fixed properties
    private RecyclerView mRecyclerView;
    private DismissCallbacks mCallbacks;
    private boolean mIsVertical;

    private int mViewWidth = 1; // 1 and not 0 to prevent dividing by zero

    // Transient properties
    private List<PendingDismissData> mPendingDismisses = new ArrayList<PendingDismissData>();
    private int mDismissAnimationRefCount = 0;
    private float mDownX;
    private float mDownY;
    private boolean mSwiping;
    private int mSwipingSlop;
    private VelocityTracker mVelocityTracker;
    private int mDownPosition;
    private View mDownView;
    private boolean mPaused;


    public interface DismissCallbacks {

        boolean canDismiss(int position);


        void onDismiss(View view);
    }


    public SwipeDismissRecyclerViewTouchListener(Builder builder) {
        ViewConfiguration vc = ViewConfiguration.get(builder.mRecyclerView.getContext());
        mSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity() * 16;
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        mAnimationTime = builder.mRecyclerView.getContext().getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        mRecyclerView = builder.mRecyclerView;
        mCallbacks = builder.mCallbacks;
        mIsVertical = builder.mIsVertical;
    }


    public void setEnabled(boolean enabled) {
        mPaused = !enabled;
    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {


        if (mViewWidth < 2) {
            mViewWidth = mIsVertical ? mRecyclerView.getHeight() : mRecyclerView.getWidth();
        }

        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                if (mPaused) {
                    return false;
                }

                // TODO: ensure this is a finger, and set a flag

                // Find the child view that was touched (perform a hit test)
                Rect rect = new Rect();
                int childCount = mRecyclerView.getChildCount();
                int[] listViewCoords = new int[2];
                mRecyclerView.getLocationOnScreen(listViewCoords);
                int x = (int) motionEvent.getRawX() - listViewCoords[0];
                int y = (int) motionEvent.getRawY() - listViewCoords[1];
                View child;
                for (int i = 0; i < childCount; i++) {
                    child = mRecyclerView.getChildAt(i);
                    child.getHitRect(rect);
                    if (rect.contains(x, y)) {
                        mDownView = child;
                        break;
                    }
                }


                if (mDownView != null) {
                    mDownX = motionEvent.getRawX();
                    mDownY = motionEvent.getRawY();

                    mDownPosition = mRecyclerView.getChildPosition(mDownView);
                    if (mCallbacks.canDismiss(mDownPosition)) {
                        mVelocityTracker = VelocityTracker.obtain();
                        mVelocityTracker.addMovement(motionEvent);
                    } else {
                        mDownView = null;
                    }
                }
                return false;
            }

            case MotionEvent.ACTION_CANCEL: {
                if (mVelocityTracker == null) {
                    break;
                }

                if (mDownView != null && mSwiping) {
                    // cancel
                    if (mIsVertical) {
                        mDownView.animate()
                                .translationY(0)
                                .alpha(1)
                                .setDuration(mAnimationTime)
                                .setListener(null);
                    } else {
                        mDownView.animate()
                                .translationX(0)
                                .alpha(1)
                                .setDuration(mAnimationTime)
                                .setListener(null);
                    }
                }
                mVelocityTracker.recycle();
                mVelocityTracker = null;
                mDownX = 0;
                mDownY = 0;
                mDownView = null;
                mDownPosition = ListView.INVALID_POSITION;
                mSwiping = false;
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (mVelocityTracker == null) {
                    break;
                }

                float deltaX = motionEvent.getRawX() - mDownX;
                float deltaY = motionEvent.getRawY() - mDownY;
                mVelocityTracker.addMovement(motionEvent);
                mVelocityTracker.computeCurrentVelocity(1000);
                float velocityX = mVelocityTracker.getXVelocity();
                float velocityY = mVelocityTracker.getYVelocity();
                float absVelocityX = Math.abs(velocityX);
                float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
                boolean dismiss = false;
                boolean dismissRight = false;

                if (mIsVertical) {
                    if (Math.abs(deltaY) > mViewWidth / 2 && mSwiping) {
                        dismiss = true;
                        dismissRight = deltaY > 0;
                    } else if (mMinFlingVelocity <= absVelocityY && absVelocityY <= mMaxFlingVelocity
                            && absVelocityX < absVelocityY && mSwiping) {
                        // dismiss only if flinging in the same direction as dragging
                        dismiss = (velocityY < 0) == (deltaY < 0);
                        dismissRight = mVelocityTracker.getYVelocity() > 0;
                    }
                    if (dismiss && mDownPosition != ListView.INVALID_POSITION) {
                        // dismiss
                        final View downView = mDownView; // mDownView gets null'd before animation ends
                        final int downPosition = mDownPosition;
                        ++mDismissAnimationRefCount;
                        mDownView.animate()
                                .translationY(dismissRight ? mViewWidth : -mViewWidth)
                                .alpha(0)
                                .setDuration(mAnimationTime)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        performDismiss(downView, downPosition);
                                    }
                                });
                    } else {
                        // cancel
                        mDownView.animate()
                                .translationY(0)
                                .alpha(1)
                                .setDuration(mAnimationTime)
                                .setListener(null);
                    }
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                    mDownX = 0;
                    mDownY = 0;
                    mDownView = null;
                    mDownPosition = ListView.INVALID_POSITION;
                    mSwiping = false;
                } else {

                    if (Math.abs(deltaX) > mViewWidth / 2 && mSwiping) {
                        dismiss = true;
                        dismissRight = deltaX > 0;
                    } else if (mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity
                            && absVelocityY < absVelocityX && mSwiping) {
                        // dismiss only if flinging in the same direction as dragging
                        dismiss = (velocityX < 0) == (deltaX < 0);
                        dismissRight = mVelocityTracker.getXVelocity() > 0;
                    }
                    if (dismiss && mDownPosition != ListView.INVALID_POSITION) {
                        // dismiss
                        final View downView = mDownView; // mDownView gets null'd before animation ends
                        final int downPosition = mDownPosition;
                        ++mDismissAnimationRefCount;
                        mDownView.animate()
                                .translationX(dismissRight ? mViewWidth : -mViewWidth)
                                .alpha(0)
                                .setDuration(mAnimationTime)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        performDismiss(downView, downPosition);
                                    }
                                });
                    } else {
                        // cancel
                        mDownView.animate()
                                .translationX(0)
                                .alpha(1)
                                .setDuration(mAnimationTime)
                                .setListener(null);
                    }
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                    mDownX = 0;
                    mDownY = 0;
                    mDownView = null;
                    mDownPosition = ListView.INVALID_POSITION;
                    mSwiping = false;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {

                if (mVelocityTracker == null || mPaused) {
                    break;
                }

                mVelocityTracker.addMovement(motionEvent);
                float deltaX = motionEvent.getRawX() - mDownX;
                float deltaY = motionEvent.getRawY() - mDownY;
                if (mIsVertical) {
                    if (Math.abs(deltaY) > mSlop && Math.abs(deltaX) < Math.abs(deltaY) / 2) {
                        mSwiping = true;
                        mSwipingSlop = (deltaY > 0 ? mSlop : -mSlop);
                        mRecyclerView.requestDisallowInterceptTouchEvent(true);

                        // Cancel ListView's touch (un-highlighting the item)
                        MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                                (motionEvent.getActionIndex()
                                        << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                        mRecyclerView.onTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                    }

                    if (mSwiping) {
                        mDownView.setTranslationY(deltaY - mSwipingSlop);
                        mDownView.setAlpha(Math.max(0f, Math.min(1f,
                                1f - 2f * Math.abs(deltaY) / mViewWidth)));
                        return true;
                    }

                } else {
                    if (Math.abs(deltaX) > mSlop && Math.abs(deltaY) < Math.abs(deltaX) / 2) {
                        mSwiping = true;
                        mSwipingSlop = (deltaX > 0 ? mSlop : -mSlop);
                        mRecyclerView.requestDisallowInterceptTouchEvent(true);

                        // Cancel ListView's touch (un-highlighting the item)
                        MotionEvent cancelEvent = MotionEvent.obtain(motionEvent);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                                (motionEvent.getActionIndex()
                                        << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                        mRecyclerView.onTouchEvent(cancelEvent);
                        cancelEvent.recycle();
                    }

                    if (mSwiping) {
                        mDownView.setTranslationX(deltaX - mSwipingSlop);
                        mDownView.setAlpha(Math.max(0f, Math.min(1f,
                                1f - 2f * Math.abs(deltaX) / mViewWidth)));
                        return true;
                    }
                }
                break;
            }
        }
        return false;
    }

    class PendingDismissData implements Comparable<PendingDismissData> {
        public int position;
        public View view;

        public PendingDismissData(int position, View view) {
            this.position = position;
            this.view = view;
        }

        @Override
        public int compareTo(PendingDismissData other) {
            // Sort by descending position
            return other.position - position;
        }
    }

    private void performDismiss(final View dismissView, final int dismissPosition) {
        // Animate the dismissed list item to zero-height and fire the dismiss callback when
        // all dismissed list item animations have completed. This triggers layout on each animation
        // frame; in the future we may want to do something smarter and more performant.

        final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();
        final int originalHeight;
        if(mIsVertical)
            originalHeight = dismissView.getWidth();
        else
            originalHeight = dismissView.getHeight();

        ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 1).setDuration(mAnimationTime);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                --mDismissAnimationRefCount;
                if (mDismissAnimationRefCount == 0) {
                    // No active animations, process all pending dismisses.
                    // Sort by descending position
                    Collections.sort(mPendingDismisses);

                    int[] dismissPositions = new int[mPendingDismisses.size()];
                    for (int i = mPendingDismisses.size() - 1; i >= 0; i--) {
                        dismissPositions[i] = mPendingDismisses.get(i).position;
                    }
                    mCallbacks.onDismiss(dismissView);

                    // Reset mDownPosition to avoid MotionEvent.ACTION_UP trying to start a dismiss
                    // animation with a stale position
                    mDownPosition = ListView.INVALID_POSITION;

                    ViewGroup.LayoutParams lp;
                    for (PendingDismissData pendingDismiss : mPendingDismisses) {
                        // Reset view presentation
                        pendingDismiss.view.setAlpha(1f);
                        if(mIsVertical)
                            pendingDismiss.view.setTranslationY(0);
                        else
                            pendingDismiss.view.setTranslationX(0);
                        lp = pendingDismiss.view.getLayoutParams();
                        if(mIsVertical)
                            lp.width = originalHeight;
                        else
                            lp.height = originalHeight;

                        pendingDismiss.view.setLayoutParams(lp);
                    }

                    // Send a cancel event
                    long time = SystemClock.uptimeMillis();
                    MotionEvent cancelEvent = MotionEvent.obtain(time, time,
                            MotionEvent.ACTION_CANCEL, 0, 0, 0);
                    mRecyclerView.dispatchTouchEvent(cancelEvent);

                    mPendingDismisses.clear();
                }
            }
        });

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if(mIsVertical)
                    lp.width = (Integer) valueAnimator.getAnimatedValue();
                else
                    lp.height = (Integer) valueAnimator.getAnimatedValue();
                dismissView.setLayoutParams(lp);
            }
        });

        mPendingDismisses.add(new PendingDismissData(dismissPosition, dismissView));
        animator.start();
    }
    static public class Builder{
        private RecyclerView mRecyclerView;
        private DismissCallbacks mCallbacks;

        private boolean mIsVertical = false;


        public  Builder(RecyclerView recyclerView,DismissCallbacks callbacks){
            mRecyclerView = recyclerView;
            mCallbacks = callbacks;
        }

        public Builder IsVertical(boolean isVertical){
            mIsVertical = isVertical;
            return this;
        }



        public SwipeDismissRecyclerViewTouchListener Build(){
            return new SwipeDismissRecyclerViewTouchListener(this);
        }


    }
}