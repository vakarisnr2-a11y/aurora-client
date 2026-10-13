import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Converts a Minecraft 1.8.9 resource pack (pack format 1) into a 1.21.1 pack (format 34). Used for the built-in
 * Bedwars pack:
 *
 * <pre>
 * java tools/PackConverter.java "[3Supay] f16x.zip" src/main/resources/resourcepacks/aurora_bedwars
 * </pre>
 *
 * <p>What it does:</p>
 * <ul>
 *   <li>renames block and item textures to their 1.13+ names ({@code blocks/wool_colored_red} becomes
 *       {@code block/red_wool}, {@code items/gold_sword} becomes {@code item/golden_sword}, ...), keeping animation
 *       {@code .mcmeta} files;</li>
 *   <li>cuts the 1.8 {@code gui/icons.png} and {@code gui/widgets.png} atlases into the 1.20.2+ GUI sprites (hearts,
 *       food, armor, air, crosshair, XP and jump bars, ping icons, hotbar, selection, buttons);</li>
 *   <li>splits {@code particle/particles.png} into individual particles, and the compass and clock animation strips
 *       into their numbered frames;</li>
 *   <li>builds the 1.21 bed entity textures from the six 1.8 bed block textures and recolors the (red) blanket for
 *       all 16 colors;</li>
 *   <li>re-lays out chests for the model change in 1.15 (faces turned and top/bottom and front/back swapped) and
 *       splits double chests into left and right halves;</li>
 *   <li>copies what kept its name (armor models, containers, colormaps, sky, font, misc) and drops what 1.21 cannot
 *       use (MCPatcher/OptiFine folders, system files, textures that no longer exist).</li>
 * </ul>
 */
public class PackConverter {
    static final Map<String, byte[]> src = new LinkedHashMap<>();
    static final Set<String> used = new HashSet<>();
    static Path out;
    static int written;

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("usage: java tools/PackConverter.java <1.8 pack .zip or folder> <output folder>");
            System.exit(1);
        }
        load(Path.of(args[0]));
        out = Path.of(args[1]);
        deleteTree(out);
        Files.createDirectories(out);
        packMeta();
        renamed("blocks/", "block/", BLOCKS);
        renamed("items/", "item/", ITEMS);
        copied();
        strip("items/compass", "item/compass_%02d");
        strip("items/clock", "item/clock_%02d");
        gui();
        particles();
        beds();
        chests();
        ladderModel();
        Set<String> skipped = new TreeSet<>();
        for (String k : src.keySet()) if (!used.contains(k) && !k.endsWith("/")) skipped.add(k);
        System.out.println("Wrote " + written + " files to " + out.toAbsolutePath());
        System.out.println("Not used (" + skipped.size() + "): " + String.join(", ", skipped));
    }

    // =====================================================================================================
    // Input / output
    // =====================================================================================================

    static void load(Path p) throws IOException {
        if (Files.isDirectory(p)) {
            try (Stream<Path> walk = Files.walk(p)) {
                for (Path f : walk.filter(Files::isRegularFile).toList()) src.put(p.relativize(f).toString().replace('\\', '/'), Files.readAllBytes(f));
            }
        } else {
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(p))) {
                for (ZipEntry e; (e = zip.getNextEntry()) != null; ) if (!e.isDirectory()) src.put(e.getName(), zip.readAllBytes());
            }
        }
        // Packs are sometimes zipped with an extra top folder: strip it.
        if (!src.containsKey("pack.mcmeta")) {
            String prefix = src.keySet().stream().filter(k -> k.endsWith("/pack.mcmeta")).findFirst()
                .map(k -> k.substring(0, k.length() - "pack.mcmeta".length())).orElseThrow(() -> new IOException("No pack.mcmeta found"));
            Map<String, byte[]> moved = new LinkedHashMap<>();
            src.forEach((k, v) -> {
                if (k.startsWith(prefix)) moved.put(k.substring(prefix.length()), v);
            });
            src.clear();
            src.putAll(moved);
        }
    }

    static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }

    static final String TEX = "assets/minecraft/textures/";

    static byte[] raw(String path) {
        byte[] b = src.get(path);
        if (b != null) used.add(path);
        return b;
    }

    /** A texture from the source pack ({@code path} relative to {@code textures/}, without .png), or null. */
    static Img tex(String path) throws IOException {
        byte[] b = raw(TEX + path + ".png");
        if (b == null) return null;
        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(b));
        BufferedImage argb = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
        argb.getGraphics().drawImage(bi, 0, 0, null);
        Img img = new Img(bi.getWidth(), bi.getHeight());
        argb.getRGB(0, 0, img.w, img.h, img.px, 0, img.w);
        return img;
    }

    static void write(String path, byte[] data) throws IOException {
        Path f = out.resolve(path);
        Files.createDirectories(f.getParent());
        Files.write(f, data);
        written++;
    }

    static void save(Img img, String texPath) throws IOException {
        Path f = out.resolve(TEX + texPath + ".png");
        Files.createDirectories(f.getParent());
        ImageIO.write(img.toBuffered(), "png", f.toFile());
        written++;
    }

    static void text(String path, String content) throws IOException {
        write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    static void packMeta() throws IOException {
        String meta = new String(raw("pack.mcmeta"), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("\"description\"\\s*:\\s*(\"(?:[^\"\\\\]|\\\\.)*\")").matcher(meta);
        String description = m.find() ? m.group(1) : "\"Bedwars pack\"";
        text("pack.mcmeta", "{\n  \"pack\": {\n    \"pack_format\": 34,\n    \"description\": " + description.replaceAll("^\"\\s+", "\"") + "\n  }\n}\n");
        byte[] icon = raw("pack.png");
        if (icon != null) write("pack.png", icon);
    }

    // =====================================================================================================
    // Images
    // =====================================================================================================

    static final class Img {
        final int w, h;
        final int[] px;

        Img(int w, int h) {
            this.w = w;
            this.h = h;
            px = new int[w * h];
        }

        int get(int x, int y) {
            return x >= 0 && y >= 0 && x < w && y < h ? px[y * w + x] : 0;
        }

        void set(int x, int y, int c) {
            if (x >= 0 && y >= 0 && x < w && y < h) px[y * w + x] = c;
        }

        Img crop(int x, int y, int cw, int ch) {
            Img c = new Img(cw, ch);
            for (int yy = 0; yy < ch; yy++) for (int xx = 0; xx < cw; xx++) c.set(xx, yy, get(x + xx, y + yy));
            return c;
        }

        /** Copies {@code src} to (dx, dy). */
        void paste(Img s, int dx, int dy) {
            for (int y = 0; y < s.h; y++) for (int x = 0; x < s.w; x++) set(dx + x, dy + y, s.get(x, y));
        }

        Img flipV() {
            Img c = new Img(w, h);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) c.set(x, h - 1 - y, get(x, y));
            return c;
        }

        Img rot180() {
            Img c = new Img(w, h);
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) c.set(w - 1 - x, h - 1 - y, get(x, y));
            return c;
        }

        BufferedImage toBuffered() {
            BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            b.setRGB(0, 0, w, h, px, 0, w);
            return b;
        }
    }

    /** Scale factor of a texture relative to its 16x size (2 for a 32x pack, ...). */
    static int scaleOf(Img img, int vanillaWidth) {
        return Math.max(1, img.w / vanillaWidth);
    }

    // =====================================================================================================
    // Renamed and copied textures
    // =====================================================================================================

    /** 1.8 name = 1.21 name(s), comma separated. Names not listed but present in {@link #SAME_BLOCKS} keep their name. */
    static final String[] BLOCKS = {
        "anvil_base=anvil", "anvil_top_damaged_0=anvil_top", "anvil_top_damaged_1=chipped_anvil_top", "anvil_top_damaged_2=damaged_anvil_top",
        "brick=bricks", "cobblestone_mossy=mossy_cobblestone", "comparator_off=comparator", "deadbush=dead_bush",
        "dirt_podzol_side=podzol_side", "dirt_podzol_top=podzol_top", "dispenser_front_horizontal=dispenser_front",
        "dropper_front_horizontal=dropper_front", "endframe_eye=end_portal_frame_eye", "endframe_side=end_portal_frame_side",
        "endframe_top=end_portal_frame_top", "farmland_dry=farmland", "farmland_wet=farmland_moist", "fire_layer_0=fire_0",
        "fire_layer_1=fire_1", "flower_allium=allium", "flower_blue_orchid=blue_orchid", "flower_dandelion=dandelion",
        "flower_houstonia=azure_bluet", "flower_oxeye_daisy=oxeye_daisy", "flower_rose=poppy", "flower_tulip_orange=orange_tulip",
        "flower_tulip_pink=pink_tulip", "flower_tulip_red=red_tulip", "flower_tulip_white=white_tulip", "furnace_front_off=furnace_front",
        "grass_side=grass_block_side", "grass_side_overlay=grass_block_side_overlay", "grass_side_snowed=grass_block_snow",
        "grass_top=grass_block_top", "hardened_clay=terracotta", "ice_packed=packed_ice", "itemframe_background=item_frame",
        "melon_stem_connected=attached_melon_stem", "melon_stem_disconnected=melon_stem", "mob_spawner=spawner",
        "mushroom_block_skin_brown=brown_mushroom_block", "mushroom_block_skin_red=red_mushroom_block",
        "mushroom_block_skin_stem=mushroom_stem", "mushroom_brown=brown_mushroom", "mushroom_red=red_mushroom",
        "nether_brick=nether_bricks", "noteblock=note_block", "piston_top_normal=piston_top", "portal=nether_portal",
        "prismarine_dark=dark_prismarine", "prismarine_rough=prismarine", "pumpkin_face_off=carved_pumpkin",
        "pumpkin_face_on=jack_o_lantern", "pumpkin_stem_connected=attached_pumpkin_stem", "pumpkin_stem_disconnected=pumpkin_stem",
        "quartz_block_chiseled=chiseled_quartz_block", "quartz_block_chiseled_top=chiseled_quartz_block_top",
        "quartz_block_lines=quartz_pillar", "quartz_block_lines_top=quartz_pillar_top", "quartz_ore=nether_quartz_ore",
        "rail_activator=activator_rail", "rail_activator_powered=activator_rail_on", "rail_detector=detector_rail",
        "rail_detector_powered=detector_rail_on", "rail_golden=powered_rail", "rail_golden_powered=powered_rail_on",
        "rail_normal=rail", "rail_normal_turned=rail_corner", "red_sandstone_carved=chiseled_red_sandstone",
        "red_sandstone_normal=red_sandstone", "red_sandstone_smooth=cut_red_sandstone", "redstone_lamp_off=redstone_lamp",
        "redstone_torch_on=redstone_torch", "reeds=sugar_cane", "repeater_off=repeater", "sandstone_carved=chiseled_sandstone",
        "sandstone_normal=sandstone", "sandstone_smooth=cut_sandstone", "sapling_roofed_oak=dark_oak_sapling", "slime=slime_block",
        "sponge_wet=wet_sponge", "stone_andesite=andesite", "stone_andesite_smooth=polished_andesite", "stone_diorite=diorite",
        "stone_diorite_smooth=polished_diorite", "stone_granite=granite", "stone_granite_smooth=polished_granite",
        "stone_slab_side=smooth_stone_slab_side", "stone_slab_top=smooth_stone", "stonebrick=stone_bricks",
        "stonebrick_carved=chiseled_stone_bricks", "stonebrick_cracked=cracked_stone_bricks", "stonebrick_mossy=mossy_stone_bricks",
        "tallgrass=short_grass", "torch_on=torch", "trapdoor=oak_trapdoor", "trip_wire=tripwire", "trip_wire_source=tripwire_hook",
        "waterlily=lily_pad", "web=cobweb", "door_wood_lower=oak_door_bottom", "door_wood_upper=oak_door_top",
        "door_iron_lower=iron_door_bottom", "door_iron_upper=iron_door_top", "double_plant_fern_bottom=large_fern_bottom",
        "double_plant_fern_top=large_fern_top", "double_plant_grass_bottom=tall_grass_bottom", "double_plant_grass_top=tall_grass_top",
        "double_plant_paeonia_bottom=peony_bottom", "double_plant_paeonia_top=peony_top", "double_plant_rose_bottom=rose_bush_bottom",
        "double_plant_rose_top=rose_bush_top", "double_plant_sunflower_back=sunflower_back", "double_plant_sunflower_bottom=sunflower_bottom",
        "double_plant_sunflower_front=sunflower_front", "double_plant_sunflower_top=sunflower_top", "double_plant_syringa_bottom=lilac_bottom",
        "double_plant_syringa_top=lilac_top",
    };

    /** Block textures whose name did not change. */
    static final String[] SAME_BLOCKS = {
        "beacon", "bedrock", "bookshelf", "brewing_stand", "brewing_stand_base", "cactus_bottom", "cactus_side", "cactus_top", "cake_bottom",
        "cake_inner", "cake_side", "cake_top", "cauldron_bottom", "cauldron_inner", "cauldron_side", "cauldron_top", "clay",
        "coal_block", "coal_ore", "coarse_dirt", "cobblestone", "comparator_on", "crafting_table_front", "crafting_table_side",
        "crafting_table_top", "daylight_detector_inverted_top", "daylight_detector_side", "daylight_detector_top", "diamond_block",
        "diamond_ore", "dirt", "dispenser_front_vertical", "dragon_egg", "dropper_front_vertical", "emerald_block", "emerald_ore",
        "enchanting_table_bottom", "enchanting_table_side", "enchanting_table_top", "end_stone", "fern", "flower_pot", "furnace_front_on",
        "furnace_side", "furnace_top", "glass", "glass_pane_top", "glowstone", "gold_block", "gold_ore", "gravel", "hay_block_side",
        "hay_block_top", "hopper_inside", "hopper_outside", "hopper_top", "ice", "iron_bars", "iron_block", "iron_ore", "iron_trapdoor",
        "jukebox_side", "jukebox_top", "ladder", "lapis_block", "lapis_ore", "lava_flow", "lava_still", "lever", "melon_side", "melon_top",
        "mushroom_block_inside", "mycelium_side", "mycelium_top", "netherrack", "obsidian", "piston_bottom", "piston_inner", "piston_side",
        "piston_top_sticky", "prismarine_bricks", "pumpkin_side", "pumpkin_top", "quartz_block_bottom", "quartz_block_side",
        "quartz_block_top", "red_sand", "red_sandstone_bottom", "red_sandstone_top", "redstone_block", "redstone_lamp_on", "redstone_ore",
        "redstone_torch_off", "repeater_on", "sand", "sandstone_bottom", "sandstone_top", "sea_lantern", "snow", "soul_sand", "sponge",
        "stone", "tnt_bottom", "tnt_side", "tnt_top", "vine", "water_flow", "water_still",
    };

    static final String[] ITEMS = {
        "apple_golden=golden_apple", "beef_cooked=cooked_beef", "book_enchanted=enchanted_book", "book_normal=book",
        "book_writable=writable_book", "book_written=written_book", "bow_standby=bow", "bucket_empty=bucket", "bucket_lava=lava_bucket",
        "bucket_milk=milk_bucket", "bucket_water=water_bucket", "fireball=fire_charge", "fishing_rod_uncast=fishing_rod",
        "gold_axe=golden_axe", "gold_boots=golden_boots", "gold_chestplate=golden_chestplate", "gold_helmet=golden_helmet",
        "gold_hoe=golden_hoe", "gold_leggings=golden_leggings", "gold_pickaxe=golden_pickaxe", "gold_shovel=golden_shovel",
        "gold_sword=golden_sword", "map_empty=map", "map_filled=filled_map", "potion_bottle_drinkable=potion",
        "potion_bottle_empty=glass_bottle", "potion_bottle_splash=splash_potion,lingering_potion", "slimeball=slime_ball",
        "wood_axe=wooden_axe", "wood_hoe=wooden_hoe", "wood_pickaxe=wooden_pickaxe", "wood_shovel=wooden_shovel", "wood_sword=wooden_sword",
    };

    static final String[] SAME_ITEMS = {
        "apple", "arrow", "blaze_powder", "blaze_rod", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2", "carrot_on_a_stick",
        "chainmail_boots", "chainmail_chestplate", "chainmail_helmet", "chainmail_leggings", "charcoal", "coal", "diamond", "diamond_axe",
        "diamond_boots", "diamond_chestplate", "diamond_helmet", "diamond_hoe", "diamond_leggings", "diamond_pickaxe", "diamond_shovel",
        "diamond_sword", "egg", "emerald", "empty_armor_slot_boots", "empty_armor_slot_chestplate", "empty_armor_slot_helmet",
        "empty_armor_slot_leggings", "ender_eye", "ender_pearl", "experience_bottle", "fishing_rod_cast", "flint", "flint_and_steel",
        "gold_ingot", "iron_axe", "iron_boots", "iron_chestplate", "iron_helmet", "iron_hoe", "iron_ingot", "iron_leggings", "iron_pickaxe",
        "iron_shovel", "iron_sword", "leather_boots", "leather_boots_overlay", "leather_chestplate", "leather_chestplate_overlay",
        "leather_helmet", "leather_helmet_overlay", "leather_leggings", "leather_leggings_overlay", "name_tag", "nether_star", "paper",
        "potion_overlay", "shears", "snowball", "stick", "stone_axe", "stone_hoe", "stone_pickaxe", "stone_shovel", "stone_sword",
    };

    static final String[] COLORS_18 = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "silver", "cyan",
        "purple", "blue", "brown", "green", "red", "black"};

    static String color121(String c) {
        return c.equals("silver") ? "light_gray" : c;
    }

    /** Copies one texture (and its .mcmeta) under one or more new names. */
    static void copyTexture(String from, String... to) throws IOException {
        byte[] png = raw(TEX + from + ".png");
        if (png == null) return;
        byte[] meta = raw(TEX + from + ".png.mcmeta");
        for (String t : to) {
            write(TEX + t + ".png", png);
            if (meta != null) write(TEX + t + ".png.mcmeta", meta);
        }
    }

    static void renamed(String fromDir, String toDir, String[] table) throws IOException {
        for (String row : table) {
            String[] kv = row.split("=");
            String[] targets = kv[1].split(",");
            for (int i = 0; i < targets.length; i++) targets[i] = toDir + targets[i];
            copyTexture(fromDir + kv[0], targets);
        }
        String[] same = fromDir.equals("blocks/") ? SAME_BLOCKS : SAME_ITEMS;
        for (String n : same) copyTexture(fromDir + n, toDir + n);
        if (fromDir.equals("blocks/")) {
            for (String c : COLORS_18) {
                String n = color121(c);
                copyTexture("blocks/wool_colored_" + c, "block/" + n + "_wool");
                copyTexture("blocks/glass_" + c, "block/" + n + "_stained_glass");
                copyTexture("blocks/glass_pane_top_" + c, "block/" + n + "_stained_glass_pane_top");
                copyTexture("blocks/hardened_clay_stained_" + c, "block/" + n + "_terracotta");
            }
            for (String w : new String[]{"oak", "spruce", "birch", "jungle", "acacia", "big_oak"}) {
                String n = w.equals("big_oak") ? "dark_oak" : w;
                copyTexture("blocks/planks_" + w, "block/" + n + "_planks");
                copyTexture("blocks/log_" + w, "block/" + n + "_log");
                copyTexture("blocks/log_" + w + "_top", "block/" + n + "_log_top");
                copyTexture("blocks/leaves_" + w, "block/" + n + "_leaves");
                if (!w.equals("big_oak")) copyTexture("blocks/sapling_" + w, "block/" + n + "_sapling");
                if (!w.equals("oak")) {
                    copyTexture("blocks/door_" + (w.equals("big_oak") ? "dark_oak" : w) + "_lower", "block/" + n + "_door_bottom");
                    copyTexture("blocks/door_" + (w.equals("big_oak") ? "dark_oak" : w) + "_upper", "block/" + n + "_door_top");
                }
            }
            for (int i = 0; i <= 7; i++) copyTexture("blocks/wheat_stage_" + i, "block/wheat_stage" + i);
            for (int i = 0; i <= 3; i++) {
                copyTexture("blocks/carrots_stage_" + i, "block/carrots_stage" + i);
                copyTexture("blocks/potatoes_stage_" + i, "block/potatoes_stage" + i);
            }
            for (int i = 0; i <= 2; i++) {
                copyTexture("blocks/cocoa_stage_" + i, "block/cocoa_stage" + i);
                copyTexture("blocks/nether_wart_stage_" + i, "block/nether_wart_stage" + i);
            }
            for (int i = 0; i <= 9; i++) copyTexture("blocks/destroy_stage_" + i, "block/destroy_stage_" + i);
        }
    }

    /** Textures that kept their path in 1.21. */
    static void copied() throws IOException {
        for (String m : new String[]{"chainmail", "diamond", "gold", "iron", "leather"}) {
            for (String l : new String[]{"1", "2"}) copyTexture("models/armor/" + m + "_layer_" + l, "models/armor/" + m + "_layer_" + l);
        }
        for (String l : new String[]{"1", "2"}) copyTexture("models/armor/leather_layer_" + l + "_overlay", "models/armor/leather_layer_" + l + "_overlay");
        for (String c : new String[]{"inventory", "generic_54", "crafting_table", "furnace", "dispenser", "hopper", "enchanting_table",
            "anvil", "brewing_stand", "beacon", "horse"}) {
            copyTexture("gui/container/" + c, "gui/container/" + c);
        }
        for (int i = 0; i < 6; i++) copyTexture("gui/title/background/panorama_" + i, "gui/title/background/panorama_" + i);
        copyTexture("gui/book", "gui/book");
        copyTexture("colormap/grass", "colormap/grass");
        copyTexture("colormap/foliage", "colormap/foliage");
        for (String e : new String[]{"sun", "moon_phases", "rain", "snow", "clouds", "end_sky"}) copyTexture("environment/" + e, "environment/" + e);
        for (String m : new String[]{"pumpkinblur", "vignette", "shadow", "underwater", "unknown_pack"}) copyTexture("misc/" + m, "misc/" + m);
        copyTexture("misc/enchanted_item_glint", "misc/enchanted_glint_item", "misc/enchanted_glint_entity");
        copyTexture("font/ascii", "font/ascii");
    }

    /** A vertical animation strip (1.8 compass and clock) cut into numbered frames. */
    static void strip(String from, String pattern) throws IOException {
        Img img = tex(from);
        raw(TEX + from + ".png.mcmeta");
        if (img == null) return;
        int frames = img.h / img.w;
        for (int i = 0; i < frames; i++) save(img.crop(0, i * img.w, img.w, img.w), String.format(pattern, i));
    }

    // =====================================================================================================
    // GUI atlases
    // =====================================================================================================

    static void gui() throws IOException {
        Img widgets = tex("gui/widgets");
        if (widgets != null) {
            int s = scaleOf(widgets, 256);
            save(widgets.crop(0, 0, 182 * s, 22 * s), "gui/sprites/hud/hotbar");
            save(widgets.crop(0, 22 * s, 24 * s, 23 * s), "gui/sprites/hud/hotbar_selection");
            button(widgets.crop(0, 46 * s, 200 * s, 20 * s), "button_disabled", 1);
            button(widgets.crop(0, 66 * s, 200 * s, 20 * s), "button", 3);
            button(widgets.crop(0, 86 * s, 200 * s, 20 * s), "button_highlighted", 3);
        }
        Img icons = tex("gui/icons");
        if (icons == null) return;
        int s = scaleOf(icons, 256);
        Map<String, int[]> sprites = new LinkedHashMap<>();
        sprites.put("hud/crosshair", new int[]{0, 0, 15, 15});
        String[] kinds = {"", "poisoned_", "withered_"};
        for (int hardcore = 0; hardcore < 2; hardcore++) {
            int y = hardcore == 0 ? 0 : 45;
            String hc = hardcore == 0 ? "" : "hardcore_";
            for (int k = 0; k < 3; k++) {
                int base = 16 + k * 36;
                sprites.put("hud/heart/" + kinds[k] + hc + "full", new int[]{base + 36, y, 9, 9});
                sprites.put("hud/heart/" + kinds[k] + hc + "half", new int[]{base + 45, y, 9, 9});
                sprites.put("hud/heart/" + kinds[k] + hc + "full_blinking", new int[]{base + 54, y, 9, 9});
                sprites.put("hud/heart/" + kinds[k] + hc + "half_blinking", new int[]{base + 63, y, 9, 9});
            }
            sprites.put("hud/heart/absorbing_" + hc + "full", new int[]{160, y, 9, 9});
            sprites.put("hud/heart/absorbing_" + hc + "half", new int[]{169, y, 9, 9});
            sprites.put("hud/heart/absorbing_" + hc + "full_blinking", new int[]{160, y, 9, 9});
            sprites.put("hud/heart/absorbing_" + hc + "half_blinking", new int[]{169, y, 9, 9});
            sprites.put("hud/heart/container" + (hardcore == 0 ? "" : "_hardcore"), new int[]{16, y, 9, 9});
            sprites.put("hud/heart/container" + (hardcore == 0 ? "" : "_hardcore") + "_blinking", new int[]{25, y, 9, 9});
        }
        sprites.put("hud/heart/vehicle_container", new int[]{52, 9, 9, 9});
        sprites.put("hud/heart/vehicle_full", new int[]{88, 9, 9, 9});
        sprites.put("hud/heart/vehicle_half", new int[]{97, 9, 9, 9});
        sprites.put("hud/armor_empty", new int[]{16, 9, 9, 9});
        sprites.put("hud/armor_half", new int[]{25, 9, 9, 9});
        sprites.put("hud/armor_full", new int[]{34, 9, 9, 9});
        sprites.put("hud/air", new int[]{16, 18, 9, 9});
        sprites.put("hud/air_bursting", new int[]{25, 18, 9, 9});
        sprites.put("hud/food_empty", new int[]{16, 27, 9, 9});
        sprites.put("hud/food_full", new int[]{52, 27, 9, 9});
        sprites.put("hud/food_half", new int[]{61, 27, 9, 9});
        sprites.put("hud/food_full_hunger", new int[]{88, 27, 9, 9});
        sprites.put("hud/food_half_hunger", new int[]{97, 27, 9, 9});
        sprites.put("hud/food_empty_hunger", new int[]{133, 27, 9, 9});
        sprites.put("hud/experience_bar_background", new int[]{0, 64, 182, 5});
        sprites.put("hud/experience_bar_progress", new int[]{0, 69, 182, 5});
        sprites.put("hud/jump_bar_background", new int[]{0, 84, 182, 5});
        sprites.put("hud/jump_bar_progress", new int[]{0, 89, 182, 5});
        String[] pings = {"ping_5", "ping_4", "ping_3", "ping_2", "ping_1", "ping_unknown"};
        for (int i = 0; i < pings.length; i++) sprites.put("icon/" + pings[i], new int[]{0, 176 + i * 8, 10, 8});
        for (Map.Entry<String, int[]> e : sprites.entrySet()) {
            int[] r = e.getValue();
            save(icons.crop(r[0] * s, r[1] * s, r[2] * s, r[3] * s), "gui/sprites/" + e.getKey());
        }
    }

    /** A button sprite with vanilla's nine-slice scaling so it stretches like 1.8's buttons did. */
    static void button(Img img, String name, int border) throws IOException {
        save(img, "gui/sprites/widget/" + name);
        text(TEX + "gui/sprites/widget/" + name + ".png.mcmeta",
            "{\n  \"gui\": {\n    \"scaling\": {\n      \"type\": \"nine_slice\",\n      \"width\": 200,\n      \"height\": 20,\n      \"border\": " + border + "\n    }\n  }\n}\n");
    }

    /** The 1.8 particle atlas (8 px cells, 16 per row) cut into the 1.21 particle textures it covers. */
    static void particles() throws IOException {
        Img atlas = tex("particle/particles");
        if (atlas == null) return;
        int s = scaleOf(atlas, 128), c = 8 * s;
        Map<String, Integer> cells = new LinkedHashMap<>();
        for (int i = 0; i < 8; i++) cells.put("generic_" + i, i);
        for (int i = 0; i < 4; i++) cells.put("splash_" + i, 19 + i);
        cells.put("bubble", 32);
        cells.put("flame", 48);
        cells.put("lava", 49);
        cells.put("note", 64);
        cells.put("critical_hit", 65);
        cells.put("enchanted_hit", 66);
        cells.put("heart", 80);
        cells.put("angry", 81);
        cells.put("glint", 82);
        for (Map.Entry<String, Integer> e : cells.entrySet()) {
            int i = e.getValue();
            save(atlas.crop((i % 16) * c, (i / 16) * c, c, c), "particle/" + e.getKey());
        }
    }

    // =====================================================================================================
    // Beds
    // =====================================================================================================

    static final String[] BED_COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
        "purple", "blue", "brown", "green", "red", "black"};
    static final int[] DYE = {0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52, 0x9D9D97, 0x169C9C,
        0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21};

    /**
     * 1.21 draws beds as entities from one 64x64 texture (head and foot cuboids 16x16x6 at (0,0) and (0,22), legs at
     * x 50+). It is built from the 1.8 block textures: the tops (rotated so the pillow end faces up), the mattress
     * part (rows 7-12) of the side and end textures, and the bottom. 1.8 beds are always red, so the other colors
     * are made by giving the red blanket pixels the dye color.
     */
    static void beds() throws IOException {
        Img headTop = tex("blocks/bed_head_top"), feetTop = tex("blocks/bed_feet_top"), headSide = tex("blocks/bed_head_side"),
            feetSide = tex("blocks/bed_feet_side"), headEnd = tex("blocks/bed_head_end"), feetEnd = tex("blocks/bed_feet_end");
        Img bottom = tex("blocks/bed_bottom");
        if (bottom == null) bottom = tex("blocks/planks_oak");
        if (headTop == null || feetTop == null || headSide == null || feetSide == null || headEnd == null || feetEnd == null) return;
        int s = scaleOf(headTop, 16), n = 16 * s;
        Img bed = new Img(64 * s, 64 * s);
        // Mattress tops: the pillow end is the right edge in 1.8 and the top edge here.
        bed.paste(rotateCcw(headTop), 6 * s, 6 * s);
        bed.paste(rotateCcw(feetTop), 6 * s, 28 * s);
        // Ends: the mattress rows 7..12 of the end textures, frame row first.
        for (int r = 0; r < 6 * s; r++) {
            for (int x = 0; x < n; x++) {
                bed.set(6 * s + x, r, headEnd.get(x, 13 * s - 1 - r));
                bed.set(22 * s + x, 22 * s + r, feetEnd.get(x, 13 * s - 1 - r));
            }
        }
        // Sides: 6 wide (height of the mattress) by 16 long; the head end is at the top.
        for (int part = 0; part < 2; part++) {
            Img side = part == 0 ? headSide : feetSide;
            int v = part == 0 ? 6 * s : 28 * s;
            for (int y = 0; y < n; y++) {
                for (int c = 0; c < 6 * s; c++) {
                    bed.set(c, v + y, side.get(n - 1 - y, 13 * s - 1 - c));
                    bed.set(22 * s + c, v + y, side.get(n - 1 - y, 7 * s + c));
                }
            }
        }
        // Undersides and legs.
        bed.paste(scaled(bottom, n), 28 * s, 6 * s);
        bed.paste(scaled(bottom, n), 28 * s, 28 * s);
        Img leg = headSide.crop(13 * s, 13 * s, 3 * s, 3 * s);
        for (int y = 0; y < 24 * s; y += 3 * s) for (int x = 50 * s; x < 62 * s; x += 3 * s) bed.paste(leg, x, y);
        int[] red = blanketStats(bed);
        for (int i = 0; i < BED_COLORS.length; i++) save(BED_COLORS[i].equals("red") ? bed : recolor(bed, red, DYE[i]), "entity/bed/" + BED_COLORS[i]);
    }

    /** Rotates a square image 90 degrees counterclockwise (the right edge becomes the top edge). */
    static Img rotateCcw(Img img) {
        Img out = new Img(img.h, img.w);
        for (int y = 0; y < out.h; y++) for (int x = 0; x < out.w; x++) out.set(x, y, img.get(img.w - 1 - y, x));
        return out;
    }

    static Img scaled(Img img, int size) {
        if (img.w == size) return img;
        Img out = new Img(size, size);
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) out.set(x, y, img.get(x * img.w / size, y * img.h / size));
        return out;
    }

    static boolean isBlanket(int c) {
        if ((c >>> 24) == 0) return false;
        float[] hsv = java.awt.Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null);
        return (hsv[0] < 0.06f || hsv[0] > 0.92f) && hsv[1] > 0.35f && hsv[2] > 0.18f;
    }

    /** Average saturation and brightness of the red blanket pixels. */
    static int[] blanketStats(Img bed) {
        double s = 0, v = 0;
        int n = 0;
        for (int c : bed.px) {
            if (!isBlanket(c)) continue;
            float[] hsv = java.awt.Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null);
            s += hsv[1];
            v += hsv[2];
            n++;
        }
        return new int[]{(int) Math.round(n == 0 ? 600 : s / n * 1000), (int) Math.round(n == 0 ? 600 : v / n * 1000)};
    }

    /** Gives the blanket pixels the dye color, keeping their shading relative to the average blanket red. */
    static Img recolor(Img bed, int[] stats, int dye) {
        float[] d = java.awt.Color.RGBtoHSB((dye >> 16) & 255, (dye >> 8) & 255, dye & 255, null);
        double refS = stats[0] / 1000.0, refV = stats[1] / 1000.0;
        Img out = new Img(bed.w, bed.h);
        for (int i = 0; i < bed.px.length; i++) {
            int c = bed.px[i];
            if (!isBlanket(c)) {
                out.px[i] = c;
                continue;
            }
            float[] hsv = java.awt.Color.RGBtoHSB((c >> 16) & 255, (c >> 8) & 255, c & 255, null);
            float sat = (float) Math.min(1, d[1] * hsv[1] / refS);
            float val = (float) Math.min(1, d[2] * hsv[2] / refV);
            out.px[i] = (c & 0xFF000000) | (java.awt.Color.HSBtoRGB(d[0], sat, val) & 0xFFFFFF);
        }
        return out;
    }

    // =====================================================================================================
    // Chests
    // =====================================================================================================

    /** UV regions of a model cuboid at (u, v) with size (w, h, d): down, up, west, north, east, south. */
    static int[][] faces(int u, int v, int w, int h, int d) {
        return new int[][]{{u + d, v, w, d}, {u + d + w, v, w, d}, {u, v + d, d, h}, {u + d, v + d, w, h}, {u + d + w, v + d, d, h},
            {u + d + w + d, v + d, w, h}};
    }

    static final int DOWN = 0, UP = 1, WEST = 2, NORTH = 3, EAST = 4, SOUTH = 5;

    /**
     * 1.8 rendered chests with the model flipped by scale(1, -1, -1); since 1.15 the model is upright. For the same
     * look, each new face comes from the opposite old face: up/down swap and flip vertically, front (now south)
     * and back swap, and sides and front/back turn 180 degrees.
     */
    static Img chestFace(Img old, int[][] of, int face, int s, int from, int width) {
        int[] r = switch (face) {
            case DOWN -> of[UP];
            case UP -> of[DOWN];
            case NORTH -> of[SOUTH];
            case SOUTH -> of[NORTH];
            default -> of[face];
        };
        int fw = width < 0 ? r[2] : width;
        Img part = old.crop((r[0] + from) * s, r[1] * s, fw * s, r[3] * s);
        return face == DOWN || face == UP ? part.flipV() : part.rot180();
    }

    static void chests() throws IOException {
        for (String name : new String[]{"normal", "trapped", "ender", "christmas"}) {
            Img old = tex("entity/chest/" + name);
            if (old == null) continue;
            int s = scaleOf(old, 64);
            Img chest = new Img(64 * s, 64 * s);
            int[][] boxes = {{0, 0, 14, 5, 14}, {0, 19, 14, 10, 14}, {0, 0, 2, 4, 1}};
            for (int[] b : boxes) {
                int[][] f = faces(b[0], b[1], b[2], b[3], b[4]);
                for (int face = 0; face < 6; face++) chest.paste(chestFace(old, f, face, s, 0, -1), f[face][0] * s, f[face][1] * s);
            }
            save(chest, "entity/chest/" + name);
        }
        for (String name : new String[]{"normal", "trapped", "christmas"}) {
            Img old = tex("entity/chest/" + name + "_double");
            if (old == null) continue;
            int s = scaleOf(old, 128);
            save(doubleHalf(old, s, false), "entity/chest/" + name + "_right");
            save(doubleHalf(old, s, true), "entity/chest/" + name + "_left");
        }
    }

    /**
     * One half of a double chest. The 1.8 large chest is one 30-wide model; the half with the lower x (where the
     * old faces start, except the south face which runs the other way) is vanilla's "right" texture.
     */
    static Img doubleHalf(Img old, int s, boolean left) {
        Img half = new Img(64 * s, 64 * s);
        int[][] boxes = {{0, 0, 30, 5, 14, 15}, {0, 19, 30, 10, 14, 15}, {0, 0, 2, 4, 1, 1}};
        for (int[] b : boxes) {
            int[][] of = faces(b[0], b[1], b[2], b[3], b[4]);
            int hw = b[5];
            int[][] nf = faces(b[0], b[1], hw, b[3], b[4]);
            for (int face = 0; face < 6; face++) {
                Img part;
                if (face == WEST || face == EAST) {
                    boolean outer = left ? face == EAST : face == WEST;
                    if (!outer) continue; // the inner side is hidden by the other half
                    part = chestFace(old, of, face, s, 0, -1);
                } else {
                    int oldFace = face == DOWN ? UP : face == UP ? DOWN : face == NORTH ? SOUTH : NORTH;
                    boolean runsBackwards = oldFace == SOUTH; // the south face's u runs from high to low x
                    boolean firstHalf = left == runsBackwards;
                    part = chestFace(old, of, face, s, firstHalf ? 0 : hw, hw);
                }
                half.paste(part, nf[face][0] * s, nf[face][1] * s);
            }
        }
        return half;
    }

    // =====================================================================================================
    // Models
    // =====================================================================================================

    /** The pack's 3D ladder model: texture references move from blocks/ to block/. */
    static void ladderModel() throws IOException {
        byte[] b = raw("assets/minecraft/models/block/ladder.json");
        if (b == null) return;
        String json = new String(b, StandardCharsets.UTF_8).replace("\"blocks/", "\"minecraft:block/");
        text("assets/minecraft/models/block/ladder.json", json);
    }
}
