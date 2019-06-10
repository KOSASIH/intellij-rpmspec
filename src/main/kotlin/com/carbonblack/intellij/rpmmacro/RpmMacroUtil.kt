package com.carbonblack.intellij.rpmmacro

import com.carbonblack.intellij.rpmmacro.psi.RpmMacroFile
import com.carbonblack.intellij.rpmmacro.psi.RpmMacroMacro
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.io.isFile
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.streams.toList

object RpmMacroUtil {

    private fun String.runCommand(workingDir: File? = null): String? {
        return try {
            val parts = this.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                    .directory(workingDir)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

            proc.waitFor(60, TimeUnit.MINUTES)
            proc.inputStream.bufferedReader().readText()
        } catch(e: IOException) {
            e.printStackTrace()
            null
        }
    }

    val macroPathFiles: Set<VirtualFile> by lazy {
        // Parse the rpm macro paths
        val output = "rpm --showrc".runCommand()
        var paths : List<String> = emptyList()
        if (output != null) {
            val regex = Regex("Macro path: (.*)")
            for (line in output.split("\n")) {
                val match = regex.find(output)?.groups?.get(1)
                if (match != null) {
                    paths = match.value.replace("%{_target}", "x86_64-linux").split(":")
                    break
                }
            }
        }

        // Find files matching macro paths
        val files : MutableSet<VirtualFile> = mutableSetOf()
        for (path in paths) {
            if (path.contains("*")) {
                val start : String = path.substringBeforeLast("/")
                val glob : String = path.substringAfterLast("/")

                val matcher = FileSystems.getDefault().getPathMatcher("glob:$glob")

                files += Files.walk(Paths.get(start))
                        .filter { it: Path? -> it?.let { it.isFile() && matcher.matches(it.fileName) } ?: false }
                        .map { LocalFileSystem.getInstance().findFileByPath(it.toString()) }
                        .toList()
                        .filterNotNull()
            } else {
                val file = LocalFileSystem.getInstance().findFileByPath(path)
                if (file != null) {
                    files += file
                }
            }
        }

        files
    }

    fun findMacros(project: Project, key: String): List<RpmMacroMacro> {
        var result = emptyList<RpmMacroMacro>()
        val virtualFiles = FileTypeIndex.getFiles(RpmMacroFileType, GlobalSearchScope.everythingScope(project))
        val rpmMacroFiles  = virtualFiles.map { PsiManager.getInstance(project).findFile(it) }.filter { it is RpmMacroFile }
        for (file in rpmMacroFiles) {
            val macros = PsiTreeUtil.findChildrenOfType(file, RpmMacroMacro::class.java)
            result = macros.filter { it.name == key }
        }
        return result
    }

    fun findMacros(file: PsiFile): List<RpmMacroMacro> {
        val result = ArrayList<RpmMacroMacro>()
        if (file is RpmMacroFile) {
            val macros = PsiTreeUtil.findChildrenOfType(file, RpmMacroMacro::class.java)
            result.addAll(macros)
        }
        return result
    }
}