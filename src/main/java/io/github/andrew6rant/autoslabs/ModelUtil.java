package io.github.andrew6rant.autoslabs;

import io.github.thepoultryman.arrp_but_different.api.RuntimeResourcePack;
import io.github.thepoultryman.arrp_but_different.json.state.JBlockModel;
import io.github.thepoultryman.arrp_but_different.json.state.JState;
import io.github.thepoultryman.arrp_but_different.json.state.JVariant;
import io.github.thepoultryman.arrp_but_different.json.model.*;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

public class ModelUtil {
    public static void setup(RuntimeResourcePack AUTO_SLABS_RESOURCES, Block block) {
        Identifier id = Registries.BLOCK.getId(block);
        String namespace = id.getNamespace();
        String path = id.getPath();
        Identifier vertical_north_south_top_slab = Identifier.of(namespace, "block/"+path + "_vertical_north_south_top");
        Identifier vertical_north_south_bottom_slab = Identifier.of(namespace, "block/"+path + "_vertical_north_south_bottom");
        Identifier vertical_east_west_top_slab = Identifier.of(namespace, "block/"+path + "_vertical_east_west_top");
        Identifier vertical_east_west_bottom_slab = Identifier.of(namespace, "block/"+path + "_vertical_east_west_bottom");

        // Yes, I know these models are incredibly inefficient, but I need to parent them this way for the best mod compatibility.
        JModel verticalSlabNorthSouthTopModel = JModel.model(namespace+":block/"+path)
                .element(new JElement().from(0, 0, 0).to(16, 16, 8)
                        .faces(new JFaces()
                                .north(new JFace("side").cullface(Direction.NORTH).uv(0, 0, 16, 16))
                                .east(new JFace("side").cullface(Direction.EAST).uv(8, 0, 16, 16))
                                .south(new JFace("side").uv(0, 0, 16, 16))
                                .west(new JFace("side").cullface(Direction.WEST).uv(0, 0, 8, 16))
                                .up(new JFace("top").cullface(Direction.UP).uv(0, 0, 16, 8))
                                .down(new JFace("bottom").cullface(Direction.DOWN).uv(0, 0, 16, 8))));

        JModel verticalSlabNorthSouthBottomModel = JModel.model(namespace+":block/"+path)
                .element(new JElement().from(0, 0, 8).to(16, 16, 16)
                        .faces(new JFaces()
                                .north(new JFace("side").uv(0, 0, 16, 16))
                                .east(new JFace("side").cullface(Direction.EAST).uv(0, 0, 8, 16))
                                .south(new JFace("side").cullface(Direction.SOUTH).uv(0, 0, 16, 16))
                                .west(new JFace("side").cullface(Direction.WEST).uv(8, 0, 16, 16))
                                .up(new JFace("top").cullface(Direction.UP).uv(0, 8, 16, 16))
                                .down(new JFace("bottom").cullface(Direction.DOWN).uv(0, 0, 16, 8))));

        JModel verticalSlabEastWestTopModel = JModel.model(namespace+":block/"+path)
                .element(new JElement().from(8, 0, 0).to(16, 16, 16)
                        .faces(new JFaces()
                                .north(new JFace("side").cullface(Direction.NORTH).uv(0, 0, 8, 16))
                                .east(new JFace("side").cullface(Direction.EAST).uv(0, 0, 16, 16))
                                .south(new JFace("side").cullface(Direction.SOUTH).uv(8, 0, 16, 16))
                                .west(new JFace("side").uv(0, 0, 16, 16))
                                .up(new JFace("top").cullface(Direction.UP).uv(8, 0, 16, 16))
                                .down(new JFace("bottom").cullface(Direction.DOWN).uv(8, 0, 16, 16))));

        JModel verticalSlabEastWestBottomModel = JModel.model(namespace+":block/"+path)
                .element(new JElement().from(0, 0, 0).to(8, 16, 16)
                        .faces(new JFaces()
                                .north(new JFace("side").cullface(Direction.NORTH).uv(8, 0, 16, 16))
                                .east(new JFace("side").uv(0, 0, 16, 16))
                                .south(new JFace("side").cullface(Direction.SOUTH).uv(0, 0, 8, 16))
                                .west(new JFace("side").cullface(Direction.WEST).uv(0, 0, 16, 16))
                                .up(new JFace("top").cullface(Direction.UP).uv(0, 0, 8, 16))
                                .down(new JFace("bottom").cullface(Direction.DOWN).uv(0, 0, 8, 16))));

        AUTO_SLABS_RESOURCES.addModel(vertical_north_south_top_slab, verticalSlabNorthSouthTopModel);
        AUTO_SLABS_RESOURCES.addModel(vertical_north_south_bottom_slab, verticalSlabNorthSouthBottomModel);
        AUTO_SLABS_RESOURCES.addModel(vertical_east_west_top_slab, verticalSlabEastWestTopModel);
        AUTO_SLABS_RESOURCES.addModel(vertical_east_west_bottom_slab, verticalSlabEastWestBottomModel);

        AUTO_SLABS_RESOURCES.addBlockSate(id, new JState(new JVariant()
                .put("type=bottom,vertical_type=north_south", new JBlockModel(vertical_north_south_bottom_slab))
                .put("type=bottom,vertical_type=east_west", new JBlockModel(vertical_east_west_bottom_slab))
                .put("type=top,vertical_type=north_south", new JBlockModel(vertical_north_south_top_slab))
                .put("type=top,vertical_type=east_west", new JBlockModel(vertical_east_west_top_slab))
        ));
    }
}
