package spa.lyh.cn.verticalscrollviewpager.test;

import android.view.View;

import androidx.viewpager.widget.ViewPager;

public class VerticalPageTransformer implements ViewPager.PageTransformer{

    @Override
    public void transformPage(View view, float position) {
        if (position >= -1 && position <= 1) {
            view.setTranslationX(view.getWidth() * -position);
            float yPosition = position * view.getHeight();
            view.setTranslationY(yPosition);
        }
    }
}
