#!/usr/bin/env python3
"""Generate Mihon's modern JSON store from a verified, locally signed release APK.

Contract: official mihonapp/mihon v0.21.3 NetworkExtensionStore.kt.
Requires the project's pinned Android SDK tools; never reads signing secrets.
"""

import argparse
import hashlib
import json
import math
import os
from pathlib import Path
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"
PACKAGE = "app.bookorbit.mihonext"


def require(condition, message):
    if not condition:
        raise ValueError(message)


def run(*args):
    return subprocess.check_output([str(arg) for arg in args], text=True)


def generate(args):
    require(re.fullmatch(r"[A-Za-z0-9-]+/[A-Za-z0-9_.-]+", args.repository), "Invalid owner/repository")
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    require(sdk, "Set ANDROID_HOME or ANDROID_SDK_ROOT")
    sdk = Path(sdk)
    apk = args.apk.resolve()
    require(apk.is_file(), "Release APK does not exist")
    require(re.fullmatch(r"[A-Za-z0-9_.-]+\.apk", apk.name), "Unsafe APK asset name")

    # APK v2 verification authenticates both the bytes and the manifest used below.
    verification = run(sdk / "build-tools/36.0.0/apksigner", "verify", "--verbose", "--print-certs", apk)
    fingerprints = re.findall(r"^Signer #\d+ certificate SHA-256 digest: ([a-f0-9]{64})$", verification, re.M)
    pinned = (ROOT / "repo/signing-key.sha256").read_text().strip()
    require(re.fullmatch(r"[a-f0-9]{64}", pinned), "Invalid pinned signing certificate")
    require(fingerprints == [pinned], "APK must have exactly the pinned release signer; debug/other keys are refused")
    require("Verified using v2 scheme (APK Signature Scheme v2): true" in verification, "APK v2 signature required")

    manifest = ET.fromstring(run(sdk / "cmdline-tools/latest/bin/apkanalyzer", "manifest", "print", apk))
    require(manifest.get("package") == PACKAGE, "Unexpected APK package")
    version = manifest.get(ANDROID + "versionName")
    code = int(manifest.get(ANDROID + "versionCode", "0"))
    require(version and re.fullmatch(r"\d+\.\d+\.\d+", version), "Expected numeric extension version")
    require(args.tag == "v" + version, "Release tag must equal v + APK versionName")
    require(code > 0, "Invalid APK versionCode")
    app = manifest.find("application")
    require(app is not None and app.get(ANDROID + "allowBackup") == "false", "Backup must be disabled")
    require(any(item.get(ANDROID + "name") == "tachiyomi.extension" for item in manifest.findall("uses-feature")), "Missing Mihon extension feature")
    metadata = {item.get(ANDROID + "name"): item.get(ANDROID + "value") for item in app.findall("meta-data")}
    # Some command-line tools render float metadata as a rounded raw integer
    # (e.g. 1070386368.000000). Read the typed value with pinned aapt2 instead.
    tree = run(sdk / "build-tools/36.0.0/aapt2", "dump", "xmltree", "--file", "AndroidManifest.xml", apk)
    api_match = re.search(r':name\([^)]+\)="tachiyomix\.extensionLib"[^\n]*\n\s+A: [^\n]*:value\([^)]+\)=([^\s]+)', tree)
    require(api_match, "Missing typed extension API metadata")
    api_value = api_match[1].strip('"')
    require(math.isclose(float(api_value), 1.6, rel_tol=0, abs_tol=1e-6), f"Unsupported extension API: {api_value!r}")
    print(f"Verified APK API from pinned aapt2: {api_value!r} (canonical 1.6)")
    require(metadata.get("tachiyomi.extension.class") == PACKAGE + ".BookOrbitSourceFactory", "Unexpected factory")

    # Derive the immutable slot IDs/count/language from this release's own source.
    source_root = args.source_root / "app/src/main/kotlin/app/bookorbit/mihonext"
    identity = (source_root / "model/Identity.kt").read_text()
    factory = (source_root / "BookOrbitSourceFactory.kt").read_text()
    source = (source_root / "BookOrbitSource.kt").read_text()
    base_match = re.search(r"const val BASE_SLOT_SOURCE_ID = ([\d_]+)L", identity)
    # Older release sources used a literal range; current sources share Identity.SOURCE_COUNT.
    if "(1..Identity.SOURCE_COUNT).map" in factory:
        count_match = re.search(r"const val SOURCE_COUNT = (\d+)", identity)
    else:
        count_match = re.search(r"\(1\.\.(\d+)\)\.map", factory)
    lang_match = re.search(r'override val lang = "([a-z]+)"', source)
    require(base_match and count_match and lang_match, "Source layout changed: review feed generation")
    require("return BASE_SLOT_SOURCE_ID + slot" in identity, "Source ID policy changed: review feed generation")
    base_id = int(base_match[1].replace("_", ""))
    count = int(count_match[1])
    require(count in (3, 6) and lang_match[1] == "all", "Slot/language policy changed: review feed generation")
    if "Identity.sourceName(slot)" in source:
        require(count == 3 and 'listOf("I", "II", "III")' in identity, "Source naming policy changed: review feed generation")
        source_names = ["BookOrbit I", "BookOrbit II", "BookOrbit III"]
    else:
        source_names = [f"BookOrbit · 슬롯 {slot}" for slot in range(1, count + 1)]
    warnings = {"0": "UNSPECIFIED", "1": "SAFE", "2": "MIXED", "3": "NSFW"}
    content_warning = warnings[metadata["tachiyomix.contentWarning"]]

    owner, repository = args.repository.split("/")
    website = "https://github.com/" + args.repository
    site = f"https://{owner.lower()}.github.io/{repository}"
    extension = {
        "name": metadata["tachiyomix.name"],
        "packageName": PACKAGE,
        "resources": {
            "apkUrl": f"{website}/releases/download/{args.tag}/{apk.name}",
            "iconUrl": site + "/icon.png",
        },
        "extensionLib": "1.6",
        "versionCode": code,
        "versionName": version,
        "contentWarning": content_warning,
        "sources": [
            {"id": base_id + slot, "name": source_names[slot - 1], "language": lang_match[1], "homeUrl": "", "mirrorUrls": [], "message": None}
            for slot in range(1, count + 1)
        ],
    }
    store = {
        "name": metadata["tachiyomix.name"] + " for Mihon",
        "badgeLabel": "BookOrbit",
        "signingKey": pinned,
        "contact": {"website": website, "discord": None},
        "extensionList": {"extensions": [extension]},
        "extensionListUrl": None,
    }
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    (output / "repo.json").write_text(json.dumps(store, ensure_ascii=False, indent=2) + "\n")
    (output / "index.json").write_text(json.dumps(store["extensionList"], ensure_ascii=False, indent=2) + "\n")
    (output / "SHA256SUMS").write_text(hashlib.sha256(apk.read_bytes()).hexdigest() + "  " + apk.name + "\n")
    (output / ".nojekyll").touch()
    icon = ROOT / "repo/icon.png"
    if icon.resolve() != (output / "icon.png").resolve():
        shutil.copyfile(icon, output / "icon.png")
    print(f"Verified release {version} (code {code}), {count} fixed sources; generated {output / 'repo.json'}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--repository", required=True)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--output", type=Path, default=ROOT / "repo")
    parser.add_argument("--source-root", type=Path, default=ROOT, help="Checkout of the exact release tag (default: this project)")
    try:
        generate(parser.parse_args())
    except (ValueError, KeyError, OSError, subprocess.CalledProcessError) as error:
        parser.exit(1, f"Feed generation failed: {error}\n")
