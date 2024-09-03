package com.example.plugins

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.postgresql.ds.PGSimpleDataSource
import java.time.LocalDate
import java.util.*

@Serializable
data class Student(
    val id: Int = 0,
    val firstName: String,
    val lastName: String,
    val middleName: String?,
    val birthDate: String,
    val groupName: String,
)


fun Application.setupDB(): PGSimpleDataSource {
    val dbUrl = environment.config.property("database.url").getString()
    val dbUser = environment.config.property("database.user").getString()
    val dbPassword = environment.config.property("database.password").getString()

    val db = PGSimpleDataSource().apply {
        setURL(dbUrl)
        user = dbUser
        password = dbPassword
    }
    val connection = db.connection

    connection.prepareStatement(
        """
        CREATE TABLE IF NOT EXISTS students (
            id SERIAL PRIMARY KEY,
            first_name VARCHAR(50) NOT NULL,
            last_name VARCHAR(50) NOT NULL,
            middle_name VARCHAR(50),
            birth_date DATE NOT NULL,
            group_name VARCHAR(50) NOT NULL
        );
        """.trimIndent()
    ).executeUpdate()
    return db
}

fun Application.configureRouting() {
    val db = setupDB()



    routing {
        get("/") {
            println({}.javaClass.getResource("/index.html")?.readText().toString())
            call.respondText({}.javaClass.getResource("/index.html")?.readText().toString(), ContentType.Text.Html)

        }
        route("/students") {
            post("/add") {
                val student = call.receive<Student>()
                val connection = db.connection
                connection.use {
                    val statement = connection.prepareStatement(
                        "INSERT INTO students (first_name, last_name, middle_name, birth_date, group_name) VALUES (?, ?, ?, ?, ?) RETURNING id"
                    )
                    statement.setString(1, student.firstName)
                    statement.setString(2, student.lastName)
                    statement.setString(3, student.middleName)
                    statement.setDate(4, java.sql.Date.valueOf(student.birthDate))
                    statement.setString(5, student.groupName)
                    val resultSet = statement.executeQuery()
                    if (resultSet.next()) {
                        call.respond(HttpStatusCode.Created, resultSet.getString("id"))
                    } else {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to add student")
                    }
                }
            }
            delete("/{id}") {
                val id = call.parameters["id"]!!.toInt()
                val connection = db.connection
                connection.use {
                    val statement = connection.prepareStatement("DELETE FROM students WHERE id = ?")
                    statement.setInt(1, id)
                    val rowsAffected = statement.executeUpdate()
                    if (rowsAffected > 0) {
                        call.respond(HttpStatusCode.OK, "Student deleted")
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Student not found")
                    }
                }
            }

            get("/") {
                val connection = db.connection
                connection.use {
                    val statement = connection.prepareStatement("SELECT * FROM students")
                    val resultSet = statement.executeQuery()
                    val students = mutableListOf<Student>()
                    while (resultSet.next()) {
                        students.add(
                            Student(
                                id = resultSet.getInt("id"),
                                firstName = resultSet.getString("first_name"),
                                lastName = resultSet.getString("last_name"),
                                middleName = resultSet.getString("middle_name"),
                                birthDate = resultSet.getDate("birth_date").toString(),
                                groupName = resultSet.getString("group_name")
                            )
                        )
                    }
                    call.respond(HttpStatusCode.OK, students)
                }
            }
        }
    }
}
