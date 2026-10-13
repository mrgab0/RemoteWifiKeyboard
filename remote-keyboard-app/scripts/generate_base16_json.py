import urllib.request
import zipfile
import io
import re
import json
import os

URL = "https://github.com/tinted-theming/schemes/archive/refs/heads/spec-0.11.zip"
OUTPUT_FILE = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "base16_schemes.json")

print("Descargando esquemas de Base16 desde GitHub...")
req = urllib.request.Request(URL, headers={"User-Agent": "Mozilla/5.0"})
with urllib.request.urlopen(req) as resp:
    zip_bytes = resp.read()

print(f"Descarga completa ({len(zip_bytes) / 1024:.1f} KB). Procesando esquemas...")
z = zipfile.ZipFile(io.BytesIO(zip_bytes))
schemes = []

for name in z.namelist():
    if name.startswith("schemes-spec-0.11/base16/") and name.endswith(".yaml"):
        raw_text = z.read(name).decode("utf-8", errors="ignore")
        
        name_match = re.search(r'name:\s*["\']?([^"\'\r\n]+)["\']?', raw_text)
        if not name_match:
            continue
        theme_name = name_match.group(1).strip()
        slug = name.split("/")[-1].replace(".yaml", "")

        author_match = re.search(r'author:\s*["\']?([^"\'\r\n]+)["\']?', raw_text)
        author = author_match.group(1).strip() if author_match else "Community"

        var_match = re.search(r'variant:\s*["\']?([^"\'\r\n]+)["\']?', raw_text)
        variant = var_match.group(1).strip().lower() if var_match else "dark"

        palette_map = {}
        for line in raw_text.splitlines():
            m = re.search(r'^\s*(base0[0-9A-Fa-f]):\s*["\']?#?([0-9a-fA-F]{6})["\']?', line)
            if m:
                palette_map[m.group(1).lower()] = "#" + m.group(2).upper()

        if len(palette_map) == 16:
            # Orden estricto base00 a base0f
            palette_list = [palette_map[f"base0{i:x}"] for i in range(16)]
            schemes.append({
                "id": slug,
                "name": theme_name,
                "author": author,
                "variant": variant,
                "palette": palette_list
            })

# Ordenar alfabéticamente por nombre
schemes.sort(key=lambda s: s["name"].lower())

os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    json.dump(schemes, f, separators=(",", ":"))

size_kb = os.path.getsize(OUTPUT_FILE) / 1024
print(f"¡Exito! Generados {len(schemes)} esquemas Base16 en: {OUTPUT_FILE} ({size_kb:.1f} KB)")
