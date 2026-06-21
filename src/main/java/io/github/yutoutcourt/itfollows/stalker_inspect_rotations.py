import json

bbmodel_path = r"c:\Users\wapzi\Desktop\Programmation\Java\ItFollows\stalker_blockbench\TEST\TMP\toww_geckolib.geo.bbmodel"

with open(bbmodel_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

groups = data.get('groups', [])
rotated_groups = [g for g in groups if g.get('rotation') and any(r != 0 for r in g['rotation'])]
print(f"Total groups: {len(groups)}")
print(f"Rotated groups: {len(rotated_groups)}")
for g in rotated_groups[:5]:
    print(f"Group: name='{g.get('name')}', rotation={g.get('rotation')}, origin={g.get('origin')}")
