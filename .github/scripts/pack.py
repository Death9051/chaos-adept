#!/usr/bin/env python3
"""Состав сборки с Modrinth: разрешение версий, lock-файл, скачивание.

  pack.py resolve           — по pack/mods.txt выбрать версии (с зависимостями), записать pack/modrinth.lock.json
                              и отчёт pack-report.md (версии, лицензии, стороны, зависимости)
  pack.py check             — lock соответствует mods.txt? (код 1, если нет)
  pack.py fetch --side client|server --dest DIR [--skip-dev] [--with-shaders] [--cache DIR]
                            — скачать файлы из lock в DIR/mods и DIR/shaderpacks, сверить sha512

Lock повторяет поля modrinth.index.json (path, hashes, downloads, fileSize, env), чтобы
из него потом собрать .mrpack без пересчёта.
"""
import argparse
import hashlib
import json
import os
import shutil
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

API = "https://api.modrinth.com/v2"
UA = "Death9051/chaos-adept (modpack CI)"
MC = "1.21.1"
LOADER = "neoforge"
SHADER_LOADERS = ["iris"]
ROOT = Path(__file__).resolve().parents[2]
SPEC = ROOT / "pack" / "mods.txt"
LOCK = ROOT / "pack" / "modrinth.lock.json"


def get(path, params=None):
    url = API + path
    if params:
        url += "?" + urllib.parse.urlencode(params)
    for attempt in range(5):
        req = urllib.request.Request(url, headers={"User-Agent": UA})
        try:
            with urllib.request.urlopen(req, timeout=30) as r:
                return json.load(r)
        except urllib.error.HTTPError as e:
            if e.code == 404:
                return None
            if e.code == 429 or e.code >= 500:
                time.sleep(2 * (attempt + 1))
                continue
            raise
        except urllib.error.URLError:
            time.sleep(2 * (attempt + 1))
    raise RuntimeError("Modrinth не отвечает: " + url)


def read_spec():
    entries = []
    for raw in SPEC.read_text(encoding="utf-8").splitlines():
        line = raw.split("#", 1)[0].strip()
        if not line:
            continue
        parts = line.split()
        kind, slug = parts[0], parts[1]
        rest = parts[2:]
        flags = {p for p in rest if p in ("dev",)}
        pins = [p for p in rest if p not in flags and p != "-"]
        if kind not in ("mod", "shader"):
            sys.exit(f"mods.txt: неизвестный вид «{kind}» в строке: {raw}")
        entries.append({"kind": kind, "slug": slug, "pin": pins[0] if pins else None, "flags": sorted(flags)})
    return entries


def spec_hash():
    return hashlib.sha256(SPEC.read_bytes()).hexdigest()


def loaders_for(kind):
    return SHADER_LOADERS if kind == "shader" else [LOADER]


def compatible(version, kind):
    return MC in version.get("game_versions", []) and any(l in version.get("loaders", []) for l in loaders_for(kind))


def pick_version(project_id, kind, pin=None):
    versions = get(f"/project/{project_id}/version", {
        "loaders": json.dumps(loaders_for(kind)),
        "game_versions": json.dumps([MC]),
    }) or []
    versions = [v for v in versions if compatible(v, kind)]
    if pin:
        for v in versions:
            if pin in (v["id"], v["version_number"]):
                return v, None
        return None, f"версия {pin} не найдена среди сборок {'/'.join(loaders_for(kind))} {MC}"
    if not versions:
        return None, f"нет сборок {'/'.join(loaders_for(kind))} для {MC}"
    releases = [v for v in versions if v["version_type"] == "release"]
    return (releases or versions)[0], None


def primary_file(version):
    files = version["files"]
    for f in files:
        if f.get("primary"):
            return f
    return files[0]


def side(value):
    return "unsupported" if value == "unsupported" else ("required" if value == "required" else "optional")


def resolve():
    spec = read_spec()
    chosen = {}      # project_id -> запись
    problems = []
    notes = []
    queue = []
    for e in spec:
        project = get(f"/project/{e['slug']}")
        if not project:
            problems.append(f"{e['slug']}: проект не найден на Modrinth")
            continue
        queue.append((project, e["kind"], e["pin"], e["flags"], None))

    while queue:
        project, kind, pin, flags, required_by = queue.pop(0)
        pid = project["id"]
        if pid in chosen:
            if required_by:
                chosen[pid]["required_by"].append(required_by)
            continue
        version, err = pick_version(pid, kind, pin)
        if err:
            problems.append(f"{project['slug']}: {err}" + (f" (нужен для {required_by})" if required_by else ""))
            continue
        f = primary_file(version)
        folder = "shaderpacks" if kind == "shader" else "mods"
        entry = {
            "slug": project["slug"],
            "title": project["title"],
            "kind": kind,
            "project_id": pid,
            "version_id": version["id"],
            "version_number": version["version_number"],
            "version_type": version["version_type"],
            "license": (project.get("license") or {}).get("id"),
            "flags": flags,
            "required_by": [required_by] if required_by else [],
            "path": f"{folder}/{f['filename']}",
            "hashes": {"sha1": f["hashes"]["sha1"], "sha512": f["hashes"]["sha512"]},
            "downloads": [f["url"]],
            "fileSize": f["size"],
            "env": {"client": side(project.get("client_side")), "server": side(project.get("server_side"))},
            "dependencies": [],
            "incompatible": [],
        }
        if version["version_type"] != "release":
            notes.append(f"{project['slug']}: взята версия {version['version_type']} {version['version_number']} — релизной нет")
        chosen[pid] = entry
        for dep in version.get("dependencies", []):
            dtype = dep.get("dependency_type")
            dpid = dep.get("project_id")
            dvid = dep.get("version_id")
            if dtype == "embedded":
                continue
            if not dpid and dvid:
                dv = get(f"/version/{dvid}")
                dpid = dv and dv.get("project_id")
            if not dpid:
                continue
            dproject = get(f"/project/{dpid}")
            if not dproject:
                continue
            if dtype == "incompatible":
                entry["incompatible"].append(dproject["slug"])
                continue
            if dtype != "required":
                continue
            entry["dependencies"].append(dproject["slug"])
            dpin = None
            if dvid:
                dv = get(f"/version/{dvid}")
                if dv and compatible(dv, "mod"):
                    dpin = dvid
                else:
                    notes.append(f"{project['slug']}: зависимость {dproject['slug']} указана на несовместимую версию, беру последнюю под {LOADER} {MC}")
            queue.append((dproject, "mod", dpin, [], project["slug"]))

    slugs = {e["slug"] for e in chosen.values()}
    for e in chosen.values():
        for bad in e["incompatible"]:
            if bad in slugs:
                problems.append(f"{e['slug']} несовместим с {bad}, а оба в составе")

    files = sorted(chosen.values(), key=lambda x: (x["kind"], x["slug"]))
    lock = {"spec_sha256": spec_hash(), "minecraft": MC, "loader": LOADER,
            "resolved_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()), "files": files}
    LOCK.write_text(json.dumps(lock, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    write_report(files, problems, notes)
    print(f"Lock: {len(files)} файлов, проблем: {len(problems)}")
    for p in problems:
        print("::error::" + p)
    for n in notes:
        print("::warning::" + n)
    return 1 if problems else 0


def write_report(files, problems, notes):
    out = ["# Состав сборки (разрешено с Modrinth)", ""]
    if problems:
        out += ["## Проблемы", ""] + [f"- {p}" for p in problems] + [""]
    if notes:
        out += ["## Заметки", ""] + [f"- {n}" for n in notes] + [""]
    out += ["| Мод | Версия | Лицензия | Клиент | Сервер | Зависимости | Подтянут из-за |", "|---|---|---|---|---|---|---|"]
    for e in files:
        out.append(f"| {e['title']} (`{e['slug']}`) | {e['version_number']} | {e['license']} | {e['env']['client']} | "
                   f"{e['env']['server']} | {', '.join(e['dependencies']) or '—'} | {', '.join(e['required_by']) or '—'} |")
    Path(os.environ.get("PACK_REPORT", "pack-report.md")).write_text("\n".join(out) + "\n", encoding="utf-8")


def check():
    if not LOCK.exists():
        print("Lock-файла нет: нужен pack.py resolve")
        return 1
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    if lock.get("spec_sha256") != spec_hash():
        print("mods.txt изменён после lock: нужен pack.py resolve")
        return 1
    print("Lock соответствует mods.txt")
    return 0


def sha512(path):
    h = hashlib.sha512()
    with open(path, "rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def fetch(args):
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    dest = Path(args.dest)
    cache = Path(args.cache) if args.cache else None
    for sub in ("mods", "shaderpacks"):
        target = dest / sub
        target.mkdir(parents=True, exist_ok=True)
        for old in target.glob("*"):
            if old.is_file():
                old.unlink()
    count = 0
    for e in lock["files"]:
        if e["env"][args.side] == "unsupported":
            continue
        if args.skip_dev and "dev" in e["flags"]:
            continue
        if e["kind"] == "shader" and not args.with_shaders:
            continue
        target = dest / e["path"]
        cached = cache / e["hashes"]["sha512"][:32] if cache else None
        if cached and cached.exists() and sha512(cached) == e["hashes"]["sha512"]:
            shutil.copyfile(cached, target)
        else:
            req = urllib.request.Request(e["downloads"][0], headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=120) as r, open(target, "wb") as out:
                shutil.copyfileobj(r, out)
            if sha512(target) != e["hashes"]["sha512"]:
                sys.exit(f"Хеш не совпал: {e['path']}")
            if cached:
                cached.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(target, cached)
        count += 1
    print(f"Скачано в {dest}: {count} файлов ({args.side})")
    return 0


def main():
    p = argparse.ArgumentParser()
    sub = p.add_subparsers(dest="cmd", required=True)
    sub.add_parser("resolve")
    sub.add_parser("check")
    f = sub.add_parser("fetch")
    f.add_argument("--side", choices=["client", "server"], required=True)
    f.add_argument("--dest", required=True)
    f.add_argument("--skip-dev", action="store_true")
    f.add_argument("--with-shaders", action="store_true")
    f.add_argument("--cache")
    a = p.parse_args()
    if a.cmd == "resolve":
        return resolve()
    if a.cmd == "check":
        return check()
    return fetch(a)


if __name__ == "__main__":
    sys.exit(main())
