package dev.primeclient.core.modules.qol;

import dev.primeclient.core.adapter.MinecraftAdapter;
import dev.primeclient.core.adapter.RenderContext;
import dev.primeclient.core.event.ClientTickEvent;
import dev.primeclient.core.hud.HudAnchor;
import dev.primeclient.core.hud.HudElement;
import dev.primeclient.core.hud.HudManager;
import dev.primeclient.core.module.BooleanSetting;
import dev.primeclient.core.module.IntSetting;
import dev.primeclient.core.module.Module;
import dev.primeclient.core.module.ModuleCategory;
import dev.primeclient.core.theme.Theme;
import dev.primeclient.core.theme.ThemeManager;
import dev.primeclient.core.util.ColorUtil;

/**
 * Circular terrain minimap with entity dots, north badge, and optional mark.
 * Terrain is cached and throttled; entity dots refresh every frame.
 */
public final class MinimapModule extends Module {

    private static final int PRIME_RED = 0xFFE11D2E;
    private static final long SAMPLE_INTERVAL_MS = 300L;
    private static final int MAX_ENTITIES = 64;
    /** Cap terrain buffer side so dense sampling stays FPS-friendly. */
    private static final int MAX_TERRAIN_SIDE = 161;

    private final IntSetting size = addSetting(new IntSetting(
            "size", "Size", "Minimap diameter in pixels", 96, 64, 160));
    private final IntSetting zoom = addSetting(new IntSetting(
            "zoom", "Zoom", "Terrain radius (1 = close, 6 = wide)", 3, 1, 6));
    private final BooleanSetting showPlayers = addSetting(new BooleanSetting(
            "show-players", "Show players", "Dot other players on the map", true));
    private final BooleanSetting showMobs = addSetting(new BooleanSetting(
            "show-mobs", "Show mobs", "Dot hostile and passive mobs", true));
    private final BooleanSetting rotateWithPlayer = addSetting(new BooleanSetting(
            "rotate", "Rotate with player", "Map faces your look direction (off = north up)", true));
    private final BooleanSetting showMark = addSetting(new BooleanSetting(
            "show-mark", "Show mark", "Show the saved position mark", true));
    private final BooleanSetting markHere = addSetting(new BooleanSetting(
            "mark-here", "Mark here", "Save your current position as the minimap mark", false));

    private final Element element;
    private final MinecraftAdapter adapter;

    private double markX;
    private double markZ;
    private boolean hasMark;

    public MinimapModule(HudManager hud, ThemeManager themes, MinecraftAdapter adapter) {
        super("minimap", "Minimap", "Circular terrain map around you", ModuleCategory.QOL);
        this.adapter = adapter;
        this.element = hud.register(new Element(themes, adapter, size, zoom,
                showPlayers, showMobs, rotateWithPlayer, showMark, this));
        element.setVisible(false);
        listen(ClientTickEvent.class, event -> onTick());
    }

    @Override
    protected void onEnable() {
        element.setVisible(true);
    }

    @Override
    protected void onDisable() {
        element.setVisible(false);
        markHere.set(false);
    }

    private void onTick() {
        if (!markHere.get()) {
            return;
        }
        markHere.set(false);
        if (!adapter.hasPlayer()) {
            return;
        }
        markX = adapter.playerX();
        markZ = adapter.playerZ();
        hasMark = true;
    }

    boolean hasMark() {
        return hasMark;
    }

    double markX() {
        return markX;
    }

    double markZ() {
        return markZ;
    }

    private static final class Element extends HudElement {

        private final ThemeManager themes;
        private final MinecraftAdapter adapter;
        private final IntSetting size;
        private final IntSetting zoom;
        private final BooleanSetting showPlayers;
        private final BooleanSetting showMobs;
        private final BooleanSetting rotateWithPlayer;
        private final BooleanSetting showMark;
        private final MinimapModule owner;

        private int[] terrain;
        private int terrainSide;
        private int lastRadius = -1;
        private int lastDensity = -1;
        private boolean lastRotate;
        private long lastSampleMillis;
        private double lastSampleX = Double.NaN;
        private double lastSampleZ = Double.NaN;

        private final float[] entX = new float[MAX_ENTITIES];
        private final float[] entZ = new float[MAX_ENTITIES];
        private final byte[] entType = new byte[MAX_ENTITIES];
        private int entCount;

        Element(ThemeManager themes, MinecraftAdapter adapter,
                IntSetting size, IntSetting zoom,
                BooleanSetting showPlayers, BooleanSetting showMobs,
                BooleanSetting rotateWithPlayer, BooleanSetting showMark,
                MinimapModule owner) {
            super("minimap", "Minimap", HudAnchor.TOP_RIGHT, -8, 8);
            this.themes = themes;
            this.adapter = adapter;
            this.size = size;
            this.zoom = zoom;
            this.showPlayers = showPlayers;
            this.showMobs = showMobs;
            this.rotateWithPlayer = rotateWithPlayer;
            this.showMark = showMark;
            this.owner = owner;
        }

        @Override
        public int measureWidth(RenderContext ctx) {
            return size.get();
        }

        @Override
        public int measureHeight(RenderContext ctx) {
            return size.get();
        }

        @Override
        public void render(RenderContext ctx, long nowMillis) {
            int diameter = size.get();
            int radiusPx = diameter / 2;
            Theme theme = themes.active();
            int accent = theme.accent() != 0 ? theme.accent() : PRIME_RED;

            drawFrame(ctx, diameter, radiusPx, accent);

            if (!adapter.hasPlayer() || !adapter.isInGame()) {
                drawPlayerArrow(ctx, radiusPx, radiusPx, accent);
                return;
            }

            int blockRadius = blockRadius();
            int density = sampleDensity(blockRadius);
            boolean rotate = rotateWithPlayer.get();
            maybeResampleTerrain(nowMillis, blockRadius, density, rotate);
            drawTerrain(ctx, diameter, radiusPx);

            float range = blockRadius + 2f;
            entCount = adapter.minimapSampleEntities(range, rotate, entX, entZ, entType);
            drawEntities(ctx, diameter, radiusPx, blockRadius);
            drawMark(ctx, diameter, radiusPx, blockRadius, rotate, accent);
            drawNorth(ctx, diameter, radiusPx, rotate, accent);
            drawPlayerArrow(ctx, radiusPx, radiusPx, accent);
        }

        private int blockRadius() {
            // zoom 1 → 16 blocks, zoom 6 → 56 blocks
            return 8 + zoom.get() * 8;
        }

        /** Prefer 2× sub-block samples when the buffer stays under {@link #MAX_TERRAIN_SIDE}. */
        private static int sampleDensity(int blockRadius) {
            if (blockRadius * 4 + 1 <= MAX_TERRAIN_SIDE) {
                return 2;
            }
            return 1;
        }

        private void drawFrame(RenderContext ctx, int diameter, int radiusPx, int accent) {
            // Soft outer drop
            ctx.fillSoftShadow(2, 3, diameter + 2, diameter + 2, radiusPx + 1, 0x88000000);
            // Outer dark ring (glass rim)
            ctx.fillRoundedRect(-2, -2, diameter + 4, diameter + 4, radiusPx + 2, 0xEE050508);
            // Accent hairline
            ctx.fillRoundedBorder(-1, -1, diameter + 2, diameter + 2, radiusPx + 1, 1,
                    ColorUtil.withAlpha(accent, 0.55f), 0x00000000);
            // Inner glass plate
            ctx.fillRoundedRect(0, 0, diameter, diameter, radiusPx, 0xF008080C);
            // Soft inner vignette ring
            ctx.fillRoundedBorder(1, 1, diameter - 2, diameter - 2, Math.max(1, radiusPx - 1), 1,
                    0x66000000, 0x00000000);
            // Thin prime accent on the outer edge
            ctx.fillRoundedBorder(0, 0, diameter, diameter, radiusPx, 1,
                    ColorUtil.withAlpha(accent, 0.92f), 0x00000000);
        }

        private void maybeResampleTerrain(long nowMillis, int blockRadius, int density, boolean rotate) {
            boolean moved = Double.isNaN(lastSampleX)
                    || Math.abs(adapter.playerX() - lastSampleX) >= 1.25
                    || Math.abs(adapter.playerZ() - lastSampleZ) >= 1.25;
            boolean settingsChanged = blockRadius != lastRadius
                    || density != lastDensity
                    || rotate != lastRotate;
            boolean due = nowMillis - lastSampleMillis >= SAMPLE_INTERVAL_MS;
            if (!moved && !settingsChanged && !due && terrain != null) {
                return;
            }
            int side = blockRadius * 2 * density + 1;
            if (terrain == null || terrainSide != side) {
                terrain = new int[side * side];
                terrainSide = side;
            }
            if (adapter.minimapSampleSurface(blockRadius, density, rotate, terrain)) {
                lastSampleMillis = nowMillis;
                lastSampleX = adapter.playerX();
                lastSampleZ = adapter.playerZ();
                lastRadius = blockRadius;
                lastDensity = density;
                lastRotate = rotate;
            }
        }

        private void drawTerrain(RenderContext ctx, int diameter, int radiusPx) {
            if (terrain == null || terrainSide <= 0) {
                return;
            }
            float scale = diameter / (float) terrainSide;
            float edgeSoft = 1.6f;
            float rOuter = radiusPx - 0.5f;
            float rInner = rOuter - edgeSoft;
            float rOuter2 = rOuter * rOuter;
            float rInner2 = rInner * rInner;

            for (int ty = 0; ty < terrainSide; ty++) {
                int row = ty * terrainSide;
                int runColor = 0;
                int runStartSx = 0;
                boolean haveRun = false;
                int sy = Math.round(ty * scale);
                int sh = Math.max(1, Math.round((ty + 1) * scale) - sy);
                float cy = sy + sh * 0.5f - radiusPx;

                for (int tx = 0; tx <= terrainSide; tx++) {
                    int color = 0;
                    int sx = Math.round(tx * scale);
                    if (tx < terrainSide) {
                        color = terrain[row + tx];
                        float cx = sx + scale * 0.5f - radiusPx;
                        float d2 = cx * cx + cy * cy;
                        if (d2 > rOuter2) {
                            color = 0;
                        } else if (d2 > rInner2) {
                            float d = (float) Math.sqrt(d2);
                            float a = 1f - (d - rInner) / edgeSoft;
                            color = ColorUtil.withAlpha(color, Math.clamp(a, 0f, 1f) * 0.95f);
                        }
                    }
                    if (haveRun && color == runColor && tx < terrainSide) {
                        continue;
                    }
                    if (haveRun && runColor != 0) {
                        int sw = Math.max(1, sx - runStartSx);
                        ctx.fillRect(runStartSx, sy, sw, sh, runColor);
                    }
                    if (tx >= terrainSide) {
                        break;
                    }
                    runColor = color;
                    runStartSx = sx;
                    haveRun = true;
                }
            }
        }

        private void drawEntities(RenderContext ctx, int diameter, int radiusPx, int blockRadius) {
            float scale = diameter / (float) (blockRadius * 2 + 1);
            int r2 = (radiusPx - 4) * (radiusPx - 4);
            for (int i = 0; i < entCount; i++) {
                byte type = entType[i];
                if (type == MinecraftAdapter.MINIMAP_ENTITY_PLAYER && !showPlayers.get()) {
                    continue;
                }
                if ((type == MinecraftAdapter.MINIMAP_ENTITY_HOSTILE
                        || type == MinecraftAdapter.MINIMAP_ENTITY_PASSIVE) && !showMobs.get()) {
                    continue;
                }
                float mx = entX[i];
                float mz = entZ[i];
                int sx = radiusPx + Math.round(mx * scale);
                int sy = radiusPx + Math.round(mz * scale);
                int dx = sx - radiusPx;
                int dy = sy - radiusPx;
                if (dx * dx + dy * dy > r2) {
                    continue;
                }
                boolean player = type == MinecraftAdapter.MINIMAP_ENTITY_PLAYER;
                int color = switch (type) {
                    case MinecraftAdapter.MINIMAP_ENTITY_PLAYER -> 0xFF3B82F6;
                    case MinecraftAdapter.MINIMAP_ENTITY_HOSTILE -> 0xFFEF4444;
                    case MinecraftAdapter.MINIMAP_ENTITY_PASSIVE -> 0xFF22C55E;
                    default -> 0xFFFBBF24;
                };
                if (player) {
                    // Larger diamond with dark outline
                    drawDiamond(ctx, sx, sy, 3, 0xE0000000);
                    drawDiamond(ctx, sx, sy, 2, color);
                    ctx.fillRect(sx, sy, 1, 1, 0xFFFFFFFF);
                } else {
                    int body = 2;
                    ctx.fillRect(sx - body - 1, sy - body - 1, body * 2 + 3, body * 2 + 3, 0xE0000000);
                    ctx.fillRect(sx - body, sy - body, body * 2 + 1, body * 2 + 1, color);
                }
            }
        }

        private static void drawDiamond(RenderContext ctx, int cx, int cy, int radius, int argb) {
            for (int dy = -radius; dy <= radius; dy++) {
                int half = radius - Math.abs(dy);
                ctx.fillRect(cx - half, cy + dy, half * 2 + 1, 1, argb);
            }
        }

        private void drawMark(RenderContext ctx, int diameter, int radiusPx, int blockRadius,
                              boolean rotate, int accent) {
            if (!showMark.get() || !owner.hasMark()) {
                return;
            }
            double dx = owner.markX() - adapter.playerX();
            double dz = owner.markZ() - adapter.playerZ();
            float mx;
            float mz;
            if (rotate) {
                float yaw = adapter.playerYaw();
                double rad = Math.toRadians(yaw);
                float sin = (float) Math.sin(rad);
                float cos = (float) Math.cos(rad);
                float forwardX = -sin;
                float forwardZ = cos;
                float rightX = cos;
                float rightZ = sin;
                mx = (float) (dx * rightX + dz * rightZ);
                mz = (float) -(dx * forwardX + dz * forwardZ);
            } else {
                mx = (float) dx;
                mz = (float) dz;
            }
            float scale = diameter / (float) (blockRadius * 2 + 1);
            int sx = radiusPx + Math.round(mx * scale);
            int sy = radiusPx + Math.round(mz * scale);
            int ddx = sx - radiusPx;
            int ddy = sy - radiusPx;
            if (ddx * ddx + ddy * ddy > (radiusPx - 5) * (radiusPx - 5)) {
                return;
            }
            ctx.fillRect(sx - 1, sy - 3, 3, 7, 0xE0000000);
            ctx.fillRect(sx - 3, sy - 1, 7, 3, 0xE0000000);
            ctx.fillRect(sx, sy - 2, 1, 5, accent);
            ctx.fillRect(sx - 2, sy, 5, 1, accent);
        }

        private void drawNorth(RenderContext ctx, int diameter, int radiusPx, boolean rotate, int accent) {
            float angle;
            if (rotate) {
                angle = -adapter.playerYaw() - 180f;
            } else {
                angle = -90f;
            }
            double rad = Math.toRadians(angle);
            int nx = radiusPx + (int) Math.round(Math.cos(rad) * (radiusPx - 11));
            int ny = radiusPx + (int) Math.round(Math.sin(rad) * (radiusPx - 11));
            int badge = 11;
            ctx.fillRoundedRect(nx - badge / 2, ny - badge / 2, badge, badge, badge / 2, 0xDD050508);
            ctx.fillRoundedBorder(nx - badge / 2, ny - badge / 2, badge, badge, badge / 2, 1,
                    ColorUtil.withAlpha(accent, 0.9f), 0x00000000);
            int tw = ctx.uiTextWidth("N");
            ctx.drawUiText("N", nx - tw / 2, ny - ctx.uiFontHeight() / 2, ColorUtil.withAlpha(accent, 0.98f));
        }

        private void drawPlayerArrow(RenderContext ctx, int cx, int cy, int accent) {
            double rad;
            if (rotateWithPlayer.get()) {
                rad = Math.toRadians(-90); // tip up when map rotates with look
            } else if (adapter.hasPlayer()) {
                rad = Math.toRadians(adapter.playerYaw() + 90);
            } else {
                rad = Math.toRadians(-90);
            }
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            // Tip / wings in local “up” space, then rotate
            float tipLen = 6.5f;
            float wingLen = 4.2f;
            float wingSpread = 2.6f;
            int tipX = cx + Math.round(cos * tipLen);
            int tipY = cy + Math.round(sin * tipLen);
            int leftX = cx + Math.round(cos * -wingLen + (-sin) * wingSpread);
            int leftY = cy + Math.round(sin * -wingLen + cos * wingSpread);
            int rightX = cx + Math.round(cos * -wingLen + (-sin) * -wingSpread);
            int rightY = cy + Math.round(sin * -wingLen + cos * -wingSpread);
            // Outline then fill
            fillTriangle(ctx, tipX, tipY, leftX, leftY, rightX, rightY, 0xF0000000, 1);
            fillTriangle(ctx, tipX, tipY, leftX, leftY, rightX, rightY, 0xFFF5F5F7, 0);
            // Accent tip notch
            int midX = (leftX + rightX) / 2;
            int midY = (leftY + rightY) / 2;
            int notchX = (tipX * 2 + midX) / 3;
            int notchY = (tipY * 2 + midY) / 3;
            ctx.fillRect(notchX - 1, notchY - 1, 3, 3, accent);
            ctx.fillRect(cx - 1, cy - 1, 3, 3, ColorUtil.withAlpha(accent, 0.85f));
        }

        /** Scanline-filled triangle; outlineGrow expands silhouette when &gt; 0. */
        private static void fillTriangle(RenderContext ctx,
                                         int x0, int y0, int x1, int y1, int x2, int y2,
                                         int argb, int outlineGrow) {
            int minY = Math.min(y0, Math.min(y1, y2)) - outlineGrow;
            int maxY = Math.max(y0, Math.max(y1, y2)) + outlineGrow;
            int[] span = new int[2];
            for (int y = minY; y <= maxY; y++) {
                span[0] = Integer.MAX_VALUE;
                span[1] = Integer.MIN_VALUE;
                scanEdge(y, x0, y0, x1, y1, span);
                scanEdge(y, x1, y1, x2, y2, span);
                scanEdge(y, x2, y2, x0, y0, span);
                if (span[0] > span[1]) {
                    continue;
                }
                int xMin = span[0] - outlineGrow;
                int xMax = span[1] + outlineGrow;
                ctx.fillRect(xMin, y, xMax - xMin + 1, 1, argb);
            }
        }

        private static void scanEdge(int y, int x0, int y0, int x1, int y1, int[] span) {
            if (y0 == y1) {
                if (y == y0) {
                    span[0] = Math.min(span[0], Math.min(x0, x1));
                    span[1] = Math.max(span[1], Math.max(x0, x1));
                }
                return;
            }
            int yMin = Math.min(y0, y1);
            int yMax = Math.max(y0, y1);
            if (y < yMin || y > yMax) {
                return;
            }
            float t = (y - y0) / (float) (y1 - y0);
            int x = Math.round(x0 + (x1 - x0) * t);
            span[0] = Math.min(span[0], x);
            span[1] = Math.max(span[1], x);
        }
    }
}
