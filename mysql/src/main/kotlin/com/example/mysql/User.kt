package com.example.mysql

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import java.sql.SQLType
import java.sql.Types
import java.time.LocalDateTime

class User(
    private val name: String,
    private val age: Int,
    private val grade: String,
    private val createdAt: LocalDateTime
) {

    fun toSqlParam(): SqlParameterSource{
        return MapSqlParameterSource()
            .addValue("name", name)
            .addValue("age",age)
            .addValue("grade",grade)
            .addValue("createdAt",createdAt)
    }
}