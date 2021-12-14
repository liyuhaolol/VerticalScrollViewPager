package spa.lyh.cn.verticalscrollviewpager;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        VerticalViewPager viewPager = findViewById(R.id.vertical_viewpager);
        ViewPagerAdapter bannerPagerAdapter = new ViewPagerAdapter(this);
        viewPager.setPageTransformer(false,new VerticalPageTransformer());
        viewPager.setAdapter(bannerPagerAdapter);
        ArrayList<String> list = new ArrayList<>();
        list.add("1");
        list.add("2");
        list.add("3");
        bannerPagerAdapter.setData(list);
    }
}