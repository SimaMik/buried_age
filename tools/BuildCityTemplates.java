import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class BuildCityTemplates {

    static final Path DIR = Path.of("src/main/resources/data/buried_age/structure/city");
    static final String CAVITY = "buried_age:cavity_marker";
    static final Set<String> KEEP = Set.of("minecraft:jigsaw", "minecraft:chest", "minecraft:decorated_pot",
            "buried_age:hephaestus_forge", "buried_age:cella_marker");

    sealed interface Tag permits TByte, TShort, TInt, TLong, TFloat, TDouble, TBytes, TStr, TList, TComp, TInts, TLongs {}
    record TByte(byte v) implements Tag {}
    record TShort(short v) implements Tag {}
    record TInt(int v) implements Tag {}
    record TLong(long v) implements Tag {}
    record TFloat(float v) implements Tag {}
    record TDouble(double v) implements Tag {}
    record TBytes(byte[] v) implements Tag {}
    record TStr(String v) implements Tag {}
    static final class TList implements Tag { int elem; List<Tag> v; TList(int e, List<Tag> l) { elem = e; v = l; } }
    static final class TComp implements Tag { LinkedHashMap<String, Tag> v; TComp(LinkedHashMap<String, Tag> m) { v = m; } }
    record TInts(int[] v) implements Tag {}
    record TLongs(long[] v) implements Tag {}

    static int typeOf(Tag t) {
        if (t instanceof TByte) return 1;
        if (t instanceof TShort) return 2;
        if (t instanceof TInt) return 3;
        if (t instanceof TLong) return 4;
        if (t instanceof TFloat) return 5;
        if (t instanceof TDouble) return 6;
        if (t instanceof TBytes) return 7;
        if (t instanceof TStr) return 8;
        if (t instanceof TList) return 9;
        if (t instanceof TComp) return 10;
        if (t instanceof TInts) return 11;
        return 12;
    }

    static Tag read(DataInput in, int type) throws IOException {
        switch (type) {
            case 1: return new TByte(in.readByte());
            case 2: return new TShort(in.readShort());
            case 3: return new TInt(in.readInt());
            case 4: return new TLong(in.readLong());
            case 5: return new TFloat(in.readFloat());
            case 6: return new TDouble(in.readDouble());
            case 7: { int n = in.readInt(); byte[] b = new byte[n]; in.readFully(b); return new TBytes(b); }
            case 8: return new TStr(in.readUTF());
            case 9: {
                int t = in.readUnsignedByte();
                int n = in.readInt();
                List<Tag> l = new ArrayList<>();
                for (int i = 0; i < n; i++) l.add(read(in, t));
                return new TList(t, l);
            }
            case 10: {
                LinkedHashMap<String, Tag> m = new LinkedHashMap<>();
                while (true) {
                    int t = in.readUnsignedByte();
                    if (t == 0) break;
                    m.put(in.readUTF(), read(in, t));
                }
                return new TComp(m);
            }
            case 11: { int n = in.readInt(); int[] x = new int[n]; for (int i = 0; i < n; i++) x[i] = in.readInt(); return new TInts(x); }
            case 12: { int n = in.readInt(); long[] x = new long[n]; for (int i = 0; i < n; i++) x[i] = in.readLong(); return new TLongs(x); }
            default: throw new IOException("bad tag " + type);
        }
    }

    static void write(DataOutput out, Tag t) throws IOException {
        if (t instanceof TByte x) out.writeByte(x.v());
        else if (t instanceof TShort x) out.writeShort(x.v());
        else if (t instanceof TInt x) out.writeInt(x.v());
        else if (t instanceof TLong x) out.writeLong(x.v());
        else if (t instanceof TFloat x) out.writeFloat(x.v());
        else if (t instanceof TDouble x) out.writeDouble(x.v());
        else if (t instanceof TBytes x) { out.writeInt(x.v().length); out.write(x.v()); }
        else if (t instanceof TStr x) out.writeUTF(x.v());
        else if (t instanceof TList x) {
            int et = x.v.isEmpty() ? x.elem : typeOf(x.v.get(0));
            out.writeByte(et);
            out.writeInt(x.v.size());
            for (Tag e : x.v) write(out, e);
        } else if (t instanceof TComp x) {
            for (Map.Entry<String, Tag> e : x.v.entrySet()) {
                out.writeByte(typeOf(e.getValue()));
                out.writeUTF(e.getKey());
                write(out, e.getValue());
            }
            out.writeByte(0);
        } else if (t instanceof TInts x) { out.writeInt(x.v().length); for (int i : x.v()) out.writeInt(i); }
        else if (t instanceof TLongs x) { out.writeInt(x.v().length); for (long l : x.v()) out.writeLong(l); }
    }

    static TComp load(Path p) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(p))))) {
            int t = in.readUnsignedByte();
            in.readUTF();
            return (TComp) read(in, t);
        }
    }

    static void save(TComp root, Path p) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(Files.newOutputStream(p))))) {
            out.writeByte(10);
            out.writeUTF("");
            write(out, root);
        }
    }

    static Tag get(Tag t, String... path) {
        for (String k : path) {
            if (!(t instanceof TComp c)) return null;
            t = c.v.get(k);
            if (t == null) return null;
        }
        return t;
    }

    static String str(Tag t) { return t instanceof TStr s ? s.v() : null; }

    static int num(Tag t) {
        if (t instanceof TInt x) return x.v();
        if (t instanceof TByte b) return b.v();
        if (t instanceof TShort s) return s.v();
        return Integer.MIN_VALUE;
    }

    static int[] pos(Tag b) {
        TList l = (TList) get(b, "pos");
        return new int[] { num(l.v.get(0)), num(l.v.get(1)), num(l.v.get(2)) };
    }

    static final class Tpl {
        TComp root;
        int sx, sy, sz;
        List<String> palette = new ArrayList<>();
        List<Tag> paletteEntries;
        List<Tag> blocks;
        Map<Long, Tag> byPos = new HashMap<>();

        Tpl(TComp root) {
            this.root = root;
            TList size = (TList) get(root, "size");
            sx = num(size.v.get(0));
            sy = num(size.v.get(1));
            sz = num(size.v.get(2));
            paletteEntries = ((TList) get(root, "palette")).v;
            for (Tag e : paletteEntries) palette.add(str(get(e, "Name")));
            blocks = ((TList) get(root, "blocks")).v;
            for (Tag b : blocks) {
                int[] p = pos(b);
                byPos.put(key(p[0], p[1], p[2]), b);
            }
        }

        static long key(int x, int y, int z) { return ((long) x << 40) | ((long) y << 20) | (z & 0xFFFFFL); }

        String name(int x, int y, int z) {
            Tag b = byPos.get(key(x, y, z));
            return b == null ? null : palette.get(num(get(b, "state")));
        }

        boolean inside(int x, int y, int z) { return x >= 0 && y >= 0 && z >= 0 && x < sx && y < sy && z < sz; }

        int paletteIndex(String blockName) {
            return paletteIndex(blockName, Map.of());
        }

        int paletteIndex(String blockName, Map<String, String> props) {
            for (int i = 0; i < palette.size(); i++) {
                if (!blockName.equals(palette.get(i))) continue;
                Tag existing = get(paletteEntries.get(i), "Properties");
                if (props.isEmpty() && existing == null) return i;
                if (!props.isEmpty() && existing instanceof TComp c && c.v.size() == props.size()) {
                    boolean same = true;
                    for (Map.Entry<String, String> e : props.entrySet())
                        if (!e.getValue().equals(str(c.v.get(e.getKey())))) { same = false; break; }
                    if (same) return i;
                }
            }

            LinkedHashMap<String, Tag> m = new LinkedHashMap<>();
            m.put("Name", new TStr(blockName));
            if (!props.isEmpty()) {
                LinkedHashMap<String, Tag> pm = new LinkedHashMap<>();
                for (Map.Entry<String, String> e : props.entrySet()) pm.put(e.getKey(), new TStr(e.getValue()));
                m.put("Properties", new TComp(pm));
            }

            paletteEntries.add(new TComp(m));
            palette.add(blockName);
            return palette.size() - 1;
        }

        Tag blockAt(int x, int y, int z) { return byPos.get(key(x, y, z)); }

        int stateAt(int x, int y, int z) {
            Tag b = byPos.get(key(x, y, z));
            return b == null ? -1 : num(get(b, "state"));
        }

        void set(int x, int y, int z, String blockName) {
            setState(x, y, z, paletteIndex(blockName));
        }

        void setState(int x, int y, int z, int idx) {
            Tag b = byPos.get(key(x, y, z));
            if (b != null) {
                ((TComp) b).v.put("state", new TInt(idx));
                ((TComp) b).v.remove("nbt");
                return;
            }
            LinkedHashMap<String, Tag> m = new LinkedHashMap<>();
            List<Tag> pl = new ArrayList<>();
            pl.add(new TInt(x));
            pl.add(new TInt(y));
            pl.add(new TInt(z));
            m.put("pos", new TList(3, pl));
            m.put("state", new TInt(idx));
            TComp nb = new TComp(m);
            blocks.add(nb);
            byPos.put(key(x, y, z), nb);
        }

        List<Tag> jigsaws() {
            List<Tag> out = new ArrayList<>();
            for (Tag b : blocks) if ("minecraft:jigsaw".equals(palette.get(num(get(b, "state"))))) out.add(b);
            return out;
        }

        String orientationOf(Tag jig) {
            return str(get(paletteEntries.get(num(get(jig, "state"))), "Properties", "orientation"));
        }

        void resize(int newSy) {
            sy = newSy;
            TList size = (TList) get(root, "size");
            size.v.set(1, new TInt(newSy));
        }
    }

    static int[] frontOf(String orientation) {
        String front = orientation.substring(0, orientation.indexOf('_'));
        switch (front) {
            case "north": return new int[] { 0, 0, -1 };
            case "south": return new int[] { 0, 0, 1 };
            case "west": return new int[] { -1, 0, 0 };
            case "east": return new int[] { 1, 0, 0 };
            case "up": return new int[] { 0, 1, 0 };
            case "down": return new int[] { 0, -1, 0 };
            default: throw new IllegalStateException(orientation);
        }
    }

    /**
     * Some buildings were saved with their ground floor one block lower than the streets: their
     * room already starts at local y=0, so the player steps down out of the road. Lift the whole
     * template one block, keeping the jigsaw at y=1, and grow a real floor underneath.
     */
    static void raiseIfLow(String file) throws IOException {
        Path p = DIR.resolve(file);
        Tpl t = new Tpl(load(p));

        int cavity0 = 0;
        int cavity1 = 0;
        for (int x = 0; x < t.sx; x++) for (int z = 0; z < t.sz; z++) {
            if (CAVITY.equals(t.name(x, 0, z))) cavity0++;
            if (t.inside(x, 1, z) && CAVITY.equals(t.name(x, 1, z))) cavity1++;
        }

        if (cavity1 == 0 || cavity0 * 100 < cavity1 * 40) {
            System.out.printf("  %-18s floor already level (cavity y0=%d y1=%d), left alone%n", file, cavity0, cavity1);
            return;
        }

        String floor = floorMaterial(t);
        List<Tag> jigsaws = t.jigsaws();
        Set<Long> jigsawCells = new HashSet<>();
        for (Tag j : jigsaws) {
            int[] jp = pos(j);
            jigsawCells.add(Tpl.key(jp[0], jp[1], jp[2]));
        }

        int[][][] oldState = new int[t.sx][t.sy][t.sz];
        for (int x = 0; x < t.sx; x++) for (int y = 0; y < t.sy; y++) for (int z = 0; z < t.sz; z++)
            oldState[x][y][z] = t.stateAt(x, y, z);

        List<Tag> keptJigsaws = new ArrayList<>(jigsaws);
        t.blocks.clear();
        t.byPos.clear();
        t.resize(t.sy + 1);

        for (Tag j : keptJigsaws) {
            int[] jp = pos(j);
            t.blocks.add(j);
            t.byPos.put(Tpl.key(jp[0], jp[1], jp[2]), j);
        }

        int moved = 0;
        for (int x = 0; x < t.sx; x++) for (int y = t.sy - 2; y >= 0; y--) for (int z = 0; z < t.sz; z++) {
            int state = oldState[x][y][z];
            if (state < 0 || jigsawCells.contains(Tpl.key(x, y, z))) continue;
            if (jigsawCells.contains(Tpl.key(x, y + 1, z))) continue;
            t.setState(x, y + 1, z, state);
            moved++;
        }

        int filled = 0;
        for (int x = 0; x < t.sx; x++) for (int z = 0; z < t.sz; z++) {
            int above = t.stateAt(x, 1, z);
            String aboveName = above < 0 ? null : t.palette.get(above);
            if (aboveName == null) continue;
            if (CAVITY.equals(aboveName) || KEEP.contains(aboveName)) {
                t.set(x, 0, z, floor);
            } else {
                t.setState(x, 0, z, above);
            }

            filled++;
        }

        save(t.root, p);
        System.out.printf("  %-18s raised one block (cavity y0=%d y1=%d), %d blocks moved, %d floor cells, floor %s%n",
                file, cavity0, cavity1, moved, filled, floor.replace("minecraft:", ""));
    }

    static String floorMaterial(Tpl t) {
        Map<String, Integer> count = new HashMap<>();
        for (int x = 0; x < t.sx; x++) for (int z = 0; z < t.sz; z++) {
            String n = t.name(x, 0, z);
            if (n == null || CAVITY.equals(n) || KEEP.contains(n)) continue;
            if (n.equals("minecraft:gravel") || n.equals("minecraft:dirt") || n.equals("minecraft:coarse_dirt")) continue;
            if (n.contains("_slab") || n.contains("_stairs") || n.contains("_wall") || n.contains("_fence")) continue;
            count.merge(n, 1, Integer::sum);
        }

        return count.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse("minecraft:stone_bricks");
    }

    /** Move a jigsaw connector to another face of the template, keeping its pool data. */
    static void moveJigsaw(String file, int fx, int fy, int fz, int tx, int ty, int tz, String orientation)
            throws IOException {
        Path p = DIR.resolve(file);
        Tpl t = new Tpl(load(p));
        Tag from = t.blockAt(fx, fy, fz);
        if (from == null || !"minecraft:jigsaw".equals(t.palette.get(num(get(from, "state"))))) {
            System.out.printf("  %-18s no jigsaw at %d,%d,%d, already moved%n", file, fx, fy, fz);
            return;
        }

        Tag nbt = get(from, "nbt");
        String fill = t.name(fx, fy + 1, fz);
        if (fill == null || "minecraft:jigsaw".equals(fill)) fill = "minecraft:gravel";
        t.set(fx, fy, fz, fill);

        int idx = t.paletteIndex("minecraft:jigsaw", Map.of("orientation", orientation));
        t.setState(tx, ty, tz, idx);
        ((TComp) t.blockAt(tx, ty, tz)).v.put("nbt", nbt);
        save(t.root, p);
        System.out.printf("  %-18s jigsaw %d,%d,%d -> %d,%d,%d facing %s%n",
                file, fx, fy, fz, tx, ty, tz, orientation);
    }

    /** Point a block that carries a horizontal facing in a fixed direction inside the template. */
    static void faceBlock(String file, String blockName, String facing) throws IOException {
        Path p = DIR.resolve(file);
        Tpl t = new Tpl(load(p));
        int touched = 0;
        for (Tag b : new ArrayList<>(t.blocks)) {
            int state = num(get(b, "state"));
            if (!blockName.equals(t.palette.get(state))) continue;
            Map<String, String> props = new LinkedHashMap<>();
            Tag existing = get(t.paletteEntries.get(state), "Properties");
            if (existing instanceof TComp c) c.v.forEach((k, v) -> props.put(k, str(v)));
            props.put("facing", facing);
            ((TComp) b).v.put("state", new TInt(t.paletteIndex(blockName, props)));
            touched++;
        }

        save(t.root, p);
        System.out.printf("  %-18s %s facing=%s (%d block%s)%n",
                file, blockName.replace("buried_age:", ""), facing, touched, touched == 1 ? "" : "s");
    }

    static void carve(String file) throws IOException {
        Path p = DIR.resolve(file);
        Tpl t = new Tpl(load(p));
        Tag jig = null;
        for (Tag j : t.jigsaws()) {
            String nm = str(get(j, "nbt", "name"));
            if (nm != null && nm.startsWith("buried_age:building")) { jig = j; break; }
        }
        if (jig == null) {
            System.out.println("  " + file + ": no building jigsaw, skipped");
            return;
        }

        int[] jp = pos(jig);
        int[] f = frontOf(t.orientationOf(jig));
        int dx = -f[0];
        int dz = -f[2];
        int changed = 0;
        StringBuilder trace = new StringBuilder();

        // Without a bound a building whose ground floor is completely collapsed, like the rich
        // house, would be tunnelled end to end. Two thirds of the way in is enough to be inside.
        int reach = Math.min(16, (2 * (dx != 0 ? t.sx : t.sz)) / 3);
        int extra = -1;
        for (int step = 1; step <= reach; step++) {
            int x = jp[0] + dx * step;
            int y = jp[1];
            int z = jp[2] + dz * step;
            if (!t.inside(x, y, z)) break;
            String at = t.name(x, y, z);
            boolean open = CAVITY.equals(at) && t.inside(x, y + 1, z) && CAVITY.equals(t.name(x, y + 1, z));
            if (open && extra < 0 && sideOpen(t, x, y, z, dx, dz)) {
                extra = 0;
                trace.append(" <-room");
            }

            for (int up = 0; up <= 1; up++) {
                int yy = y + up;
                if (!t.inside(x, yy, z)) continue;
                String cur = t.name(x, yy, z);
                if (cur != null && KEEP.contains(cur)) continue;
                if (CAVITY.equals(cur)) continue;
                String above = t.inside(x, yy + 1, z) ? t.name(x, yy + 1, z) : null;
                if (above != null && KEEP.contains(above) && !"minecraft:jigsaw".equals(above)) continue;
                t.set(x, yy, z, CAVITY);
                changed++;
            }

            if (extra < 0) {
                trace.append(' ').append(step).append(':').append(at == null ? "void" : at.replace("minecraft:", ""));
            } else if (extra-- == 0) {
                break;
            }
        }

        save(t.root, p);
        System.out.printf("  %-18s jigsaw %d,%d,%d dir %d,%d -> carved %d cells;%s%n",
                file, jp[0], jp[1], jp[2], dx, dz, changed, trace);
    }

    /** A corridor we cut ourselves is one cell wide; a real room is open to at least one side. */
    static boolean sideOpen(Tpl t, int x, int y, int z, int dx, int dz) {
        int lx = dz;
        int lz = dx;
        for (int s : new int[] { 1, -1 }) {
            int nx = x + lx * s;
            int nz = z + lz * s;
            if (t.inside(nx, y, nz) && CAVITY.equals(t.name(nx, y, nz))) return true;
        }

        return false;
    }

    static boolean roomLike(Tpl t, int x, int y, int z, int dx, int dz) {
        int lx = dz;
        int lz = dx;
        for (int s : new int[] { 1, -1 }) {
            String n = t.inside(x + lx * s, y, z + lz * s) ? t.name(x + lx * s, y, z + lz * s) : null;
            if (CAVITY.equals(n)) return true;
        }
        return false;
    }

    record Spec(int x, int y, int z, String name, String target, String pool, int sel) {}

    static void makeCopy(String src, String dst, Spec... specs) throws IOException {
        Tpl t = new Tpl(load(DIR.resolve(src)));
        Set<String> hit = new LinkedHashSet<>();
        for (Tag j : t.jigsaws()) {
            int[] jp = pos(j);
            for (Spec s : specs) {
                if (s.x != jp[0] || s.y != jp[1] || s.z != jp[2]) continue;
                TComp nbt = (TComp) get(j, "nbt");
                if (nbt == null) throw new IOException(src + ": jigsaw without nbt at " + Arrays.toString(jp));
                nbt.v.put("name", new TStr(s.name));
                nbt.v.put("target", new TStr(s.target));
                nbt.v.put("pool", new TStr(s.pool));
                nbt.v.put("joint", new TStr("aligned"));
                nbt.v.put("selection_priority", new TInt(s.sel));
                nbt.v.put("placement_priority", new TInt(0));
                hit.add(jp[0] + "," + jp[1] + "," + jp[2]);
            }
        }
        for (Spec s : specs) {
            String k = s.x + "," + s.y + "," + s.z;
            if (!hit.contains(k)) throw new IOException(src + ": no jigsaw at " + k);
        }
        if (hit.size() != t.jigsaws().size())
            throw new IOException(dst + ": template has " + t.jigsaws().size() + " jigsaws, only " + hit.size() + " specified");
        save(t.root, DIR.resolve(dst));
        System.out.println("  " + dst + "  <- " + src + "  (" + hit.size() + " jigsaws)");
    }

    public static void main(String[] args) throws Exception {
        String[] buildings = { "house_poor_a.nbt", "house_poor_b.nbt", "house_medium.nbt", "house_rich.nbt",
                "workshop.nbt", "theatre.nbt", "villa.nbt", "temple.nbt" };

        System.out.println("== theatre: connector onto the long side, so the rows face the street ==");
        moveJigsaw("theatre.nbt", 7, 1, 0, 0, 1, 9, "west_up");

        System.out.println("== temple: the forge faces the cella entrance ==");
        faceBlock("temple.nbt", "buried_age:hephaestus_forge", "west");

        System.out.println("== lift the buildings whose floor sits below street level ==");
        for (String f : buildings) raiseIfLow(f);

        System.out.println("== carve approaches ==");
        for (String f : buildings) carve(f);

        System.out.println("== district streets ==");
        for (String d : new String[] { "rich", "medium", "poor" }) {
            String street = "buried_age:street_" + d;
            String side = "buried_age:street_" + d + "_side";
            String spool = "buried_age:city/street_" + d;
            String bpool = "buried_age:city/buildings_" + d;

            makeCopy("street_straight_a.nbt", "street_" + d + "_a.nbt",
                    new Spec(0, 1, 2, street, street, spool, 0),
                    new Spec(6, 1, 2, street, street, spool, 0),
                    new Spec(3, 1, 0, side, "buried_age:building", bpool, 10),
                    new Spec(3, 1, 4, side, "buried_age:building", bpool, 10));

            makeCopy("street_straight_b.nbt", "street_" + d + "_b.nbt",
                    new Spec(0, 1, 2, street, street, spool, 0),
                    new Spec(6, 1, 2, street, street, spool, 0),
                    new Spec(3, 1, 0, side, "buried_age:building", bpool, 10),
                    new Spec(3, 1, 4, side, "buried_age:building", bpool, 10));

            makeCopy("street_turn.nbt", "street_" + d + "_turn.nbt",
                    new Spec(0, 1, 2, street, street, spool, 0),
                    new Spec(2, 1, 0, street, street, spool, 0),
                    new Spec(4, 1, 2, side, "buried_age:building", bpool, 10));

            makeCopy("street_crossroads.nbt", "street_" + d + "_cross.nbt",
                    new Spec(0, 1, 2, street, street, spool, 0),
                    new Spec(4, 1, 2, street, street, spool, 0),
                    new Spec(2, 1, 0, side, "buried_age:building", bpool, 10),
                    new Spec(2, 1, 4, side, "buried_age:building", bpool, 10));

            makeCopy("terminator.nbt", "terminator_" + d + ".nbt",
                    new Spec(2, 1, 0, street, "buried_age:empty", "minecraft:empty", 0));
        }


        System.out.println("== villa closing off the rich street ==");
        makeCopy("villa.nbt", "villa_end_rich.nbt",
                new Spec(11, 1, 0, "buried_age:street_rich", "buried_age:empty", "minecraft:empty", 0));

        System.out.println("== temple road ==");
        makeCopy("street_straight_a.nbt", "temple_road.nbt",
                new Spec(0, 1, 2, "buried_age:temple_road", "buried_age:empty", "minecraft:empty", 0),
                new Spec(6, 1, 2, "buried_age:temple_road_end", "buried_age:building", "buried_age:city/temple", 20),
                new Spec(3, 1, 0, "buried_age:temple_road_side", "buried_age:empty", "minecraft:empty", 0),
                new Spec(3, 1, 4, "buried_age:temple_road_side", "buried_age:empty", "minecraft:empty", 0));

        System.out.println("== agora ==");
        makeCopy("agora.nbt", "agora.nbt",
                new Spec(7, 1, 0, "buried_age:agora", "buried_age:temple_road", "buried_age:city/temple_road", 20),
                new Spec(0, 1, 7, "buried_age:agora", "buried_age:street_rich", "buried_age:city/street_rich", 10),
                new Spec(7, 1, 14, "buried_age:agora", "buried_age:street_medium", "buried_age:city/street_medium", 10),
                new Spec(14, 1, 7, "buried_age:agora", "buried_age:street_poor", "buried_age:city/street_poor", 10),
                new Spec(7, 3, 7, "buried_age:agora_hint", "buried_age:surface_hint", "buried_age:city/surface_hints", 0));
        System.out.println("done");
    }
}
