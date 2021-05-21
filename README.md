# RxCache

[![Maven Central](https://img.shields.io/maven-central/v/by.shostko/rx-cache?style=flat)](#integration) [![API-level](https://img.shields.io/badge/API-14+-blue?style=flat&logo=android)](https://source.android.com/setup/start/build-numbers) [![License](https://img.shields.io/badge/license-Apach%202.0-green?style=flat)](#license) 

Provides logic to cache stream item and stay subscribed until Lifecycle ON_DESTROY

## Integration

The library is now available in Maven Central repository:
```gradle
dependencies {
    implementation 'by.shostko:rx-cache:0.+'
}
```

Also don't forget to add mandatory dependencies:
```gradle
dependencies {
    implementation 'io.reactivex.rxjava2:rxjava:2.+' 
    implementation 'androidx.lifecycle:lifecycle-runtime:2.+'
}
```

### License

Released under the [Apache 2.0 license](LICENSE).

```
Copyright 2019 Sergey Shostko

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
