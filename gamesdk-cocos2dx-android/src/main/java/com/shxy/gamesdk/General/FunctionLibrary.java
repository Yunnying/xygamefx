package com.shxy.gamesdk.General;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.text.format.Formatter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.review.ReviewException;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.model.ReviewErrorCode;
import com.google.android.play.core.review.testing.FakeReviewManager;

import java.util.Locale;


public class FunctionLibrary
{
	private static String mVersionName = "";
	private static String mVersionCode = "";
	private static Activity mActivity = null;
	private static FrameLayout mFrameLayout = null;
	private static boolean mIsChromeBook = false;//是否ChromeBook，在检测时赋值
	private static final String TAG = "General";

	protected static void init(Activity activity, FrameLayout frameLayout)
	{
		mActivity = activity;
		mFrameLayout = frameLayout;
	}
	//获取版本名称和版本号
	public static void initAppVersion()
	{
		if (mActivity == null)
		{
			return;
		}

		PackageManager manager = mActivity.getPackageManager();
		PackageInfo apkInfo;
		try
		{
			apkInfo = manager.getPackageInfo(mActivity.getPackageName(), PackageManager.GET_ACTIVITIES);
			mVersionName = apkInfo.versionName;
			mVersionCode = String.format(Locale.getDefault(),"%d", apkInfo.versionCode);
		}
		catch (PackageManager.NameNotFoundException e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * 启动GooglePlay进行评分
	 */
	protected static void doRate()
	{
		if(mActivity == null)
		{
			return;
		}
		mActivity.runOnUiThread(() -> {
			String appPackageName = mActivity.getPackageName();
			try
			{
				mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
			}
			catch (android.content.ActivityNotFoundException anfe)
			{
				try
				{
					mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
				}
				catch (Exception e)
				{
					PrintLogE(String.valueOf(e.getMessage()));
				}
			}
			catch (Exception e)
			{
				PrintLogE(String.valueOf(e.getMessage()));
			}
		});
	}

	/**
	 * 应用内评分界面，暂未实装
	 */
	protected static void doRateInApp(){
		if(mActivity == null)
		{
			return;
		}
		mActivity.runOnUiThread(() -> {
			Context context = mActivity.getApplicationContext();
			ReviewManager manager = new FakeReviewManager(context);//测试用Review界面
			Task<ReviewInfo> request = manager.requestReviewFlow();
			request.addOnCompleteListener(task -> {
				if (task.isSuccessful()) {
					// We can get the ReviewInfo object
					ReviewInfo reviewInfo = task.getResult();
					Task<Void> flow = manager.launchReviewFlow(mActivity, reviewInfo);
					flow.addOnCompleteListener(task1 -> {
						Log.d(TAG, "doRateInApp: show Rateview in App success!");
					});
				} else {
					// There was some problem, log or handle the error code.
					@ReviewErrorCode int reviewErrorCode = ((ReviewException) task.getException()).getErrorCode();
				}
			});
		});
	}

	/**
	 * 唤醒分享界面
	 * @param shareText
	 */
	protected static void doShareText(final String shareText)
	{
		if(mActivity == null)
		{
			return;
		}
		mActivity.runOnUiThread(() -> {
			try
			{
				Intent intent = new Intent(Intent.ACTION_SEND);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.putExtra(Intent.EXTRA_TEXT, shareText);
				intent.putExtra(Intent.EXTRA_SUBJECT, shareText);
				intent.setType("text/plain"); // 纯文本
				mActivity.startActivity(Intent.createChooser(intent,"Share"));
			}
			catch (Exception e)
			{
				PrintLogE(String.valueOf(e.getMessage()));
			}
		});
	}

	private static String getVersionName()
	{
		return mVersionName;
	}
	private static String getVersionCode()
	{
		return mVersionCode;
	}

	//获取设备总内存(MB)
	protected static int getTotalMemory()
	{
		ActivityManager manager = (ActivityManager) mActivity.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		//设备内存
		manager.getMemoryInfo(mi);
		return (int)mi.totalMem/(1024 * 1024);
	}
	//获取设备当前可用内存(MB)
	protected static int getAvailableMemory()
	{
		ActivityManager manager = (ActivityManager) mActivity.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
		//设备内存
		manager.getMemoryInfo(mi);
		String availMem = Formatter.formatFileSize(mActivity, mi.availMem);
		PrintLogI("设备剩余运行内存 availMem: " + availMem);
		PrintLogI(String.format(Locale.getDefault(),"%d", mi.availMem));
		return (int)mi.availMem/(1024 * 1024);
	}

	private static void PrintLogI(String message)
	{
		Log.i("FuncLib Debug Log: ", message);
	}
	private static void PrintLogE(String message)
	{
		Log.e("FuncLib Error Log: ", message);
	}

	/**
	 * 信息弹窗，弹出默认样式的toast
	 * @param message toast内容
	 */
	protected static void doAlert(final String message)
	{
		if(mActivity == null)
		{
			return;
		}
		mActivity.runOnUiThread(() -> {
			Toast.makeText(mActivity, message, Toast.LENGTH_SHORT).show();
		});
	}

	/**
	 * 将text内容复制到设备的剪贴板
	 * @param text 要复制的内容
	 */
	protected static void copyToClipboard(final String text)
	{
		if(mActivity == null)
		{
			return;
		}
		mActivity.runOnUiThread(() -> {
			ClipboardManager cmb = (ClipboardManager)mActivity.getSystemService(Context.CLIPBOARD_SERVICE);
			ClipData data = ClipData.newPlainText("Label", text);
			cmb.setPrimaryClip(data);
		});
	}

	//获取设备品牌(Chromebook返回 google)
	private static String getDeviceBrand()
	{
		String deviceBrand = Build.BRAND.toLowerCase();
		PrintLogI("getDeviceBrand:" + deviceBrand);
		return deviceBrand;
	}
	//获取设备制造商(Chromebook返回 google)
	private static String getDeviceManufacturer()
	{
		String deviceManufacturer = Build.MANUFACTURER.toLowerCase();
		PrintLogI("getDeviceManufacturer:" + deviceManufacturer);
		return deviceManufacturer;
	}
	//是否Pad
	private static boolean isPad(Activity activity)
	{
		WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		DisplayMetrics dm = new DisplayMetrics();
		display.getMetrics(dm);
		double x = Math.pow(dm.widthPixels / dm.xdpi, 2);
		double y = Math.pow(dm.heightPixels / dm.ydpi, 2);
		double screenInches = Math.sqrt(x + y); // 屏幕尺寸
		PrintLogI("screenInches:" + screenInches);
		return screenInches >= 11.0;
	}
	//是否ChromeBook
	protected static boolean checkChromeBook()
	{
		mIsChromeBook = (getDeviceBrand().equals("google") && getDeviceManufacturer().equals("google") && isPad(mActivity));
		return mIsChromeBook;
	}


}