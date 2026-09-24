# Samples.AndroidStatus
Android status sample app.

---

### Set device owner

```
$ adb shell dpm set-device-owner samples.android.status.debug/samples.android.status.MainDeviceAdminReceiver
```

#### List owners

```
$ adb shell dpm list-owners
```

#### Unset device owner

```
$ adb shell dpm remove-active-admin samples.android.status.debug/samples.android.status.MainDeviceAdminReceiver
```

#### Force stop

```
$ adb shell am force-stop samples.android.status.debug
```

---
