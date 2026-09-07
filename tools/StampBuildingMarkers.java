import java.nio.file.*;
import java.util.*;

public class StampBuildingMarkers {
    static final Path DIR = BuildCityTemplates.DIR;
    static final String MARKER = "buried_age:building_marker";
    static final String OLD_CELLA = "buried_age:cella_marker";
    static final String CELLA_ID = "greek/temple_cella";
    static final int CELLA_RADIUS = 2;
    static final Set<String> RUBBLE = Set.of("minecraft:gravel", "minecraft:dirt", "minecraft:coarse_dirt");

    record Job(String file, String building) {}

    static final List<Job> JOBS = List.of(
            new Job("temple.nbt", "greek/temple"),
            new Job("agora.nbt", "greek/agora"),
            new Job("theatre.nbt", "greek/theatre"),
            new Job("villa.nbt", "greek/villa"),
            new Job("villa_end_rich.nbt", "greek/villa"),
            new Job("house_rich.nbt", "greek/house_rich"),
            new Job("house_medium.nbt", "greek/house_medium"),
            new Job("house_poor_a.nbt", "greek/house_poor"),
            new Job("house_poor_b.nbt", "greek/house_poor"),
            new Job("workshop.nbt", "greek/workshop"));

    static BuildCityTemplates.TComp markerNbt(String building, int radius) {
        LinkedHashMap<String, BuildCityTemplates.Tag> m = new LinkedHashMap<>();
        m.put("id", new BuildCityTemplates.TStr(MARKER));
        m.put("building", new BuildCityTemplates.TStr(building));
        m.put("radius", new BuildCityTemplates.TInt(radius));
        return new BuildCityTemplates.TComp(m);
    }

    static String buildingOf(BuildCityTemplates.Tag block) {
        return BuildCityTemplates.str(BuildCityTemplates.get(block, "nbt", "building"));
    }

    public static void main(String[] args) throws Exception {
        for (Job job : JOBS) {
            Path path = DIR.resolve(job.file());
            if (!Files.exists(path)) {
                System.out.println("  " + job.file() + "  missing, skipped");
                continue;
            }

            BuildCityTemplates.Tpl t = new BuildCityTemplates.Tpl(BuildCityTemplates.load(path));
            boolean changed = false;

            int cella = t.palette.indexOf(OLD_CELLA);
            if (cella >= 0) {
                ((BuildCityTemplates.TComp) t.paletteEntries.get(cella)).v.put("Name", new BuildCityTemplates.TStr(MARKER));
                t.palette.set(cella, MARKER);
                int converted = 0;
                for (BuildCityTemplates.Tag b : t.blocks) {
                    if (BuildCityTemplates.num(BuildCityTemplates.get(b, "state")) == cella) {
                        ((BuildCityTemplates.TComp) b).v.put("nbt", markerNbt(CELLA_ID, CELLA_RADIUS));
                        converted++;
                    }
                }
                System.out.println("  " + job.file() + "  converted " + converted + " cella markers -> " + CELLA_ID);
                changed = true;
            }

            boolean present = false;
            for (BuildCityTemplates.Tag b : t.blocks) {
                if (MARKER.equals(t.palette.get(BuildCityTemplates.num(BuildCityTemplates.get(b, "state"))))
                        && job.building().equals(buildingOf(b))) {
                    present = true;
                    break;
                }
            }

            if (!present) {
                double cx = (t.sx - 1) / 2.0, cy = Math.min(t.sy - 1, 2), cz = (t.sz - 1) / 2.0;
                BuildCityTemplates.Tag best = null;
                double bestDist = Double.MAX_VALUE;
                for (BuildCityTemplates.Tag b : t.blocks) {
                    String name = t.palette.get(BuildCityTemplates.num(BuildCityTemplates.get(b, "state")));
                    boolean open = "minecraft:air".equals(name) || BuildCityTemplates.CAVITY.equals(name);
                    if (!open && !RUBBLE.contains(name)) continue;
                    int[] p = BuildCityTemplates.pos(b);
                    double d = (p[0] - cx) * (p[0] - cx) + (p[2] - cz) * (p[2] - cz) + 4.0 * (p[1] - cy) * (p[1] - cy);
                    if (!open) d += 100000.0;
                    if (d < bestDist) { bestDist = d; best = b; }
                }
                if (best == null) {
                    System.out.println("  " + job.file() + "  no air or rubble block to hold the marker, skipped");
                } else {
                    int radius = Math.max(4, Math.min(14, (Math.max(t.sx, t.sz) + 1) / 2 + 1));
                    int[] p = BuildCityTemplates.pos(best);
                    String bestName = t.palette.get(BuildCityTemplates.num(BuildCityTemplates.get(best, "state")));
                    boolean buried = RUBBLE.contains(bestName);
                    int idx = buried ? t.paletteIndex(MARKER, Map.of("buried", "true")) : t.paletteIndex(MARKER);
                    ((BuildCityTemplates.TComp) best).v.put("state", new BuildCityTemplates.TInt(idx));
                    ((BuildCityTemplates.TComp) best).v.put("nbt", markerNbt(job.building(), radius));
                    System.out.println("  " + job.file() + "  " + job.building() + " at " + p[0] + "," + p[1] + "," + p[2]
                            + " radius " + radius + (buried ? " BURIED in rubble, disguised as gravel" : "") + " (template " + t.sx + "x" + t.sy + "x" + t.sz + ")");
                    changed = true;
                }
            } else {
                boolean converted = false;
                for (BuildCityTemplates.Tag b : t.blocks) {
                    int state = BuildCityTemplates.num(BuildCityTemplates.get(b, "state"));
                    if (!MARKER.equals(t.palette.get(state)) || !job.building().equals(buildingOf(b))) continue;
                    if (BuildCityTemplates.get(t.paletteEntries.get(state), "Properties", "buried") != null) continue;
                    int[] p = BuildCityTemplates.pos(b);
                    int rubble = 0;
                    for (int[] d : new int[][] { {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}, {0,0,1}, {0,0,-1} }) {
                        String n = t.name(p[0] + d[0], p[1] + d[1], p[2] + d[2]);
                        if (n == null || RUBBLE.contains(n) || (!"minecraft:air".equals(n) && !BuildCityTemplates.CAVITY.equals(n))) rubble++;
                    }
                    if (rubble == 6) {
                        ((BuildCityTemplates.TComp) b).v.put("state", new BuildCityTemplates.TInt(t.paletteIndex(MARKER, Map.of("buried", "true"))));
                        converted = true;
                        changed = true;
                    }
                }
                System.out.println("  " + job.file() + "  already carries " + job.building() + (converted ? " (now buried: enclosed in solid blocks)" : ""));
            }

            if (changed) BuildCityTemplates.save(t.root, path);
        }
    }
}
