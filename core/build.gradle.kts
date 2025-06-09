plugins {
    id("java")
}

dependencies {
    implementation("io.netty:netty-all:4.2.0.Final")
    implementation("org.json:json:20240303")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("io.jsonwebtoken:jjwt-impl:0.12.6")
    implementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation("org.yaml:snakeyaml:2.0")
    implementation("org.slf4j:slf4j-api:2.0.12")
    
    runtimeOnly("org.slf4j:slf4j-simple:2.0.12")
}
