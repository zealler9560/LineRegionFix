# LineRegionFix

> LSPosed 模块：仅作用于 LINE（`jp.naver.line.android`），把客户端能读到的设备地区信号伪装成所选地区，并记录 LINE 实际查询了哪些信号。已实测：启用后 LINE 的地区拦截报错（VrkKJ_64）消失、进入后续验证流程。注意：它只能处理客户端信号，服务器按账号注册国做的判定改不了。
>
> 成品 APK 在本仓库的 Releases 里，可直接下载安装。

## 配置界面（v3.0 起）

装好后桌面有 LineRegionFix 图标，打开就是设置页（仿 Nrfr 布局）：

- **选择 SIM 卡**：全部 SIM / SIM 1 / SIM 2（作用于带槽位参数的运营商查询）
- **选择国家码**：日本 / 韩国 / 台湾 / 香港 / 泰国 / 美国 / 德国
- **选择运营商**：随国家联动的运营商列表（MCC+MNC 写死在预设里，如日本 NTT DOCOMO 44010、au 44007、SoftBank 44020）
- 时区与语言地区随国家自动配套（如日本 = Asia/Tokyo + ja_JP）
- 「还原设置」回到默认日本；「保存生效」写入配置，强停 LINE 重开后生效

没打开过设置页时，默认就是日本 / NTT DOCOMO（与 v1、v2 行为一致）。LINE 进程读到的生效配置会打进 LSPosed 日志（`LineRegionFix: config loaded -> ...`），可据此确认配置已被读取。

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
