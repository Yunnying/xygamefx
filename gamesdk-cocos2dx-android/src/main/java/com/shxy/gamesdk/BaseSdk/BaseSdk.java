package com.shxy.gamesdk.BaseSdk;

import android.app.Activity;
import android.content.SharedPreferences;

import com.shxy.gamesdk.BaseSdk.BaseSdk;

import java.util.Map;

/**
 * @author: 翟宇翔
 * @date: 2023/11/24
 * 基础SDK，为其他SDK提供本地持久化存储和游戏引擎的交互
 */
public class BaseSdk {
    private static Activity sActivity;// 当前的Activity
    private static final String PREFS_NAME = "ShxyGameSDKPrefsFile";// 保存数据的文件名
    /**
     * 初始化方法
     * @param activity 当前的Activity
     */
    public static void init(Activity activity){
        sActivity = activity;
    }

    /**
     * 根据指定的键获取布尔值
     * @param key 键名
     * @param defaultValue 默认值
     * @return 与指定键关联的布尔值，如果键不存在则返回默认值
     */
    public static boolean getBoolForKey(String key, boolean defaultValue) {
        SharedPreferences settings = sActivity.getSharedPreferences(PREFS_NAME, 0);
        try {
            return settings.getBoolean(key, defaultValue);
        }
        catch (Exception ex) {
            ex.printStackTrace();

            Map allValues = settings.getAll();
            Object value = allValues.get(key);
            if ( value instanceof String)
            {
                return  Boolean.parseBoolean(value.toString());
            }
            else if (value instanceof Integer)
            {
                int intValue = ((Integer) value).intValue();
                return (intValue !=  0) ;
            }
            else if (value instanceof Float)
            {
                float floatValue = ((Float) value).floatValue();
                return (floatValue != 0.0f);
            }
        }

        return defaultValue;
    }

    /**
     * 根据指定的键获取SharedPreferences中的整数值，如果不存在该键则返回默认值
     * @param key 键
     * @param defaultValue 默认值
     * @return 对应键的整数值或默认值
     */
    public static int getIntegerForKey(String key, int defaultValue) {
        SharedPreferences settings = sActivity.getSharedPreferences(PREFS_NAME, 0);
        try {
            return settings.getInt(key, defaultValue);
        }
        catch (Exception ex) {
            ex.printStackTrace();

            Map allValues = settings.getAll();
            Object value = allValues.get(key);
            if ( value instanceof String) {
                return  Integer.parseInt(value.toString());
            }
            else if (value instanceof Float)
            {
                return ((Float) value).intValue();
            }
            else if (value instanceof Boolean)
            {
                boolean booleanValue = ((Boolean) value).booleanValue();
                if (booleanValue)
                    return 1;
            }
        }

        return defaultValue;
    }

    /**
     * 根据指定的键获取对应的长整型值
     * @param key 键
     * @param defaultValue 默认值
     * @return 键对应的长整型值，如果不存在则返回默认值
     */
    public static long getLongForKey(String key, long defaultValue) {
        SharedPreferences settings = sActivity.getSharedPreferences(PREFS_NAME, 0);
        try {
            return settings.getLong(key, defaultValue);
        }
        catch (Exception ex) {
            ex.printStackTrace();

            Map allValues = settings.getAll();
            Object value = allValues.get(key);
            if ( value instanceof String) {
                return  Long.parseLong(value.toString());
            }
            else if (value instanceof Float)
            {
                return ((Float) value).longValue();
            }
            else if (value instanceof Boolean)
            {
                boolean booleanValue = ((Boolean) value).booleanValue();
                if (booleanValue)
                    return 1L;
            }
        }

        return defaultValue;
    }

    /**
     * 根据指定的键获取SharedPreferences中的浮点数值，如果不存在该键则返回默认值
     * @param key 键名
     * @param defaultValue 默认值
     * @return 对应键名的浮点数值，如果不存在则返回默认值
     */
    public static float getFloatForKey(String key, float defaultValue) {
        SharedPreferences settings = sActivity.getSharedPreferences(PREFS_NAME, 0);
        try {
            return settings.getFloat(key, defaultValue);
        }
        catch (Exception ex) {
            ex.printStackTrace();

            Map allValues = settings.getAll();
            Object value = allValues.get(key);
            if ( value instanceof String) {
                return  Float.parseFloat(value.toString());
            }
            else if (value instanceof Integer)
            {
                return ((Integer) value).floatValue();
            }
            else if (value instanceof Boolean)
            {
                boolean booleanValue = ((Boolean) value).booleanValue();
                if (booleanValue)
                    return 1.0f;
            }
        }

        return defaultValue;
    }

    /**
     * 根据指定的键获取SharedPreferences中的双精度浮点数值，如果不存在该键则返回默认值。
     * SharedPreferences不支持保存double类型的值，因此转换成float。
     * @param key 键名
     * @param defaultValue 默认值
     * @return 对应键名的双精度浮点数值，如果不存在则返回默认值
     */
    public static double getDoubleForKey(String key, double defaultValue) {
        // SharedPreferences不支持保存double类型的值，因此转换成float
        return getFloatForKey(key, (float) defaultValue);
    }

    /**
     * 根据指定的键获取SharedPreferences中的字符串数值，如果不存在该键则返回默认值
     * @param key 键名
     * @param defaultValue 默认值
     * @return 对应键名的字符串数值，如果不存在则返回默认值
     */
    public static String getStringForKey(String key, String defaultValue) {
        SharedPreferences settings = sActivity.getSharedPreferences(PREFS_NAME, 0);
        try {
            return settings.getString(key, defaultValue);
        }
        catch (Exception ex) {
            ex.printStackTrace();

            return settings.getAll().get(key).toString();
        }
    }

    /**
     * 根据指定的键设置Boolean值
     * @param key 键名
     * @param value 值
     */
    public static void setBoolForKey(String key, boolean value) {
        SharedPreferences settings = sActivity.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * 根据指定的键设置整数值
     * @param key 键名
     * @param value 值
     */
    public static void setIntegerForKey(String key, int value) {
        SharedPreferences settings = sActivity.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     * 根据指定的键设置长整型值
     * @param key 键名
     * @param value 值
     */
    public static void setLongForKey(String key, long value) {
        SharedPreferences settings = sActivity.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    /**
     * 根据指定的键设置浮点数值
     * @param key 键名
     * @param value 值
     */
    public static void setFloatForKey(String key, float value) {
        SharedPreferences settings = sActivity.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    /**
     * 根据指定的键设置双精度浮点数值
     * @param key 键名
     * @param value 值
     */
    public static void setDoubleForKey(String key, double value) {
        // SharedPreferences doesn't support recording double value
        SharedPreferences settings = sActivity.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(key, (float)value);
        editor.apply();
    }

    /**
     * 根据指定的键设置字符串值
     * @param key 键名
     * @param value 值
     */
    public static void setStringForKey(String key, String value) {
        SharedPreferences settings = sActivity.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * 根据指定的键删除对应的数值
     * @param key 键名
     */
    public static void deleteValueForKey(String key) {
        SharedPreferences settings = sActivity.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(key);
        editor.apply();
    }

    /**
     * 在GL线程中执行指定的Runnable
     * @param runnable Runnable
     */
    public static void runOnGLThread(final Runnable runnable) {
        nativeRunOnGLThread(runnable);
    }
    private static native void nativeRunOnGLThread(final Object runnable);
}
