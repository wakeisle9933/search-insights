package com.si.main.searchinsights.service

import com.si.main.searchinsights.enum.ReportFrequency
import com.si.main.searchinsights.util.DateUtils
import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import org.apache.commons.io.output.ByteArrayOutputStream
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamSource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.time.LocalDate

@Service
class MailService(
    private val mailSender: JavaMailSender,

    @Value("\${mail.receiver}")
    private val mail: String
) {

    @Throws(MessagingException::class)
    fun sendMail(excelData: ByteArrayOutputStream, fileName: String, frequency: ReportFrequency, customStartDate: String? = null, customEndDate: String? = null) {
        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        val (startDate, endDate) = when (frequency) {
            ReportFrequency.DAILY -> Pair(DateUtils.getFormattedDateBeforeDays(3), DateUtils.getFormattedDateBeforeDays(3))
            ReportFrequency.WEEKLY -> Pair(DateUtils.getFormattedDateBeforeDays(10), DateUtils.getFormattedDateBeforeDays(3))
            ReportFrequency.MONTHLY -> Pair(DateUtils.getFirstDayOfPreviousMonth(), DateUtils.getLastDayOfPreviousMonth())
            ReportFrequency.CUSTOM -> Pair(customStartDate ?: LocalDate.now(), customEndDate ?: LocalDate.now())
        }

        helper.setSubject("${startDate.toString().substring(5)} ~ ${endDate.toString().substring(5)} $frequency Google Search Insights")

        val htmlContent = """
            <html>
                <body>
                    <h1>$frequency Google Search Insights</h1>
                    <p>Report period: $startDate ~ $endDate</p>
                    <p>Please find the attached Excel file for detailed insights.</p>
                </body>
            </html>
        """.trimIndent()

        helper.setText(htmlContent, true)
        helper.addBcc(mail)
        helper.addAttachment(fileName, InputStreamSource {
            ByteArrayInputStream(excelData.toByteArray())
        })
        mailSender.send(message)
    }

}
