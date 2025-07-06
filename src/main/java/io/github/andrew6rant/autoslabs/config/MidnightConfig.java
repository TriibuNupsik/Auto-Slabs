package io.github.andrew6rant.autoslabs.config;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.awt.Color;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** MidnightConfig v2.4.0 by TeamMidnightDust & Motschen
 *  Single class config library - feel free to copy!
 *  Based on https://github.com/Minenash/TinyConfig
 *  Credits to Minenash */

@SuppressWarnings("unchecked")
public abstract class MidnightConfig {
    private static final Pattern INTEGER_ONLY = Pattern.compile("(-?[0-9]*)");
    private static final Pattern DECIMAL_ONLY = Pattern.compile("-?(\\d+\\.?\\d*|\\d*\\.?\\d+|\\.)");
    private static final Pattern HEXADECIMAL_ONLY = Pattern.compile("(-?[#0-9a-fA-F]*)");

    private static final List<EntryInfo> entries = new ArrayList<>();

    public static class EntryInfo {
        Field field;
        Class<?> dataType;
        int width, listIndex;
        boolean centered;
        Object defaultValue, value, function;
        String modid, tempValue;
        boolean inLimits = true;
        Text name, error;
        ClickableWidget actionButton;
        // Tab tab; // Omitted: no tab support in this version

        public void setValue(Object value) {
            if (this.field.getType() != List.class) {
                this.value = value;
                this.tempValue = value.toString();
            } else {
                writeList(this.listIndex, value);
                this.tempValue = toTemporaryValue();
            }
        }
        public String toTemporaryValue() {
            if (this.field.getType() != List.class) return this.value.toString();
            else try { return ((List<?>) this.value).get(this.listIndex).toString(); } catch (Exception ignored) {return "";}
        }
        public <T> void writeList(int index, T value) {
            var list = (List<T>) this.value;
            if (index >= list.size()) list.add(value);
            else list.set(index, value);
        }
    }

    public static final Map<String, Class<?>> configClass = new HashMap<>();
    private static Path path;

    private static final Gson gson = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT).excludeFieldsWithModifiers(Modifier.PRIVATE)
            .addSerializationExclusionStrategy(new HiddenAnnotationExclusionStrategy())
            .setPrettyPrinting().create();

    public static void init(String modid, Class<?> config) {
        path = FabricLoader.getInstance().getConfigDir().resolve(modid + ".json");
        configClass.put(modid, config);

        for (Field field : config.getFields()) {
            EntryInfo info = new EntryInfo();
            if ((field.isAnnotationPresent(Entry.class) || field.isAnnotationPresent(Comment.class)) && !field.isAnnotationPresent(Server.class) && !field.isAnnotationPresent(Hidden.class)
                    && FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)
                initClient(modid, field, info);
            if (field.isAnnotationPresent(Entry.class))
                try { info.defaultValue = field.get(null);
                } catch (IllegalAccessException ignored) {}
        }
        try { gson.fromJson(Files.newBufferedReader(path), config); }
        catch (Exception e) { write(modid); }

        for (EntryInfo info : entries) {
            if (info.field.isAnnotationPresent(Entry.class)) try {
                info.value = info.field.get(null);
                info.tempValue = info.toTemporaryValue();
            } catch (IllegalAccessException ignored) {}
        }
    }
    @Environment(EnvType.CLIENT)
    private static void initClient(String modid, Field field, EntryInfo info) {
        info.dataType = getUnderlyingType(field);
        Entry e = field.getAnnotation(Entry.class);
        Comment c = field.getAnnotation(Comment.class);
        info.width = e != null ? e.width() : 0;
        info.field = field; info.modid = modid;
        boolean requiredModLoaded = true;
        // No requiredMod logic, as we don't have PlatformFunctions.isModLoaded
        if (e != null) {
            if (!e.name().isEmpty()) info.name = Text.translatable(e.name());
            if (info.dataType == int.class) textField(info, Integer::parseInt, INTEGER_ONLY, (int) e.min(), (int) e.max(), true);
            else if (info.dataType == float.class) textField(info, Float::parseFloat, DECIMAL_ONLY, (float) e.min(), (float) e.max(), false);
            else if (info.dataType == double.class) textField(info, Double::parseDouble, DECIMAL_ONLY, e.min(), e.max(), false);
            else if (info.dataType == String.class) textField(info, String::length, null, Math.min(e.min(), 0), Math.max(e.max(), 1), true);
            else if (info.dataType == boolean.class) {
                Function<Object, Text> func = value -> Text.translatable((Boolean) value ? "gui.yes" : "gui.no").formatted((Boolean) value ? Formatting.GREEN : Formatting.RED);
                info.function = new AbstractMap.SimpleEntry<ButtonWidget.PressAction, Function<Object, Text>>(button -> {
                    info.setValue(!(Boolean) info.value); button.setMessage(func.apply(info.value));
                }, func);
            } else if (info.dataType.isEnum()) {
                List<?> values = Arrays.asList(field.getType().getEnumConstants());
                Function<Object, Text> func = value -> {
                    String translationKey = modid + ".midnightconfig.enum." + info.dataType.getSimpleName() + "." + info.toTemporaryValue();
                    return I18n.hasTranslation(translationKey) ? Text.translatable(translationKey) : Text.literal(info.toTemporaryValue());
                };
                info.function = new AbstractMap.SimpleEntry<ButtonWidget.PressAction, Function<Object, Text>>(button -> {
                    int index = values.indexOf(info.value) + 1;
                    info.value = values.get(index >= values.size() ? 0 : index); button.setMessage(func.apply(info.value));
                }, func);
            }
        } else if (c != null) {
            info.centered = c.centered();
        }
        if (requiredModLoaded) entries.add(info);
    }
    public static Class<?> getUnderlyingType(Field field) {
        Class<?> rawType = field.getType();
        if (field.getType() == List.class)
            rawType = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        try { return (Class<?>) rawType.getField("TYPE").get(null);
        } catch (NoSuchFieldException | IllegalAccessException ignored) { return rawType; }
    }
    public static Tooltip getTooltip(EntryInfo info, boolean isButton) {
        String key = info.modid + ".midnightconfig."+info.field.getName()+(!isButton ? ".label" : "" )+".tooltip";
        return Tooltip.of(isButton && info.error != null ? info.error : I18n.hasTranslation(key) ? Text.translatable(key) : Text.empty());
    }

    private static void textField(EntryInfo info, Function<String,Number> f, Pattern pattern, double min, double max, boolean cast) {
        boolean isNumber = pattern != null;
        info.function = (BiFunction<TextFieldWidget, ButtonWidget, Predicate<String>>) (t, b) -> s -> {
            s = s.trim();
            if (!(s.isEmpty() || !isNumber || pattern.matcher(s).matches())) return false;

            Number value = 0; boolean inLimits = false; info.error = null;
            if (!(isNumber && s.isEmpty()) && !s.equals("-") && !s.equals(".")) {
                try { value = f.apply(s); } catch(NumberFormatException e){ return false; }
                inLimits = value.doubleValue() >= min && value.doubleValue() <= max;
                info.error = inLimits? null : Text.literal(value.doubleValue() < min ?
                        "§cMinimum " + (isNumber? "value" : "length") + (cast? " is " + (int)min : " is " + min) :
                        "§cMaximum " + (isNumber? "value" : "length") + (cast? " is " + (int)max : " is " + max)).formatted(Formatting.RED);
                t.setTooltip(getTooltip(info, true));
            }

            info.tempValue = s;
            t.setEditableColor(inLimits? 0xFFFFFFFF : 0xFFFF7777);
            info.inLimits = inLimits;
            b.active = entries.stream().allMatch(e -> e.inLimits);

            if (inLimits) {
                info.setValue(isNumber ? value : s);
            }

            if (info.field.getAnnotation(Entry.class).isColor()) {
                if (!s.contains("#")) s = '#' + s;
                if (!HEXADECIMAL_ONLY.matcher(s).matches()) return false;
                try { info.actionButton.setMessage(Text.literal("⬛").setStyle(Style.EMPTY.withColor(Color.decode(info.tempValue).getRGB())));
                } catch (Exception ignored) {}
            }
            return true;
        };
    }
    public static void write(String modid) {
        path = FabricLoader.getInstance().getConfigDir().resolve(modid + ".json");
        try {
            if (!Files.exists(path)) Files.createFile(path);
            Files.write(path, gson.toJson(configClass.get(modid).getDeclaredConstructor().newInstance()).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Environment(EnvType.CLIENT)
    public static Screen getScreen(Screen parent, String modid) {
        return new MidnightConfigScreen(parent, modid);
    }
    @Environment(EnvType.CLIENT)
    public static class MidnightConfigScreen extends Screen {
        protected MidnightConfigScreen(Screen parent, String modid) {
            super(Text.translatable(modid + ".midnightconfig." + "title"));
            this.parent = parent; this.modid = modid;
            this.translationPrefix = modid + ".midnightconfig.";
            loadValues();
        }
        public final String translationPrefix, modid;
        public final Screen parent;
        public MidnightConfigListWidget list;
        public ButtonWidget done;
        public double scrollProgress = 0d;

        @Override
        public void tick() {
            super.tick();
            scrollProgress = list.getScrollAmount();
            for (EntryInfo info : entries) try {info.field.set(null, info.value);} catch (IllegalAccessException ignored) {}
            updateButtons();
        }
        public void updateButtons() {
            if (this.list != null) {
                for (ButtonEntry entry : this.list.children()) {
                    if (entry.buttons != null && entry.buttons.size() > 1) {
                        if (entry.buttons.get(0) instanceof ClickableWidget widget)
                            if (widget.isFocused() || widget.isHovered()) widget.setTooltip(getTooltip(entry.info, true));
                        if (entry.buttons.get(1) instanceof ButtonWidget button)
                            button.active = !Objects.equals(entry.info.value.toString(), entry.info.defaultValue.toString());
        }}}}
        public void loadValues() {
            try { gson.fromJson(Files.newBufferedReader(path), configClass.get(modid)); }
            catch (Exception e) { write(modid); }

            for (EntryInfo info : entries) {
                if (info.field.isAnnotationPresent(Entry.class))
                    try { info.value = info.field.get(null); info.tempValue = info.toTemporaryValue();
                    } catch (IllegalAccessException ignored) {}
            }
        }
        @Override
        public void close() {
            loadValues();
            Objects.requireNonNull(client).setScreen(parent);
        }
        @Override
        public void init() {
            super.init();
            this.addDrawableChild(ButtonWidget.builder(ScreenTexts.CANCEL, button -> this.close()).dimensions(this.width / 2 - 154, this.height - 26, 150, 20).build());
            done = this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, (button) -> {
                for (EntryInfo info : entries) if (info.modid.equals(modid)) try { info.field.set(null, info.value); } catch (IllegalAccessException ignored) {}
                write(modid);
                Objects.requireNonNull(client).setScreen(parent);
            }).dimensions(this.width / 2 + 4, this.height - 26, 150, 20).build());

            this.list = new MidnightConfigListWidget(this.client, this.width, this.height - 57, 24, 25);
            this.addSelectableChild(this.list); fillList();
        }
        public void fillList() {
            for (EntryInfo info : entries) {
                if (info.modid.equals(modid)) {
                    Text name = Objects.requireNonNullElseGet(info.name, () -> Text.translatable(translationPrefix + info.field.getName()));
                    ButtonWidget resetButton = ButtonWidget.builder(Text.literal("Reset").formatted(Formatting.RED), (button -> {
                        info.value = info.defaultValue; info.listIndex = 0;
                        info.tempValue = info.toTemporaryValue();
                        list.clear(); fillList();
                    })).dimensions(width - 205, 0, 40, 20).build();

                    if (info.function != null) {
                        ClickableWidget widget;
                        Entry e = info.field.getAnnotation(Entry.class);

                        if (info.function instanceof Map.Entry) {
                            var values = (Map.Entry<ButtonWidget.PressAction, Function<Object, Text>>) info.function;
                            if (info.dataType.isEnum())
                                values.setValue(value -> Text.translatable(translationPrefix + "enum." + info.field.getType().getSimpleName() + "." + info.value.toString()));
                            widget = ButtonWidget.builder(values.getValue().apply(info.value), values.getKey()).dimensions(width - 185, 0, 150, 20).tooltip(getTooltip(info, true)).build();
                        }
                        else if (e.isSlider())
                            widget = new MidnightSliderWidget(width - 185, 0, 150, 20, Text.of(info.tempValue), (Double.parseDouble(info.tempValue) - e.min()) / (e.max() - e.min()), info);
                        else widget = new TextFieldWidget(textRenderer, width - 185, 0, 150, 20, Text.empty());

                        if (widget instanceof TextFieldWidget textField) {
                            textField.setMaxLength(info.width); textField.setText(info.tempValue);
                            Predicate<String> processor = ((BiFunction<TextFieldWidget, ButtonWidget, Predicate<String>>) info.function).apply(textField, done);
                            textField.setTextPredicate(processor);
                        }
                        widget.setTooltip(getTooltip(info, true));
                        this.list.addButton(List.of(widget, resetButton), name, info);
                    } else this.list.addButton(List.of(), name, info);
                } list.setScrollAmount(scrollProgress);
                updateButtons();
            }
        }
        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context,mouseX,mouseY,delta);
            this.list.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFF);
        }
    }
    @Environment(EnvType.CLIENT)
    public static class MidnightConfigListWidget extends ElementListWidget<ButtonEntry> {
        public MidnightConfigListWidget(MinecraftClient client, int width, int height, int y, int itemHeight) { super(client, width, height, y, itemHeight); }
        @Override public int getScrollbarX() { return this.width -7; }
        public void addButton(List<ClickableWidget> buttons, Text text, EntryInfo info) { this.addEntry(new ButtonEntry(buttons, text, info)); }
        public void clear() { this.clearEntries(); }
        @Override public int getRowWidth() { return 10000; }
    }
    public static class ButtonEntry extends ElementListWidget.Entry<ButtonEntry> {
        private static final TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        public final Text text;
        public final List<ClickableWidget> buttons;
        public final EntryInfo info;
        public boolean centered = false;

        public ButtonEntry(List<ClickableWidget> buttons, Text text, EntryInfo info) {
            this.buttons = buttons; this.text = text; this.info = info;
            if (info != null) this.centered = info.centered;
        }
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            buttons.forEach(b -> { b.setY(y); b.render(context, mouseX, mouseY, tickDelta);});
            if (text != null && (!text.getString().contains("spacer") || !buttons.isEmpty())) {
                if (centered) context.drawTextWithShadow(textRenderer, text, (int)(MinecraftClient.getInstance().getWindow().getScaledWidth() / 2f - (textRenderer.getWidth(text) / 2f)), y + 5, 0xFFFFFF);
                else context.drawTextWithShadow(textRenderer, text, 12, y + 5, 0xFFFFFF);
            }
        }
        public List<? extends Element> children() {return buttons;}
        public List<? extends Selectable> selectableChildren() {return buttons;}
    }
    public static class MidnightSliderWidget extends SliderWidget {
        private final EntryInfo info; private final Entry e;
        public MidnightSliderWidget(int x, int y, int width, int height, Text text, double value, EntryInfo info) {
            super(x, y, width, height, text, value);
            this.e = info.field.getAnnotation(Entry.class);
            this.info = info;
        }

        @Override
        public void updateMessage() { this.setMessage(Text.of(info.tempValue)); }

        @Override
        public void applyValue() {
            if (info.dataType == int.class) info.setValue(((Number) (e.min() + value * (e.max() - e.min()))).intValue());
            else if (info.dataType == double.class) info.setValue(Math.round((e.min() + value * (e.max() - e.min())) * (double) e.precision()) / (double) e.precision());
            else if (info.dataType == float.class) info.setValue(Math.round((e.min() + value * (e.max() - e.min())) * (float) e.precision()) / (float) e.precision());
        }
    }
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Entry {
        int width() default 400;
        double min() default Double.MIN_NORMAL;
        double max() default Double.MAX_VALUE;
        String name() default "";
        boolean isColor() default false;
        boolean isSlider() default false;
        int precision() default 100;
    }
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) public @interface Client {}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) public @interface Server {}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) public @interface Hidden {}
    @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) public @interface Comment {
        boolean centered() default false;
    }
    public static class HiddenAnnotationExclusionStrategy implements ExclusionStrategy {
        public boolean shouldSkipClass(Class<?> clazz) { return false; }
        public boolean shouldSkipField(FieldAttributes fieldAttributes) { return fieldAttributes.getAnnotation(Entry.class) == null; }
    }
}