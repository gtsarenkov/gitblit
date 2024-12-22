# Modern build instruction

The current work is intended to migrate Ant/Moxie build into Gradle. Neither Ant/Moxie is good choice for build system of modern application.

## Adopted Technologies
- JDK 21
- Gradle 8.x
- Forked Ant/Moxie 0.10.0 can be used to run build before full build support with Gradle.

## Build
A temporary improvement for Ant/Moxie is made in the [forked Moxie repository](https://github.com/gtsarenkov/moxie) to support compilation with JDK 21.
Build and install "moxie" from forked repository modernized for JDK 21. Install "moxie+ant" from `<moxie-sources-root>\moxie+ant\build\target\moxie+ant-0.10.1-SNAPSHOT.zip` into dedicated folder.
Then build gitblit with "moxie":

```
set JDK_JAVA_OPTIONS=""--add-opens=java.base/java.lang=ALL-UNNAMED"" ""--add-opens=java.base/java.net=ALL-UNNAMED"" ""--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED""
<moxie-0.10.1-SNAPSHOT>\bin\moxie.bat buildAll >build-ant-buildAll.log 2>&1
```

## Gradle

TBD