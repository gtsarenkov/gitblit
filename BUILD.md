# Modern build instruction

The current work is intended to migrate Ant/Moxie build into Gradle. Neither Ant/Moxie is good choice for build system of modern application.

## Ant/Moxie under Windows with JDK 21
A temporaly improvement for Ant/Moxie is mode in the forked Moxie repository to support compilation with JDK 21.
Build and install "moxie" from forked repository modernized for JDK 21. Install "moxie+ant" from `<moxie-sources-root>\moxie+ant\build\target\moxie+ant-0.10.1-SNAPSHOT.zip` into dedicated folder.
Then build gitblit with "moxie":

```
set JDK_JAVA_OPTIONS=""--add-opens=java.base/java.lang=ALL-UNNAMED"" ""--add-opens=java.base/java.net=ALL-UNNAMED""
<moxie-0.10.1-SNAPSHOT>\bin\moxie.bat buildAll >build-ant-buildAll.log 2>&1
```

## Gradle

By default, dependencies use Maven Central and plugins use the Gradle Plugin Portal. Existing local Maven and Moxie repositories remain available.

To use the optional Nexus cache, copy `nexus.properties.example` to `nexus.properties` in the project root. This local file is ignored by Git. Set its base URL as follows:

```properties
nexusUrl=http://172.16.0.104:8085
```

Remove the local file or leave `nexusUrl` empty to use public repositories. Configuration precedence is `-PnexusUrl`, then the `NEXUS_URL` environment variable, then `nexus.properties`. An empty Gradle property disables Nexus even if the environment or local file enables it. Both the main build and the included class-dependencies build read the same local file. Nexus must expose `repository/maven-public/` and `repository/gradle-plugins/`.
