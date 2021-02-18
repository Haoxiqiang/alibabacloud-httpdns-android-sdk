# httpdns

请参考官网文档，了解httpdns功能。[https://help.aliyun.com/product/30100.html?spm=a2c4g.750001.list.154.2d0d7b13T0aYuX](https://help.aliyun.com/product/30100.html?spm=a2c4g.750001.list.154.2d0d7b13T0aYuX)

## 注意

### 配置初始IP
请在gradle.properties中配置你所使用账号的初始服务IP，否则无法请求
```gradle
// InputYourInitServerIp 替换为启动IP, 有几个就放几个
INIT_SERVERS={"InputYourInitServerIp", "InputYourInitServerIp", "InputYourInitServerIp"}
```
### 编译项目
如果编译报如下错误
```gradle
Could not find :alicloud-android-httpdns-0.0.0.1-local-SNAPSHOT-end2end-forTest:.
```
请先执行
```
./gradlew copyAARForApp
```

### 运行测试case
```
// end2end test
./gradlew clean :app:testDebugUnitTest

// unit test
./gradlew clean :httpdns-sdk:testEnd2endForTestUnitTest
```

### 打包
需要先修改gradle.properties的INIT_SERVERS配置，然后执行如下命令打包
```
// 2.0.0 为 版本号
./gradlew clean :httpdns-sdk:assembleNormalRelease -PVERSION_NAME=2.0.0
```