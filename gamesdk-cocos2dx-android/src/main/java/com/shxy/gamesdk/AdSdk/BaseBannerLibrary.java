package com.shxy.gamesdk.AdSdk;

import android.app.Activity;
import android.widget.FrameLayout;

/**
 * @author: 翟宇翔
 * @date: 2023/12/15
 */
public abstract class BaseBannerLibrary {
    protected abstract void initBannerLib(Activity activity, FrameLayout frameLayout);
    protected void initBannerAd(){}

    protected abstract void loadBannerAd();
    protected abstract void showBannerAd(final boolean isVisible);
    protected abstract boolean isBannerLoaded();
    protected abstract boolean isBannerLoading();
    protected abstract boolean isBannerAdVisible();
    protected abstract float getAdHeight();

}
