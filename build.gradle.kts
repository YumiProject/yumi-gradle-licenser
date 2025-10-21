plugins {
	id("dev.yumi.gradle.licenser") version "2.1.+"
	id("com.gradle.plugin-publish") version "1.3.1"

	kotlin("jvm") version "2.0.0"

	`maven-publish`
	signing
}

val javaVersion = Integer.parseInt(project.property("java_version").toString())

repositories {
	mavenCentral()
}

// Add a source set for the functional test suite.
val functionalTest: SourceSet by sourceSets.creating

dependencies {
	compileOnly(libs.jetbrains.annotations)
	api(libs.jgit)
	// Use JUnit Jupiter for testing.
	val junitPlatform = platform(libs.junit.bom)
	testImplementation(junitPlatform)
	testImplementation(libs.junit.jupiter)
	testRuntimeOnly(libs.junit.launcher)
	"functionalTestImplementation"(junitPlatform)
	"functionalTestImplementation"(libs.junit.jupiter)
	"functionalTestRuntimeOnly"(libs.junit.launcher)
}

gradlePlugin {
	website = "https://github.com/YumiProject/yumi-gradle-licenser"
	vcsUrl = "https://github.com/YumiProject/yumi-gradle-licenser"

	// Define the plugin.
	plugins {
		create("yumi_gradle_licenser") {
			id = "dev.yumi.gradle.licenser"
			displayName = "Yumi Gradle Licenser"
			description =
				"A plugin to automatically manage license headers in project files, designed to be flexible to easily support many use cases like having different header kinds for different files."
			tags = listOf("licenser", "licensing", "licenses", "license-header")
			implementationClass = "dev.yumi.gradle.licenser.YumiLicenserGradlePlugin"
		}
	}

	testSourceSets(functionalTest)
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(javaVersion))
	}

	withSourcesJar()
	withJavadocJar()

	testResultsDir.set(layout.buildDirectory.dir("junit-xml"))
}

kotlin {
	// Require explicit visibility/type definitions for public types, among other things
	explicitApi()
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.isDeprecation = true
	options.release.set(javaVersion)
}

tasks.withType<Javadoc>().configureEach {
	options {
		this as StandardJavadocDocletOptions

		addStringOption("Xdoclint:all,-missing", "-quiet")
	}
}

tasks.jar {
	inputs.property("archives_name", base.archivesName)

	from("LICENSE") {
		rename { "${it}_${inputs.properties["archives_name"]}" }
	}
}

license {
	rule(file("codeformat/HEADER"))
	exclude("scenarios/**")
}

signing {
	val signingKeyId: String? by project
	val signingKey: String? by project
	val signingPassword: String? by project
	isRequired = signingKeyId != null && signingKey != null && signingPassword != null
	useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
}

configurations["functionalTestImplementation"].extendsFrom(configurations.testImplementation.get())

val functionalTestTask = tasks.register<Test>("functionalTest") {
	group = "verification"
	testClassesDirs = functionalTest.output.classesDirs
	classpath = functionalTest.runtimeClasspath
}

tasks.check {
	// Run the functional tests as part of `check`.
	dependsOn(functionalTestTask)
}

tasks.withType<Test>().configureEach {
	// Using JUnitPlatform for running tests
	useJUnitPlatform()
	systemProperty("yumi.gradle.licenser.debug", System.getProperty("yumi.gradle.licenser.debug"))

	testLogging {
		events("passed")
	}
}

publishing {
	repositories {
		mavenLocal()
	}
}
