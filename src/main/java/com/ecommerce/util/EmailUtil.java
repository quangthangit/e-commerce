package com.ecommerce.util;

import org.springframework.beans.factory.annotation.Value;

import com.ecommerce.Service.EmailService;
import com.ecommerce.model.Email;

public class EmailUtil {
	
    public static void sendVerificationEmail(String email, String token, EmailService emailService,String subject,String body) throws Exception {
        Email mail = new Email();
        mail.setSubject(subject);
        mail.setMessage(body);
        emailService.sendMail(email, mail);
    }
}
