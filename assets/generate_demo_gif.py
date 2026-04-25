"""
Generate the demo GIF for README.

The GIF shows a short terminal session that highlights NRG's killer features:

    1. `cat README.src.md` reveals a syntax-highlighted source with metadata
       variables, multi-language substitutions (en/ru/ja/zh/es) using `${name}`
       interpolation, and widget calls (TOC, import).
    2. `nrg -f README.src.md` emits five "File ... created" log lines, one per
       configured language, appearing one after the other.
    3. `ls README.*` lists all five generated files (highlighted) next to the
       single, dimmed `.src.md` source.

Output: assets/demo.gif (deterministic, ~16 seconds).
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

# --- Layout & palette ---------------------------------------------------------

WIDTH, HEIGHT = 900, 700
PADDING_X, PADDING_Y = 24, 60  # leave room for the title bar

BG = (24, 24, 27)            # zinc-900
TITLE_BAR_BG = (39, 39, 42)  # zinc-800
TITLE_TEXT = (212, 212, 216)
DOT_RED, DOT_YELLOW, DOT_GREEN = (255, 95, 86), (255, 189, 46), (39, 201, 63)

# Prompt / shell
PROMPT_USER = (134, 239, 172)   # green-300
PROMPT_HOST = (147, 197, 253)
PROMPT_PATH = (253, 224, 71)    # yellow-300
PROMPT_SIGN = (244, 244, 245)
COMMAND = (244, 244, 245)
CARET = (244, 244, 245)

# nrg INFO log
INFO_TIMESTAMP = (113, 113, 122)
INFO_TAG = (250, 204, 21)
INFO_TEXT = (228, 228, 231)

# ls output
FILE_SOURCE = (113, 113, 122)
FILE_GENERATED = (134, 239, 172)

# cat README.md output (rendered file)
GEN_HEADER_COMMENT = (113, 113, 122)
GEN_HEADING_HASH = (244, 114, 182)   # pink-400
GEN_HEADING_TEXT = (244, 244, 245)
GEN_RESOLVED = (190, 242, 100)        # lime-300 — values that came from substitution
GEN_QUOTE_MARK = (148, 163, 184)      # slate-400
GEN_QUOTE_TEXT = (203, 213, 225)
GEN_TOC_NUM = (103, 232, 249)         # cyan-300 — TOC numbering
GEN_TOC_LINK = (147, 197, 253)        # blue-300 — TOC link text
GEN_TOC_ANCHOR = (113, 113, 122)      # dim — (#anchor) suffix
GEN_BULLET = (244, 114, 182)          # pink — list markers
GEN_BULLET_NAME = (244, 244, 245)
GEN_BULLET_DESC = (161, 161, 170)     # zinc-400 — descriptions

# Syntax highlighting for the .src.md source view
SYN_COMMENT = (100, 116, 139)   # slate-500
SYN_META_KEY = (250, 204, 21)   # yellow-400
SYN_META_VAL = (216, 180, 254)  # purple-300
SYN_HEADING = (244, 244, 245)
SYN_BRACE = (203, 213, 225)     # slate-300
SYN_LANG = (103, 232, 249)      # cyan-300
SYN_STRING = (134, 239, 172)    # green-300
SYN_INTERP = (251, 146, 60)     # orange-400
SYN_WIDGET = (240, 171, 252)    # fuchsia-300
SYN_WIDGET_NAME = (147, 197, 253)
SYN_WIDGET_ARG = (203, 213, 225)
SYN_QUOTE = (74, 222, 128)
SYN_PUNCT = (203, 213, 225)
SYN_CITATION = (165, 180, 252)  # indigo-300

# Fonts: Consolas for Latin/Cyrillic, MS Gothic for CJK fallback.
FONT_PATH = "C:\\Windows\\Fonts\\consola.ttf"
FONT_BOLD_PATH = "C:\\Windows\\Fonts\\consolab.ttf"
CJK_FONT_PATH = "C:\\Windows\\Fonts\\simsun.ttc"  # full CJK, incl. simplified Chinese
FONT_SIZE = 17
LINE_HEIGHT = 23

font = ImageFont.truetype(FONT_PATH, FONT_SIZE)
font_bold = ImageFont.truetype(FONT_BOLD_PATH, FONT_SIZE)
cjk_font = ImageFont.truetype(CJK_FONT_PATH, FONT_SIZE, index=0)
title_font = ImageFont.truetype(FONT_BOLD_PATH, 14)


def _is_cjk(ch: str) -> bool:
    cp = ord(ch)
    return (
        0x2E80 <= cp <= 0x9FFF        # CJK Unified Ideographs + radicals
        or 0xAC00 <= cp <= 0xD7AF     # Hangul
        or 0xF900 <= cp <= 0xFAFF     # CJK Compatibility Ideographs
        or 0xFF00 <= cp <= 0xFFEF     # Halfwidth/Fullwidth Forms
        or 0x3000 <= cp <= 0x30FF     # CJK punctuation, Hiragana, Katakana
    )


def _segment_by_script(text: str) -> list[tuple[str, bool]]:
    """Split text into (run, is_cjk) chunks so each chunk can use one font."""
    if not text:
        return []
    runs: list[tuple[str, bool]] = []
    buf = [text[0]]
    cur = _is_cjk(text[0])
    for ch in text[1:]:
        is_cjk = _is_cjk(ch)
        if is_cjk == cur:
            buf.append(ch)
        else:
            runs.append(("".join(buf), cur))
            buf = [ch]
            cur = is_cjk
    runs.append(("".join(buf), cur))
    return runs


def draw_segment(draw: ImageDraw.ImageDraw, x: int, y: int, text: str,
                 color: tuple[int, int, int], bold: bool) -> int:
    """Draw `text` at (x, y) in `color`, switching to MS Gothic for CJK runs.
    Returns new x after the drawn text."""
    base = font_bold if bold else font
    for run, is_cjk in _segment_by_script(text):
        f = cjk_font if is_cjk else base
        draw.text((x, y), run, font=f, fill=color)
        bbox = draw.textbbox((x, y), run, font=f)
        x = bbox[2]
    return x


# --- Scene model --------------------------------------------------------------

# Each "rendered line" is a list of (text, color, bold) tuples.
Line = list  # list[tuple[str, tuple[int, int, int], bool]]

PROMPT_SEGMENTS = [
    ("user@nanolaba", PROMPT_USER, False),
    (":", PROMPT_SIGN, False),
    ("~/my-project", PROMPT_PATH, False),
    ("$ ", PROMPT_SIGN, False),
]


def make_prompt(command_text: str = "", show_caret: bool = False) -> Line:
    line: Line = list(PROMPT_SEGMENTS)
    if command_text:
        line.append((command_text, COMMAND, False))
    if show_caret:
        line.append(("█", CARET, False))
    return line


def make_info(filename: str, size: int) -> Line:
    return [
        ("INFO 25.04.2026 19:47:42 ", INFO_TIMESTAMP, False),
        ("[NRG] ", INFO_TAG, True),
        (f'File "{filename}" created, total size {size} bytes', INFO_TEXT, False),
    ]


def make_ls_line() -> Line:
    """ls README.*  -- five generated files (highlighted) + source (dim)."""
    cells = [
        ("README.es.md", FILE_GENERATED, True),
        ("README.ja.md", FILE_GENERATED, True),
        ("README.md", FILE_GENERATED, True),
        ("README.ru.md", FILE_GENERATED, True),
        ("README.src.md", FILE_SOURCE, False),
        ("README.zh.md", FILE_GENERATED, True),
    ]
    out: Line = []
    for i, (name, color, bold) in enumerate(cells):
        if i:
            out.append(("  ", COMMAND, False))
        out.append((name, color, bold))
    return out


def gen_lines() -> list[Line]:
    """Rendered `README.md` (default language = en).

    Designed for the wow moment: a tiny `.src.md` template expands into a
    polished, syntax-coloured page — substitutions highlighted in lime, TOC
    auto-generated by `${widget:tableOfContents}`, body sections pulled in by
    `${widget:import(...)}`."""
    L: list[Line] = []
    L.append([("<!-- This file was automatically generated by Nanolaba Readme "
               "Generator (NRG) 1.0 -->", GEN_HEADER_COMMENT, False)])
    L.append([("<!-- Visit https://github.com/nanolaba/readme-generator for "
               "details -->", GEN_HEADER_COMMENT, False)])
    L.append([("# ", GEN_HEADING_HASH, True),
              ("Welcome to ", GEN_HEADING_TEXT, True),
              ("My App", GEN_RESOLVED, True)])
    L.append([("", COMMAND, False)])
    L.append([("> ", GEN_QUOTE_MARK, False),
              ("v", GEN_QUOTE_TEXT, False),
              ("1.2.0", GEN_RESOLVED, True)])
    L.append([("", COMMAND, False)])
    # ── TOC: rendered by ${widget:tableOfContents} ─────────────────
    L.append([("## ", GEN_HEADING_HASH, True),
              ("Table of contents", GEN_HEADING_TEXT, True)])
    L.append([("", COMMAND, False)])
    toc = [
        ("1.", "Features", "#features"),
        ("    1.1", "Multi-language output", "#multi-language-output"),
        ("    1.2", "Custom widgets", "#custom-widgets"),
        ("    1.3", "Built-in TOC", "#built-in-toc"),
        ("2.", "Installation", "#installation"),
        ("3.", "License", "#license"),
    ]
    for num, title, anchor in toc:
        L.append([(num + " ", GEN_TOC_NUM, True),
                  (title, GEN_TOC_LINK, False),
                  (f"  ({anchor})", GEN_TOC_ANCHOR, False)])
    L.append([("", COMMAND, False)])
    # ── Body: pulled in by ${widget:import(path='docs/intro.src.md')} ──
    L.append([("## ", GEN_HEADING_HASH, True),
              ("Features", GEN_HEADING_TEXT, True)])
    L.append([("", COMMAND, False)])
    bullets = [
        ("Multi-language output", "one .src.md, many .md"),
        ("Custom widgets",        "TOC, import, dates, badges"),
        ("Built-in TOC",          "auto-generated from headings"),
    ]
    for name, desc in bullets:
        L.append([("- ", GEN_BULLET, True),
                  (name, GEN_BULLET_NAME, True),
                  (" — ", GEN_BULLET_DESC, False),
                  (desc, GEN_BULLET_DESC, False)])
    return L


# --- Source view (syntax-highlighted) ----------------------------------------

def src_lines() -> list[Line]:
    """Hand-tokenized syntax-highlighted view of README.src.md."""
    L: list[Line] = []
    L.append([("<!--@nrg.languages=", SYN_COMMENT, False),
              ("en,ru,ja,zh,es", SYN_META_VAL, False),
              ("-->", SYN_COMMENT, False)])
    L.append([("<!--@name=", SYN_COMMENT, False),
              ("My App", SYN_META_VAL, False),
              ("-->", SYN_COMMENT, False)])
    L.append([("<!--@version=", SYN_COMMENT, False),
              ("1.2.0", SYN_META_VAL, False),
              ("-->", SYN_COMMENT, False)])
    L.append([("", COMMAND, False)])
    L.append([("# ", SYN_HEADING, True),
              ("${", SYN_BRACE, False), ("en", SYN_LANG, True), (":", SYN_PUNCT, False),
              ("'Welcome to ", SYN_STRING, False),
              ("${", SYN_INTERP, False), ("name", SYN_INTERP, True), ("}", SYN_INTERP, False),
              ("'", SYN_STRING, False), (",", SYN_PUNCT, False)])
    for code, greeting in [
        ("ru", "'Добро пожаловать в "),
        ("ja", "'"),
        ("zh", "'欢迎使用 "),
        ("es", "'¡Bienvenido a "),
    ]:
        L.append([("   ", COMMAND, False),
                  (code, SYN_LANG, True), (":", SYN_PUNCT, False),
                  (greeting, SYN_STRING, False),
                  ("${", SYN_INTERP, False), ("name", SYN_INTERP, True), ("}", SYN_INTERP, False),
                  ("'" if code != "ja" else " へようこそ'", SYN_STRING, False),
                  ("," if code != "es" else "}", SYN_PUNCT, False)])
    L.append([("", COMMAND, False)])
    L.append([("> v", SYN_CITATION, False),
              ("${", SYN_BRACE, False), ("version", SYN_INTERP, True), ("}", SYN_BRACE, False)])
    L.append([("", COMMAND, False)])
    L.append([("${", SYN_BRACE, False),
              ("widget:", SYN_WIDGET, False),
              ("tableOfContents", SYN_WIDGET_NAME, True),
              ("}", SYN_BRACE, False)])
    L.append([("${", SYN_BRACE, False),
              ("widget:", SYN_WIDGET, False),
              ("import", SYN_WIDGET_NAME, True),
              ("(", SYN_PUNCT, False),
              ("path=", SYN_WIDGET_ARG, False),
              ("'docs/intro.src.md'", SYN_STRING, False),
              (")", SYN_PUNCT, False),
              ("}", SYN_BRACE, False)])
    return L


# --- Rendering ----------------------------------------------------------------

def draw_chrome(img: Image.Image) -> None:
    draw = ImageDraw.Draw(img)
    draw.rectangle((0, 0, WIDTH, 36), fill=TITLE_BAR_BG)
    for i, color in enumerate((DOT_RED, DOT_YELLOW, DOT_GREEN)):
        cx = 18 + i * 22
        draw.ellipse((cx, 12, cx + 12, 24), fill=color)
    title = "nrg — multi-language README generator"
    bbox = draw.textbbox((0, 0), title, font=title_font)
    tw = bbox[2] - bbox[0]
    draw.text(((WIDTH - tw) // 2, 11), title, font=title_font, fill=TITLE_TEXT)


MAX_LINES = (HEIGHT - PADDING_Y - 16) // LINE_HEIGHT


def render_frame(lines: list[Line]) -> Image.Image:
    img = Image.new("RGB", (WIDTH, HEIGHT), BG)
    draw_chrome(img)
    draw = ImageDraw.Draw(img)
    visible = lines[-MAX_LINES:]   # auto-scroll: drop oldest lines if overflow
    y = PADDING_Y
    for line in visible:
        x = PADDING_X
        for text, color, bold in line:
            x = draw_segment(draw, x, y, text, color, bold)
        y += LINE_HEIGHT
    return img


# --- Frame sequence -----------------------------------------------------------

def build_frames() -> tuple[list[Image.Image], list[int]]:
    frames: list[Image.Image] = []
    durations: list[int] = []
    history: list[Line] = []

    def push(extra_tail: list[Line], duration_ms: int) -> None:
        frames.append(render_frame(history + extra_tail))
        durations.append(duration_ms)

    def commit(tail: list[Line]) -> None:
        history.extend(tail)

    def type_command(cmd: str, char_ms: int = 60, hold_after_ms: int = 350) -> None:
        for i in range(1, len(cmd) + 1):
            push([make_prompt(cmd[:i], show_caret=True)], char_ms)
        push([make_prompt(cmd, show_caret=True)], hold_after_ms)
        push([make_prompt(cmd, show_caret=False)], 200)
        commit([make_prompt(cmd, show_caret=False)])

    # Scene 0 — empty prompt
    push([make_prompt(show_caret=True)], 500)

    # Scene 1 — `cat README.src.md` + source listing
    type_command("cat README.src.md", char_ms=55, hold_after_ms=300)
    src = src_lines()
    # Reveal source line by line for a "page-printing" feel.
    for i in range(1, len(src) + 1):
        push(src[:i], 70)
    commit(src)
    push([], 1500)  # hold so reader can scan the highlights

    # Scene 2 — `nrg -f README.src.md`
    type_command("nrg -f README.src.md", char_ms=55, hold_after_ms=400)

    # Scene 3 — five INFO lines, staggered.
    sizes = [
        ("README.md", 1024),
        ("README.ru.md", 1342),
        ("README.ja.md", 1187),
        ("README.zh.md", 1098),
        ("README.es.md", 1276),
    ]
    info_lines: list[Line] = []
    for name, size in sizes:
        info_lines.append(make_info(name, size))
        push(info_lines.copy(), 650)
    commit(info_lines)
    push([], 600)

    # Scene 4 — `ls README.*`
    type_command("ls README.*", char_ms=60, hold_after_ms=350)
    commit([make_ls_line()])
    push([], 600)

    # Scene 5 — `cat README.md`: prove the substitutions actually resolved.
    type_command("cat README.md", char_ms=55, hold_after_ms=300)
    rendered = gen_lines()
    for i in range(1, len(rendered) + 1):
        push(rendered[:i], 90)
    commit(rendered)
    push([], 10000)  # final hold on the resolved output (10s before the GIF loops)

    return frames, durations


def main() -> None:
    out = Path(__file__).parent / "demo.gif"
    frames, durations = build_frames()
    frames[0].save(
        out,
        save_all=True,
        append_images=frames[1:],
        duration=durations,
        loop=0,
        optimize=True,
        disposal=2,
    )
    total_ms = sum(durations)
    size_kb = out.stat().st_size / 1024
    print(
        f"Wrote {out} — {len(frames)} frames, "
        f"{total_ms / 1000:.2f}s, {size_kb:.1f} KB"
    )


if __name__ == "__main__":
    main()
