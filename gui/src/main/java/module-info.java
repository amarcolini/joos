module joos.gui {
    requires javafx.controls;
    requires javafx.graphics;
    requires tornadofx;
    requires transitive kotlin.stdlib;
    requires kotlin.reflect;
    uses kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader;
    uses kotlin.reflect.jvm.internal.impl.resolve.ExternalOverridabilityCondition;
    uses kotlin.reflect.jvm.internal.impl.util.ModuleVisibilityHelper;
    requires io.github.classgraph;
    exports com.amarcolini.joos.gui;
    exports com.amarcolini.joos.gui.rendering;
    exports com.amarcolini.joos.gui.style;
    exports com.amarcolini.joos.gui.trajectory;
}