package com.example.strong_body;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 与网页 gym-eye-welcome.html 相同的马赛克网格算法：浅灰圆角块 + 白缝，水平不旋转。
 */
public class MosaicBackgroundView extends View {

    private static final int TILE_COLOR = 0xFFF3F4F6;
    private static final int BG_COLOR = Color.WHITE;
    private static final int SEED = 0xcafebabe;
    private static final int MAX = 5;

    private final Paint tilePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<RectF> tileRects = new ArrayList<>();
    private float cellPx;
    private float gapPx;
    private float radiusPx;

    public MosaicBackgroundView(Context context) {
        super(context);
        init();
    }

    public MosaicBackgroundView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MosaicBackgroundView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        tilePaint.setStyle(Paint.Style.FILL);
        tilePaint.setColor(TILE_COLOR);
        setClickable(false);
        setFocusable(false);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w <= 0 || h <= 0) return;
        cellPx = dp(11);
        gapPx = dp(2);
        radiusPx = dp(4);
        buildTiles(w, h);
    }

    private float dp(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private void buildTiles(int widthPx, int heightPx) {
        tileRects.clear();
        float unit = cellPx + gapPx;
        int cols = Math.max(10, (int) Math.ceil((widthPx + gapPx) / unit));
        int rows = Math.max(12, (int) Math.ceil((heightPx + gapPx) / unit));

        boolean[][] occ = new boolean[rows][cols];
        Mulberry32 rand = new Mulberry32(SEED);
        List<int[]> tiles = new ArrayList<>();

        for (int guard = 0; guard < 900; guard++) {
            if (tryPlace(rand, occ, rows, cols, tiles)) continue;
            boolean progressed = false;
            outer:
            for (int sr = 0; sr < rows && !progressed; sr++) {
                for (int sc = 0; sc < cols && !progressed; sc++) {
                    if (occ[sr][sc]) continue;
                    for (int[] fb : FALLBACK_ORDER) {
                        int tw = fb[0];
                        int th = fb[1];
                        if (free(occ, rows, cols, sr, sc, tw, th)) {
                            mark(occ, sr, sc, tw, th);
                            tiles.add(new int[]{sr, sc, tw, th});
                            progressed = true;
                            break outer;
                        }
                    }
                }
            }
            if (!progressed) break;
        }

        for (int rr = 0; rr < rows; rr++) {
            for (int cc = 0; cc < cols; cc++) {
                if (occ[rr][cc]) continue;
                boolean placed = false;
                int maxW = Math.min(MAX, cols - cc);
                for (int lw = maxW; lw >= 2; lw--) {
                    if (free(occ, rows, cols, rr, cc, lw, 1)) {
                        mark(occ, rr, cc, lw, 1);
                        tiles.add(new int[]{rr, cc, lw, 1});
                        placed = true;
                        break;
                    }
                }
                if (placed) continue;
                int maxH = Math.min(MAX, rows - rr);
                for (int lh = maxH; lh >= 2; lh--) {
                    if (free(occ, rows, cols, rr, cc, 1, lh)) {
                        mark(occ, rr, cc, 1, lh);
                        tiles.add(new int[]{rr, cc, 1, lh});
                        placed = true;
                        break;
                    }
                }
                if (placed) continue;
                mark(occ, rr, cc, 1, 1);
                tiles.add(new int[]{rr, cc, 1, 1});
            }
        }

        for (int[] t : tiles) {
            tileRects.add(gridToRect(t[1], t[0], t[2], t[3]));
        }
    }

    private RectF gridToRect(int col, int row, int cw, int ch) {
        float left = col * (cellPx + gapPx);
        float top = row * (cellPx + gapPx);
        float w = cw * cellPx + (cw - 1) * gapPx;
        float h = ch * cellPx + (ch - 1) * gapPx;
        return new RectF(left, top, left + w, top + h);
    }

    private static boolean free(boolean[][] occ, int rows, int cols, int r, int c, int w, int h) {
        if (r + h > rows || c + w > cols) return false;
        for (int y = r; y < r + h; y++) {
            for (int x = c; x < c + w; x++) {
                if (occ[y][x]) return false;
            }
        }
        return true;
    }

    private static void mark(boolean[][] occ, int r, int c, int w, int h) {
        for (int y = r; y < r + h; y++) {
            for (int x = c; x < c + w; x++) {
                occ[y][x] = true;
            }
        }
    }

    private boolean tryPlace(Mulberry32 rand, boolean[][] occ, int rows, int cols, List<int[]> tiles) {
        int[] sz = pickRectSize(rand);
        int w = Math.min(sz[0], MAX);
        int h = Math.min(sz[1], MAX);
        if (w == h && w > 1 && rand.next() > 0.15f) {
            if (rand.next() > 0.5f) {
                w = Math.max(1, w - 1);
            } else {
                h = Math.max(1, h - 1);
            }
        }
        int fw = w;
        int fh = h;
        for (int best = 0; best < 160; best++) {
            int c = (int) (rand.next() * cols);
            int r = (int) (rand.next() * rows);
            if (free(occ, rows, cols, r, c, fw, fh)) {
                mark(occ, r, c, fw, fh);
                tiles.add(new int[]{r, c, fw, fh});
                return true;
            }
        }
        return false;
    }

    private int[] pickRectSize(Mulberry32 rand) {
        int[][] shapes = {
                {2, 1}, {3, 1}, {4, 1}, {5, 1},
                {1, 2}, {1, 3}, {1, 4}, {1, 5},
                {3, 2}, {2, 3}, {4, 2}, {2, 4}, {3, 4}, {4, 3}, {5, 2}, {2, 5},
                {2, 1}, {1, 2}, {3, 1}, {1, 3}
        };
        if (rand.next() < 0.06f) {
            int s = 2 + ((int) (rand.next() * 2));
            return new int[]{s, s};
        }
        int i = (int) (rand.next() * shapes.length);
        return new int[]{shapes[i][0], shapes[i][1]};
    }

    private static final int[][] FALLBACK_ORDER = {
            {2, 1}, {1, 2}, {3, 1}, {1, 3}, {4, 1}, {1, 4}, {5, 1}, {1, 5},
            {3, 2}, {2, 3}, {4, 2}, {2, 4}, {2, 2}, {1, 1}
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(BG_COLOR);
        for (RectF r : tileRects) {
            canvas.drawRoundRect(r, radiusPx, radiusPx, tilePaint);
        }
    }

    private static final class Mulberry32 {
        private int a;

        Mulberry32(int seed) {
            this.a = seed;
        }

        float next() {
            int t = (a += 0x6d2b79f5);
            t = imul(t ^ (t >>> 15), t | 1);
            t ^= t + imul(t ^ (t >>> 7), t | 61);
            long x = Integer.toUnsignedLong(t ^ (t >>> 14));
            return x / 4294967296.0f;
        }

        /** 与 JS Math.imul 一致 */
        private static int imul(int x, int y) {
            return (int) ((long) x * (long) y);
        }
    }
}
