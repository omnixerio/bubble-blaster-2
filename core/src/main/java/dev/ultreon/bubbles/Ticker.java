package dev.ultreon.bubbles;


public class Ticker {
    
    int ticks;

    public Ticker() {

    }

    
    public int advance() {
        return ++this.ticks;
    }

    
    public int get() {
        return this.ticks;
    }

    public void reset() {
        this.ticks = 0;
    }
}
