import java.nio.file.*;
import java.util.*;

public class DumpTemplates {
    public static void main(String[] args) throws Exception {
        Path dir = args.length > 0 ? Path.of(args[0]) : BuildCityTemplates.DIR;
        List<Path> files = new ArrayList<>();
        try (var s = Files.list(dir)) { s.filter(p -> p.toString().endsWith(".nbt")).sorted().forEach(files::add); }
        for (Path p : files) {
            BuildCityTemplates.Tpl t = new BuildCityTemplates.Tpl(BuildCityTemplates.load(p));
            StringBuilder sb = new StringBuilder();
            sb.append(p.getFileName()).append("  ").append(t.sx).append("x").append(t.sy).append("x").append(t.sz).append("\n");
            Map<String, Integer> counts = new TreeMap<>();
            int minY = Integer.MAX_VALUE;
            for (BuildCityTemplates.Tag b : t.blocks) {
                String name = t.palette.get(BuildCityTemplates.num(BuildCityTemplates.get(b, "state")));
                counts.merge(name, 1, Integer::sum);
                int[] pos = BuildCityTemplates.pos(b);
                if (!"minecraft:air".equals(name) && !"minecraft:structure_void".equals(name) && !name.endsWith("_marker")) minY = Math.min(minY, pos[1]);
                if (name.contains("marker") || name.equals("minecraft:jigsaw") || name.contains("hephaestus_forge")) {
                    sb.append("   ").append(name).append(" @ ").append(pos[0]).append(",").append(pos[1]).append(",").append(pos[2]);
                    BuildCityTemplates.Tag props = BuildCityTemplates.get(t.paletteEntries.get(BuildCityTemplates.num(BuildCityTemplates.get(b, "state"))), "Properties");
                    if (props instanceof BuildCityTemplates.TComp c) sb.append(" ").append(render(c));
                    BuildCityTemplates.Tag nbt = BuildCityTemplates.get(b, "nbt");
                    if (nbt instanceof BuildCityTemplates.TComp c) {
                        for (String k : List.of("building", "radius", "name", "target", "pool", "orientation")) {
                            BuildCityTemplates.Tag v = c.v.get(k);
                            if (v != null) sb.append(" ").append(k).append("=").append(v instanceof BuildCityTemplates.TStr s ? s.v() : String.valueOf(BuildCityTemplates.num(v)));
                        }
                    }
                    sb.append("\n");
                }
            }
            int loot = 0, items = 0;
            for (BuildCityTemplates.Tag b : t.blocks) {
                if (BuildCityTemplates.get(b, "nbt", "LootTable") != null) loot++;
                if (BuildCityTemplates.get(b, "nbt", "Items") != null || BuildCityTemplates.get(b, "nbt", "target") != null) items++;
            }
            sb.append("   loot-table blocks=").append(loot).append(" item-carrying blocks=").append(items).append('\n');
            sb.append("   lowest solid y=").append(minY).append("; palette:");
            for (String n : t.palette) if (n.contains("buried_age")) sb.append(" ").append(n.replace("buried_age:", ""));
            sb.append("\n   blocks:");
            for (var e : counts.entrySet()) if (!e.getKey().startsWith("minecraft:")) sb.append(" ").append(e.getKey()).append("=").append(e.getValue());
            System.out.println(sb);
        }
    }

    static String render(BuildCityTemplates.TComp c) {
        StringBuilder sb = new StringBuilder("{");
        for (var e : c.v.entrySet()) sb.append(e.getKey()).append("=").append(e.getValue() instanceof BuildCityTemplates.TStr s ? s.v() : "?").append(",");
        return sb.append("}").toString();
    }
}
