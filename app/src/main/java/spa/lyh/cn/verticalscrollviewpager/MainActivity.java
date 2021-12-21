package spa.lyh.cn.verticalscrollviewpager;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.util.ArrayList;

import spa.lyh.cn.verticalscrollviewpager.test.VerticalPageTransformer;
import spa.lyh.cn.verticalscrollviewpager.test.AutoVerticalViewPager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AutoVerticalViewPager viewPager = findViewById(R.id.vertical_viewpager);
        ViewPagerAdapter bannerPagerAdapter = new ViewPagerAdapter(this);
        ArrayList<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        bannerPagerAdapter.setData(list);

        viewPager.setPageTransformer(false,new VerticalPageTransformer());
        viewPager.setAdapter(bannerPagerAdapter);
        viewPager.setCurrentItem(0);
        viewPager.startAutoScroll(4000);
    }
}