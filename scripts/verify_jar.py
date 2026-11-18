#!/usr/bin/env python3
"""Static checks on the built jar: metadata, entrypoints, mixin config, Java version.
Usage: verify_jar.py <path-to-jar>. Exits 1 if any check fails."""
import json
import struct
import sys
import zipfile

jar_path = sys.argv[1]
z = zipfile.ZipFile(jar_path)
names = set(z.namelist())
failures = []


def check(ok, text):
    print(("PASS  " if ok else "FAIL  ") + text)
    if not ok:
        failures.append(text)


print(f"### Jar check: {jar_path}\n")
check("fabric.mod.json" in names, "fabric.mod.json present")
meta = json.loads(z.read("fabric.mod.json"))
print(f"      id={meta.get('id')} version={meta.get('version')} environment={meta.get('environment')}")
check(meta.get("id") == "wallsight", "mod id is wallsight")
check("${version}" not in str(meta.get("version")), "version placeholder was expanded")
check(meta.get("environment") == "client", "environment is client (not loaded on dedicated servers)")

depends = meta.get("depends", {})
check(depends.get("minecraft") == "26.1.2", "targets Minecraft 26.1.2 exactly")
check(depends.get("java") == ">=25", "requires Java 25+")
check(depends.get("fabricloader", "").startswith(">=0.20"), "requires Fabric Loader 0.20+")
check("owo-lib" in depends and "fabric-api" in depends, "declares owo-lib and fabric-api dependencies")

classes = [n for n in names if n.endswith(".class")]
check(len(classes) > 0, f"{len(classes)} classes in jar")
bad = [n for n in classes if struct.unpack(">H", z.read(n)[6:8])[0] != 69]
check(not bad, "every class is Java 25 bytecode (class-file major version 69)")

for key, entries in meta.get("entrypoints", {}).items():
    for entry in entries:
        check(entry.replace(".", "/") + ".class" in names, f"entrypoint '{key}': {entry} exists")

for mixin in meta.get("mixins", []):
    cfg = mixin["config"] if isinstance(mixin, dict) else mixin
    check(cfg in names, f"mixin config {cfg} present")
    mj = json.loads(z.read(cfg))
    check(mj.get("compatibilityLevel") == "JAVA_25", "mixin compatibilityLevel is JAVA_25")
    for env in ("mixins", "client", "server"):
        for cls in mj.get(env, []):
            path = mj["package"].replace(".", "/") + "/" + cls.replace(".", "/") + ".class"
            check(path in names, f"mixin class {cls} ({env}) exists")

lang = json.loads(z.read("assets/wallsight/lang/en_us.json"))
check("key.wallsight.open_settings" in lang, "keybind name has a translation")
check("key.category.wallsight.general" in lang, "keybind category has a translation")

stray = [n for n in names if n.startswith(("net/minecraft/", "org/slf4j/", "io/wispforest/", "com/terraformersmc/"))]
check(not stray, "no stub or third-party classes bundled in the jar")

print()
if failures:
    print(f"{len(failures)} check(s) FAILED")
    sys.exit(1)
print("All checks passed.")
