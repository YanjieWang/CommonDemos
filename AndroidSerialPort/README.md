## AndroidSerialPort

[![Join the chat at https://gitter.im/jp1017/AndroidSerialPort](https://badges.gitter.im/jp1017/AndroidSerialPort.svg)](https://gitter.im/jp1017/AndroidSerialPort?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge) [![Build Status](https://travis-ci.org/jp1017/AndroidSerialPort.svg?branch=master)](https://travis-ci.org/jp1017/AndroidSerialPort) [![GitHub stars](https://img.shields.io/github/stars/jp1017/AndroidSerialPort.svg?style=social&label=Star&maxAge=10800)](https://github.com/jp1017/AndroidSerialPort)

安卓串口相关操作

<img src="https://cloud.githubusercontent.com/assets/7868514/13865849/dee51aa8-ecea-11e5-99cd-0d8efa532314.png" width="320" />

## 测试

需要自己开发板，现在一般手机都没有预留串口

该程序固定了串口为 `/dev/ttyAMA0`，自己可看源码更改

或者看另一个项目：https://github.com/jp1017/AndroidSerialPortSample

## 来源

[Android串口操作，简化android-serialport-api的demo（附源码）](http://blog.csdn.net/akunainiannian/article/details/8740007)

我把文中的源码导入 `android studio`

源码来自谷歌：[android-serialport-api](https://code.google.com/archive/p/android-serialport-api/)

涉及到的 jni 开发可以参考：[【Android 应用开发】Android 开发 之 JNI入门 - NDK从入门到精通](http://blog.csdn.net/shulianghan/article/details/18964835)

jni 开发遇到的坑：https://github.com/jp1017/Android-Collection/issues/10

## 20171011更新

刚刚发现另一个库: https://github.com/felHR85/UsbSerial
