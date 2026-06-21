import json

bbmodel_path = r"c:\Users\wapzi\Desktop\Programmation\Java\ItFollows\stalker_blockbench\TEST\TMP\toww_geckolib.geo.bbmodel"

with open(bbmodel_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

print("Resolution:", data.get('resolution'))

print("\n--- FIRST ELEMENT ---")
elements = data.get('elements', [])
if elements:
    print(json.dumps(elements[0], indent=2))
else:
    print("No elements")

print("\n--- FIRST GROUP ---")
groups = data.get('groups', [])
if groups:
    # Groups can be dicts or strings/UUIDs depending on the format.
    # Let's print the type and structure.
    print("Group type:", type(groups[0]))
    print(json.dumps(groups[0], indent=2))
else:
    print("No groups")

print("\n--- OUTLINER ---")
outliner = data.get('outliner', [])
if outliner:
    print("Outliner size:", len(outliner))
    print("First 3 outliner items:")
    for item in outliner[:3]:
        print(type(item), item)

print("\n--- ANIMATIONS FIRST ANIMATION ---")
animations = data.get('animations', [])
if animations:
    anim = animations[0]
    print("Keys in anim:", list(anim.keys()))
    print("First animation name:", anim.get('name'))
    # Print keyframes or bones inside the animation
    anim_bones = anim.get('animators', {})
    print("Bones with animators count:", len(anim_bones))
    if anim_bones:
        first_bone_key = list(anim_bones.keys())[0]
        print(f"Animator for bone {first_bone_key}:", type(anim_bones[first_bone_key]))
        print(json.dumps(anim_bones[first_bone_key], indent=2)[:500] + "...")
