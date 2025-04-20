package com.si.main.searchinsights.config

import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Configuration
class SwaggerConfig {

    @Bean
    fun operationCustomizer(): OperationCustomizer {
        return OperationCustomizer { operation, handlerMethod ->
            if (handlerMethod.method.name == "sendDailyMailing") {
                val defaultDate = LocalDate.now().minusDays(3).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                operation.parameters?.forEach { parameter ->
                    if (parameter.name == "fromDate" || parameter.name == "toDate") {
                        // example 값만 설정해도 스웨거에서는 Try it out 눌렀을 때 자동으로 채워짐
                        parameter.example = defaultDate

                        if (parameter.schema is Schema<*>) {
                            (parameter.schema as Schema<*>).example = defaultDate
                        }
                    }
                }
            }
            operation
        }
    }
}