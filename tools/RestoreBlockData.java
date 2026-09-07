import java.nio.file.*;
import java.util.*;

public class RestoreBlockData {
    public static void main(String[] args) throws Exception {
        Path oldDir = Path.of(args[0]);
        List<Path> files = new ArrayList<>();
        try (var s = Files.list(BuildCityTemplates.DIR)) { s.filter(p -> p.toString().endsWith(".nbt")).sorted().forEach(files::add); }
        for (Path p : files) {
            Path old = oldDir.resolve(p.getFileName());
            if (!Files.exists(old)) continue;
            BuildCityTemplates.Tpl now = new BuildCityTemplates.Tpl(BuildCityTemplates.load(p));
            BuildCityTemplates.Tpl was = new BuildCityTemplates.Tpl(BuildCityTemplates.load(old));
            if (now.sx != was.sx || now.sz != was.sz || now.sy < was.sy) continue;
            int dy = now.sy - was.sy;
            int restored = 0;
            for (BuildCityTemplates.Tag b : was.blocks) {
                BuildCityTemplates.Tag nbt = BuildCityTemplates.get(b, "nbt");
                if (nbt == null) continue;
                String name = was.palette.get(BuildCityTemplates.num(BuildCityTemplates.get(b, "state")));
                if (name.equals("minecraft:jigsaw") || name.endsWith("_marker")) continue;
                int[] pos = BuildCityTemplates.pos(b);
                BuildCityTemplates.Tag target = now.blockAt(pos[0], pos[1] + dy, pos[2]);
                if (target == null || BuildCityTemplates.get(target, "nbt") != null) continue;
                if (!name.equals(now.palette.get(BuildCityTemplates.num(BuildCityTemplates.get(target, "state"))))) continue;
                ((BuildCityTemplates.TComp) target).v.put("nbt", nbt);
                restored++;
            }
            if (restored > 0) {
                BuildCityTemplates.save(now.root, p);
                System.out.println("  " + p.getFileName() + "  restored NBT on " + restored + " blocks (dy=" + dy + ")");
            }
        }
    }
}
