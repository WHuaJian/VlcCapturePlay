package com.whj.vlc.player.utils;

import android.app.Activity;

import java.util.Iterator;
import java.util.Stack;

/**
 * @author William
 * @Github WHuaJian
 * Created at 2018/6/20 下午9:10
 */

public class VlcUIManager {

    private static Stack<Activity> activityStack;
    private static VlcUIManager instance;



    private VlcUIManager() {
    }

    /**
     * 单一实例
     */
    public static VlcUIManager getAppManager() {
        if (instance == null) {
            instance = new VlcUIManager();
        }
        return instance;
    }

    /**
     * 添加Activity到堆栈
     */
    public void addActivity(Activity activity) {
        if (activityStack == null) {
            activityStack = new Stack<>();
        }
        activityStack.add(activity);
    }


    public int size() {
        return (activityStack == null || activityStack.size() == 0) ? 0 : activityStack.size();
    }

    /**
     * 结束指定的Activity
     */
    public void finishActivity(Activity activity) {
        if (activity != null) {
            activityStack.remove(activity);
            activity.finish();
        }
    }

    /**
     * 结束指定类名的Activity
     */
    public void finishActivity(Class<?> cls) {

        Iterator<Activity> iterator = activityStack.iterator();
        while (iterator.hasNext()) {
            Activity activity = iterator.next();
            if (activity.getClass().equals(cls)) {
                iterator.remove();
                activity.finish();
            }
        }

    }

}
