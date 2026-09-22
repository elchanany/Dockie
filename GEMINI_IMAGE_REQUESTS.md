# Gemini image requests — Dockie Play graphics

Create only the assets below. Existing launcher artwork is a bulb on a dock (warm paper background `#F6F3EC`, ink `#1C1D21`, amber glow `#E39B2D`, sap green accents `#3E5C4B` / `#A8CBB2`). Brand: Dockie by EYC Digital. No stock phone mockups with fake UI. No purple gradients. No emojis.

---

## 1) Feature graphic

- **Dimensions:** 1024 × 500 px  
- **Format:** PNG ≤ 1 MB  
- **Purpose:** Google Play feature graphic  
- **Text constraints:** Optional wordmark “Dockie” only; no long marketing copy; keep critical content inside a center safe zone (~824×400)  
- **Reference art:** `app/src/main/res/drawable/ic_launcher_foreground.xml`, `ic_bulb_filled.xml`, palette in `ui/theme/Color.kt`  
- **Visual brief:** Calm premium full-bleed composition — soft warm paper-to-cream atmosphere, large simplified bulb-on-dock mark left or center-left, subtle wireless-charge ring, generous negative space, modern minimal utility product feel  

**Ready-to-paste Gemini prompt:**

```
Create a Google Play feature graphic, exactly 1024x500 pixels, PNG. Brand: Dockie by EYC Digital. Minimal premium utility aesthetic. Background: soft warm paper cream (#F6F3EC) with very subtle depth, not flat white, no purple, no neon glow spam. Hero mark: a simple line-drawn light bulb resting on a curved wireless charging dock dish, dark ink strokes (#1C1D21), small amber glowing core (#E39B2D). Optional small clean wordmark “Dockie” in a modern geometric sans, restrained. No fake phone screenshots, no UI cards, no badges, no stickers, no emojis, no stock photos. Keep the mark and title inside the center safe area. Calm, lightweight, trustworthy.
```

---

## 2) Optional polished 512×512 store icon (only if upgrading)

A deterministic `play-store-assets/icon-512.png` already exists from the launcher motif. Replace only if you want higher polish.

- **Dimensions:** 512 × 512  
- **Format:** PNG ≤ 1 MB  
- **Full-bleed icon,** no rounded-rect mask (Play applies mask)  

**Prompt:**

```
Create a 512x512 Google Play app icon PNG for Dockie. Full-bleed square, no rounded corners baked in. Soft warm cream background (#F6F3EC). Centered simple bulb-on-wireless-dock mark: dark ink outlines (#1C1D21), amber core (#E39B2D), clean geometric lines, generous padding for adaptive masking. No text, no shadow stacks, no purple, no glossy skeuomorphism.
```
