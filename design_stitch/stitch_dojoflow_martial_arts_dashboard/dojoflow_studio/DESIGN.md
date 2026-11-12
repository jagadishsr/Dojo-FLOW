---
name: DojoFLOW Studio
colors:
  surface: '#f8f9ff'
  surface-dim: '#d5dae5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eef4ff'
  surface-container: '#e9eef9'
  surface-container-high: '#e3e8f4'
  surface-container-highest: '#dde3ee'
  on-surface: '#161c24'
  on-surface-variant: '#5b403e'
  inverse-surface: '#2b3139'
  inverse-on-surface: '#ebf1fc'
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
  tertiary: '#4c4f58'
  on-tertiary: '#ffffff'
  tertiary-container: '#656771'
  on-tertiary-container: '#e5e6f2'
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
  tertiary-fixed: '#e0e2ed'
  tertiary-fixed-dim: '#c4c6d1'
  on-tertiary-fixed: '#181b23'
  on-tertiary-fixed-variant: '#44474f'
  background: '#f8f9ff'
  on-background: '#161c24'
  surface-variant: '#dde3ee'
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
  headline-lg-mobile:
    fontFamily: Rajdhani
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 30px
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
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-tablet: 1.25rem
  gutter-desktop: 1.5rem
  margin: 1rem
  margin-tablet: 2rem
  margin-desktop: 2.5rem
  space-2xs: 0.25rem
  space-xs: 0.5rem
  space-sm: 0.75rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
  space-2xl: 3rem
---

# DojoFLOW Studio — Design System & Authoritative Specification (Sprint 04 Final)

## Brand & Visual Identity (Lacquered Federation Modernism)

This design system delivers an operational interface tailored for martial arts academies, dojos, and modern federation management. Combining the discipline and intentionality of martial practice with structural enterprise efficiency, the visual atmosphere balances precision, respect, and functional agility.

The visual movement anchors in **Lacquered Federation Modernism**:
- **Tone:** Authoritative, disciplined, refined, and frictionless.
- **Audience:** Academy owners, Top Sensei Admins, Shihans, Senseis, front-desk administrators, and practitioners.
- **Atmospheric Stance:** Interfaces avoid loud gamification or generic aesthetics. Respect, symmetry, and clear hierarchy dominate, utilizing warm grounds, controlled crimson accents, crisp outlines, and generous spatial discipline.

## Typography Architecture (Rajdhani + Manrope Direction)

Typography strictly follows the unified dual-font system:
- **Rajdhani (Authoritative Display & Heading Font):**
  - Applied to federation titles, screen headers, section titles, class/event names, modal titles, and student display names.
  - Weights: 500 (Medium), 600 (SemiBold), 700 (Bold).
  - Characterized by angular martial precision, condensed geometry, and high executive authority.
- **Manrope (Authoritative Body, Metrics & Supporting Font):**
  - Applied to all body copy, descriptions, numeric metrics, timestamps, badges, status pills, table cells, form labels, and input fields.
  - Weights: 400 (Regular), 500 (Medium), 600 (SemiBold), 700 (Bold), 800 (ExtraBold).
  - Delivers exceptional numeric and micro-text legibility on compact mobile viewports (360–430 dp).

## Authoritative Palette

The palette employs a restrained, intentional balance:
- **Primary Red (`#C9212B`):** Applied to focal interaction points, state affirmations, primary CTAs, active tab highlights, and the raised punch FAB.
- **Deep Red (`#971923`):** Anchors hover states, key micro-gradients (`linear-gradient(135deg, #971923 0%, #C9212B 100%)`), and elevated metric callouts.
- **On-Surface Ink (`#161C24`):** Crisp high-contrast primary text and icons.
- **Secondary Neutral (`#5B403E` / `#777D87`):** Metadata, column headers, secondary action states, and subtle badges.
- **Warm Canvas Ground (`#F8F9FF` / `#F7F6F3`):** Replaces harsh sterile digital whites with a warm, tatami-neutral ground layer. Elevated cards, modals, and sheets rest atop this canvas in pure white (`#FFFFFF`).
- **Semantic Accents:** Success (`#1E9B61`) highlights belt advancement, cleared memberships, and valid payments; Warning (`#E9A52A`) tracks inventory thresholds and renewal expirations; Error (`#BA1A1A`) indicates absences or overdue balances.

## Global Header & Federation Branding
- **Background:** Gradient `#971923` to `#C9212B` with ambient drop shadow.
- **Crest Asset:** Approved JFSKIF crest vector asset embedded cleanly as a self-contained inline SVG (gold-bordered rising sun with karate punching silhouette and golden "JFSKIF" label) eliminating broken third-party image URLs.
- **Federation Title:** `JAPAN FUNAKOSHI SHOTOKAN KARATE INDIA FEDERATION (JFSKIF)` in bold uppercase.
- **Branding Subtitle:** Strictly `"Powered by DojoFLOW"` in sentence case on one line.
- **Header Actions:** Settings gear icon and user profile avatar.

## Dojo Selection & Scoping Rules
- **Dashboard Only:** The dojo branch switcher and "+ Add Branch" button appear strictly on the Home Dashboard.
- **Contextual Screen Content:** All other destinations (Students, Schedule, Events, Stats) automatically use the selected dojo context (`Sri Sankara Dojo (Chennai)`).
- **Single Operational Context:** The system enforces single-dojo scoping; no "ALL DOJOS" or aggregate branch selection is allowed.
- **Add Branch Action:** Links directly to the new branch registration form (`add_dojo_branch_form/code.html`).

## Persistent Bottom Navigation
- Five fixed destinations:
  1. **Students** (`sports_martial_arts`)
  2. **Schedule** (`calendar_today`)
  3. **Home** (Central raised 62–66 dp circular button with red gradient and white KaratePunchEmblem — **STRICTLY NO TEXT LABEL**)
  4. **Events** (`emoji_events`)
  5. **Stats** (`analytics` - financial analytics and fee collections)

## Sample Data & Temporal Consistency
- **Year:** Consistent **2026** operational sample dates across all screens.
- **Active Demonstration Dojo:** `Sri Sankara Dojo (Chennai)` managed by `Shihan Rajesh Kumar (6th Dan)`.