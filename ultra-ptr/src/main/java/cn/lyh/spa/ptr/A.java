package cn.lyh.spa.ptr;

import android.content.Context;

public class A {

    public int a = 0;

    private static A instance;

    public static A getInstance(){
        if (instance == null){
            synchronized (A.class){
                if (instance == null){
                    instance = new A();
                }
            }
        }
        return instance;
    }

}
