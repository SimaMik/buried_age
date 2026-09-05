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
import net.minecraft.client.gui.screens.inventory.PageButton;
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
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The archaeological journal, drawn on the vanilla book. One tab for finds, one per city prefix.
 * Pages turn with the book's own buttons, the arrow keys and the mouse wheel.
 */
public class JournalScreen extends Screen {
    private static final int IMAGE_WIDTH = 192;
    private static final int IMAGE_HEIGHT = 192;
    private static final int TEXT_X = 36;
    private static final int TEXT_WIDTH = 114;
    private static final int PAGE_INDICATOR_RIGHT = 148;
    private static final int TABS_Y = 12;
    private static final int CONTENT_Y = 34;
    private static final int TAB_WIDTH = 18;

    private static final int GRID_COLUMNS = 6;
    private static final int GRID_ROWS = 5;
    private static final int CELL = 18;
    private static final int FINDS_PER_PAGE = GRID_COLUMNS * GRID_ROWS;

    private static final int BUILDINGS_PER_PAGE = 2;
    private static final int BUILDING_HEIGHT = 62;
    private static final int BUILDING_ICON = 32;
    private static final int BUILDING_TEXT_WIDTH = TEXT_WIDTH - BUILDING_ICON - 4;
    private static final int LOOT_ICON_STEP = 16;

    private static final int INK = 0xFF2A2013;
    private static final int FADED_INK = 0xFF6F6353;
    private static final int SILHOUETTE = 0xFF3A3126;
    private static final int LOCKED_ICON_TINT = 0xFF4A4238;

    private final JournalBook book;
    private final List<Tab> tabs = new ArrayList<>();
    private final Map<Identifier, Optional<TextureAtlasSprite>> spriteCache = new HashMap<>();
    private int tab;
    private int page;
    private PageButton forwardButton;
    private PageButton backButton;

    public JournalScreen() {
        super(Component.translatable("journal.buried_age.title"));
        this.book = ClientJournal.book();
        this.tabs.add(new Tab(Component.translatable("journal.buried_age.tab.finds"), new ItemStack(Items.BRUSH), null));
        for (String group : this.book.groups()) {
            this.tabs.add(new Tab(Component.translatable(JournalEntry.Building.groupNameKey(group)),
                    new ItemStack(Items.CHISELED_STONE_BRICKS), group));
        }
    }

    private record Tab(Component title, ItemStack icon, @Nullable String group) {
        boolean isFinds() {
            return this.group == null;
        }
    }

    @Override
    protected void init() {
        int left = this.backgroundLeft();
        int top = this.backgroundTop();
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .pos((this.width - 200) / 2, top + IMAGE_HEIGHT + 2).width(200).build());
        this.forwardButton = this.addRenderableWidget(new PageButton(left + 116, top + 157, true, button -> this.pageForward(), true));
        this.backButton = this.addRenderableWidget(new PageButton(left + 43, top + 157, false, button -> this.pageBack(), true));
        this.updateButtons();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int backgroundLeft() {
        return (this.width - IMAGE_WIDTH) / 2;
    }

    private int backgroundTop() {
        return 2;
    }

    private JournalProgress progress() {
        return this.minecraft.player == null ? JournalProgress.EMPTY : this.minecraft.player.getData(ModAttachments.JOURNAL);
    }

    private Tab currentTab() {
        return this.tabs.get(this.tab);
    }

    private int pageCount() {
        Tab current = this.currentTab();
        int entries = current.isFinds() ? this.book.finds().size() : this.book.buildings(current.group()).size();
        int perPage = current.isFinds() ? FINDS_PER_PAGE : BUILDINGS_PER_PAGE;
        return Math.max(1, (entries + perPage - 1) / perPage);
    }

    public void selectTab(int index) {
        this.tab = index;
        this.page = 0;
        this.updateButtons();
    }

    private void pageForward() {
        if (this.page < this.pageCount() - 1) {
            this.page++;
        }
        this.updateButtons();
    }

    private void pageBack() {
        if (this.page > 0) {
            this.page--;
        }
        this.updateButtons();
    }

    private void updateButtons() {
        this.forwardButton.visible = this.page < this.pageCount() - 1;
        this.backButton.visible = this.page > 0;
    }

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
            int hit = this.tabAt(event.x(), event.y());
            if (hit >= 0) {
                if (hit != this.tab) {
                    this.selectTab(hit);
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private int tabAt(double mouseX, double mouseY) {
        int left = this.backgroundLeft() + TEXT_X;
        int top = this.backgroundTop() + TABS_Y;
        if (mouseY < top || mouseY >= top + TAB_WIDTH) {
            return -1;
        }
        int index = (int) ((mouseX - left) / TAB_WIDTH);
        return mouseX >= left && index >= 0 && index < this.tabs.size() ? index : -1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, BookViewScreen.BOOK_LOCATION,
                this.backgroundLeft(), this.backgroundTop(), 0.0F, 0.0F, IMAGE_WIDTH, IMAGE_HEIGHT, 256, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int left = this.backgroundLeft();
        int top = this.backgroundTop();

        this.drawTabs(graphics, left, top, mouseX, mouseY);

        Component indicator = Component.translatable("book.pageIndicator", this.page + 1, this.pageCount());
        graphics.text(this.font, indicator, left + PAGE_INDICATOR_RIGHT - this.font.width(indicator), top + 16, INK, false);

        JournalProgress progress = this.progress();
        Tab current = this.currentTab();
        if (this.book.isEmpty()) {
            this.drawWrapped(graphics, Component.translatable("journal.buried_age.empty"), left + TEXT_X, top + CONTENT_Y, TEXT_WIDTH, 8, FADED_INK);
        } else if (current.isFinds()) {
            this.drawFinds(graphics, progress, left, top, mouseX, mouseY);
        } else {
            this.drawBuildings(graphics, progress, current.group(), left, top, mouseX, mouseY);
        }
    }

    private void drawTabs(GuiGraphicsExtractor graphics, int left, int top, int mouseX, int mouseY) {
        int x = left + TEXT_X;
        int y = top + TABS_Y;
        for (int i = 0; i < this.tabs.size(); i++) {
            Tab entry = this.tabs.get(i);
            int tabX = x + i * TAB_WIDTH;
            graphics.item(entry.icon(), tabX + 1, y + 1);
            if (i == this.tab) {
                graphics.fill(tabX + 1, y + TAB_WIDTH, tabX + TAB_WIDTH - 1, y + TAB_WIDTH + 1, INK);
            }
        }

        int hovered = this.tabAt(mouseX, mouseY);
        if (hovered >= 0) {
            graphics.setTooltipForNextFrame(this.font, this.tabs.get(hovered).title(), mouseX, mouseY);
        }
    }

    private void drawFinds(GuiGraphicsExtractor graphics, JournalProgress progress, int left, int top, int mouseX, int mouseY) {
        List<JournalEntry.Find> finds = this.book.finds();
        int found = 0;
        for (JournalEntry.Find find : finds) {
            if (progress.hasFind(find.key())) {
                found++;
            }
        }

        int x0 = left + TEXT_X;
        int y0 = top + CONTENT_Y;
        graphics.text(this.font, Component.translatable("journal.buried_age.finds.counter", found, finds.size()), x0, y0, INK, false);

        int gridY = y0 + 12;
        int first = this.page * FINDS_PER_PAGE;
        JournalEntry.Find hoveredFind = null;
        boolean hoveredKnown = false;
        for (int i = first; i < Math.min(finds.size(), first + FINDS_PER_PAGE); i++) {
            int slot = i - first;
            int cellX = x0 + (slot % GRID_COLUMNS) * CELL;
            int cellY = gridY + (slot / GRID_COLUMNS) * CELL;
            JournalEntry.Find find = finds.get(i);
            ItemStack icon = find.icon(this.minecraft.level.registryAccess());
            boolean known = progress.hasFind(find.key());
            if (known) {
                graphics.item(icon, cellX + 1, cellY + 1);
            } else {
                this.drawSilhouette(graphics, find, icon, cellX + 1, cellY + 1);
            }

            if (mouseX >= cellX && mouseX < cellX + CELL && mouseY >= cellY && mouseY < cellY + CELL) {
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
                               int left, int top, int mouseX, int mouseY) {
        List<JournalEntry.Building> buildings = this.book.buildings(group);
        int x0 = left + TEXT_X;
        int first = this.page * BUILDINGS_PER_PAGE;
        for (int i = first; i < Math.min(buildings.size(), first + BUILDINGS_PER_PAGE); i++) {
            int y = top + CONTENT_Y + (i - first) * BUILDING_HEIGHT;
            JournalEntry.Building building = buildings.get(i);
            boolean known = progress.hasBuilding(building.id());

            Component name = known
                    ? Component.translatable(building.nameKey())
                    : Component.translatable("journal.buried_age.unknown");
            graphics.text(this.font, name, x0, y, known ? INK : FADED_INK, false);

            int iconY = y + 11;
            graphics.blit(RenderPipelines.GUI_TEXTURED, building.icon(), x0, iconY, 0.0F, 0.0F,
                    BUILDING_ICON, BUILDING_ICON, BUILDING_ICON, BUILDING_ICON, known ? -1 : LOCKED_ICON_TINT);

            Component description = known
                    ? Component.translatable(building.descriptionKey())
                    : Component.translatable("journal.buried_age.unknown.building");
            this.drawWrapped(graphics, description, x0 + BUILDING_ICON + 4, iconY, BUILDING_TEXT_WIDTH, 4, known ? INK : FADED_INK);

            if (known && !building.loot().isEmpty()) {
                int lootY = iconY + BUILDING_ICON + 2;
                Component label = Component.translatable("journal.buried_age.loot");
                graphics.text(this.font, label, x0, lootY + 4, FADED_INK, false);
                int lootX = x0 + this.font.width(label) + 3;
                for (Identifier id : building.loot()) {
                    if (lootX + LOOT_ICON_STEP > x0 + TEXT_WIDTH || !BuiltInRegistries.ITEM.containsKey(id)) {
                        break;
                    }
                    ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
                    graphics.item(stack, lootX, lootY);
                    if (mouseX >= lootX && mouseX < lootX + LOOT_ICON_STEP && mouseY >= lootY && mouseY < lootY + LOOT_ICON_STEP) {
                        graphics.setTooltipForNextFrame(this.font, stack.getHoverName(), mouseX, mouseY);
                    }
                    lootX += LOOT_ICON_STEP;
                }
            }
        }
    }

    private void drawWrapped(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int maxLines, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, width);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            graphics.text(this.font, lines.get(i), x, y + i * 9, color, false);
        }
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
