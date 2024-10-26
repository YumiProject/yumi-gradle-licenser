# Yumi Licenser Gradle Plugin

![Java 17](https://img.shields.io/badge/language-Java%2017-9115ff.svg?style=flat-square)
[![GitHub license](https://img.shields.io/github/license/YumiProject/yumi-gradle-licenser?style=flat-square)](https://raw.githubusercontent.com/YumiProject/yumi-gradle-licenser/main/LICENSE)
![Version](https://img.shields.io/github/v/tag/YumiProject/yumi-gradle-licenser?label=version&style=flat-square)

A license header manager plugin for Gradle.

The goal of this plugin is to ensure that the source files contains a predefined license header determined by some rules,
and optionally to generate them automatically through a Gradle task.

This plugin is different from other similar plugins as it manages more easily projects bound to multiple licenses.

## Usage

For a project you need to apply the plugin to your project:

```groovy
plugins {
	id "dev.yumi.gradle.licenser" version "2.0.+"
}
```

This plugin is available on the Gradle Plugin Portal.

## Tasks

|      Name       | Description                                                 |
|:---------------:|:------------------------------------------------------------|
| `applyLicenses` | Updates the license headers in the selected source files.   |
| `checkLicenses` | Verifies the license headers for the selected source files. |

More tasks are available for each source set.

## Configuration

The plugin can be configured using the `license` extension on the project.

```groovy
license {
	// Add a license header rule, at least one must be present.
	rule(file("codeformat/HEADER"))

	// Exclude/include certain file types, defaults are provided to easily deal with Java/Kotlin projects.
	include("**/*.java") // Include Java files into the file resolution.
	exclude("**/*.properties") // Exclude properties files from the file resolution.
}
```

These configuration options are not final as more may come in the future.
Feel free to request changes through the issue tracker if the current options don't satisfy your needs.

## License header rule

A license header rule is a file containing a template-like license header.
It will define how the license headers should look like, with the ability to resolve some variables like the file creation year,
and the ability to make some lines optional.

Multiple license header rules can be given to the plugin, the plugin will then attempt to identify the
license headers to use by trying each given rules in the same order as the definition of those rules.

An example file can be found in this repository: [`codeformat/HEADER`](./codeformat/HEADER).

### Format

A license header rule contains the license header text, along with pre-processor statements and variable tokens.

Variable tokens are parsed by matching any characters between `${` and `}`. For example: `${VARIABLE}`.

File example:
```
License text ${CREATION_YEAR}.

#optional
Optional line
#end

#year_selection file
```

#### Variables

There are the default variables:

|      Name       |                                     Default Value                                     | Description                                                                   |
|:---------------:|:-------------------------------------------------------------------------------------:|:------------------------------------------------------------------------------|
| `CREATION_YEAR` | Current year if the year selection is `file`, or the project creation year otherwise. | The year the file or project was created (depends on `year_selection` value). |
|   `FILE_NAME`   |                             The name of the current file.                             | The name of the current file.                                                 |

More variables can be defined with the usage of the `type` pre-processor.

#### Pre-Processor Statements

##### `optional`

Optional text is encapsulated between an `#optional` statement and ends with a `#end` statement.

##### `type`

The type pre-processor statement allows to define a new variable with a type:
`#type <VARIABLE NAME> <VARIABLE TYPE>`

##### `year_selection`

Defines how the creation and modification years for each file should be gathered: per-project or per-file:
`#year_selection <file|project>`.

The default value is `project`.
