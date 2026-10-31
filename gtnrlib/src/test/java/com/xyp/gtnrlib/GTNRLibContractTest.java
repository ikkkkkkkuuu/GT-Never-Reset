package com.xyp.gtnrlib;

import static com.xyp.gtnrlib.testing.ContractAssertions.check;
import static com.xyp.gtnrlib.testing.ContractAssertions.finish;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.xyp.gtnrlib.client.GTNRLibClient;

public class GTNRLibContractTest {

    public static final Path SOURCES = Paths.get("src/main/java/com/xyp/gtnrlib");
    public static final Path RESOURCES = Paths.get("src/main/resources/assets/gtnrlib");

    public static void main(String[] args) throws Exception {
        run();
        finish("GTNRLib");
    }

    public static void run() throws Exception {
        ClassLoader loader = GTNRLibContractTest.class.getClassLoader();
        check(
            loader.getResource("com/science/gtnl/ScienceNotLeisure.class") == null,
            "library contracts run without GTNL classes");
        check(
            loader.getResource("gregtech/api/GregTechAPI.class") == null,
            "library contracts run without GregTech classes");
        GTNRLibClient.connectConfig();
        HudContracts.run();
        WheelContracts.run();
        LayoutContracts.run();
        ThemeContracts.run();
        FxContracts.run();
        RegistryContracts.run();
    }

}
