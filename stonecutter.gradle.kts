plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.21.1-neoforge"

stonecutter parameters {
    constants.match(node.metadata.project.substringAfterLast('-'), "fabric", "forge", "neoforge")

    replacements.string(eval(node.metadata.version, "<1.19.3")) {
        replace("import net.minecraft.core.registries.Registries;", "import net.minecraft.core.Registry;")
        replace("(Registries.BLOCK,", "(Registry.BLOCK_REGISTRY,")
        replace("(Registries.ITEM,", "(Registry.ITEM_REGISTRY,")
    }

    replacements.string(eval(node.metadata.version, "<1.17")) {
        replace("List.of(", "com.google.common.collect.ImmutableList.of(")
        replace("List.copyOf(", "com.google.common.collect.ImmutableList.copyOf(")
        replace("Map.copyOf(", "com.google.common.collect.ImmutableMap.copyOf(")
        replace(".toList()", ".collect(com.google.common.collect.ImmutableList.toImmutableList())")
    }
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.BIN
}
