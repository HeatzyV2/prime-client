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
    static final int PANEL_W = 280;
    static final int PRIMARY_H = 28;
    static final int CELL_H = 24;
    static final int QUIT_H = 26;
    static final int INNER_PAD = 14;
    static final int GAP = 8;
    static final int FOOTER_H = 28;
    /** Space after logo before PRIME wordmark. */
    static final int LOGO_TO_BRAND = 4;
    /** PRIME + CLIENT stack height (two lines). */
    static final int BRAND_TEXT_H = 24;
    /** Divider → GAME MENU → panel breathing room. */
    static final int DIVIDER_TO_TITLE = 8;
    static final int TITLE_TO_PANEL = 10;

    static GameMenuLayout compute(int screenWidth, int screenHeight) {
        int footerH = FOOTER_H;
        int footerY = screenHeight - footerH;
        // Usable area excludes the sticky footer so the composition centers optically.
        int usableH = Math.max(1, footerY);

        int logoH = Math.max(22, Math.min(34, Math.round(screenHeight * 0.042f)));
        int brandY = logoH + LOGO_TO_BRAND;
        int dividerY = brandY + BRAND_TEXT_H;
        int titleY = dividerY + DIVIDER_TO_TITLE;
        int header = titleY + TITLE_TO_PANEL; // end of branding → start of panel

        int panelInner = INNER_PAD * 2 + PRIMARY_H + GAP
                + CELL_H * 3 + GAP * 2
                + GAP + QUIT_H;
        int panelH = panelInner;
        int panelW = Math.min(PANEL_W, Math.max(240, screenWidth - 48));

        int compositionH = header + panelH;
        // Center in usable area; slight upward bias so heavy panel doesn't feel low.
        int startY = Math.max(6, (usableH - compositionH) / 2 - 6);
        // Keep a clear gap above the footer bar.
        int maxStart = Math.max(6, usableH - compositionH - 10);
        startY = Math.min(startY, maxStart);

        int panelX = (screenWidth - panelW) / 2;
        int panelY = startY + header;
        int contentX = panelX + INNER_PAD;
        int contentW = panelW - INNER_PAD * 2;
        int primaryY = panelY + INNER_PAD;
        int gridY = primaryY + PRIMARY_H + GAP;
        int cellGapX = 8;
        int cellW = (contentW - cellGapX) / 2;
        int quitY = gridY + CELL_H * 3 + GAP * 2 + GAP;

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

    int gridCellX(int col) {
        return gridX + col * (cellW + cellGapX);
    }

    int gridCellY(int row) {
        return gridY + row * (cellH + cellGapY);
    }
}
