package com.shxy.gamesdk.AdSdk;

import android.app.Activity;

/**
 * @author: 翟宇翔
 * @date: 2023/12/15
 */
public abstract class BaseOpenLibrary {
    protected abstract void initAdOpenLib(Activity activity);
    protected void initOpenAd(){}
    protected abstract void loadOpenAd();
    protected abstract void showOpenAd();
    protected abstract boolean isOpenAdLoading();
    protected abstract boolean isOpenAdLoaded();
}
