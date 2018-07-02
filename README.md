# modifplugin
gradle plugin --更改packageid
在工程的gradle文件中添加
```gradle
buildscript {
    
    repositories {
        ....
        flatDir name: 'modifyplugin', dir: 'modifyplugin/libs'//plugin path
    }
    dependencies {
        .....
        classpath 'com.lzp.modifyplugin:modifplugin:1.0'//命名是我们的groupId：moduleName：version
    }
}
```
在model的gradle文件中添加
```gradle
apply plugin: 'com.lzp.modifyplugin'

modify{
    packageId '0x62'//0x02~0x7e之间
}

```

Resoures.arsc parse
[ArscParser](https://github.com/initLiu90/ArscParser)
