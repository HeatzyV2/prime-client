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

    static GameMenuLayout compute(int screenWidth, int screenHeight) {
        int logoH = Math.max(22, Math.min(36, Math.round(screenHeight * 0.045f)));
        int brandBlock = logoH + 28;
        int header = brandBlock + 18;
        int panelInner = INNER_PAD * 2 + PRIMARY_H + GAP
                + CELL_H * 3 + GAP * 2
                + GAP + QUIT_H;
        int panelH = panelInner;
        int panelW = Math.min(PANEL_W, Math.max(240, screenWidth - 48));
        int totalH = header + panelH + 10 + FOOTER_H;
        int startY = Math.max(8, (screenHeight - totalH) / 2 - 4);

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
                startY + logoH + 2,
                startY + brandBlock + 2,
                startY + brandBlock + 10,
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
                screenHeight - FOOTER_H,
                FOOTER_H
        );
    }

    int gridCellX(int col) {
        return gridX + col * (cellW + cellGapX);
    }

    int gridCellY(int row) {
        return gridY + row * (cellH + cellGapY);
    }
}
