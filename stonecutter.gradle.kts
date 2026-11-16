plugins {
    id("dev.kikugie.stonecutter")
}
stonecutter active "1.21.1-neoforge"

stonecutter parameters {
    constants.match(node.metadata.project.substringAfterLast('-'), "fabric", "forge", "neoforge")
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.BIN
}
