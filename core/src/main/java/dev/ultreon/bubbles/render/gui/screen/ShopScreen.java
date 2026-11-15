package dev.ultreon.bubbles.render.gui.screen;

import com.badlogic.gdx.graphics.Texture;
import dev.ultreon.bubbles.BubbleBlaster;
import dev.ultreon.bubbles.init.Fonts;
import dev.ultreon.bubbles.item.ItemType;
import dev.ultreon.bubbles.item.ItemTypes;
import dev.ultreon.bubbles.render.Colors;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.render.gui.widget.Button;
import dev.ultreon.bubbles.render.gui.widget.TabWidget;
import dev.ultreon.bubbles.shop.Shop;
import dev.ultreon.bubbles.shop.ShopItem;
import dev.ultreon.bubbles.text.TextObject;
import dev.ultreon.libs.commons.v0.Identifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShopScreen extends Screen {

    public enum StoreCategory {
        ITEMS("bubbleblaster.screen.store.tab.items"),
        UPGRADES("bubbleblaster.screen.store.tab.upgrades"),
        BOOSTS("bubbleblaster.screen.store.tab.boosts"),
        COSMETICS("bubbleblaster.screen.store.tab.cosmetics");

        private final String translationKey;

        StoreCategory(String translationKey) {
            this.translationKey = translationKey;
        }

        public TextObject displayName() {
            return TextObject.translation(this.translationKey);
        }

        public List<ShopItem> getItems() {
            return switch (this) {
                case ITEMS -> new ArrayList<>(Shop.getItems());
                case UPGRADES -> new ArrayList<>(Shop.getUpgrades());
                case BOOSTS -> new ArrayList<>(Shop.getBoosts());
                case COSMETICS -> new ArrayList<>(Shop.getCosmetics());
            };
        }
    }

    private TabWidget tabWidget;
    private final TabWidget.TabContent[] tabContents = new TabWidget.TabContent[StoreCategory.values().length];
    private StoreCategory selectedCategory = StoreCategory.UPGRADES;
    private int currentPage = 0;

    private static final int TAB_HEIGHT = 50;
    private static final int TAB_WIDTH = 150;
    private static final int CONTENT_MARGIN = 20;
    private static final int ITEMS_PER_PAGE = 3;
    private static final int ITEM_HEIGHT = 60;
    private static final int ITEM_SPACING = 10;
    private static final int ITEM_WIDTH = 400;
    private static final int ITEM_ICON_SIZE = 40;
    private static final int ITEM_ICON_MARGIN = 10;
    private static final int PAGINATION_HEIGHT = 40;

    public ShopScreen() {
        super(TextObject.translation("bubbleblaster.screen.store.title"));
    }

    @Override
    public void init() {
        this.currentPage = 0;
        this.createTabWidget();
        this.populateTabContents();
    }

    @Override
    public void renderBackground(Renderer renderer) {
        renderer.fill(0, 0, this.width, 50, Colors.grayscale(0x1e));
        renderer.fillEffect(0, 50, this.width, 3);
        renderer.fillGradient(0, 53, this.width, 20, Colors.argb(0x20000000), Colors.TRANSPARENT);
    }

    private void createTabWidget() {
        var contentStartY = TAB_HEIGHT + CONTENT_MARGIN;
        var contentHeight = this.height - contentStartY;

        for (var i = 0; i < StoreCategory.values().length; i++) {
            this.tabContents[i] = TabWidget.TabContent.builder()
                    .bounds(this.middleX - ITEM_WIDTH / 2, contentStartY, ITEM_WIDTH, contentHeight)
                    .build();
        }

        this.tabWidget = TabWidget.builder()
                .bounds(0, 0, this.width, this.height)
                .tab(StoreCategory.ITEMS.displayName())
                .content(this.tabContents[0])
                .tab(StoreCategory.UPGRADES.displayName())
                .content(this.tabContents[1])
                .tab(StoreCategory.BOOSTS.displayName())
                .content(this.tabContents[2])
                .tab(StoreCategory.COSMETICS.displayName())
                .content(this.tabContents[3])
                .selectedIndex(this.selectedCategory.ordinal())
                .tabWidth(TAB_WIDTH)
                .tabHeight(TAB_HEIGHT)
                .gap(10)
                .onTabChanged(this::onTabSelected)
                .build();

        this.add(this.tabWidget);
    }

    private void populateTabContents() {
        for (var i = 0; i < StoreCategory.values().length; i++) {
            var category = StoreCategory.values()[i];
            var content = this.tabContents[i];
            var items = category.getItems();
            this.populateContent(content, items, category);
        }
    }

    private void onTabSelected(int tabIndex) {
        this.selectedCategory = StoreCategory.values()[tabIndex];
        this.currentPage = 0;
        this.refreshActiveContent();
    }

    private void refreshActiveContent() {
        var content = this.tabContents[this.selectedCategory.ordinal()];
        var items = this.selectedCategory.getItems();
        this.populateContent(content, items, this.selectedCategory);
    }

    private void populateContent(TabWidget.TabContent content, List<ShopItem> items, StoreCategory category) {
        content.clear();
        int categoryPage = 0;
        var totalPages = (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE);

        if (category == this.selectedCategory) {
            categoryPage = this.currentPage;
            if (categoryPage >= totalPages) {
                categoryPage = Math.max(0, totalPages - 1);
            }
        }

        var firstItemIndex = categoryPage * ITEMS_PER_PAGE;
        var visibleItemCount = Math.min(ITEMS_PER_PAGE, Math.max(0, items.size() - firstItemIndex));
        var itemBlockHeight = visibleItemCount == 0 ? 0 : visibleItemCount * ITEM_HEIGHT + (visibleItemCount - 1) * ITEM_SPACING;
        var paginationBlockHeight = totalPages > 1 ? ITEM_SPACING + PAGINATION_HEIGHT : 0;
        var blockHeight = itemBlockHeight + paginationBlockHeight;
        var relativeY = Math.max(0, (content.getHeight() - blockHeight) / 2);

        this.renderItemButtons(content, items, categoryPage, relativeY, category);
        this.renderPaginationButtons(content, items, totalPages, categoryPage, relativeY + itemBlockHeight + ITEM_SPACING, category);
    }

    private void renderItemButtons(TabWidget.TabContent content, List<ShopItem> items, int page, int relativeStartY, StoreCategory category) {
        var fromIndex = page * ITEMS_PER_PAGE;
        var toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, items.size());

        for (var i = fromIndex; i < toIndex; i++) {
            var item = items.get(i);
            var itemIndex = i;
            var y = content.getY() + relativeStartY + (i - fromIndex) * (ITEM_HEIGHT + ITEM_SPACING);

            var text = this.getItemText(item);
            var button = new ShopItemButton(
                    content.getX(), y, ITEM_WIDTH, ITEM_HEIGHT, text,
                    () -> this.onPurchaseItem(itemIndex, category),
                    this.getItemIcon(item)
            );

            button.enabled = this.canPurchase(item);

            content.add(button);
        }
    }

    private TextObject getItemText(ShopItem item) {
        var name = item.getName().getText();
        var price = item.getPrice();
        return TextObject.translation("bubbleblaster.screen.store.item", name, price);
    }

    private Identifier getItemIcon(ShopItem item) {
        return item.getIcon();
    }

    private boolean canPurchase(ShopItem item) {
        return item.canBuy() && this.game.getGlobalData().getMoney() >= item.getPrice();
    }

    private void onPurchaseItem(int itemIndex, StoreCategory category) {
        var items = category.getItems();
        if (itemIndex < 0 || itemIndex >= items.size()) return;

        var item = items.get(itemIndex);
        if (!this.canPurchase(item)) return;

        var money = this.game.getGlobalData().getMoney();
        this.game.getGlobalData().setMoney(money - item.getPrice());
        try {
            this.game.getGlobalData().dump();
        } catch (IOException e) {
            BubbleBlaster.crash(e);
        }

        item.onBought();
        this.refreshActiveContent();
    }

    private void renderPaginationButtons(TabWidget.TabContent content, List<ShopItem> items, int totalPages, int page, int relativeStartY, StoreCategory category) {
        if (totalPages <= 1) return;

        var absStartY = content.getY() + relativeStartY;
        var prevButton = Button.builder()
                .text(TextObject.translation("bubbleblaster.misc.prev"))
                .bounds(content.getX() + (ITEM_WIDTH - 200) / 2, absStartY, 100, PAGINATION_HEIGHT)
                .command(() -> {
                    if (page > 0) {
                        if (category == this.selectedCategory) {
                            this.currentPage--;
                        }
                        this.refreshActiveContent();
                    }
                })
                .build();
        prevButton.enabled = page > 0;
        content.add(prevButton);

        var nextButton = Button.builder()
                .text(TextObject.translation("bubbleblaster.misc.next"))
                .bounds(content.getX() + (ITEM_WIDTH - 200) / 2 + 100, absStartY, 100, PAGINATION_HEIGHT)
                .command(() -> {
                    if (page < totalPages - 1) {
                        if (category == this.selectedCategory) {
                            this.currentPage++;
                        }
                        this.refreshActiveContent();
                    }
                })
                .build();
        nextButton.enabled = page < totalPages - 1;
        content.add(nextButton);
    }

    @Override
    public void render(Renderer renderer, int mouseX, int mouseY, float deltaTime) {
        this.renderBackground(renderer);
        this.renderMoney(renderer);
        this.renderPaginationInfo(renderer);
        this.renderChildren(renderer, mouseX, mouseY, deltaTime);
    }

    private void renderMoney(Renderer renderer) {
        var money = this.game.getGlobalData().getMoney();
        renderer.drawTextRight(Fonts.SANS_PARAGRAPH.get(), TextObject.translation("bubbleblaster.screen.title.money", money), this.width - 20, 35, Colors.WHITE);
    }

    private void renderPaginationInfo(Renderer renderer) {
        var items = this.selectedCategory.getItems();
        var totalPages = (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE);
        if (totalPages <= 1) return;

        var pageText = TextObject.translation("bubbleblaster.screen.store.page", this.currentPage + 1, totalPages);
        renderer.drawTextCenter(this.font, pageText.getText(), this.middleX, CONTENT_MARGIN + TAB_HEIGHT - 15, Colors.WHITE);
    }

    private static class ShopItemButton extends Button {
        private final Identifier iconId;
        private Texture icon;

        private ShopItemButton(int x, int y, int width, int height, TextObject text, Runnable command, Identifier iconId) {
            super(x, y, width, height);
            this.setText(text);
            this.setCommand(command);
            this.setFont(Fonts.DEFAULT.get());
            this.iconId = iconId;
        }

        @Override
        public void render(Renderer renderer, int mouseX, int mouseY, float deltaTime) {
            super.render(renderer, mouseX, mouseY, deltaTime);
            if (this.iconId == null) return;

            if (this.icon == null) {
                this.icon = this.game.getTextureManager().getOrLoadTexture(this.iconId);
            }
            renderer.blit(this.icon, this.x + ITEM_ICON_MARGIN, this.y + (this.height - ITEM_ICON_SIZE) / 2f, ITEM_ICON_SIZE, ITEM_ICON_SIZE);
        }
    }
}
