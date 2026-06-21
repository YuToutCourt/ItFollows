import json

bbmodel_path = r"c:\Users\wapzi\Desktop\Programmation\Java\ItFollows\stalker_blockbench\TEST\TMP\toww_geckolib.geo.bbmodel"

with open(bbmodel_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

outliner = data.get('outliner', [])

def print_outliner_item(item, depth=0):
    indent = "  " * depth
    if isinstance(item, str):
        # This is a UUID of an element
        print(f"{indent}Element UUID: {item}")
    elif isinstance(item, dict):
        name = item.get('name', 'unnamed')
        uuid = item.get('uuid', 'no-uuid')
        children = item.get('children', [])
        ik = item.get('ik', '')
        print(f"{indent}Group: name='{name}', uuid='{uuid}', children_count={len(children)}")
        for child in children:
            print_outliner_item(child, depth + 1)
    else:
        print(f"{indent}Unknown item type: {type(item)}")

print("Outliner tree:")
for item in outliner:
    print_outliner_item(item)
