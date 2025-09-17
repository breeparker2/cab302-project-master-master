package app.demo;

public final class Settings {
    private static double brightness = 1.0; // 1.0 = neutral
    private static double volume = 1.0;     // 1.0 = full

    public static double getBrightness() { return brightness; }
    public static void setBrightness(double b) { brightness = b; }

    public static double getVolume() { return volume; }
    public static void setVolume(double v) { volume = v; }
}
