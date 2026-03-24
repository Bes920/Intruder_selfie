import java.io.File
import java.util.Scanner
import java.util.Locale
import kotlin.system.exitProcess

// --- OOP: Abstraction & Interface ---
interface GradeProcessor {
    fun process()
}

// --- OOP: Data Class (Encapsulation) ---
data class Student(
    val name: String,
    val scores: List<Double>
) {
    // High-order property using average()
    val average: Double = if (scores.isNotEmpty()) scores.average() else 0.0
    val grade: Char = calculateGrade(average)
    val status: String = if (grade == 'F') "Fail" else "Pass"

    fun generateReport(): String = """
        --- Grade Report ---
        Student: $name
        Scores: ${scores.joinToString()}
        Average Score: ${String.format(Locale.US, "%.2f", average)}
        Grade: $grade
        Status: $status
        --------------------
    """.trimIndent()

    fun toCsvRow(): String = "$name,${String.format(Locale.US, "%.2f", average)},$grade,$status,${scores.joinToString(",")}"

    companion object {
        fun calculateGrade(average: Double): Char = when {
            average >= 90.0 -> 'A'
            average >= 80.0 -> 'B'
            average >= 70.0 -> 'C'
            average >= 60.0 -> 'D'
            else -> 'F'
        }
    }
}

// --- OOP: Implementation of logic in a Class ---
class StudentGradeCalculator(private val scanner: Scanner) : GradeProcessor {
    
    override fun process() {
        while (true) {
            displayMenu()
            val choice = scanner.nextLine().trim()
            val students = mutableListOf<Student>()

            when (choice) {
                "1" -> enterGradesManually()?.let { students.add(it) }
                "2" -> students.addAll(importFromCsv())
                "3" -> {
                    println("Exiting...")
                    exitProcess(0)
                }
                else -> println("Invalid option.")
            }

            if (students.isNotEmpty()) {
                // Lambda: forEach used to process the list
                println("\n--- Processing Complete ---")
                students.forEach { student -> println(student.generateReport()) }
                
                offerExport(students)
            }
        }
    }

    private fun displayMenu() {
        println("\n--- Grade Calculator Menu ---")
        println("1. Manual Entry")
        println("2. CSV Import")
        println("3. Exit")
        print("Choose: ")
    }

    private fun enterGradesManually(): Student? {
        print("Name: ")
        val name = scanner.nextLine().trim().ifEmpty { "Unknown" }
        val scores = mutableListOf<Double>()
        
        // High-order concept: Reading until 'done'
        while (true) {
            print("Score (or 'done'): ")
            val input = scanner.nextLine().trim()
            if (input.equals("done", ignoreCase = true)) break
            
            // Lambda & Safe call
            input.toDoubleOrNull()?.let { score ->
                if (score >= 0) scores.add(score) else println("Non-negative please.")
            } ?: println("Invalid score.")
        }
        return if (scores.isNotEmpty()) Student(name, scores) else null
    }

    private fun importFromCsv(): List<Student> {
        print("File Path: ")
        val file = File(scanner.nextLine().trim())
        if (!file.exists()) return emptyList<Student>().also { println("File not found.") }

        return try {
            // High-Order: useLines ensures file is closed automatically
            file.useLines { lines ->
                // Lambda: mapNotNull filters and transforms in one go
                lines.mapNotNull { line ->
                    val parts = line.split(',')
                    if (parts.size >= 2) {
                        val name = parts[0].trim()
                        val scores = parts.drop(1).mapNotNull { it.trim().toDoubleOrNull() }
                        if (scores.isNotEmpty()) Student(name, scores) else null
                    } else null
                }.toList()
            }
        } catch (e: Exception) {
            println("Error: ${e.message}")
            emptyList()
        }
    }

    private fun offerExport(students: List<Student>) {
        print("\nExport to CSV? (y/n): ")
        if (scanner.nextLine().trim().equals("y", ignoreCase = true)) {
            print("Export Path (e.g. /path/to/file.csv): ")
            val path = scanner.nextLine().trim()
            saveToCsv(students, path)
        }
    }

    private fun saveToCsv(students: List<Student>, path: String) {
        try {
            File(path).printWriter().use { out ->
                out.println("Name,Average Score,Grade,Status,Scores...")
                // Lambda: Iterate and print
                students.forEach { out.println(it.toCsvRow()) }
            }
            println("Export successful to $path")
        } catch (e: Exception) {
            println("Export failed: ${e.message}")
        }
    }
}

fun main() {
    val scanner = Scanner(System.`in`).useLocale(Locale.US)
    // OOP: Instantiating and using the processor
    val calculator = StudentGradeCalculator(scanner)
    calculator.process()
}
