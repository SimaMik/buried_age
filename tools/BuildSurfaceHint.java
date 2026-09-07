import java.nio.file.*;
import java.util.*;

public class BuildSurfaceHint {
    public static void main(String[] args) throws Exception {
        Path file = Path.of("src/main/resources/data/buried_age/structure/surface_hint.nbt");
        BuildCityTemplates.TComp root = BuildCityTemplates.load(file);
        BuildCityTemplates.Tpl old = new BuildCityTemplates.Tpl(root);

        BuildCityTemplates.Tag jigsaw = null;
        int jigsawState = -1;
        for (BuildCityTemplates.Tag b : old.blocks) {
            int state = BuildCityTemplates.num(BuildCityTemplates.get(b, "state"));
            if ("minecraft:jigsaw".equals(old.palette.get(state))) {
                jigsaw = b;
                jigsawState = state;
            }
        }
        if (jigsaw == null) throw new IllegalStateException("no jigsaw in " + file);

        BuildCityTemplates.TComp nbt = (BuildCityTemplates.TComp) BuildCityTemplates.get(jigsaw, "nbt");
        nbt.v.put("final_state", new BuildCityTemplates.TStr("minecraft:air"));
        BuildCityTemplates.TComp block = new BuildCityTemplates.TComp(new LinkedHashMap<>());
        block.v.put("pos", new BuildCityTemplates.TList(3, List.of(new BuildCityTemplates.TInt(0), new BuildCityTemplates.TInt(0), new BuildCityTemplates.TInt(0))));
        block.v.put("state", new BuildCityTemplates.TInt(0));
        block.v.put("nbt", nbt);

        root.v.put("size", new BuildCityTemplates.TList(3, List.of(new BuildCityTemplates.TInt(2), new BuildCityTemplates.TInt(3), new BuildCityTemplates.TInt(2))));
        root.v.put("palette", new BuildCityTemplates.TList(10, List.of(old.paletteEntries.get(jigsawState))));
        root.v.put("blocks", new BuildCityTemplates.TList(10, List.of(block)));
        root.v.put("entities", new BuildCityTemplates.TList(10, new ArrayList<>()));
        BuildCityTemplates.save(root, file);
        System.out.println("wrote " + file + ": 2x3x2, one jigsaw, final state air");
    }
}
