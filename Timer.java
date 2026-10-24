/**
 * A passive countdown clock. It never fires an event: whoever needs to react
 * to time running out has to ask isTimeUp() (the console watcher thread polls
 * it through Quiz.isOpen()).
 */
public class Timer {
    private long startTime;
    private long duration; // milliseconds
    private volatile boolean isActive; // read by the watcher thread, written by the main thread

    /** Starts (or restarts) the countdown for the given number of minutes. */
    public void startTimer(int minutes) {
        this.duration = minutes * 60L * 1000L;
        this.startTime = System.currentTimeMillis();
        this.isActive = true;
    }

    /** Milliseconds left; 0 when the time has run out or the timer was stopped. */
    public long getRemainingTime() {
        if (!isActive) {
            return 0;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0, duration - elapsed);
    }

    public boolean isTimeUp() {
        return getRemainingTime() == 0;
    }

    /** Stops the clock immediately; isTimeUp() is true afterwards. */
    public void stopTimer() {
        isActive = false;
    }
}
