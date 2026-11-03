package io.muse.lineregionfix;

import android.telephony.TelephonyManager;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LineRegionFix: LSPosed module that spoofs LINE's client-side device-region signals
 * to Japan (JP) and logs queried signals for diagnostic observation.
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "LineRegionFix";
    private static final String TARGET_PKG = "jp.naver.line.android";

    private static final String SPOOF_COUNTRY_ISO = "jp";
    private static final String SPOOF_OPERATOR_NUMERIC = "44010";
    private static final String SPOOF_OPERATOR_ALPHA = "NTT DOCOMO";

    private static final Set<String> LOGGED_HITS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private static void logFirstHit(String key, String message) {
        if (LOGGED_HITS.add(key)) {
            XposedBridge.log(TAG + ": " + message);
        }
    }

    private static String getSpoofedSystemProperty(String key) {
        if (key == null) {
            return null;
        }
        if (key.contains("iso-country")
                || "gsm.sim.operator.iso-country".equals(key)
                || "gsm.operator.iso-country".equals(key)) {
            return SPOOF_COUNTRY_ISO;
        }
        if ("gsm.sim.operator.numeric".equals(key) || "gsm.operator.numeric".equals(key)) {
            return SPOOF_OPERATOR_NUMERIC;
        }
        if ("gsm.sim.operator.alpha".equals(key) || "gsm.operator.alpha".equals(key)) {
            return SPOOF_OPERATOR_ALPHA;
        }
        return null;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam == null || !TARGET_PKG.equals(lpparam.packageName)) {
            return;
        }

        XposedBridge.log(TAG + ": installing hooks in " + lpparam.packageName
                + " (process: " + lpparam.processName + ")");

        hookTelephonyManager();
        hookSystemProperties(lpparam.classLoader);
        hookTimeZone();
        hookLocale();

        XposedBridge.log(TAG + ": all hooks installed in " + lpparam.packageName);
    }

    private void hookTelephonyManager() {
        hookTelephonyNoArg("getSimCountryIso", SPOOF_COUNTRY_ISO);
        hookTelephonyNoArg("getNetworkCountryIso", SPOOF_COUNTRY_ISO);
        hookTelephonyIntArg("getNetworkCountryIso", SPOOF_COUNTRY_ISO);
        hookTelephonyNoArg("getSimOperator", SPOOF_OPERATOR_NUMERIC);
        hookTelephonyNoArg("getNetworkOperator", SPOOF_OPERATOR_NUMERIC);
        hookTelephonyNoArg("getSimOperatorName", SPOOF_OPERATOR_ALPHA);
        hookTelephonyNoArg("getNetworkOperatorName", SPOOF_OPERATOR_ALPHA);
    }

    private void hookTelephonyNoArg(final String methodName, final String spoofedValue) {
        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, methodName, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(spoofedValue);
                    logFirstHit("TelephonyManager." + methodName,
                            "TelephonyManager." + methodName + "() -> " + spoofedValue);
                }
            });
            XposedBridge.log(TAG + ": hooked TelephonyManager." + methodName + "()");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook TelephonyManager." + methodName + "(): " + t.getMessage());
        }
    }

    private void hookTelephonyIntArg(final String methodName, final String spoofedValue) {
        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, methodName, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(spoofedValue);
                    Object slotArg = (param.args != null && param.args.length > 0) ? param.args[0] : "?";
                    logFirstHit("TelephonyManager." + methodName + "(int)",
                            "TelephonyManager." + methodName + "(" + slotArg + ") -> " + spoofedValue);
                }
            });
            XposedBridge.log(TAG + ": hooked TelephonyManager." + methodName + "(int)");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook TelephonyManager." + methodName + "(int): " + t.getMessage());
        }
    }

    private void hookSystemProperties(ClassLoader classLoader) {
        Class<?> sysPropClass;
        try {
            sysPropClass = XposedHelpers.findClass("android.os.SystemProperties", classLoader);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to find android.os.SystemProperties: " + t.getMessage());
            return;
        }

        try {
            XposedHelpers.findAndHookMethod(sysPropClass, "get", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length < 1 || !(param.args[0] instanceof String)) {
                        return;
                    }
                    String key = (String) param.args[0];
                    String spoofed = getSpoofedSystemProperty(key);
                    if (spoofed != null) {
                        param.setResult(spoofed);
                        logFirstHit("SystemProperties.get:" + key,
                                "SystemProperties.get(\"" + key + "\") -> " + spoofed);
                    }
                }
            });
            XposedBridge.log(TAG + ": hooked SystemProperties.get(String)");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook SystemProperties.get(String): " + t.getMessage());
        }

        try {
            XposedHelpers.findAndHookMethod(sysPropClass, "get", String.class, String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length < 1 || !(param.args[0] instanceof String)) {
                        return;
                    }
                    String key = (String) param.args[0];
                    String spoofed = getSpoofedSystemProperty(key);
                    if (spoofed != null) {
                        param.setResult(spoofed);
                        logFirstHit("SystemProperties.get_def:" + key,
                                "SystemProperties.get(\"" + key + "\", def) -> " + spoofed);
                    }
                }
            });
            XposedBridge.log(TAG + ": hooked SystemProperties.get(String, String)");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook SystemProperties.get(String, String): " + t.getMessage());
        }
    }

    private void hookTimeZone() {
        try {
            XposedHelpers.findAndHookMethod(TimeZone.class, "getDefault", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(TimeZone.getTimeZone("Asia/Tokyo"));
                    logFirstHit("TimeZone.getDefault",
                            "TimeZone.getDefault() -> Asia/Tokyo");
                }
            });
            XposedBridge.log(TAG + ": hooked TimeZone.getDefault()");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook TimeZone.getDefault(): " + t.getMessage());
        }
    }

    private void hookLocale() {
        try {
            XposedHelpers.findAndHookMethod(Locale.class, "getDefault", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    param.setResult(Locale.JAPAN);
                    logFirstHit("Locale.getDefault",
                            "Locale.getDefault() -> ja_JP");
                }
            });
            XposedBridge.log(TAG + ": hooked Locale.getDefault()");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook Locale.getDefault(): " + t.getMessage());
        }
    }
}
