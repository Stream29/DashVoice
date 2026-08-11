# Task Tree

- [done] Migrate DashVoice to a pure Compose application
  - [done] Determine an IDEA-compatible Compose configuration
  - [done] Downgrade AGP to the IDEA-supported version
  - [done] Configure Compose plugins and dependencies
    - [done] Add Compose versions and aliases
    - [done] Enable Compose in the application module
  - [done] Move Kotlin code out of the Java source directory
    - [done] Relocate application sources to `src/main/kotlin`
    - [done] Remove the empty Java source directory
  - [done] Replace the View-based activity with Compose UI
    - [done] Implement the Compose application content
    - [done] Implement the Compose Material theme
  - [done] Verify the build, lint, and source layout
    - [done] Build and lint with the existing JDK 17 daemon
    - [done] Check IDE analysis and AGP compatibility
    - [done] Confirm the Java source directory is absent

# Details

Use Jetpack Compose for the application UI and keep Kotlin sources under `app/src/main/kotlin`. Remove the `app/src/main/java` source directory.

IntelliJ IDEA 2026.2.1 reports AGP 9.1.0 as its latest supported version. Use AGP 9.1.0, its built-in Kotlin 2.2.10 toolchain, the matching Compose compiler plugin 2.2.10, Compose BOM 2026.06.01, and Activity Compose 1.13.0.

`assembleDebug` and `lintDebug` pass with JDK 17. The lint report contains only available-version notices. The IDEA project build also passes after pinning the Gradle daemon criteria to JDK 17. Kotlin sources live exclusively under `app/src/main/kotlin`.
