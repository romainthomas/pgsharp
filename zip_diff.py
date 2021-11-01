import pathlib
import zipfile

CWD = pathlib.Path(__file__).parent

CHEAT_FILE = CWD / "pgs1.27.0_com.nianticlabs.pokemongo_0.213.2_2021070901.apk"
ORIG_FILE  = CWD / "com.nianticlabs.pokemongo_0.213.2-2021070901.apk"


cheat_list = dict()
ofile_list = dict()

with zipfile.ZipFile(CHEAT_FILE) as cheat:
    for f in cheat.namelist():
        info = cheat.getinfo(f)
        cheat_list[f] = info.CRC

with zipfile.ZipFile(ORIG_FILE) as zipf:
    for f in zipf.namelist():
        info = zipf.getinfo(f)
        ofile_list[f] = info.CRC

diff = set(cheat_list.keys()) - set(ofile_list.keys())
diff = set(ofile_list.keys()) - set(cheat_list.keys())

for f in diff:
    print(f)

commond_files = cheat_list.keys() & ofile_list.keys()

for common in commond_files:
    if cheat_list[common] != ofile_list[common]:
        print(common)



