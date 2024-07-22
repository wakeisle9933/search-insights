package com.si.main.searchinsights.service

import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import org.apache.commons.io.output.ByteArrayOutputStream
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamSource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream

@Service
class MailService(
    private val mailSender: JavaMailSender,

    @Value("\${mail.receiver}")
    private val mail: String
) {

    @Throws(MessagingException::class)
    fun sendMail(excelData: ByteArrayOutputStream, fileName: String) {
        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        helper.setSubject("Google Search Insights")
        helper.setText("", true) // HTML 내용 설정
        helper.addBcc(mail)
        helper.addAttachment(fileName, InputStreamSource {
            ByteArrayInputStream(excelData.toByteArray())
        })
        mailSender.send(message)
    }

}
