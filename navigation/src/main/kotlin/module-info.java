module joos.navigation {
    requires com.fasterxml.jackson.dataformat.yaml;
    requires transitive kotlin.stdlib;
    requires com.fasterxml.jackson.kotlin;
    requires com.fasterxml.jackson.databind;
    requires commons.math3;
    exports com.amarcolini.joos.control;
    exports com.amarcolini.joos.drive;
    exports com.amarcolini.joos.followers;
    exports com.amarcolini.joos.geometry;
    exports com.amarcolini.joos.kinematics;
    exports com.amarcolini.joos.localization;
    exports com.amarcolini.joos.path;
    exports com.amarcolini.joos.profile;
    exports com.amarcolini.joos.trajectory;
    exports com.amarcolini.joos.trajectory.config;
    exports com.amarcolini.joos.trajectory.constraints;
    exports com.amarcolini.joos.util;
}