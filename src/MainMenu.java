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
import java.awt.Polygon;

public class MainMenu extends Engine.Prog {

    public static MainMenu instance;

    public ArrayList<Script> kids = new ArrayList<>();

    public Camera cam;

    public MainMenu() {
        super("Checkers Advanced", 414, 437);
        instance = this;
        new Settings();
        new Duel();
        new Checkers();
        cam = new Camera(0, 0, Screen.instance.getSize().width, Screen.instance.getSize().height);
        new Background();
        new StartButton();
        start();
    }

    public void start() {
        for (int x = 0; x < kids.size(); x++)
            kids.get(x).start();
    }

    public void destroy() {
        for (int x = 0; x < kids.size(); x++)
            kids.get(x).destroy();
    }

    public void StartGame() {
        destroy();
        Checkers.instance.start();
    }

    public class Background extends Script {

        public static Background instance;

        public Background() { MainMenu.instance.kids.add(this); }

        public void start() {
            cam.addToDraw(this);
        }

        public void destroy() {
            cam.removeToDraw(this);
        }

        public void render(Graphics g) {
            g.setColor(Color.gray);
            g.fillRect(0, 0, (int) (Screen.instance.getSize().width + 1), (int) (Screen.instance.getSize().height - 1));
            g.setColor(Color.black);
            g.drawString("Checkers", 15, 20);
            g.setColor(Color.red);
            g.drawString("ADVANCED", 15, 35);
        }
    }

    public class StartButton extends Script {

        RectCollider coll = new RectCollider(new Coord(15, 60), new PCoord(0, 50), 10);

        public enum ColorState {
            rest(new Color(0, 255, 0)),
            hover(new Color(60, 210, 0)),
            press(new Color(60, 210, 0));
            Color c;
            ColorState(Color c) { this.c = c; }
        }

        boolean isSelected = false;
        boolean isPressed = false;

        public StartButton() {
            MainMenu.instance.kids.add(this);
        }

        public void start() {
            cam.addToDraw(this);
            addToUpdate(this);
            MouseDetect.instance.addP(this);
            MouseDetect.instance.addR(this);
        }

        public void destroy() {
            cam.removeToDraw(this);
            removeToUpdate(this);
            MouseDetect.instance.removeP(this);
            MouseDetect.instance.removeR(this);
        }

        public void update() {
            isSelected = coll.isColliding(new Collider(MouseDetect.instance.mousePos()));
            if (!isSelected) isPressed = false;
        }

        public void mousePressed(MouseEvent e) {
            if (isSelected) {
                isPressed = true;
                StartGame();
            }
        }
        public void mouseReleased(MouseEvent e) { if (isPressed) {  } isPressed = false; }

        public void render(Graphics g) {
            if (isSelected && isPressed)
                g.setColor(ColorState.press.c);
            else if (isSelected)
                g.setColor(ColorState.hover.c);
            else
                g.setColor(ColorState.rest.c);
            g.fillRoundRect((int) coll.og.x, (int) (coll.og.y - coll.width), (int) coll.length.d, (int) coll.width * 2, 5, 5);
            g.setColor(Color.black);
            g.drawString("Start", (int) coll.og.x + 12, (int) (coll.og.y - coll.width) + 14);
        }
    }


    public class Checkers {

        public boolean canMove = true;

        public Mode mode = Mode.checkers;

        public Turn turn;

        public Camera cam;
        public Camera wCam;

        public static Checkers instance;

        public ArrayList<Script> kids = new ArrayList<Script>();

        public Piece[][] board = new Piece[8][8];

        public ArrayList<Piece> pieces = new ArrayList<Piece>();

        public Checkers() {
            instance = this;
            cam = new Camera(0, 0, Screen.instance.getSize().width, Screen.instance.getSize().height);
            wCam = new Camera(0, 0, Screen.instance.getSize().width, Screen.instance.getSize().height);
            new WinnerText();
        }

        public void start() {
            canMove = true;
            mode = Mode.checkers;
            new Board();
            turn = Turn.p1;
            for (int x = 0; x < 24; x++) {
                if ((x - (int) (x / 8)) % 2 == 0)
                    board[x%8][x/8] = new Piece(PieceType.p1, new Coord(x%8, (int) x/8));
                else
                    board[x%8][x/8] = null;
            }
            for (int x = 63; x > 39; x--) {
                if ((x - (int) (x / 8)) % 2 == 0)
                    board[x%8][x/8] = new Piece(PieceType.p2, new Coord(x%8, (int) x/8));
                else
                    board[x%8][x/8] = null;
            }
            for (int x = 0; x < kids.size(); x++) {
                kids.get(x).start();
            }
        }

        public void destroy() {
            hide();
            MainMenu.instance.start();
        }

        public void show() {
            for (int x = 0; x < kids.size(); x++) {
                kids.get(x).start();
            }
        }

        public void hide() {
            for (int x = 0; x < kids.size(); x++) {
                if (kids.get(x).sim)
                    kids.get(x).destroy();
            }
        }

        public void checkEndGame() {
            System.out.println(pieces.size());
            if (pieces.isEmpty()) {
                endGame(Turn.none);
                return;
            }
            boolean p1W;
            if (pieces.get(0).type.isP1) p1W = true;
            else p1W = false;
            for (int x = 1; x < pieces.size(); x++) {
                if (pieces.get(x).type.isP1 != p1W) return;
            }
            endGame(p1W ? Turn.p1 : Turn.p2);
        }

        public void endGame(Turn winner) {
            canMove = false;
            WinnerText.instance.start(winner);
        }

        public class Piece extends Script {

            public PieceType type;

            public boolean isHeld = false;
            public boolean isHighlighted = false;

            public static Piece heldPiece = null;

            public Coord pos = new Coord();
            public Coord bPos = new Coord();

            public CircleCollider coll = new CircleCollider(pos, 20);

            public float attackCool = 0;
            public float shieldCharge = 4;
            public float feignCool = 0;

            public float attackCoolMax = 4;
            public float shieldChargeMax = 4;
            public float feignCoolMax = 2;
            public Coord kb = null;

            public boolean isAttacking = false;
            public boolean isShielding = false;
            public boolean isFeigning = false;
            public float kbdTime = 0;

            public int health = 2;

            public float spd = 5;

            public Sword sword;
            public Shield shield;

            public int iFrames = 0;

            public boolean render = true;

            public Piece(PieceType t, Coord bp) {
                type = t;
                Checkers.instance.pieces.add(this);
                bPos.x = bp.x();
                bPos.y = bp.y();
                pos.x = bp.x() * 50 + 25;
                pos.y = bp.y() * 50 + 25;
                kids.add(this);
                sword = new Sword(this);
                shield = new Shield(this);
            }

            ////////////////////////////////////////////////////////////////////////
            public void attemptMove(Coord to) {
                if (to.x <= 7 && to.x >= 0 && to.y <= 7 && to.y >= 0) {
                    if (board[to.x()][to.y()] == null) {
                        int dx = bPos.x() - to.x();
                        int dy = bPos.y() - to.y();
                        if (type.isP1) {
                            if (Math.abs(dx) == 1 && ((dy == -1) || (dy == 1 && type.isK))) {
                                board[to.x()][to.y()] = this;
                                board[bPos.x()][bPos.y()] = null;
                                bPos = to;
                                pos.x = bPos.x() * 50 + 25;
                                pos.y = bPos.y() * 50 + 25;
                                if (bPos.y() == 7)
                                    type = PieceType.p1k;
                                turn = Turn.p2;
                                return;
                            }
                            if (Math.abs(dx) == 2 && ((dy == -2) || (dy == 2 && type.isK))) {
                                if (board[bPos.x() - (int) dx/2][bPos.y() - (int) dy/2] != null) {
                                    if (!board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2].type.isP1) {
                                        /*board[to.x()][to.y()] = this;
                                        board[bPos.x()][bPos.y()] = null;
                                        board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2].destroy();
                                        kids.remove(board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2]);
                                        pieces.remove(board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2]);
                                        board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2] = null;
                                        bPos = to;
                                        pos.x = bPos.x() * 50 + 25;
                                        pos.y = bPos.y() * 50 + 25;
                                        if (bPos.y() == 7)
                                            type = PieceType.p1k;
                                        turn = Turn.p2;*/
                                        sim = false;
                                        turn = Turn.p2;
                                        board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2].sim = false;
                                        mode = Mode.swordfight;
                                        Checkers.instance.hide();
                                        Duel.instance.start(this, board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2], to);
                                        sim = true;
                                        board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2].sim = true;
                                        return;
                                    }
                                }
                            }
                        }
                        else {
                            if (Math.abs(dx) == 1 && ((dy == 1) || (dy == -1 && type.isK))) {
                                board[to.x()][to.y()] = this;
                                board[bPos.x()][bPos.y()] = null;
                                bPos = to;
                                pos.x = bPos.x() * 50 + 25;
                                pos.y = bPos.y() * 50 + 25;
                                if (bPos.y() == 0)
                                    type = PieceType.p2k;
                                turn = Turn.p1;
                                return;
                            }
                            if (Math.abs(dx) == 2 && ((dy == 2) || (dy == -2 && type.isK))) {
                                if (board[bPos.x() - (int) dx/2][bPos.y() - (int) dy/2] != null) {
                                    if (board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2].type.isP1) {
                                        /*board[to.x()][to.y()] = this;
                                        board[bPos.x()][bPos.y()] = null;
                                        board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2].destroy();
                                        kids.remove(board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2]);
                                        pieces.remove(board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2]);
                                        board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2] = null;
                                        bPos = to;
                                        pos.x = bPos.x() * 50 + 25;
                                        pos.y = bPos.y() * 50 + 25;
                                        if (bPos.y() == 0)
                                            type = PieceType.p2k;
                                        turn = Turn.p1;*/
                                        sim = false;
                                        turn = Turn.p1;
                                        board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2].sim = false;
                                        mode = Mode.swordfight;
                                        Checkers.instance.hide();
                                        Duel.instance.start(this, board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2], to);
                                        sim = true;
                                        board[bPos.x() - (int) dx / 2][bPos.y() - (int) dy / 2].sim = true;
                                        return;
                                    }
                                }
                            }
                        }
                    }
                }
                pos.x = bPos.x() * 50 + 25;
                pos.y = bPos.y() * 50 + 25;
            }

            public void succeedMove(Coord to) {
                Checkers.instance.board[bPos.x()][bPos.y()] = null;
                Checkers.instance.board[to.x()][to.y()] = this;
                bPos = to;
                pos.x = bPos.x() * 50 + 25;
                pos.y = bPos.y() * 50 + 25;
                if (type.isP1) {
                    if (bPos.y() == 7)
                        type = PieceType.p1k;
                }
                if (!type.isP1) {
                    if (bPos.y() == 0)
                        type = PieceType.p2k;
                }
                turn = type.isP1 ? Turn.p1 : Turn.p2;
            }

            public void move(float dx, float dy) { if (mode == Mode.swordfight && !isAttacking && !isShielding) { pos.x += dx; pos.y -= dy; } }

            public void moveToward(float x, float y) {
                Coord v = new Coord(x, y);
                v.reduce();
                v.x *= spd;
                v.y *= spd;
                move(-v.x, -v.y);
            }

            public void attack() { if (attackCool == 0) { isAttacking = true; attackCool = -attackCoolMax; } }

            public void feign() { if (feignCool == 0) { isFeigning = true; feignCool = -feignCoolMax; } }

            public void shield() { if (shieldCharge > 0) isShielding = true; }
            public void unshield() { isShielding = false; }

            public void start() {
                render = true;
                cam.addToDraw(this);
                MouseDetect.instance.addP(this);
                MouseDetect.instance.addR(this);
                KeyDetect.instance.addP(this);
                KeyDetect.instance.addH(this);
                KeyDetect.instance.addR(this);
                addToUpdate(this);
            }

            public void destroy() {
                render = false;
                cam.removeToDraw(this);
                MouseDetect.instance.removeP(this);
                MouseDetect.instance.removeR(this);
                KeyDetect.instance.removeP(this);
                KeyDetect.instance.removeH(this);
                KeyDetect.instance.removeR(this);
                removeToUpdate(this);
                sword.destroy();
                shield.destroy();
            }

            public void update() {
                if (!canMove) return;
                if (mode == Mode.swordfight) {
                    if (iFrames > 0)
                        iFrames--;
                    //System.out.println(shieldCharge + ", " + attackCool + " / " + feignCool + " ||| " + isAttacking + " / " + isFeigning);
                    if (isShielding) {
                        shieldCharge -= 1f / frameRate;
                        if (shieldCharge <= 0) {
                            shieldCharge = -2;
                            isShielding = false;
                        }
                    } else if (shieldCharge <= 0) {
                        shieldCharge += 1f / frameRate;
                        if (shieldCharge >= 0) {
                            shieldCharge = 2;
                        }
                    } else if (shieldCharge < shieldChargeMax) {
                        shieldCharge += 1f / frameRate;
                        if (shieldCharge > shieldChargeMax) {
                            shieldCharge = shieldChargeMax;
                        }
                    }
                    if (attackCool < 0) {
                        attackCool += 1f/frameRate;
                        if (isAttacking) {
                            if (attackCool >= -attackCoolMax + 1f) {
                                isAttacking = false;
                            }
                        }
                        if (attackCool >= 0) {
                            attackCool = 0;
                        }
                    }
                    if (feignCool < 0) {
                        feignCool += 1f/frameRate;
                        if (feignCool >= -1f) isFeigning = false;
                        if (feignCool > 0) { feignCool = 0; }
                    }
                    if (kb != null) {
                        kbdTime -= 1f/frameRate;
                        move(kb.x / frameRate, kb.y / frameRate);
                        if (kbdTime <= 0) {
                            kbdTime = 0;
                            kb = null;
                        }
                    }
                }
                else {
                    if (isHeld) {
                        pos.x = MouseDetect.instance.mousePos().x - 20;
                        pos.y = MouseDetect.instance.mousePos().y - 20;
                    } else if (heldPiece == null) {
                        if (coll.isColliding(new Collider(MouseDetect.instance.mousePos()))) {
                            isHighlighted = true;
                        } else
                            isHighlighted = false;
                    }
                }
            }

            public void mousePressed(MouseEvent e) {
                if (!canMove) return;
                if (e.getButton() == 1 && mode.isCheckers && ((turn == Turn.p1 && type.isP1) || (turn == Turn.p2 && !type.isP1))) {
                    if (coll.isColliding(new Collider(MouseDetect.instance.mousePos()))) {
                        isHeld = true;
                        pos.x = MouseDetect.instance.mousePos().x;
                        pos.y = MouseDetect.instance.mousePos().y;
                        heldPiece = this;
                    }
                }
            }

            public void mouseReleased(MouseEvent e) {
                if (!canMove) return;
                if (e.getButton() == 1 && isHeld) {
                    isHeld = false;
                    heldPiece = null;
                    attemptMove(new Coord((int) ((pos.x + 25) / 50), (int) ((pos.y + 25) / 50)));
                }
            }

            public void keyPressed(int e) {
                if (!canMove) return;
                if (type.isP1) {
                    if (e == KeyEvent.VK_Q) shield();
                    if (e == KeyEvent.VK_E) attack();
                    if (e == KeyEvent.VK_F) feign();
                }
                if (!type.isP1) {
                    if (e == KeyEvent.VK_SHIFT) shield();
                    if (e == KeyEvent.VK_SLASH) attack();
                    if (e == KeyEvent.VK_CONTROL) feign();
                }
            }

            public void keyHeld(int e) {
                if (!canMove) return;
                Coord d = new Coord();
                if (type.isP1) {
                    if (e == KeyEvent.VK_W) d.y++;
                    if (e == KeyEvent.VK_S) d.y--;
                    if (e == KeyEvent.VK_A) d.x--;
                    if (e == KeyEvent.VK_D) d.x++;
                }
                if (!type.isP1) {
                    if (e == KeyEvent.VK_UP) d.y++;
                    if (e == KeyEvent.VK_DOWN) d.y--;
                    if (e == KeyEvent.VK_LEFT) d.x--;
                    if (e == KeyEvent.VK_RIGHT) d.x++;
                }
                d.reduce();
                d.x *= spd;
                d.y *= spd;
                move(d.x, d.y);
            }

            public void keyReleased(int e) {
                if (!canMove) return;
                if (type.isP1) {
                    if (e == KeyEvent.VK_Q) unshield();
                }
                if (!type.isP1) {
                    if (e == KeyEvent.VK_SHIFT) unshield();
                }
            }

            public void render(Graphics g) {
                if (!render) return;
                if (mode == Mode.swordfight) {
                    if (isAttacking || isFeigning) {
                        sword.render(g);
                    }
                    if (isShielding) {
                        shield.render(g);
                    }
                    if (type.isP1)
                        g.setColor(Settings.instance.p1Color);
                    else
                        g.setColor(Settings.instance.p2Color);
                    g.fillOval(pos.x(), pos.y(), 40, 40);
                    if (type.isK) {
                        g.setColor(new Color(250, 200, 0));
                        g.drawString("K", pos.x() + 15, pos.y() + 23);
                    }
                }
                else if (isHeld) {
                    g.setColor(Color.green);
                    g.drawRect((int) ((pos.x() + 25) / 50) * 50, (int) ((pos.y() + 25) / 50) * 50, 50, 50);
                    g.drawRect((int) ((pos.x() + 25) / 50) * 50 + 1, (int) ((pos.y() + 25) / 50) * 50 + 1, 48, 48);
                    if (type.isP1)
                        g.setColor(Settings.instance.p1Color);
                    else
                        g.setColor(Settings.instance.p2Color);
                    g.fillOval(pos.x(), pos.y(), 40, 40);
                    if (type.isK) {
                        g.setColor(new Color(250, 200, 0));
                        g.drawString("K", pos.x() + 17, pos.y() + 23);
                    }
                }
                else {
                    if (type.isP1)
                        g.setColor(Settings.instance.p1Color);
                    else
                        g.setColor(Settings.instance.p2Color);
                    g.fillOval(5 + bPos.x() * 50, 5 + bPos.y() * 50, 40, 40);
                    if (type.isK) {
                        g.setColor(new Color(250, 200, 0));
                        g.drawString("K", pos.x() - 3, pos.y() + 3);
                    }
                    if (isHighlighted) {
                        g.setColor(Color.red);
                        g.drawOval(5 + bPos.x() * 50, 5 + bPos.y() * 50, 40, 40);
                        g.drawOval(6 + bPos.x() * 50, 6 + bPos.y() * 50, 38, 38);
                    }
                }
            }

            public class Sword extends Script {
                Piece p;

                public CircleCollider coll;

                public Sword(Piece p) {
                    this.p = p;
                    coll = new CircleCollider(p.pos, 60);
                }

                public void render(Graphics g) {
                    Piece o = Duel.instance.opp(p);
                    if (o != null) {
                        g.setColor(Color.gray);
                        int angle = 180 - (int) (Coord.angleBetween(p.pos, o.pos) * 180 / Math.PI);
                        g.fillArc(p.pos.x() - 40, p.pos.y() - 40, 120, 120, angle - 30, 60);
                    }
                }
            }

            public class Shield extends Script {
                Piece p;

                public Shield(Piece p) {
                    this.p = p;
                }

                public void render(Graphics g) {
                    g.setColor(Color.cyan);
                    g.fillOval(p.pos.x() - 5, p.pos.y() - 5, 50, 50);
                }
            }
        }

        public class Board extends Script {

            public Board() {
                kids.add(this);
            }

            public void start() {
                cam.addToDraw(this);
            }

            public void destroy() {
                cam.removeToDraw(this);
            }

            public void render(Graphics g) {
                g.setColor(Settings.instance.t1Color);
                g.fillRect(0, 0, 400, 400);
                g.setColor(Settings.instance.t2Color);
                for (int x = 1; x <= 63; x++) {
                    if ((x + (int) (x / 8)) % 2 == 1)
                        g.fillRect((x % 8) * 50, (int) (x / 8) * 50, 50, 50);
                }
            }
        }

        public class WinnerText extends Script {
            public float timer;

            public static WinnerText instance;

            public Turn winner;

            public WinnerText() {
                instance = this;
            }

            public void start(Turn w) {
                winner = w;
                timer = 10;
                addToUpdate(this);
                wCam.addToDraw(this);
            }

            public void destroy() {
                removeToUpdate(this);
                wCam.removeToDraw(this);
            }

            public void update() {
                timer -= 1f/frameRate;
                if (timer <= 0) {
                    destroy();
                    Checkers.instance.destroy();
                    MainMenu.instance.start();
                }
            }

            public void render(Graphics g) {
                g.setColor(Color.white);
                g.fillRect(0, 0, 300, 30);
                g.setColor(Color.black);
                g.drawRect(0, 0, 300, 30);
                if (winner == Turn.none)
                    g.drawString("A Tie!", 10, 15);
                if (winner == Turn.p1)
                    g.drawString("Player One Wins!", 10, 15);
                if (winner == Turn.p2)
                    g.drawString("Player Two Wins", 10, 15);
            }
        }
    }


    public class Duel extends Script{


        public Camera cam;

        public static Duel instance;

        public ArrayList<Script> kids = new ArrayList<Script>();

        public Checkers.Piece challenger;
        public Checkers.Piece defender;

        public Coord attemptedMove;

        public float timeActive = 0;

        public Coord boundMin = new Coord();
        public Coord boundMax = new Coord();

        public Duel() {
            instance = this;
            cam = new Camera(0, 0, Screen.instance.getSize().width, Screen.instance.getSize().height);
            new Background();
        }

        public Checkers.Piece opp(Checkers.Piece p) {
            if (p == challenger)
                return defender;
            if (p == defender)
                return challenger;
            return null;
        }

        public Checkers.Piece p1Piece() {
            if (challenger != null)
                if (challenger.type.isP1)
                    return challenger;
            if (defender != null)
                if (defender.type.isP1)
                    return defender;
            return null;
        }

        public Checkers.Piece p2Piece() {
            if (challenger != null)
                if (!challenger.type.isP1)
                    return challenger;
            if (defender != null)
                if (!defender.type.isP1)
                    return defender;
            return null;
        }

        public void start(Checkers.Piece c, Checkers.Piece d, Coord move) {
            boundMin.x = cam.x;
            boundMin.y = cam.y;
            boundMax.x = cam.w;
            boundMax.y = cam.h;
            timeActive = 0;
            challenger = c;
            defender = d;
            attemptedMove = move;
            if (challenger.health < 3) {
                challenger.health++;
            }
            for (Script s : kids) {
                s.start();
            }
            c.start();
            d.start();
            addToUpdate(this);
        }

        public void destroy() {
            removeToUpdate(this);
            for (Script s : kids) {
                s.destroy();
            }
        }

        public void update() {
            timeActive += 1f / frameRate;
            if (timeActive >= 60) {
                boundMin.x += .2f;
                boundMin.y += .2f;
                boundMax.x -= .4f;
                boundMax.y -= .4f;
            }
            if (challenger.isAttacking) {
                if (challenger.sword.coll.isColliding(defender.coll)) {
                    if (defender.isShielding) {
                        defender.shieldCharge -= 1.5f;
                        if (defender.shieldCharge < 0) defender.shieldCharge -= defender.shieldChargeMax;
                        defender.kb = new Coord(defender.pos.x - challenger.pos.x, defender.pos.y - challenger.pos.y);
                        defender.kb.reduce();
                        if (timeActive > 30)
                            defender.kb.x *= 15 + (float) Math.pow(timeActive / 2 - 15, 2);
                        else
                            defender.kb.x *= 15;
                        defender.iFrames = 30;
                    }
                    else {
                        defender.kb = new Coord(defender.pos.x - challenger.pos.x, defender.pos.y - challenger.pos.y);
                        defender.kb.reduce();
                        if (timeActive > 30)
                            defender.kb.x *= 30 + (float) Math.pow(timeActive - 30, 2);
                        else
                            defender.kb.x *= 30;
                        if (defender.iFrames == 0)
                            defender.health--;
                        defender.iFrames = 30;
                    }
                }
            }
            if (defender.isAttacking) {
                if (defender.sword.coll.isColliding(challenger.coll)) {
                    if (challenger.isShielding) {
                        challenger.shieldCharge -= 1.5f;
                        if (challenger.shieldCharge < 0) challenger.shieldCharge -= challenger.shieldChargeMax;
                        challenger.kb = new Coord(challenger.pos.x - defender.pos.x, challenger.pos.y - defender.pos.y);
                        challenger.kb.reduce();
                        if (timeActive > 30)
                            challenger.kb.x *= 15 + (float) Math.pow(timeActive / 2 - 15, 2);
                        else
                            challenger.kb.x *= 15;
                        challenger.iFrames = 30;
                    }
                    else {
                        challenger.kb = new Coord(challenger.pos.x - defender.pos.x, challenger.pos.y - defender.pos.y);
                        challenger.kb.reduce();
                        if (timeActive > 30)
                            challenger.kb.x *= 30 + (float) Math.pow(timeActive - 30, 2);
                        else
                            challenger.kb.x *= 15;
                        if (challenger.iFrames == 0)
                            challenger.health--;
                        challenger.iFrames = 30;
                    }
                }
            }
            if (challenger.pos.x < boundMin.x - 20 || challenger.pos.x > boundMax.x + boundMin.x || challenger.pos.y < boundMin.y || challenger.pos.y > boundMax.y + boundMin.y) challenger.health = 0;
            if (defender.pos.x < boundMin.x - 20 || defender.pos.x > boundMax.x + boundMin.x || defender.pos.y < boundMin.y || defender.pos.y > boundMax.y + boundMin.y) defender.health = 0;
            if (challenger.health <= 0 || defender.health <= 0) { end(); }
        }

        public void end() {
            destroy();
            challenger.iFrames = 0;
            challenger.isFeigning = false;
            challenger.isAttacking = false;
            challenger.isShielding = false;
            challenger.feignCool = 0;
            challenger.attackCool = 0;
            defender.iFrames = 0;
            defender.isFeigning = false;
            defender.isAttacking = false;
            defender.isShielding = false;
            defender.feignCool = 0;
            defender.attackCool = 0;
            challenger.pos.x = challenger.bPos.x() * 50 + 25;
            challenger.pos.y = challenger.bPos.y() * 50 + 25;
            defender.pos.x = defender.bPos.x() * 50 + 25;
            defender.pos.y = defender.bPos.y() * 50 + 25;
            defender.shieldCharge = defender.shieldChargeMax;
            kids.remove(challenger);
            kids.remove(defender);
            cam.removeToDraw(challenger);
            cam.removeToDraw(defender);
            Checkers.instance.cam.removeToDraw(challenger);
            Checkers.instance.cam.removeToDraw(defender);
            if (challenger.health <= 0) {
                Checkers.instance.kids.remove(Checkers.instance.board[challenger.bPos.x()][challenger.bPos.y()]);
                Checkers.instance.pieces.remove(Checkers.instance.board[challenger.bPos.x()][challenger.bPos.y()]);
                Checkers.instance.board[challenger.bPos.x()][challenger.bPos.y()] = null;
                challenger.destroy();
                challenger.render = false;
            }
            else {
                challenger.succeedMove(attemptedMove);
            }
            if (defender.health <= 0) {
                Checkers.instance.kids.remove(Checkers.instance.board[defender.bPos.x()][defender.bPos.y()]);
                Checkers.instance.pieces.remove(Checkers.instance.board[defender.bPos.x()][defender.bPos.y()]);
                Checkers.instance.board[defender.bPos.x()][defender.bPos.y()] = null;
                defender.destroy();
                defender.render = false;
            }
            Checkers.instance.mode = Mode.checkers;
            Checkers.instance.checkEndGame();
            Checkers.instance.show();
        }

        public class Background extends Script {

            public Background() {
                kids.add(this);
            }

            public void start() {
                cam.addToDraw(this);
            }

            public void destroy() {
                cam.removeToDraw(this);
            }

            public void render(Graphics g) {
                g.setColor(Color.red);
                g.fillRect(0, 0, Screen.instance.getSize().width, Screen.instance.getSize().height);
                g.setColor(Color.white);
                g.fillRect(boundMin.x(), boundMin.y(), boundMax.x(), boundMax.y());
            }
        }

    }

}
