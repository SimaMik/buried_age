package com.sima.buriedage.client.journal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.sima.buriedage.journal.ClientJournal;
import com.sima.buriedage.journal.JournalBook;
import com.sima.buriedage.journal.JournalEntry;
import com.sima.buriedage.journal.JournalProgress;
import com.sima.buriedage.registry.ModAttachments;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The archaeological journal as an open book: two vanilla book pages side by side, spines meeting
 * in the middle, drawn under one scale so the spread fills the window at any GUI scale. One tab for
 * finds, one per city prefix. Spreads turn with the page arrows, the arrow keys and the mouse wheel.
 */
public class JournalScreen extends Screen {
    private static final int PAGE = 192;
    /** The page body occupies x 20..165 of the texture; the right page starts here so the two stitched edges meet. */
    private static final int RIGHT_PAGE_X = 152;
    private static final int SPREAD_WIDTH = RIGHT_PAGE_X + PAGE;
    private static final float PAGE_UV = PAGE / 256.0F;
    private static final int TEXT_WIDTH = 114;
    private static final int LEFT_TEXT_X = 42;
    private static final int RIGHT_TEXT_X = RIGHT_PAGE_X + 36;
    private static final int TABS_Y = 12;
    private static final int HEADER_Y = 16;
    private static final int CONTENT_Y = 34;
    private static final int TAB_WIDTH = 18;
    private static final int ARROW_WIDTH = 23;
    private static final int ARROW_HEIGHT = 13;
    private static final int ARROW_Y = 157;
    private static final int BACK_ARROW_X = LEFT_TEXT_X + 4;
    private static final int FORWARD_ARROW_X = RIGHT_TEXT_X + TEXT_WIDTH - ARROW_WIDTH - 4;
    private static final int DONE_GAP = 6;
    private static final int MARGIN = 8;

    private static final int GRID_COLUMNS = 4;
    private static final int GRID_ROWS = 4;
    private static final int CELL = 28;
    private static final float FIND_ICON_SCALE = 1.5F;
    private static final int FIND_ICON = Math.round(16 * FIND_ICON_SCALE);
    private static final int FIND_ICON_INSET = (CELL - FIND_ICON) / 2;
    private static final int GRID_INSET = (TEXT_WIDTH - GRID_COLUMNS * CELL) / 2;
    private static final int GRID_Y = CONTENT_Y + 2;
    private static final int FINDS_PER_PAGE = GRID_COLUMNS * GRID_ROWS;
    private static final int FINDS_PER_SPREAD = FINDS_PER_PAGE * 2;

    private static final int BUILDINGS_PER_SPREAD = 2;
    private static final int BUILDING_ICON = 32;
    private static final int LOOT_COLUMNS = 4;
    private static final int LOOT_ROWS = 3;
    private static final int LOOT_STEP = 18;
    private static final int LOOT_X = BUILDING_ICON + 6;
    private static final float SMALL_TEXT = 0.75F;
    private static final float SMALL_LINE = 9 * SMALL_TEXT;
    private static final int PAGE_BOTTOM = ARROW_Y - 3;

    private static final int INK = 0xFF2A2013;
    private static final int FADED_INK = 0xFF6F6353;
    private static final int SILHOUETTE = 0xFF3A3126;
    private static final int LOCKED_ICON_TINT = 0xFF4A4238;

    private static final Identifier FORWARD = Identifier.withDefaultNamespace("widget/page_forward");
    private static final Identifier FORWARD_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/page_forward_highlighted");
    private static final Identifier BACKWARD = Identifier.withDefaultNamespace("widget/page_backward");
    private static final Identifier BACKWARD_HIGHLIGHTED = Identifier.withDefaultNamespace("widget/page_backward_highlighted");

    private final JournalBook book;
    private final List<Tab> tabs = new ArrayList<>();
    private final Map<Identifier, Optional<TextureAtlasSprite>> spriteCache = new HashMap<>();
    private int tab;
    private int spread;
    private float scale = 1.0F;
    private int originX;
    private int originY;

    public JournalScreen() {
        super(Component.translatable("journal.buried_age.title"));
        this.book = ClientJournal.book();
        this.tabs.add(new Tab(Component.translatable("journal.buried_age.tab.finds"), new ItemStack(Items.BRUSH), null));
        for (String group : this.book.groups()) {
            this.tabs.add(new Tab(Component.translatable(JournalEntry.Building.groupNameKey(group)),
                    new ItemStack(Items.QUARTZ_PILLAR), group));
        }
    }

    private record Tab(Component title, ItemStack icon, @Nullable String group) {
        boolean isFinds() {
            return this.group == null;
        }
    }

    // ---------------------------------------------------------------- layout

    /** The spread is drawn in book units (384 x 192) under one scale that fits the window. */
    private void layout() {
        float byHeight = (this.height - 20 - 2 * MARGIN - DONE_GAP) / (float) PAGE;
        float byWidth = (this.width - 2 * MARGIN) / (float) SPREAD_WIDTH;
        this.scale = Mth.clamp(Math.min(byHeight, byWidth), 0.75F, 4.0F);
        this.originX = Math.round((this.width - SPREAD_WIDTH * this.scale) / 2.0F);
        this.originY = Math.max(MARGIN, Math.round((this.height - 20 - DONE_GAP - PAGE * this.scale) / 2.0F));
    }

    @Override
    protected void init() {
        this.layout();
        int doneY = Math.min(this.height - 22, Math.round(this.originY + PAGE * this.scale) + DONE_GAP);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .pos((this.width - 200) / 2, doneY).width(200).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private float bookX(double mouseX) {
        return (float) ((mouseX - this.originX) / this.scale);
    }

    private float bookY(double mouseY) {
        return (float) ((mouseY - this.originY) / this.scale);
    }

    private static boolean inside(float x, float y, int x0, int y0, int w, int h) {
        return x >= x0 && x < x0 + w && y >= y0 && y < y0 + h;
    }

    // ---------------------------------------------------------------- state

    private JournalProgress progress() {
        return this.minecraft.player == null ? JournalProgress.EMPTY : this.minecraft.player.getData(ModAttachments.JOURNAL);
    }

    private Tab currentTab() {
        return this.tabs.get(this.tab);
    }

    private int spreadCount() {
        Tab current = this.currentTab();
        int entries = current.isFinds() ? this.book.finds().size() : this.book.buildings(current.group()).size();
        int perSpread = current.isFinds() ? FINDS_PER_SPREAD : BUILDINGS_PER_SPREAD;
        return Math.max(1, (entries + perSpread - 1) / perSpread);
    }

    public void selectTab(int index) {
        this.tab = index;
        this.spread = 0;
    }

    private boolean canGoForward() {
        return this.spread < this.spreadCount() - 1;
    }

    private boolean canGoBack() {
        return this.spread > 0;
    }

    private void pageForward() {
        if (this.canGoForward()) {
            this.spread++;
            this.playTurn();
        }
    }

    private void pageBack() {
        if (this.canGoBack()) {
            this.spread--;
            this.playTurn();
        }
    }

    private void playTurn() {
        this.minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        }
        return switch (event.key()) {
            case 266, 263 -> {
                this.pageBack();
                yield true;
            }
            case 267, 262 -> {
                this.pageForward();
                yield true;
            }
            default -> false;
        };
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY < 0) {
            this.pageForward();
            return true;
        }
        if (scrollY > 0) {
            this.pageBack();
            return true;
        }
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            float x = this.bookX(event.x());
            float y = this.bookY(event.y());
            int hit = this.tabAt(x, y);
            if (hit >= 0) {
                if (hit != this.tab) {
                    this.selectTab(hit);
                }
                return true;
            }
            if (this.canGoBack() && inside(x, y, BACK_ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT)) {
                this.pageBack();
                return true;
            }
            if (this.canGoForward() && inside(x, y, FORWARD_ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT)) {
                this.pageForward();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private int tabAt(float x, float y) {
        if (y < TABS_Y || y >= TABS_Y + TAB_WIDTH || x < LEFT_TEXT_X) {
            return -1;
        }
        int index = (int) ((x - LEFT_TEXT_X) / TAB_WIDTH);
        return index < this.tabs.size() ? index : -1;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        this.layout();
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.originX, this.originY);
        graphics.pose().scale(this.scale, this.scale);
        graphics.blit(BookViewScreen.BOOK_LOCATION, 0, 0, PAGE, PAGE, PAGE_UV, 0.0F, 0.0F, PAGE_UV);
        graphics.blit(BookViewScreen.BOOK_LOCATION, RIGHT_PAGE_X, 0, SPREAD_WIDTH, PAGE, 0.0F, PAGE_UV, 0.0F, PAGE_UV);
        graphics.pose().popMatrix();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        float x = this.bookX(mouseX);
        float y = this.bookY(mouseY);

        graphics.pose().pushMatrix();
        graphics.pose().translate(this.originX, this.originY);
        graphics.pose().scale(this.scale, this.scale);

        this.drawTabs(graphics, x, y, mouseX, mouseY);
        this.drawArrows(graphics, x, y);

        Component indicator = Component.translatable("book.pageIndicator", this.spread + 1, this.spreadCount());
        graphics.text(this.font, indicator, RIGHT_TEXT_X + TEXT_WIDTH - this.font.width(indicator), HEADER_Y, INK, false);

        JournalProgress progress = this.progress();
        Tab current = this.currentTab();
        if (this.book.isEmpty()) {
            this.drawWrapped(graphics, Component.translatable("journal.buried_age.empty"), LEFT_TEXT_X, CONTENT_Y, TEXT_WIDTH, 8, FADED_INK);
        } else if (current.isFinds()) {
            this.drawFinds(graphics, progress, x, y, mouseX, mouseY);
        } else {
            this.drawBuildings(graphics, progress, current.group(), x, y, mouseX, mouseY);
        }

        graphics.pose().popMatrix();
    }

    private void drawTabs(GuiGraphicsExtractor graphics, float x, float y, int mouseX, int mouseY) {
        for (int i = 0; i < this.tabs.size(); i++) {
            int tabX = LEFT_TEXT_X + i * TAB_WIDTH;
            graphics.item(this.tabs.get(i).icon(), tabX + 1, TABS_Y + 1);
            if (i == this.tab) {
                graphics.fill(tabX + 1, TABS_Y + TAB_WIDTH, tabX + TAB_WIDTH - 1, TABS_Y + TAB_WIDTH + 1, INK);
            }
        }
        int hovered = this.tabAt(x, y);
        if (hovered >= 0) {
            graphics.setTooltipForNextFrame(this.font, this.tabs.get(hovered).title(), mouseX, mouseY);
        }
    }

    private void drawArrows(GuiGraphicsExtractor graphics, float x, float y) {
        if (this.canGoBack()) {
            boolean hot = inside(x, y, BACK_ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hot ? BACKWARD_HIGHLIGHTED : BACKWARD, BACK_ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT);
        }
        if (this.canGoForward()) {
            boolean hot = inside(x, y, FORWARD_ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, hot ? FORWARD_HIGHLIGHTED : FORWARD, FORWARD_ARROW_X, ARROW_Y, ARROW_WIDTH, ARROW_HEIGHT);
        }
    }

    private void drawFinds(GuiGraphicsExtractor graphics, JournalProgress progress, float x, float y, int mouseX, int mouseY) {
        List<JournalEntry.Find> finds = this.book.finds();
        int found = 0;
        for (JournalEntry.Find find : finds) {
            if (progress.hasFind(find.key())) {
                found++;
            }
        }
        Component counter = Component.translatable("journal.buried_age.finds.counter", found, finds.size());
        int counterX = Math.round(LEFT_TEXT_X + TEXT_WIDTH - this.font.width(counter) * SMALL_TEXT);
        this.drawWrapped(graphics, counter, counterX, HEADER_Y + 1, TEXT_WIDTH, 1, INK, SMALL_TEXT);

        JournalEntry.Find hoveredFind = null;
        boolean hoveredKnown = false;
        int first = this.spread * FINDS_PER_SPREAD;
        for (int i = first; i < Math.min(finds.size(), first + FINDS_PER_SPREAD); i++) {
            int slot = i - first;
            int pageX = slot < FINDS_PER_PAGE ? LEFT_TEXT_X : RIGHT_TEXT_X;
            int inPage = slot % FINDS_PER_PAGE;
            int cellX = pageX + GRID_INSET + (inPage % GRID_COLUMNS) * CELL;
            int cellY = GRID_Y + (inPage / GRID_COLUMNS) * CELL;
            JournalEntry.Find find = finds.get(i);
            ItemStack icon = find.icon(this.minecraft.level.registryAccess());
            boolean known = progress.hasFind(find.key());
            graphics.pose().pushMatrix();
            graphics.pose().translate(cellX + FIND_ICON_INSET, cellY + FIND_ICON_INSET);
            graphics.pose().scale(FIND_ICON_SCALE, FIND_ICON_SCALE);
            if (known) {
                graphics.item(icon, 0, 0);
            } else {
                this.drawSilhouette(graphics, find, icon, 0, 0);
            }
            graphics.pose().popMatrix();
            if (inside(x, y, cellX, cellY, CELL, CELL)) {
                hoveredFind = find;
                hoveredKnown = known;
            }
        }

        if (hoveredFind != null) {
            List<Component> lines = new ArrayList<>();
            if (hoveredKnown) {
                lines.add(hoveredFind.icon(this.minecraft.level.registryAccess()).getHoverName());
                lines.add(Component.translatable(this.findDescriptionKey(hoveredFind)).withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.translatable("journal.buried_age.unknown"));
                lines.add(Component.translatable("journal.buried_age.unknown.find").withStyle(ChatFormatting.GRAY));
            }
            graphics.setTooltipForNextFrame(this.font, lines, Optional.empty(), mouseX, mouseY);
        }
    }

    private String findDescriptionKey(JournalEntry.Find find) {
        String specific = find.descriptionKey();
        return I18n.exists(specific) ? specific : find.fallbackDescriptionKey();
    }

    private void drawBuildings(GuiGraphicsExtractor graphics, JournalProgress progress, String group,
                               float x, float y, int mouseX, int mouseY) {
        List<JournalEntry.Building> buildings = this.book.buildings(group);
        int first = this.spread * BUILDINGS_PER_SPREAD;
        for (int i = first; i < Math.min(buildings.size(), first + BUILDINGS_PER_SPREAD); i++) {
            int pageX = i == first ? LEFT_TEXT_X : RIGHT_TEXT_X;
            this.drawBuilding(graphics, progress, buildings.get(i), pageX, x, y, mouseX, mouseY);
        }
    }

    /** One building fills one page: name, icon with the loot grid beside it, then the description in small print. */
    private void drawBuilding(GuiGraphicsExtractor graphics, JournalProgress progress, JournalEntry.Building building,
                              int pageX, float x, float y, int mouseX, int mouseY) {
        boolean known = progress.hasBuilding(building.id());
        Component name = known ? Component.translatable(building.nameKey()) : Component.translatable("journal.buried_age.unknown");
        graphics.text(this.font, name, pageX, CONTENT_Y, known ? INK : FADED_INK, false);
        graphics.fill(pageX, CONTENT_Y + 10, pageX + TEXT_WIDTH, CONTENT_Y + 11, known ? INK : FADED_INK);

        int iconY = CONTENT_Y + 15;
        graphics.blit(RenderPipelines.GUI_TEXTURED, building.icon(), pageX, iconY, 0.0F, 0.0F,
                BUILDING_ICON, BUILDING_ICON, BUILDING_ICON, BUILDING_ICON, known ? -1 : LOCKED_ICON_TINT);

        int textY = iconY + BUILDING_ICON + 6;
        if (known && !building.loot().isEmpty()) {
            int lootX = pageX + LOOT_X;
            this.drawWrapped(graphics, Component.translatable("journal.buried_age.loot"), lootX, iconY, TEXT_WIDTH - LOOT_X, 1, FADED_INK, SMALL_TEXT);
            int lootY = iconY + 10;
            int shown = 0;
            for (Identifier id : building.loot()) {
                if (shown >= LOOT_COLUMNS * LOOT_ROWS) {
                    break;
                }
                if (!BuiltInRegistries.ITEM.containsKey(id)) {
                    continue;
                }
                int slotX = lootX + (shown % LOOT_COLUMNS) * LOOT_STEP;
                int slotY = lootY + (shown / LOOT_COLUMNS) * LOOT_STEP;
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
                graphics.item(stack, slotX, slotY);
                if (inside(x, y, slotX, slotY, 16, 16)) {
                    graphics.setTooltipForNextFrame(this.font, stack.getHoverName(), mouseX, mouseY);
                }
                shown++;
            }
            int rows = (shown + LOOT_COLUMNS - 1) / LOOT_COLUMNS;
            textY = Math.max(textY, lootY + rows * LOOT_STEP + 2);
        }

        Component description = known
                ? Component.translatable(building.descriptionKey())
                : Component.translatable("journal.buried_age.unknown.building");
        int maxLines = (int) ((PAGE_BOTTOM - textY) / SMALL_LINE);
        this.drawWrapped(graphics, description, pageX, textY, TEXT_WIDTH, maxLines, known ? INK : FADED_INK, SMALL_TEXT);
    }

    private void drawWrapped(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int maxLines, int color) {
        this.drawWrapped(graphics, text, x, y, width, maxLines, color, 1.0F);
    }

    /** Wraps {@code text} into {@code width} book units and draws it under {@code scale}, so the font shrinks with it. */
    private void drawWrapped(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int maxLines, int color, float scale) {
        List<FormattedCharSequence> lines = this.font.split(text, (int) (width / scale));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            graphics.text(this.font, lines.get(i), 0, i * 9, color, false);
        }
        graphics.pose().popMatrix();
    }

    /**
     * The unknown-item silhouette: the item's own sprite, multiplied down to a dark shade. Nothing
     * is drawn from a placeholder texture; a 3D item without a flat sprite falls back to a dark square.
     */
    private void drawSilhouette(GuiGraphicsExtractor graphics, JournalEntry.Find find, ItemStack stack, int x, int y) {
        Optional<TextureAtlasSprite> sprite = this.spriteCache.computeIfAbsent(find.key(), key -> Optional.ofNullable(this.resolveSprite(stack)));
        if (sprite.isPresent()) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite.get(), x, y, 16, 16, SILHOUETTE);
        } else {
            graphics.fill(x + 2, y + 2, x + 14, y + 14, SILHOUETTE);
        }
    }

    private @Nullable TextureAtlasSprite resolveSprite(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        TrackingItemStackRenderState state = new TrackingItemStackRenderState();
        this.minecraft.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI, this.minecraft.level, null, 0);
        Material.Baked material = state.pickParticleMaterial(RandomSource.create(0L));
        return material == null ? null : material.sprite();
    }
}
