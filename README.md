# httpdns

请参考官网文档，了解httpdns功能。[https://help.aliyun.com/product/30100.html?spm=a2c4g.750001.list.154.2d0d7b13T0aYuX](https://help.aliyun.com/product/30100.html?spm=a2c4g.750001.list.154.2d0d7b13T0aYuX)

## 注意

### 配置初始IP
请在gradle.properties中配置你所使用账号的初始服务IP，否则无法请求
```gradle
// InputYourInitServerIp 替换为启动IP, 有几个就放几个
INIT_SERVERS={"InputYourInitServerIp", "InputYourInitServerIp", "InputYourInitServerIp"}
```

### 运行测试case

```
// end2end test
./gradlew clean :app:testEnd2endForTestUnitTest

// unit test
./gradlew clean :httpdns-sdk:testEnd2endForTestUnitTest
```

## 感谢
本项目中Inet64Util工具类 由[Shinelw](https://github.com/Shinelw)贡献支持，感谢。