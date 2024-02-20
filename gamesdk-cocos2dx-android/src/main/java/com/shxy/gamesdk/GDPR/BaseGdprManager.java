package com.shxy.gamesdk.GDPR;

import android.app.Activity;

/**
 * @author: 翟宇翔
 * @date: 2023/12/18
 */
public abstract class BaseGdprManager {
    protected abstract void init(Activity activity, boolean isDebug);
    protected abstract boolean isGDPRCountry();
    protected abstract int getAdLevels();
    protected abstract void startGDPR();
    protected abstract void updateGDPR();
    protected void setURL(String privacyPolicyURL, String termsOfServiceURL) {}
}
