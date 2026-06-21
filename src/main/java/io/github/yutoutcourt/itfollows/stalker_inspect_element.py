import json

bbmodel_path = r"c:\Users\wapzi\Desktop\Programmation\Java\ItFollows\stalker_blockbench\TEST\TMP\toww_geckolib.geo.bbmodel"

with open(bbmodel_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

elements = data.get('elements', [])
for i in range(min(len(elements), 3)):
    print(f"\n--- ELEMENT {i} ({elements[i].get('name')}) ---")
    print(json.dumps(elements[i], indent=2))
