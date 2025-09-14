import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.function.*;
import java.lang.Math;
//import javax.swing.LookAndFeel;
//import javax.swing.plaf.basic.BasicLookAndFeel;
import java.lang.Thread;
    
// look at Area class

public class Engine {

    public static void main(String[] args) {
        new MainMenu();
    }

    public static class Prog {

        public static Random randomIntGen;
  
        public int frame = 0;
        public int frameRate = 25;
  
        public PQ<Script> toUpdate = new PQ<Script>();
  
        public ArrayList<Script> toAddU = new ArrayList<Script>();
        public ArrayList<Script> toRemU = new ArrayList<Script>();
          
        public ArrayList<UpdateThread> ts = new ArrayList<UpdateThread>();
        int numThreads = 10;
        
        Timer updateTimer;

        long prevT;
        
        public Prog() {
            /*new Screen("Unnamed Program", 400, 400);
            initialize();*/
        }
        
        public Prog(String name) {
            new Screen(name, 400, 400);
            initialize();
        }
        
        public Prog(String name, int x, int y) {
            new Screen(name, x, y);
            initialize();
        }
  
        public void initialize() {
            for (int x = 0; x < numThreads; x++) {
                ts.add(new UpdateThread());
            }
            randomIntGen = new Random();
            updateTimer = new Timer(frameRate, new UpdateTimer());
            updateTimer.start();
            //new TimerThread();
            //new Timer(frameRate, () -> update(), true);
            prevT = System.currentTimeMillis();
        }
  
        public void update() {
            //System.out.println("start: " + (System.currentTimeMillis() - prevT));
            prevT = System.currentTimeMillis();
            frame++;
            //toUpdate.size();
            KeyDetect.instance.checkKeysHeld();
            //System.out.println("k: " + (System.currentTimeMillis() - prevT));
            prevT = System.currentTimeMillis();
            for (Script s : toUpdate) {
                if (s.sim)
                    while (true) {
                        if (freeThread() != null) {
                            freeThread().set(() -> s.update()).run();
                            break;
                        }
                        Thread.onSpinWait();
                        //SwingUtilities.invokeLater(() -> s.update());
                    }
            }
            //System.out.println("u: " + (System.currentTimeMillis() - prevT));
            prevT = System.currentTimeMillis();
            //System.out.println(Screen.instance.isFocused());
            Screen.instance.draw.repaint();
            for (Script s : toAddU) {
                toUpdate.add(s);
            }
            for (Script s : toRemU) {
                toUpdate.remove(s);
            }
            toAddU.clear();
            toRemU.clear();
            MouseDetect.instance.updateMouse();
            //System.out.println("e: " + (System.currentTimeMillis() - prevT));
            prevT = System.currentTimeMillis();
        }
  
        public UpdateThread freeThread() {
            for (UpdateThread ut : ts) {
                if (ut.isFree) {
                    return ut;
                }
            }
            return null;
        }
  
        public class UpdateTimer implements ActionListener {
          
            @Override
            public void actionPerformed(ActionEvent e) {
                update();
            }
          
        }
  
        public void addToUpdate(Script s) {
            toAddU.add(s);
        }
  
        public void removeToUpdate(Script s) {
            toRemU.add(s);
        }
  
        public class UpdateThread extends Thread {
            Runnable r;
            boolean isFree = true;
            public UpdateThread() {}
            public UpdateThread set(Runnable r) {
                this.r = r;
                return this;
            }
            public void run() {
                isFree = false;
                r.run();
                try {
                  join();
                }
                catch (InterruptedException i) {
                    i.printStackTrace();
                }
                isFree = true;
                //return;
            }
        }
        
        public class Screen extends JFrame {
  
          public static Screen instance;
          public CameraManager draw;
  
          public Screen() {
              instance = this;
              addKeyListener(new KeyDetect());
              addMouseListener(new MouseDetect());
              draw = new CameraManager();
              add(draw);
              setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
              setSize(500, 400 /*+ 26*/);
              draw.setSize(500, 400 /*+ 26*/);
              setVisible(true);
              System.out.println("new screen");
          }
          
          public Screen(String name, int x, int y) {
              super(name);
              instance = this;
              addKeyListener(new KeyDetect());
              addMouseListener(new MouseDetect());
              draw = new CameraManager();
              add(draw);
              setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
              setSize(x, y /*+ 27*/);
              draw.setSize(x, y /*+ 27*/);
              setVisible(true);
              System.out.println("new screen");
          }
          
        }
  
        public class CameraManager extends JPanel {
        
            public CameraManager() {
                super();
            }
  
            public static void addToDraw(Script s) {
                for (Camera c : Camera.cams)
                    c.toDraw.add(s);
            }
  
            public static void addToDraw(Script s, int cs) {
                for (int x = 0; x < Camera.cams.size(); x++) {
                    if (cs % 2 == 1)
                        Camera.cams.get(x).toDraw.add(s);
                    cs = cs >> 1;
                }
            }
  
            public static void removeToDraw(Script s) {
                for (Camera c : Camera.cams)
                    c.toDraw.remove(s);
            }
  
            public static void removeToDraw(Script s, int cs) {
                for (int x = 0; x < Camera.cams.size(); x++) {
                    if (cs % 2 == 1)
                        Camera.cams.get(x).toDraw.remove(s);
                    cs = cs >> 1;
                }
            }
              
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                for (Camera c : Camera.cams)
                    c.paint(g);
            }
            
        }
  
        public class Camera {
            public static ArrayList<Camera> cams = new ArrayList<Camera>();
            public ArrayList<Script> toDraw = new ArrayList<Script>();
            int x; int y; int w; int h;
            public Camera(int x, int y, int w, int h) {
                this.x = x;
                this.y = y;
                this.w = w;
                this.h = h;
                cams.add(this);
            }

            public void resize(int x, int y, int w, int h) {
                this.x = x;
                this.y = y;
                this.w = w;
                this.h = h;
            }

            public void addToDraw(Script s) { toDraw.add(s); }
  
            public void removeToDraw(Script s) {
                toDraw.remove(s);
            }
      
            public void paint(Graphics g) {
                g.setClip(x, y, w, h);
                for (Script s : toDraw) {
                    if (s.sim)
                      s.render(g);
                }
            }
        }
  
        public class KeyDetect extends KeyAdapter {
  
          public static KeyDetect instance;
          
          TreeSet<Integer> heldKeys = new TreeSet<Integer>();
  
          HashSet<Script> pressListen = new HashSet<Script>();
          HashSet<Script> holdListen = new HashSet<Script>();
          HashSet<Script> releaseListen = new HashSet<Script>();
  
          boolean isHandlingKeys = false;
  
          public KeyDetect() {
              instance = this;
          }
          
          public void keyPressed(KeyEvent e) {
              isHandlingKeys = true;
              heldKeys.add(e.getKeyCode());
              for (Script s : pressListen) {
                  s.keyPressed(e.getKeyCode());
              }
              isHandlingKeys = false;
          }
  
          public void checkKeysHeld() {
              isHandlingKeys = true;
              for (int key : heldKeys) {
                  for (Script s : holdListen) {
                      s.keyHeld(key);
                  }
              }
              isHandlingKeys = false;
          }
  
          public void keyReleased(KeyEvent e) {
              isHandlingKeys = true;
              heldKeys.remove(e.getKeyCode());
              for (Script s : releaseListen) {
                  s.keyReleased(e.getKeyCode());
              }
              isHandlingKeys = false;
          }
          
          public void addP(Script s) { pressListen.add(s); }
          public void addH(Script s) { holdListen.add(s); }
          public void addR(Script s) { releaseListen.add(s); }
          
          public void removeP(Script s) { if (isHandlingKeys) SwingUtilities.invokeLater(() -> pressListen.remove(s)); else pressListen.remove(s); }
          public void removeH(Script s) { if (isHandlingKeys) SwingUtilities.invokeLater(() -> holdListen.remove(s)); else holdListen.remove(s); }
          public void removeR(Script s) { if (isHandlingKeys) SwingUtilities.invokeLater(() -> releaseListen.remove(s)); else releaseListen.remove(s); }
        }
  
        public class MouseDetect extends MouseAdapter {
            
            static MouseDetect instance;
          
            //TreeSet<Integer> heldKeys = new TreeSet<Integer>();
  
            HashSet<Script> clickListen = new HashSet<Script>();
            HashSet<Script> pressListen = new HashSet<Script>();
            HashSet<Script> releaseListen = new HashSet<Script>();
            HashSet<Script> moveListen = new HashSet<Script>();

            Coord prevMousePos = new Coord();

            boolean isMouseDown = false;
            
            boolean isHandlingMouse = false;

            int framesDown = 0;
            int cDir = -1;
  
            public MouseDetect() {
                instance = this;
            }

            public void updateMouse() {
                framesDown += cDir;
                prevMousePos = mousePos();
            }

            public Coord deltaMousePos() {
                return new Coord(mousePos().x - prevMousePos.x, mousePos().y - prevMousePos.y);
            }
  
            public void mouseMoved(MouseEvent e) {
                isHandlingMouse = true;
                for (Script s : moveListen) {
                    s.mouseMoved(e);
                }
                isHandlingMouse = false;
            }
            
            public void mouseClicked(MouseEvent e) {
                isHandlingMouse = true;
                for (Script s : clickListen) {
                    s.mouseClicked(e);
                }
                isHandlingMouse = false;
            }
  
            public void mousePressed(MouseEvent e) {

                isMouseDown = true;
                isHandlingMouse = true;
                for (Script s : pressListen) {
                    s.mousePressed(e);
                }
                framesDown = 0;
                cDir = 1;
                isHandlingMouse = false;
            }
            
            public void mouseReleased(MouseEvent e) {
                isMouseDown = false;
                isHandlingMouse = true;
                for (Script s : releaseListen) {
                    s.mouseReleased(e);
                }
                framesDown = 0;
                cDir = -1;
                isHandlingMouse = false;
            }

            //public Coord mousePos() { return new Coord(MouseInfo.getPointerInfo().getLocation()); }
            public Coord mousePos() {
                Coord c = new Coord(Screen.instance.getMousePosition());
                c.y -= 29.5f;
                c.x -= 6.25f;
                return c;
            }

            public void addM(Script s) { moveListen.add(s); }
            public void addC(Script s) { clickListen.add(s); }
            public void addP(Script s) { pressListen.add(s); }
            public void addR(Script s) { releaseListen.add(s); }
  
            public void removeM(Script s) { if (isHandlingMouse) SwingUtilities.invokeLater(() -> moveListen.remove(s)); else moveListen.remove(s); }
            public void removeC(Script s) { if (isHandlingMouse) SwingUtilities.invokeLater(() -> clickListen.remove(s)); else clickListen.remove(s); }
            public void removeP(Script s) { if (isHandlingMouse) SwingUtilities.invokeLater(() -> pressListen.remove(s)); else pressListen.remove(s); }
            public void removeR(Script s) { if (isHandlingMouse) SwingUtilities.invokeLater(() -> releaseListen.remove(s)); else releaseListen.remove(s); }
        }
  
        public class Script {
  
            //public static HashSet<Script> allInstances = new HashSet<Script>();
  
            public boolean sim = true;
  
            /*public void Script() {
                allInstances.add(this);
            }*/
          
            public void start() {}
            public void destroy() {}
            public void keyPressed(int e) {}
            public void keyHeld(int e) {}
            public void keyReleased(int e) {}
            public void mouseMoved(MouseEvent e) {}
            public void mouseClicked(MouseEvent e) {}
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void update() { /*System.out.println("update");*/ }
            public void render(Graphics g) {
                g.setColor(Color.green);
                g.fillRect(0, 0, 100, 200);
                System.out.println("render");
            }
          
        }
  
        public class Coord extends Object implements Comparable<Coord> {
  
            public float x;
            public float y;
    
            public Coord() { x = 0; y = 0; }
            public Coord(float x, float y) { this.x = x; this.y = y; }
            public Coord(Coord copy) { x = copy.x; y = copy.y; }
            public Coord(Point copy) { x = 0; if (copy != null) x = copy.x; y = 0; if (copy != null) y = copy.y; }

            public int x() { return (int) x; }

            public int y() { return (int) y; }
            
            @Override
            public boolean equals(Object o) {
                System.out.println("test");
                if (o != null) {
                    if (o instanceof Coord) {
                        Coord temp = (Coord) o;
                        return temp.x == x && temp.y == y;
            } } return false; }
            
            public int compareTo(Coord c) {
                if (mag() == c.mag()) return 0;
                if (mag() < c.mag()) return -1;
                return 1;
            }
    
            public void reduce() {
                float m = mag();
                if (m == 0) { return; }
                x = x / m;
                y = y / m;
            }
    
            public float distanceTo(Coord c) {
                return (float) Math.sqrt((c.x - x) * (c.x - x) + (c.y - y) * (c.y - y));
            }
    
            public float distanceTo(Coord c, Coord o) {
                return (float) Math.sqrt((c.x + o.x - x) * (c.x + o.x - x) + (c.y + o.y -y) * (c.y + o.y - y));
            }
    
            public float mag() { return (float) Math.sqrt(x * x + y * y); }
            public double angle() { return Math.atan2(y, x); }
    
            public static double angleBetween(Coord c1, Coord c2) {
                return Math.atan2(c1.y - c2.y, c1.x - c2.x);
            }

            public void turn(double a) {
                x = (float) (mag() * Math.cos(angle() + a));
                y = (float) (mag() * Math.sin(angle() + a));
            }

            public void setAngle(double a) {
                x = (float) (mag() * Math.cos(a));
                y = (float) (mag() * Math.sin(a));
            }
    
            public PCoord getPCoord() { return new PCoord(angle(), mag()); }
    
            public String toString() {
                return "(" + x + ", " + y + ")";
            }
  
        }
  
        public class PCoord extends Object implements Comparable<PCoord> {
  
            public double a;
            public float d;
            
            public PCoord() { a = 0; d = 0; }
            public PCoord(double a, float d) { this.a = a; this.d = d; }
            
            @Override
            public boolean equals(Object o) {
                System.out.println("test");
                if (o != null) {
                  if (o instanceof PCoord) {
                      PCoord temp = (PCoord) o;
                      return temp.a == a && temp.d == d;
            } } return false; }
            
            public int compareTo(PCoord c) {
                if (d == c.d) return 0;
                if (d < c.d) return -1;
                return 1;
            }
            
            public float x() { return (int) (Math.cos(a) * d); }
            public float y() { return (int) (Math.sin(a) * d); }
            
            public Coord getCoord() { return new Coord(x(), y()); }
            
            public String toString() {
              return "(" + d +" @ " + a + ")";
            }
        }

        public class Line extends Object implements Comparable<Line> {
            Coord s;
            Coord e;
            public Line() {
                s = new Coord();
                e = new Coord();
            }
            public Line(Coord start, Coord end) {
                s = start;
                e = end;
            }
            @Override
            public boolean equals(Object o) {
                if (o != null) {
                  if (o instanceof Line) {
                      Line temp = (Line) o;
                      return temp.s.x == s.x && temp.s.y == s.y && temp.e.x == e.x && temp.e.y == e.y;
            } } return false; }
            public int compareTo(Line c) {
                /*if (temp.s.x == s.x && temp.s.y == s.y && temp.e.x == e.x && temp.e.y == e.y) return 0;
                if ((Math.abs(temp.s.x) > Math.abs(s.x) && Math.abs(temp.e.x) > Math.abs(e.x)) || (Math.abs(temp.s.y) > Math.abs(s.y) && Math.abs(temp.e.y) > Math.abs(e.y))) return -1;
                return 1;*/
                if (slope() > c.slope()) return 1;
                if (slope() < c.slope()) return -1;
                return 0;
            }
            public float slope() {
                return dy() / dx();
            }
            public int dir() {
                // none = -1, ver = 0, up = 1, hor = 2, down = 3
                if (e.x == s.x) {
                    if (e.y == s.y) return -1;
                    return 0;
                }
                if (e.y == s.y) return 2;
                if (slope() > 0) return 1;
                return 3;
            }
            public float yInt() {
                return s.y-s.x*slope();
            }
            public float xInt() {
                return s.x-s.y/slope();
            }
            public float dy() {
                return e.y - s.y;
            }
            public float dx() {
                return e.x - s.x;
            }
            public String toString() {
                return "y "+(-s.y)+" = "+slope()+" * ( x "+(-s.x)+" ) { "+s.x+" , "+e.x+" }";
            }
        }
  
        public class Collider {
          
            Coord og;
    
            public Collider(Coord c) {
                og = c;
            }
    
            public boolean isColliding(Collider c) {
                return og == c.og;
            }
    
            public boolean isColliding(CircleCollider c) {
                return Math.sqrt((og.x - c.og.x)*(og.x - c.og.x) + (og.y - c.og.y)*(og.y - c.og.y)) <= c.rad;
            }
    
            public boolean isColliding(RectCollider c) {
                float transformedX = (float) (-Math.cos(c.length.a)*(c.og.x-og.x)+Math.sin(c.length.a)*(-c.og.y+og.y));
                float transformedY = (float) (-Math.sin(c.length.a)*(c.og.x-og.x)-Math.cos(c.length.a)*(-c.og.y+og.y));
                if (c.length.d - transformedX < 0) { return false; }
                if (c.width - transformedY < 0) { return false; }
                if (transformedX < 0) { return false; }
                if (c.width + transformedY < 0) { return false; }
                return true;
            }
        }

        public class LineCollider extends Collider {
            Line l;
            public LineCollider(Coord s, Coord e) {
                super(s);
                l = new Line(s, e);
            }
        }
  
        public class RectCollider extends Collider {
  
            public PCoord length;
            public float width;
    
            public RectCollider(Coord c, PCoord l, float w) {
                super(c);
                length = l;
                width = w;
            }
    
            public boolean isColliding(Collider c) {
              float transformedX = (float) (-Math.cos(length.a)*(og.x-c.og.x)+Math.sin(length.a)*(-og.y+c.og.y));
              float transformedY = (float) (-Math.sin(length.a)*(og.x-c.og.x)-Math.cos(length.a)*(-og.y+c.og.y));
              if (length.d - transformedX < 0) { return false; }
              if (width - transformedY < 0) { return false; }
              if (transformedX < 0) { return false; }
              if (width + transformedY < 0) { return false; }
              return true;
            }
    
            public boolean isColliding(CircleCollider c) {
                float transformedX = (float) (-Math.cos(length.a)*(og.x-c.og.x)+Math.sin(length.a)*(-og.y+c.og.y));
                float transformedY = (float) (-Math.sin(length.a)*(og.x-c.og.x)-Math.cos(length.a)*(-og.y+c.og.y));
                byte trues = (byte) 0;
                byte falses = (byte) 0;
                if (length.d - transformedX + c.rad < 0) { return false; }
                else if (length.d - transformedX + c.rad >= c.rad) { trues++; }
                if (width - transformedY + c.rad < 0) { return false; }
                else if (width - transformedY + c.rad >= c.rad) { trues++; falses += 1; }
                if (transformedX + c.rad < 0) { return false; }
                else if (transformedX + c.rad >= c.rad) { trues++; falses += 2; }
                if (width + transformedY + c.rad < 0) { return false; }
                else if (width + transformedY + c.rad >= c.rad) { trues++; falses += 4; }
                if (trues >= 3) { return true; }
                else {
                    if (falses == 1) {
                        if (c.rad - og.distanceTo(new Coord(transformedX - length.d, transformedY - width)) <= c.rad) { return true; }
                        return false;
                    }
                    if (falses == 3) {
                        if (c.rad - og.distanceTo(new Coord(transformedX, transformedY - width)) <= c.rad) { return true; }
                        return false;
                    }
                    if (falses == 6) {
                        if (c.rad - og.distanceTo(new Coord(transformedX, transformedY + width)) <= c.rad) { return true; }
                        return false;
                    }
                    if (falses == 4) {
                        if (c.rad - og.distanceTo(new Coord(transformedX - length.d, transformedY + width)) <= c.rad) { return true; }
                        return false;
                    }
                }
                return false;
            }
    
            public boolean isColliding(RectCollider c) {
                System.out.println("to be implemented");
                return false;
            }
          
        }
  
        public class CircleCollider extends Collider {
          
            public float rad;
            
            public CircleCollider(Coord c, float r) {
                super(c);
                rad = r;
            }
    
            public boolean isColliding(Collider c) {
                return og.distanceTo(c.og) <= rad;
            }
    
            public boolean isColliding(CircleCollider c) {
                return og.distanceTo(c.og) <= c.rad + rad;
            }
    
            public boolean isColliding(RectCollider c) {
                float transformedX = (float) (-Math.cos(c.length.a)*(c.og.x-og.x)+Math.sin(c.length.a)*(-c.og.y+og.y));
                float transformedY = (float) (-Math.sin(c.length.a)*(c.og.x-og.x)-Math.cos(c.length.a)*(-c.og.y+og.y));
                byte trues = (byte) 0;
                byte falses = (byte) 0;
                if (c.length.d - transformedX + rad < 0) { return false; }
                else if (c.length.d - transformedX + rad >= rad) { trues++; }
                if (c.width - transformedY + rad < 0) { return false; }
                else if (c.width - transformedY + rad >= rad) { trues++; falses += 1; }
                if (transformedX + rad < 0) { return false; }
                else if (transformedX + rad >= rad) { trues++; falses += 2; }
                if (c.width + transformedY + rad < 0) { return false; }
                else if (c.width + transformedY + rad >= rad) { trues++; falses += 4; }
                if (trues >= 3) { return true; }
                else {
                    if (falses == 1) {
                        if (rad - c.og.distanceTo(new Coord(transformedX - c.length.d, transformedY - c.width)) <= rad) { return true; }
                        return false;
                    }
                    if (falses == 3) {
                        if (rad - c.og.distanceTo(new Coord(transformedX, transformedY - c.width)) <= rad) { return true; }
                        return false;
                    }
                    if (falses == 6) {
                        if (rad - c.og.distanceTo(new Coord(transformedX, transformedY + c.width)) <= rad) { return true; }
                        return false;
                    }
                    if (falses == 4) {
                        if (rad - c.og.distanceTo(new Coord(transformedX - c.length.d, transformedY + c.width)) <= rad) { return true; }
                        return false;
                    }
                }
                return false;
            }
    
            public ArrayList<Coord> getEdgeContacts(Collider c) {
                ArrayList<Coord> ret = new ArrayList<Coord>();
                if (og.distanceTo(c.og) == rad) {
                    ret.add(c.og);
                }
                return ret;
            }
    
            public ArrayList<Coord> getEdgeContacts(CircleCollider c) {
                System.out.println("to be implemented");
                ArrayList<Coord> ret = new ArrayList<Coord>();
                float dx = (og.x-c.og.x);
                float dy = (og.y-c.og.y);
                if (og.distanceTo(c.og) < Math.abs(rad-c.rad)) { return ret; } // should be guarunteed
                if (og.y != c.og.y) {
                    float a = (float) Math.pow((dx*dx+dy*dy-c.rad*c.rad+rad*rad)
                        / (2*dy), 2)-rad*rad
                        / (1 + (float) Math.pow(dx/dy, 2));
                    float b = (dx*dx+dy*dy-c.rad*c.rad+rad*rad)*dx/dy
                        / (dy+dx*dx/dy);
                    if (Math.pow(b, 2)-4*a > 0) {
                        ret.add(new Coord((-b+(float) Math.sqrt(b*b-4*a))/2, (((-b+(float) Math.sqrt(b*b-4*a))/2)*-2*dx+dy*dy+dx*dx-c.rad*c.rad+rad*rad)/2*dy));
                        ret.add(new Coord((-b-(float) Math.sqrt(b*b-4*a))/2, (((-b-(float) Math.sqrt(b*b-4*a))/2)*-2*dx+dy*dy+dx*dx-c.rad*c.rad+rad*rad)/2*dy));
                    }
                    else if (Math.pow(b, 2)-4*a == 0) {
                        ret.add(new Coord((-b+(float) Math.sqrt(b*b-4*a))/2, (((-b+(float) Math.sqrt(b*b-4*a))/2)*-2*dx+dy*dy+dx*dx-c.rad*c.rad+rad*rad)/2*dy));
                    }
                }
                else {
                    dy = dx;
                    dx = 0;
                    float a = -((float) Math.pow((dy*dy-c.rad*c.rad+rad*rad) / (2*dy), 2)-rad*rad);
                    if (a > 0) {
                        ret.add(new Coord((dy*dy-c.rad*c.rad+rad*rad)/2*dy, (float) Math.sqrt(a)));
                        ret.add(new Coord(ret.get(0).x, -ret.get(0).y));
                    }
                    else if (a == 0) {
                        ret.add(new Coord((dy*dy-c.rad*c.rad+rad*rad)/2*dy, (float) Math.sqrt(a)));
                    }
                }
                return ret;
            }
    
            public ArrayList<Coord> getEdgeContacts(RectCollider c) {
                ArrayList<Coord> ret = new ArrayList<Coord>();
                System.out.println("to be implemented");
                return ret;
            }
  
        }
  
        public class Group extends Script {
            public HashSet<Script> items = new HashSet<Script>();
        }

        //public class PhysicsBody {}
      
    }
  
}
