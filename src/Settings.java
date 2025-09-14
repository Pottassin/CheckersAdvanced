import java.awt.*;

public class Settings {

    public static Settings instance;

    public Color p1Color = new Color(190, 190, 115);

    public Color p2Color = new Color(40, 40, 40);

    public Color t1Color = new Color(255, 255, 255);

    public Color t2Color = new Color(0, 0, 0);

    public Settings() {
        instance = this;
    }
}
