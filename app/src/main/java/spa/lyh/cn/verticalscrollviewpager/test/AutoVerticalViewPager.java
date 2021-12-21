package spa.lyh.cn.verticalscrollviewpager.test;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

public class AutoVerticalViewPager extends ViewPager {

    public interface OnPageClickListener {
        void onPageClick(AutoVerticalViewPager pager, int position);
    }

    private static final int MSG_AUTO_SCROLL = 0;
    private static final int DEFAULT_INTERNAL_IM_MILLIS = 2000;

    private boolean autoScroll = false;
    private int intervalInMillis;
    private int touchSlop;
    private float mInitialMotionX;
    private float mInitialMotionY;
    private float mLastMotionX;
    private float mLastMotionY;

    private H handler;

    private OnPageChangeListener mOnPageChangeListener;
    private List<OnPageChangeListener> mOnPageChangeListeners = new LinkedList<>();

    private PagerAdapter wrappedPagerAdapter;
    private PagerAdapter wrapperPagerAdapter;

    private AutoScrollFactorScroller scroller;

    private OnPageClickListener onPageClickListener;

    private InnerDataSetObserver mObserver = new InnerDataSetObserver();

    private class H extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_AUTO_SCROLL:
                    setCurrentItem(getCurrentItem() + 1);
                    sendEmptyMessageDelayed(MSG_AUTO_SCROLL, intervalInMillis);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }


    public AutoVerticalViewPager(@NonNull Context context) {
        super(context);
        init();
    }

    public AutoVerticalViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        super.addOnPageChangeListener(new InnerOnPageChangeListener());

        handler = new H();
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        pauseAutoScroll();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (autoScroll) {
            startAutoScroll();
        }
    }

    public void startAutoScroll() {
        startAutoScroll(intervalInMillis != 0 ? intervalInMillis : DEFAULT_INTERNAL_IM_MILLIS);
    }

    public void startAutoScroll(int intervalInMillis) {
        // Only post scroll message when necessary.
        if (getCount() > 1) {
            this.intervalInMillis = intervalInMillis;
            autoScroll = true;
            pauseAutoScroll();
            handler.sendEmptyMessageDelayed(MSG_AUTO_SCROLL, intervalInMillis);
        }
    }

    public void stopAutoScroll() {
        autoScroll = false;
        pauseAutoScroll();
    }

    public void setInterval(int intervalInMillis) {
        this.intervalInMillis = intervalInMillis;
    }

    public void setScrollFactor(double factor) {
        setScrollerIfNeeded();
        scroller.setFactor(factor);
    }

    public OnPageClickListener getOnPageClickListener() {
        return onPageClickListener;
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (autoScroll) {
            if (hasWindowFocus) {
                startAutoScroll();
            } else {
                pauseAutoScroll();
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setOnPageChangeListener(OnPageChangeListener listener) {
        mOnPageChangeListener = listener;
    }

    @Override
    public void addOnPageChangeListener(OnPageChangeListener listener) {
        mOnPageChangeListeners.add(listener);
    }

    @Override
    public void clearOnPageChangeListeners() {
        super.clearOnPageChangeListeners();
        mOnPageChangeListeners.clear();
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
        if (wrappedPagerAdapter != null && mObserver != null) {
            wrappedPagerAdapter.unregisterDataSetObserver(mObserver);
        }
        wrappedPagerAdapter = adapter;
        if (wrappedPagerAdapter != null && mObserver != null) {
            wrappedPagerAdapter.registerDataSetObserver(mObserver);
        }
        wrapperPagerAdapter = (wrappedPagerAdapter == null) ? null : new AutoScrollPagerAdapter(adapter);
        super.setAdapter(wrapperPagerAdapter);

        if (adapter != null && adapter.getCount() != 0) {
            post(new Runnable() {
                @Override
                public void run() {
                    setCurrentItem(0, false);
                }
            });
        }
    }

    @Override
    public PagerAdapter getAdapter() {
        // In order to be compatible with ViewPagerIndicator
        return wrappedPagerAdapter;
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(item + 1);
    }

    @Override
    public void setCurrentItem(int item, boolean smoothScroll) {
        super.setCurrentItem(item + 1, smoothScroll);
    }

    @Override
    public int getCurrentItem() {
        int curr = super.getCurrentItem();
        if (wrappedPagerAdapter != null && wrappedPagerAdapter.getCount() > 1) {
            if (curr == 0) {
                curr = wrappedPagerAdapter.getCount() - 1;
            } else if (curr == wrapperPagerAdapter.getCount() - 1) {
                curr = 0;
            } else {
                curr = curr - 1;
            }
        }
        return curr;
    }

    private void setScrollerIfNeeded() {
        if (scroller != null) {
            return;
        }
        try {
            Field scrollerField = ViewPager.class.getDeclaredField("mScroller");
            scrollerField.setAccessible(true);
            Field interpolatorField = ViewPager.class.getDeclaredField("sInterpolator");
            interpolatorField.setAccessible(true);
            scroller = new AutoScrollFactorScroller(getContext(), (Interpolator) interpolatorField.get(null));
            scrollerField.set(this, scroller);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pauseAutoScroll() {
        handler.removeMessages(MSG_AUTO_SCROLL);
    }

    private class InnerOnPageChangeListener implements OnPageChangeListener {
        private int mLastSelectedPage = -1;

        private InnerOnPageChangeListener() {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == SCROLL_STATE_IDLE && getCount() > 1) {
                if (getCurrentItemOfWrapper() == 0) {
                    // scroll to the last page
                    setCurrentItem(getCount() - 1, false);
                } else if (getCurrentItemOfWrapper() == getCountOfWrapper() - 1) {
                    // scroll to the first page
                    setCurrentItem(0, false);
                }
            }
            if (mOnPageChangeListener != null) {
                mOnPageChangeListener.onPageScrollStateChanged(state);
            }
            for (OnPageChangeListener onPageChangeListener : mOnPageChangeListeners) {
                onPageChangeListener.onPageScrollStateChanged(state);
            }
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            if (mOnPageChangeListener != null && position > 0 && position < getCount()) {
                mOnPageChangeListener.onPageScrolled(position - 1, positionOffset, positionOffsetPixels);
            }
            for (OnPageChangeListener onPageChangeListener : mOnPageChangeListeners) {
                onPageChangeListener.onPageScrolled(position - 1, positionOffset, positionOffsetPixels);
            }
        }

        @Override
        public void onPageSelected(final int position) {
            final int pos;
            // Fix position
            if (position == 0) {
                pos = getCount() - 1;
            } else if (position == getCountOfWrapper() - 1) {
                pos = 0;
            } else {
                pos = position - 1;
            }

            // Comment this, onPageSelected will be triggered twice for position 0 and getCount -1.
            // Uncomment this, PageIndicator will have trouble.
//                if (mLastSelectedPage != pos) {
            mLastSelectedPage = pos;
            // Post a Runnable in order to be compatible with ViewPagerIndicator because
            // onPageSelected is invoked before onPageScrollStateChanged.

            if (mOnPageChangeListener != null) {
                AutoVerticalViewPager.this.post(new Runnable() {
                    @Override
                    public void run() {
                        mOnPageChangeListener.onPageSelected(pos);
                    }
                });
            }

            for (final OnPageChangeListener mListener : mOnPageChangeListeners) {
                AutoVerticalViewPager.this.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onPageSelected(pos);
                    }
                });
            }
        }
    }

    private int getCountOfWrapper() {
        if (wrapperPagerAdapter != null) {
            return wrapperPagerAdapter.getCount();
        }
        return 0;
    }

    private int getCurrentItemOfWrapper() {
        return super.getCurrentItem();
    }

    private int getCount() {
        if (wrappedPagerAdapter != null) {
            return wrappedPagerAdapter.getCount();
        }
        return 0;
    }

/*    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return super.onTouchEvent(swapTouchEvent(MotionEvent.obtain(ev)));
    }*/

    /*@Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return super.onInterceptTouchEvent(swapTouchEvent(MotionEvent.obtain(ev)));
    }*/

    private MotionEvent swapTouchEvent(MotionEvent event) {
        float width = getWidth();
        float height = getHeight();
        event.setLocation((event.getY() / height) * width, ((event.getX() / width) * height));
        return event;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                Log.e("qwer","ACTION_DOWN");
                if (getCurrentItemOfWrapper() + 1 == getCountOfWrapper()) {
                    setCurrentItem(0, false);
                } else if (getCurrentItemOfWrapper() == 0) {
                    setCurrentItem(getCount() - 1, false);
                }
                pauseAutoScroll();
                mInitialMotionX = ev.getX();
                mInitialMotionY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                Log.e("qwer","ACTION_MOVE");
                mLastMotionX = ev.getX();
                mLastMotionY = ev.getY();
                if ((int) Math.abs(mLastMotionX - mInitialMotionX) > touchSlop
                        || (int) Math.abs(mLastMotionY - mInitialMotionY) > touchSlop) {
                    Log.e("qwer","Math");
                    mInitialMotionX = 0.0f;
                    mInitialMotionY = 0.0f;
                }
                break;
            case MotionEvent.ACTION_UP:
                Log.e("qwer","ACTION_UP");
                if (autoScroll) {
                    startAutoScroll();
                }

                // Manually swipe not affected by scroll factor.
                if (scroller != null) {
                    final double lastFactor = scroller.getFactor();
                    scroller.setFactor(1);
                    post(new Runnable() {
                        @Override
                        public void run() {
                            scroller.setFactor(lastFactor);
                        }
                    });
                }

                mLastMotionX = ev.getX();
                mLastMotionY = ev.getY();
                if ((int) mInitialMotionX != 0 && (int) mInitialMotionY != 0) {
                    if ((int) Math.abs(mLastMotionX - mInitialMotionX) < touchSlop
                            && (int) Math.abs(mLastMotionY - mInitialMotionY) < touchSlop) {
                        mInitialMotionX = 0.0f;
                        mInitialMotionY = 0.0f;
                        mLastMotionX = 0.0f;
                        mLastMotionY = 0.0f;
                        if (onPageClickListener != null) {
                            onPageClickListener.onPageClick(this, getCurrentItem());
                        }
                    }
                }
                break;
        }
        requestDisallowInterceptTouchEvent(true);//截断触摸事件
        return super.onTouchEvent(swapTouchEvent(MotionEvent.obtain(ev)));
    }

    private class InnerDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            if (wrapperPagerAdapter != null) {
                wrapperPagerAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onInvalidated() {
            if (wrapperPagerAdapter != null) {
                wrapperPagerAdapter.notifyDataSetChanged();
            }
        }
    }

}
