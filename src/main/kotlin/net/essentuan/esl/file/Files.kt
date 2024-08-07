package net.essentuan.esl.file

import java.io.File
import java.nio.file.Path

operator fun Path.div(other: String): Path =
    resolve(other)

operator fun Path.div(other: Path): Path =
    resolve(other)

operator fun String.div(other: String): Path =
    Path.of(this, other)

operator fun String.div(other: Path): Path =
    Path.of(this).resolve(other)

operator fun File.div(other: String): Path =
    toPath().resolve(other)

operator fun File.div(other: Path): Path =
    toPath().resolve(other)