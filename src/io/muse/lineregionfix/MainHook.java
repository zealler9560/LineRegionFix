package io.muse.lineregionfix;

import android.telephony.TelephonyManager;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LineRegionFix: LSPosed module that spoofs LINE's client-side device-region signals.
 * The target region/carrier are configurable from the module's settings UI
 * (stored in SharedPreferences "config", read here via XSharedPreferences);
 * defaults are Japan / NTT DOCOMO when nothing has been configured.
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "LineRegionFix";
    private static final String TARGET_PKG = "jp.naver.line.android";
    private static final String MODULE_PKG = "io.muse.lineregionfix";

    private static XSharedPreferences prefs;

    private static final Set<String> LOGGED_HITS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private static void logFirstHit(String key, String message) {
        if (LOGGED_HITS.add(key)) {
            XposedBridge.log(TAG + ": " + message);
        }
    }

    // ---------- config ----------
    private static XSharedPreferences prefs() {
        if (prefs == null) {
            try {
                prefs = new XSharedPreferences(MODULE_PKG, SettingsActivity.PREFS_NAME);
                prefs.makeWorldReadable();
            } catch (Throwable t) {
                XposedBridge.log(TAG + ": failed to open XSharedPreferences: " + t.getMessage());
            }
        } else {
            try {
                prefs.reload();
            } catch (Throwable ignored) {
            }
        }
        return prefs;
    }

    private static String cfg(String key, String def) {
        try {
            XSharedPreferences p = prefs();
            if (p != null) {
                String v = p.getString(key, null);
                if (v != null && v.length() > 0) {
                    return v;
                }
            }
        } catch (Throwable ignored) {
        }
        return def;
    }

    private static String cfgIso() {
        return cfg(SettingsActivity.KEY_ISO, RegionPresets.REGIONS[0].iso);
    }

    private static String cfgNumeric() {
        return cfg(SettingsActivity.KEY_NUMERIC, RegionPresets.REGIONS[0].carriers[0].numeric);
    }

    private static String cfgAlpha() {
        return cfg(SettingsActivity.KEY_ALPHA, RegionPresets.REGIONS[0].carriers[0].alpha);
    }

    private static String cfgTz() {
        return cfg(SettingsActivity.KEY_TZ, RegionPresets.REGIONS[0].timezoneId);
    }

    private static Locale cfgLocale() {
        String tag = cfg(SettingsActivity.KEY_LOCALE, RegionPresets.REGIONS[0].localeTag);
        try {
            String[] parts = tag.split("_");
            if (parts.length == 2) {
                return new Locale(parts[0], parts[1]);
            }
        } catch (Throwable ignored) {
        }
        return Locale.JAPAN;
    }

    private static int cfgSimScope() {
        try {
            XSharedPreferences p = prefs();
            if (p != null) {
                return p.getInt(SettingsActivity.KEY_SIM_SCOPE, 0);
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    /** SIM scope filter for the int-arg overload: arg is treated as the slot index. */
    private static boolean slotAllowed(Object arg) {
        int scope = cfgSimScope();
        if (scope == 0) {
            return true;
        }
        if (arg instanceof Integer) {
            int idx = (Integer) arg;
            return scope == 1 ? idx == 0 : idx == 1;
        }
        return true;
    }

    private static String getSpoofedSystemProperty(String key) {
        if (key == null) {
            return null;
        }
        if (key.contains("iso-country")
                || "gsm.sim.operator.iso-country".equals(key)
                || "gsm.operator.iso-country".equals(key)) {
            return cfgIso();
        }
        if ("gsm.sim.operator.numeric".equals(key) || "gsm.operator.numeric".equals(key)) {
            return cfgNumeric();
        }
        if ("gsm.sim.operator.alpha".equals(key) || "gsm.operator.alpha".equals(key)) {
            return cfgAlpha();
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
        XposedBridge.log(TAG + ": config loaded -> iso=" + cfgIso()
                + ", numeric=" + cfgNumeric() + ", alpha=" + cfgAlpha()
                + ", tz=" + cfgTz() + ", locale=" + cfg(SettingsActivity.KEY_LOCALE,
                RegionPresets.REGIONS[0].localeTag) + ", simScope=" + cfgSimScope());

        hookTelephonyManager();
        hookSystemProperties(lpparam.classLoader);
        hookTimeZone();
        hookLocale();

        XposedBridge.log(TAG + ": all hooks installed in " + lpparam.packageName);
    }

    private interface ValueSupplier {
        String get();
    }

    private void hookTelephonyManager() {
        hookTelephonyNoArg("getSimCountryIso", () -> cfgIso());
        hookTelephonyNoArg("getNetworkCountryIso", () -> cfgIso());
        hookTelephonyIntArg("getNetworkCountryIso", () -> cfgIso());
        hookTelephonyNoArg("getSimOperator", () -> cfgNumeric());
        hookTelephonyNoArg("getNetworkOperator", () -> cfgNumeric());
        hookTelephonyNoArg("getSimOperatorName", () -> cfgAlpha());
        hookTelephonyNoArg("getNetworkOperatorName", () -> cfgAlpha());
    }

    private void hookTelephonyNoArg(final String methodName, final ValueSupplier supplier) {
        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, methodName, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    String v = supplier.get();
                    param.setResult(v);
                    logFirstHit("TelephonyManager." + methodName,
                            "TelephonyManager." + methodName + "() -> " + v);
                }
            });
            XposedBridge.log(TAG + ": hooked TelephonyManager." + methodName + "()");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook TelephonyManager." + methodName + "(): " + t.getMessage());
        }
    }

    private void hookTelephonyIntArg(final String methodName, final ValueSupplier supplier) {
        try {
            XposedHelpers.findAndHookMethod(TelephonyManager.class, methodName, int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Object slotArg = (param.args != null && param.args.length > 0) ? param.args[0] : "?";
                    if (!slotAllowed(slotArg)) {
                        logFirstHit("TelephonyManager." + methodName + "(int)-skip" + slotArg,
                                "TelephonyManager." + methodName + "(" + slotArg + ") skipped by SIM scope");
                        return;
                    }
                    String v = supplier.get();
                    param.setResult(v);
                    logFirstHit("TelephonyManager." + methodName + "(int)",
                            "TelephonyManager." + methodName + "(" + slotArg + ") -> " + v);
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
                    String tz = cfgTz();
                    param.setResult(TimeZone.getTimeZone(tz));
                    logFirstHit("TimeZone.getDefault",
                            "TimeZone.getDefault() -> " + tz);
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
                    Locale loc = cfgLocale();
                    param.setResult(loc);
                    logFirstHit("Locale.getDefault",
                            "Locale.getDefault() -> " + loc);
                }
            });
            XposedBridge.log(TAG + ": hooked Locale.getDefault()");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to hook Locale.getDefault(): " + t.getMessage());
        }
    }
}
