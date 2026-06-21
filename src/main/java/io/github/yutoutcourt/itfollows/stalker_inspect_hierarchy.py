import json

bbmodel_path = r"c:\Users\wapzi\Desktop\Programmation\Java\ItFollows\stalker_blockbench\TEST\TMP\toww_geckolib.geo.bbmodel"

with open(bbmodel_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

print("Resolution:", data.get('resolution'))

# Let's inspect the keys of groups and elements
groups = data.get('groups', [])
print(f"Total groups: {len(groups)}")
if groups:
    print("Example group keys:", list(groups[0].keys()))
    # Let's print all groups and their names, uuid, origin, parent, and children
    for g in groups[:10]:
        print(f"Group: name={g.get('name')}, uuid={g.get('uuid')}, parent={g.get('parent')}, pivot={g.get('pivot')}, children={len(g.get('children', []))}")

elements = data.get('elements', [])
print(f"\nTotal elements: {len(elements)}")
if elements:
    print("Example element keys:", list(elements[0].keys()))
    for el in elements[:5]:
         print(f"Element: name={el.get('name')}, uuid={el.get('uuid')}, origin={el.get('from')}, size={el.get('to')}, uv_offset={el.get('uv_offset')}")
