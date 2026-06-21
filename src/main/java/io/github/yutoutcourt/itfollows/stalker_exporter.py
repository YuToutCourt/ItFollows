import json
import base64
import os

bbmodel_path = r"c:\Users\wapzi\Desktop\Programmation\Java\ItFollows\stalker_blockbench\TEST\TMP\toww_geckolib.geo.bbmodel"
target_geo_path = r"c:\Users\wapzi\Desktop\Programmation\Java\ItFollows\src\main\resources\assets\itfollows\geo\stalker.geo.json"
target_anim_path = r"c:\Users\wapzi\Desktop\Programmation\Java\ItFollows\src\main\resources\assets\itfollows\animations\stalker.animation.json"
target_tex_path = r"c:\Users\wapzi\Desktop\Programmation\Java\ItFollows\src\main\resources\assets\itfollows\textures\entity\stalker.png"

with open(bbmodel_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

# 1. Export texture
print("Exporting texture...")
textures = data.get('textures', [])
if textures:
    tex = textures[0]
    source = tex.get('source', '')
    if source.startswith('data:image/png;base64,'):
        base64_data = source.split('base64,')[1]
        img_data = base64.b64decode(base64_data)
        os.makedirs(os.path.dirname(target_tex_path), exist_ok=True)
        with open(target_tex_path, 'wb') as img_f:
            img_f.write(img_data)
        print(f"Texture saved to {target_tex_path}")
    else:
        print("Error: texture source is not in base64 PNG format.")
else:
    print("Warning: no textures found in bbmodel.")

# 2. Build map of Group UUID -> Group property dict
groups_flat = data.get('groups', [])
group_map = {}
for g in groups_flat:
    group_map[g['uuid']] = g

# Build map of Element UUID -> Element dict
elements_flat = data.get('elements', [])
element_map = {}
for el in elements_flat:
    element_map[el['uuid']] = el

# Helper to negate values / Molang expressions
def negate_expr(expr):
    if expr is None:
        return 0
    if isinstance(expr, (int, float)):
        return -expr
    # If it is a string representation of a number
    try:
        val = float(expr)
        return -val
    except ValueError:
        # Molang expression: wrap in minus
        # Remove any leading spaces
        s = str(expr).strip()
        if s.startswith("-"):
            return s[1:]
        else:
            return f"-({s})"

def parse_expr(expr):
    if expr is None:
        return 0
    try:
        return float(expr)
    except ValueError:
        return str(expr)

# Traverse outliner to build bones list
bones = []

def process_outliner_node(node, parent_name=None):
    if isinstance(node, str):
        # Element UUID: in our parser, cubes are added to their parent group (bone)
        # We don't process root elements as bones, they would need a root group.
        # But Blockbench usually puts cubes inside groups.
        return
    
    if isinstance(node, dict):
        group_uuid = node.get('uuid')
        group_prop = group_map.get(group_uuid)
        if not group_prop:
            print(f"Warning: group properties not found for UUID {group_uuid}")
            return
        
        name = group_prop.get('name', 'unnamed')
        origin = group_prop.get('origin', [0, 0, 0])
        rotation = group_prop.get('rotation', [0, 0, 0])
        
        bone = {
            "name": name,
            "pivot": origin,
        }
        if parent_name:
            bone["parent"] = parent_name
            
        # Invert X and Y for bone rotations in Bedrock
        rx, ry, rz = rotation
        if rx != 0 or ry != 0 or rz != 0:
            bone["rotation"] = [-rx, -ry, rz]
            
        # Process cubes directly inside this group
        cubes = []
        children = node.get('children', [])
        for child in children:
            if isinstance(child, str): # Element UUID
                el = element_map.get(child)
                if el and el.get('export', True):
                    c_from = el.get('from', [0, 0, 0])
                    c_to = el.get('to', [0, 0, 0])
                    c_origin = el.get('origin', [0, 0, 0])
                    c_rot = el.get('rotation', [0, 0, 0])
                    
                    cube = {
                        "origin": c_from,
                        "size": [c_to[0] - c_from[0], c_to[1] - c_from[1], c_to[2] - c_from[2]]
                    }
                    
                    # Inflate
                    if 'inflate' in el:
                        cube['inflate'] = el['inflate']
                    
                    # UV (Face UV format)
                    uv_faces = {}
                    faces = el.get('faces', {})
                    for face_name, face_data in faces.items():
                        if 'uv' in face_data:
                            u1, v1, u2, v2 = face_data['uv']
                            uv_faces[face_name] = {
                                "uv": [u1, v1],
                                "uv_size": [u2 - u1, v2 - v1]
                            }
                    if uv_faces:
                        cube["uv"] = uv_faces
                        
                    # Cube Rotation (if any)
                    cx, cy, cz = c_rot
                    if cx != 0 or cy != 0 or cz != 0:
                        cube["rotation"] = [-cx, -cy, cz]
                        cube["pivot"] = c_origin
                        
                    cubes.append(cube)
                    
        if cubes:
            bone["cubes"] = cubes
            
        bones.append(bone)
        
        # Recurse for subgroups
        for child in children:
            if isinstance(child, dict):
                process_outliner_node(child, parent_name=name)

# Process outliner root nodes
outliner = data.get('outliner', [])
for node in outliner:
    process_outliner_node(node)

# Wrap geometry
geo_export = {
    "format_version": "1.12.0",
    "minecraft:geometry": [
        {
            "description": {
                "identifier": "geometry.stalker",
                "texture_width": data.get('resolution', {}).get('width', 64),
                "texture_height": data.get('resolution', {}).get('height', 64),
                "visible_bounds_width": 5,
                "visible_bounds_height": 4.5,
                "visible_bounds_offset": [0, 1.25, 0]
            },
            "bones": bones
        }
    ]
}

# Save geometry
os.makedirs(os.path.dirname(target_geo_path), exist_ok=True)
with open(target_geo_path, 'w', encoding='utf-8') as f:
    json.dump(geo_export, f, indent=4)
print(f"Geometry saved to {target_geo_path}")

# 3. Export animations
print("Exporting animations...")
anims_export = {}
animations = data.get('animations', [])

for anim in animations:
    anim_name = anim.get('name', 'unnamed')
    loop = anim.get('loop', 'once')
    length = anim.get('length', 0)
    
    # GeckoLib loop formats
    loop_val = True if loop in (True, 'loop') else False
    if loop == 'hold':
         # In some contexts loop can be hold. Let's keep it as boolean or string as needed.
         loop_val = 'hold'
    
    bones_anim = {}
    animators = anim.get('animators', {})
    for bone_uuid, animator in animators.items():
        bone_name = animator.get('name')
        if not bone_name:
            # Try to lookup from group_map
            g_prop = group_map.get(bone_uuid)
            if g_prop:
                bone_name = g_prop.get('name')
        
        if not bone_name:
            continue
            
        keyframes = animator.get('keyframes', [])
        if not keyframes:
            continue
            
        # Group keyframes by channel
        channels = {}
        for kf in keyframes:
            ch = kf.get('channel', 'rotation')
            if ch not in channels:
                channels[ch] = []
            channels[ch].append(kf)
            
        bone_entry = {}
        for ch, kfs in channels.items():
            # Sort by time
            kfs.sort(key=lambda x: x.get('time', 0))
            
            kf_dict = {}
            for kf in kfs:
                t = kf.get('time', 0)
                t_str = f"{t:.4f}"
                # Remove trailing zeros to keep clean
                if '.' in t_str:
                    t_str = t_str.rstrip('0').rstrip('.')
                    if t_str == "":
                        t_str = "0.0"
                
                dp_list = kf.get('data_points', [])
                if dp_list:
                    dp = dp_list[0]
                    x = parse_expr(dp.get('x', 0))
                    y = parse_expr(dp.get('y', 0))
                    z = parse_expr(dp.get('z', 0))
                    
                    if ch == 'rotation':
                        # Negate X and Y
                        x = negate_expr(x)
                        y = negate_expr(y)
                    elif ch == 'position':
                        # Negate X
                        x = negate_expr(x)
                        
                    # Format
                    kf_dict[t_str] = [x, y, z]
            
            if kf_dict:
                bone_entry[ch] = kf_dict
                
        if bone_entry:
            bones_anim[bone_name] = bone_entry
            
    anims_export[anim_name] = {
        "loop": loop_val,
        "animation_length": length,
        "bones": bones_anim
    }

final_anims = {
    "format_version": "1.8.0",
    "animations": anims_export
}

os.makedirs(os.path.dirname(target_anim_path), exist_ok=True)
with open(target_anim_path, 'w', encoding='utf-8') as f:
    json.dump(final_anims, f, indent=4)
print(f"Animations saved to {target_anim_path}")
print("Export complete!")
