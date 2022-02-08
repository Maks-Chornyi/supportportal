package com.supportportal.supportportal.service;

import com.sun.mail.smtp.SMTPTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

import static com.supportportal.supportportal.config.constant.EmailConstant.*;

@Service
public class EmailService {

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    public void sendRegistrationEmail(String firstName, String password, String email) {
        Message message = createEmail(firstName, password, email);
        try {
            SMTPTransport smtpTransport = (SMTPTransport) getEmailSession().getTransport(SIMPLE_MAIL_TRANSFER_PROTOCOL);
            smtpTransport.connect(GMAIL_SMTP_SERVER, USERNAME, PASSWORD);
            final Address[] allRecipients = message.getAllRecipients();
            smtpTransport.sendMessage(message, allRecipients);
            smtpTransport.close();
            LOGGER.info("Message successfully sent to new user: " + firstName, ", email: " + email);
        } catch (MessagingException ex) {
            ex.getStackTrace();
            LOGGER.error("Message sending was unsuccessful for user: " +firstName + ", email: "+ email);
        }

    }

    private Message createEmail(String firstName, String password, String email) {
        MimeMessage message = new MimeMessage(getEmailSession());
        try {
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email, false));
//            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(CC_EMAIL, false));
            message.setSubject(EMAIL_SUBJECT);
            message.setText("Hello, " + firstName + "\n\nYour new account password is: " + password + "\n\nThe Support Team");
            message.setSentDate(new Date());
            message.saveChanges();
        } catch (MessagingException e) {
            e.printStackTrace();
        }

        return message;
    }

    private Session getEmailSession() {
        Properties properties = System.getProperties();
        properties.put(SMTP_HOST, GMAIL_SMTP_SERVER);
        properties.put(SMTP_AUTH, true);
        properties.put(SMTP_PORT, DEFAULT_PORT);
        properties.put(SMTP_STARTTLS_ENABLE, true);
        properties.put(SMTP_STARTTLS_REQUIRED, true);

        return Session.getInstance(properties, null);
    }
}
