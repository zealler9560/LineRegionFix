# LineRegionFix

> LSPosed 模块：仅作用于 LINE（`jp.naver.line.android`），把客户端能读到的设备地区信号统一伪装成日本（SIM/网络国家码 jp、运营商 44010 NTT DOCOMO、时区 Asia/Tokyo、语言地区 ja_JP），并记录 LINE 实际查询了哪些信号。已实测：启用后 LINE 的地区拦截报错（VrkKJ_64）消失、进入后续验证流程。注意：它只能处理客户端信号，服务器按账号注册国做的判定改不了。
>
> 成品 APK 在本仓库的 Releases 里，可直接下载安装。

An LSPosed module that spoofs client-side device-region signals in LINE (`jp.naver.line.android`) to Japan (JP) and logs queried signals for diagnostic analysis.

## What It Hooks & Why

LINE may gate certain features, services, or UI components based on country and region. To determine which signals LINE evaluates on-device and spoof them to Japan:

1. **`android.telephony.TelephonyManager`**:
   - `getSimCountryIso()` -> `"jp"`
   - `getNetworkCountryIso()` & `getNetworkCountryIso(int)` -> `"jp"`
   - `getSimOperator()` -> `"44010"` (NTT DOCOMO MCC+MNC)
   - `getNetworkOperator()` -> `"44010"`
   - `getSimOperatorName()` -> `"NTT DOCOMO"`
   - `getNetworkOperatorName()` -> `"NTT DOCOMO"`

2. **`android.os.SystemProperties`**:
   - `get(String)` and `get(String, String)`:
     - Keys containing `"iso-country"` or equal to `"gsm.sim.operator.iso-country"` / `"gsm.operator.iso-country"` -> `"jp"`
     - Keys equal to `"gsm.sim.operator.numeric"` / `"gsm.operator.numeric"` -> `"44010"`
     - Keys equal to `"gsm.sim.operator.alpha"` / `"gsm.operator.alpha"` -> `"NTT DOCOMO"`
     - All other keys are left completely untouched; only spoofed keys are logged.

3. **`java.util.TimeZone`**:
   - `getDefault()` -> returns `Asia/Tokyo`. (v1 only logged the real timezone; field logs showed LINE reading `Asia/Shanghai`, so v2 spoofs it for real.)

4. **`java.util.Locale`**:
   - `getDefault()` -> returns `Locale.JAPAN` (`ja_JP`). Added in v2 for the same reason.

All hooks execute inside the LINE process and record installations and first-hit invocations via `XposedBridge.log` with prefix `LineRegionFix: `.

## Honest Limitation

If LINE enforces regional restrictions server-side based on the registered account's phone country or account metadata, no client-side hook can alter the server's backend judgment. This module covers all client-side device signals that LINE can query locally, and diagnostic logs reveal exactly which telemetry and device signals LINE inspects during runtime.

## Installation & Usage

1. **Build APK**: Run `./build.sh` (requires Android SDK build-tools, platforms/android-35, and JDK).
2. **Install APK**: Install `LineRegionFix.apk` onto the target Android device.
3. **Enable in LSPosed**:
   - Open the LSPosed Manager.
   - Enable the **LineRegionFix** module.
   - Ensure the module scope is set **ONLY** to LINE (`jp.naver.line.android`).
4. **Restart LINE**:
   - Force-stop LINE: `adb shell am force-stop jp.naver.line.android`
   - Reopen LINE from the launcher.
5. **Inspect Logs**:
   - Check LSPosed logs or logcat:
     ```bash
     adb logcat -d | grep "LineRegionFix:"
     ```
   - Look for hook installation messages and first-hit spoof logs indicating which signals LINE queried.
