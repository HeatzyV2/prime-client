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
 * Circular terrain minimap with entity dots, north indicator, and optional mark.
 * Terrain pixels are throttled; entity dots refresh every frame.
 */
public final class MinimapModule extends Module {

    private static final int PRIME_RED = 0xFFE11D2E;
    private static final long SAMPLE_INTERVAL_MS = 280L;
    private static final int MAX_ENTITIES = 64;

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

            ctx.fillSoftShadow(1, 1, diameter + 2, diameter + 2, radiusPx + 1, 0x66000000);
            ctx.fillRoundedRect(0, 0, diameter, diameter, radiusPx, 0xEE0A0A0C);
            ctx.fillRoundedBorder(0, 0, diameter, diameter, radiusPx, 2,
                    ColorUtil.withAlpha(accent, 0.85f), 0x00000000);

            if (!adapter.hasPlayer() || !adapter.isInGame()) {
                drawPlayerArrow(ctx, radiusPx, radiusPx, 0f, accent);
                return;
            }

            int blockRadius = blockRadius();
            boolean rotate = rotateWithPlayer.get();
            maybeResampleTerrain(nowMillis, blockRadius, rotate);
            drawTerrain(ctx, diameter, radiusPx, blockRadius);

            float range = blockRadius + 2f;
            entCount = adapter.minimapSampleEntities(range, rotate, entX, entZ, entType);
            drawEntities(ctx, diameter, radiusPx, blockRadius);
            drawMark(ctx, diameter, radiusPx, blockRadius, rotate, accent);
            drawNorth(ctx, diameter, radiusPx, rotate, accent);
            drawPlayerArrow(ctx, radiusPx, radiusPx, 0f, accent);
        }

        private int blockRadius() {
            // zoom 1 → 16 blocks, zoom 6 → 56 blocks
            return 8 + zoom.get() * 8;
        }

        private void maybeResampleTerrain(long nowMillis, int blockRadius, boolean rotate) {
            boolean moved = Double.isNaN(lastSampleX)
                    || Math.abs(adapter.playerX() - lastSampleX) >= 1.5
                    || Math.abs(adapter.playerZ() - lastSampleZ) >= 1.5;
            boolean settingsChanged = blockRadius != lastRadius || rotate != lastRotate;
            boolean due = nowMillis - lastSampleMillis >= SAMPLE_INTERVAL_MS;
            if (!moved && !settingsChanged && !due && terrain != null) {
                return;
            }
            int side = blockRadius * 2 + 1;
            if (terrain == null || terrainSide != side) {
                terrain = new int[side * side];
                terrainSide = side;
            }
            if (adapter.minimapSampleSurface(blockRadius, rotate, terrain)) {
                lastSampleMillis = nowMillis;
                lastSampleX = adapter.playerX();
                lastSampleZ = adapter.playerZ();
                lastRadius = blockRadius;
                lastRotate = rotate;
            }
        }

        private void drawTerrain(RenderContext ctx, int diameter, int radiusPx, int blockRadius) {
            if (terrain == null || terrainSide <= 0) {
                return;
            }
            float scale = diameter / (float) terrainSide;
            int r2 = radiusPx * radiusPx;
            for (int ty = 0; ty < terrainSide; ty++) {
                int row = ty * terrainSide;
                int px = -1;
                int runColor = 0;
                int runStartSx = 0;
                int sy = Math.round(ty * scale);
                int sh = Math.max(1, Math.round((ty + 1) * scale) - sy);
                for (int tx = 0; tx <= terrainSide; tx++) {
                    int color = tx < terrainSide ? terrain[row + tx] : 0;
                    int sx = Math.round(tx * scale);
                    // Circular mask in pixel space
                    if (tx < terrainSide) {
                        int cx = sx + (Math.round(scale) / 2) - radiusPx;
                        int cy = sy + sh / 2 - radiusPx;
                        if (cx * cx + cy * cy > r2 - 4) {
                            color = 0;
                        }
                    }
                    if (color == runColor && tx < terrainSide) {
                        continue;
                    }
                    if (runColor != 0 && px >= 0) {
                        int sw = Math.max(1, sx - runStartSx);
                        ctx.fillRect(runStartSx, sy, sw, sh, runColor);
                    }
                    runColor = color;
                    runStartSx = sx;
                    px = tx;
                }
            }
        }

        private void drawEntities(RenderContext ctx, int diameter, int radiusPx, int blockRadius) {
            float scale = diameter / (float) (blockRadius * 2 + 1);
            int r2 = (radiusPx - 3) * (radiusPx - 3);
            for (int i = 0; i < entCount; i++) {
                byte type = entType[i];
                if (type == MinecraftAdapter.MINIMAP_ENTITY_PLAYER && !showPlayers.get()) {
                    continue;
                }
                if ((type == MinecraftAdapter.MINIMAP_ENTITY_HOSTILE
                        || type == MinecraftAdapter.MINIMAP_ENTITY_PASSIVE) && !showMobs.get()) {
                    continue;
                }
                // Surface sample uses +X east, +row south; entity rel matches that
                float mx = entX[i];
                float mz = entZ[i];
                int sx = radiusPx + Math.round(mx * scale);
                int sy = radiusPx + Math.round(mz * scale);
                int dx = sx - radiusPx;
                int dy = sy - radiusPx;
                if (dx * dx + dy * dy > r2) {
                    continue;
                }
                int color = switch (type) {
                    case MinecraftAdapter.MINIMAP_ENTITY_PLAYER -> 0xFF3B82F6;
                    case MinecraftAdapter.MINIMAP_ENTITY_HOSTILE -> 0xFFEF4444;
                    case MinecraftAdapter.MINIMAP_ENTITY_PASSIVE -> 0xFF22C55E;
                    default -> 0xFFFBBF24;
                };
                ctx.fillRect(sx - 1, sy - 1, 3, 3, color);
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
                // Same basis as VersionAdapter surface sample (forward up)
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
            if (ddx * ddx + ddy * ddy > (radiusPx - 4) * (radiusPx - 4)) {
                return;
            }
            ctx.fillRect(sx - 1, sy - 2, 3, 5, accent);
            ctx.fillRect(sx - 2, sy - 1, 5, 3, accent);
        }

        private void drawNorth(RenderContext ctx, int diameter, int radiusPx, boolean rotate, int accent) {
            float angle;
            if (rotate) {
                // North relative to player yaw (0 = south in MC)
                angle = -adapter.playerYaw() - 180f;
            } else {
                angle = -90f; // top of map
            }
            double rad = Math.toRadians(angle);
            int nx = radiusPx + (int) Math.round(Math.cos(rad) * (radiusPx - 7));
            int ny = radiusPx + (int) Math.round(Math.sin(rad) * (radiusPx - 7));
            ctx.drawText("N", nx - 2, ny - 4, ColorUtil.withAlpha(accent, 0.95f), true);
        }

        private void drawPlayerArrow(RenderContext ctx, int cx, int cy, float ignoredYaw, int accent) {
            // Screen angle: 0° = east, 90° = south (Y down). Facing south (yaw 0) → down.
            double rad;
            if (rotateWithPlayer.get()) {
                rad = Math.toRadians(-90); // always point up when map rotates
            } else if (adapter.hasPlayer()) {
                rad = Math.toRadians(adapter.playerYaw() + 90);
            } else {
                rad = Math.toRadians(-90);
            }
            int len = 5;
            int tipX = cx + (int) Math.round(Math.cos(rad) * len);
            int tipY = cy + (int) Math.round(Math.sin(rad) * len);
            double backRad1 = rad + Math.PI * 0.75;
            double backRad2 = rad - Math.PI * 0.75;
            ctx.fillRect(cx - 1, cy - 1, 3, 3, 0xFFFFFFFF);
            ctx.fillRect(tipX - 1, tipY - 1, 3, 3, accent);
            int bx1 = cx + (int) Math.round(Math.cos(backRad1) * 3);
            int by1 = cy + (int) Math.round(Math.sin(backRad1) * 3);
            int bx2 = cx + (int) Math.round(Math.cos(backRad2) * 3);
            int by2 = cy + (int) Math.round(Math.sin(backRad2) * 3);
            ctx.fillRect(bx1, by1, 2, 2, 0xFFFFFFFF);
            ctx.fillRect(bx2, by2, 2, 2, 0xFFFFFFFF);
            for (int i = 1; i < len; i++) {
                int sx = cx + (int) Math.round(Math.cos(rad) * i);
                int sy = cy + (int) Math.round(Math.sin(rad) * i);
                ctx.fillRect(sx, sy, 2, 2, 0xFFFFFFFF);
            }
        }
    }
}
