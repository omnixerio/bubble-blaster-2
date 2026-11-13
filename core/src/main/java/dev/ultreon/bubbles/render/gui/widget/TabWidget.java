package dev.ultreon.bubbles.render.gui.widget;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Rectangle;
import dev.ultreon.bubbles.init.Fonts;
import dev.ultreon.bubbles.render.Colors;
import dev.ultreon.bubbles.render.Renderer;
import dev.ultreon.bubbles.render.gui.GuiStateListener;
import dev.ultreon.bubbles.text.TextObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TabWidget extends Container implements GuiStateListener {

    public static final com.badlogic.gdx.graphics.Color TAB_HEADER_COLOR = Colors.rgba(0xff, 0xff, 0xff, 0x60);

    public static class TabItem {
        private final TextObject text;
        private final List<Consumer<TabItem>> selectHandlers = new ArrayList<>();
        private TabContent content;

        public TabItem(TextObject text) {
            this.text = text;
        }

        public TextObject getText() {
            return this.text;
        }

        public void onSelect(Consumer<TabItem> handler) {
            this.selectHandlers.add(handler);
        }

        public void fireSelect() {
            for (var handler : this.selectHandlers) {
                handler.accept(this);
            }
        }

        public TabContent getContent() {
            return this.content;
        }

        public void setContent(TabContent content) {
            this.content = content;
        }
    }

    public static class TabContent extends Container {
        public TabContent(int x, int y, int width, int height) {
            super(x, y, width, height);
        }

        @Override
        public void render(Renderer renderer, int mouseX, int mouseY, float deltaTime) {
            for (var child : this.children)
                if (child.visible) {
                    child.render(renderer, mouseX, mouseY, deltaTime);
                }
        }

        public void clear() {
            this.clearWidgets();
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            protected Rectangle bounds = new Rectangle(0, 0, 400, 300);

            protected Builder() {
            }

            public TabContent build() {
                return new TabContent(
                        (int) this.bounds.x,
                        (int) this.bounds.y,
                        (int) this.bounds.width,
                        (int) this.bounds.height
                );
            }

            public Builder bounds(Rectangle bounds) {
                this.bounds = bounds;
                return this;
            }

            public Builder bounds(int x, int y, int width, int height) {
                this.bounds = new Rectangle(x, y, width, height);
                return this;
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final List<TabItem> tabs = new ArrayList<>();
    private final List<Button> tabButtons = new ArrayList<>();
    private int selectedIndex = 0;
    private final int tabWidth;
    private final int tabHeight;
    private final int gap;
    private Consumer<Integer> onTabChanged;

    public TabWidget(List<TabItem> tabs, int selectedIndex, int tabWidth, int tabHeight, int gap, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.tabs.addAll(tabs);
        this.tabWidth = tabWidth;
        this.tabHeight = tabHeight;
        this.gap = gap;
        this.selectedIndex = Math.min(selectedIndex, this.tabs.size() - 1);
    }

    public int getSelectedIndex() {
        return this.selectedIndex;
    }

    public TabItem getSelectedTab() {
        return this.tabs.get(this.selectedIndex);
    }

    public void setSelectedIndex(int index) {
        if (index < 0 || index >= this.tabs.size() || index == this.selectedIndex) {
            return;
        }
        this.selectedIndex = index;
        this.updateTabButtons();
        if (this.onTabChanged != null) {
            this.onTabChanged.accept(this.selectedIndex);
        }
        this.tabs.get(this.selectedIndex).fireSelect();
    }

    public void setOnTabChanged(Consumer<Integer> handler) {
        this.onTabChanged = handler;
    }

    public List<TabItem> getTabs() {
        return new ArrayList<>(this.tabs);
    }

    private void updateTabButtons() {
        for (var button : this.tabButtons) {
            button.enabled = true;
        }
        this.updateTabContentVisibility();
    }

    private void updateTabContentVisibility() {
        for (var i = 0; i < this.tabs.size(); i++) {
            var content = this.tabs.get(i).getContent();
            if (content != null) {
                content.visible = (i == this.selectedIndex);
            }
        }
    }

    private void createTabButtons() {
        this.tabButtons.clear();
        var totalWidth = this.tabs.size() * this.tabWidth + (this.tabs.size() - 1) * this.gap;
        var startX = (this.width - totalWidth) / 2;

        var index = 0;
        for (var tab : this.tabs) {
            final var tabIndex = index;
            var x = startX + index * (this.tabWidth + this.gap);
            var y = 0;

            var tabButton = Button.builder()
                    .text(tab.getText())
                    .bounds(x, y, this.tabWidth, this.tabHeight)
                    .command(() -> this.setSelectedIndex(tabIndex))
                    .build();
            this.tabButtons.add(tabButton);
            index++;
        }
    }

    @Override
    public void make() {
        super.make();
        this.createTabButtons();
        this.createTabContent();
    }

    private void createTabContent() {
        for (var tab : this.tabs) {
            var content = tab.getContent();
            if (content != null) {
                this.add(content);
            }
        }
        this.updateTabContentVisibility();
    }

    private int getTabButtonAt(int mouseX, int mouseY) {
        for (var i = 0; i < this.tabButtons.size(); i++) {
            var button = this.tabButtons.get(i);
            var btnX = this.x + button.getX();
            var btnY = this.y + button.getY();
            if (mouseX >= btnX && mouseX < btnX + button.getWidth() && mouseY >= btnY && mouseY < btnY + button.getHeight()) {
                return i;
            }
        }
        return -1;
    }

    private int hoveredTabButton = -1;
    private int pressedTabButton = -1;

    @Override
    public boolean mousePress(int x, int y, int button) {
        var tabIdx = this.getTabButtonAt(x, y);
        if (tabIdx >= 0) {
            this.pressedTabButton = tabIdx;
            this.tabButtons.get(tabIdx).mousePress(x, y, button);
            return true;
        }
        this.pressedTabButton = -1;
        // Delegate to active tab content
        var activeContent = this.tabs.get(this.selectedIndex).getContent();
        if (activeContent != null && activeContent.visible) {
            return activeContent.mousePress(x, y, button);
        }
        return false;
    }

    @Override
    public boolean mouseRelease(int x, int y, int button) {
        if (this.pressedTabButton >= 0) {
            var btn = this.tabButtons.get(this.pressedTabButton);
            var btnX = this.x + btn.getX();
            var btnY = this.y + btn.getY();
            if (x >= btnX && x < btnX + btn.getWidth() && y >= btnY && y < btnY + btn.getHeight()) {
                btn.mouseRelease(x, y, button);
            }
            this.pressedTabButton = -1;
            return true;
        }
        // Delegate to active tab content
        var activeContent = this.tabs.get(this.selectedIndex).getContent();
        if (activeContent != null && activeContent.visible) {
            return activeContent.mouseRelease(x, y, button);
        }
        return false;
    }

    @Override
    public void mouseMove(int x, int y) {
        var tabIdx = this.getTabButtonAt(x, y);
        if (tabIdx != this.hoveredTabButton) {
            if (this.hoveredTabButton >= 0) {
                this.tabButtons.get(this.hoveredTabButton).mouseExit();
            }
            this.hoveredTabButton = tabIdx;
            if (this.hoveredTabButton >= 0) {
                this.tabButtons.get(this.hoveredTabButton).mouseEnter(x, y);
            }
        }
    }

    @Override
    public void render(Renderer renderer, int mouseX, int mouseY, float deltaTime) {
        this.renderTabButtons(renderer, mouseX, mouseY);
        this.renderTabHeader(renderer);
        this.renderChildren(renderer, mouseX, mouseY, deltaTime);
    }

    private void renderTabButtons(Renderer renderer, int mouseX, int mouseY) {
        for (var button : this.tabButtons) {
            var btnX = this.x + button.getX();
            var btnY = this.y + button.getY();
            // Temporarily set absolute coordinates for rendering
            var oldX = button.getX();
            var oldY = button.getY();
            button.setX(btnX);
            button.setY(btnY);
            button.render(renderer, mouseX, mouseY, 0);
            button.setX(oldX);
            button.setY(oldY);
        }
    }

    private void renderTabHeader(Renderer renderer) {
        for (var i = 0; i < this.tabButtons.size(); i++) {
            var button = this.tabButtons.get(i);
            if (i == this.selectedIndex) {
                var btnX = this.x + button.getX();
                var btnY = this.y + button.getY();
                renderer.fill(btnX, btnY, button.getWidth(), button.getHeight(), TAB_HEADER_COLOR);
            }
        }
    }

    public static class Builder {
        protected Rectangle bounds = new Rectangle(10, 10, 400, 100);
        protected final List<TabItem> tabs = new ArrayList<>();
        protected int selectedIndex = 0;
        protected int tabWidth = 120;
        protected int tabHeight = 40;
        protected int gap = 10;
        protected BitmapFont font = Fonts.DEFAULT.get();
        protected Consumer<Integer> onTabChanged;

        protected Builder() {
        }

        public TabWidget build() {
            var widget = new TabWidget(
                    this.tabs,
                    this.selectedIndex,
                    this.tabWidth,
                    this.tabHeight,
                    this.gap,
                    (int) this.bounds.x,
                    (int) this.bounds.y,
                    (int) this.bounds.width,
                    (int) this.bounds.height
            );
            if (this.onTabChanged != null) {
                widget.setOnTabChanged(this.onTabChanged);
            }
            return widget;
        }

        public Builder bounds(Rectangle bounds) {
            this.bounds = bounds;
            return this;
        }

        public Builder bounds(int x, int y, int width, int height) {
            this.bounds = new Rectangle(x, y, width, height);
            return this;
        }

        public Builder tab(TextObject text) {
            this.tabs.add(new TabItem(text));
            return this;
        }

        public Builder tab(String text) {
            this.tabs.add(new TabItem(TextObject.literal(text)));
            return this;
        }

        public Builder content(TabContent content) {
            if (!this.tabs.isEmpty()) {
                this.tabs.get(this.tabs.size() - 1).setContent(content);
            }
            return this;
        }

        public Builder selectedIndex(int index) {
            this.selectedIndex = index;
            return this;
        }

        public Builder tabWidth(int width) {
            this.tabWidth = width;
            return this;
        }

        public Builder tabHeight(int height) {
            this.tabHeight = height;
            return this;
        }

        public Builder gap(int gap) {
            this.gap = gap;
            return this;
        }

        public  Builder font(BitmapFont font) {
            this.font = font;
            return this;
        }

        public Builder onTabChanged(Consumer<Integer> handler) {
            this.onTabChanged = handler;
            return this;
        }
    }
}
