AndroidStudio离线运行方案

1.将本库中的.gradle文件夹放入和studio同级文件夹

2.在studio\bin\idea.properties文件开头添加如下3行
idea.config.path=${idea.home.path}/../.AndroidStudio/config
idea.system.path=${idea.home.path}/../.AndroidStudio/system
gradle.user.home=${idea.home.path}/../.gradle

3.在有网环境新建项目，运行项目，待项目可完全运行后，运行updateCache

4.将studio SDK .gradle .m2 .AndroidStudio 5个目录拷贝到离线电脑。

done
