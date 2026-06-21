import json

bbmodel_path = r"c:\Users\wapzi\Desktop\Programmation\Java\ItFollows\stalker_blockbench\TEST\TMP\toww_geckolib.geo.bbmodel"

with open(bbmodel_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

groups = data.get('groups', [])
parented_groups = [g for g in groups if g.get('parent') is not None]
print(f"Total groups: {len(groups)}")
print(f"Groups with non-None parent: {len(parented_groups)}")

# Let's inspect some group structures
if groups:
    print("\nFirst group:")
    print(json.dumps(groups[0], indent=2))
