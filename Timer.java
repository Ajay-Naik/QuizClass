public class Timer {
    private long startTime;
    private long duration; // milliseconds
    private boolean isActive;

    public void startTimer(int minutes) {
        this.duration = minutes * 60L * 1000L;
        this.startTime = System.currentTimeMillis();
        this.isActive = true;
    }

    public long getRemainingTime() {
        if (!isActive) return 0;
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.max(0, duration - elapsed);
    }

    public boolean isTimeUp() {
        return getRemainingTime() == 0;
    }

    public void displayCountdown() {
        long seconds = getRemainingTime() / 1000;
        System.out.println("Time remaining: " + seconds / 60 + "m " + seconds % 60 + "s");
    }

    public void stopTimer() {
        isActive = false;
    }
}