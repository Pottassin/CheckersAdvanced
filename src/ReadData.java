import java.io.*;
import java.nio.charset.StandardCharsets;


public class ReadData {
    public static void main(String[] args) throws Exception {
        new MainMenu();
        // Adjust paths if needed
        String pythonExe = "C:\\Users\\gabri\\AppData\\Local\\Programs\\Python\\Python312\\python.exe";               // or "python3"
        String script = "C:\\Users\\gabri\\Better-Checker--main\\Better-Checker--main\\openpose.py";

        ProcessBuilder pb = new ProcessBuilder(pythonExe, "-u", script);
        pb.redirectErrorStream(true); // merge stderr -> stdout so you see Python errors
        Process p = pb.start();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = br.readLine()) != null) {
                // parse or route JSON line; for demo just print first 80 chars
                //System.out.println("[PY->JAVA] " + (line.length() > 80 ? line.substring(0,80)+"…" : line));
                // Here you'd use a JSON lib (Gson/Jackson) to parse `line`
                if (line != null) {
                    String tempLine = line;
                    int r = tempLine.indexOf("LEFT_WRIST");
                    float handX;
                    float handY;
                    if (r >= 0) {
                        tempLine = tempLine.substring(0, r);
                        tempLine = tempLine.substring(tempLine.lastIndexOf("x") + 4);
                        tempLine = tempLine.substring(0, tempLine.indexOf(","));
                        handX = Float.parseFloat(tempLine);
                        tempLine = line;
                        tempLine = tempLine.substring(0, r);
                        tempLine = tempLine.substring(tempLine.lastIndexOf("y") + 4);
                        tempLine = tempLine.substring(0, tempLine.indexOf(","));
                        handY = Float.parseFloat(tempLine);
                        if (handX >= .8f) {
                            if (MainMenu.Duel.instance.defender != null)
                                MainMenu.Duel.instance.defender.shield();
                            if (MainMenu.Duel.instance.challenger != null)
                                MainMenu.Duel.instance.challenger.shield();
                        }
                        else {
                            if (MainMenu.Duel.instance.defender != null)
                                MainMenu.Duel.instance.defender.unshield();
                            if (MainMenu.Duel.instance.challenger != null)
                                MainMenu.Duel.instance.challenger.unshield();
                        }
                        if (handY >= 1f) {
                            if (MainMenu.Duel.instance.defender != null)
                                MainMenu.Duel.instance.defender.feign();
                            if (MainMenu.Duel.instance.challenger != null)
                                MainMenu.Duel.instance.challenger.feign();
                        }
                        if (handY <= .5f) {
                            if (MainMenu.Duel.instance.defender != null)
                                MainMenu.Duel.instance.defender.attack();
                            if (MainMenu.Duel.instance.challenger != null)
                                MainMenu.Duel.instance.challenger.attack();

                        }
                    }
                    tempLine = line;
                    r = tempLine.indexOf("RIGHT_WRIST");
                    if (r >= 0) {
                        tempLine = tempLine.substring(0, r);
                        tempLine = tempLine.substring(tempLine.lastIndexOf("x") + 4);
                        tempLine = tempLine.substring(0, tempLine.indexOf(","));
                        handX = Float.parseFloat(tempLine) - .35f;
                        tempLine = line;
                        //System.out.println(handX);
                        tempLine = tempLine.substring(0, r);
                        tempLine = tempLine.substring(tempLine.lastIndexOf("y") + 4);
                        tempLine = tempLine.substring(0, tempLine.indexOf(","));
                        handY = Float.parseFloat(tempLine) - .82f;
                        //System.out.println(handY);
                        if (Math.sqrt(handX*handX+handY*handY) >= .1f) {
                            if (MainMenu.Duel.instance.defender != null)
                                MainMenu.Duel.instance.defender.moveToward(handX, handY);
                            if (MainMenu.Duel.instance.challenger != null)
                                MainMenu.Duel.instance.challenger.moveToward(handX, handY);
                        }
                    }
                }
            }
        }
        int exit = p.waitFor();
        System.out.println("Python exited with code " + exit);
    }
}