package dev.primeclient.core.gui.menu;

/** Geometry for the Prime pause / game menu. */
record GameMenuLayout(
        int logoY,
        int logoH,
        int brandY,
        int dividerY,
        int titleY,
        int panelX,
        int panelY,
        int panelW,
        int panelH,
        int primaryX,
        int primaryY,
        int primaryW,
        int primaryH,
        int socialX,
        int socialY,
        int socialW,
        int socialH,
        int gridX,
        int gridY,
        int cellW,
        int cellH,
        int cellGapX,
        int cellGapY,
        int quitX,
        int quitY,
        int quitW,
        int quitH,
        int footerY,
        int footerH
) {
    static final int PANEL_W = 288;
    static final int PRIMARY_H = 30;
    static final int SOCIAL_H = 26;
    static final int CELL_H = 26;
    static final int QUIT_H = 28;
    static final int INNER_PAD = 16;
    static final int GAP = 8;
    static final int FOOTER_H = 22;
    /** Space after logo before PRIME wordmark. */
    static final int LOGO_TO_BRAND = 4;
    /** PRIME + CLIENT stack height (two lines). */
    static final int BRAND_TEXT_H = 24;
    /** Divider → GAME MENU → panel: keep as one tight block. */
    static final int DIVIDER_TO_TITLE = 4;
    /** Glyph room for the GAME MENU line (top of text → bottom of glyphs). */
    static final int TITLE_LINE_H = 9;
    /** Breath between GAME MENU glyphs and panel top edge. */
    static final int TITLE_TO_PANEL = 4;
    /**
     * Slight upward nudge from true vertical center so the block does not feel
     * heavy toward the sticky footer (optical center, not math center).
     */
    static final int OPTICAL_UPWARD_BIAS = 12;
    /** Minimum clear gap between panel bottom and sticky footer. */
    static final int FOOTER_CLEARANCE = 20;
    static final int GRID_ROWS = 3;

    static GameMenuLayout compute(int screenWidth, int screenHeight) {
        int footerH = FOOTER_H;
        int footerY = screenHeight - footerH;
        // Usable area excludes the sticky footer — center within that band only.
        int usableH = Math.max(1, footerY);

        int logoH = Math.max(22, Math.min(34, Math.round(screenHeight * 0.042f)));
        int brandY = logoH + LOGO_TO_BRAND;
        int dividerY = brandY + BRAND_TEXT_H;
        int titleY = dividerY + DIVIDER_TO_TITLE;
        // titleY is text top; reserve glyph height then a tight gap to the panel.
        int header = titleY + TITLE_LINE_H + TITLE_TO_PANEL;

        int panelInner = INNER_PAD * 2
                + PRIMARY_H + GAP
                + SOCIAL_H + GAP
                + CELL_H * GRID_ROWS + GAP * (GRID_ROWS - 1)
                + GAP + QUIT_H;
        int panelH = panelInner;
        int panelW = Math.min(PANEL_W, Math.max(248, screenWidth - 48));

        int compositionH = header + panelH;
        int free = Math.max(0, usableH - compositionH);
        // True vertical center of usable area, then a small upward optical bias.
        int startY = Math.max(6, (free / 2) - OPTICAL_UPWARD_BIAS);
        // Keep a clear gap above the footer bar.
        int maxStart = Math.max(6, usableH - compositionH - FOOTER_CLEARANCE);
        startY = Math.min(startY, maxStart);

        int panelX = (screenWidth - panelW) / 2;
        int panelY = startY + header;
        int contentX = panelX + INNER_PAD;
        int contentW = panelW - INNER_PAD * 2;
        int primaryY = panelY + INNER_PAD;
        int socialY = primaryY + PRIMARY_H + GAP;
        int gridY = socialY + SOCIAL_H + GAP;
        int cellGapX = 8;
        int cellW = (contentW - cellGapX) / 2;
        int quitY = gridY + CELL_H * GRID_ROWS + GAP * (GRID_ROWS - 1) + GAP;

        return new GameMenuLayout(
                startY,
                logoH,
                startY + brandY,
                startY + dividerY,
                startY + titleY,
                panelX,
                panelY,
                panelW,
                panelH,
                contentX,
                primaryY,
                contentW,
                PRIMARY_H,
                contentX,
                socialY,
                contentW,
                SOCIAL_H,
                contentX,
                gridY,
                cellW,
                CELL_H,
                cellGapX,
                GAP,
                contentX,
                quitY,
                contentW,
                QUIT_H,
                footerY,
                footerH
        );
    }

    int contentW() {
        return primaryW;
    }

    int gridCellX(int col) {
        return gridX + col * (cellW + cellGapX);
    }

    int gridCellY(int row) {
        return gridY + row * (cellH + cellGapY);
    }
}
