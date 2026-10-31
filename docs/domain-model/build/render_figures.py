# -*- coding: utf-8 -*-
"""Render all 5 Mermaid diagrams to SVG and crisp high-DPI PNG using Playwright + Chrome"""
import os
import sys

# Ensure UTF-8 output
if sys.stdout.encoding != 'utf-8':
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

build_dir = os.path.dirname(os.path.abspath(__file__))
doc_dir = os.path.dirname(build_dir)
diag_dir = os.path.join(doc_dir, "assets", "diagrams")
fig_dir = os.path.join(doc_dir, "assets", "figures")
mermaid_js = os.path.abspath(os.path.join(build_dir, "..", "..", "..", "2", "scripts", "mermaid.min.js")).replace('\\', '/')

diagram_configs = [
    {
        "id": "fig_1",
        "file": "fig_1_entities_vs_vo.mmd",
        "scale": 2.5,
        "width": 1200,
        "theme": "neutral"
    },
    {
        "id": "fig_2",
        "file": "fig_2_services.mmd",
        "scale": 2.5,
        "width": 1400,
        "theme": "neutral"
    },
    {
        "id": "fig_3",
        "file": "fig_3_cli_flow.mmd",
        "scale": 2.5,
        "width": 1200,
        "theme": "neutral"
    },
    {
        "id": "fig_4",
        "file": "fig_4_domain_model.mmd",
        "scale": 2.0,
        "width": 2600,
        "theme": "neutral"
    },
    {
        "id": "fig_5",
        "file": "fig_5_order_lifecycle.mmd",
        "scale": 2.5,
        "width": 1200,
        "theme": "neutral"
    }
]

def render_all():
    from playwright.sync_api import sync_playwright

    html_content = f"""<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<script src="file:///{mermaid_js}"></script>
<style>
body {{
    margin: 0;
    padding: 30px;
    background: white;
    font-family: Arial, "Segoe UI", Tahoma, sans-serif;
}}
#container {{
    display: inline-block;
    background: white;
}}
svg {{
    max-width: 100% !important;
}}
</style>
</head>
<body>
<div id="container"></div>
<script>
mermaid.initialize({{
    startOnLoad: false,
    theme: 'neutral',
    themeVariables: {{
        fontFamily: 'Arial, "Segoe UI", Tahoma, sans-serif',
        fontSize: '14px',
        primaryColor: '#eef2f7',
        primaryBorderColor: '#2b6cb0',
        primaryTextColor: '#1a365d',
        lineColor: '#4a5568'
    }},
    flowchart: {{ useMaxWidth: false, htmlLabels: true }},
    class: {{ useMaxWidth: false }}
}});

async function renderDiagram(code) {{
    const id = 'diagram_' + Math.random().toString(36).substring(2, 9);
    const res = await mermaid.render(id, code);
    document.getElementById('container').innerHTML = res.svg;
    return res.svg;
}}
</script>
</body>
</html>
"""

    temp_html = os.path.join(build_dir, "render_runner.html")
    with open(temp_html, "w", encoding="utf-8") as f:
        f.write(html_content)

    html_url = "file:///" + temp_html.replace('\\', '/')

    print("Launching Chrome via Playwright...")
    with sync_playwright() as p:
        browser = p.chromium.launch(executable_path=r'C:\Program Files\Google\Chrome\Application\chrome.exe')
        
        for cfg in diagram_configs:
            fig_id = cfg["id"]
            mmd_path = os.path.join(diag_dir, cfg["file"])
            svg_out = os.path.join(fig_dir, f"{fig_id}.svg")
            png_out = os.path.join(fig_dir, f"{fig_id}.png")
            
            print(f"Processing {fig_id} ({cfg['file']})...")
            with open(mmd_path, "r", encoding="utf-8") as f:
                code = f.read().strip()
                
            page = browser.new_page(
                viewport={"width": cfg["width"], "height": 2000},
                device_scale_factor=cfg["scale"]
            )
            page.goto(html_url)
            
            try:
                svg_str = page.evaluate("code => renderDiagram(code)", code)
                with open(svg_out, "w", encoding="utf-8") as f:
                    f.write(svg_str)
                print(f"  [OK] Saved SVG: {svg_out} ({len(svg_str)} bytes)")
                
                container = page.locator("#container")
                container.screenshot(path=png_out)
                png_size = os.path.getsize(png_out)
                print(f"  [OK] Saved PNG: {png_out} ({png_size} bytes)")
            except Exception as e:
                print(f"  [ERROR] Failed to render {fig_id}: {e}")
            finally:
                page.close()
                
        browser.close()

    if os.path.exists(temp_html):
        os.remove(temp_html)
    print("All figures rendered successfully!")

if __name__ == "__main__":
    render_all()
