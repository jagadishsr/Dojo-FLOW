---
name: Dojo Discipline
colors:
  surface: '#faf9ff'
  surface-dim: '#d8d9e4'
  surface-bright: '#faf9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3fe'
  surface-container: '#ecedf8'
  surface-container-high: '#e6e7f3'
  surface-container-highest: '#e0e2ed'
  on-surface: '#181b23'
  on-surface-variant: '#5b403e'
  inverse-surface: '#2d3039'
  inverse-on-surface: '#eff0fb'
  outline: '#906f6d'
  outline-variant: '#e4bdba'
  surface-tint: '#bb1423'
  primary: '#a30018'
  on-primary: '#ffffff'
  primary-container: '#c9212b'
  on-primary-container: '#ffe0de'
  inverse-primary: '#ffb3ae'
  secondary: '#b02c32'
  on-secondary: '#ffffff'
  secondary-container: '#fe6666'
  on-secondary-container: '#69000f'
  tertiary: '#005574'
  on-tertiary: '#ffffff'
  tertiary-container: '#006f96'
  on-tertiary-container: '#cdebff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#ffdad7'
  primary-fixed-dim: '#ffb3ae'
  on-primary-fixed: '#410004'
  on-primary-fixed-variant: '#930015'
  secondary-fixed: '#ffdad8'
  secondary-fixed-dim: '#ffb3b0'
  on-secondary-fixed: '#410006'
  on-secondary-fixed-variant: '#8e111e'
  tertiary-fixed: '#c3e8ff'
  tertiary-fixed-dim: '#82d0fb'
  on-tertiary-fixed: '#001e2c'
  on-tertiary-fixed-variant: '#004c69'
  background: '#faf9ff'
  on-background: '#181b23'
  surface-variant: '#e0e2ed'
  canvas-warm: '#F7F6F3'
  surface-card: '#FFFFFF'
  text-secondary: '#777D87'
  border-subtle: '#E6E6E8'
  status-success: '#1E9B61'
  status-warning: '#E9A52A'
  status-error: '#D94141'
typography:
  display:
    fontFamily: Rajdhani
    fontSize: 40px
    fontWeight: '700'
    lineHeight: 46px
    letterSpacing: -0.01em
  headline-lg:
    fontFamily: Rajdhani
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: 0.01em
  headline-md:
    fontFamily: Rajdhani
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: 0.01em
  headline-sm:
    fontFamily: Rajdhani
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  section-title:
    fontFamily: Rajdhani
    fontSize: 15px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.04em
  org-title:
    fontFamily: Rajdhani
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 18px
    letterSpacing: 0.06em
  card-title:
    fontFamily: Rajdhani
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 22px
  metric-stat:
    fontFamily: Manrope
    fontSize: 28px
    fontWeight: '800'
    lineHeight: 34px
    letterSpacing: -0.02em
  body-lg:
    fontFamily: Manrope
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 22px
  body-md:
    fontFamily: Manrope
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 19px
  body-sm:
    fontFamily: Manrope
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-md:
    fontFamily: Manrope
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Manrope
    fontSize: 11px
    fontWeight: '600'
    lineHeight: 14px
    letterSpacing: 0.04em
  label-badge:
    fontFamily: Manrope
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 12px
    letterSpacing: 0.05em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
---

# DojoFLOW — Authoritative Design Specification & Guidelines (Lacquered Federation Modernism)

## 1. Typography System (Rajdhani + Manrope Direction)
- **Authoritative Display & Heading Font:** **Rajdhani** (Weights: 500, 600, 700)
  - Applied to federation title, screen headers, section titles, class/event names, modal titles, and student names.
  - Characterized by angular martial precision, condensed geometry, and high executive authority.
- **Authoritative Body & Metric Font:** **Manrope** (Weights: 400, 500, 600, 700, 800)
  - Applied to all body copy, descriptions, numeric metrics, timestamps, badges, status pills, table cells, form labels, and input fields.
  - Characterized by modern geometric clarity and superior legibility across compact mobile screens (360–430 dp).

## 2. Authoritative Palette
- **Primary Crimson:** `#C9212B` (Header gradient right, active pills, primary buttons, raised punch FAB)
- **Deep Federation Red:** `#971923` (Header gradient left, active card accents, prominent badges)
- **Warm Canvas Ground:** `#FAF9FF` / `#F7F6F3` (Master background, warm tatami cleanliness)
- **Pure Surface Card:** `#FFFFFF` (Surface container lowest, crisp card surfaces)
- **Primary On-Surface Text:** `#181B23` (Headings and high-contrast text)
- **Muted Text / Secondary:** `#777D87` / `#5B403E` (Labels, captions, timestamps, supporting notes)
- **Fine Dividers / Outlines:** `#E0E2ED` / `#E6E6E8` (Subtle 1px borders)
- **Status Success:** `#1E9B61` (Active status, attendance live, paid invoices)
- **Status Warning:** `#E9A52A` (Pending dues, grading review alerts)
- **Status Error:** `#BA1A1A` / `#D94141` (Absent status, overdue dues, alert banners)

## 3. Persistent Global Header & Federation Branding
- **Background:** Gradient from `#971923` to `#C9212B` with ambient drop shadow.
- **Crest Asset:** Approved JFSKIF crest vector asset (gold-bordered rising sun with karate punching silhouette and golden "JFSKIF" label) embedded cleanly without external dependencies.
- **Federation Title:** `JAPAN FUNAKOSHI SHOTOKAN KARATE INDIA FEDERATION (JFSKIF)` in bold uppercase.
- **Branding Subtitle:** Strictly `"Powered by DojoFLOW"` in sentence case on one line.
- **Actions:** Quick settings cog and user profile avatar.

## 4. Dojo Selection & Scoping Rules
- **Dashboard Exclusive:** The dojo branch switcher and "+ Add Branch" button appear strictly on the Home Dashboard.
- **Active Operational Context:** The selected branch (`Sri Sankara Dojo (Chennai)`) governs all app destinations (Students, Schedule, Events, Stats).
- **No Aggregate Mode:** The system enforces single-dojo scoping; no "ALL DOJOS" button is permitted.
- **Add Branch Action:** Provides seamless access to the new branch registration form (`add_dojo_branch_form/code.html`).

## 5. Persistent Bottom Navigation (5 Destinations)
1. **Students:** `sports_martial_arts` icon + `STUDENTS` label.
2. **Schedule:** `calendar_today` icon + `SCHEDULE` label.
3. **Home (Center Raised FAB):** 
   - 62–66 dp circular button with red gradient (`#971923` to `#C9212B`).
   - White KaratePunchEmblem centered.
   - **STRICTLY NO TEXT LABEL** underneath.
4. **Events:** `emoji_events` icon + `EVENTS` label.
5. **Stats:** `analytics` icon + `STATS` label (financial analytics, revenue, fee collections).

## 6. Sample Data & Temporal Consistency
- **Baseline Year:** Consistent **2026** operational sample dates (e.g., `Tuesday, 24 Nov 2026`). All legacy 2024/2025 references are deprecated.
- **Active Demonstration Dojo:** `Sri Sankara Dojo (Chennai)` managed by `Shihan Rajesh Kumar (6th Dan)`.
