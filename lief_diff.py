import pathlib
import zipfile
import sys
import zlib
import lief

CWD = pathlib.Path(__file__).parent

CHEAT_FILE = sys.argv[1]
ORIG_FILE  = sys.argv[2]

target = "classes2.dex"

hela_dex = None
pgo_dex  = None

with zipfile.ZipFile(CHEAT_FILE) as zip_file:
    with zip_file.open(target) as f:
        hela_dex = f.read()


with zipfile.ZipFile(ORIG_FILE) as zip_file:
    with zip_file.open(target) as f:
        pgo_dex = f.read()

hela_dex = lief.DEX.parse(list(hela_dex))
pgo_dex  = lief.DEX.parse(list(pgo_dex))

hela = {f"{m.cls.pretty_name}.{m.name}.{m.prototype!s}": len(m.bytecode) for m in hela_dex.methods}
pgo  = {f"{m.cls.pretty_name}.{m.name}.{m.prototype!s}": len(m.bytecode) for m in pgo_dex.methods}

for k, size_hela in hela.items():
    size_pgo = pgo[k]
    if size_pgo != size_hela:
        print(f"Mismatch: {k}")

# Diff strings
print("Diff Strings")
print(set(hela_dex.strings) ^ set(pgo_dex.strings))

# Diff methods
print(set(hela.keys()) ^ set(pgo.keys()))



