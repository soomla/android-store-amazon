### v2.0.6 [view commit logs](https://github.com/soomla/android-store-amazon/compare/v2.0.5...v2.0.6)

* Changes
  * Removed binaries from repo

### v2.0.3 [view commit logs](https://github.com/soomla/android-store-amazon/compare/v2.0.2...v2.0.3)

* Changes
  * Implemented stubs for changes in IIabService

### v2.0.2 [view commit logs](https://github.com/soomla/android-store-amazon/compare/v2.0.1...v2.0.2)

* New Features
  * Pass back User ID for purposes of server-side receipt verification

### v2.0.1 [view commit logs](https://github.com/soomla/android-store-amazon/compare/v2.0.0...v2.0.1)

* New Features
  * Adding missing notifyFulfillment functionality fo compliance with Amazon IAP 2.0

* Fixes
  * Fetch SKUs operation gets stuck when no skus provided

### v2.0.0 [view commit logs](https://github.com/soomla/android-store-amazon/compare/v1.0.0...v2.0.0)

*** Migrating from IAP v1.0 to IAP v2.0 ***:
  1. Add `in-app-purchasing-2.0.1.jar` instead of `in-app-purchasing-1.0.3.jar`
  1. Change `ResponseReceiver` package name in `AndroidManifest.xml` to `com.amazon.device.iap`:
  ```xml
  <receiver android:name = "com.amazon.device.iap.ResponseReceiver" >
    ...
    </receiver>
    ```

  1. Read more about [Migrating from IAP v1.0 to IAP v2.0](https://developer.amazon.com/public/apis/earn/in-app-purchasing/docs-v2/migrate-iapv1-apps-to-iapv2)
