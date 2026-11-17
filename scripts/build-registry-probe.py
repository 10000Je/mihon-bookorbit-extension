#!/usr/bin/env python3
"""Build a separate, disposable APK to test Mihon's cached SourceFactory lifecycle."""
from pathlib import Path
import shutil
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
target = Path(tempfile.mkdtemp(prefix="bookorbit-registry-probe-"))
shutil.copytree(root, target, dirs_exist_ok=True, ignore=shutil.ignore_patterns(
    ".git", ".gradle", ".kotlin", "build", "artifacts", ".env*", "*.jks", "*.keystore",
    "*.p12", "*.pem", "*signing*.properties",
))
build = target / "app/build.gradle.kts"
original = build.read_text()
assert original.count('applicationId = "app.bookorbit.mihonext"') == 1
build.write_text(original.replace('applicationId = "app.bookorbit.mihonext"', 'applicationId = "app.bookorbit.mihonprobe"'))
manifest = target / "app/src/main/AndroidManifest.xml"
manifest.write_text(manifest.read_text().replace("BookOrbit Independent", "BookOrbit Cached Registry Probe")
                    .replace("app.bookorbit.mihonext.BookOrbitSourceFactory", "app.bookorbit.mihonext.CachedRegistryProbeFactory"))
shutil.copy2(root / "experiments/CachedRegistryProbeFactory.kt",
             target / "app/src/main/kotlin/app/bookorbit/mihonext/CachedRegistryProbeFactory.kt")
subprocess.run([str(target / "gradlew"), "-p", str(target), ":app:assembleDebug"], check=True)
print("Temporary probe APK:", target / "app/build/outputs/apk/debug/app-debug.apk")
