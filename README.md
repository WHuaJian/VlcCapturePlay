# Vlc 网络播放器，支持截屏，支持rtsp、rtmp、udp等流媒体地址

## 用法
```Java
     Intent intent = new Intent(this,VlcPlayActivity.class);
     intent.putExtra(VlcPlayActivity.URL, "rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov");
     startActivity(intent);
```

## 引入方式
```
   implementation 'com.whj.vlc:VlcCapturePlayer:1.0.0'

```
如果无法引用，在app目录build.gradle添加
```
   repositories {
       maven { url "https://dl.bintray.com/whj/Maven" }
   }

```


## License
```
 Copyright 2019, WHuaJian

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
