# Yumi Gradle Licenser Changelog

## 1.0.0 - Initial Release

- Initial release.
- Added support for JVM-based Gradle projects.
- Added basic license header syntax.
- Added license header application and check tasks.

## 1.1.0

- Added missing support for the Kotlin multiplatform plugin ([#1](https://github.com/YumiProject/yumi-gradle-licenser/issues/1)).
- Improved functional tests.

## 1.1.1

- Fixed Kotlin multiplatform support when the plugin is applied after the licenser ([#1](https://github.com/YumiProject/yumi-gradle-licenser/issues/1#issuecomment-1931569894)).

## 1.1.2

- Fixed default inclusion of build directories for verification ([#2](https://github.com/YumiProject/yumi-gradle-licenser/issues/2)).

## 1.2.0

- Added support for XML-style comments, including a default configuration ([#3]).
- Added some extra default file-types for C-style header comments,
  specifically for common web languages and stylesheet formats ([#3]).

[#3]: https://github.com/YumiProject/yumi-gradle-licenser/pull/3
