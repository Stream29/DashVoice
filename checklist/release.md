# Release

- Review the exact source diff, tests, and known unresolved issues before release.
- Increase `versionCode` for a new release and set the intended `versionName`.
- Detect the available Gradle daemon JVM and explicitly select the compatible
  Java 17 runtime; do not compete with other builds. Limit worker concurrency.
- Run debug unit tests and release assembly; inspect the actual test reports
  and release output metadata rather than relying on old build artifacts.
- Keep signing keys, passwords, local settings, backups, and generated APKs out
  of Git. Use the established signing identity unless key rotation is separately
  approved; verify the signed APK's package, version, certificate, and alignment.
- Commit and push reviewed source before creating an annotated release tag.
- Ensure the tag points to the tested source commit; never replace an existing
  published tag or asset silently.
- Attach the signed APK and a matching SHA-256 sidecar to the release. Validate
  draft assets before publishing and verify the published download afterward.
- Distinguish public release from local device deployment. Do not install on a
  device or declare unresolved input-method defects fixed merely to finish a release.
- Update owning submodule pointers from the innermost repository outward, push
  each referenced commit before its parent, and verify recursive checkout.
