/*
 * Copyright (C) 2014 nagoya0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.misocast.bidirectionalswiperefreshlayout;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.CustomSwipeRefreshLayout;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.view.animation.Animation.AnimationListener;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.ScrollView;

/**
 * @author nagoya0
 *
 */
public class BidirectionalSwipeRefreshLayout extends CustomSwipeRefreshLayout {
    private static final String LOG_TAG = BidirectionalSwipeRefreshLayout.class.getSimpleName();

    private final Animation mAnimateToStartPosition = new Animation() {
        @Override
        public void applyTransformation(float interpolatedTime, Transformation t) {
            int targetTop = 0;
            if (mFrom != mOriginalOffsetTop) {
                targetTop = (mFrom + (int)((mOriginalOffsetTop - mFrom) * interpolatedTime));
            }
            int offset = targetTop - mTarget.getTop();
            final int currentTop = mTarget.getTop();
            if (mFrom > mOriginalOffsetTop) {
                if (offset + currentTop < 0) {
                    offset = 0 - currentTop;
                }
            } else if (mFrom < mOriginalOffsetTop) {
                if (offset + currentTop > 0) {
                    offset = 0 - currentTop;
                }
            }
            setTargetOffsetTopAndBottom(offset);
        }
    };

    public BidirectionalSwipeRefreshLayout(Context context) {
        super(context);
    }

    public BidirectionalSwipeRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void animateOffsetToStartPosition(int from, AnimationListener listener) {
        mFrom = from;
        mAnimateToStartPosition.reset();
        mAnimateToStartPosition.setDuration(mMediumAnimationDuration);
        mAnimateToStartPosition.setAnimationListener(listener);
        mAnimateToStartPosition.setInterpolator(mDecelerateInterpolator);
        mTarget.startAnimation(mAnimateToStartPosition);
    }

    /**
     * @return Whether it is possible for the child view of this layout to
     *         scroll down. Override this if the child view is a custom view.
     */
    public boolean canChildScrollDown() {
        if (android.os.Build.VERSION.SDK_INT < 14) {
            if (mTarget instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) mTarget;

                final Adapter adapter = absListView.getAdapter();
                if (null == adapter || adapter.isEmpty()) {
                    return false;
                }
                final int lastItemPosition = absListView.getCount() - 1;
                final int lastVisiblePosition = absListView.getLastVisiblePosition();
                if (lastVisiblePosition >= lastItemPosition - 1) {
                    final int childIndex = lastVisiblePosition - absListView.getFirstVisiblePosition();
                    final View lastVisibleChild = absListView.getChildAt(childIndex);
                    if (lastVisibleChild != null) {
                        return absListView.getTop() + lastVisibleChild.getBottom() > absListView.getBottom();
                    }
                }
            } else if (mTarget instanceof ScrollView) {
                final ScrollView scrollView = (ScrollView) mTarget;
                View scrollViewChild = scrollView.getChildAt(0);
                if (null != scrollViewChild) {
                    return scrollView.getScrollY() < (scrollViewChild.getHeight() - getHeight());
                }
            } else if (mTarget instanceof WebView) {
                final WebView webView = (WebView) mTarget;
                float exactContentHeight = FloatMath.floor(webView.getContentHeight() * webView.getScale());
                return webView.getScrollY() < (exactContentHeight - webView.getHeight());
            }
            return true;
        } else {
            return ViewCompat.canScrollVertically(mTarget, 1);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        ensureTarget();

        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        boolean canScrollUp = canChildScrollUp();
        boolean canScrollDown = canChildScrollDown();
        if (!isEnabled() || mReturningToStart || (canScrollUp && canScrollDown)) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                mCurrPercentage = 0;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mActivePointerId == INVALID_POINTER) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but don't have an active pointer id.");
                    return false;
                }

                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float yDiff = y - mInitialMotionY;
                if (!canScrollUp) {
                    if (yDiff > mTouchSlop) {
                        mLastMotionY = y;
                        mIsBeingDragged = true;
                    }
                } else if (!canScrollDown) {
                    if (yDiff < -mTouchSlop) {
                        mLastMotionY = y;
                        mIsBeingDragged = true;
                    }
                }
                break;

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mCurrPercentage = 0;
                mActivePointerId = INVALID_POINTER;
                break;
        }

        return mIsBeingDragged;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        if (mReturningToStart && action == MotionEvent.ACTION_DOWN) {
            mReturningToStart = false;
        }

        boolean canScrollUp = canChildScrollUp();
        boolean canScrollDown = canChildScrollDown();
        if (!isEnabled() || mReturningToStart || (canScrollUp && canScrollDown)) {
            // Fail fast if we're not in a state where a swipe is possible
            return false;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionY = mInitialMotionY = ev.getY();
                mActivePointerId = MotionEventCompat.getPointerId(ev, 0);
                mIsBeingDragged = false;
                mCurrPercentage = 0;
                break;

            case MotionEvent.ACTION_MOVE:
                final int pointerIndex = MotionEventCompat.findPointerIndex(ev, mActivePointerId);
                if (pointerIndex < 0) {
                    Log.e(LOG_TAG, "Got ACTION_MOVE event but have an invalid active pointer id.");
                    return false;
                }

                final float y = MotionEventCompat.getY(ev, pointerIndex);
                final float yDiff = y - mInitialMotionY;

                if (!mIsBeingDragged) {
                    if (!canScrollUp) {
                    	if (yDiff > mTouchSlop) {
                            mIsBeingDragged = true;
                    	}
                    } else if (!canScrollDown) {
                    	if (yDiff < -mTouchSlop) {
                            mIsBeingDragged = true;
                    	}
                    }
                }

                if (mIsBeingDragged) {
                    if (!canScrollUp) {
                        // User velocity passed min velocity; trigger a refresh
                        if (yDiff > mDistanceToTriggerSync) {
                            // User movement passed distance; trigger a refresh
                            startRefresh();
                        } else {
                            // Just track the user's movement
                            setTriggerPercentage(
                                    mAccelerateInterpolator.getInterpolation(
                                            yDiff / mDistanceToTriggerSync));
                            updateContentOffsetTop((int) (yDiff));
                            if (mLastMotionY > y && mTarget.getTop() == getPaddingTop()) {
                                // If the user puts the view back at the top, we
                                // don't need to. This shouldn't be considered
                                // cancelling the gesture as the user can restart from the top.
                                removeCallbacks(mCancel);
                            } else {
                                updatePositionTimeout();
                            }
                        }
                    } else if (!canScrollDown) {
                        // User velocity passed min velocity; trigger a refresh
                        if (yDiff < -mDistanceToTriggerSync) {
                            // User movement passed distance; trigger a refresh
                            startRefresh();
                        } else {
                            // Just track the user's movement
                            setTriggerPercentage(
                                    mAccelerateInterpolator.getInterpolation(
                                            -yDiff / mDistanceToTriggerSync));
                            updateContentOffsetTopWhenPullUp((int) (yDiff));
                            if (mLastMotionY < y && mTarget.getBottom() == getPaddingBottom()) {
                                // If the user puts the view back at the top, we
                                // don't need to. This shouldn't be considered
                                // cancelling the gesture as the user can restart from the top.
                                removeCallbacks(mCancel);
                            } else {
                                updatePositionTimeout();
                            }
                        }
                    }
                    mLastMotionY = y;
                }
                break;

            case MotionEventCompat.ACTION_POINTER_DOWN: {
                final int index = MotionEventCompat.getActionIndex(ev);
                mLastMotionY = MotionEventCompat.getY(ev, index);
                mActivePointerId = MotionEventCompat.getPointerId(ev, index);
                break;
            }

            case MotionEventCompat.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsBeingDragged = false;
                mCurrPercentage = 0;
                mActivePointerId = INVALID_POINTER;
                return false;
        }

        return true;
    }

    private void updateContentOffsetTopWhenPullUp(int targetTop) {
        final int currentTop = mTarget.getTop();
        if (targetTop < -mDistanceToTriggerSync) {
            targetTop = (int) -mDistanceToTriggerSync;
        } else if (targetTop > 0) {
            targetTop = 0;
        }
        setTargetOffsetTopAndBottom(targetTop - currentTop);
    }
}
