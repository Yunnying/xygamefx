package com.shxy.gamesdk.AdSdk;

import android.app.Activity;

/**
 * @author: 翟宇翔
 * @date: 2023/12/15
 */
public abstract class BaseFullLibrary {
    protected abstract void initFullLib(Activity activity);
    protected void initFullAd(){}
    protected abstract void loadFullAd();
    protected abstract void showFullAd();
    protected abstract boolean isFullAdLoading();
    protected abstract boolean isFullAdLoaded();

}
