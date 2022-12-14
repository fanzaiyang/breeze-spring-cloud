---
title: 快速开始
order: 1
---

## 背景

在日常开发过程中，时常发现有一些简单的功能会被经常使用到，又没有一个比较好用的功能集合，因此在开发项目是需要反复配置，造成了大量不必要的重复性简单劳动，所以在从过往经验的基础上对日常使用到功能进行了一个通用封装，方便后期项目维护及开发。

## 微风Cloud

**微风Cloud**是基于SpringCloud开发的，一系列实用微服务业务组件库。

**微风Cloud**开发遵循以下原则：

* 开箱即用：开发者可以自由选择仅仅使用到功能，关闭不需要的功能。
* 简单配置：在开启组件功能后，无需进行复杂配置即可使用组件的基本功能。
* 个性定制：每个组件提供大量额外配置，自定义组件复杂功能。
* 高级扩展：每个组件都可以被重写，已实现个性化需求。

## 版本管理

1. 微风Cloud依赖版本

| 依赖                                                                      | 版本         |
| ----------------------------------------------------------------------- | ---------- |
| [spring-boot](https://spring.io/projects/spring-boot)                   | 2.7.7      |
| [spring-cloud](https://spring.io/projects/spring-cloud)                 | 2021.0.5   |
| [spring-cloud-alibaba](https://spring.io/projects/spring-cloud-alibaba) | 2021.0.1.0 |
| [spring-boot-admin](https://github.com/codecentric/spring-boot-admin)   | 2.6.9      |
| [hutool-all](https://gitee.com/dromara/hutool/)                         | 5.8.10     |

2. 微风Cloud版本管理

微风Cloud组件使用**统一的版本管理**策略，每个子组件继承父级组件版本。

# 组件列表

![GitHub](https://img.shields.io/github/license/fanzaiyang/breeze-spring-cloud.png) ![GitHub release (latest by date)](https://img.shields.io/github/v/release/fanzaiyang/breeze-spring-cloud.png) ![Maven Central](https://img.shields.io/maven-central/v/cn.fanzy.breeze/breeze-spring-cloud.png)

| 名称                                       | 说明                                                                             |
| ---------------------------------------- | ------------------------------------------------------------------------------ |
| breeze-core                              | 微风Cloud组件库的核心模块，封装了常用工具类等。                                                     |
| breeze-web-spring-boot-starter           | web组件：封装了异常、缓存、验证码、过滤器等。                                                       |
| breeze-minio-spring-boot-starter         | MinIO组件：封装了[minio](https://min.io/)对象存储功能，方便在项目任何位置，使用。                        |
| breeze-auth-spring-boot-starter          | 授权组件：对[sa-token](https://sa-token.dev33.cn/doc/index.html#/)二次封装，一个简单易上手的授权组件。 |
| breeze-auth-redis                        | 让授权写入到redis及redisson支持。                                                        |
| breeze-auth-jwt-spring-boot-starter      | 授权支持jwt模式                                                                      |
| breeze-admin-spring-boot-starter         | 集中常用登录与系统管理的功能                                                                 |
| + breeze-wechat                          | 微信相关组件，帮助开发者快速集成微信相关接口功能                                                       |
| -  breeze-wechat-cp-spring-boot-starter  | 企业微信                                                                           |
| -  breeze-wechat-ma-spring-boot-starter  | 微信小程序                                                                          |
| -  breeze-wechat-mp-spring-boot-starter  | 微信公众号                                                                          |
| -  breeze-wechat-pay-spring-boot-starter | 微信支付                                                                           |

# 组件使用说明

2. 替换pom中的parent **「推荐」**
   * 最新版本号：![GitHub release (latest by date)](https://img.shields.io/github/v/release/fanzaiyang/breeze-spring-cloud.png)

```xml
<parent>
    <groupId>cn.fanzy.breeze</groupId>
    <artifactId>breeze-spring-cloud</artifactId>
    <version>最新版本号</version>
    <relativePath/> <!-- lookup parent from repository -->
</parent>
```

> Tips：使用这个方式，你不需要指定组件的版本号。锁定依赖版本。

3. 以web组件`breeze-web-spring-boot-starter`为例，在`pom.xml`配置如下：

```xml
<!-- pom.xml -->
<dependency>
    <groupId>cn.fanzy.breeze</groupId>
    <artifactId>breeze-web-spring-boot-starter</artifactId>
</dependency>
```

4. 添加配置

微风Cloud组件的配置文件，都以`breeze`开头。以`breeze-web-spring-boot-starter`组件为例，关闭全局异常捕获功能，只需要在配置文件中，作如下配置：

```yaml
# 关闭web全局异常捕获，默认：true
breeze:
  web:
    exception: 
      enable: true
```

组件的详细说明，在各组件介绍中，会详细说明，请阅读对应章节。
