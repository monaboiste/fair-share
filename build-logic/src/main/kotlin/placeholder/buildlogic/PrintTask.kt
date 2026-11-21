package placeholder.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

abstract class PrintTask : DefaultTask() {

    @get:Input
    abstract val lines: ListProperty<String>

    @TaskAction
    fun print() {
        lines.get().forEach(logger::quiet)
    }
}

fun <T : Task> TaskContainer.printAfter(
    target: TaskProvider<T>,
    vararg lines: Provider<String>,
) {
    printAfter(target.name, *lines)
}

fun TaskContainer.printAfter(
    targetName: String,
    vararg lines: Provider<String>,
) {
    val printer = register<PrintTask>("${targetName}PrintAfter") {
        lines.forEach(this.lines::add)
    }

    matching { it.name == targetName }.configureEach {
        finalizedBy(printer)
    }
}
