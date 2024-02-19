package com.shxy.gamesdk.AdSdk;

import android.app.Activity;

/**
 * @author: 翟宇翔
 * @date: 2023/12/15
 */
public abstract class BaseRewardLibrary {
    public abstract void initRewardLib(Activity activity);
    public void initRewardAd(){}
    protected abstract void loadRewardAd();
    protected abstract void showRewardAd();
    protected abstract boolean isRewardAdLoading();
    protected abstract boolean isRewardAdLoaded();
}
