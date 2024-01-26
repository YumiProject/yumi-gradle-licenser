plugins {
	id("dev.yumi.gradle.licenser").version("1.0.+")
	id("com.gradle.plugin-publish").version("1.2.0")
	id("maven-publish")
	id("signing")
}

group = "dev.yumi"
version = "1.1.0"
val javaVersion = 17

repositories {
	mavenCentral()
}

// Add a source set for the functional test suite.
val functionalTest: SourceSet by sourceSets.creating

dependencies {
	compileOnly(libs.jetbrains.annotations)
	api(libs.jgit)
	// Use JUnit Jupiter for testing.
	testImplementation(libs.junit.jupiter)
	testRuntimeOnly(libs.junit.launcher)
}

gradlePlugin {
	website = "https://github.com/YumiProject/yumi-gradle-licenser"
	vcsUrl = "https://github.com/YumiProject/yumi-gradle-licenser"

	// Define the plugin.
	plugins {
		create("yumi_gradle_licenser") {
			id = "dev.yumi.gradle.licenser"
			displayName = "Yumi Gradle Licenser"
			description = "A plugin to automatically manage license headers in project files, designed to be flexible to easily support many use cases like having different header kinds for different files."
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
	from("LICENSE") {
		rename { "${it}_${base.archivesName.get()}" }
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
