package dev.ultreon.bubbles.render.gui.screen;

import dev.ultreon.bubbles.shop.Shop;
import dev.ultreon.bubbles.shop.ShopItem;
import dev.ultreon.bubbles.render.Colors;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.render.gui.widget.Button;
import dev.ultreon.bubbles.render.gui.widget.TabWidget;
import dev.ultreon.bubbles.text.TextObject;

import java.util.ArrayList;
import java.util.List;

public class ShopScreen extends Screen {

    public enum StoreCategory {
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
            switch (this) {
                case UPGRADES:
                    return new ArrayList<>(Shop.getUpgrades());
                case BOOSTS:
                    return new ArrayList<>(Shop.getBoosts());
                case COSMETICS:
                    return new ArrayList<>(Shop.getCosmetics());
                default:
                    return new ArrayList<>();
            }
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
            var category = StoreCategory.values()[i];
            this.tabContents[i] = TabWidget.TabContent.builder()
                    .bounds(this.middleX - ITEM_WIDTH / 2, contentStartY, ITEM_WIDTH, contentHeight)
                    .build();
        }

        this.tabWidget = TabWidget.builder()
                .bounds(0, 0, this.width, this.height)
                .tab(StoreCategory.UPGRADES.displayName())
                .content(this.tabContents[0])
                .tab(StoreCategory.BOOSTS.displayName())
                .content(this.tabContents[1])
                .tab(StoreCategory.COSMETICS.displayName())
                .content(this.tabContents[2])
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

        var relativeY = CONTENT_MARGIN;
        this.renderItemButtons(content, items, categoryPage, relativeY, category);
        this.renderPaginationButtons(content, items, totalPages, categoryPage, relativeY + ITEMS_PER_PAGE * (ITEM_HEIGHT + ITEM_SPACING), category);
    }

    private void renderItemButtons(TabWidget.TabContent content, List<ShopItem> items, int page, int relativeStartY, StoreCategory category) {
        var fromIndex = page * ITEMS_PER_PAGE;
        var toIndex = Math.min(fromIndex + ITEMS_PER_PAGE, items.size());

        for (var i = fromIndex; i < toIndex; i++) {
            var item = items.get(i);
            var itemIndex = i;
            var y = content.getY() + relativeStartY + (i - fromIndex) * (ITEM_HEIGHT + ITEM_SPACING);

            var text = this.getItemText(item);
            var button = Button.builder()
                    .text(text)
                    .bounds(content.getX(), y, ITEM_WIDTH, ITEM_HEIGHT)
                    .command(() -> this.onPurchaseItem(itemIndex, category))
                    .build();

            if (!item.canBuy()) {
                button.enabled = false;
            }

            content.add(button);
        }
    }

    private TextObject getItemText(ShopItem item) {
        var name = item.getName();
        var price = item.getPrice();
        return TextObject.translation("bubbleblaster.screen.store.item", name, price);
    }

    private void onPurchaseItem(int itemIndex, StoreCategory category) {
        var items = category.getItems();
        if (itemIndex < 0 || itemIndex >= items.size()) return;

        var item = items.get(itemIndex);
        if (!item.canBuy()) return;

        var money = this.game.getGlobalData().getMoney();
        if (money < item.getPrice()) return;

        this.game.getGlobalData().setMoney(money - item.getPrice());
        item.onBought();

        this.refreshActiveContent();
    }

    private void renderPaginationButtons(TabWidget.TabContent content, List<ShopItem> items, int totalPages, int page, int relativeStartY, StoreCategory category) {
        if (totalPages <= 1) return;

        var absStartY = content.getY() + relativeStartY;
        var prevButton = Button.builder()
                .text(TextObject.translation("bubbleblaster.misc.prev"))
                .bounds(content.getX() + (ITEM_WIDTH - 200) / 2, absStartY, 100, 40)
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
                .bounds(content.getX() + (ITEM_WIDTH - 200) / 2 + 100, absStartY, 100, 40)
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
        this.renderPaginationInfo(renderer);
        this.renderChildren(renderer, mouseX, mouseY, deltaTime);
    }

    private void renderPaginationInfo(Renderer renderer) {
        var items = this.selectedCategory.getItems();
        var totalPages = (int) Math.ceil(items.size() / (double) ITEMS_PER_PAGE);
        if (totalPages <= 1) return;

        var pageText = TextObject.translation("bubbleblaster.screen.store.page", this.currentPage + 1, totalPages);
        renderer.drawTextCenter(this.font, pageText.getText(), this.middleX, CONTENT_MARGIN + TAB_HEIGHT - 15, Colors.WHITE);
    }
}
