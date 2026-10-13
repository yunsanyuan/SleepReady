# Gradle Wrapper

`gradlew` / `gradlew.bat`、`gradle-wrapper.properties` 和 `gradle-wrapper.jar` 都应提交进仓库。  
贡献者（以及 GitHub Actions）只靠 Wrapper 就能构建，不必预先安装 Gradle。

若本地缺失 `gradle-wrapper.jar`，在已安装 Gradle 8.4+ 的环境执行：

```sh
gradle wrapper --gradle-version 8.4
```

对应发行版见 `gradle-wrapper.properties` 里的 `distributionUrl`。
