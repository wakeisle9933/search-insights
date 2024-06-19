package com.si.main.searchinsights.service

import jakarta.mail.MessagingException
import jakarta.mail.internet.MimeMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class MailService(
    private val mailSender: JavaMailSender,

    @Value("\${mail.receiver}")
    private val mail: String
) {

    @Throws(MessagingException::class)
    fun sendMail() {
        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true, "UTF-8")
        helper.setSubject("Google Search Insights")
        helper.setText("", true) // HTML 내용 설정
        helper.addBcc(mail)

        mailSender.send(message)
    }

}
