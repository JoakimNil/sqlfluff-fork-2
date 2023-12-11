package co.anbora.labs.sqlfluff.lint

import co.anbora.labs.sqlfluff.ide.runner.SqlFluffLintRunner
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiFile
import java.util.concurrent.CompletableFuture
import kotlin.io.path.pathString

object CustomLinter: Linter() {

    override fun buildCommandLineArgs(
        python: String,
        lint: String,
        lintOptions: String,
        file: PsiFile,
        document: Document
    ): SqlFluffLintRunner.Param {
        // Create a CompletableFuture
        val future = CompletableFuture<Unit>()

        // Run the save operation on the EDT
        ApplicationManager.getApplication().invokeLater {
            try {
                // Save the currently unsaved changes
                val fileDocumentManager = FileDocumentManager.getInstance()
                fileDocumentManager.saveDocument(document)
                future.complete(Unit) // Complete the future upon successful save
            } catch (e: Exception) {
                future.completeExceptionally(e) // Complete exceptionally in case of error
            }
        }

        // Wait for the future (i.e. save operation) to complete
        future.get()

        // Now run the linter command
        val nioFile = file.virtualFile.toNioPath()
        return SqlFluffLintRunner.Param(
            execPath = python,
            extraArgs = listOf(lint, LINT_COMMAND, nioFile.pathString, *lintOptions.split(" ").toTypedArray())
        )
    }
}
