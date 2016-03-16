package org.jetbrains.kotlin.gradle.util

import java.io.File

class ProcessOutput(
        val cmd: List<String>,
        val workingDir: File,
        val exitCode: Int,
        val stdout: String
) {
    val isSuccessful: Boolean
        get() = exitCode == 0

    override fun toString(): String = """
Executing process was ${if (isSuccessful) "successful" else "unsuccessful"}
    Command: ${cmd.joinToString()}
    Working directory: ${workingDir.absolutePath}
    Exit code: $exitCode
    Output: $stdout
"""
}

fun runProcess(cmd: List<String>, workingDir: File): ProcessOutput {
    val builder = ProcessBuilder(cmd)
    builder.directory(workingDir)
    // redirectErrorStream merges stdout and stderr, so it can be get from process.inputStream
    builder.redirectErrorStream(true)

    val process = builder.start()
    // important to read inputStream, otherwise the process may hang on some systems
    val output = process.inputStream!!.bufferedReader().readText()
    val exitCode = process.waitFor()

    return ProcessOutput(cmd, workingDir, exitCode, output)
}

fun createGradleCommand(tailParameters: List<String>): List<String> {
    return if (isWindows())
        listOf("cmd", "/C", "gradlew.bat") + tailParameters
    else
        listOf("/bin/bash", "./gradlew") + tailParameters
}

private fun isWindows(): Boolean = System.getProperty("os.name")!!.contains("Windows")


