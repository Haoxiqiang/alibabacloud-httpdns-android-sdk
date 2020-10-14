# httpdns

请参考官网文档，了解httpdns功能。[https://help.aliyun.com/product/30100.html?spm=a2c4g.750001.list.154.2d0d7b13T0aYuX](https://help.aliyun.com/product/30100.html?spm=a2c4g.750001.list.154.2d0d7b13T0aYuX)

## 注意

请在HttpDnsConfig中配置你所使用账号的初始服务IP，否则无法请求
```java
    // HttpDnsConfig.java
    // 此处需要配置自己的初始服务IP
    static String[] SERVER_IPS = new String[]{"初始服务IP"};
    // 此处需要配置自己的初始服务IP
    static final String[] SCHEDULE_CENTER_URLS = {"初始服务IP"};
```
